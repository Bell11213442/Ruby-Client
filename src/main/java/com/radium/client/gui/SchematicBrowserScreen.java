package com.radium.client.gui;
// radium client

import com.radium.client.gui.utils.TextEditor;
import com.radium.client.modules.misc.SchematicBuilder;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SchematicBrowserScreen extends Screen {
    // Spotify-like colors
    private static final int SPOTIFY_CARD = 0xFF181818; // Card background
    private static final int SPOTIFY_HOVER = 0xFF282828; // Hover color
    private static final int SPOTIFY_TEXT = 0xFFFFFFFF; // White text
    private static final int SPOTIFY_TEXT_SECONDARY = 0xFFB3B3B3; // Gray text
    private static final int SPOTIFY_GREEN = 0xFF1DB954; // Spotify green
    private final SchematicBuilder module;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final TextEditor searchEditor = new TextEditor();
    private final int fileHeight = 24;
    private final int cornerRadius = 12;
    private final List<File> schematicFiles = new ArrayList<>();
    private List<File> filteredFiles = new ArrayList<>();
    private int scrollOffset = 0;
    private File selectedFile = null;
    // Draggable panel
    private int panelX = 100;
    private int panelY = 50;
    private final int panelWidth = 500;
    private final int panelHeight = 400;
    private final int headerHeight = 50;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public SchematicBrowserScreen(SchematicBuilder module) {
        super(Text.literal("Select Schematic"));
        this.module = module;
        loadSchematicFiles();
    }

    private void loadSchematicFiles() {
        schematicFiles.clear();
        File schematicsDir = getSchematicsDirectory();

        if (schematicsDir.exists() && schematicsDir.isDirectory()) {
            File[] files = schematicsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String name = file.getName().toLowerCase();
                        if (name.endsWith(".nbt") || name.endsWith(".schematic") ||
                                name.endsWith(".schem") || name.endsWith(".litematic")) {
                            schematicFiles.add(file);
                        }
                    }
                }
            }
        }

        // If directory doesn't exist, create it
        if (!schematicsDir.exists()) {
            schematicsDir.mkdirs();
        }

        filterFiles();
    }

    private File getSchematicsDirectory() {
        File radiumDir = new File(client.runDirectory, "radium");
        return new File(radiumDir, "schematics");
    }

    private void filterFiles() {
        String query = searchEditor.getText().toLowerCase();
        if (query.isEmpty()) {
            filteredFiles = new ArrayList<>(schematicFiles);
        } else {
            filteredFiles = schematicFiles.stream()
                    .filter(file -> file.getName().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }
        scrollOffset = 0;
    }

    @Override
    protected void init() {
        super.init();

        // Center panel on screen
        panelX = (width - panelWidth) / 2;
        panelY = (height - panelHeight) / 2;

        int buttonWidth = 100;
        int buttonHeight = 36;
        int buttonY = panelY + panelHeight - 50;

        // Select button (Spotify green)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Select"), b -> {
            if (selectedFile != null) {
                module.setSelectedSchematic(selectedFile.getAbsolutePath());
                this.client.setScreen(null);
            }
        }).dimensions(panelX + panelWidth / 2 - buttonWidth - 10, buttonY, buttonWidth, buttonHeight).build());

        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Cancel"), b -> {
            this.client.setScreen(null);
        }).dimensions(panelX + panelWidth / 2 + 10, buttonY, buttonWidth, buttonHeight).build());

        // Refresh button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Refresh"), b -> {
            loadSchematicFiles();
        }).dimensions(panelX + panelWidth / 2 - buttonWidth / 2, buttonY - 40, buttonWidth, buttonHeight).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Dark background overlay
        context.fill(0, 0, width, height, 0xCC000000);

        // Main panel with rounded corners (Spotify style)
        RenderUtils.fillRoundRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, SPOTIFY_CARD);

        // Header bar (draggable area)
        boolean headerHovered = mouseX >= panelX && mouseX < panelX + panelWidth &&
                mouseY >= panelY && mouseY < panelY + headerHeight;
        int headerColor = headerHovered ? SPOTIFY_HOVER : SPOTIFY_CARD;
        RenderUtils.fillRoundRect(context, panelX, panelY, panelWidth, headerHeight, cornerRadius, cornerRadius, 0, 0, headerColor);

        // Title in header
        context.drawText(textRenderer, "Select Schematic", panelX + 20, panelY + 18, SPOTIFY_TEXT, false);

        // Search bar
        int searchY = panelY + headerHeight + 15;
        int searchHeight = 40;
        int searchX = panelX + 20;
        int searchWidth = panelWidth - 40;

        boolean searchHovered = mouseX >= searchX && mouseX < searchX + searchWidth &&
                mouseY >= searchY && mouseY < searchY + searchHeight;
        int searchBgColor = searchEditor.isActive() ? SPOTIFY_HOVER : (searchHovered ? 0xFF1F1F1F : 0xFF0F0F0F);
        RenderUtils.fillRoundRect(context, searchX, searchY, searchWidth, searchHeight, 8, searchBgColor);

        String searchText = searchEditor.getText();
        if (searchEditor.isActive()) {
            int cursorPos = searchEditor.getCursorPosition();
            String beforeCursor = searchText.substring(0, Math.min(cursorPos, searchText.length()));
            String afterCursor = searchText.substring(Math.min(cursorPos, searchText.length()));
            searchText = beforeCursor + "_" + afterCursor;
        } else if (searchText.isEmpty()) {
            searchText = "Search schematics...";
        }
        int textColor = searchEditor.isActive() ? SPOTIFY_TEXT : SPOTIFY_TEXT_SECONDARY;
        context.drawText(textRenderer, searchText, searchX + 12, searchY + 12, textColor, false);

        // File list area
        int listY = searchY + searchHeight + 15;
        int listHeight = panelHeight - (listY - panelY) - 100;
        int listX = panelX + 20;
        int listWidth = panelWidth - 40;

        // List background
        RenderUtils.fillRoundRect(context, listX, listY, listWidth, listHeight, 8, 0xFF0F0F0F);

        int visibleFiles = listHeight / fileHeight;
        int maxScroll = Math.max(0, filteredFiles.size() - visibleFiles);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        context.enableScissor(listX, listY, listX + listWidth, listY + listHeight);

        // Render file list
        for (int i = 0; i < visibleFiles && (i + scrollOffset) < filteredFiles.size(); i++) {
            File file = filteredFiles.get(i + scrollOffset);
            int fileY = listY + 5 + (i * fileHeight);
            boolean isSelected = file.equals(selectedFile);
            boolean isHovered = mouseX >= listX && mouseX < listX + listWidth &&
                    mouseY >= fileY && mouseY < fileY + fileHeight;

            // File item background
            if (isSelected) {
                RenderUtils.fillRoundRect(context, listX + 5, fileY, listWidth - 10, fileHeight - 2, 6, SPOTIFY_GREEN);
            } else if (isHovered) {
                RenderUtils.fillRoundRect(context, listX + 5, fileY, listWidth - 10, fileHeight - 2, 6, SPOTIFY_HOVER);
            }

            // File name
            int textColor2 = isSelected ? SPOTIFY_TEXT : SPOTIFY_TEXT_SECONDARY;
            String fileName = file.getName();
            if (textRenderer.getWidth(fileName) > listWidth - 30) {
                fileName = textRenderer.trimToWidth(fileName, listWidth - 50) + "...";
            }
            context.drawText(textRenderer, fileName, listX + 15, fileY + 6, textColor2, false);
        }

        context.disableScissor();

        // Scrollbar
        if (maxScroll > 0 && filteredFiles.size() > 0) {
            int scrollbarX = listX + listWidth - 8;
            int scrollbarWidth = 4;
            int scrollbarHeight = listHeight - 10;
            float scrollProgress = (float) scrollOffset / maxScroll;
            int thumbHeight = Math.max(20, (int) (scrollbarHeight * ((float) visibleFiles / filteredFiles.size())));
            int thumbY = listY + 5 + (int) (scrollProgress * (scrollbarHeight - thumbHeight));

            RenderUtils.fillRoundRect(context, scrollbarX, listY + 5, scrollbarWidth, scrollbarHeight, 2, 0x33000000);
            RenderUtils.fillRoundRect(context, scrollbarX, thumbY, scrollbarWidth, thumbHeight, 2, SPOTIFY_TEXT_SECONDARY);
        }

        // File count
        String countText = filteredFiles.size() + " file" + (filteredFiles.size() != 1 ? "s" : "");
        if (filteredFiles.size() != schematicFiles.size()) {
            countText += " (filtered)";
        }
        context.drawText(textRenderer, countText, panelX + 20, panelY + panelHeight - 30, SPOTIFY_TEXT_SECONDARY, false);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // Check if clicking on header to drag
            if (mouseX >= panelX && mouseX < panelX + panelWidth &&
                    mouseY >= panelY && mouseY < panelY + headerHeight) {
                dragging = true;
                dragOffsetX = (int) mouseX - panelX;
                dragOffsetY = (int) mouseY - panelY;
                return true;
            }

            int listY = panelY + headerHeight + 70;
            int listHeight = panelHeight - (listY - panelY) - 100;
            int listX = panelX + 20;
            int listWidth = panelWidth - 40;

            // Check if clicking on file list
            if (mouseX >= listX && mouseX < listX + listWidth &&
                    mouseY >= listY && mouseY < listY + listHeight) {
                int clickedIndex = ((int) mouseY - listY - 5) / fileHeight;
                if (clickedIndex >= 0 && clickedIndex < filteredFiles.size() - scrollOffset) {
                    selectedFile = filteredFiles.get(clickedIndex + scrollOffset);
                    return true;
                }
            }

            // Check if clicking on search bar
            int searchY = panelY + headerHeight + 15;
            if (mouseX >= panelX + 20 && mouseX < panelX + panelWidth - 20 &&
                    mouseY >= searchY && mouseY < searchY + 40) {
                searchEditor.startEditing(searchEditor.getText());
                return true;
            } else {
                searchEditor.stopEditing();
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            int newX = (int) mouseX - dragOffsetX;
            int newY = (int) mouseY - dragOffsetY;

            // Constrain to screen bounds
            newX = Math.max(0, Math.min(width - panelWidth, newX));
            newY = Math.max(0, Math.min(height - panelHeight, newY));

            panelX = newX;
            panelY = newY;

            // Update button positions
            updateButtonPositions();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateButtonPositions() {
        // Buttons are managed by Screen, but we need to update their positions
        // This is a simplified approach - in a full implementation you'd track button references
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchEditor.isActive()) {
            if (searchEditor.handleKeyPress(keyCode, scanCode, modifiers)) {
                filterFiles();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchEditor.isActive()) {
            if (searchEditor.handleCharType(chr, modifiers)) {
                filterFiles();
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int listY = panelY + headerHeight + 70;
        int listHeight = panelHeight - (listY - panelY) - 100;
        int listX = panelX + 20;
        int listWidth = panelWidth - 40;

        if (mouseX >= listX && mouseX < listX + listWidth &&
                mouseY >= listY && mouseY < listY + listHeight) {
            scrollOffset -= (int) verticalAmount * 3;
            scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, filteredFiles.size() - (listHeight / fileHeight))));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
}
