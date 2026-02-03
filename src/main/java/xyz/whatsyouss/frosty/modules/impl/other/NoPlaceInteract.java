package xyz.whatsyouss.frosty.modules.impl.other;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.settings.impl.ButtonSetting;

import java.util.Arrays;
import java.util.List;

public class NoPlaceInteract extends Module {

    public ButtonSetting swing;

    private static final List<String> BLOCK_COLORS = Arrays.asList("white", "green", "blue", "dark_purple", "gold", "light_purple", "aqua");

    private static final List<Item> PLACEABLE_ITEMS = Arrays.asList(
            Items.PLAYER_HEAD,
            Items.SKELETON_SKULL,
            Items.WITHER_SKELETON_SKULL,
            Items.ZOMBIE_HEAD,
            Items.CREEPER_HEAD,
            Items.PIGLIN_HEAD,
            Items.ARMOR_STAND,
            Items.COBWEB
    );

    public NoPlaceInteract() {
        super("NoPlaceInteract", category.Other);

        this.registerSetting(swing = new ButtonSetting("Swing", false));
    }

    @Override
    public String getDesc() {
        return "Stop place placeable skyblock items clientside";
    }

    public boolean isPlaceable(Item item) {
        return PLACEABLE_ITEMS.contains(item);
    }
}
