package xyz.whatsyouss.frosty.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class FrostyMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("frosty");
    private static final String SUPPORTED_SODIUM_VERSION = "0.9.1+mc26.2";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.endsWith(".SodiumBlockRendererXrayMixin")) {
            return true;
        }

        return FabricLoader.getInstance().getModContainer("sodium")
                .map(container -> {
                    String version = container.getMetadata().getVersion().getFriendlyString();
                    if (!SUPPORTED_SODIUM_VERSION.equals(version)) {
                        LOGGER.warn("Xray's Sodium renderer hook supports {}, but found {}; disabling the hook",
                                SUPPORTED_SODIUM_VERSION, version);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
                         String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
                          String mixinClassName, IMixinInfo mixinInfo) {
    }
}
