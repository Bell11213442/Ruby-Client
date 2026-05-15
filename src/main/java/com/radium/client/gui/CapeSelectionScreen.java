package com.radium.client.gui;

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.client.Cape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class CapeSelectionScreen extends Screen {

    private final Screen parent;

    public CapeSelectionScreen(Screen parent) {
        super(Text.literal("Select Cape"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Back"), (button) -> {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        }).dimensions(this.width / 2 - 100, this.height - 40, 200, 20).build());

        int previewWidth = 50;
        int previewHeight = 60;
        int padding = 20;

        List<CapeOption> options = new ArrayList<>();

        if (RadiumClient.getCapeManager() != null) {
            for (Identifier capeId : RadiumClient.getCapeManager().getAvailableCapes()) {
                options.add(new CapeOption(getCapeNameFromIdentifier(capeId), capeId));
            }
        }

        int totalWidth = options.size() * (previewWidth + padding) - padding;
        int x = (this.width - totalWidth) / 2;
        int y = this.height / 2 - 50;

        for (CapeOption option : options) {
            this.addDrawableChild(new CapeToggleButtonWidget(x, y, previewWidth, previewHeight, option));
            x += previewWidth + padding;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "Drag & Drop Cape Files Here", this.width / 2, this.height - 60, 0xAAAAAA);
    }

    @Override
    public void filesDragged(List<Path> paths) {
        Path capesDir = Paths.get("config", "radium", "capes");
        boolean capeAdded = false;
        try {
            if (!Files.exists(capesDir)) {
                Files.createDirectories(capesDir);
            }

            for (Path path : paths) {
                if (path.toString().toLowerCase().endsWith(".png")) {
                    String newFileName = path.getFileName().toString().toLowerCase();
                    Path newFile = capesDir.resolve(newFileName);
                    Files.copy(path, newFile, StandardCopyOption.REPLACE_EXISTING);
                    capeAdded = true;
                }
            }

            if (capeAdded && this.client != null) {
                this.client.execute(() -> {
                    RadiumClient.getCapeManager().loadCapes();
                    this.client.setScreen(new CapeSelectionScreen(this.parent));
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getCapeNameFromIdentifier(Identifier identifier) {
        if (identifier == null) return "Unknown";
        String path = identifier.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1, path.length() - 4);
        String name = fileName.replace('_', ' ').replace('-', ' ');
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private record CapeOption(String name, Identifier texture) {
    }

    private static class CapeToggleButtonWidget extends ButtonWidget {
        private final CapeOption option;

        public CapeToggleButtonWidget(int x, int y, int width, int height, CapeOption option) {
            super(x, y, width, height, Text.literal(option.name()), (button) -> ((CapeToggleButtonWidget) button).onPressAction(), DEFAULT_NARRATION_SUPPLIER);
            this.option = option;
        }

        public void onPressAction() {
            Cape capeModule = RadiumClient.moduleManager.getModule(Cape.class);
            if (capeModule == null || RadiumClient.getCapeManager() == null) return;

            if (!capeModule.isEnabled()) {
                capeModule.toggle();
            }
            RadiumClient.getCapeManager().setSelectedCape(option.texture());
        }

        @Override
        public void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            MinecraftClient client = MinecraftClient.getInstance();

            context.drawTexture(
                    option.texture(),
                    this.getX(), this.getY(),
                    this.width, this.height,
                    1, 1,
                    10, 16,
                    64, 32
            );

            context.drawBorder(this.getX(), this.getY(), this.width, this.height, this.isHovered() ? 0xFFFFFFFF : 0xFFA0A0A0);

            boolean isSelected = false;
            Cape capeModule = RadiumClient.moduleManager.getModule(Cape.class);
            if (capeModule != null && RadiumClient.getCapeManager() != null) {
                Identifier selectedCape = RadiumClient.getCapeManager().getSelectedCape();
                isSelected = capeModule.isEnabled() && option.texture().equals(selectedCape);
            }

            String radio = isSelected ? "◉" : "◯";
            int radioColor = isSelected ? 0xFF86E57E : 0xFFAAAAAA;
            int textY = this.getY() + this.height + 5;

            context.drawTextWithShadow(client.textRenderer, radio, this.getX(), textY, radioColor);

            String name = client.textRenderer.trimToWidth(option.name(), this.width - 10);
            context.drawTextWithShadow(client.textRenderer, name, this.getX() + 12, textY, 0xFFFFFFFF);
        }
    }
}
