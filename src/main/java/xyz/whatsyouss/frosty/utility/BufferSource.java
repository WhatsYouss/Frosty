package xyz.whatsyouss.frosty.utility;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Simple wrapper around {@link StagedVertexBuffer} to replace Minecraft's
 * {@code MultiBufferSource} which was removed in 26.2-snapshot-5.
 */
public final class BufferSource implements AutoCloseable
{
    private final StagedVertexBuffer stagedBuffer;
    private final List<StagedVertexBuffer.Draw> draws = new ArrayList<>();
    private final List<RenderType> drawTypes = new ArrayList<>();
    private final boolean reusable;
    private boolean closed;

    public BufferSource()
    {
        this(false);
    }

    private BufferSource(boolean reusable)
    {
        this.reusable = reusable;
        this.stagedBuffer = new StagedVertexBuffer(
                () -> "BufferSource", RenderType.BIG_BUFFER_SIZE);
    }

    public static BufferSource reusable()
    {
        return new BufferSource(true);
    }

    public VertexConsumer getBuffer(RenderType renderType)
    {
        if(closed)
            throw new IllegalStateException("BufferSource is closed");

        if(!drawTypes.isEmpty() && drawTypes.getLast() == renderType
                && renderType.canConsolidateConsecutiveGeometry())
            return stagedBuffer.getVertexBuilder(draws.getLast());

        StagedVertexBuffer.Draw draw =
                stagedBuffer.appendDraw(renderType.format(),
                        renderType.primitiveTopology(), renderType.sortOnUpload()
                                ? RenderSystem.getProjectionType().vertexSorting() : null);

        draws.add(draw);
        drawTypes.add(renderType);
        return stagedBuffer.getVertexBuilder(draw);
    }

    public void uploadAndDraw()
    {
        try
        {
            if(draws.isEmpty())
                return;

            stagedBuffer.upload();

            for(int i = 0; i < draws.size(); i++)
                draw(drawTypes.get(i), draws.get(i));

            if(reusable)
                stagedBuffer.endFrame();
            else
                stagedBuffer.endDraw();

        }finally
        {
            draws.clear();
            drawTypes.clear();
            if(!reusable)
                close();
        }
    }

    @Override
    public void close()
    {
        if(closed)
            return;

        closed = true;
        stagedBuffer.close();
    }

    private void draw(RenderType type, StagedVertexBuffer.Draw draw)
    {
        StagedVertexBuffer.ExecuteInfo info = stagedBuffer.getExecuteInfo(draw);

        if(info != null)
            type.prepare().drawFromBuffer(info);
    }
}
