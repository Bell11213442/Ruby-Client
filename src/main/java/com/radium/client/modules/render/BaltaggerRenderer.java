package com.radium.client.modules.render;
// radium client

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.radium.client.client.RadiumClient;
import com.radium.client.modules.donut.Baltagger;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BaltaggerRenderer implements WorldRenderEvents.AfterEntities {
    private static final BaltaggerRenderer INSTANCE = new BaltaggerRenderer();
    private final Map<String, Long> moneyCache = new ConcurrentHashMap<>();
    private final Set<String> pendingRequests = ConcurrentHashMap.newKeySet();
    private final Set<String> failedRequests = ConcurrentHashMap.newKeySet();
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    private BaltaggerRenderer() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public static BaltaggerRenderer getInstance() {
        return INSTANCE;
    }

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(INSTANCE);
    }

    public void onTick(MinecraftClient client) {
        if (client.world == null || client.player == null) return;

        Baltagger mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(Baltagger.class)
                : null;

        if (mod == null || !mod.isEnabled()) return;
        if (mod.getApiKey().isEmpty()) return;

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == null) continue;
            if (!mod.shouldShowSelf() && player == client.player) continue;

            String playerName = player.getGameProfile().getName();
            if (playerName == null || playerName.isEmpty()) continue;

            if (!moneyCache.containsKey(playerName) &&
                    !pendingRequests.contains(playerName) &&
                    !failedRequests.contains(playerName)) {
                fetchPlayerMoney(playerName, mod);
            }
        }
    }

    public boolean shouldCancelNametagRendering(Entity entity) {
        return false;
    }

    @Override
    public void afterEntities(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null || client.player == null) return;

        Baltagger mod = RadiumClient.getModuleManager() != null
                ? RadiumClient.getModuleManager().getModule(Baltagger.class)
                : null;
        if (mod == null || !mod.isEnabled()) return;
        if (mod.getApiKey().isEmpty()) return;

        var matrices = context.matrixStack();
        var camera = context.camera();
        var vertexConsumers = context.consumers();
        float tickDelta = context.tickCounter().getTickDelta(true);
        double camX = camera.getPos().x;
        double camY = camera.getPos().y;
        double camZ = camera.getPos().z;

        TextRenderer tr = client.textRenderer;

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player == null) continue;
            if (player == client.player && !mod.shouldShowSelf()) continue;

            String name = player.getGameProfile().getName();
            if (name == null || name.isEmpty()) continue;

            Long money = getMoney(name);
            if (money == null) continue;

            String moneyText = formatMoney(money);
            int moneyColor = mod.getMoneyColor() | 0xFF000000;

            double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()) - camX;
            double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) - camY + player.getHeight() + 0.6;
            double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()) - camZ;

            matrices.push();
            matrices.translate(x, y, z);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.scale(-0.025f, -0.025f, 0.025f);

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            int hearts = Math.round(player.getHealth() + player.getAbsorptionAmount());
            int heartColor = 0xFFFF0000;

            String fullText = hearts + " ❤" + moneyText;
            int totalWidth = tr.getWidth(fullText);
            float startX = -totalWidth / 2f;
            int overlayY = -tr.fontHeight;

            int currentX = 0;
            String heartsText = hearts + "";
            tr.draw(net.minecraft.text.Text.literal(heartsText), startX + currentX, overlayY, 0xFFFFFFFF, false, matrix, vertexConsumers,
                    net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
            currentX += tr.getWidth(heartsText);

            tr.draw(net.minecraft.text.Text.literal(" ❤"), startX + currentX, overlayY, heartColor, false, matrix, vertexConsumers,
                    net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);
            currentX += tr.getWidth(" ❤");

            tr.draw(net.minecraft.text.Text.literal(moneyText), startX + currentX, overlayY, moneyColor, false, matrix, vertexConsumers,
                    net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0, 15728880);

            matrices.pop();
        }
    }

    private void fetchPlayerMoney(String playerName, Baltagger mod) {
        if (mod.getApiKey().isEmpty()) return;

        pendingRequests.add(playerName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.donutsmp.net/v1/stats/" + playerName))
                .header("accept", "application/json")
                .header("Authorization", mod.getApiKey())
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    pendingRequests.remove(playerName);

                    if (response.statusCode() == 200) {
                        try {
                            JsonObject json = gson.fromJson(response.body(), JsonObject.class);
                            JsonObject result = json.getAsJsonObject("result");
                            String moneyStr = result.get("money").getAsString();
                            double moneyDouble = Double.parseDouble(moneyStr);
                            long money = (long) moneyDouble;

                            moneyCache.put(playerName, money);

                        } catch (Exception e) {
                            failedRequests.add(playerName);
                        }
                    } else {
                        failedRequests.add(playerName);
                    }
                })
                .exceptionally(e -> {
                    pendingRequests.remove(playerName);
                    failedRequests.add(playerName);
                    return null;
                });
    }


    public void clearCache() {
        moneyCache.clear();
        pendingRequests.clear();
        failedRequests.clear();
    }

    public Long getMoney(String playerName) {
        return moneyCache.get(playerName);
    }

    public String formatMoney(long money) {
        if (money >= 1_000_000_000L) {
            double value = money / 1_000_000_000.0;
            return formatCompact(value, "B");
        } else if (money >= 1_000_000L) {
            double value = money / 1_000_000.0;
            return formatCompact(value, "M");
        } else if (money >= 1_000L) {
            double value = money / 1_000.0;
            return formatCompact(value, "k");
        } else {
            return "$" + money;
        }
    }

    private String formatCompact(double value, String unit) {
        String s = String.format("%.1f", value);
        if (s.endsWith(".0")) {
            s = s.substring(0, s.length() - 2);
        }
        return "$" + s + unit;
    }
}
