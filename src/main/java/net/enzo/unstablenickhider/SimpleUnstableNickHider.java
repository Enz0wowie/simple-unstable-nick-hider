package net.enzo.unstablenickhider;

import net.minecraft.client.MinecraftClient;
import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class SimpleUnstableNickHider implements ModInitializer {
	public static final String MOD_ID = "simple-unstable-nick-hider";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Starting Simple Unstable Nick Hider...");
		UnstableNickHiderConfig.load();
	}

	public static Pattern getUsernamePattern() {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client != null && client.getSession() != null) {
			String username = client.getSession().getUsername();
			if (username != null && !username.isEmpty()) {
				return Pattern.compile(Pattern.quote(username), Pattern.CASE_INSENSITIVE);
			}
		}
		return Pattern.compile("__NONE__");
	}
}