package xyz.whatsyouss.frosty.utility;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

import static xyz.whatsyouss.frosty.Frosty.mc;


public class EntityUtils extends OtherClientPlayerEntity {

    public boolean doNotPush;

    public boolean hideWhenInsideCamera;

    public boolean noHit;

    public EntityUtils(PlayerEntity player, String name, float health, boolean copyInv) {
        super(mc.world, new GameProfile(UUID.randomUUID(), name));

        copyPositionAndRotation(player);

        lastYaw = getYaw();
        lastPitch = getPitch();
        headYaw = player.headYaw;
        lastHeadYaw = headYaw;
        bodyYaw = player.bodyYaw;
        lastBodyYaw = bodyYaw;

        getAttributes().setFrom(player.getAttributes());
        setPose(player.getPose());

        if (health <= 20) {
            setHealth(health);
        } else {
            setHealth(health);
            setAbsorptionAmount(health - 20);
        }

        if (copyInv) getInventory().clone(player.getInventory());
    }

    public void spawn() {
        unsetRemoved();
        mc.world.addEntity(this);
    }

    public void despawn() {
        mc.world.removeEntity(getId(), RemovalReason.DISCARDED);
        setRemoved(RemovalReason.DISCARDED);
    }
}