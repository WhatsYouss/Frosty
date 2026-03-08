package xyz.whatsyouss.frosty;

import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.IEventBus;
import net.fabricmc.api.ModInitializer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.whatsyouss.frosty.commands.CommandManager;
import xyz.whatsyouss.frosty.config.ConfigManager;
import xyz.whatsyouss.frosty.events.impl.PreUpdateEvent;
import xyz.whatsyouss.frosty.events.impl.SettingUpdateEvent;
import xyz.whatsyouss.frosty.gui.ClickGui;
import xyz.whatsyouss.frosty.modules.Module;
import xyz.whatsyouss.frosty.modules.ModuleManager;
import xyz.whatsyouss.frosty.utility.Rotations;
import xyz.whatsyouss.frosty.utility.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

public class Frosty implements ModInitializer {
	public static final String MOD_ID = "frosty";
	public static MinecraftClient mc;
	public static final IEventBus EVENT_BUS = new EventBus();
	boolean applied = false;
	public static boolean modulesInitialized = false;

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public ModuleManager moduleManager;
	public CommandManager commandManager;

	public Frosty() {
		moduleManager = new ModuleManager();
		commandManager = new CommandManager();
	}

	@EventHandler
	public void onPreUpdate(PreUpdateEvent e) {
		if (!modulesInitialized) {
			ConfigManager.loadConfig();
			ConfigManager.loadServerConfig();
			modulesInitialized = true;
		}
		if (Utils.nullCheck()) {
			for (Module module : ModuleManager.getModules()) {
				if (mc.currentScreen == null) {
					module.onKeyBind();
				} else if (mc.currentScreen instanceof ClickGui) {
					module.guiUpdate();
				}
				if (module.isEnabled()) {
					module.onUpdate();
				}
			}
		}
	}

	@EventHandler
	public void onSettingUpdate(SettingUpdateEvent event) {
		ConfigManager.saveConfig();
		ConfigManager.saveServerConfig();
	}

	@Override
	public void onInitialize() {
		mc = MinecraftClient.getInstance();
		moduleManager.register();
		commandManager.register();
		EVENT_BUS.registerLambdaFactory("xyz.whatsyouss.frosty", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
		EVENT_BUS.subscribe(this);
		EVENT_BUS.subscribe(new Rotations());
		ConfigManager.createConfigDir();
		modulesInitialized = false;
	}

	public static void registerCapeTexture(Identifier id, File file) {
		try {
			if (!file.exists() || !file.isFile()) {
				return;
			}

			NativeImage image = NativeImage.read(new FileInputStream(file));
			int srcWidth = image.getWidth();
			int srcHeight = image.getHeight();
			int targetWidth = 64;
			int targetHeight = 32;
			if (srcWidth != targetWidth || srcHeight != targetHeight) {
				int imageWidth = srcWidth;
				int imageHeight = srcHeight;
				while (imageWidth < srcWidth || imageHeight < srcHeight) {
					imageWidth *= 2;
					imageHeight *= 2;
				}
				if (imageWidth < srcWidth * 2) {
					imageWidth = srcWidth;
					imageHeight = srcHeight;
				}
				NativeImage resizedImage = new NativeImage(imageWidth, imageHeight, true);
				for (int x = 0; x < imageWidth; x++) {
					for (int y = 0; y < imageHeight; y++) {
						int srcX = (int) ((float) x / imageWidth * srcWidth);
						int srcY = (int) ((float) y / imageHeight * srcHeight);
						srcX = Math.min(srcX, srcWidth - 1);
						srcY = Math.min(srcY, srcHeight - 1);
						resizedImage.setColor(x, y, image.getColor(srcX, srcY));
					}
				}
				image.close();
				image = resizedImage;
			}

			Supplier<String> nameSupplier = id::toString;
			mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(nameSupplier, image));
		} catch (Exception e) {
		}
	}
}