package xyz.whatsyouss.frosty.mixin;

import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;
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
    private DirectionalLayoutWidget grid;
    @Unique private ButtonWidget reconnectBtn;
    @Unique private ButtonWidget autoReconnectToggleBtn;
    @Unique private double time = ModuleManager.autoReconnect.delay.getInput() / 50;

    protected DisconnectedScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V", shift = At.Shift.BEFORE))
    private void addButtons(CallbackInfo ci) {

        if (ModuleManager.autoReconnect.lastServerConnection != null) {
            reconnectBtn = new ButtonWidget.Builder(Text.literal(getText()), button -> tryConnecting()).width(200).build();
            grid.add(reconnectBtn);

            autoReconnectToggleBtn = new ButtonWidget.Builder(Text.literal(getAutoReconnectText()), button -> {
                ModuleManager.autoReconnect.toggle();
                reconnectBtn.setMessage(Text.literal(getText()));
                autoReconnectToggleBtn.setMessage(Text.literal(getAutoReconnectText()));
                time = ModuleManager.autoReconnect.delay.getInput() / 50;
            }).width(200).build();
            grid.add(autoReconnectToggleBtn);
        }
    }

    @Override
    public void tick() {
        if (!ModuleManager.autoReconnect.isEnabled() || ModuleManager.autoReconnect.lastServerConnection == null) return;

        if (time <= 0) {
            tryConnecting();
        } else {
            time--;
            if (reconnectBtn != null) reconnectBtn.setMessage(Text.literal(getText()));
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
        ConnectScreen.connect(new TitleScreen(), mc, lastServer.left(), lastServer.right(), false, null);
    }
}
