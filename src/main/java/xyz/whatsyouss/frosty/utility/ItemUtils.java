package xyz.whatsyouss.frosty.utility;

import com.mojang.authlib.properties.Property;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;
import java.util.Optional;

public class ItemUtils {
    public static @NotNull String getHeadTexture(@NotNull ItemStack stack) {
        if (!stack.is(Items.PLAYER_HEAD) || !stack.has(DataComponents.PROFILE)) return "";

        ResolvableProfile profile = stack.get(DataComponents.PROFILE);
        if (profile == null) return "";

        return profile.partialProfile().properties().get("textures").stream()
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
        String str = "{textures:{SKIN:{url:\"http://textures.minecraft.net/texture/"+textureUUID+"\"}}}";
        return Base64.getEncoder().encodeToString(str.getBytes());
    }
}
