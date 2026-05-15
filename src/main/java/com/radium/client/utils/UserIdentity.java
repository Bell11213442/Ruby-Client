package com.radium.client.utils;
// radium client

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class UserIdentity {
    private static final String FILE_NAME = "radium_id.txt";
    private static UUID cachedId;
    private static String cachedDiscordId;

    public static String getOrCreateId() {
        if (cachedId != null) return cachedId.toString();
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("radium");
        Path idFile = configDir.resolve(FILE_NAME);

        try {
            if (!Files.exists(configDir)) Files.createDirectories(configDir);
            if (Files.exists(idFile)) {
                cachedId = UUID.fromString(Files.readString(idFile).trim());
            } else {
                cachedId = UUID.randomUUID();
                Files.writeString(idFile, cachedId.toString());
            }
        } catch (Exception e) {
            cachedId = UUID.randomUUID();
        }
        return cachedId.toString();
    }


    public static String getDiscordId() {

        if (cachedDiscordId != null) {
            return cachedDiscordId;
        }


        cachedDiscordId = "123456789012345678";

        return cachedDiscordId;
    }
}

