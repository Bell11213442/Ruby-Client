package com.radium.client.utils.network;
// radium client

import com.google.gson.Gson;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Http {
    private static final Gson GSON = new Gson();

    public static Request get(String url) {
        return new Request("GET", url);
    }

    public static Request post(String url) {
        return new Request("POST", url);
    }

    public static class Request {
        private final String method;
        private final String url;
        private String body;
        private String contentType;
        private String authorization;

        private Request(String method, String url) {
            this.method = method;
            this.url = url;
        }

        public Request bodyJson(String json) {
            this.body = json;
            this.contentType = "application/json";
            return this;
        }

        public Request bodyForm(String form) {
            this.body = form;
            this.contentType = "application/x-www-form-urlencoded";
            return this;
        }

        public Request bearer(String token) {
            this.authorization = "Bearer " + token;
            return this;
        }

        public <T> T sendJson(Class<T> responseClass) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setRequestMethod(method);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (contentType != null) {
                    conn.setRequestProperty("Content-Type", contentType);
                }

                if (authorization != null) {
                    conn.setRequestProperty("Authorization", authorization);
                }

                if (body != null) {
                    conn.setDoOutput(true);
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = body.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }
                }

                int responseCode = conn.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    return GSON.fromJson(new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8), responseClass);
                }

                return null;
            } catch (IOException e) {
                System.err.println("HTTP request failed: " + e.getMessage());
                return null;
            }
        }
    }
}

