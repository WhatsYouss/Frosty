package xyz.whatsyouss.frosty.utility;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

public enum RenderLayers
{
    ;

    /**
     * Similar to {link RenderType#getLines()}, but with line width 2.
     */
    public static final RenderType LINES = RenderType.create("frosty:lines",
            RenderSetup.builder(ShaderPipelines.DEPTH_TEST_LINES)
                    .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                    .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                    .createRenderSetup());

    /**
     * Similar to {link RenderType#getLines()}, but with line width 2 and no
     * depth test.
     */
    public static final RenderType ESP_LINES =
            RenderType.create("frosty:esp_lines",
                    RenderSetup.builder(ShaderPipelines.ESP_LINES)
                            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                            .createRenderSetup());

    /**
     * Similar to {link RenderType#getDebugQuads()}, but with culling enabled.
     */
    public static final RenderType QUADS = RenderType.create("frosty:quads",
            RenderSetup.builder(ShaderPipelines.QUADS).sortOnUpload()
                    .createRenderSetup());

    /**
     * Similar to {link RenderType#getDebugQuads()}, but with culling enabled
     * and no depth test.
     */
    public static final RenderType ESP_QUADS = RenderType.create(
            "frosty:esp_quads", RenderSetup.builder(ShaderPipelines.ESP_QUADS)
                    .sortOnUpload().createRenderSetup());

    /**
     * Similar to {link RenderType#getDebugQuads()}, but with no depth test.
     */
    public static final RenderType ESP_QUADS_NO_CULLING =
            RenderType.create("frosty:esp_quads_no_culling",
                    RenderSetup.builder(ShaderPipelines.ESP_QUADS_NO_CULLING)
                            .sortOnUpload().useLightmap().createRenderSetup());

    /**
     * Returns either {@link #QUADS} or {@link #ESP_QUADS} depending on the
     * value of {@code depthTest}.
     */
    public static RenderType getQuads(boolean depthTest)
    {
        return depthTest ? QUADS : ESP_QUADS;
    }

    /**
     * Returns either {@link #LINES} or {@link #ESP_LINES} depending on the
     * value of {@code depthTest}.
     */
    public static RenderType getLines(boolean depthTest)
    {
        return depthTest ? LINES : ESP_LINES;
    }
}