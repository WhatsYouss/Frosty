package xyz.whatsyouss.frosty.gui;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import xyz.whatsyouss.frosty.modules.impl.client.UI;

public final class GlassRenderer {
    private static final int BLUR_RADIUS = 18;
    private static final int SHADOW_MARGIN = 32;
    private static final int MAX_RADIUS = 64;
    private static final float[] BLUR_WEIGHTS = gaussian(BLUR_RADIUS);
    private static final int BUFFER_USAGE = GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_MAP_WRITE;
    private static final Identifier GLASS_VERTEX = Identifier.parse("frosty:blit_fullscreen");
    private static final Identifier GLASS_FRAGMENT = Identifier.parse("frosty:liquid_glass");
    private static final Identifier BLUR_FRAGMENT = Identifier.parse("frosty:liquid_glass_blur");

    private static final List<Panel> panels = new ArrayList<>();
    private static boolean blurMarked;
    private static RenderPipeline glassPipeline;
    private static RenderPipeline blurPipeline;
    private static GpuBuffer quadBuffer;
    private static GpuBuffer samplerInfo;
    private static GpuBuffer panelInfo;
    private static GpuBuffer blurXConfig;
    private static GpuBuffer blurYConfig;
    private static GpuTexture blurTemp;
    private static GpuTextureView blurTempView;
    private static GpuTexture blurred;
    private static GpuTextureView blurredView;
    private static int physicalPanelX;
    private static int physicalPanelY;
    private static int physicalPanelWidth;
    private static int physicalPanelHeight;

    private GlassRenderer() {
    }

    public static void beginFrame() {
        panels.clear();
        blurMarked = false;
    }

    public static void recordPanel(GuiGraphicsExtractor context, float x, float y, float width,
                                   float height, float radius, boolean light) {
        recordPanel(context, x, y, width, height, radius, light, 1.0f);
    }

    public static void recordPanel(GuiGraphicsExtractor context, float x, float y, float width,
                                   float height, float radius, boolean light, float opacity) {
        if (!blurMarked) {
            context.blurBeforeThisStratum();
            blurMarked = true;
        }
        panels.add(new Panel(x, y, width, height, radius, light, clamp(opacity, 0.0f, 1.0f)));
    }

    public static boolean render(Minecraft minecraft) {
        if (panels.isEmpty()) {
            return false;
        }
        RenderSystem.assertOnRenderThread();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        ensureResources(mainTarget.width, mainTarget.height);
        uploadSamplerInfo(mainTarget.width, mainTarget.height);
        runBlur(mainTarget);
        for (Panel panel : panels) {
            uploadPanelInfo(minecraft, mainTarget.width, mainTarget.height, panel);
            composite(mainTarget);
        }
        return true;
    }

    private static void ensureResources(int width, int height) {
        if (glassPipeline == null) {
            glassPipeline = pipeline("liquid_glass", GLASS_FRAGMENT, "PanelInfo", "Sampler0", "Sampler1");
            blurPipeline = pipeline("liquid_glass_blur", BLUR_FRAGMENT, "BlurConfig", "DiffuseSampler");
            RenderSystem.getDevice().precompilePipeline(glassPipeline, null);
            RenderSystem.getDevice().precompilePipeline(blurPipeline, null);
        }
        if (quadBuffer == null) {
            ByteBuffer data = ByteBuffer.allocateDirect(48).order(ByteOrder.nativeOrder());
            data.asFloatBuffer().put(new float[]{0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0});
            quadBuffer = RenderSystem.getDevice().createBuffer(() -> "frosty-glass-quad", GpuBuffer.USAGE_VERTEX, data);
            samplerInfo = RenderSystem.getDevice().createBuffer(() -> "frosty-glass-sampler-info", BUFFER_USAGE, 16);
            panelInfo = RenderSystem.getDevice().createBuffer(() -> "frosty-glass-panel-info", BUFFER_USAGE, 96);
            long blurSize = 16L + (long) (MAX_RADIUS + 1) * 16L;
            blurXConfig = RenderSystem.getDevice().createBuffer(() -> "frosty-glass-blur-x", BUFFER_USAGE, blurSize);
            blurYConfig = RenderSystem.getDevice().createBuffer(() -> "frosty-glass-blur-y", BUFFER_USAGE, blurSize);
        }
        if (blurred == null || blurred.getWidth(0) != width || blurred.getHeight(0) != height) {
            closeBlurTargets();
            blurTemp = createTarget("frosty-glass-blur-temp", width, height);
            blurTempView = RenderSystem.getDevice().createTextureView(blurTemp);
            blurred = createTarget("frosty-glass-blur-result", width, height);
            blurredView = RenderSystem.getDevice().createTextureView(blurred);
        }
    }

    private static RenderPipeline pipeline(String name, Identifier fragment, String uniform, String... samplers) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(Identifier.parse("frosty:pipeline/" + name))
                .withVertexShader(GLASS_VERTEX)
                .withFragmentShader(fragment)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                .withUniform(uniform, UniformType.UNIFORM_BUFFER)
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withVertexFormat(DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS);
        for (String sampler : samplers) {
            builder.withSampler(sampler);
        }
        return builder.build();
    }

    private static GpuTexture createTarget(String label, int width, int height) {
        return RenderSystem.getDevice().createTexture(label,
                GpuTexture.USAGE_RENDER_ATTACHMENT | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8, width, height, 1, 1);
    }

    private static void uploadSamplerInfo(int width, int height) {
        try (GpuBuffer.MappedView mapped = RenderSystem.getDevice().createCommandEncoder().mapBuffer(samplerInfo, false, true)) {
            Std140Builder.intoBuffer(mapped.data()).putVec2(width, height).putVec2(width, height);
        }
        uploadBlur(blurXConfig, 1.0f, 0.0f);
        uploadBlur(blurYConfig, 0.0f, 1.0f);
    }

    private static void uploadPanelInfo(Minecraft minecraft, int width, int height, Panel panel) {
        float scale = (float) minecraft.getWindow().getGuiScale();
        float panelX = panel.x * scale;
        float panelY = height - (panel.y + panel.height) * scale;
        float panelWidth = panel.width * scale;
        float panelHeight = panel.height * scale;
        physicalPanelX = (int) Math.floor(panelX);
        physicalPanelY = (int) Math.floor(panelY);
        physicalPanelWidth = (int) Math.ceil(panelX + panelWidth) - physicalPanelX;
        physicalPanelHeight = (int) Math.ceil(panelY + panelHeight) - physicalPanelY;
        int mode = (int) UI.liquidGlassMode.getValue();
        float tint = mode == 0 ? (panel.light ? 0.00f : 0.01f) : (panel.light ? 0.85f : 0.89f);
        float surface = 0.28f + panel.opacity * 0.62f;

        try (GpuBuffer.MappedView mapped = RenderSystem.getDevice().createCommandEncoder().mapBuffer(panelInfo, false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(mapped.data());
            builder.putVec4(panelX, panelY, panelWidth, panelHeight);
            builder.putVec4(panel.radius * scale, panel.radius * scale, panel.radius * scale, panel.radius * scale);
            builder.putVec4(panel.light ? 0.72f : 0.18f, panel.light ? 0.84f : 0.31f, 1.0f, tint);
            builder.putVec4(42.0f, 1.17f, 1.0f, 120.0f);
            builder.putVec4(0.0f, 0.92f, 170.0f, 0.0f);
            builder.putVec4((float) (System.nanoTime() / 1_000_000_000.0), 13.0f, 0.26f, surface);
        }
    }

    private static void uploadBlur(GpuBuffer buffer, float x, float y) {
        try (GpuBuffer.MappedView mapped = RenderSystem.getDevice().createCommandEncoder().mapBuffer(buffer, false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(mapped.data());
            builder.putVec4(x, y, BLUR_RADIUS, 0.0f);
            for (int index = 0; index <= MAX_RADIUS; index++) {
                builder.putFloat(index <= BLUR_RADIUS ? BLUR_WEIGHTS[index] : 0.0f);
                builder.align(16);
            }
        }
    }

    private static float[] gaussian(int radius) {
        float[] weights = new float[radius + 1];
        float sigma = radius / 3.0f;
        float sum = 0.0f;
        for (int index = 0; index <= radius; index++) {
            float value = (float) Math.exp(-0.5f * index * index / (sigma * sigma));
            weights[index] = value;
            sum += index == 0 ? value : value * 2.0f;
        }
        for (int index = 0; index <= radius; index++) weights[index] /= sum;
        return weights;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void runBlur(RenderTarget mainTarget) {
        var encoder = RenderSystem.getDevice().createCommandEncoder();
        int refractionMargin = Math.max(64, (int) Math.ceil(mainTarget.height * 0.06));
        draw(encoder, blurTempView, blurPipeline, blurXConfig, "DiffuseSampler", mainTarget.getColorTextureView(),
                blurArea(mainTarget, refractionMargin + BLUR_RADIUS));
        draw(encoder, blurredView, blurPipeline, blurYConfig, "DiffuseSampler", blurTempView,
                blurArea(mainTarget, refractionMargin));
    }

    private static void composite(RenderTarget mainTarget) {
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "frosty liquid glass", mainTarget.getColorTextureView(), OptionalInt.empty(),
                mainTarget.useDepth ? mainTarget.getDepthTextureView() : null, OptionalDouble.empty())) {
            pass.setPipeline(glassPipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("SamplerInfo", samplerInfo);
            pass.setUniform("PanelInfo", panelInfo);
            var sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
            pass.bindTexture("Sampler0", mainTarget.getColorTextureView(), sampler);
            pass.bindTexture("Sampler1", blurredView, sampler);
            applyScissor(pass, blurArea(mainTarget, SHADOW_MARGIN));
            drawQuad(pass);
        }
    }

    private static void draw(com.mojang.blaze3d.systems.CommandEncoder encoder, GpuTextureView output,
                             RenderPipeline pipeline, GpuBuffer config, String samplerName, GpuTextureView input,
                             Scissor area) {
        try (RenderPass pass = encoder.createRenderPass(() -> "frosty liquid glass blur", output, OptionalInt.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("SamplerInfo", samplerInfo);
            pass.setUniform("BlurConfig", config);
            pass.bindTexture(samplerName, input, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            applyScissor(pass, area);
            drawQuad(pass);
        }
    }

    private static Scissor blurArea(RenderTarget target, int margin) {
        int x0 = Math.clamp(physicalPanelX - margin, 0, target.width);
        int y0 = Math.clamp(physicalPanelY - margin, 0, target.height);
        int x1 = Math.clamp(physicalPanelX + physicalPanelWidth + margin, 0, target.width);
        int y1 = Math.clamp(physicalPanelY + physicalPanelHeight + margin, 0, target.height);
        return new Scissor(x0, y0, Math.max(0, x1 - x0), Math.max(0, y1 - y0));
    }

    private static void applyScissor(RenderPass pass, Scissor area) {
        pass.enableScissor(area.x, area.y, area.width, area.height);
    }

    private static void drawQuad(RenderPass pass) {
        var indexInfo = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        pass.setVertexBuffer(0, quadBuffer);
        pass.setIndexBuffer(indexInfo.getBuffer(6), indexInfo.type());
        pass.drawIndexed(0, 0, 6, 1);
    }

    private static void closeBlurTargets() {
        if (blurTempView != null) blurTempView.close();
        if (blurTemp != null) blurTemp.close();
        if (blurredView != null) blurredView.close();
        if (blurred != null) blurred.close();
        blurTempView = null;
        blurTemp = null;
        blurredView = null;
        blurred = null;
    }

    private record Panel(float x, float y, float width, float height, float radius, boolean light, float opacity) {
    }

    private record Scissor(int x, int y, int width, int height) {
    }
}
