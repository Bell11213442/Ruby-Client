package com.radium.client.modules.misc;
// radium client

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SkinChanger extends Module {
    private final StringSetting targetUsername = new StringSetting("Target Username", "DrDonutt");

    private Identifier cachedSkinTexture = null;
    private String cachedUsername = "";

    public SkinChanger() {
        super("SkinChanger", "Changes your skin client-side to another player's skin", Category.MISC);
        this.addSettings(targetUsername);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        cachedSkinTexture = null;
        cachedUsername = "";
        fetchAndCacheSkin();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cachedSkinTexture = null;
        cachedUsername = "";
    }

    public String getTargetUsername() {
        return targetUsername.getValue();
    }

    public boolean isProtecting() {
        return this.isEnabled();
    }

    public Identifier getCachedSkinTexture() {
        String currentUsername = targetUsername.getValue();
        if (cachedSkinTexture == null || !cachedUsername.equals(currentUsername)) {
            fetchAndCacheSkin();
        }
        return cachedSkinTexture;
    }

    private void fetchAndCacheSkin() {
        String username = targetUsername.getValue();
        if (username == null || username.isEmpty()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                UUID uuid = fetchUUID(username);
                if (uuid == null) {
                    return;
                }

                String skinUrl = fetchSkinUrl(uuid);
                if (skinUrl == null) {
                    return;
                }

                Identifier texture = downloadAndRegisterSkin(skinUrl, username);
                if (texture != null) {
                    cachedSkinTexture = texture;
                    cachedUsername = username;
                }
            } catch (Exception e) {
            }
        });
    }

    private UUID fetchUUID(String username) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                return null;
            }

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());
            inputStream.close();

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String uuidString = json.get("id").getAsString();

            return UUID.fromString(
                    uuidString.replaceFirst(
                            "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                            "$1-$2-$3-$4-$5"
                    )
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String fetchSkinUrl(UUID uuid) {
        try {
            String uuidString = uuid.toString().replace("-", "");
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuidString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                return null;
            }

            InputStream inputStream = connection.getInputStream();
            String response = new String(inputStream.readAllBytes());
            inputStream.close();

            JsonObject json = JsonParser.parseString(response).getAsJsonObject();
            String value = json.getAsJsonArray("properties")
                    .get(0).getAsJsonObject()
                    .get("value").getAsString();

            String decodedValue = new String(Base64.getDecoder().decode(value));
            JsonObject texturesJson = JsonParser.parseString(decodedValue).getAsJsonObject();

            return texturesJson.getAsJsonObject("textures")
                    .getAsJsonObject("SKIN")
                    .get("url").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    private Identifier downloadAndRegisterSkin(String skinUrl, String username) {
        try {
            URL url = new URL(skinUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (connection.getResponseCode() != 200) {
                return null;
            }

            InputStream inputStream = connection.getInputStream();
            NativeImage image = NativeImage.read(inputStream);
            inputStream.close();

            Identifier textureId = Identifier.of("radium", "skins/" + username.toLowerCase());

            mc.execute(() -> {
                mc.getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(image));
            });

            return textureId;
        } catch (Exception e) {
            return null;
        }
    }
}