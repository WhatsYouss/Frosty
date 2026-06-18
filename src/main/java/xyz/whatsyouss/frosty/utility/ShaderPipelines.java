package xyz.whatsyouss.frosty.utility;

import java.util.Optional;

import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public enum ShaderPipelines {
    ;

    /**
     * Similar to the RENDERTYPE_LINES Snippet, but without fog.
     */
    public static final Snippet FOGLESS_LINES_SNIPPET = RenderPipeline
            .builder(RenderPipelines.LINES_SNIPPET)
            .withVertexShader(Identifier.parse("frosty:fogless_lines"))
            .withFragmentShader(Identifier.parse("frosty:fogless_lines"))
            .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
            .withCull(false)
            .withVertexBinding(0,
                    DefaultVertexFormat.POSITION_COLOR_NORMAL_LINE_WIDTH)
            .withPrimitiveTopology(PrimitiveTopology.LINES).buildSnippet();

    /**
     * Similar to the LINES ShaderPipeline, but with no fog.
     */
    public static final RenderPipeline DEPTH_TEST_LINES =
            RenderPipelines.register(RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
                    .withLocation(
                            Identifier.parse("frosty:pipeline/depth_test_lines"))
                    .withDepthStencilState(DepthStencilState.DEFAULT).build());

    /**
     * Similar to the LINES ShaderPipeline, but with no depth test or fog.
     */
    public static final RenderPipeline ESP_LINES =
            RenderPipelines.register(RenderPipeline.builder(FOGLESS_LINES_SNIPPET)
                    .withLocation(Identifier.parse("frosty:pipeline/esp_lines"))
                    .withDepthStencilState(Optional.empty()).build());

    /**
     * Similar to the DEBUG_QUADS ShaderPipeline, but with culling enabled.
     */
    public static final RenderPipeline QUADS = RenderPipelines
            .register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.parse("frosty:pipeline/quads"))
                    .withDepthStencilState(DepthStencilState.DEFAULT).build());

    /**
     * Similar to the DEBUG_QUADS ShaderPipeline, but with culling enabled
     * and no depth test.
     */
    public static final RenderPipeline ESP_QUADS = RenderPipelines
            .register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.parse("frosty:pipeline/esp_quads"))
                    .withDepthStencilState(Optional.empty()).build());

    /**
     * Similar to the DEBUG_QUADS ShaderPipeline, but with no depth test.
     */
    public static final RenderPipeline ESP_QUADS_NO_CULLING = RenderPipelines
            .register(RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Identifier.parse("frosty:pipeline/esp_quads"))
                    .withDepthStencilState(Optional.empty()).withCull(false).build());
}