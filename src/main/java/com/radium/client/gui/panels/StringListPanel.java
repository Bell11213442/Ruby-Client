package com.radium.client.gui.panels;
// radium client

import com.radium.client.gui.RadiumGuiTheme;
import com.radium.client.gui.settings.StringListSetting;
import com.radium.client.gui.utils.DragHandler;
import com.radium.client.gui.utils.GuiUtils;
import com.radium.client.gui.utils.TextEditor;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StringListPanel {
    private final DragHandler dragHandler;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int panelWidth = 125;
    private final int minPanelHeight = 75;
    private final int maxPanelHeight = 150;
    private final int itemHeight = 14;
    private final int padding = 4;
    private final int headerHeight = 15;
    private final int maxVisibleItems = 3;
    private final int addButtonHeight = 14;
    private final int searchBarHeight = 11;
    private final TextEditor editingTextEditor = new TextEditor();
    private final TextEditor searchEditor = new TextEditor();
    private final Map<String, Identifier> headTextureCache = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loadingHeads = new ConcurrentHashMap<>();
    private StringListSetting editingList;
    private int panelX = 500, panelY = 200;
    private int panelHeight = minPanelHeight;
    private int scrollOffset = 0;
    private int editingIndex = -1;
    private boolean isAddingNew = false;

    public StringListPanel() {
        this.dragHandler = new DragHandler();
    }

    private void updatePanelHeight() {
        if (editingList == null) {
            panelHeight = minPanelHeight;
            return;
        }
        int itemsCount = getFilteredItems().size();
        int contentHeight = headerHeight + searchBarHeight + padding * 2;
        if (itemsCount == 0) {
            panelHeight = Math.max(minPanelHeight, contentHeight + addButtonHeight + padding * 2);
        } else if (itemsCount <= maxVisibleItems) {
            panelHeight = contentHeight + (itemsCount * (itemHeight + 4)) + padding + addButtonHeight + padding;
            panelHeight = Math.min(panelHeight, maxPanelHeight);
        } else {
            int visibleItemsHeight = maxVisibleItems * (itemHeight + 4);
            panelHeight = contentHeight + visibleItemsHeight + padding + addButtonHeight + padding;
            panelHeight = Math.min(panelHeight, maxPanelHeight);
        }
    }

    private java.util.List<String> getFilteredItems() {
        if (editingList == null) return java.util.Collections.emptyList();
        if (searchEditor.getText().isEmpty()) return editingList.getList();
        String query = searchEditor.getText().toLowerCase();
        return editingList.getList().stream()
                .filter(item -> item != null && item.toLowerCase().contains(query))
                .collect(java.util.stream.Collectors.toList());
    }

    public StringListSetting getEditingList() {
        return editingList;
    }

    public void setEditingList(StringListSetting list) {
        this.editingList = list;
        if (list != null) {
            scrollOffset = 0;
            editingIndex = -1;
            editingTextEditor.setText("");
            editingTextEditor.stopEditing();
            isAddingNew = false;
            updatePanelHeight();
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float progress) {
        if (editingList == null) return;

        updatePanelHeight();
        int[] pos = dragHandler.getAnimatedPosition();
        panelX = pos[0];
        panelY = pos[1];

        boolean isDragging = dragHandler.isDragging();
        int cornerRadius = 6;


        int purpleBase = RadiumGuiTheme.getAccentColor();
        int darkPurple = RadiumGuiTheme.blendColors(purpleBase, 0xFF000000, 0.8f);
        int panelBgColor = RadiumGuiTheme.applyAlpha(darkPurple, progress * 0.4f);
        RenderUtils.fillRoundRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, panelBgColor);


        int glowColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.15f);
        if (glowColor != 0) {
            RenderUtils.drawRoundRect(context, panelX + 1, panelY + 1, panelWidth - 2, panelHeight - 2, cornerRadius - 1, glowColor);
        }


        float borderAlpha = isDragging ? 1.0f : 0.7f;
        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * borderAlpha);
        RenderUtils.drawRoundRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, borderColor);


        int headerColor = RadiumGuiTheme.applyAlpha(darkPurple, progress * 0.4f);
        RenderUtils.fillRoundTabTop(context, panelX, panelY, panelWidth, headerHeight, cornerRadius, headerColor);


        int titleColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getTextColor(), (int) (progress * 255)) | 0xFF000000;
        String title = editingList.getName();
        context.drawText(client.textRenderer, title, panelX + padding, panelY + (headerHeight - 8) / 2, titleColor, false);


        int closeButtonX = panelX + panelWidth - 9;
        int closeButtonY = panelY + (headerHeight - 5) / 2;
        boolean closeHovered = GuiUtils.isHovered(mouseX, mouseY, closeButtonX - 2, closeButtonY, 7, 5);
        int closeBgColor = closeHovered ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.8f) :
                RadiumGuiTheme.applyAlpha(0x00000000, 0f);
        if (closeBgColor != 0) {
            RenderUtils.fillRoundRect(context, closeButtonX - 2, closeButtonY, 7, 5, 2, closeBgColor);
        }
        int closeTextColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (progress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, "✕", closeButtonX - 1, closeButtonY, closeTextColor, false);


        int searchY = panelY + headerHeight + padding;
        int searchX = panelX + padding;
        int searchWidth = panelWidth - padding * 2;
        boolean searchHovered = GuiUtils.isHovered(mouseX, mouseY, searchX, searchY, searchWidth, searchBarHeight);
        int searchBgColor = (searchEditor.isActive() || searchHovered) ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), progress * 0.2f) :
                RadiumGuiTheme.applyAlpha(0xFF333333, progress * 0.2f);
        RenderUtils.fillRoundRect(context, searchX, searchY, searchWidth, searchBarHeight, 2, searchBgColor);

        String searchPlaceholder = editingList != null ? "Search " + editingList.getName().toLowerCase() + "..." : "Search...";
        String searchText = searchEditor.getText();
        String searchDisplay;
        if (searchEditor.isActive()) {
            int cursorPos = searchEditor.getCursorPosition();
            String beforeCursor = searchText.substring(0, cursorPos);
            String afterCursor = searchText.substring(cursorPos);
            searchDisplay = beforeCursor + "_" + afterCursor;
        } else if (searchText.isEmpty()) {
            searchDisplay = searchPlaceholder;
        } else {
            searchDisplay = searchText;
        }
        int searchTextColor = RadiumGuiTheme.applyAlpha(
                searchEditor.getText().isEmpty() ? RadiumGuiTheme.getDisabledTextColor() : RadiumGuiTheme.getTextColor(),
                (int) (progress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, searchDisplay, searchX + 2, searchY + (searchBarHeight - 8) / 2, searchTextColor, false);


        java.util.List<String> filteredItems = getFilteredItems();
        int listY = searchY + searchBarHeight + padding;
        int listHeight = panelHeight - headerHeight - searchBarHeight - addButtonHeight - padding * 4;
        int visibleItemsHeight = maxVisibleItems * (itemHeight + 4);
        int actualListHeight = Math.min(listHeight, visibleItemsHeight);

        context.enableScissor(panelX, listY, panelX + panelWidth, listY + actualListHeight);

        int yOffset = listY - scrollOffset;
        int maxScroll = Math.max(0, (filteredItems.size() * (itemHeight + 4)) - actualListHeight);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;


        if (filteredItems.isEmpty()) {
            String itemName = editingList != null ? editingList.getName().toLowerCase() : "items";
            String emptyText = searchEditor.getText().isEmpty() ? "No " + itemName + " yet" : "No " + itemName + " found";
            int emptyTextX = panelX + (panelWidth - client.textRenderer.getWidth(emptyText)) / 2;
            int emptyTextY = listY + actualListHeight / 2 - 4;
            int emptyTextColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getDisabledTextColor(), (int) (progress * 255)) | 0xFF000000;
            context.drawText(client.textRenderer, emptyText, emptyTextX, emptyTextY, emptyTextColor, false);
        } else {
            for (int i = 0; i < filteredItems.size(); i++) {
                if (yOffset + itemHeight < listY || yOffset > listY + actualListHeight) {
                    yOffset += itemHeight + 4;
                    continue;
                }

                String item = filteredItems.get(i);
                String displayText = item != null ? item : "";


                int originalIndex = editingList.getList().indexOf(item);
                if (editingIndex == originalIndex) {
                    String editorText = editingTextEditor.getText();
                    int cursorPos = editingTextEditor.getCursorPosition();
                    String beforeCursor = editorText.substring(0, cursorPos);
                    String afterCursor = editorText.substring(cursorPos);
                    displayText = beforeCursor + "_" + afterCursor;
                }

                int itemX = panelX + padding;
                int itemWidth = panelWidth - padding * 2 - 4;
                int itemY = yOffset;

                boolean itemHovered = GuiUtils.isHovered(mouseX, mouseY, itemX, itemY, itemWidth, itemHeight);
                boolean isEditing = editingIndex == originalIndex;


                int itemBgColor = (isEditing || itemHovered) ?
                        RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), progress * 0.2f) :
                        RadiumGuiTheme.applyAlpha(0xFF333333, progress * 0.2f);
                RenderUtils.fillRoundRect(context, itemX, itemY, itemWidth, itemHeight, 4, itemBgColor);


                int headSize = itemHeight - 6;
                int headX = itemX + 4;
                int headY = itemY + 3;
                boolean headDrawn = false;

                if (item != null && !item.trim().isEmpty()) {
                    try {
                        String username = item.trim();
                        String usernameLower = username.toLowerCase();
                        Identifier headTexture = headTextureCache.get(usernameLower);


                        if (headTexture == null && !loadingHeads.getOrDefault(usernameLower, false)) {
                            loadingHeads.put(usernameLower, true);
                            fetchHeadFromVZGE(username);
                        }


                        if (headTexture != null) {
                            RenderUtils.drawRoundTexture(context, headTexture, headX, headY, headSize, headSize, 2);
                            headDrawn = true;
                        }
                    } catch (Exception e) {

                    }
                }


                if (!headDrawn) {
                    int placeholderColor = RadiumGuiTheme.applyAlpha(0xFF666666, progress * 0.2f);
                    RenderUtils.fillRoundRect(context, headX, headY, headSize, headSize, 2, placeholderColor);
                }


                int textX = headX + headSize + 3;
                int textY = itemY + (itemHeight - 8) / 2;
                int textColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getTextColor(), (int) (progress * 255)) | 0xFF000000;
                int maxTextWidth = itemWidth - headSize - 3 - 22;
                String display = displayText.length() > 20 ? displayText.substring(0, 17) + "..." : displayText;
                if (client.textRenderer.getWidth(display) > maxTextWidth) {
                    display = client.textRenderer.trimToWidth(display, maxTextWidth - client.textRenderer.getWidth("...")) + "...";
                }
                context.drawText(client.textRenderer, display, textX, textY, textColor, false);


                int deleteButtonWidth = 20;
                int deleteButtonHeight = itemHeight - 2;
                int deleteButtonX = itemX + itemWidth - deleteButtonWidth - 2;
                int deleteButtonY = itemY + 1;
                boolean deleteHovered = GuiUtils.isHovered(mouseX, mouseY, deleteButtonX, deleteButtonY, deleteButtonWidth, deleteButtonHeight);
                int deleteBgColor = deleteHovered ?
                        RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.8f) :
                        RadiumGuiTheme.applyAlpha(0xFF333333, progress * 0.2f);
                RenderUtils.fillRoundRect(context, deleteButtonX, deleteButtonY, deleteButtonWidth, deleteButtonHeight, 2, deleteBgColor);
                int deleteTextColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (progress * 255)) | 0xFF000000;
                String deleteText = "×";
                int deleteTextX = deleteButtonX + (deleteButtonWidth - client.textRenderer.getWidth(deleteText)) / 2;
                context.drawText(client.textRenderer, deleteText, deleteTextX, deleteButtonY + (deleteButtonHeight - 8) / 2, deleteTextColor, false);

                yOffset += itemHeight + 4;
            }
        }

        context.disableScissor();


        int addButtonY = panelY + panelHeight - addButtonHeight - padding;
        int addButtonX = panelX + padding;
        int addButtonWidth = panelWidth - padding * 2;
        boolean addHovered = GuiUtils.isHovered(mouseX, mouseY, addButtonX, addButtonY, addButtonWidth, addButtonHeight);
        int addBgColor = addHovered ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.8f) :
                RadiumGuiTheme.applyAlpha(0xFF555555, progress * 0.3f);
        RenderUtils.fillRoundRect(context, addButtonX, addButtonY, addButtonWidth, addButtonHeight, 3, addBgColor);

        String addText;
        if (isAddingNew && editingTextEditor.isActive()) {
            String editorText = editingTextEditor.getText();
            int cursorPos = editingTextEditor.getCursorPosition();
            String beforeCursor = editorText.substring(0, cursorPos);
            String afterCursor = editorText.substring(cursorPos);
            addText = beforeCursor + "_" + afterCursor;
        } else {
            addText = "Add";
        }
        int addTextX = addButtonX + (addButtonWidth - client.textRenderer.getWidth(addText)) / 2;
        int addTextY = addButtonY + (addButtonHeight - 8) / 2;
        int addTextColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (progress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, addText, addTextX, addTextY, addTextColor, false);


        if (filteredItems.size() > maxVisibleItems && maxScroll > 0) {
            int scrollbarX = panelX + panelWidth - 4;
            int scrollbarY = listY;
            int scrollbarWidth = 2;
            int scrollbarHeight = actualListHeight;
            int scrollbarBgColor = RadiumGuiTheme.applyAlpha(0xFF333333, progress * 0.3f);
            RenderUtils.fillRoundRect(context, scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, 1, scrollbarBgColor);

            int thumbHeight = Math.max(10, (int) ((double) actualListHeight / filteredItems.size() * maxVisibleItems));
            int thumbY = scrollbarY + (int) ((double) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
            int thumbColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.8f);
            RenderUtils.fillRoundRect(context, scrollbarX, thumbY, scrollbarWidth, thumbHeight, 1, thumbColor);
        }
    }

    public boolean handleClick(double mx, double my, int button) {
        if (editingList == null) return false;


        int closeButtonX = panelX + panelWidth - 9;
        int closeButtonY = panelY + (headerHeight - 5) / 2;
        if (GuiUtils.isHovered(mx, my, closeButtonX - 2, closeButtonY, 7, 5)) {
            editingList = null;
            editingIndex = -1;
            editingTextEditor.setText("");
            editingTextEditor.stopEditing();
            isAddingNew = false;
            searchEditor.setText("");
            searchEditor.stopEditing();
            return true;
        }


        if (button == 0 && GuiUtils.isHovered(mx, my, panelX, panelY, panelWidth - 9, headerHeight)) {
            dragHandler.startDrag(mx, my, panelX, panelY);
            return true;
        }


        int searchY = panelY + headerHeight + padding;
        int searchX = panelX + padding;
        int searchWidth = panelWidth - padding * 2;
        if (GuiUtils.isHovered(mx, my, searchX, searchY, searchWidth, searchBarHeight)) {
            searchEditor.startEditing(searchEditor.getText());
            isAddingNew = false;
            editingIndex = -1;
            return true;
        }


        java.util.List<String> filteredItems = getFilteredItems();
        int listY = searchY + searchBarHeight + padding;
        int listHeight = panelHeight - headerHeight - searchBarHeight - addButtonHeight - padding * 4;
        int visibleItemsHeight = maxVisibleItems * (itemHeight + 4);
        int actualListHeight = Math.min(listHeight, visibleItemsHeight);
        int itemStartY = listY - scrollOffset;

        for (int i = 0; i < filteredItems.size(); i++) {
            int itemY = itemStartY + i * (itemHeight + 4);
            if (itemY + itemHeight < listY || itemY > listY + actualListHeight) continue;

            String item = filteredItems.get(i);
            int originalIndex = editingList.getList().indexOf(item);

            int itemX = panelX + padding;
            int itemWidth = panelWidth - padding * 2 - 4;
            int deleteButtonWidth = 20;
            int deleteButtonHeight = itemHeight - 2;
            int deleteButtonX = itemX + itemWidth - deleteButtonWidth - 2;
            int deleteButtonY = itemY + 1;


            if (GuiUtils.isHovered(mx, my, deleteButtonX, deleteButtonY, deleteButtonWidth, deleteButtonHeight)) {
                editingList.remove(originalIndex);
                if (editingIndex == originalIndex) {
                    editingIndex = -1;
                    editingTextEditor.setText("");
                    editingTextEditor.stopEditing();
                } else if (editingIndex > originalIndex) {
                    editingIndex--;
                }
                updatePanelHeight();
                return true;
            }


            int headSize = itemHeight - 3;
            int clickAreaX = itemX + headSize + 3;
            int clickAreaWidth = itemWidth - headSize - 3 - deleteButtonWidth - 3;
            if (GuiUtils.isHovered(mx, my, clickAreaX, itemY, clickAreaWidth, itemHeight)) {
                editingIndex = originalIndex;
                editingTextEditor.startEditing(item != null ? item : "");
                isAddingNew = false;
                searchEditor.stopEditing();
                return true;
            }
        }


        int addButtonY = panelY + panelHeight - addButtonHeight - padding;
        int addButtonX = panelX + padding;
        int addButtonWidth = panelWidth - padding * 2;
        if (GuiUtils.isHovered(mx, my, addButtonX, addButtonY, addButtonWidth, addButtonHeight)) {
            if (isAddingNew) {
                String text = editingTextEditor.getText().trim();
                if (!text.isEmpty()) {
                    editingList.add(text);
                    updatePanelHeight();
                }
                isAddingNew = false;
                editingTextEditor.setText("");
                editingTextEditor.stopEditing();
            } else {
                isAddingNew = true;
                editingIndex = -1;
                editingTextEditor.setText("");
                editingTextEditor.startEditing("");
                searchEditor.stopEditing();
            }
            return true;
        }


        if (searchEditor.isActive() && !GuiUtils.isHovered(mx, my, searchX, searchY, searchWidth, searchBarHeight)) {
            searchEditor.stopEditing();
        }

        return GuiUtils.isHovered(mx, my, panelX, panelY, panelWidth, panelHeight);
    }

    public boolean handleScroll(double mx, double my, double hAmount, double vAmount) {
        if (editingList == null) return false;
        int searchY = panelY + headerHeight + padding;
        int listY = searchY + searchBarHeight + padding;
        int listHeight = panelHeight - headerHeight - searchBarHeight - addButtonHeight - padding * 4;
        int visibleItemsHeight = maxVisibleItems * (itemHeight + 4);
        int actualListHeight = Math.min(listHeight, visibleItemsHeight);

        if (!GuiUtils.isHovered(mx, my, panelX, listY, panelWidth, actualListHeight)) {
            return false;
        }

        java.util.List<String> filteredItems = getFilteredItems();
        int maxScroll = Math.max(0, (filteredItems.size() * (itemHeight + 4)) - actualListHeight);
        scrollOffset -= vAmount * 15;
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        return true;
    }

    public boolean handleDrag(double mx, double my, int button, double dx, double dy) {
        if (editingList == null) return false;

        if (dragHandler.isDragging()) {
            dragHandler.updatePosition(mx, my);
            return true;
        }

        return false;
    }

    public boolean handleRelease(double mx, double my, int button) {
        if (button == 0) {
            dragHandler.stopDrag();
        }
        return false;
    }

    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (editingList == null) return false;


        if (searchEditor.isActive()) {
            if (searchEditor.handleKeyPress(keyCode, scanCode, modifiers)) {
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    searchEditor.setText("");
                }
                return true;
            }
            return false;
        }


        if (editingIndex == -1 && !isAddingNew) return false;
        if (!editingTextEditor.isActive()) return false;

        if (editingTextEditor.handleKeyPress(keyCode, scanCode, modifiers)) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                editingIndex = -1;
                isAddingNew = false;
                editingTextEditor.cancelEditing();
            } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
                String text = editingTextEditor.getText().trim();
                if (isAddingNew) {
                    if (!text.isEmpty()) {
                        editingList.add(text);
                        updatePanelHeight();
                    }
                    isAddingNew = false;
                    editingTextEditor.stopEditing();
                } else if (editingIndex >= 0) {
                    if (editingIndex < editingList.size()) {
                        if (!text.isEmpty()) {
                            editingList.set(editingIndex, text);
                        } else {
                            editingList.remove(editingIndex);
                            updatePanelHeight();
                        }
                    }
                    editingIndex = -1;
                    editingTextEditor.stopEditing();
                }
            }
            return true;
        }

        return false;
    }

    public boolean handleCharType(char chr, int modifiers) {
        if (editingList == null) return false;


        if (searchEditor.isActive()) {
            if (searchEditor.handleCharType(chr, modifiers)) {
                scrollOffset = 0;
                return true;
            }
            return false;
        }


        if (editingIndex == -1 && !isAddingNew) return false;
        if (!editingTextEditor.isActive()) return false;

        return editingTextEditor.handleCharType(chr, modifiers);
    }

    public boolean isOpen() {
        return editingList != null;
    }

    private void fetchHeadFromVZGE(String username) {
        if (username == null || username.trim().isEmpty()) return;

        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://minotar.net/helm/" + username + "/64.png";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "RadiumClient/1.0 (+https://radiumclient.com)");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                if (connection.getResponseCode() != 200) {
                    loadingHeads.remove(username.toLowerCase());
                    return;
                }

                InputStream inputStream = connection.getInputStream();
                NativeImage image = NativeImage.read(inputStream);
                inputStream.close();

                Identifier textureId = Identifier.of("radium", "friends/heads/" + username.toLowerCase());

                client.execute(() -> {
                    try {
                        client.getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(image));
                        headTextureCache.put(username.toLowerCase(), textureId);
                    } catch (Exception e) {

                    } finally {
                        loadingHeads.remove(username.toLowerCase());
                    }
                });
            } catch (Exception e) {
                loadingHeads.remove(username.toLowerCase());
            }
        });
    }
}


