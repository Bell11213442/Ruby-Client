package com.radium.client.gui.panels;

import com.radium.client.gui.RadiumGuiTheme;
import com.radium.client.gui.components.SearchBar;
import com.radium.client.gui.settings.BlockSetting;
import com.radium.client.gui.utils.DragHandler;
import com.radium.client.gui.utils.GuiUtils;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BlockSelectionPanel {
    private static final int HEADER_HEIGHT = 30;
    private final SearchBar searchBar;
    private final DragHandler dragHandler;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int panelWidth = 150;
    private final int panelHeight = 180;
    private BlockSetting editingSetting;
    private int panelX = 100, panelY = 100;
    private int scrollOffset = 0;

    public BlockSelectionPanel() {
        this.searchBar = new SearchBar();
        this.dragHandler = new DragHandler();
    }

    public BlockSetting getEditingSetting() {
        return editingSetting;
    }

    public void setEditingBlock(BlockSetting setting) {
        this.editingSetting = setting;
        if (setting != null) {
            searchBar.clear();
            scrollOffset = 0;
        }
    }

    public boolean isOpen() {
        return editingSetting != null;
    }

    public void render(DrawContext context, int mouseX, int mouseY, float progress) {
        if (editingSetting == null) return;

        int[] pos = dragHandler.getAnimatedPosition();
        panelX = pos[0];
        panelY = pos[1];

        int cornerRadius = 12;

        int bgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSettingsPanelColor(), progress * RadiumGuiTheme.getPanelAlpha());
        RenderUtils.fillRoundRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, bgColor);

        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.4f);
        RenderUtils.drawRoundRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, borderColor);

        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryHeader(), progress * RadiumGuiTheme.getPanelAlpha());
        RenderUtils.fillRoundTabTop(context, panelX, panelY, panelWidth, HEADER_HEIGHT, cornerRadius, headerColor);

        int titleColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (progress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, "Select Blocks", panelX + 10, panelY + 10, titleColor, true);

        int closeX = panelX + panelWidth - 20;
        int closeY = panelY + 8;
        boolean closeHovered = GuiUtils.isHovered(mouseX, mouseY, closeX, closeY, 16, 16);
        int closeColor = closeHovered ? RadiumGuiTheme.getAccentColor() : 0xFFCCCCCC;
        context.drawText(client.textRenderer, "✕", closeX + 2, panelY + 10,
                RadiumGuiTheme.applyAlpha(closeColor, (int) (progress * 255)) | 0xFF000000, false);

        int searchBarX = panelX + 10;
        int searchBarY = panelY + 35;
        int searchBarWidth = panelWidth - 20;
        int searchBarHeight = 20;
        searchBar.setWidth(searchBarWidth);
        boolean searchHovered = GuiUtils.isHovered(mouseX, mouseY, searchBarX, searchBarY, searchBarWidth, searchBarHeight);
        int searchBarColor = (searchBar.isEditing() || searchHovered) ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), progress * 0.5f) :
                RadiumGuiTheme.applyAlpha(0x00000000, 0f);

        int subRadius = Math.max(2, cornerRadius / 2);
        RenderUtils.fillRoundRect(context, searchBarX, searchBarY, searchBarWidth, searchBarHeight, subRadius, searchBarColor);

        String searchText = searchBar.getQuery();
        if (searchBar.isEditing() && searchText.isEmpty()) {
            searchText = "Search for a block...";
        } else if (!searchBar.isEditing() && searchText.isEmpty()) {
            searchText = "Search for a block...";
        }
        context.drawText(client.textRenderer, searchText, searchBarX + 5, searchBarY + 6, titleColor, false);

        int itemsStartY = panelY + 60;
        int itemsHeight = panelHeight - 70;

        int BLOCK_SIZE = 20;
        int BLOCK_PADDING = 3;
        int BLOCKS_PER_ROW = 6;

        RenderUtils.fillRoundRect(context, panelX + 5, itemsStartY, panelWidth - 10, itemsHeight, subRadius,
                RadiumGuiTheme.applyAlpha(0x00000000, 0f));

        context.enableScissor(panelX + 5, itemsStartY, panelX + panelWidth - 5, itemsStartY + itemsHeight);

        List<Block> allBlocks = getFilteredBlocks();

        int totalRows = (int) Math.ceil((double) allBlocks.size() / BLOCKS_PER_ROW);
        int visibleRows = itemsHeight / (BLOCK_SIZE + BLOCK_PADDING);
        int maxRowScroll = Math.max(0, totalRows - visibleRows);
        int rowScrollOffset = Math.max(0, Math.min(scrollOffset / BLOCKS_PER_ROW, maxRowScroll));

        int startIndex = rowScrollOffset * BLOCKS_PER_ROW;
        int endIndex = Math.min(startIndex + (visibleRows * BLOCKS_PER_ROW), allBlocks.size());

        int gridWidth = (BLOCKS_PER_ROW * BLOCK_SIZE) + ((BLOCKS_PER_ROW - 1) * BLOCK_PADDING);
        int gridStartX = panelX + (panelWidth - gridWidth) / 2;

        for (int i = startIndex; i < endIndex; i++) {
            Block block = allBlocks.get(i);
            int row = (i - startIndex) / BLOCKS_PER_ROW;
            int col = (i - startIndex) % BLOCKS_PER_ROW;

            int blockX = gridStartX + col * (BLOCK_SIZE + BLOCK_PADDING);
            int blockY = itemsStartY + 5 + row * (BLOCK_SIZE + BLOCK_PADDING);

            boolean blockHovered = GuiUtils.isHovered(mouseX, mouseY, blockX, blockY, BLOCK_SIZE, BLOCK_SIZE);
            boolean selected = editingSetting.containsBlock(block);

            int blockBgColor;
            if (selected) {
                blockBgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.4f);
            } else if (blockHovered) {
                blockBgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), progress * 0.3f);
            } else {
                blockBgColor = RadiumGuiTheme.applyAlpha(0x00000000, 0f);
            }

            if (blockBgColor != 0) {
                int blockRadius = 2;
                RenderUtils.fillRoundRect(context, blockX, blockY, BLOCK_SIZE, BLOCK_SIZE, blockRadius, blockBgColor);
            }

            if (selected) {
                int blockBorderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.9f);
                RenderUtils.drawRoundRect(context, blockX, blockY, BLOCK_SIZE, BLOCK_SIZE, 2, blockBorderColor);
            }

            ItemStack blockStack = new ItemStack(block.asItem());
            if (!blockStack.isEmpty() && blockStack.getItem() != Items.AIR) {
                context.drawItem(blockStack, blockX, blockY);
            } else {
                context.drawText(client.textRenderer, "?", blockX + 6, blockY + 6, 0xFFFFFFFF, true);
            }
        }

        context.disableScissor();

        if (maxRowScroll > 0) {
            int scrollBarX = panelX + panelWidth - 12;
            int scrollBarY = itemsStartY + 5;
            int scrollBarHeight = itemsHeight - 10;
            int scrollBarWidth = 4;

            int scrollBarBgColor = RadiumGuiTheme.applyAlpha(0x50000000, progress);
            RenderUtils.fillRoundRect(context, scrollBarX, scrollBarY, scrollBarWidth, scrollBarHeight, 2, scrollBarBgColor);

            float scrollProgress = maxRowScroll > 0 ? (float) rowScrollOffset / maxRowScroll : 0f;
            int handleHeight = Math.max(20, (int) ((visibleRows / (float) totalRows) * scrollBarHeight));
            int handleY = scrollBarY + (int) ((scrollBarHeight - handleHeight) * scrollProgress);

            int handleColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress);
            RenderUtils.fillRoundRect(context, scrollBarX, handleY, scrollBarWidth, handleHeight, 2, handleColor);
        }
    }

    public boolean handleClick(double mx, double my, int button) {
        if (editingSetting == null) return false;

        int closeX = panelX + panelWidth - 20;
        int closeY = panelY + 8;
        int closeSize = 16;
        if (GuiUtils.isHovered(mx, my, closeX, closeY, closeSize, closeSize)) {
            editingSetting = null;
            return true;
        }

        if (button == 0 && GuiUtils.isHovered(mx, my, panelX, panelY, panelWidth, HEADER_HEIGHT)) {
            if (!GuiUtils.isHovered(mx, my, closeX, closeY, closeSize, closeSize)) {
                dragHandler.startDrag(mx, my, panelX, panelY);
                return true;
            }
        }

        int searchBarX = panelX + 10;
        int searchBarY = panelY + 35;
        if (searchBar.handleClick(mx, my, button, searchBarX, searchBarY)) {
            return true;
        }

        int itemsStartY = panelY + 60;
        int itemsHeight = panelHeight - 70;
        int BLOCK_SIZE = 20;
        int BLOCK_PADDING = 3;
        int BLOCKS_PER_ROW = 6;

        if (GuiUtils.isHovered(mx, my, panelX + 5, itemsStartY, panelWidth - 10, itemsHeight)) {
            List<Block> allBlocks = getFilteredBlocks();
            int totalRows = (int) Math.ceil((double) allBlocks.size() / BLOCKS_PER_ROW);
            int visibleRows = itemsHeight / (BLOCK_SIZE + BLOCK_PADDING);
            int maxRowScroll = Math.max(0, totalRows - visibleRows);
            int rowScrollOffset = Math.max(0, Math.min(scrollOffset / BLOCKS_PER_ROW, maxRowScroll));
            int startIndex = rowScrollOffset * BLOCKS_PER_ROW;

            int gridWidth = (BLOCKS_PER_ROW * BLOCK_SIZE) + ((BLOCKS_PER_ROW - 1) * BLOCK_PADDING);
            int gridStartX = panelX + (panelWidth - gridWidth) / 2;

            int clickedX = (int) mx - gridStartX;
            int clickedY = (int) my - itemsStartY - 5;
            int col = clickedX / (BLOCK_SIZE + BLOCK_PADDING);
            int row = clickedY / (BLOCK_SIZE + BLOCK_PADDING);

            if (col >= 0 && col < BLOCKS_PER_ROW && row >= 0) {
                int clickedIndex = startIndex + (row * BLOCKS_PER_ROW) + col;
                if (clickedIndex >= 0 && clickedIndex < allBlocks.size()) {
                    Block clickedBlock = allBlocks.get(clickedIndex);
                    toggleSelection(clickedBlock);
                    return true;
                }
            }
        }

        return GuiUtils.isHovered(mx, my, panelX, panelY, panelWidth, panelHeight);
    }

    public boolean handleDrag(double mx, double my, int button, double dx, double dy) {
        if (editingSetting == null) return false;
        if (dragHandler.isDragging()) {
            dragHandler.updatePosition(mx, my);
            return true;
        }
        return false;
    }

    public boolean handleRelease(double mx, double my, int button) {
        if (editingSetting == null) return false;
        if (button == 0) dragHandler.stopDrag();
        return false;
    }

    public boolean handleScroll(double mx, double my, double h, double v) {
        if (editingSetting == null) return false;
        int itemsStartY = panelY + 60;
        int itemsHeight = panelHeight - 70;
        if (GuiUtils.isHovered(mx, my, panelX + 5, itemsStartY, panelWidth - 10, itemsHeight)) {
            List<Block> allBlocks = getFilteredBlocks();
            int BLOCKS_PER_ROW = 6;
            int BLOCK_SIZE = 20;
            int BLOCK_PADDING = 3;
            int totalRows = (int) Math.ceil((double) allBlocks.size() / BLOCKS_PER_ROW);
            int visibleRows = itemsHeight / (BLOCK_SIZE + BLOCK_PADDING);
            int maxRowScroll = Math.max(0, totalRows - visibleRows);

            if (maxRowScroll > 0) {
                scrollOffset -= (int) v * BLOCKS_PER_ROW;
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxRowScroll * BLOCKS_PER_ROW));
                return true;
            }
        }
        return false;
    }

    public boolean handleKeyPress(int keyCode, int scanCode, int modifiers) {
        if (editingSetting == null) return false;
        return searchBar.handleKeyPress(keyCode, scanCode, modifiers);
    }

    public boolean handleCharType(char chr, int modifiers) {
        if (editingSetting == null) return false;
        return searchBar.handleCharType(chr, modifiers);
    }

    private void toggleSelection(Block block) {
        if (editingSetting.containsBlock(block)) {
            editingSetting.removeBlock(block);
        } else {
            editingSetting.addBlock(block);
        }
    }

    private List<Block> getFilteredBlocks() {
        List<Block> allBlocks = new ArrayList<>();
        String query = searchBar.getQuery().toLowerCase();
        Set<Block> selectedBlocks = editingSetting.getBlocks();

        for (Block block : Registries.BLOCK) {
            Identifier id = Registries.BLOCK.getId(block);
            if (id == null) continue;

            ItemStack blockItem = new ItemStack(block.asItem());
            if (blockItem.isEmpty() || blockItem.getItem() == Items.AIR) {
                continue;
            }

            String blockName = formatBlockName(id.getPath());
            if (blockName.toLowerCase().contains(query)) {
                allBlocks.add(block);
            }
        }

        allBlocks.sort((b1, b2) -> {
            boolean b1Selected = selectedBlocks.contains(b1);
            boolean b2Selected = selectedBlocks.contains(b2);
            if (b1Selected && !b2Selected) return -1;
            if (!b1Selected && b2Selected) return 1;
            return formatBlockName(Registries.BLOCK.getId(b1).getPath())
                    .compareToIgnoreCase(formatBlockName(Registries.BLOCK.getId(b2).getPath()));
        });

        return allBlocks;
    }

    private String formatBlockName(String path) {
        return Arrays.stream(path.split("_"))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }
}
