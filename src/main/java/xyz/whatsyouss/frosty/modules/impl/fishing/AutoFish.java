package xyz.whatsyouss.frosty.modules.impl.fishing;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

import java.util.List;
import java.util.Random;

public class AutoFish extends Module {
    private ButtonSetting autoThrow;
    private ButtonSetting antiAFK;
    private SliderSetting maxWait;

    // 1=Throw, 2=Wait, 3=Finish
    private int currentMode = 0;

    private Thread antiAFKThread;
    private boolean hookBiten = false;
    private long throwTick = 0;
    private long waitStartTick = 0;
    private int rodSlot = 0;
    private InteractionHand rodHand = InteractionHand.MAIN_HAND;
    private boolean failed = false;

    public AutoFish() {
        super("AutoFish", "自动钓鱼", category.Fishing);

        this.registerSetting(autoThrow = new ButtonSetting("Auto Throw", true));
        this.registerSetting(antiAFK = new ButtonSetting("Anti AFK", true));
        this.registerSetting(maxWait = new SliderSetting("Max Wait", "s", 30, 5, 60, 1));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (!Utils.nullCheck()) {
            return;
        }
        checkRod();

        long currentTick = mc.level.getGameTime();

        switch (currentMode) {
            case 0 -> {
                if (autoThrow.isToggled()) {
                    currentMode = 1;
                }
            }
            case 1 -> { // Throw
                if (mc.player.fishing != null || rightClick()) {
                    currentMode = 2;
                    waitStartTick = currentTick;
                }
                throwTick = currentTick;
            }
            case 2 -> { // Wait
                checkForArmorStand();
                if (mc.player.fishing == null) {
                    if (currentTick - throwTick >= 20) {
                        currentMode = 1;
                    }
                    return;
                }
                if (currentTick - waitStartTick >= maxWait.getInput() * 20) {
                    currentMode = 3;
                }
                if (hookBiten) {
                    currentMode = 3;
                }
            }
            case 3 -> { // Finish
                rightClick();
                hookBiten = false;
                currentMode = 0;
            }
        }
    }

    @Override
    public void onEnable() {
        this.rodHand = null;
        ItemStack mainHand = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = mc.player.getItemInHand(InteractionHand.OFF_HAND);

        if (mainHand.getItem() instanceof FishingRodItem) {
            rodHand = InteractionHand.MAIN_HAND;
            rodSlot = mc.player.getInventory().getSelectedSlot();
        } else if (offHand.getItem() instanceof FishingRodItem) {
            rodHand = InteractionHand.OFF_HAND;
        }

        if (rodHand == null) {
            Utils.addChatMessage("You must holding your fishing rod!");
            this.disable();
            return;
        }

        if (antiAFK.isToggled()) {
            antiAFKThread = new Thread(() -> {
                while (isEnabled()) {
                    try {
                        Thread.sleep(15000 + (long) (new Random().nextDouble() * 10000 - 5000));
                        boolean direction = new Random().nextBoolean();
                        int ms = new Random().nextInt(0, 25) * 25;
                        float yawChange = 1.0f;

                        if (direction) {
                            mc.player.setYRot(mc.player.getYRot() + yawChange);
                            Thread.sleep(5000 + ms);
                            mc.player.setYRot(mc.player.getYRot() - yawChange);
                        } else {
                            mc.player.setYRot(mc.player.getYRot() - yawChange);
                            Thread.sleep(5000 + ms);
                            mc.player.setYRot(mc.player.getYRot() + yawChange);
                        }
                    } catch (Throwable e) {
                        if (e instanceof InterruptedException) break;
                        e.printStackTrace();
                    }
                }
            }, "antiAFK");
            antiAFKThread.start();
        }
    }

    @Override
    public void onDisable() {
        if (!failed && mc.player.fishing != null && rodHand != null) {
            mc.gameMode.useItem(mc.player, rodHand);
            mc.player.swing(rodHand);
        }
        failed = false;
        currentMode = 0;
        if (antiAFKThread != null) {
            antiAFKThread.interrupt();
        }
    }

    private void checkRod() {
        if (rodHand == null) {
            failed = true;
            this.disable();
            return;
        }
        if (rodHand == InteractionHand.MAIN_HAND) {
            if (rodSlot == mc.player.getInventory().getSelectedSlot()) {
                return;
            }
            Utils.addChatMessage("§7[§f§lAutoFish§7] §cItem changed, disable module!");
            failed = true;
            this.disable();
        }
    }

    private boolean rightClick() {
        if (mc.gameMode == null || rodHand == null) return false;
        mc.gameMode.useItem(mc.player, rodHand);
        mc.player.swing(rodHand);
        return true;
    }

    private void checkForArmorStand() {
        if (!Utils.nullCheck() || mc.player.fishing == null) return;

        List<ArmorStand> armorStands = mc.level.getEntitiesOfClass(
                ArmorStand.class,
                mc.player.fishing.getBoundingBox().inflate(2.0),
                stand -> true
        );

        for (ArmorStand stand : armorStands) {
            if (stand.getCustomName() != null) {
                if (stand.getCustomName().getString().contains("!!!")) {
                    hookBiten = true;
                    break;
                }
            }
        }
    }
}