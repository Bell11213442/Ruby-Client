package com.radium.client.gui.components;
// radium client

import com.radium.client.gui.RadiumGuiTheme;
import com.radium.client.gui.settings.ItemSetting;
import com.radium.client.gui.utils.GuiUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.List;
import java.util.stream.Collectors;

public class ItemList {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int itemHeight = 24;
    private int scrollOffset = 0;

    public void render(DrawContext context, int x, int y, int width, int height,
                       String searchQuery, float animationProgress) {

        GuiUtils.drawRoundedRect(context, x, y, width, height, 5,
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSettingsBackground(),
                        animationProgress * RadiumGuiTheme.getPanelAlpha()));


        context.enableScissor(x, y, x + width, y + height);


        List<Item> items = getFilteredItems(searchQuery);
        int visibleItems = height / itemHeight;
        int maxScroll = Math.max(0, items.size() - visibleItems);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));


        for (int i = 0; i < visibleItems && (i + scrollOffset) < items.size(); i++) {
            Item item = items.get(i + scrollOffset);
            int itemY = y + 5 + (i * itemHeight);

            renderItem(context, item, x + 5, itemY, width - 20, itemHeight, animationProgress);
        }

        context.disableScissor();


        if (maxScroll > 0) {
            renderScrollbar(context, x, y, width, height, items.size(), visibleItems, animationProgress);
        }
    }

    private void renderItem(DrawContext context, Item item, int x, int y, int width, int height,
                            float animationProgress) {

        int bgColor = RadiumGuiTheme.applyAlpha(0x00000000, 0f);
        GuiUtils.drawRoundedRect(context, x, y, width, height, 3, bgColor);


        ItemStack itemStack = new ItemStack(item);
        context.drawItem(itemStack, x + 4, y + 4);


        String itemName = item.getName().getString();
        int textColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getTextColor(),
                (int) (animationProgress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, itemName, x + 28, y + 8, textColor, false);
    }

    private void renderScrollbar(DrawContext context, int x, int y, int width, int height,
                                 int totalItems, int visibleItems, float animationProgress) {
        int scrollBarX = x + width - 15;
        int scrollBarY = y + 5;
        int scrollBarHeight = height - 10;
        int scrollBarWidth = 8;


        GuiUtils.drawRoundedRect(context, scrollBarX, scrollBarY, scrollBarWidth,
                scrollBarHeight, 4, RadiumGuiTheme.applyAlpha(0x50000000, animationProgress));


        float scrollProgress = (float) scrollOffset / (totalItems - visibleItems);
        int handleHeight = Math.max(20, (int) ((visibleItems / (float) totalItems) * scrollBarHeight));
        int handleY = scrollBarY + (int) ((scrollBarHeight - handleHeight) * scrollProgress);

        GuiUtils.drawRoundedRect(context, scrollBarX, handleY, scrollBarWidth, handleHeight, 4,
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), animationProgress));
    }

    public boolean handleClick(double mx, double my, int button, int x, int y, ItemSetting itemSetting) {
        if (button != 0) return false;

        int height = 180;
        String searchQuery = "";

        List<Item> items = getFilteredItems(searchQuery);
        int visibleItems = height / itemHeight;

        if (GuiUtils.isHovered(mx, my, x, y, 190, height)) {
            int clickedIndex = ((int) my - (y + 5)) / itemHeight;

            if (clickedIndex >= 0 && clickedIndex < visibleItems &&
                    (clickedIndex + scrollOffset) < items.size()) {
                Item selectedItem = items.get(clickedIndex + scrollOffset);
                itemSetting.setValue(selectedItem);
                return true;
            }
        }

        return false;
    }

    public boolean handleScroll(double mx, double my, double hAmount, double vAmount,
                                int x, int y) {
        if (GuiUtils.isHovered(mx, my, x, y, 200, 180)) {
            scrollOffset -= (int) vAmount * 3;
            return true;
        }
        return false;
    }

    private List<Item> getFilteredItems(String query) {
        return Registries.ITEM.stream()
                .filter(item -> item != Items.AIR &&
                        item.getName().getString().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void reset() {
        scrollOffset = 0;
    }
}

