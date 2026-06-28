package xyz.whatsyouss.frosty.modules.impl.other;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SelectSetting;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;
import xyz.whatsyouss.frosty.utility.Utils;

public class AutoHarp extends Module {

    private String[] modes = new String[]{"Normal", "Predict"};
    private SelectSetting mode;
    private SliderSetting delay, doubleClickDelay;

    private long clickTime = -1;
    private int targetSlot = -1;
    private int lastWoolSlot = -1;

    private int predictedSlot = -1;

    private boolean triggerDoubleClick = false;
    private boolean isExecutingDoubleClick = false;

    public AutoHarp() {
        super("AutoHarp", "自动竖琴", category.Other);

        this.registerSetting(mode = new SelectSetting("Mode", 0, modes));
        this.registerSetting(delay = new SliderSetting("Delay", "ms", 225, 100, 500, 25));
        this.registerSetting(doubleClickDelay = new SliderSetting("Double click delay", "ms", 350, 200, 500, 25));
    }

    @Override
    public void guiUpdate() {
        this.delay.setVisibilityCondition(() -> mode.getValue() == 1);
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.gui.screen() instanceof ContainerScreen genericContainerScreen) {
            String title = genericContainerScreen.getTitle().getString();

            if (!title.startsWith("Harp -")) {
                resetStatus();
                return;
            }

            AbstractContainerMenu menu = genericContainerScreen.getMenu();

            if (clickTime != -1 && System.currentTimeMillis() >= clickTime) {
                if (mode.getValue() == 0 && isExecutingDoubleClick) {
                    if (targetSlot != -1 && menu.getSlot(targetSlot).getItem().getItem() != Blocks.QUARTZ_BLOCK.asItem()) {
                        resetStatus();
                        return;
                    }
                }

                if (targetSlot != -1) {
                    clickSlot(menu, targetSlot);
                }

                if (triggerDoubleClick) {
                    triggerDoubleClick = false;
                    this.isExecutingDoubleClick = true;
                    long nextDelay = (long) doubleClickDelay.getInput();
                    this.clickTime = System.currentTimeMillis() + nextDelay;
                } else {
                    resetStatus();
                }
                return;
            }

            if (clickTime == -1) {
                if (mode.getValue() == 0) {
                    int currentWoolSlot = -1;
                    boolean isDoubleNote = false;
                    int foundQuartzSlot = -1;

                    for (int i = 37; i <= 43; i++) {
                        ItemStack itemStack = menu.getSlot(i).getItem();
                        if (itemStack.getItem() == Blocks.QUARTZ_BLOCK.asItem()) {
                            int upperSlot = i - 9;
                            ItemStack upperStack = menu.getSlot(upperSlot).getItem();

                            if (isWool(upperStack)) {
                                currentWoolSlot = upperSlot;
                                foundQuartzSlot = i;

                                ItemStack higherStack = menu.getSlot(upperSlot - 9).getItem();
                                if (isWool(higherStack)) {
                                    isDoubleNote = true;
                                }
                                break;
                            }
                        }
                    }

                    if (currentWoolSlot != -1) {
                        if (currentWoolSlot != lastWoolSlot) {
                            this.targetSlot = foundQuartzSlot;
                            this.clickTime = System.currentTimeMillis();
                            this.lastWoolSlot = currentWoolSlot;

                            if (isDoubleNote) {
                                this.triggerDoubleClick = true;
                            }
                        }
                    } else {
                        lastWoolSlot = -1;
                    }

                } else {
                    int currentWoolSlot = -1;
                    boolean isDoubleNote = false;

                    for (int i = 28; i <= 34; i++) {
                        ItemStack itemStack = menu.getSlot(i).getItem();
                        if (isWool(itemStack)) {
                            currentWoolSlot = i;

                            ItemStack upperStack = menu.getSlot(i - 9).getItem();
                            if (isWool(upperStack)) {
                                isDoubleNote = true;
                            }
                            break;
                        }
                    }

                    if (currentWoolSlot != -1) {
                        if (currentWoolSlot != lastWoolSlot) {
                            this.targetSlot = currentWoolSlot + 9;
                            long currentDelay = (long) delay.getInput();
                            this.clickTime = System.currentTimeMillis() + currentDelay;
                            this.lastWoolSlot = currentWoolSlot;

                            if (isDoubleNote) {
                                this.triggerDoubleClick = true;
                            }
                        }
                    } else {
                        lastWoolSlot = -1;
                    }

                    boolean upperHasWool = false;
                    int upperWoolSlot = -1;
                    for (int i = 19; i <= 25; i++) {
                        ItemStack itemStack = menu.getSlot(i).getItem();
                        if (isWool(itemStack)) {
                            if (currentWoolSlot != -1 && i == currentWoolSlot - 9) {
                                continue;
                            }
                            upperHasWool = true;
                            upperWoolSlot = i;
                            break;
                        }
                    }

                    if (upperHasWool) {
                        predictedSlot = upperWoolSlot;
                    } else if (predictedSlot != -1) {
                        this.targetSlot = predictedSlot + 18;
                        long currentDelay = (long) delay.getInput();
                        this.clickTime = System.currentTimeMillis() + currentDelay;
                        predictedSlot = -1;
                    }
                }
            }
        } else {
            resetStatus();
        }
    }

    private boolean isWool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        String name = stack.getItem().toString().toLowerCase();
        return name.endsWith("_wool") || name.equals("wool");
    }

    private void clickSlot(AbstractContainerMenu menu, int slotId) {
        mc.gameMode.handleContainerInput(menu.containerId,
                slotId,
                2,
                ContainerInput.CLONE,
                mc.player);
    }

    private void resetStatus() {
        this.clickTime = -1;
        this.targetSlot = -1;
        this.triggerDoubleClick = false;
        this.isExecutingDoubleClick = false;
    }

    @Override
    public void onDisable() {
        resetStatus();
        this.lastWoolSlot = -1;
        this.predictedSlot = -1;
    }
}