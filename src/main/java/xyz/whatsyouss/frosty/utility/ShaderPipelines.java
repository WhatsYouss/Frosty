package xyz.whatsyouss.frosty.utility;

import java.util.Optional;

import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.DepthStencilState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline.Snippet;
import com.mojang.renderpearl.api.pipeline.ShaderType;
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

    public static final RenderPipeline ENTITY_TRANSLUCENT_NO_DEPTH = RenderPipelines
            .register(copyWithoutDepth(RenderPipelines.ENTITY_TRANSLUCENT,
                    Identifier.parse("frosty:pipeline/entity_translucent_no_depth")));

    private static RenderPipeline copyWithoutDepth(RenderPipeline source, Identifier location) {
        RenderPipeline.Builder builder = RenderPipeline.builder()
                .withLocation(location)
                .withVertexShader(source.getShaders().get(ShaderType.VERTEX))
                .withFragmentShader(source.getShaders().get(ShaderType.FRAGMENT))
                .withCull(source.isCull())
                .withPolygonMode(source.getPolygonMode())
                .withPrimitiveTopology(source.getPrimitiveTopology())
                .withPushConstantSize(source.pushConstantSize())
                .withDepthStencilState(Optional.empty());

        source.getShaderDefines().flags().forEach(builder::withShaderDefine);
        source.getShaderDefines().values().forEach((name, value) ->
                builder.withShaderDefine(name, Float.parseFloat(value)));

        var colorTargetStates = source.getColorTargetStates();
        for (int i = 0; i < colorTargetStates.size(); i++) {
            ColorTargetState state = colorTargetStates.get(i);
            if (state == null) {
                builder.withUnusedColorTargetState(i);
            } else {
                builder.withColorTargetState(i, state);
            }
        }

        for (int i = 0; i < source.getVertexFormatBindings().size(); i++) {
            builder.withVertexBinding(i, source.getVertexFormatBinding(i));
        }
        source.getBindGroupLayouts().forEach(builder::withBindGroupLayout);

        return builder.build();
    }
}
