package xyz.whatsyouss.frosty.modules.impl.farming;

import com.mojang.authlib.properties.Property;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

final class PestMarkerDetector {
    private static final List<String> TEXTURES = List.of(
            "70a1e836bf1968b2eaa4837227a19204f17295d870ee9e754bd6b6d60ddbed3c",
            "a24c69f96ce5562221e195c8ef2bfad71ebf7f95f5ae914a484a8d0ec21672674",
            "6403ba4027a333d8d2fd32ab59d1cfdbaa7d908d80d2381db2a69cbe65450ad8",
            "9d90e777826a52461368e26d1b2e19bfa1ba582d60248e545f4124d0f731842",
            "4b24a482a32db1ea78fb98060b0c2fa4a373cbd18a68edddeb7419455a59cda9",
            "be6baf6431a9daa2ca604d5a3c26e9a761d5952f0817174a4fe0b764616e21ff",
            "52a9fe05bc663efcd12e56a3ccc5ec035bf577b78708548b6f4ffcf1d30eccfe",
            "6545c4b34e5b5470be94de100e61f7816f81bc5a11dfdf0eccf890172da5d0a",
            "a8abb471db0ab78703011997dc8b40798a941f3a4dec3ec61cbeec2af8cffe8",
            "7a79d0fd677b54530961117ef84adc206e2cc5045c1344d61d776bf8ac2fe1ba",
            "1e04bb6367caa4e88f5fd0ee80f0745d137a604223dbbc42a16471fdf64bb83",
            "4ce69e90adf34718f313ec24d6c6135b69b3788c61849844666ccc83ca640c0b16",
            "254aff4c0b2dce3a672349cc0e99e6f3a9deebe4b3556e84611eca250a7821bf");

    private PestMarkerDetector() {}

    static boolean isPestMarker(ArmorStand marker, Iterable<Entity> entities) {
        ItemStack head = marker.getItemBySlot(EquipmentSlot.HEAD);
        if (head.isEmpty()) return false;
        ResolvableProfile profile = head.get(DataComponents.PROFILE);
        if (profile == null || profile.partialProfile().properties() == null) return false;
        var textures = profile.partialProfile().properties().get("textures");
        if (textures == null || textures.isEmpty()) return false;
        if (!head.has(DataComponents.CUSTOM_NAME)) {
            for (Property property : textures) {
                try {
                    String decoded = new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8);
                    if (TEXTURES.stream().anyMatch(decoded::contains)) return true;
                } catch (IllegalArgumentException ignored) { }
            }
        }
        for (Entity entity : entities) {
            if ((entity instanceof Bat || entity instanceof Silverfish)
                    && entity.isAlive() && entity.distanceToSqr(marker) <= 4.0) return true;
        }
        return false;
    }
}

