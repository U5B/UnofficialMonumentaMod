package ch.njol.unofficialmonumentamod;

import ch.njol.unofficialmonumentamod.hud.HudEditScreen;
import ch.njol.unofficialmonumentamod.options.Options;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class UnofficialMonumentaModClient implements ClientModInitializer {

	// TODO:
	// sage's insight has no ClassAbility, but has stacks
	// spellshock however has a ClassAbility, but doesn't really need to be displayed...

	public static final String MOD_IDENTIFIER = "unofficial-monumenta-mod";

	public static final String OPTIONS_FILE_NAME = "unofficial-monumenta-mod.json";

	public static Options options = new Options();

	public static final AbilityHandler abilityHandler = new AbilityHandler();

	public static KeyBinding openHudEditScreenKeybinding;

	@Override
	public void onInitializeClient() {

		FabricModelPredicateProviderRegistry.register(new Identifier("on_head"),
			(itemStack, clientWorld, livingEntity, seed) -> livingEntity != null && itemStack == livingEntity.getEquippedStack(EquipmentSlot.HEAD) ? 1 : 0);

		try {
			options = readJsonFile(Options.class, OPTIONS_FILE_NAME);
		} catch (FileNotFoundException e) {
			// Config file doesn't exist, so use default config (and write config file).
			writeJsonFile(options, OPTIONS_FILE_NAME);
		} catch (IOException | JsonParseException e) {
			// Any issue with the config file silently reverts to the default config
			e.printStackTrace();
		}

		openHudEditScreenKeybinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
			MOD_IDENTIFIER + ".keybindings.key.editHud",
			InputUtil.Type.KEYSYM,
			GLFW.GLFW_KEY_UNKNOWN,
			MOD_IDENTIFIER + ".keybindings.category"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			abilityHandler.tick();

			if (openHudEditScreenKeybinding.wasPressed() && !client.options.hudHidden) {
				if (client.currentScreen instanceof HudEditScreen) {
					client.currentScreen.close();
				} else {
					client.setScreen(new HudEditScreen(client.currentScreen));
				}
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(ChannelHandler.CHANNEL_ID, new ChannelHandler());
	}

	public static void onDisconnect() {
		abilityHandler.onDisconnect();
	}

	private static <T> T readJsonFile(Class<T> c, String filePath) throws IOException, JsonParseException {
		try (FileReader reader = new FileReader(FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile())) {
			return new GsonBuilder().create().fromJson(reader, c);
		}
	}

	private static void writeJsonFile(Object o, String filePath) {
		try (FileWriter writer = new FileWriter((FabricLoader.getInstance().getConfigDir().resolve(filePath).toFile()))) {
			writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(o));
		} catch (IOException e) {
			// Silently ignore save errors
			e.printStackTrace();
		}
	}

	public static void saveConfig() {
		MinecraftClient.getInstance().execute(() -> {
			writeJsonFile(options, OPTIONS_FILE_NAME);
		});
	}

}
