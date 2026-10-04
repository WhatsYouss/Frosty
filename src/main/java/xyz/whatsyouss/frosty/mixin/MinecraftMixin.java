package xyz.whatsyouss.frosty.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
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
import xyz.whatsyouss.frosty.utility.UpdateChecker;
import xyz.whatsyouss.frosty.utility.Utils;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow
    public ClientLevel level;

    @Shadow
    public LocalPlayer player;

    @Unique
    private boolean frosty$languagePromptShown;

    @Unique
    private boolean frosty$skyblockUpdateChecked;

    /**
     * Keep vanilla's held-attack path available while a module owns the
     * released cursor. The screen and attack-key checks remain in vanilla.
     */
    @ModifyExpressionValue(
            method = "handleKeybinds",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/MouseHandler;isMouseGrabbed()Z"
            )
    )
    private boolean frosty$allowHeldAttackWhileUngrabbed(boolean mouseGrabbed) {
        Minecraft minecraft = (Minecraft) (Object) this;
        return mouseGrabbed
                || (frosty$allowsBackgroundInput() && minecraft.options.keyAttack.isDown());
    }

    /** Keep ticking without opening the pause screen during background input. */
    @ModifyExpressionValue(
            method = "pauseIfInactive",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/Window;isFocused()Z"
            )
    )
    private boolean frosty$keepRunningWhileUnfocused(boolean focused) {
        return focused || frosty$allowsBackgroundInput();
    }

    @Unique
    private boolean frosty$allowsBackgroundInput() {
        return level != null
                && player != null
                && ((ModuleManager.ungrabMouse != null && ModuleManager.ungrabMouse.isEnabled())
                || (ModuleManager.farmingMacro != null && ModuleManager.farmingMacro.isControllingMouse()));
    }

    /** Start the tracker capture only when Minecraft actually performs the vacuum's attack swing. */
    @Inject(method = "startAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"))
    private void frosty$beginPestTrackerOnAttack(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.pestCleaner != null && ModuleManager.pestCleaner.isEnabled()) {
            ModuleManager.pestCleaner.onTrackerAttack();
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void onInit(CallbackInfo info) {
        ConfigManager.loadConfig();
        ConfigManager.loadServerConfig();
        UpdateChecker.requestStartupCheck();
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
        if (Utils.nullCheck()) {
            if (!frosty$skyblockUpdateChecked) {
                String sidebar = Utils.stripColor(Utils.getScoreboardSidebarLines().toString().toLowerCase());
                if (sidebar.contains("skyblock")) {
                    frosty$skyblockUpdateChecked = true;
                    UpdateChecker.notifyIfAvailable();
                    if (!ModuleManager.update.ignoreNotifications.isToggled()) {
                        Utils.addChatMessage("Ignore this notification in Module-Update", "在版本更新模块中忽略该提示");
                    }
                }
            }
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
