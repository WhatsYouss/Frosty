package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.whatsyouss.frosty.Frosty;
import xyz.whatsyouss.frosty.config.ConfigManager;
import xyz.whatsyouss.frosty.events.impl.PostUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.gui.LanguageSelectScreen;
import xyz.whatsyouss.frosty.modules.ModuleManager;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public ClientLevel level;

    @Shadow
    public LocalPlayer player;

    @Unique
    private boolean frosty$languagePromptShown;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        ConfigManager.loadConfig();
        ConfigManager.loadServerConfig();
    }

    @Inject(method = "createTitle", at = @At("RETURN"), cancellable = true)
    private void onGetWindowTitle(CallbackInfoReturnable<String> cir) {
        if (ModuleManager.title.isEnabled()) {
            String original = ModuleManager.title.keepOriginal.isToggled() ? cir.getReturnValue() + " " : "";
            cir.setReturnValue(original + "Frosty");
        }
    }
    @Inject(at = @At("HEAD"), method = "tick()V")
    public void onPreTick(CallbackInfo info) {
        if (level != null && player != null) {
            Frosty.EVENT_BUS.post(new PreUpdateEvent());
        }
    }

    @Inject(at = @At("TAIL"), method = "tick()V")
    public void onPostTick(CallbackInfo info) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (!frosty$languagePromptShown
                && ConfigManager.shouldShowLanguagePrompt()
                && minecraft.gui.screen() instanceof TitleScreen titleScreen) {
            frosty$languagePromptShown = true;
            minecraft.gui.setScreen(new LanguageSelectScreen(titleScreen));
            return;
        }

        if (level != null && player != null) {
            Frosty.EVENT_BUS.post(new PostUpdateEvent());
        }
    }
}
