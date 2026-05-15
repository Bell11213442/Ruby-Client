package com.radium.client.client;
// radium client

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public final class Main implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Wait for client to be fully initialized before creating RadiumClient
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            new RadiumClient();
        });
    }
}
