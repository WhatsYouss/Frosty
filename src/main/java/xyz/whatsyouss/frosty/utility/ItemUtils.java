package xyz.whatsyouss.frosty.utility;

import com.mojang.authlib.properties.Property;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Optional;

public class ItemUtils {
    public static @NotNull String getHeadTexture(@NotNull ItemStack stack) {
        if (!stack.isOf(Items.PLAYER_HEAD) || !stack.contains(DataComponentTypes.PROFILE)) return "";

        ProfileComponent profile = stack.get(DataComponentTypes.PROFILE);
        if (profile == null) return "";

        return profile.getGameProfile().properties().get("textures").stream()
                .map(Property::value)
                .findFirst()
                .orElse("");
    }

    public static @NotNull Optional<String> getHeadTextureOptional(ItemStack stack) {
        String texture = getHeadTexture(stack);
        if (texture.isBlank()) return Optional.empty();
        return Optional.of(texture);
    }

    public static @NotNull String toTextureBase64(String textureUUID) {
        //noinspection HttpUrlsUsage
        String str = "{textures:{SKIN:{url:\"http://textures.minecraft.net/texture/"+textureUUID+"\"}}}";
        return Base64.getEncoder().encodeToString(str.getBytes());
    }
}
