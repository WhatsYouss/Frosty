package xyz.whatsyouss.frosty.gui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class SmoothRoundedRenderer {
    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
                    .withLocation(Identifier.parse("frosty:pipeline/smooth_rounded_gui"))
                    .withVertexShader(Identifier.parse("frosty:smooth_rounded_gui"))
                    .withFragmentShader(Identifier.parse("frosty:smooth_rounded_gui"))
                    .withVertexFormat(DefaultVertexFormat.ENTITY, VertexFormat.Mode.QUADS)
                    .build());

    private SmoothRoundedRenderer() {
    }

    public static void fill(GuiGraphicsExtractor graphics, float x, float y, float width,
                            float height, float radius, int color) {
        add(graphics, x, y, width, height, radius, color, false, false);
    }

    public static void border(GuiGraphicsExtractor graphics, float x, float y, float width,
                              float height, float radius, int color) {
        add(graphics, x, y, width, height, radius, color, true, false);
    }

    public static void topFill(GuiGraphicsExtractor graphics, float x, float y, float width,
                               float height, float radius, int color) {
        add(graphics, x, y, width, height, radius, color, false, true);
    }

    public static void topBorder(GuiGraphicsExtractor graphics, float x, float y, float width,
                                 float height, float radius, int color) {
        add(graphics, x, y, width, height, radius, color, true, true);
    }

    private static void add(GuiGraphicsExtractor graphics, float x, float y, float width,
                            float height, float radius, int color, boolean border, boolean topOnly) {
        if (width <= 0.0f || height <= 0.0f || (color >>> 24) == 0) return;
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        int widthPixels = Math.max(1, Math.round(width * scale));
        int heightPixels = Math.max(1, Math.round(height * scale));
        int radiusPixels = Math.max(0, Math.round(Math.min(radius,
                Math.min(width * 0.5f, topOnly ? height : height * 0.5f)) * scale));
        int strokePixels = Math.max(1, Math.round(scale));
        int mode = topOnly ? (border ? -1 - strokePixels : -1) : (border ? strokePixels : 0);
        Matrix3x2f pose = new Matrix3x2f(graphics.pose());
        ScreenRectangle scissor = graphics.scissorStack.peek();
        int left = (int) Math.floor(x);
        int top = (int) Math.floor(y);
        int right = (int) Math.ceil(x + width);
        int bottom = (int) Math.ceil(y + height);
        ScreenRectangle bounds = new ScreenRectangle(left, top, right - left, bottom - top)
                .transformMaxBounds(pose);
        if (scissor != null) bounds = scissor.intersection(bounds);
        graphics.guiRenderState.addGuiElement(new RoundedState(pose, scissor, bounds,
                x, y, x + width, y + height, radiusPixels, mode, widthPixels, heightPixels, color));
    }

    private record RoundedState(Matrix3x2fc pose, ScreenRectangle scissorArea,
                                ScreenRectangle bounds, float x0, float y0, float x1, float y1,
                                int radiusPixels, int mode, int widthPixels, int heightPixels,
                                int color) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vertices) {
            vertex(vertices, x0, y0, 0.0f, 0.0f);
            vertex(vertices, x0, y1, 0.0f, 1.0f);
            vertex(vertices, x1, y1, 1.0f, 1.0f);
            vertex(vertices, x1, y0, 1.0f, 0.0f);
        }

        private void vertex(VertexConsumer vertices, float x, float y, float u, float v) {
            vertices.addVertexWith2DPose(pose, x, y)
                    .setUv(u, v)
                    .setColor(color)
                    .setUv1(radiusPixels, mode)
                    .setUv2(widthPixels, heightPixels)
                    .setNormal(0.0f, 0.0f, 1.0f);
        }

        @Override
        public RenderPipeline pipeline() {
            return PIPELINE;
        }

        @Override
        public TextureSetup textureSetup() {
            return TextureSetup.noTexture();
        }
    }
}
