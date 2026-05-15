package com.radium.client.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class CapeManager {
    private final List<Identifier> availableCapes = new ArrayList<>();
    private final File customCapeDir = new File("config/radium/capes");
    private Identifier selectedCape;

    public CapeManager() {
    }

    public void loadCapes() {
        if (MinecraftClient.getInstance().getTextureManager() == null) return;
        availableCapes.clear();

        if (!customCapeDir.exists()) {
            customCapeDir.mkdirs();
        }

        File[] files = customCapeDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
        if (files == null) return;

        for (File file : files) {
            try {
                String validFileName = file.getName().toLowerCase().replaceAll("[^a-z0-9/._-]", "_");
                Identifier id = Identifier.of("radium_capes", validFileName);

                if (availableCapes.contains(id)) continue;

                try (FileInputStream stream = new FileInputStream(file)) {
                    NativeImage image = NativeImage.read(stream);
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                    MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);
                    availableCapes.add(id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (selectedCape == null && !availableCapes.isEmpty()) {
            selectedCape = availableCapes.get(0);
        }
    }

    public List<Identifier> getAvailableCapes() {
        if (availableCapes.isEmpty()) loadCapes();
        return availableCapes;
    }

    public Identifier getSelectedCape() {
        if (availableCapes.isEmpty()) loadCapes();
        return selectedCape;
    }

    public void setSelectedCape(Identifier selectedCape) {
        this.selectedCape = selectedCape;
    }
}
