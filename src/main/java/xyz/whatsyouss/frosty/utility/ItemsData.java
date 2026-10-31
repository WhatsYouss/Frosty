package xyz.whatsyouss.frosty.utility;

import com.google.common.collect.ImmutableMultimap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import xyz.whatsyouss.frosty.modules.impl.render.AntiTexture;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class ItemsData {
    public static final ItemsData INSTANCE = new ItemsData();

    private Map<String, Map<String, String>> itemIds = new HashMap<>();
    private final Map<String, GameProfile> cachedItems = new HashMap<>();

    private ItemsData() {
        try (InputStream is = getClass().getResourceAsStream("/assets/frosty/ItemDataSet.json")) {
            if (is != null) {
                itemIds = new Gson().fromJson(new InputStreamReader(is),
                        new TypeToken<Map<String, Map<String, String>>>(){}.getType());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String skyblockId(ItemStack itemStack) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag extraAttributes = customData.copyTag();

        String sbId = extraAttributes.getString("id").orElse(null);
        boolean isQuiver = extraAttributes.getString("quiver_arrow").isPresent();

        if (isQuiver && sbId == null) return "NRP$QUIVER";
        return sbId;
    }

    public String modelId(ItemStack itemStack) {
        String id = skyblockId(itemStack);
        if (id == null || !itemIds.containsKey(id)) return null;
        return itemIds.get(id).get("model");
    }

    public GameProfile gameProfile(String sbId) {
        if (AntiTexture.whitelistedItems.contains(sbId)) return null;
        return cachedItems.get(sbId);
    }

    public GameProfile gameProfile(ItemStack itemStack) {
        String id = skyblockId(itemStack);
        return id != null ? gameProfile(id) : null;
    }

    public Identifier fromModelId(ItemStack itemStack, Identifier modelId) {
        if (modelId == null) return null;
        if (!modelId.getNamespace().startsWith("hypixel_skyblock")) return modelId;

        String sbId = skyblockId(itemStack);
        if (sbId == null || AntiTexture.whitelistedItems.contains(sbId)) return modelId;

        if (itemStack.is(Items.PAPER)) {
            Map<String, String> cache = itemIds.get(sbId);
            if (cache == null) return modelId;

            String id = modelId(itemStack);
            if (id == null) return modelId;

            if ("minecraft:player_head".equals(id)) {
                String textureValue = cache.get("value");
                if (textureValue != null && !cachedItems.containsKey(sbId)) {
                    PropertyMap props = new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", textureValue)));
                    cachedItems.put(sbId, new GameProfile(UUID.randomUUID(), "nrp$fakeItem", props));
                }
            }
            return Identifier.parse(id);
        }

        return BuiltInRegistries.ITEM.getKey(itemStack.getItem());
    }
}