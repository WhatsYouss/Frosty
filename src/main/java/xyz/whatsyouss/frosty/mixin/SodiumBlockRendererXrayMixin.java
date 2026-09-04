package xyz.whatsyouss.frosty.mixin;

import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.material.Material;
import net.caffeinemc.mods.sodium.client.render.model.MutableQuadViewImpl;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.impl.render.Xray;

@Pseudo
@Mixin(value = BlockRenderer.class, remap = false)
public abstract class SodiumBlockRendererXrayMixin {
    @Unique
    private BlockState frosty$currentState;

    @Inject(
            method = "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD")
    )
    private void frosty$xrayCaptureState(BlockStateModel model, BlockState state, BlockPos pos,
                                          BlockPos origin, CallbackInfo ci) {
        frosty$currentState = state;
    }

    @ModifyArg(
            method = "renderModel(Lnet/minecraft/client/renderer/block/dispatch/BlockStateModel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;prepareCulling(Z)V"
            ),
            index = 0
    )
    private boolean frosty$xrayDisableOreCulling(boolean cull) {
        return Xray.shouldShowBlock(frosty$currentState) ? false : cull;
    }

    @Inject(
            method = "processQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void frosty$xrayApplyOpacity(MutableQuadViewImpl quad, CallbackInfo ci) {
        int alpha = Xray.transparentAlphaFor(frosty$currentState);
        if (alpha < 0) {
            return;
        }

        quad.setRenderType(ChunkSectionLayer.TRANSLUCENT);
        for (int i = 0; i < 4; i++) {
            quad.setColor(i, alpha << 24 | quad.baseColor(i) & 0x00FFFFFF);
        }
    }

    @ModifyArg(
            method = "processQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;bufferQuad(Lnet/caffeinemc/mods/sodium/client/render/model/MutableQuadViewImpl;[FLnet/caffeinemc/mods/sodium/client/render/chunk/terrain/material/Material;)V"
            ),
            index = 2
    )
    private Material frosty$xrayUseTranslucentMaterial(Material material) {
        return Xray.shouldTransparentBlock(frosty$currentState) ? DefaultMaterials.TRANSLUCENT : material;
    }
}
