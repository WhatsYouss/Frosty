package xyz.whatsyouss.frosty.utility;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

import static xyz.whatsyouss.frosty.Frosty.mc;


public class EntityUtils extends RemotePlayer {

    public boolean doNotPush;

    public boolean hideWhenInsideCamera;

    public boolean noHit;

    public EntityUtils(RemotePlayer player, String name, float health, boolean copyInv) {
        super(mc.level, new GameProfile(UUID.randomUUID(), name));

        copyPosition(player);

        this.yRotO = getYRot();
        this.xRotO = getXRot();

        this.setYHeadRot(player.getYHeadRot());
        this.yHeadRotO = player.yHeadRotO;
        this.yBodyRot = player.yBodyRot;
        this.yBodyRotO = player.yBodyRotO;

        if (player.getAttributes() != null) {
            this.getAttributes().assignAllValues(player.getAttributes());
        }

        setPose(player.getPose());

        if (health <= 20) {
            setHealth(health);
        } else {
            setHealth(health);
            setAbsorptionAmount(health - 20);
        }

        if (copyInv) {
            this.getInventory().replaceWith(player.getInventory());
        }
    }

    public void spawn() {
        unsetRemoved();
        mc.level.addEntity(this);
    }

    public void despawn() {
        mc.level.removeEntity(getId(), RemovalReason.DISCARDED);
        setRemoved(RemovalReason.DISCARDED);
    }
}