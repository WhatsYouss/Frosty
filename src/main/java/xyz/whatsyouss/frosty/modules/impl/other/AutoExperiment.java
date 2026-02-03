package xyz.whatsyouss.frosty.modules.impl.other;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

import java.util.ArrayList;
import java.util.List;

public class AutoExperiment extends Module {

    private SliderSetting delay;

    private static final Object2ObjectMap<Item, Item> TERRACOTTA_TO_GLASS = Object2ObjectMaps.unmodifiable(
            new Object2ObjectArrayMap<>(
                    new Item[]{
                            Items.RED_TERRACOTTA, Items.ORANGE_TERRACOTTA, Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA,
                            Items.GREEN_TERRACOTTA, Items.CYAN_TERRACOTTA, Items.LIGHT_BLUE_TERRACOTTA, Items.BLUE_TERRACOTTA,
                            Items.PURPLE_TERRACOTTA, Items.PINK_TERRACOTTA
                    },
                    new Item[]{
                            Items.RED_STAINED_GLASS, Items.ORANGE_STAINED_GLASS, Items.YELLOW_STAINED_GLASS, Items.LIME_STAINED_GLASS,
                            Items.GREEN_STAINED_GLASS, Items.CYAN_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS, Items.BLUE_STAINED_GLASS,
                            Items.PURPLE_STAINED_GLASS, Items.PINK_STAINED_GLASS
                    }
            )
    );

    private boolean chronoGlintFound = false;
    private int chronoGlintFoundAt = -1;
    private List<Item> chronoClickStack = new ArrayList<>();
    private int chronoLastCycle = 0;
    private int chronoCurrentCycle = 0;
    private Item chronoLastModeItem = null;
    private int chronoStartSeconds = -1;

    private List<Integer> ultraClickStack = new ArrayList<>();
    private int ultraStartSeconds = -1;
    private String currentScreenTitle = "";
    private boolean isChronomatronActive = false;
    private boolean isUltrasequencerActive = false;
    private int tickCounter = 0;

    public AutoExperiment() {
        super("AutoExperiment", category.Other);
        this.registerSetting(delay = new SliderSetting("Delay", 4, 2, 8, 1));
    }

    @EventHandler
    public void onPreUpdate(PreUpdateEvent event) {
        if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen) {
            String title = genericContainerScreen.getTitle().getString();
            if (!currentScreenTitle.equals(title)) {
                currentScreenTitle = title;
                resetAllSolvers();

                if (title.startsWith("Chronomatron (")) {
                    isChronomatronActive = true;
                } else if (title.startsWith("Ultrasequencer (")) {
                    isUltrasequencerActive = true;
                }
            }

            if (isChronomatronActive) {
                tickChronomatron(genericContainerScreen);
            } else if (isUltrasequencerActive) {
                tickUltrasequencer(genericContainerScreen);
            }
        } else {
            if (!currentScreenTitle.isEmpty()) {
                currentScreenTitle = "";
                resetAllSolvers();
            }
        }
    }

    private void tickChronomatron(GenericContainerScreen screen) {
        tickCounter++;
        Inventory inventory = screen.getScreenHandler().getInventory();
        chronoCurrentCycle = getChronoCycle(inventory);
        Item currentModeItem = inventory.getStack(49).getItem();

        if ((chronoCurrentCycle > 0 && currentModeItem == Items.GLOWSTONE) ||
                (chronoCurrentCycle == chronoLastCycle && currentModeItem != chronoLastModeItem)) {
            chronoStartSeconds = -1;
            if (!chronoGlintFound) {
                for (int i = 10; i < 43; i++) {
                    if (inventory.getStack(i).hasGlint()) {
                        chronoGlintFound = true;
                        chronoGlintFoundAt = i;
                        chronoClickStack.add(TERRACOTTA_TO_GLASS.get(inventory.getStack(i).getItem()));
                        break;
                    }
                }
            } else if (!inventory.getStack(chronoGlintFoundAt).hasGlint()) {
                chronoGlintFound = false;
                chronoGlintFoundAt = -1;
            }
        } else {
            if (chronoStartSeconds == -1) {
                chronoStartSeconds = inventory.getStack(49).getCount();
            }

            if (tickCounter % delay.getInput() == 0 && inventory.getStack(49).getCount() < chronoStartSeconds) {
                inputChronomatronSequence(inventory, screen);
            }
        }

        chronoLastCycle = chronoCurrentCycle;
        chronoLastModeItem = currentModeItem;
    }

    private void inputChronomatronSequence(Inventory inventory, GenericContainerScreen screen) {
        if (mc.player.currentScreenHandler.getCursorStack().getItem() == Items.AIR) {
            for (int i = 10; i < 43; i++) {
                if (!chronoClickStack.isEmpty()) {
                    if (inventory.getStack(i).getItem() == chronoClickStack.get(0)) {
                        ComponentChangesHash.ComponentHasher hasher = component -> component.hashCode();
                        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                                screen.getScreenHandler().syncId,
                                screen.getScreenHandler().getRevision(),
                                (short) i,
                                (byte) 0,
                                SlotActionType.PICKUP,
                                Int2ObjectMaps.emptyMap(),
                                ItemStackHash.fromItemStack(mc.player.currentScreenHandler.getCursorStack(), hasher)
                        ));
                        chronoClickStack.remove(0);
                        break;
                    }
                }
            }
        }
    }

    private void tickUltrasequencer(GenericContainerScreen screen) {
        tickCounter++;
        Inventory inventory = screen.getScreenHandler().getInventory();
        Item currentModeItem = inventory.getStack(49).getItem();

        if (currentModeItem == Items.GLOWSTONE) {
            ultraStartSeconds = -1;
            for (int i = 0; i < 45; i++) {
                if (!inventory.getStack(i).getItem().getName().toString().contains("pane")) {
                    if (inventory.getStack(i).getCount() == (ultraClickStack.size() + 1)) {
                        ultraClickStack.add(i);
                    }
                }
            }
        } else if (currentModeItem == Items.CLOCK) {
            if (ultraStartSeconds == -1) {
                ultraStartSeconds = inventory.getStack(49).getCount();
            }

            if (tickCounter % delay.getInput() == 0 && inventory.getStack(49).getCount() < ultraStartSeconds) {
                inputUltrasequencerSequence(inventory, screen);
            }
        }
    }

    private void inputUltrasequencerSequence(Inventory inventory, GenericContainerScreen screen) {
        if (mc.player.currentScreenHandler.getCursorStack().getItem() == Items.AIR) {
            for (int i = 0; i < 45; i++) {
                if (!ultraClickStack.isEmpty()) {
                    if (i == ultraClickStack.get(0)) {
                        ComponentChangesHash.ComponentHasher hasher = component -> component.hashCode();
                        mc.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(
                                screen.getScreenHandler().syncId,
                                screen.getScreenHandler().getRevision(),
                                (short) i,
                                (byte) 0,
                                SlotActionType.PICKUP,
                                Int2ObjectMaps.emptyMap(),
                                ItemStackHash.fromItemStack(mc.player.currentScreenHandler.getCursorStack(), hasher)
                        ));
                        ultraClickStack.remove(0);
                        break;
                    }
                }
            }
        }
    }

    private int getChronoCycle(Inventory inventory) {
        return inventory.getStack(4).getCount();
    }

    private void resetAllSolvers() {
        chronoClickStack.clear();
        chronoGlintFound = false;
        chronoGlintFoundAt = -1;
        chronoLastCycle = 0;
        chronoCurrentCycle = 0;
        chronoLastModeItem = null;
        chronoStartSeconds = -1;

        ultraClickStack.clear();
        ultraStartSeconds = -1;

        isChronomatronActive = false;
        isUltrasequencerActive = false;
    }
}