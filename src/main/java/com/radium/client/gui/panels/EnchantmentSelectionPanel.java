package com.radium.client.gui.panels;
// radium client

import com.radium.client.gui.RadiumGuiTheme;
import com.radium.client.gui.components.SearchBar;
import com.radium.client.gui.settings.EnchantmentSetting;
import com.radium.client.gui.utils.DragHandler;
import com.radium.client.gui.utils.GuiUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.*;
import java.util.stream.Collectors;

public class EnchantmentSelectionPanel {
    private static final int HEADER_HEIGHT = 18;
    private static final int SEARCH_HEIGHT = 25;
    private static final List<String> AMETHYST_ENCHANTS = Arrays.asList(
            "Amethyst Pickaxe",
            "Amethyst Axe",
            "Amethyst Sell Axe",
            "Amethyst Shovel"
    );
    private static final List<String> ALL_ENCHANTS = Arrays.asList(

            "Mending",
            "Unbreaking",
            "Curse of Vanishing",

            "Aqua Affinity",
            "Blast Protection",
            "Curse of Binding",
            "Depth Strider",
            "Feather Falling",
            "Fire Protection",
            "Frost Walker",
            "Projectile Protection",
            "Protection",
            "Respiration",
            "Soul Speed",
            "Thorns",
            "Swift Sneak",

            "Bane of Arthropods",
            "Breach",
            "Density",
            "Efficiency",
            "Fire Aspect",
            "Looting",
            "Lunge",
            "Impaling",
            "Knockback",
            "Sharpness",
            "Smite",
            "Sweeping Edge",
            "Wind Burst",

            "Channeling",
            "Flame",
            "Infinity",
            "Loyalty",
            "Riptide",
            "Multishot",
            "Piercing",
            "Power",
            "Punch",
            "Quick Charge",

            "Fortune",
            "Luck of the Sea",
            "Lure",
            "Silk Touch"
    );
    private final SearchBar searchBar;
    private final DragHandler dragHandler;
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int panelWidth = 200;
    private final int panelHeight = 250;
    private final int itemHeight = 16;
    private final Set<String> selectedAmethystEnchants = new HashSet<>();
    private final Set<String> selectedCustomEnchants = new HashSet<>();
    private EnchantmentSetting editingSetting;
    private int panelX = 100, panelY = 100;
    private int scrollOffset = 0;

    public EnchantmentSelectionPanel() {
        this.searchBar = new SearchBar();
        this.dragHandler = new DragHandler();
    }

    public EnchantmentSetting getEditingSetting() {
        return editingSetting;
    }

    public void setEditingSetting(EnchantmentSetting setting) {
        this.editingSetting = setting;
        if (setting != null) {
            searchBar.clear();
            scrollOffset = 0;
            selectedAmethystEnchants.clear();
            selectedAmethystEnchants.addAll(setting.getAmethystEnchants());
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
        boolean isDragging = dragHandler.isDragging();


        int cornerRadius = 12;


        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), progress * 0.4f);
        if (isDragging) {
            borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.6f);
        }
        com.radium.client.utils.render.RenderUtils.drawRoundRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, borderColor);


        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryHeader(), progress * 0.3f);
        GuiUtils.drawRoundedRect(context, panelX, panelY, panelWidth, HEADER_HEIGHT, cornerRadius, headerColor);


        int titleColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getTextColor(), (int) (progress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, "Select Enchantments", panelX + 8, panelY + 6, titleColor, false);


        int closeX = panelX + panelWidth - 15;
        int closeY = panelY + 6;
        boolean closeHovered = GuiUtils.isHovered(mouseX, mouseY, closeX - 2, closeY - 2, 12, 12);
        int closeColor = closeHovered ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), (int) (progress * 255)) | 0xFF000000 :
                RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (progress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, "X", closeX, closeY, closeColor, false);


        int searchY = panelY + HEADER_HEIGHT + 5;
        int searchHeight = 20;
        int searchWidth = panelWidth - 20;
        int searchX = panelX + 10;


        boolean searchActive = searchBar.isEditing();
        int searchBgColor = searchActive ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), progress * 0.15f) :
                RadiumGuiTheme.applyAlpha(0x00000000, 0f);
        GuiUtils.drawRoundedRect(context, searchX, searchY, searchWidth, searchHeight, 5, searchBgColor);


        if (searchActive) {
            int searchBorderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.4f);
            com.radium.client.utils.render.RenderUtils.drawRoundRect(context, searchX, searchY, searchWidth, searchHeight, 5, searchBorderColor);
        }


        String searchText = searchBar.getQuery();
        String displayText = searchText;
        if (searchActive) {

            displayText = searchText.isEmpty() ? "Search enchantments..." : searchText;
        } else {
            displayText = searchText.isEmpty() ? "Search enchantments..." : searchText;
        }
        int textColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (progress * 255)) | 0xFF000000;
        context.drawText(client.textRenderer, displayText, searchX + 5, searchY + 6, textColor, false);


        int listStartY = panelY + HEADER_HEIGHT + SEARCH_HEIGHT + 8;
        int listHeight = panelHeight - HEADER_HEIGHT - SEARCH_HEIGHT - 13;

        context.enableScissor(panelX + 5, listStartY, panelX + panelWidth - 5, listStartY + listHeight);

        List<EnchantmentEntry> entries = getFilteredEntries();
        int visible = listHeight / itemHeight;
        int maxScroll = Math.max(0, entries.size() - visible);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));

        for (int i = 0; i < visible && (i + scrollOffset) < entries.size(); i++) {
            EnchantmentEntry entry = entries.get(i + scrollOffset);
            int itemY = listStartY + 2 + (i * itemHeight);
            renderEnchantmentEntry(context, entry, panelX + 8, itemY, panelWidth - 25, itemHeight, mouseX, mouseY, progress);
        }

        context.disableScissor();


        if (maxScroll > 0) {
            renderScrollbar(context, listStartY, listHeight, scrollOffset, maxScroll, visible, entries.size(), mouseX, mouseY, progress);
        }
    }

    private void renderEnchantmentEntry(DrawContext context, EnchantmentEntry entry,
                                        int x, int y, int width, int height,
                                        int mouseX, int mouseY, float progress) {
        boolean hovered = GuiUtils.isHovered(mouseX, mouseY, x, y, width, height);
        boolean selected = isSelected(entry);


        if (selected || hovered) {
            int bgColor = selected ?
                    RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.4f) :
                    RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), progress * 0.3f);
            GuiUtils.drawRoundedRect(context, x - 2, y - 2, width + 4, height + 4, 4, bgColor);
        }


        if (selected) {
            int itemBorderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.9f);
            com.radium.client.utils.render.RenderUtils.drawRoundRect(context, x - 2, y - 2, width + 4, height + 4, 4, itemBorderColor);
        }


        int textColor = 0xFFFFFFFF;
        String name = entry.isAmethyst ? "§d" + entry.name : entry.name;

        if (client.textRenderer.getWidth(name) > width - 10) {
            name = client.textRenderer.trimToWidth(name, width - 15) + "...";
        }

        context.drawText(client.textRenderer, name, x + 3, y + 4, textColor, true);
    }

    private void renderScrollbar(DrawContext context, int listStartY, int listHeight,
                                 int scrollOffset, int maxScroll, int visible, int total,
                                 int mouseX, int mouseY, float progress) {
        int scrollBarX = panelX + panelWidth - 12;
        int scrollBarY = listStartY + 5;
        int scrollBarHeight = listHeight - 10;
        int scrollBarWidth = 5;

        float scrollProgress = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0f;
        int handleHeight = Math.max(20, (int) ((visible / (float) total) * scrollBarHeight));
        int handleY = scrollBarY + (int) ((scrollBarHeight - handleHeight) * scrollProgress);

        boolean scrollHovered = GuiUtils.isHovered(mouseX, mouseY, scrollBarX, handleY, scrollBarWidth, handleHeight);
        int handleColor = scrollHovered ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.8f) :
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), progress * 0.5f);

        GuiUtils.drawRoundedRect(context, scrollBarX, handleY, scrollBarWidth, handleHeight, 3, handleColor);
    }

    public boolean handleClick(double mx, double my, int button) {
        if (editingSetting == null) return false;


        int closeX = panelX + panelWidth - 15;
        int closeY = panelY + 6;
        int closeSize = 12;
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


        int searchY = panelY + HEADER_HEIGHT + 5;
        if (searchBar.handleClick(mx, my, button, panelX + 10, searchY)) {
            return true;
        }


        int listStartY = panelY + HEADER_HEIGHT + SEARCH_HEIGHT + 8;
        int listHeight = panelHeight - HEADER_HEIGHT - SEARCH_HEIGHT - 13;

        if (GuiUtils.isHovered(mx, my, panelX + 5, listStartY, panelWidth - 10, listHeight)) {
            int clickedIndex = ((int) my - (listStartY + 2)) / itemHeight;
            List<EnchantmentEntry> entries = getFilteredEntries();
            if (clickedIndex >= 0 && clickedIndex + scrollOffset < entries.size()) {
                toggleSelection(entries.get(clickedIndex + scrollOffset));
                return true;
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
        int listStartY = panelY + HEADER_HEIGHT + SEARCH_HEIGHT + 8;
        int listHeight = panelHeight - HEADER_HEIGHT - SEARCH_HEIGHT - 13;
        if (GuiUtils.isHovered(mx, my, panelX + 5, listStartY, panelWidth - 10, listHeight)) {
            scrollOffset -= (int) v * 2;
            scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, getFilteredEntries().size() - (listHeight / itemHeight))));
            return true;
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

    private boolean isSelected(EnchantmentEntry entry) {
        if (entry.isAmethyst) return selectedAmethystEnchants.contains(entry.name);
        if (entry.enchantmentKey == null) return selectedCustomEnchants.contains(entry.name);
        return editingSetting.getEnchantments().contains(entry.enchantmentKey);
    }

    private void toggleSelection(EnchantmentEntry entry) {
        if (entry.isAmethyst) {
            if (selectedAmethystEnchants.contains(entry.name)) {
                editingSetting.removeAmethystEnchant(entry.name);
                selectedAmethystEnchants.remove(entry.name);
            } else {
                editingSetting.addAmethystEnchant(entry.name);
                selectedAmethystEnchants.add(entry.name);
            }
        } else if (entry.enchantmentKey == null) {

            if (selectedCustomEnchants.contains(entry.name)) {
                selectedCustomEnchants.remove(entry.name);
            } else {
                selectedCustomEnchants.add(entry.name);
            }

            editingSetting.setMetadata("selectedCustomEnchants", new ArrayList<>(selectedCustomEnchants));
        } else {

            if (editingSetting.getEnchantments().contains(entry.enchantmentKey))
                editingSetting.removeEnchantment(entry.enchantmentKey);
            else
                editingSetting.addEnchantment(entry.enchantmentKey);
        }
    }

    private List<EnchantmentEntry> getFilteredEntries() {
        List<EnchantmentEntry> entries = new ArrayList<>();
        String query = searchBar.getQuery().toLowerCase();

        Registry<Enchantment> reg = null;


        if (client.world != null) {
            try {
                DynamicRegistryManager drm = client.world.getRegistryManager();
                reg = drm.get(RegistryKeys.ENCHANTMENT);
            } catch (Exception e) {

            }
        }


        if (reg == null) {
            for (String enchant : ALL_ENCHANTS)
                if (enchant.toLowerCase().contains(query))
                    entries.add(new EnchantmentEntry(enchant, null, false));

            for (String a : AMETHYST_ENCHANTS)
                if (a.toLowerCase().contains(query))
                    entries.add(new EnchantmentEntry(a, null, true));
            entries.sort(Comparator.comparing(entry -> entry.name));
            return entries;
        }


        Set<String> registryEnchantNames = new HashSet<>();
        for (RegistryKey<Enchantment> key : reg.getKeys()) {
            String name = getEnchantmentName(key);
            registryEnchantNames.add(name.toLowerCase());
            if (name.toLowerCase().contains(query))
                entries.add(new EnchantmentEntry(name, key, false));
        }


        for (String enchant : ALL_ENCHANTS) {
            if (!registryEnchantNames.contains(enchant.toLowerCase()) && enchant.toLowerCase().contains(query)) {
                entries.add(new EnchantmentEntry(enchant, null, false));
            }
        }


        for (String a : AMETHYST_ENCHANTS)
            if (a.toLowerCase().contains(query))
                entries.add(new EnchantmentEntry(a, null, true));

        entries.sort(Comparator.comparing(entry -> entry.name));
        return entries;
    }

    private String getEnchantmentName(RegistryKey<Enchantment> key) {
        String id = key.getValue().getPath();
        return Arrays.stream(id.split("_"))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    private record EnchantmentEntry(String name, RegistryKey<Enchantment> enchantmentKey, boolean isAmethyst) {
    }
}

