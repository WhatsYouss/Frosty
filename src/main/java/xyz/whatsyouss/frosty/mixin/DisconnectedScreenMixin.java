package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.whatsyouss.frosty.modules.ModuleManager;

import static xyz.whatsyouss.frosty.Frosty.mc;

@Mixin(DisconnectedScreen.class)
public abstract class DisconnectedScreenMixin extends Screen {
    @Shadow
    @Final
    private LinearLayout layout;
    @Unique private Button reconnectBtn;
    @Unique private Button autoReconnectToggleBtn;
    @Unique private double time = ModuleManager.autoReconnect.delay.getInput() / 50;

    protected DisconnectedScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;arrangeElements()V", shift = At.Shift.BEFORE))
    private void addButtons(CallbackInfo ci) {

        if (ModuleManager.autoReconnect.lastServerConnection != null) {
            reconnectBtn = new Button.Builder(Component.literal(getText()), button -> tryConnecting()).width(200).build();
            layout.addChild(reconnectBtn);

            autoReconnectToggleBtn = new Button.Builder(Component.literal(getAutoReconnectText()), button -> {
                ModuleManager.autoReconnect.toggle();
                reconnectBtn.setMessage(Component.literal(getText()));
                autoReconnectToggleBtn.setMessage(Component.literal(getAutoReconnectText()));
                time = ModuleManager.autoReconnect.delay.getInput() / 50;
            }).width(200).build();
            layout.addChild(autoReconnectToggleBtn);
        }
    }

    @Override
    public void tick() {
        if (!ModuleManager.autoReconnect.isEnabled() || ModuleManager.autoReconnect.lastServerConnection == null) return;

        if (time <= 0) {
            tryConnecting();
        } else {
            time--;
            if (reconnectBtn != null) reconnectBtn.setMessage(Component.literal(getText()));
        }
    }

    @Unique
    private String getText() {
        String reconnectText = "Reconnect";
        if (ModuleManager.autoReconnect.isEnabled()) reconnectText += " " + String.format("(%.1fms)", time * 50);
        return reconnectText;
    }

    @Unique
    private String getAutoReconnectText() {
        return "Auto Reconnect: " + (ModuleManager.autoReconnect.isEnabled() ? "§aEnabled" : "§cDisabled");
    }

    @Unique
    private void tryConnecting() {
        var lastServer = ModuleManager.autoReconnect.lastServerConnection;
        ConnectScreen.startConnecting(new TitleScreen(), mc, lastServer.left(), lastServer.right(), false, null);
    }
}
