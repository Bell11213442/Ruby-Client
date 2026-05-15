package com.radium.client.utils;
// radium client

import com.radium.client.client.RadiumClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class WebhookUtils {
    private static final String WEBHOOK_AVATAR = "https://i.imgur.com/0K2q9D9.png";
    private static final String WEBHOOK_NAME = "Radium Alert";
    private static final String FOOTER_TEXT = "Radium Client";
    private static final int EMBED_COLOR = 16711680;

    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String webhookUrl;
    private final List<Field> fields = new ArrayList<>();
    private String title = "";
    private String description = "";
    private String selfPingId = "";
    private boolean includeCoords = false;
    private boolean includeUsername = false;
    private String username = "";
    private boolean includeServer = false;
    private boolean includeTime = false;
    private File screenshotFile = null;

    public WebhookUtils(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public WebhookUtils setTitle(String title) {
        this.title = title;
        return this;
    }

    public WebhookUtils setDescription(String description) {
        this.description = description;
        return this;
    }

    public WebhookUtils setSelfPing(String discordId) {
        this.selfPingId = discordId;
        return this;
    }

    public WebhookUtils addCoords() {
        this.includeCoords = true;
        return this;
    }

    public WebhookUtils addCoords(BlockPos pos) {
        this.includeCoords = true;
        if (pos != null) {
            fields.add(new Field("Coordinates", String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()), true));
        }
        return this;
    }

    public WebhookUtils addUsername() {
        this.includeUsername = true;
        MinecraftClient mc = RadiumClient.mc;
        if (mc != null && mc.player != null) {
            this.username = mc.player.getName().getString();
        }
        return this;
    }

    public WebhookUtils addUsername(String username) {
        this.includeUsername = true;
        this.username = username;
        return this;
    }

    public WebhookUtils addField(String name, String value, boolean inline) {
        fields.add(new Field(name, value, inline));
        return this;
    }

    public WebhookUtils addServer() {
        this.includeServer = true;
        return this;
    }

    public WebhookUtils addTime() {
        this.includeTime = true;
        return this;
    }

    public WebhookUtils setScreenshot(File screenshotFile) {
        this.screenshotFile = screenshotFile;
        return this;
    }

    public void send() {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {

            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                MinecraftClient mc = RadiumClient.mc;

                String messageContent = "";
                if (!selfPingId.trim().isEmpty()) {
                    messageContent = String.format("<@%s>", selfPingId.trim());
                }

                if (includeCoords && mc != null && mc.player != null) {
                    BlockPos pos = mc.player.getBlockPos();
                    if (fields.stream().noneMatch(f -> f.name.equals("Coordinates"))) {
                        fields.add(new Field("Coordinates", String.format("X: %d, Y: %d, Z: %d", pos.getX(), pos.getY(), pos.getZ()), true));
                    }
                }

                if (includeUsername && username.isEmpty() && mc != null && mc.player != null) {
                    username = mc.player.getName().getString();
                }

                if (includeServer) {
                    String serverInfo = mc != null && mc.getCurrentServerEntry() != null
                            ? mc.getCurrentServerEntry().address : "Unknown Server";
                    if (fields.stream().noneMatch(f -> f.name.equals("Server"))) {
                        fields.add(new Field("Server", serverInfo, true));
                    }
                }

                if (includeTime) {
                    long timestampSeconds = System.currentTimeMillis() / 1000;
                    if (fields.stream().noneMatch(f -> f.name.equals("Time"))) {
                        fields.add(new Field("Time", "<t:" + timestampSeconds + ":R>", true));
                    }
                }

                StringBuilder fieldsJson = new StringBuilder();
                if (includeUsername && !username.isEmpty()) {
                    fieldsJson.append(String.format("""
                            {
                                "name": "Username",
                                "value": "%s",
                                "inline": true
                            }""", escapeJson(username)));
                }

                for (int i = 0; i < fields.size(); i++) {
                    if (i > 0 || includeUsername) {
                        fieldsJson.append(",");
                    }
                    Field field = fields.get(i);
                    fieldsJson.append(String.format("""
                            {
                                "name": "%s",
                                "value": "%s",
                                "inline": %s
                            }""", escapeJson(field.name), escapeJson(field.value), field.inline));
                }

                String thumbnailUrl = "";
                if (includeUsername && !username.isEmpty()) {
                    thumbnailUrl = String.format("https://vzge.me/bust/%s.png?y=-40", username);
                }

                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

                String imageJson = "";
                if (screenshotFile != null && screenshotFile.exists()) {
                    imageJson = ",\n                            \"image\": {\n                                \"url\": \"attachment://screenshot.png\"\n                            }";
                }

                String jsonPayload = String.format("""
                                {
                                    "content": "%s",
                                    "username": "%s",
                                    "avatar_url": "%s",
                                    "embeds": [{
                                        "title": "%s",
                                        "description": "%s",
                                        "color": %d,
                                        "fields": [%s],
                                        "footer": {
                                            "text": "%s"
                                        },
                                        "timestamp": "%sZ"%s%s
                                    }]
                                }""",
                        escapeJson(messageContent),
                        escapeJson(WEBHOOK_NAME),
                        WEBHOOK_AVATAR,
                        escapeJson(title),
                        escapeJson(description),
                        EMBED_COLOR,
                        fieldsJson,
                        escapeJson(FOOTER_TEXT),
                        timestamp,
                        !thumbnailUrl.isEmpty() ? String.format(",\n                            \"thumbnail\": {\n                                \"url\": \"%s\"\n                            }", thumbnailUrl) : "",
                        imageJson);

                if (screenshotFile != null && screenshotFile.exists()) {
                    sendWithScreenshot(jsonPayload, screenshotFile);
                } else {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(webhookUrl))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                            .timeout(Duration.ofSeconds(30))
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 204 || response.statusCode() == 200) {

                    } else {

                    }
                }
            } catch (IOException | InterruptedException e) {

            }
        });
    }

    private void sendWithScreenshot(String jsonPayload, File screenshotFile) {
        try {
            String boundary = "----Boundary" + System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URI(webhookUrl).toURL().openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            conn.setRequestProperty("User-Agent", "Radium-Client");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            try (OutputStream out = conn.getOutputStream()) {
                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write("Content-Disposition: form-data; name=\"payload_json\"\r\n".getBytes(StandardCharsets.UTF_8));
                out.write("Content-Type: application/json\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                out.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));

                out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
                out.write("Content-Disposition: form-data; name=\"file1\"; filename=\"screenshot.png\"\r\n".getBytes(StandardCharsets.UTF_8));
                out.write("Content-Type: image/png\r\n\r\n".getBytes(StandardCharsets.UTF_8));
                try (FileInputStream fis = new FileInputStream(screenshotFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                out.write("\r\n".getBytes(StandardCharsets.UTF_8));
                out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 204 || responseCode == 200) {

            } else {

            }
            conn.disconnect();

            Files.deleteIfExists(screenshotFile.toPath());
        } catch (Exception e) {

        }
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public static class Field {
        public String name;
        public String value;
        public boolean inline;

        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }
    }
}
