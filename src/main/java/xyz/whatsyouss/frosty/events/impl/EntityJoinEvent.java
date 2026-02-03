package xyz.whatsyouss.frosty.events.impl;

import net.minecraft.entity.Entity;

public class EntityJoinEvent {
    public Entity entity;

    public EntityJoinEvent(Entity entity) {
        this.entity = entity;
    }
}
