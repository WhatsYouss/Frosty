package xyz.whatsyouss.frosty.modules.impl.other;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import meteordevelopment.orbit.EventHandler;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.SliderSetting;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.world.item.Items.DYED_TERRACOTTA;
import static net.minecraft.world.item.Items.STAINED_GLASS;

public class AutoExperiment extends Module {

    private SliderSetting delay;

    private static final Object2ObjectMap<Item, Item> TERRACOTTA_TO_GLASS = Object2ObjectMaps.unmodifiable(
            new Object2ObjectArrayMap<>(
                    new Item[]{
                            DYED_TERRACOTTA.red(), DYED_TERRACOTTA.orange(), DYED_TERRACOTTA.yellow(), DYED_TERRACOTTA.lime(),
                            DYED_TERRACOTTA.green(), DYED_TERRACOTTA.cyan(), DYED_TERRACOTTA.lightBlue(), DYED_TERRACOTTA.blue(),
                            DYED_TERRACOTTA.purple(), DYED_TERRACOTTA.pink()
                    },
                    new Item[]{
                            STAINED_GLASS.red(), STAINED_GLASS.orange(), STAINED_GLASS.yellow(), STAINED_GLASS.lime(),
                            STAINED_GLASS.green(), STAINED_GLASS.cyan(), STAINED_GLASS.lightBlue(), STAINED_GLASS.blue(),
                            STAINED_GLASS.purple(), STAINED_GLASS.pink()
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
        if (mc.gui.screen() instanceof ContainerScreen genericContainerScreen) {
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

    private void tickChronomatron(AbstractContainerScreen<?> screen) {
        tickCounter++;
        AbstractContainerMenu menu = screen.getMenu();

        chronoCurrentCycle = getChronoCycle(menu);
        Item currentModeItem = menu.getSlot(49).getItem().getItem();

        if ((chronoCurrentCycle > 0 && currentModeItem == Items.GLOWSTONE) ||
                (chronoCurrentCycle == chronoLastCycle && currentModeItem != chronoLastModeItem)) {
            chronoStartSeconds = -1;
            if (!chronoGlintFound) {
                for (int i = 10; i < 43; i++) {
                    if (menu.getSlot(i).getItem().hasFoil()) {
                        chronoGlintFound = true;
                        chronoGlintFoundAt = i;
                        chronoClickStack.add(TERRACOTTA_TO_GLASS.get(menu.getSlot(i).getItem().getItem()));
                        break;
                    }
                }
            } else if (!menu.getSlot(chronoGlintFoundAt).getItem().hasFoil()) {
                chronoGlintFound = false;
                chronoGlintFoundAt = -1;
            }
        } else {
            if (chronoStartSeconds == -1) {
                chronoStartSeconds = menu.getSlot(49).getItem().getCount();
            }

            if (tickCounter % delay.getInput() == 0 &&
                    menu.getSlot(49).getItem().getCount() < chronoStartSeconds) {
                inputChronomatronSequence(menu, screen);
            }
        }

        chronoLastCycle = chronoCurrentCycle;
        chronoLastModeItem = currentModeItem;
    }

    private void inputChronomatronSequence(AbstractContainerMenu menu, AbstractContainerScreen<?> screen) {
        if (mc.player.containerMenu.getCarried().isEmpty()) {
            for (int i = 10; i < 43; i++) {
                if (!chronoClickStack.isEmpty()) {
                    if (menu.getSlot(i).getItem().getItem() == chronoClickStack.get(0)) {
                        HashedPatchMap.HashGenerator hasher = mc.getConnection().decoratedHashOpsGenenerator();

                        HashedStack carriedHashed = HashedStack.create(menu.getCarried(), hasher);

                        Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();

                        mc.getConnection().send(new ServerboundContainerClickPacket(
                                menu.containerId,
                                menu.getStateId(),
                                (short) i,
                                (byte) 0,
                                ContainerInput.PICKUP,
                                changedSlots,
                                carriedHashed
                        ));

                        chronoClickStack.remove(0);
                        break;
                    }
                }
            }
        }
    }

    private void tickUltrasequencer(AbstractContainerScreen<?> screen) {
        tickCounter++;
        AbstractContainerMenu menu = screen.getMenu();
        Item currentModeItem = menu.getSlot(49).getItem().getItem();

        if (currentModeItem == Items.GLOWSTONE) {
            ultraStartSeconds = -1;
            for (int i = 0; i < 45; i++) {
                ItemStack stack = menu.getSlot(i).getItem();
                // pane 判断：用 item registry name 包含 pane
                if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().contains("pane")) {
                    if (stack.getCount() == (ultraClickStack.size() + 1)) {
                        ultraClickStack.add(i);
                    }
                }
            }
        } else if (currentModeItem == Items.CLOCK) {
            if (ultraStartSeconds == -1) {
                ultraStartSeconds = menu.getSlot(49).getItem().getCount();
            }

            if (tickCounter % delay.getInput() == 0 &&
                    menu.getSlot(49).getItem().getCount() < ultraStartSeconds) {
                inputUltrasequencerSequence(screen);  // 只传screen，inventory从menu取
            }
        }
    }

    private void inputUltrasequencerSequence(AbstractContainerScreen<?> screen) {
        if (mc.player.containerMenu.getCarried().isEmpty()) {
            AbstractContainerMenu menu = screen.getMenu();
            for (int i = 0; i < 45; i++) {
                if (!ultraClickStack.isEmpty()) {
                    if (i == ultraClickStack.get(0)) {
                        HashedPatchMap.HashGenerator hasher = mc.getConnection().decoratedHashOpsGenenerator();

                        HashedStack carriedHashed = HashedStack.create(menu.getCarried(), hasher);

                        Int2ObjectMap<HashedStack> changedSlots = new Int2ObjectOpenHashMap<>();

                        mc.getConnection().send(new ServerboundContainerClickPacket(
                                menu.containerId,
                                menu.getStateId(),
                                (short) i,
                                (byte) 0,
                                ContainerInput.PICKUP,
                                changedSlots,
                                carriedHashed
                        ));

                        ultraClickStack.remove(0);
                        break;
                    }
                }
            }
        }
    }

    private int getChronoCycle(AbstractContainerMenu menu) {
        return menu.getSlot(4).getItem().getCount();
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