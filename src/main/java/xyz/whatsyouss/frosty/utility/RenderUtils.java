package xyz.whatsyouss.frosty.utility;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import org.joml.*;

import java.awt.Color;
import java.lang.Math;
import java.nio.ByteBuffer;
import java.util.*;

import static xyz.whatsyouss.frosty.Frosty.mc;

public class RenderUtils {

    public record ColoredBox(AABB box, Color color) {}

    public static Vec3 center;

    private static final ByteBufferBuilder BYTE_BUFFER = new ByteBufferBuilder(65536);

    public static final Matrix4f projection = new Matrix4f();

    private static final float DISTANCE_SCALE_FACTOR = 0.0025f;

    private static GpuTexture     whiteTexture;
    private static GpuTextureView whiteTextureView;
    private static GpuSampler     whiteSampler;

    private static void uploadToGpuBuffer(CommandEncoder encoder,
                                          GpuBuffer gpuBuf,
                                          ByteBuffer data,
                                          int size) {
        encoder.writeToBuffer(gpuBuf.slice(0, size), data);
    }

    private static RenderPipeline getGuiPipeline() {
        return RenderPipelines.GUI;
    }

    private static RenderPipeline getWorldPipeline() {
        return RenderPipelines.LINES;
    }

    public static void rect(GuiGraphicsExtractor ctx,
                            float x1, float y1, float x2, float y2, int color) {
        ctx.fill((int)x1, (int)y1, (int)x2, (int)y2, color);
    }

    public static void rect(GuiGraphicsExtractor ctx, float x1, float y1, float x2, float y2, int color, float width) {
        int iw = Math.max(1, (int) width);
        int ix1 = (int) x1, iy1 = (int) y1, ix2 = (int) x2, iy2 = (int) y2;
        ctx.fill(ix1,       iy1,       ix2,       iy1 + iw, color);
        ctx.fill(ix1,       iy2 - iw,  ix2,       iy2,      color);
        ctx.fill(ix1,       iy1,       ix1 + iw,  iy2,      color);
        ctx.fill(ix2 - iw,  iy1,       ix2,       iy2,      color);
    }

    public static void drawBorder(GuiGraphicsExtractor ctx,
                                  int x, int y, int width, int height, int color) {
        ctx.fill(x,             y,              x + width, y + 1,         color);
        ctx.fill(x,             y + height - 1, x + width, y + height,    color);
        ctx.fill(x,             y,              x + 1,     y + height,    color);
        ctx.fill(x + width - 1, y,              x + width, y + height,    color);
    }

    public static void drawRoundedRect(GuiGraphicsExtractor ctx,
                                       float x, float y, float width, float height,
                                       float radius, int color) {
        int left = (int) x;
        int top = (int) y;
        int right = (int) (x + width);
        int bottom = (int) (y + height);
        int roundedRadius = Math.max(0, Math.min((int) radius,
                Math.min((right - left) / 2, (bottom - top) / 2)));

        for (int row = top; row < bottom; row++) {
            int inset = roundedInset(row - top, bottom - top, roundedRadius);
            ctx.fill(left + inset, row, right - inset, row + 1, color);
        }
    }

    public static void drawRoundedBorder(GuiGraphicsExtractor ctx,
                                         float x, float y, float width, float height,
                                         float radius, int color) {
        int left = (int) x;
        int top = (int) y;
        int right = (int) (x + width);
        int bottom = (int) (y + height);
        int roundedRadius = Math.max(0, Math.min((int) radius,
                Math.min((right - left) / 2, (bottom - top) / 2)));

        for (int row = top; row < bottom; row++) {
            int outerInset = roundedInset(row - top, bottom - top, roundedRadius);
            if (row == top || row == bottom - 1) {
                ctx.fill(left + outerInset, row, right - outerInset, row + 1, color);
                continue;
            }

            int innerInset = roundedInset(row - top - 1, bottom - top - 2,
                    Math.max(0, roundedRadius - 1));
            int outerLeft = left + outerInset;
            int outerRight = right - outerInset;
            int innerLeft = left + 1 + innerInset;
            int innerRight = right - 1 - innerInset;
            ctx.fill(outerLeft, row, Math.min(innerLeft, outerRight), row + 1, color);
            ctx.fill(Math.max(innerRight, outerLeft), row, outerRight, row + 1, color);
        }
    }

    private static int roundedInset(int row, int height, int radius) {
        if (radius <= 0 || row >= radius && row < height - radius) {
            return 0;
        }

        float centerOffset = row < radius ? radius - row - 0.5f
                : row - (height - radius) + 0.5f;
        return Math.max(0, (int) Math.ceil(radius - Math.sqrt(radius * radius
                - centerOffset * centerOffset)));
    }

    public static void drawCircle(GuiGraphicsExtractor ctx,
                                  float cx, float cy, float radius, int color) {
        fillArc(ctx, cx, cy, radius, 0, 360, color);
    }

    private static void fillArc(GuiGraphicsExtractor ctx,
                                float cx, float cy, float r,
                                float startDeg, float sweepDeg, int color) {
        int segs = Math.max(8, (int) Math.ceil(r));
        float step = sweepDeg / segs;
        for (int i = 0; i < segs; i++) {
            double a1 = Math.toRadians(startDeg + i * step);
            double a2 = Math.toRadians(startDeg + (i + 1) * step);
            int rx1 = (int)(Math.cos(a1) * r), ry1 = (int)(Math.sin(a1) * r);
            int rx2 = (int)(Math.cos(a2) * r), ry2 = (int)(Math.sin(a2) * r);
            int minX = (int)(cx + Math.min(0, Math.min(rx1, rx2)));
            int maxX = (int)(cx + Math.max(0, Math.max(rx1, rx2)) + 1);
            int minY = (int)(cy + Math.min(0, Math.min(ry1, ry2)));
            int maxY = (int)(cy + Math.max(0, Math.max(ry1, ry2)) + 1);
            ctx.fill(minX, minY, maxX, maxY, color);
        }
    }

    private static void outlineArc(GuiGraphicsExtractor ctx,
                                   float cx, float cy, float r,
                                   float startDeg, float sweepDeg, int color) {
        int segs = Math.max(8, (int) Math.ceil(r));
        float step = sweepDeg / segs;
        for (int i = 0; i <= segs; i++) {
            double a = Math.toRadians(startDeg + i * step);
            int px = (int)(cx + Math.cos(a) * r);
            int py = (int)(cy + Math.sin(a) * r);
            ctx.fill(px, py, px + 1, py + 1, color);
        }
    }

    public static void drawBoxFilled(PoseStack stack, AABB box, Color c, boolean depthTest) {
        MultiBufferSource.BufferSource vcp = getVCP();
        RenderType layer = RenderLayers.getQuads(depthTest);
        VertexConsumer buffer = vcp.getBuffer(layer);

        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        AABB relative = box.move(-cam.x, -cam.y, -cam.z);

        int color = c.getRGB() | (c.getAlpha() << 24);
        drawSolidBoxInternal(stack, buffer, relative, color);

        vcp.endBatch(layer);
    }

    public static void drawBoxes(PoseStack stack, Collection<ColoredBox> boxes, float fillAlpha,
                                 float outlineAlpha, float lineWidth, boolean drawFill,
                                 boolean drawOutline, boolean depthTest) {
        if (boxes.isEmpty()) return;

        MultiBufferSource.BufferSource buffers = getVCP();
        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        if (drawFill) {
            RenderType layer = RenderLayers.getQuads(depthTest);
            VertexConsumer buffer = buffers.getBuffer(layer);
            for (ColoredBox box : boxes) {
                drawSolidBoxInternal(stack, buffer, box.box().move(-cam.x, -cam.y, -cam.z), withAlpha(box.color(), fillAlpha));
            }
            buffers.endBatch(layer);
        }
        if (drawOutline) {
            RenderType layer = RenderLayers.getLines(depthTest);
            VertexConsumer buffer = buffers.getBuffer(layer);
            for (ColoredBox box : boxes) {
                drawOutlinedBoxInternal(stack, buffer, box.box().move(-cam.x, -cam.y, -cam.z), withAlpha(box.color(), outlineAlpha), lineWidth);
            }
            buffers.endBatch(layer);
        }
    }

    private static int withAlpha(Color color, float alpha) {
        int value = Mth.clamp(Math.round(alpha * 255.0f), 0, 255);
        return value << 24 | color.getRGB() & 0x00FFFFFF;
    }

    private static void drawSolidBoxInternal(PoseStack matrices, VertexConsumer buffer, AABB box, int color) {
        PoseStack.Pose entry = matrices.last();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        buffer.addVertex(entry, x1, y1, z1).setColor(color);
        buffer.addVertex(entry, x2, y1, z1).setColor(color);
        buffer.addVertex(entry, x2, y1, z2).setColor(color);
        buffer.addVertex(entry, x1, y1, z2).setColor(color);

        buffer.addVertex(entry, x1, y2, z1).setColor(color);
        buffer.addVertex(entry, x1, y2, z2).setColor(color);
        buffer.addVertex(entry, x2, y2, z2).setColor(color);
        buffer.addVertex(entry, x2, y2, z1).setColor(color);

        buffer.addVertex(entry, x1, y1, z1).setColor(color);
        buffer.addVertex(entry, x1, y2, z1).setColor(color);
        buffer.addVertex(entry, x2, y2, z1).setColor(color);
        buffer.addVertex(entry, x2, y1, z1).setColor(color);

        buffer.addVertex(entry, x2, y1, z1).setColor(color);
        buffer.addVertex(entry, x2, y2, z1).setColor(color);
        buffer.addVertex(entry, x2, y2, z2).setColor(color);
        buffer.addVertex(entry, x2, y1, z2).setColor(color);

        buffer.addVertex(entry, x1, y1, z2).setColor(color);
        buffer.addVertex(entry, x2, y1, z2).setColor(color);
        buffer.addVertex(entry, x2, y2, z2).setColor(color);
        buffer.addVertex(entry, x1, y2, z2).setColor(color);

        buffer.addVertex(entry, x1, y1, z1).setColor(color);
        buffer.addVertex(entry, x1, y1, z2).setColor(color);
        buffer.addVertex(entry, x1, y2, z2).setColor(color);
        buffer.addVertex(entry, x1, y2, z1).setColor(color);
    }

    public static void drawBox(PoseStack stack, AABB box, Color c, float lineWidth, boolean depthTest) {
        MultiBufferSource.BufferSource vcp = getVCP();
        RenderType layer = RenderLayers.getLines(depthTest);
        VertexConsumer buffer = vcp.getBuffer(layer);

        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        AABB relative = box.move(-cam.x, -cam.y, -cam.z);

        int color = c.getRGB() | (c.getAlpha() << 24);
        drawOutlinedBoxInternal(stack, buffer, relative, color, lineWidth);

        vcp.endBatch(layer);
    }

    private static void drawOutlinedBoxInternal(PoseStack matrices, VertexConsumer buffer, AABB box, int color, float lineWidth) {
        PoseStack.Pose entry = matrices.last();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // bottom
        buffer.addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, 1, 0, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y1, z1).setColor(color).setNormal(entry, 1, 0, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, 0, 0, 1).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y1, z2).setColor(color).setNormal(entry, 0, 0, 1).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y1, z1).setColor(color).setNormal(entry, 0, 0, 1).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y1, z2).setColor(color).setNormal(entry, 0, 0, 1).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y1, z2).setColor(color).setNormal(entry, 1, 0, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y1, z2).setColor(color).setNormal(entry, 1, 0, 0).setLineWidth(lineWidth);

        // top
        buffer.addVertex(entry, x1, y2, z1).setColor(color).setNormal(entry, 1, 0, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y2, z1).setColor(color).setNormal(entry, 1, 0, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y2, z1).setColor(color).setNormal(entry, 0, 0, 1).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y2, z2).setColor(color).setNormal(entry, 0, 0, 1).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y2, z1).setColor(color).setNormal(entry, 0, 0, 1).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, 0, 0, 1).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y2, z2).setColor(color).setNormal(entry, 1, 0, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, 1, 0, 0).setLineWidth(lineWidth);

        // verticals
        buffer.addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y2, z1).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y1, z1).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y2, z1).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y1, z2).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x1, y2, z2).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y1, z2).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, 0, 1, 0).setLineWidth(lineWidth);
    }

    public static void drawBox(PoseStack stack, Vec3 vec, Color c, float lineWidth, boolean depthTest) {
        drawBox(stack, AABB.unitCubeFromLowerCorner(vec), c, lineWidth, depthTest);
    }

    public static void drawLine3D(PoseStack stack, Vec3 from, Vec3 to, Color c, float lineWidth, boolean depthTest) {
        if (from.distanceToSqr(to) < 1.0E-6) return;

        MultiBufferSource.BufferSource vcp = getVCP();
        RenderType layer = RenderLayers.getLines(depthTest);
        VertexConsumer buffer = vcp.getBuffer(layer);
        PoseStack.Pose entry = stack.last();

        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        float x1 = (float) (from.x - cam.x);
        float y1 = (float) (from.y - cam.y);
        float z1 = (float) (from.z - cam.z);
        float x2 = (float) (to.x - cam.x);
        float y2 = (float) (to.y - cam.y);
        float z2 = (float) (to.z - cam.z);

        Vec3 dir = to.subtract(from).normalize();
        int color = c.getRGB() | (c.getAlpha() << 24);
        buffer.addVertex(entry, x1, y1, z1).setColor(color).setNormal(entry, (float) dir.x, (float) dir.y, (float) dir.z).setLineWidth(lineWidth);
        buffer.addVertex(entry, x2, y2, z2).setColor(color).setNormal(entry, (float) dir.x, (float) dir.y, (float) dir.z).setLineWidth(lineWidth);

        vcp.endBatch(layer);
    }

    public static void drawBox(PoseStack stack, BlockPos bp, Color c, float lineWidth, boolean depthTest) {
        drawBox(stack, new AABB(bp), c, lineWidth, depthTest);
    }

    public static void outlineEntity(PoseStack matrices, Entity entity,
                                     Color color, float lineWidth, float partialTicks, boolean depthTest) {
        AABB box = entity.getBoundingBox();
        double ox = Mth.lerp(partialTicks, entity.xOld, entity.getX()) - entity.getX();
        double oy = Mth.lerp(partialTicks, entity.yOld, entity.getY()) - entity.getY();
        double oz = Mth.lerp(partialTicks, entity.zOld, entity.getZ()) - entity.getZ();
        drawBox(matrices, box.move(ox, oy, oz), color, lineWidth, depthTest);
    }

    public static void outlineEntity(PoseStack matrices, Entity entity,
                                     Color color, float lineWidth, boolean depthTest) {
        outlineEntity(matrices, entity, color, lineWidth, 1.0f, depthTest);
    }

    public static void drawBlockFilled(PoseStack stack, BlockPos pos, Color color, float alpha, boolean depthTest) {
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
        drawBoxFilled(stack, new AABB(pos), c, depthTest);
    }

    public static void drawBlockOutline(PoseStack stack, BlockPos pos, Color color, float lineWidth, boolean depthTest) {
        drawBox(stack, new AABB(pos), color, lineWidth, depthTest);
    }

    public static void drawFullBlock(PoseStack stack, BlockPos pos,
                                     Color color, float lineWidth, float fillAlpha, boolean depthTest) {
        drawBlockFilled(stack, pos, color, fillAlpha, depthTest);
        drawBlockOutline(stack, pos, color, lineWidth, depthTest);
    }

    private static void quadColor(BufferBuilder buf, Matrix4f m, int rgb,
                                  float x0, float y0, float z0,
                                  float x1, float y1, float z1,
                                  float x2, float y2, float z2,
                                  float x3, float y3, float z3) {
        buf.addVertex(m, x0, y0, z0).setColor(rgb);
        buf.addVertex(m, x1, y1, z1).setColor(rgb);
        buf.addVertex(m, x2, y2, z2).setColor(rgb);
        buf.addVertex(m, x0, y0, z0).setColor(rgb);
        buf.addVertex(m, x2, y2, z2).setColor(rgb);
        buf.addVertex(m, x3, y3, z3).setColor(rgb);
    }

    private static void edgeRaw(BufferBuilder buf, Matrix4f m, Vec3 cam,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2,
                                double thickness, Color color) {
        Vec3 start = new Vec3(x1 - cam.x, y1 - cam.y, z1 - cam.z);
        Vec3 end   = new Vec3(x2 - cam.x, y2 - cam.y, z2 - cam.z);
        Vec3 dir   = end.subtract(start);

        double fovRad      = Math.toRadians(mc.options.fov().get());
        double pixelToWorld = Math.tan(fovRad * 0.5) / mc.getWindow().getHeight();

        Vec3 os = crossNorm(dir, start);
        Vec3 oe = crossNorm(dir, end);
        if (os == null || oe == null) return;

        double ds = start.length(), de = end.length();
        os = os.scale(thickness * ds * pixelToWorld);
        oe = oe.scale(thickness * de * pixelToWorld);

        int rgb = color.getRGB();
        buf.addVertex(m, (float)(start.x-os.x), (float)(start.y-os.y), (float)(start.z-os.z)).setColor(rgb);
        buf.addVertex(m, (float)(start.x+os.x), (float)(start.y+os.y), (float)(start.z+os.z)).setColor(rgb);
        buf.addVertex(m, (float)(end.x+oe.x),   (float)(end.y+oe.y),   (float)(end.z+oe.z)  ).setColor(rgb);
        buf.addVertex(m, (float)(start.x-os.x), (float)(start.y-os.y), (float)(start.z-os.z)).setColor(rgb);
        buf.addVertex(m, (float)(end.x+oe.x),   (float)(end.y+oe.y),   (float)(end.z+oe.z)  ).setColor(rgb);
        buf.addVertex(m, (float)(end.x-oe.x),   (float)(end.y-oe.y),   (float)(end.z-oe.z)  ).setColor(rgb);
    }

    private static void edge3D(BufferBuilder buf, Matrix4f m, Vec3 cam,
                               AABB ignored, double thickness, Color color,
                               double x1, double y1, double z1,
                               double x2, double y2, double z2) {
        edgeRaw(buf, m, cam, x1, y1, z1, x2, y2, z2, thickness, color);
    }

    private static Vec3 crossNorm(Vec3 a, Vec3 b) {
        Vec3 cross = new Vec3(
                a.y * b.z - a.z * b.y,
                a.z * b.x - a.x * b.z,
                a.x * b.y - a.y * b.x);
        double len = cross.length();
        if (len < 1e-6) return null;
        return cross.scale(1.0 / len);
    }

    public static void free() {
        BYTE_BUFFER.close();
        if (whiteTextureView != null) { whiteTextureView.close(); whiteTextureView = null; }
        if (whiteTexture     != null) { whiteTexture.close();     whiteTexture     = null; }
    }

    public static void updateScreenCenter(Matrix4fc projection, Matrix4fc view) {
        RenderUtils.projection.set(projection);

        Matrix4f invProjection = new Matrix4f(projection).invert();
        Matrix4f invView = new Matrix4f(view).invert();

        Vector4f center4 = new Vector4f(0, 0, 0, 1).mul(invProjection).mul(invView);
        center4.div(center4.w);

        Vec3 camera = mc.gameRenderer.getMainCamera().position();
        center = new Vec3(camera.x + center4.x, camera.y + center4.y, camera.z + center4.z);
    }

    public static void drawText3D(PoseStack matrix, String text, double x, double y, double z, Color color, boolean scaleWithDistance) {
        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        float relX = (float) (x - cam.x);
        float relY = (float) (y - cam.y);
        float relZ = (float) (z - cam.z);

        matrix.pushPose();
        matrix.translate(relX, relY, relZ);

        Vector3f toCam = new Vector3f(-relX, -relY, -relZ);
        if (toCam.lengthSquared() < 1e-6f) toCam.set(0, 0, 1);
        toCam.normalize();

        float yaw = mc.getEntityRenderDispatcher().camera.yRot();
        float wrappedYaw = (yaw % 360 + 360) % 360;

        boolean isNorthSouth = (wrappedYaw >= 135 && wrappedYaw <= 225) || (wrappedYaw <= 45 || wrappedYaw >= 315);
        Vector3f toCamByYaw = isNorthSouth ? toCam.negate() : toCam;

        Quaternionf billboardQuat = new Quaternionf().lookAlong(toCamByYaw, new Vector3f(0, 1, 0));

        matrix.mulPose(billboardQuat);

        float scale = scaleWithDistance ? 0.025f : (float)(DISTANCE_SCALE_FACTOR * cam.distanceTo(new Vec3(x, y, z)));
        matrix.scale(scale, -scale, scale);

        int packedColor = color.getRGB() | (color.getAlpha() << 24);
        Font font = mc.font;
        int textWidth = font.width(text);
        Font.PreparedText prepared = font.prepareText(text, -textWidth / 2f, 0f, packedColor, false, 0);

        Matrix4f glyphMatrix = matrix.last().pose();

        MultiBufferSource.BufferSource vcp = getVCP();
        Font.DisplayMode displayMode = Font.DisplayMode.SEE_THROUGH;

        prepared.visit(new Font.GlyphVisitor() {
            @Override
            public void acceptGlyph(TextRenderable.Styled glyph) {
                this.render(glyph);
            }

            @Override
            public void acceptEffect(TextRenderable effect) {
                this.render(effect);
            }

            private void render(TextRenderable renderable) {
                VertexConsumer builder = vcp.getBuffer(renderable.renderType(displayMode));
                renderable.render(glyphMatrix, builder, 15728880, false);
            }
        });

        vcp.endBatch();

        matrix.popPose();
    }

    public static MultiBufferSource.BufferSource getVCP()
    {
        return mc.renderBuffers().bufferSource();
    }
}
