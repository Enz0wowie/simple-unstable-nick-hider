package net.enzo.unstablenickhider;

import net.fabricmc.api.ClientModInitializer;
import net.enzo.unstablenickhider.client.ClientKeybinds;
import net.enzo.unstablenickhider.config.UnstableNickHiderConfig;

public class SimpleUnstableNickHiderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        UnstableNickHiderConfig.load();
        ClientKeybinds.register();
    }
}
