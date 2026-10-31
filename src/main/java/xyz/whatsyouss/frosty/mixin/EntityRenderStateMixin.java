package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import xyz.whatsyouss.frosty.interfaces.IEntityRenderState;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements IEntityRenderState {
    @Unique
    private Entity entity;

    @Override
    public Entity frosty$getEntity() {
        return entity;
    }

    @Override
    public void frosty$setEntity(Entity entity) {
        this.entity = entity;
    }
}

