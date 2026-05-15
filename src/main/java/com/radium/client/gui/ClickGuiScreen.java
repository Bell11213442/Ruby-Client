package com.radium.client.gui;


import com.radium.client.client.KeybindManager;
import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.*;
import com.radium.client.gui.utils.TextEditor;
import com.radium.client.modules.Module;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClickGuiScreen extends Screen {

    private static final Map<Module.Category, Identifier> CATEGORY_ICONS = new HashMap<>();
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final int categoryWidth = 115;
    private final int moduleHeight = 15;
    private final int headerHeight = 15;
    private final Map<Module.Category, Integer> categoryX = new HashMap<>();
    private final Map<Module.Category, Integer> categoryY = new HashMap<>();
    private final Map<Module.Category, Boolean> categoryExpanded = new HashMap<>();
    private final Map<Module.Category, Float> categoryTargetX = new HashMap<>();
    private final Map<Module.Category, Float> categoryTargetY = new HashMap<>();
    private final Map<Module.Category, Long> categoryDragStartTime = new HashMap<>();
    private final Map<Module.Category, Boolean> categoryIsDragging = new HashMap<>();
    private final int panelWidth = 180;
    private final int panelHeaderHeight = 18;
    private final int settingHeight = 20;
    private final TextEditor itemSearchEditor = new TextEditor();
    private final TextEditor moduleSearchEditor = new TextEditor();
    private final int itemPanelWidth = 150;
    private final int itemPanelHeight = 180;
    private final TextEditor stringEditor = new TextEditor();
    private final long animationStartTime;
    private final int maxPanelHeight = 200;
    private final com.radium.client.gui.panels.EnchantmentSelectionPanel enchantmentPanel = new com.radium.client.gui.panels.EnchantmentSelectionPanel();
    private final com.radium.client.gui.panels.BlockSelectionPanel blockSelectionPanel = new com.radium.client.gui.panels.BlockSelectionPanel();
    private final com.radium.client.gui.panels.ColorPickerPanel colorPickerPanel = new com.radium.client.gui.panels.ColorPickerPanel();
    private final com.radium.client.gui.panels.StringListPanel stringListPanel = new com.radium.client.gui.panels.StringListPanel();
    int yOffset;
    private boolean draggingCategory = false;
    private Module.Category dragCategory;
    private int dragOffsetX, dragOffsetY;
    private Module selectedModule = null;
    private boolean draggingPanel = false;
    private int panelDragOffsetX = 0;
    private int panelDragOffsetY = 0;
    private int panelX = 700;
    private int panelY = 200;
    private float panelTargetX = 700;
    private float panelTargetY = 200;
    private long panelDragStartTime = 0;
    private boolean panelIsDragging = false;
    private ItemSetting editingItem = null;
    private int itemScrollOffset = 0;
    private int searchScrollOffset = 0;
    private int itemPanelX = 100;
    private int itemPanelY = 100;
    private float itemPanelTargetX = 100;
    private float itemPanelTargetY = 100;
    private long itemPanelDragStartTime = 0;
    private boolean itemPanelIsDragging = false;
    private boolean draggingItemPanel = false;
    private int itemPanelDragOffsetX = 0;
    private int itemPanelDragOffsetY = 0;
    private boolean draggingSlider = false;
    private SliderSetting draggedSlider = null;
    private boolean draggingNumber = false;
    private NumberSetting draggedNumber = null;
    private boolean draggingDouble = false;
    private DoubleSetting draggedDouble = null;
    private boolean listeningForKeybind = false;
    private Module keybindModule = null;
    private StringSetting editingString = null;
    private boolean isAnimating;
    private float settingsScrollOffset = 0;
    private float maxScroll = 0;
    private boolean draggingScrollBar = false;
    private boolean renderingDropdown = false;
    private float pulseAnimation = 0f;
    private float hoverAnimation = 0f;
    private int hoveredModuleIndex = -1;
    private Module.Category hoveredCategory = null;
    private Module hoveredModule = null;
    private int cachedCornerRadius = -1;
    private int cachedAccentColor = -1;
    private int cachedHoverColor = -1;
    private int cachedTextColor = -1;
    private float cachedPanelAlpha = -1;
    private KeybindSetting listeningKeybindSetting = null;
    private BlockSetting editingBlock = null;

    public ClickGuiScreen() {
        super(Text.literal("Radium ClickGUI"));
        this.animationStartTime = System.currentTimeMillis();
        this.isAnimating = true;

        RadiumClient.sendKeepAliveIfAllowed();

        if (CATEGORY_ICONS.isEmpty()) {
            CATEGORY_ICONS.put(Module.Category.CLIENT, Identifier.of("radium", "textures/client.png"));
            CATEGORY_ICONS.put(Module.Category.COMBAT, Identifier.of("radium", "textures/combat.png"));
            CATEGORY_ICONS.put(Module.Category.MISC, Identifier.of("radium", "textures/misc.png"));
            CATEGORY_ICONS.put(Module.Category.VISUAL, Identifier.of("radium", "textures/render.png"));
            CATEGORY_ICONS.put(Module.Category.DONUT, Identifier.of("radium", "textures/donut.png"));
        }


        int startX = 20;
        int startY = 20;
        int spacing = 140; // 115 + 25px gap

        setPosition(Module.Category.COMBAT, startX, startY);
        setPosition(Module.Category.VISUAL, startX + spacing, startY);
        setPosition(Module.Category.MISC, startX + spacing * 2, startY);
        setPosition(Module.Category.DONUT, startX + spacing * 3, startY);
        setPosition(Module.Category.CLIENT, startX + spacing * 4, startY);
        setPosition(Module.Category.SEARCH, startX + spacing * 5, startY);
    }

    private double scaleInput(double value) {
        return value * (client.getWindow().getScaleFactor() / 2.0);
    }

    private void setPosition(Module.Category cat, int x, int y) {
        categoryX.put(cat, x);
        categoryY.put(cat, y);
        categoryTargetX.put(cat, (float) x);
        categoryTargetY.put(cat, (float) y);
        categoryIsDragging.put(cat, false);
        categoryExpanded.put(cat, true);
    }

    @Override
    protected void init() {
        this.width = client.getWindow().getFramebufferWidth() / 2;
        this.height = client.getWindow().getFramebufferHeight() / 2;
        super.init();
        checkAndResetOverlappingCategories();
        constrainPanelPositions();
    }

    private void checkAndResetOverlappingCategories() {
        int startX = 20;
        int startY = 35;
        int spacing = 130;

        setPosition(Module.Category.COMBAT, startX, startY);
        setPosition(Module.Category.VISUAL, startX + spacing, startY);
        setPosition(Module.Category.MISC, startX + spacing * 2, startY);
        setPosition(Module.Category.DONUT, startX + spacing * 3, startY);
        setPosition(Module.Category.CLIENT, startX + spacing * 4, startY);
        setPosition(Module.Category.SEARCH, startX + spacing * 5, startY);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        mouseX = (int) scaleInput(mouseX);
        mouseY = (int) scaleInput(mouseY);
        com.radium.client.utils.render.RenderUtils.customScaledProjection(2.0);
        pulseAnimation += delta * 0.08f;
        if (pulseAnimation > 1f) pulseAnimation = 0f;

        float targetHover = (hoveredCategory != null || hoveredModuleIndex != -1) ? 1f : 0f;
        hoverAnimation += (targetHover - hoverAnimation) * 0.3f;

        this.renderBackground(context, mouseX, mouseY, delta);
        context.fill(0, 0, width, height, RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBackground(), RadiumGuiTheme.getPanelAlpha() * 0.6f));

        updateDragAnimations();

        super.render(context, mouseX, mouseY, delta);

        float animationProgress = 1.0f;
        if (isAnimating) {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            animationProgress = easeOutCubic(Math.min(1.0f, elapsed / (float) RadiumGuiTheme.ANIMATION_DURATION));
            if (animationProgress >= 1.0f) isAnimating = false;
        }

        hoveredCategory = null;
        hoveredModuleIndex = -1;
        hoveredModule = null;

        cacheThemeValues();

        for (Module.Category cat : Module.Category.values()) {
            renderCategory(context, cat, mouseX, mouseY, animationProgress);
        }

        if (selectedModule != null) {
            renderSettingsPanel(context, mouseX, mouseY, animationProgress);
        }

        renderTooltips(context, mouseX, mouseY, animationProgress);

        if (editingItem != null) {
            renderItemSelection(context, mouseX, mouseY, animationProgress);
        }

        if (colorPickerPanel.isOpen()) {
            colorPickerPanel.render(context, mouseX, mouseY, animationProgress);
        }

        if (stringListPanel.isOpen()) {
            stringListPanel.render(context, mouseX, mouseY, animationProgress);
        }


        if (enchantmentPanel.isOpen()) {
            enchantmentPanel.render(context, mouseX, mouseY, animationProgress);
        }

        if (blockSelectionPanel.isOpen()) {
            blockSelectionPanel.render(context, mouseX, mouseY, animationProgress);
        }

        if (renderingDropdown) {
            renderProfileDropdown(context, mouseX, mouseY, animationProgress);
            renderingDropdown = false;
        }
        RenderUtils.customScaledProjection(2.0);
    }

    private void cacheThemeValues() {
        com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.moduleManager.getModule(com.radium.client.modules.client.ClickGUI.class);
        cachedCornerRadius = (clickGUI != null && clickGUI.isRounded()) ? 12 : 0;
        cachedAccentColor = RadiumGuiTheme.getAccentColor();
        cachedHoverColor = RadiumGuiTheme.getHoverColor();
        cachedTextColor = RadiumGuiTheme.getTextColor();
        cachedPanelAlpha = RadiumGuiTheme.getPanelAlpha();
    }

    private void renderProfileDropdown(DrawContext context, int mouseX, int mouseY, float animationProgress) {
        if (selectedModule != null) {
            int yOffset = panelY + panelHeaderHeight + 10 - (int) settingsScrollOffset + settingHeight;
            for (Setting<?> setting : selectedModule.getSettings()) {
                if (setting instanceof ProfileSetting && ((ProfileSetting) setting).isExpanded()) {
                    renderProfileSetting(context, (ProfileSetting) setting, panelX + 10, yOffset, mouseX, mouseY, animationProgress);
                }
                yOffset += settingHeight;
            }
        }
    }

    @Override
    public void removed() {
        if (RadiumClient.configManager != null) {
            RadiumClient.configManager.saveProfile();
        }
    }

    @Override
    public void resize(MinecraftClient client, int width, int height) {
        int scaledWidth = client.getWindow().getFramebufferWidth() / 2;
        int scaledHeight = client.getWindow().getFramebufferHeight() / 2;
        super.resize(client, scaledWidth, scaledHeight);
    }

    private void constrainPanelPositions() {
        if (selectedModule != null) {
            int contentTopPadding = 10;
            int totalSettingsHeight = (selectedModule.getSettings().size() + 1) * settingHeight + contentTopPadding;
            int visibleContentHeight = Math.min(totalSettingsHeight, maxPanelHeight);
            int panelHeight = panelHeaderHeight + visibleContentHeight + 10;

            panelX = Math.max(0, Math.min(width - panelWidth, panelX));
            panelY = Math.max(0, Math.min(height - panelHeight, panelY));
            panelTargetX = panelX;
            panelTargetY = panelY;
        } else {
            panelX = Math.max(0, Math.min(width - panelWidth, panelX));
            panelY = Math.max(0, Math.min(height - panelHeaderHeight - 10, panelY));
            panelTargetX = panelX;
            panelTargetY = panelY;
        }

        itemPanelX = Math.max(0, Math.min(width - itemPanelWidth, itemPanelX));
        itemPanelY = Math.max(0, Math.min(height - itemPanelHeight, itemPanelY));
        itemPanelTargetX = itemPanelX;
        itemPanelTargetY = itemPanelY;

        for (Module.Category cat : Module.Category.values()) {
            int x = categoryX.getOrDefault(cat, 0);
            int y = categoryY.getOrDefault(cat, 0);

            x = Math.max(0, Math.min(width - categoryWidth, x));
            y = Math.max(0, Math.min(height - headerHeight, y));

            categoryX.put(cat, x);
            categoryY.put(cat, y);
            categoryTargetX.put(cat, (float) x);
            categoryTargetY.put(cat, (float) y);
        }
    }

    private void updateDragAnimations() {
        long currentTime = System.currentTimeMillis();

        long dragAnimationDuration = 300;
        for (Module.Category cat : Module.Category.values()) {
            if (categoryIsDragging.get(cat) && !draggingCategory) {
                long elapsed = currentTime - categoryDragStartTime.getOrDefault(cat, currentTime);
                float progress = Math.min(1.0f, elapsed / (float) dragAnimationDuration);
                progress = easeOutCubic(progress);

                float targetX = categoryTargetX.get(cat);
                float targetY = categoryTargetY.get(cat);
                float currentX = categoryX.get(cat);
                float currentY = categoryY.get(cat);

                float newX = currentX + (targetX - currentX) * progress;
                float newY = currentY + (targetY - currentY) * progress;

                categoryX.put(cat, Math.round(newX));
                categoryY.put(cat, Math.round(newY));

                if (progress >= 1.0f) {
                    categoryIsDragging.put(cat, false);
                }
            }
        }

        if (panelIsDragging && !draggingPanel) {
            long elapsed = currentTime - panelDragStartTime;
            float progress = Math.min(1.0f, elapsed / (float) dragAnimationDuration);
            progress = easeOutCubic(progress);

            float newX = panelX + (panelTargetX - panelX) * progress;
            float newY = panelY + (panelTargetY - panelY) * progress;

            panelX = Math.round(newX);
            panelY = Math.round(newY);

            if (progress >= 1.0f) {
                panelIsDragging = false;
            }
        }

        if (itemPanelIsDragging && !draggingItemPanel) {
            long elapsed = currentTime - itemPanelDragStartTime;
            float progress = Math.min(1.0f, elapsed / (float) dragAnimationDuration);
            progress = easeOutCubic(progress);

            float newX = itemPanelX + (itemPanelTargetX - itemPanelX) * progress;
            float newY = itemPanelY + (itemPanelTargetY - itemPanelY) * progress;

            itemPanelX = Math.round(newX);
            itemPanelY = Math.round(newY);

            if (progress >= 1.0f) {
                itemPanelIsDragging = false;
            }
        }
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private void renderCategory(DrawContext context, Module.Category cat, int mouseX, int mouseY, float animationProgress) {
        int x = categoryX.get(cat);
        int y = categoryY.get(cat);
        boolean expanded = categoryExpanded.get(cat);
        int cornerRadius = cachedCornerRadius;

        if (cat == Module.Category.SEARCH) {
            renderSearchCategory(context, cat, x, y, mouseX, mouseY, animationProgress);
            return;
        }

        int categoryHeight = headerHeight;
        if (expanded) {
            categoryHeight += getModules(cat).size() * moduleHeight + 2;
        }

        boolean isDragging = draggingCategory && dragCategory == cat;
        boolean isHovered = isHovered(mouseX, mouseY, x, y, categoryWidth, headerHeight);

        if (isHovered) {
            hoveredCategory = cat;
        }


        int categoryBgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryBackground(), animationProgress * 0.4f);

        if (isDragging) {
            categoryBgColor = blendColors(categoryBgColor, cachedAccentColor, 0.15f);
        } else if (isHovered) {
            categoryBgColor = blendColors(categoryBgColor, cachedAccentColor, 0.08f);
        }

        drawRoundedRect(context, x, y, categoryWidth, categoryHeight, cornerRadius, categoryBgColor);

        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), animationProgress * 0.6f);

        if (isDragging) {
            borderColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress * 0.8f);
        } else if (isHovered) {
            borderColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress * 0.6f);
        }


        boolean headerHovered = isHovered(mouseX, mouseY, x, y, categoryWidth, headerHeight);
        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryHeader(), animationProgress * cachedPanelAlpha);

        if (expanded) {
            drawRoundedRectTop(context, x, y, categoryWidth, headerHeight, cornerRadius, headerColor);
        } else {
            drawRoundedRect(context, x, y, categoryWidth, headerHeight, cornerRadius, headerColor);
        }


        int iconSize = 14;
        int iconPadding = 6;
        Identifier iconId = CATEGORY_ICONS.get(cat);

        com.radium.client.modules.client.ClickGUI clickGUI = RadiumClient.moduleManager.getModule(com.radium.client.modules.client.ClickGUI.class);
        boolean showIcons = clickGUI != null && clickGUI.icons.getValue();

        if (iconId != null && showIcons) {
            int iconX = x + iconPadding;
            int iconY = y + (headerHeight - iconSize) / 2;


            int accentColor = cachedAccentColor;
            float r = ((accentColor >> 16) & 0xFF) / 255.0f;
            float g = ((accentColor >> 8) & 0xFF) / 255.0f;
            float b = (accentColor & 0xFF) / 255.0f;
            context.setShaderColor(r, g, b, animationProgress);
            context.drawTexture(iconId, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        }

        String categoryName = cat.getName().toUpperCase();
        int textY = y + (headerHeight - 8) / 2;
        int baseTextWidth = textRenderer.getWidth(categoryName);


        int textX = x + (categoryWidth - baseTextWidth) / 2;

        if (headerHovered) {
            float pulseScale = 1f + 0.08f * (float) Math.sin(pulseAnimation * Math.PI * 2);
            int scaledTextWidth = (int) (baseTextWidth * pulseScale);
            textX = x + (categoryWidth - scaledTextWidth) / 2;
        }

        context.drawText(textRenderer, categoryName, textX, textY,
                RadiumGuiTheme.applyAlpha(cachedTextColor, (int) (animationProgress * 255)) | 0xFF000000, false);

        String indicator = expanded ? "-" : "+";
        int indicatorX = x + categoryWidth - 12;
        context.drawText(textRenderer, indicator, indicatorX, textY,
                RadiumGuiTheme.applyAlpha(cachedTextColor, (int) (animationProgress * 255)) | 0xFF000000, false);

        if (expanded) {
            context.fill(x + 2, y + headerHeight, x + categoryWidth - 2, y + headerHeight + 1,
                    RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSeparatorColor(), animationProgress));
        }

        if (expanded) {
            int offsetY = y + headerHeight + 2;
            List<Module> modules = getModules(cat);
            for (int i = 0; i < modules.size(); i++) {
                Module m = modules.get(i);
                boolean isLast = (i == modules.size() - 1);
                renderModule(context, m, x, offsetY, mouseX, mouseY, animationProgress, isLast, i);
                offsetY += moduleHeight;
            }
        }
    }

    private int blendColors(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void renderModule(DrawContext context, Module module, int x, int y, int mouseX, int mouseY, float animationProgress, boolean isLast, int index) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, categoryWidth, moduleHeight);
        int cornerRadius = cachedCornerRadius;

        if (hovered) {
            hoveredModuleIndex = index;
            hoveredModule = module;
        }

        int bg;
        if (module.isEnabled()) {
            bg = RadiumGuiTheme.applyAlpha(cachedAccentColor, (int) (animationProgress * 255));
        } else if (hovered) {
            bg = RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA);
        } else {
            bg = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getModuleBackground(), animationProgress * cachedPanelAlpha);
        }

        if (isLast) {
            drawRoundedRectBottom(context, x, y, cornerRadius, bg);
        } else {
            context.fill(x, y, x + categoryWidth, y + moduleHeight, bg);
        }

        int textColor = module.isEnabled() ? 0xFFFFFFFF : RadiumGuiTheme.getDisabledTextColor();
        textColor = RadiumGuiTheme.applyAlpha(textColor, (int) (animationProgress * 255)) | 0xFF000000;

        int textX = x + 8;
        int textY = y + (moduleHeight - 8) / 2;

        String moduleName = module.getName();
        int indicatorWidth = textRenderer.getWidth("...") + 4;
        int availableWidth = categoryWidth - 20 - indicatorWidth;
        if (textRenderer.getWidth(moduleName) > availableWidth) {
            moduleName = textRenderer.trimToWidth(moduleName, availableWidth - textRenderer.getWidth("...")) + "...";
        }
        context.drawText(textRenderer, moduleName, textX, textY, textColor, false);

        String indicator = "...";
        int indicatorX = x + categoryWidth - indicatorWidth;
        context.drawText(textRenderer, indicator, indicatorX, textY, textColor, false);
    }

    private void renderModuleTooltip(DrawContext context, Module module, int mouseX, int mouseY, float animationProgress) {
        String description = module.getDescription();
        if (description == null || description.trim().isEmpty()) {
            description = "No description available";
        }

        String moduleName = module.getName();
        int moduleNameWidth = textRenderer.getWidth(moduleName);


        List<String> lines = wrapText(description, 250);
        int maxLineWidth = lines.stream().mapToInt(textRenderer::getWidth).max().orElse(0);

        int contentWidth = Math.max(moduleNameWidth, maxLineWidth);
        int tooltipWidth = Math.max(120, contentWidth + 16);

        int tooltipHeight = 14 + 4 + (lines.size() * 10) + 8;

        int tooltipX = mouseX + 10;
        int tooltipY = mouseY - tooltipHeight - 5;

        int screenWidth = client.getWindow().getWidth();
        int screenHeight = client.getWindow().getHeight();


        if (tooltipX + tooltipWidth > screenWidth) {
            tooltipX = mouseX - tooltipWidth - 10;
        }
        if (tooltipY < 0) {
            tooltipY = mouseY + 20;
        }
        if (tooltipX < 0) {
            tooltipX = 5;
        }


        int tooltipBgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSettingsPanelColor(), animationProgress * 0.9f);
        drawRoundedRect(context, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 6, tooltipBgColor);


        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), animationProgress * 0.8f);
        drawRoundedRectOutline(context, tooltipX, tooltipY, tooltipWidth, tooltipHeight, 6, borderColor);


        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), (int) (animationProgress * 255)) | 0xFF000000;
        context.drawText(textRenderer, moduleName, tooltipX + 8, tooltipY + 4, headerColor, false);


        int separatorY = tooltipY + 16;
        int separatorColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSeparatorColor(), animationProgress);
        context.fill(tooltipX + 8, separatorY, tooltipX + tooltipWidth - 8, separatorY + 1, separatorColor);


        int textColor = RadiumGuiTheme.applyAlpha(0xFFCCCCCC, (int) (animationProgress * 255)) | 0xFF000000;
        int textY = separatorY + 4;
        for (String line : lines) {
            context.drawText(textRenderer, line, tooltipX + 8, textY, textColor, false);
            textY += 10;
        }
    }

    private List<String> wrapText(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            if (textRenderer.getWidth(testLine) <= maxWidth) {
                currentLine = new StringBuilder(testLine);
            } else {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    lines.add(word);
                }
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    private void renderTooltips(DrawContext context, int mouseX, int mouseY, float animationProgress) {
        if (hoveredModule != null) {
            renderModuleTooltip(context, hoveredModule, mouseX, mouseY, animationProgress);
        }
    }

    private void renderSettingsPanel(DrawContext context, int mouseX, int mouseY, float animationProgress) {
        if (selectedModule == null) return;

        int cornerRadius = cachedCornerRadius;

        int contentTopPadding = 10;
        int totalSettingsHeight = (selectedModule.getSettings().size() + 1) * settingHeight + contentTopPadding;
        int visibleContentHeight = Math.min(totalSettingsHeight, maxPanelHeight);
        int panelHeight = panelHeaderHeight + visibleContentHeight + 10;

        maxScroll = Math.max(0, totalSettingsHeight - visibleContentHeight);
        settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, maxScroll));

        boolean isDragging = draggingPanel;
        boolean isHovered = isHovered(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);


        int panelColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSettingsPanelColor(), animationProgress * cachedPanelAlpha);
        if (isDragging) {
            panelColor = blendColors(panelColor, cachedAccentColor, 0.08f);
        } else if (isHovered) {
            panelColor = blendColors(panelColor, cachedAccentColor, 0.04f);
        }

        drawRoundedRect(context, panelX, panelY, panelWidth, panelHeight, cornerRadius, panelColor);

        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), animationProgress * 0.6f);
        if (isDragging) {
            borderColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress * 0.8f);
        } else if (isHovered) {
            borderColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress * 0.6f);
        }


        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryHeader(), animationProgress * cachedPanelAlpha);
        drawRoundedRectTop(context, panelX, panelY, panelWidth, panelHeaderHeight, cornerRadius, headerColor);

        String headerText = selectedModule.getName() + " Settings";
        int headerTextColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;
        context.drawText(textRenderer, headerText, panelX + 8, panelY + 6, headerTextColor, false);

        context.drawText(textRenderer, "X", panelX + panelWidth - 15, panelY + 6,
                RadiumGuiTheme.applyAlpha(cachedAccentColor, (int) (animationProgress * 255)) | 0xFF000000, false);

        int contentY = panelY + panelHeaderHeight;
        context.enableScissor(panelX, contentY, panelX + panelWidth, contentY + visibleContentHeight);

        yOffset = contentTopPadding;
        renderKeybindSetting(context, null, panelX + 10, contentY + yOffset - (int) settingsScrollOffset, mouseX, mouseY, animationProgress);
        yOffset += settingHeight;

        for (var setting : selectedModule.getSettings()) {
            renderSetting(context, setting, panelX + 10, contentY + yOffset - (int) settingsScrollOffset, mouseX, mouseY, animationProgress);
            yOffset += settingHeight;
        }

        context.disableScissor();

        if (maxScroll > 0) {
            int scrollBarX = panelX + panelWidth - 6;
            int scrollBarWidth = 4;


            drawRoundedRect(context, scrollBarX, contentY, scrollBarWidth, visibleContentHeight, 2, 0x70000000);

            float scrollProgress = settingsScrollOffset / maxScroll;
            int totalDrawableHeight = (selectedModule.getSettings().size() + 1) * settingHeight + contentTopPadding;
            int handleHeight = (int) (((float) visibleContentHeight / totalDrawableHeight) * visibleContentHeight);
            int handleY = contentY + (int) ((visibleContentHeight - handleHeight) * scrollProgress);

            int handleColor = cachedAccentColor;
            drawRoundedRect(context, scrollBarX, handleY, scrollBarWidth, handleHeight, 2, handleColor);
        }
    }

    private void renderItemSelection(DrawContext context, int mouseX, int mouseY, float animationProgress) {
        int cornerRadius = cachedCornerRadius;

        int bgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSettingsPanelColor(), animationProgress * RadiumGuiTheme.getPanelAlpha());
        RenderUtils.fillRoundRect(context, itemPanelX, itemPanelY, itemPanelWidth, itemPanelHeight, cornerRadius, bgColor);

        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), animationProgress * 0.4f);
        RenderUtils.drawRoundRect(context, itemPanelX, itemPanelY, itemPanelWidth, itemPanelHeight, cornerRadius, borderColor);

        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryHeader(), animationProgress * RadiumGuiTheme.getPanelAlpha());
        RenderUtils.fillRoundTabTop(context, itemPanelX, itemPanelY, itemPanelWidth, 30, cornerRadius, headerColor);

        String title = "Select Item";
        int titleColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;
        context.drawText(textRenderer, title, itemPanelX + 10, itemPanelY + 10, titleColor, true);

        String closeText = "✕";
        boolean closeHovered = isHovered(mouseX, mouseY, itemPanelX + itemPanelWidth - 20, itemPanelY + 8, 16, 16);
        int closeColor = closeHovered ? RadiumGuiTheme.getAccentColor() : 0xFFCCCCCC;
        context.drawText(textRenderer, closeText, itemPanelX + itemPanelWidth - 18, itemPanelY + 10,
                RadiumGuiTheme.applyAlpha(closeColor, (int) (animationProgress * 255)) | 0xFF000000, false);

        int searchBarX = itemPanelX + 10;
        int searchBarY = itemPanelY + 35;
        int searchBarWidth = itemPanelWidth - 20;
        int searchBarHeight = 20;
        boolean searchHovered = isHovered(mouseX, mouseY, searchBarX, searchBarY, searchBarWidth, searchBarHeight);
        int searchBarColor = (itemSearchEditor.isActive() || searchHovered) ?
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), animationProgress * 0.5f) :
                RadiumGuiTheme.applyAlpha(0x00000000, 0f);

        int subRadius = Math.max(2, cornerRadius / 2);
        RenderUtils.fillRoundRect(context, searchBarX, searchBarY, searchBarWidth, searchBarHeight, subRadius, searchBarColor);

        String searchText = itemSearchEditor.getText();
        if (itemSearchEditor.isActive()) {
            int cursorPos = itemSearchEditor.getCursorPosition();
            String beforeCursor = searchText.substring(0, cursorPos);
            String afterCursor = searchText.substring(cursorPos);
            searchText = beforeCursor + "_" + afterCursor;
        } else if (searchText.isEmpty()) {
            searchText = "Search for an item...";
        }
        context.drawText(textRenderer, searchText, searchBarX + 5, searchBarY + 6, titleColor, false);

        int itemsStartY = itemPanelY + 60;
        int itemsHeight = itemPanelHeight - 70;


        int ITEM_SIZE = 20;
        int ITEM_PADDING = 3;
        int ITEMS_PER_ROW = 6;


        RenderUtils.fillRoundRect(context, itemPanelX + 5, itemsStartY, itemPanelWidth - 10, itemsHeight, subRadius,
                RadiumGuiTheme.applyAlpha(0x00000000, 0f));

        context.enableScissor(itemPanelX + 5, itemsStartY, itemPanelX + itemPanelWidth - 5, itemsStartY + itemsHeight);

        List<Item> allItems = Registries.ITEM.stream()
                .filter(item -> item != Items.AIR && item.getName().getString().toLowerCase().contains(itemSearchEditor.getText().toLowerCase()))
                .toList();

        int totalRows = (int) Math.ceil((double) allItems.size() / ITEMS_PER_ROW);
        int visibleRows = itemsHeight / (ITEM_SIZE + ITEM_PADDING);
        int maxRowScroll = Math.max(0, totalRows - visibleRows);
        int rowScrollOffset = Math.max(0, Math.min(itemScrollOffset / ITEMS_PER_ROW, maxRowScroll));

        int startIndex = rowScrollOffset * ITEMS_PER_ROW;
        int endIndex = Math.min(startIndex + (visibleRows * ITEMS_PER_ROW), allItems.size());


        int gridWidth = (ITEMS_PER_ROW * ITEM_SIZE) + ((ITEMS_PER_ROW - 1) * ITEM_PADDING);
        int gridStartX = itemPanelX + (itemPanelWidth - gridWidth) / 2;

        for (int i = startIndex; i < endIndex; i++) {
            Item item = allItems.get(i);
            int row = (i - startIndex) / ITEMS_PER_ROW;
            int col = (i - startIndex) % ITEMS_PER_ROW;

            int itemX = gridStartX + col * (ITEM_SIZE + ITEM_PADDING);
            int itemY = itemsStartY + 5 + row * (ITEM_SIZE + ITEM_PADDING);

            boolean itemHovered = isHovered(mouseX, mouseY, itemX, itemY, ITEM_SIZE, ITEM_SIZE);
            boolean selected = item == editingItem.getItem();

            int itemBgColor;
            if (selected) {
                itemBgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), animationProgress * 0.4f);
            } else if (itemHovered) {
                itemBgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getHoverColor(), animationProgress * 0.3f);
            } else {
                itemBgColor = RadiumGuiTheme.applyAlpha(0x00000000, 0f);
            }

            if (itemBgColor != 0) {
                int itemRadius = 2;
                RenderUtils.fillRoundRect(context, itemX, itemY, ITEM_SIZE, ITEM_SIZE, itemRadius, itemBgColor);
            }


            if (selected) {
                int itemBorderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), animationProgress * 0.9f);
                RenderUtils.drawRoundRect(context, itemX, itemY, ITEM_SIZE, ITEM_SIZE, 2, itemBorderColor);
            }

            ItemStack itemStack = new ItemStack(item);
            context.drawItem(itemStack, itemX, itemY);
        }

        context.disableScissor();

        if (maxRowScroll > 0) {
            int scrollBarX = itemPanelX + itemPanelWidth - 12;
            int scrollBarY = itemsStartY + 5;
            int scrollBarHeight = itemsHeight - 10;
            int scrollBarWidth = 4;

            int scrollBarBgColor = RadiumGuiTheme.applyAlpha(0x50000000, animationProgress);
            RenderUtils.fillRoundRect(context, scrollBarX, scrollBarY, scrollBarWidth, scrollBarHeight, 2, scrollBarBgColor);

            float scrollProgress = maxRowScroll > 0 ? (float) rowScrollOffset / maxRowScroll : 0f;
            int handleHeight = Math.max(20, (int) ((visibleRows / (float) totalRows) * scrollBarHeight));
            int handleY = scrollBarY + (int) ((scrollBarHeight - handleHeight) * scrollProgress);

            int handleColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), animationProgress);
            RenderUtils.fillRoundRect(context, scrollBarX, handleY, scrollBarWidth, handleHeight, 2, handleColor);
        }
    }

    private void renderKeybindSetting(DrawContext context, KeybindSetting setting, int x, int y, int mouseX, int mouseY, float animationProgress) {
        int verticalCenter = y + (settingHeight - 8) / 2;
        int textColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;

        boolean hovered = isHovered(mouseX, mouseY, x, y, panelWidth - 20, settingHeight);
        if (hovered) {
            int bgColor = RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA);
            drawRoundedRect(context, x, y, panelWidth - 20, settingHeight, 3, bgColor);
        }


        String labelText = (setting != null) ? setting.getName() : "Keybind";
        context.drawText(textRenderer, labelText, x, verticalCenter, textColor, false);


        String buttonText;
        int buttonColor;
        boolean isListening = false;

        if (setting != null) {
            isListening = listeningForKeybind && listeningKeybindSetting == setting;
            if (isListening) {
                buttonText = "Press a key...";
                buttonColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress);
            } else {
                buttonText = KeybindManager.getKeyName(setting.getValue());
                buttonColor = RadiumGuiTheme.applyAlpha(0xFF555555, animationProgress * cachedPanelAlpha);
            }
        } else {
            isListening = listeningForKeybind && listeningKeybindSetting == null && keybindModule == selectedModule;
            if (isListening) {
                buttonText = "Press a key...";
                buttonColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress);
            } else {
                buttonText = KeybindManager.getKeyName(selectedModule.getKeyBind());
                buttonColor = RadiumGuiTheme.applyAlpha(0xFF555555, animationProgress * cachedPanelAlpha);
            }
        }


        int buttonHeight = settingHeight - 4;
        int buttonWidth = Math.max(60, textRenderer.getWidth(buttonText) + 10);
        int buttonX = x + panelWidth - 30 - buttonWidth;
        int buttonY = y + 2;


        drawRoundedRect(context, buttonX, buttonY, buttonWidth, buttonHeight, 5, buttonColor);


        int buttonTextX = buttonX + (buttonWidth - textRenderer.getWidth(buttonText)) / 2;
        int buttonTextY = buttonY + (buttonHeight - 8) / 2;
        context.drawText(textRenderer, buttonText, buttonTextX, buttonTextY, 0xFFFFFFFF, false);
    }

    private void renderSetting(DrawContext context, Object setting, int x, int y, int mouseX, int mouseY, float animationProgress) {
        int textColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;
        int verticalCenter = y + (settingHeight - 8) / 2;

        if (setting instanceof BooleanSetting b) {
            if (
                    b.getName().equals("Execute Action") ||
                            b.getName().equals("Set Pos 1") ||
                            b.getName().equals("Set Pos 2") ||
                            b.getName().equals("Start Mining") ||
                            b.getName().equals("Stop Mining")
            ) {
                renderExecuteButton(context, b, x, y, mouseX, mouseY, animationProgress);
                yOffset += 4;
                return;
            }
            int boxSize = 12;
            int boxY = y + (settingHeight - boxSize) / 2;
            int boxColor = b.getValue() ?
                    RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress) :
                    RadiumGuiTheme.applyAlpha(0xFF555555, animationProgress * cachedPanelAlpha);

            drawRoundedRect(context, x, boxY, boxSize, boxSize, 4, boxColor);

            context.drawText(textRenderer, b.getName(), x + boxSize + 8, verticalCenter, textColor, false);

        } else if (setting instanceof SliderSetting s) {
            renderSlider(context, s, x, y, mouseX, mouseY, animationProgress);

        } else if (setting instanceof NumberSetting n) {
            renderNumberSlider(context, n, x, y, mouseX, mouseY, animationProgress);

        } else if (setting instanceof StringSetting s) {
            renderStringSetting(context, s, x, y, mouseX, mouseY, animationProgress, textColor, verticalCenter);

        } else if (setting instanceof ItemSetting itemSetting) {
            renderItemSetting(context, itemSetting, x, y, mouseX, mouseY, animationProgress, textColor, verticalCenter);

        } else if (setting instanceof BlockSetting blockSetting) {
            renderBlockSetting(context, blockSetting, x, y, mouseX, mouseY, animationProgress, textColor, verticalCenter);

        } else if (setting instanceof ModeSetting<?> modeSetting) {
            renderModeSetting(context, modeSetting, x, y, mouseX, mouseY, animationProgress, textColor, verticalCenter);

        } else if (setting instanceof EnchantmentSetting enchantSetting) {
            renderEnchantmentSetting(context, enchantSetting, x, y, mouseX, mouseY, animationProgress, textColor, verticalCenter);

        } else if (setting instanceof KeybindSetting keybindSetting) {
            renderKeybindSetting(context, keybindSetting, x, y, mouseX, mouseY, animationProgress);

        } else if (setting instanceof DoubleSetting d) {
            renderDoubleSlider(context, d, x, y, mouseX, mouseY, animationProgress);

        } else if (setting instanceof ColorSetting colorSetting) {
            renderColorSetting(context, colorSetting, x, y, mouseX, mouseY, animationProgress, textColor, verticalCenter);

        } else if (setting instanceof StringListSetting stringListSetting) {
            renderStringListSetting(context, stringListSetting, x, y, mouseX, mouseY, animationProgress, textColor, verticalCenter);

        } else if (setting instanceof ProfileSetting profileSetting) {
            if (profileSetting.isExpanded()) {
                renderingDropdown = true;
            } else {
                renderProfileSetting(context, profileSetting, x, y, mouseX, mouseY, animationProgress);
            }
        }
    }

    private void renderBlockSetting(DrawContext context, BlockSetting blockSetting, int x, int y, int mouseX, int mouseY, float animationProgress, int textColor, int verticalCenter) {
        String text = blockSetting.getName() + ": ";
        context.drawText(textRenderer, text, x, verticalCenter, textColor, false);
        String valueText = blockSetting.getBlocks().isEmpty() ? "None selected" : blockSetting.getBlocks().size() + " block" + (blockSetting.getBlocks().size() != 1 ? "s" : "") + " selected";
        int valueX = x + textRenderer.getWidth(text);
        int boxHeight = settingHeight - 2;
        int boxWidth = panelWidth - 20 - textRenderer.getWidth(text);
        boolean hovered = isHovered(mouseX, mouseY, valueX - 2, y + 1, boxWidth + 4, boxHeight);
        int boxColor = hovered ?
                RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);
        drawRoundedRect(context, valueX - 2, y + 1, boxWidth + 4, boxHeight, 3, boxColor);
        context.drawText(textRenderer, valueText, valueX, verticalCenter, textColor, false);
    }

    private void renderStringSetting(DrawContext context, StringSetting s, int x, int y, int mouseX, int mouseY, float animationProgress, int textColor, int verticalCenter) {
        String text = s.getName() + ": ";
        context.drawText(textRenderer, text, x, verticalCenter, textColor, false);

        String valueText;
        if (editingString == s && stringEditor.isActive()) {
            String editorText = stringEditor.getText();
            int cursorPos = stringEditor.getCursorPosition();
            String beforeCursor = editorText.substring(0, cursorPos);
            String afterCursor = editorText.substring(cursorPos);
            valueText = beforeCursor + "_" + afterCursor;
        } else {
            valueText = s.getValue();
        }
        if (s.getValue().isEmpty() && editingString != s) {
            valueText = "Click to edit...";
        }

        int valueX = x + textRenderer.getWidth(text);
        int boxHeight = settingHeight - 2;
        int boxWidth = panelWidth - 20 - textRenderer.getWidth(text);

        boolean boxHovered = isHovered(mouseX, mouseY, valueX - 2, y + 1, boxWidth + 4, boxHeight);
        int boxColor = (editingString == s || boxHovered) ?
                RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);

        drawRoundedRect(context, valueX - 2, y + 1, boxWidth + 4, boxHeight, 3, boxColor);

        context.drawText(textRenderer, valueText, valueX, verticalCenter, textColor, false);
    }

    private void renderItemSetting(DrawContext context, ItemSetting itemSetting, int x, int y, int mouseX, int mouseY, float animationProgress, int textColor, int verticalCenter) {
        String text = itemSetting.getName() + ": ";
        context.drawText(textRenderer, text, x, verticalCenter, textColor, false);

        String valueText = itemSetting.getItem().getName().getString();
        int valueX = x + textRenderer.getWidth(text);
        int boxHeight = settingHeight - 2;
        int boxWidth = panelWidth - 20 - textRenderer.getWidth(text);

        boolean hovered = isHovered(mouseX, mouseY, valueX - 2, y + 1, boxWidth + 4, boxHeight);
        int boxColor = hovered ?
                RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);

        drawRoundedRect(context, valueX - 2, y + 1, boxWidth + 4, boxHeight, 3, boxColor);

        context.drawText(textRenderer, valueText, valueX, verticalCenter, textColor, false);
    }

    private void renderModeSetting(DrawContext context, ModeSetting<?> modeSetting, int x, int y, int mouseX, int mouseY, float animationProgress, int textColor, int verticalCenter) {
        String text = modeSetting.getName() + ": ";
        context.drawText(textRenderer, text, x, verticalCenter, textColor, false);

        String valueText = modeSetting.getValue().toString();
        int valueX = x + textRenderer.getWidth(text);
        int boxHeight = settingHeight - 2;
        int boxWidth = panelWidth - 20 - textRenderer.getWidth(text);

        boolean hovered = isHovered(mouseX, mouseY, valueX - 2, y + 1, boxWidth + 4, boxHeight);
        int boxColor = hovered ?
                RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);

        drawRoundedRect(context, valueX - 2, y + 1, boxWidth + 4, boxHeight, 3, boxColor);

        context.drawText(textRenderer, valueText, valueX, verticalCenter, textColor, false);
    }

    private void renderEnchantmentSetting(DrawContext context, EnchantmentSetting enchantSetting, int x, int y, int mouseX, int mouseY, float animationProgress, int textColor, int verticalCenter) {
        String text = enchantSetting.getName() + ": ";
        context.drawText(textRenderer, text, x, verticalCenter, textColor, false);


        int totalSelected = enchantSetting.getTotalCount();

        String valueText = totalSelected == 0 ? "None" : totalSelected + " selected";
        int valueX = x + textRenderer.getWidth(text);
        int boxHeight = settingHeight - 2;
        int boxWidth = panelWidth - 20 - textRenderer.getWidth(text);

        boolean hovered = isHovered(mouseX, mouseY, valueX - 2, y + 1, boxWidth + 4, boxHeight);
        int boxColor = hovered ?
                RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);

        drawRoundedRect(context, valueX - 2, y + 1, boxWidth + 4, boxHeight, 3, boxColor);

        context.drawText(textRenderer, valueText, valueX, verticalCenter, textColor, false);
    }

    private void renderColorSetting(DrawContext context, ColorSetting colorSetting, int x, int y, int mouseX, int mouseY, float animationProgress, int textColor, int verticalCenter) {
        String text = colorSetting.getName() + ": ";
        context.drawText(textRenderer, text, x, verticalCenter, textColor, false);

        java.awt.Color color = colorSetting.getValue();
        String valueText = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());

        int valueX = x + textRenderer.getWidth(text);
        int boxHeight = settingHeight - 2;
        int boxWidth = panelWidth - 20 - textRenderer.getWidth(text);
        int colorPreviewSize = boxHeight - 4;

        boolean hovered = isHovered(mouseX, mouseY, valueX - 2, y + 1, boxWidth + 4, boxHeight);
        int boxColor = hovered ?
                RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);

        drawRoundedRect(context, valueX - 2, y + 1, boxWidth + 4, boxHeight, 3, boxColor);

        int colorInt = (255 << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        drawRoundedRect(context, valueX + 2, y + 3, colorPreviewSize, colorPreviewSize, 2,
                RadiumGuiTheme.applyAlpha(colorInt, animationProgress));

        int textX = valueX + colorPreviewSize + 6;
        context.drawText(textRenderer, valueText, textX, verticalCenter, textColor, false);
    }

    private void renderStringListSetting(DrawContext context, StringListSetting setting, int x, int y, int mouseX, int mouseY, float animationProgress, int textColor, int verticalCenter) {
        String text = setting.getName() + ": ";
        context.drawText(textRenderer, text, x, verticalCenter, textColor, false);

        String valueText = setting.size() + " item" + (setting.size() != 1 ? "s" : "");
        if (setting.isEmpty()) {
            valueText = "Click to edit...";
        }

        int valueX = x + textRenderer.getWidth(text);
        int boxHeight = settingHeight - 2;
        int boxWidth = panelWidth - 20 - textRenderer.getWidth(text);

        boolean hovered = isHovered(mouseX, mouseY, valueX - 2, y + 1, boxWidth + 4, boxHeight);
        int boxColor = hovered ?
                RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);

        drawRoundedRect(context, valueX - 2, y + 1, boxWidth + 4, boxHeight, 3, boxColor);
        context.drawText(textRenderer, valueText, valueX, verticalCenter, textColor, false);
    }

    private void renderExecuteButton(DrawContext context, BooleanSetting setting, int x, int y, int mouseX, int mouseY, float animationProgress) {
        boolean hovered = isHovered(mouseX, mouseY, x, y, panelWidth - 20, settingHeight);

        int color = hovered ? cachedAccentColor : 0xFF555555;
        drawRoundedRect(context, x, y, panelWidth - 20, settingHeight, 5, RadiumGuiTheme.applyAlpha(color, animationProgress));

        context.drawCenteredTextWithShadow(textRenderer, setting.getName(), x + (panelWidth - 20) / 2, y + (settingHeight - 8) / 2, 0xFFFFFFFF);
    }

    private void renderProfileSetting(DrawContext context, ProfileSetting setting, int x, int y, int mouseX, int mouseY, float animationProgress) {
        int textColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;
        int verticalCenter = y + (settingHeight - 8) / 2;
        String text = setting.getName() + ": ";
        context.drawText(textRenderer, text, x, verticalCenter, textColor, false);

        String valueText = setting.getValue();
        int textWidth = textRenderer.getWidth(text);
        int valueX = x + textWidth;
        int boxHeight = settingHeight - 2;
        int boxWidth = panelWidth - 20 - textWidth;

        boolean hovered = isHovered(mouseX, mouseY, valueX - 2, y + 1, boxWidth + 4, boxHeight);
        int boxColor = hovered || setting.isExpanded() ?
                RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);

        drawRoundedRect(context, valueX - 2, y + 1, boxWidth + 4, boxHeight, 3, boxColor);

        context.drawText(textRenderer, valueText, valueX, verticalCenter, textColor, false);

        if (setting.isExpanded()) {
            int dropdownY = y + boxHeight + 3;
            int dropdownHeight = setting.getProfiles().size() * 12 + 2;
            drawRoundedRect(context, valueX - 2, dropdownY, boxWidth + 4, dropdownHeight, 3, 0xFF202020);

            int itemY = dropdownY + 2;
            for (String profile : setting.getProfiles()) {
                boolean itemHovered = isHovered(mouseX, mouseY, valueX - 2, itemY, boxWidth + 4, 12);
                if (itemHovered) {
                    context.fill(valueX - 2, itemY, valueX - 2 + boxWidth + 4, itemY + 12, RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress));
                }
                context.drawText(textRenderer, profile, valueX, itemY + 2, textColor, false);
                itemY += 12;
            }
        }
    }

    private void renderSlider(DrawContext context, SliderSetting setting, int x, int y, int mouseX, int mouseY, float animationProgress) {
        String nameText = setting.getName();

        String valueText = String.valueOf((long) setting.getValue().doubleValue());
        int valueWidth = textRenderer.getWidth(valueText);

        int textColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;

        context.drawText(textRenderer, nameText, x, y + 2, textColor, false);
        context.drawText(textRenderer, valueText, x + 150 - valueWidth, y + 2, textColor, false);

        int sliderY = y + 12;

        int bgColor = RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);
        drawRoundedRect(context, x, sliderY, 150, 6, 3, bgColor);

        double position = setting.getSliderPosition();
        int filledWidth = (int) (150 * position);

        if (filledWidth > 0) {
            int fillColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress);
            drawRoundedRect(context, x, sliderY, filledWidth, 6, 3, fillColor);
        }

        int handleX = x + filledWidth - 3;
        int handleColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, animationProgress);
        drawRoundedRect(context, handleX, sliderY - 2, 6, 10, 3, handleColor);

        if (draggingSlider && draggedSlider == setting) {
            if (isHovered(mouseX, mouseY, x, y, 150, 20)) {
                double newPosition = Math.max(0.0, Math.min(1.0, (double) (mouseX - x) / 150));
                setting.setValueFromSliderPosition(newPosition);
            }
        }
    }

    private void renderNumberSlider(DrawContext context, NumberSetting setting, int x, int y, int mouseX, int mouseY, float animationProgress) {
        String nameText = setting.getName();

        String valueText = String.valueOf((long) setting.getValue().doubleValue());
        int valueWidth = textRenderer.getWidth(valueText);

        int textColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;

        context.drawText(textRenderer, nameText, x, y + 2, textColor, false);
        context.drawText(textRenderer, valueText, x + 150 - valueWidth, y + 2, textColor, false);

        int sliderY = y + 12;

        int bgColor = RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);
        drawRoundedRect(context, x, sliderY, 150, 6, 3, bgColor);

        double position = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        int filledWidth = (int) (150 * position);

        if (filledWidth > 0) {
            int fillColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress);
            drawRoundedRect(context, x, sliderY, filledWidth, 6, 3, fillColor);
        }

        int handleX = x + filledWidth - 3;
        int handleColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, animationProgress);
        drawRoundedRect(context, handleX, sliderY - 2, 6, 10, 3, handleColor);

        if (draggingNumber && draggedNumber == setting) {
            if (isHovered(mouseX, mouseY, x, y, 150, 20)) {
                double newPosition = Math.max(0.0, Math.min(1.0, (double) (mouseX - x) / 150));
                double newValue = setting.getMin() + (setting.getMax() - setting.getMin()) * newPosition;
                setting.setValue(newValue);
            }
        }
    }

    private void renderDoubleSlider(DrawContext context, DoubleSetting setting, int x, int y, int mouseX, int mouseY, float animationProgress) {
        String nameText = setting.getName();


        String valueText = String.format("%.2f", setting.getValue());
        int valueWidth = textRenderer.getWidth(valueText);

        int textColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, (int) (animationProgress * 255)) | 0xFF000000;

        context.drawText(textRenderer, nameText, x, y + 2, textColor, false);
        context.drawText(textRenderer, valueText, x + 150 - valueWidth, y + 2, textColor, false);

        int sliderY = y + 12;

        int bgColor = RadiumGuiTheme.applyAlpha(0xFF333333, animationProgress * cachedPanelAlpha);
        drawRoundedRect(context, x, sliderY, 150, 6, 3, bgColor);

        double position = (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin());
        int filledWidth = (int) (150 * position);

        if (filledWidth > 0) {
            int fillColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress);
            drawRoundedRect(context, x, sliderY, filledWidth, 6, 3, fillColor);
        }

        int handleX = x + filledWidth - 3;
        int handleColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, animationProgress);
        drawRoundedRect(context, handleX, sliderY - 2, 6, 10, 3, handleColor);

        if (draggingDouble && draggedDouble == setting) {
            if (isHovered(mouseX, mouseY, x, y, 150, 20)) {
                double newPosition = Math.max(0.0, Math.min(1.0, (double) (mouseX - x) / 150));
                double newValue = setting.getMin() + (setting.getMax() - setting.getMin()) * newPosition;


                newValue = Math.round(newValue * 100.0) / 100.0;

                setting.setValue(newValue);
            }
        }
    }

    private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        RenderUtils.fillRoundRect(context, x, y, width, height, radius, color);
    }

    private void drawRoundedRectTop(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0) {
            context.fill(x, y, x + width, y + height, color);
            return;
        }

        RenderUtils.fillRoundTabTop(context, x, y, width, height, radius, color);
    }

    private void drawRoundedRectBottom(DrawContext context, int x, int y, int radius, int color) {
        if (radius <= 0) {
            context.fill(x, y, x + 115, y + 15, color);
            return;
        }

        RenderUtils.fillRoundTabBottom(context, x, y, 115, 15, radius, color);
    }

    private void drawRoundedRectOutline(DrawContext context, int x, int y, int width, int height, int radius, int color) {
        if (radius <= 0) {
            context.fill(x, y, x + width, y + 1, color);
            context.fill(x, y + height - 1, x + width, y + height, color);
            context.fill(x, y, x + 1, y + height, color);
            context.fill(x + width - 1, y, x + width, y + height, color);
            return;
        }


        RenderUtils.drawRoundRect(context, x, y, width, height, radius, color);
    }

    private List<Module> getModules(Module.Category cat) {
        if (RadiumClient.moduleManager == null) return List.of();

        if (cat == Module.Category.SEARCH) {
            return getSearchModules();
        }

        return RadiumClient.moduleManager.getModules().stream()
                .filter(m -> m.getCategory() == cat)
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    private List<Module> getSearchModules() {
        if (RadiumClient.moduleManager == null) return List.of();
        if (moduleSearchEditor.getText().isEmpty()) return List.of();

        String query = moduleSearchEditor.getText().toLowerCase();
        return RadiumClient.moduleManager.getModules().stream()
                .filter(m -> {
                    String name = m.getName().toLowerCase();
                    String description = m.getDescription() != null ? m.getDescription().toLowerCase() : "";
                    return name.contains(query) || description.contains(query);
                })
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    private void renderSearchCategory(DrawContext context, Module.Category cat, int x, int y, int mouseX, int mouseY, float animationProgress) {
        boolean expanded = categoryExpanded.get(cat);
        int cornerRadius = cachedCornerRadius;
        int searchBarHeight = 25;
        int searchBarY = y + headerHeight + 2;

        List<Module> modules = getSearchModules();
        int maxVisibleModules = 7;
        int visibleModulesCount = Math.min(maxVisibleModules, modules.size());
        int categoryHeight = headerHeight + searchBarHeight + 2;
        if (expanded && !modules.isEmpty()) {
            categoryHeight += visibleModulesCount * moduleHeight + 2;
        }

        boolean isDragging = draggingCategory && dragCategory == cat;
        boolean isHovered = isHovered(mouseX, mouseY, x, y, categoryWidth, categoryHeight);

        if (isHovered) {
            hoveredCategory = cat;
        }


        int categoryBgColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryBackground(), animationProgress * cachedPanelAlpha);

        if (isDragging) {
            categoryBgColor = blendColors(categoryBgColor, cachedAccentColor, 0.15f);
        } else if (isHovered) {
            categoryBgColor = blendColors(categoryBgColor, cachedAccentColor, 0.08f);
        }

        drawRoundedRect(context, x, y, categoryWidth, categoryHeight, cornerRadius, categoryBgColor);

        int borderColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getBorderColor(), animationProgress * 0.6f);
        if (isDragging) {
            borderColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress * 0.8f);
        } else if (isHovered) {
            borderColor = RadiumGuiTheme.applyAlpha(cachedAccentColor, animationProgress * 0.6f);
        }


        boolean headerHovered = isHovered(mouseX, mouseY, x, y, categoryWidth, headerHeight);
        int headerColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getCategoryHeader(), animationProgress * cachedPanelAlpha);

        drawRoundedRectTop(context, x, y, categoryWidth, headerHeight, cornerRadius, headerColor);

        String categoryName = cat.getName().toUpperCase();
        int textWidth = textRenderer.getWidth(categoryName);
        int textX = x + (categoryWidth - textWidth) / 2;
        int textY = y + (headerHeight - 8) / 2;

        if (headerHovered) {
            float pulseScale = 1f + 0.08f * (float) Math.sin(pulseAnimation * Math.PI * 2);
            int scaledTextWidth = (int) (textWidth * pulseScale);
            textX = x + (categoryWidth - scaledTextWidth) / 2;
        }

        context.drawText(textRenderer, categoryName, textX, textY,
                RadiumGuiTheme.applyAlpha(cachedTextColor, (int) (animationProgress * 255)) | 0xFF000000, false);

        String indicator = expanded ? "-" : "+";
        int indicatorX = x + categoryWidth - 12;
        context.drawText(textRenderer, indicator, indicatorX, textY,
                RadiumGuiTheme.applyAlpha(cachedTextColor, (int) (animationProgress * 255)) | 0xFF000000, false);

        context.fill(x + 2, y + headerHeight, x + categoryWidth - 2, y + headerHeight + 1,
                RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getSeparatorColor(), animationProgress));

        if (expanded) {
            int searchBarX = x + 2;
            int searchBarWidth = categoryWidth - 4;
            boolean searchHovered = isHovered(mouseX, mouseY, searchBarX, searchBarY, searchBarWidth, searchBarHeight);
            int searchBarColor = (moduleSearchEditor.isActive() || searchHovered) ?
                    RadiumGuiTheme.applyAlpha(cachedHoverColor, animationProgress * RadiumGuiTheme.HOVER_ALPHA) :
                    RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getModuleBackground(), animationProgress * cachedPanelAlpha);

            drawRoundedRect(context, searchBarX, searchBarY, searchBarWidth, searchBarHeight, cornerRadius, searchBarColor);

            String searchText = moduleSearchEditor.getText();
            if (moduleSearchEditor.isActive()) {
                int cursorPos = moduleSearchEditor.getCursorPosition();
                String beforeCursor = searchText.substring(0, cursorPos);
                String afterCursor = searchText.substring(cursorPos);
                searchText = beforeCursor + "_" + afterCursor;
            } else if (searchText.isEmpty()) {
                searchText = "Search modules...";
            }

            int titleColor = RadiumGuiTheme.applyAlpha(cachedTextColor, (int) (animationProgress * 255)) | 0xFF000000;
            if (moduleSearchEditor.getText().isEmpty() && !moduleSearchEditor.isActive()) {
                titleColor = RadiumGuiTheme.applyAlpha(0xFF888888, (int) (animationProgress * 255)) | 0xFF000000;
            }

            context.drawText(textRenderer, searchText, searchBarX + 5, searchBarY + 9, titleColor, false);

            if (!modules.isEmpty()) {
                int maxScroll = Math.max(0, modules.size() - maxVisibleModules);
                searchScrollOffset = Math.max(0, Math.min(searchScrollOffset, maxScroll));

                int offsetY = searchBarY + searchBarHeight + 2;
                for (int i = 0; i < visibleModulesCount; i++) {
                    int moduleIndex = i + searchScrollOffset;
                    if (moduleIndex < modules.size()) {
                        Module m = modules.get(moduleIndex);
                        boolean isLast = (i == visibleModulesCount - 1);
                        renderModule(context, m, x, offsetY, mouseX, mouseY, animationProgress, isLast, moduleIndex);
                        offsetY += moduleHeight;
                    }
                }
            }
        }
    }

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        mx = scaleInput(mx);
        my = scaleInput(my);

        if (listeningForKeybind && keybindModule != null) {
            int mouseKeyCode = KeybindManager.getMouseButtonKeyCode(button);
            if (listeningKeybindSetting != null) {
                listeningKeybindSetting.setValue(mouseKeyCode);
            } else {
                keybindModule.setKeyBind(mouseKeyCode);
            }
            listeningForKeybind = false;
            listeningKeybindSetting = null;
            keybindModule = null;
            if (RadiumClient.getConfigManager() != null) {
                RadiumClient.getConfigManager().saveKeybinds();
            }
            return true;
        }

        StringSetting clickedStringSetting = null;

        if (blockSelectionPanel.isOpen()) {
            if (blockSelectionPanel.handleClick(mx, my, button)) {
                return true;
            }
        }

        if (enchantmentPanel.isOpen()) {
            if (enchantmentPanel.handleClick(mx, my, button)) {
                return true;
            }
        }


        if (colorPickerPanel.isOpen()) {
            if (colorPickerPanel.handleClick(mx, my, button)) {
                return true;
            }
        }


        if (stringListPanel.isOpen()) {
            if (stringListPanel.handleClick(mx, my, button)) {
                return true;
            }
        }

        if (editingItem != null) {
            if (button == 0 && isHovered((int) mx, (int) my, itemPanelX, itemPanelY, itemPanelWidth, 30)) {
                int closeButtonX = itemPanelX + itemPanelWidth - 18;
                int closeButtonY = itemPanelY + 10;
                if (!isHovered((int) mx, (int) my, closeButtonX - 2, closeButtonY, 14, 10)) {
                    draggingItemPanel = true;
                    itemPanelDragOffsetX = (int) mx - itemPanelX;
                    itemPanelDragOffsetY = (int) my - itemPanelY;
                    itemPanelIsDragging = true;
                    itemPanelDragStartTime = System.currentTimeMillis();
                    return true;
                }
            }

            int searchBarX = itemPanelX + 10;
            int searchBarY = itemPanelY + 40;
            int searchBarWidth = itemPanelWidth - 20;
            int searchBarHeight = 20;
            if (isHovered((int) mx, (int) my, searchBarX, searchBarY, searchBarWidth, searchBarHeight)) {
                if (!itemSearchEditor.isActive()) {
                    itemSearchEditor.startEditing(itemSearchEditor.getText());
                }
                return true;
            }

            if (itemSearchEditor.isActive()) {
                itemSearchEditor.stopEditing();
            }

            int closeButtonX = itemPanelX + itemPanelWidth - 18;
            int closeButtonY = itemPanelY + 10;
            if (isHovered((int) mx, (int) my, closeButtonX - 2, closeButtonY, 14, 10)) {
                editingItem = null;
                return true;
            }

            int itemsStartY = itemPanelY + 70;
            int itemsHeight = itemPanelHeight - 80;


            int ITEM_SIZE = 20;
            int ITEM_PADDING = 3;
            int ITEMS_PER_ROW = 6;

            if (isHovered((int) mx, (int) my, itemPanelX + 5, itemsStartY, itemPanelWidth - 10, itemsHeight)) {
                List<Item> allItems = Registries.ITEM.stream()
                        .filter(item -> item != Items.AIR && item.getName().getString().toLowerCase().contains(itemSearchEditor.getText().toLowerCase()))
                        .toList();

                int totalRows = (int) Math.ceil((double) allItems.size() / ITEMS_PER_ROW);
                int visibleRows = itemsHeight / (ITEM_SIZE + ITEM_PADDING);
                int maxRowScroll = Math.max(0, totalRows - visibleRows);
                int rowScrollOffset = Math.max(0, Math.min(itemScrollOffset / ITEMS_PER_ROW, maxRowScroll));
                int scrollBarX = itemPanelX + itemPanelWidth - 12;
                int scrollBarY = itemsStartY + 5;
                int scrollBarHeight = itemsHeight - 10;
                int scrollBarWidth = 4;
                if (maxRowScroll > 0 && mx >= scrollBarX && mx < scrollBarX + scrollBarWidth) {
                    float scrollProgress = (float) (my - scrollBarY) / scrollBarHeight;
                    scrollProgress = Math.max(0, Math.min(1, scrollProgress));
                    rowScrollOffset = (int) (scrollProgress * maxRowScroll);
                    itemScrollOffset = rowScrollOffset * ITEMS_PER_ROW;
                    return true;
                }

                int gridWidth = (ITEMS_PER_ROW * ITEM_SIZE) + ((ITEMS_PER_ROW - 1) * ITEM_PADDING);
                int gridStartX = itemPanelX + (itemPanelWidth - gridWidth) / 2;


                if (mx >= gridStartX && mx < gridStartX + gridWidth && mx < scrollBarX) {
                    int clickedCol = (int) ((mx - gridStartX) / (ITEM_SIZE + ITEM_PADDING));
                    int clickedRow = (int) ((my - (itemsStartY + 5)) / (ITEM_SIZE + ITEM_PADDING));


                    int cellX = gridStartX + clickedCol * (ITEM_SIZE + ITEM_PADDING);
                    int cellY = itemsStartY + 5 + clickedRow * (ITEM_SIZE + ITEM_PADDING);

                    if (clickedCol >= 0 && clickedCol < ITEMS_PER_ROW && clickedRow >= 0 &&
                            clickedRow < visibleRows &&
                            mx >= cellX && mx < cellX + ITEM_SIZE &&
                            my >= cellY && my < cellY + ITEM_SIZE) {
                        int clickedIndex = (rowScrollOffset + clickedRow) * ITEMS_PER_ROW + clickedCol;

                        if (clickedIndex >= 0 && clickedIndex < allItems.size()) {
                            Item selectedItem = allItems.get(clickedIndex);
                            editingItem.setValue(selectedItem);
                            editingItem = null;
                            return true;
                        }
                    }
                }
            }

            if (!isHovered((int) mx, (int) my, itemPanelX, itemPanelY, itemPanelWidth, itemPanelHeight)) {
                editingItem = null;
            }
            return true;
        }

        if (selectedModule != null) {
            int contentTopPadding = 10;
            int totalSettingsHeight = (selectedModule.getSettings().size() + 1) * settingHeight + contentTopPadding;
            int visibleContentHeight = Math.min(totalSettingsHeight, maxPanelHeight);
            int contentY = panelY + panelHeaderHeight;

            if (isHovered((int) mx, (int) my, panelX, contentY, panelWidth, visibleContentHeight)) {
                int yOffset = contentY + contentTopPadding - (int) settingsScrollOffset;
                yOffset += settingHeight;

                for (var setting : selectedModule.getSettings()) {
                    if (setting instanceof StringSetting s) {
                        if (isHovered((int) mx, (int) my, panelX + 10, yOffset, panelWidth - 20, settingHeight)) {
                            clickedStringSetting = s;
                            break;
                        }
                    }
                    yOffset += settingHeight;
                }
            }
        }

        if (editingString != null && editingString != clickedStringSetting) {
            editingString.setValue(stringEditor.getText());
            editingString = null;
            stringEditor.stopEditing();
        }

        if (clickedStringSetting != null) {
            if (clickedStringSetting.getName().equals("Schematic Path") &&
                    selectedModule instanceof com.radium.client.modules.misc.SchematicBuilder) {
                client.execute(() -> client.setScreen(
                        new com.radium.client.gui.SchematicBrowserScreen(
                                (com.radium.client.modules.misc.SchematicBuilder) selectedModule
                        )
                ));
                return true;
            }
            editingString = clickedStringSetting;
            stringEditor.startEditing(clickedStringSetting.getValue());
            return true;
        }

        if (selectedModule != null) {
            int contentTopPadding = 10;
            int totalSettingsHeight = (selectedModule.getSettings().size() + 1) * settingHeight + contentTopPadding;
            int visibleContentHeight = Math.min(totalSettingsHeight, maxPanelHeight);
            int panelHeight = panelHeaderHeight + visibleContentHeight + 10;

            if (isHovered((int) mx, (int) my, panelX + panelWidth - 20, panelY, 20, panelHeaderHeight)) {
                selectedModule = null;
                return true;
            }

            if (button == 0 && isHovered((int) mx, (int) my, panelX, panelY, panelWidth - 20, panelHeaderHeight)) {
                draggingPanel = true;
                panelDragOffsetX = (int) mx - panelX;
                panelDragOffsetY = (int) my - panelY;
                panelIsDragging = true;
                panelDragStartTime = System.currentTimeMillis();
                return true;
            }

            int contentY = panelY + panelHeaderHeight;
            if (isHovered((int) mx, (int) my, panelX, contentY, panelWidth, visibleContentHeight)) {
                if (maxScroll > 0 && isHovered((int) mx, (int) my, panelX + panelWidth - 6, contentY, 6, visibleContentHeight)) {
                    draggingScrollBar = true;
                    return true;
                }

                int yOffset = contentY + contentTopPadding - (int) settingsScrollOffset;


                if (isHovered((int) mx, (int) my, panelX + 10, yOffset, panelWidth - 20, settingHeight)) {
                    String buttonText = KeybindManager.getKeyName(selectedModule.getKeyBind());
                    int buttonHeight = settingHeight - 4;
                    int buttonWidth = Math.max(60, textRenderer.getWidth(buttonText) + 10);
                    int buttonX = panelX + 10 + panelWidth - 30 - buttonWidth;
                    int buttonY = yOffset + 2;

                    if (isHovered((int) mx, (int) my, buttonX, buttonY, buttonWidth, buttonHeight)) {
                        listeningForKeybind = true;
                        listeningKeybindSetting = null;
                        keybindModule = selectedModule;
                        return true;
                    }
                }

                yOffset += settingHeight;

                for (var setting : selectedModule.getSettings()) {
                    if (isHovered((int) mx, (int) my, panelX + 10, yOffset, panelWidth - 20, settingHeight)) {
                        if (button == 0) {
                            if (setting instanceof BooleanSetting b) {
                                b.toggle();
                                return true;
                            } else if (setting instanceof SliderSetting s) {
                                draggingSlider = true;
                                draggedSlider = s;
                                double newPos = Math.max(0.0, Math.min(1.0, (mx - (panelX + 10)) / (panelWidth - 30)));
                                s.setValueFromSliderPosition(newPos);
                                return true;
                            } else if (setting instanceof NumberSetting n) {
                                draggingNumber = true;
                                draggedNumber = n;
                                double newPos = Math.max(0.0, Math.min(1.0, (mx - (panelX + 10)) / (panelWidth - 30)));
                                n.setValue(n.getMin() + (n.getMax() - n.getMin()) * newPos);
                                return true;
                            } else if (setting instanceof DoubleSetting d) {
                                draggingDouble = true;
                                draggedDouble = d;
                                double newPos = Math.max(0.0, Math.min(1.0, (mx - (panelX + 10)) / (panelWidth - 30)));
                                d.setValue(d.getMin() + (d.getMax() - d.getMin()) * newPos);
                                return true;
                            } else if (setting instanceof ModeSetting<?> modeSetting) {
                                modeSetting.cycleMode();
                                return true;
                            } else if (setting instanceof ItemSetting itemSetting) {
                                editingItem = itemSetting;
                                itemScrollOffset = 0;
                                itemSearchEditor.setText("");
                                return true;
                            } else if (setting instanceof BlockSetting blockSetting) {
                                editingBlock = blockSetting;
                                blockSelectionPanel.setEditingBlock(blockSetting);
                                return true;
                            } else if (setting instanceof ColorSetting colorSetting) {
                                int desiredX = panelX + panelWidth + 8;
                                int desiredY = yOffset;
                                colorPickerPanel.openAt(colorSetting, desiredX, desiredY);
                                return true;
                            } else if (setting instanceof StringListSetting stringListSetting) {
                                stringListPanel.setEditingList(stringListSetting);
                                return true;
                            } else if (setting instanceof EnchantmentSetting enchantmentSetting) {

                                enchantmentPanel.setEditingSetting(enchantmentSetting);
                                return true;
                            } else if (setting instanceof KeybindSetting keybindSetting) {
                                int buttonHeight = settingHeight - 4;
                                String buttonText = KeybindManager.getKeyName(keybindSetting.getValue());
                                int buttonWidth = Math.max(60, textRenderer.getWidth(buttonText) + 10);
                                int buttonX = panelX + 10 + panelWidth - 30 - buttonWidth;
                                int buttonY = yOffset + 2;

                                if (isHovered((int) mx, (int) my, buttonX, buttonY, buttonWidth, buttonHeight)) {
                                    listeningForKeybind = true;
                                    listeningKeybindSetting = keybindSetting;
                                    keybindModule = selectedModule;
                                    return true;
                                }
                            } else if (setting instanceof ProfileSetting profileSetting) {
                                int textWidth = textRenderer.getWidth(profileSetting.getName() + ": ");
                                int valueX = panelX + 10 + textWidth;
                                int boxWidth = panelWidth - 20 - textWidth;

                                if (isHovered((int) mx, (int) my, valueX - 2, yOffset + 1, boxWidth + 4, settingHeight - 2)) {
                                    profileSetting.setExpanded(!profileSetting.isExpanded());
                                    return true;
                                }

                                if (profileSetting.isExpanded()) {
                                    int dropdownY = yOffset + settingHeight - 2;
                                    int itemY = dropdownY + 2;
                                    for (String profile : profileSetting.getProfiles()) {
                                        if (isHovered((int) mx, (int) my, valueX - 2, itemY, boxWidth + 4, 12)) {
                                            profileSetting.setValue(profile);
                                            profileSetting.setExpanded(false);
                                            return true;
                                        }
                                        itemY += 12;
                                    }
                                    profileSetting.setExpanded(false);
                                }
                            }
                        }
                    }
                    yOffset += settingHeight;
                }
            }

            if (isHovered((int) mx, (int) my, panelX, panelY, panelWidth, panelHeight)) {
                return true;
            }
        }

        for (Module.Category cat : Module.Category.values()) {
            int x = categoryX.get(cat);
            int y = categoryY.get(cat);

            if (cat == Module.Category.SEARCH && categoryExpanded.get(cat)) {
                int searchBarY = y + headerHeight + 2;
                int searchBarX = x + 2;
                int searchBarWidth = categoryWidth - 4;
                int searchBarHeight = 25;

                if (isHovered((int) mx, (int) my, searchBarX, searchBarY, searchBarWidth, searchBarHeight)) {
                    if (button == 0) {
                        if (!moduleSearchEditor.isActive()) {
                            moduleSearchEditor.startEditing(moduleSearchEditor.getText());
                        } else {
                            moduleSearchEditor.stopEditing();
                        }
                        return true;
                    }
                }

                List<Module> modules = getSearchModules();
                if (!modules.isEmpty()) {
                    int offsetY = searchBarY + searchBarHeight + 2;
                    for (Module m : modules) {
                        if (isHovered((int) mx, (int) my, x, offsetY, categoryWidth, moduleHeight)) {
                            if (button == 0) {
                                m.toggle();
                            } else if (button == 1) {
                                boolean wasSameModule = selectedModule == m;
                                selectedModule = wasSameModule ? null : m;
                                if (!wasSameModule) {
                                    settingsScrollOffset = 0;
                                    if (categoryX.containsKey(Module.Category.CLIENT)) {
                                        panelX = categoryX.get(Module.Category.CLIENT);
                                        panelY = categoryY.get(Module.Category.CLIENT) + 200;
                                        panelTargetX = panelX;
                                        panelTargetY = panelY;
                                    }
                                }
                            }
                            return true;
                        }
                        offsetY += moduleHeight;
                    }
                }

                if (moduleSearchEditor.isActive() && !isHovered((int) mx, (int) my, searchBarX, searchBarY, searchBarWidth, searchBarHeight)) {
                    moduleSearchEditor.stopEditing();
                    searchScrollOffset = 0;
                }
            }

            if (isHovered((int) mx, (int) my, x, y, categoryWidth, headerHeight)) {
                if (button == 0) {
                    draggingCategory = true;
                    dragCategory = cat;
                    dragOffsetX = (int) mx - x;
                    dragOffsetY = (int) my - y;
                    categoryIsDragging.put(cat, true);
                    categoryDragStartTime.put(cat, System.currentTimeMillis());
                } else if (button == 1) {
                    categoryExpanded.put(cat, !categoryExpanded.get(cat));
                }
                return true;
            }

            if (categoryExpanded.get(cat) && cat != Module.Category.SEARCH) {
                int offsetY = y + headerHeight + 2;
                List<Module> modules = getModules(cat);
                for (Module m : modules) {
                    if (isHovered((int) mx, (int) my, x, offsetY, categoryWidth, moduleHeight)) {
                        if (button == 0) {
                            m.toggle();
                        } else if (button == 1) {
                            boolean wasSameModule = selectedModule == m;
                            selectedModule = wasSameModule ? null : m;
                            if (!wasSameModule) {
                                settingsScrollOffset = 0;
                                if (categoryX.containsKey(Module.Category.CLIENT)) {
                                    panelX = categoryX.get(Module.Category.CLIENT);
                                    panelY = categoryY.get(Module.Category.CLIENT) + 200;
                                    panelTargetX = panelX;
                                    panelTargetY = panelY;
                                }
                            }
                        }
                        return true;
                    }
                    offsetY += moduleHeight;
                }
            }
        }
        if (moduleSearchEditor.isActive()) {
            moduleSearchEditor.stopEditing();
            searchScrollOffset = 0;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        mx = scaleInput(mx);
        my = scaleInput(my);
        dx = scaleInput(dx);
        dy = scaleInput(dy);
        if (blockSelectionPanel.isOpen()) {
            if (blockSelectionPanel.handleDrag(mx, my, button, dx, dy)) {
                return true;
            }
        }

        if (enchantmentPanel.isOpen()) {
            if (enchantmentPanel.handleDrag(mx, my, button, dx, dy)) {
                return true;
            }
        }


        if (colorPickerPanel.isOpen()) {
            if (colorPickerPanel.handleDrag(mx, my, button, dx, dy)) {
                return true;
            }
        }


        if (stringListPanel.isOpen()) {
            if (stringListPanel.handleDrag(mx, my, button, dx, dy)) {
                return true;
            }
        }

        if (draggingItemPanel && button == 0) {
            int newX = (int) mx - itemPanelDragOffsetX;
            int newY = (int) my - itemPanelDragOffsetY;

            itemPanelX = Math.max(0, Math.min(width - itemPanelWidth, newX));
            itemPanelY = Math.max(0, Math.min(height - itemPanelHeight, newY));

            itemPanelTargetX = itemPanelX;
            itemPanelTargetY = itemPanelY;
            return true;
        }

        if (draggingPanel && button == 0) {
            int newX = (int) mx - panelDragOffsetX;
            int newY = (int) my - panelDragOffsetY;

            panelX = Math.max(0, Math.min(width - panelWidth, newX));
            panelY = Math.max(0, Math.min(height - (panelHeaderHeight + Math.min((selectedModule.getSettings().size() + 1) * settingHeight + 10, maxPanelHeight) + 10), newY));

            panelTargetX = panelX;
            panelTargetY = panelY;
            return true;
        }

        if (draggingCategory && dragCategory != null && button == 0) {
            int newX = (int) mx - dragOffsetX;
            int newY = (int) my - dragOffsetY;

            newX = Math.max(0, Math.min(width - categoryWidth, newX));
            newY = Math.max(0, Math.min(height - headerHeight, newY));

            categoryX.put(dragCategory, newX);
            categoryY.put(dragCategory, newY);
            categoryTargetX.put(dragCategory, (float) newX);
            categoryTargetY.put(dragCategory, (float) newY);
            return true;
        }

        if (draggingScrollBar && button == 0 && selectedModule != null) {
            int contentTopPadding = 10;
            int totalSettingsHeight = (selectedModule.getSettings().size() + 1) * settingHeight + contentTopPadding;
            int visibleContentHeight = Math.min(totalSettingsHeight, maxPanelHeight);
            int contentY = panelY + panelHeaderHeight;
            float scrollRatio = (float) (my - contentY) / visibleContentHeight;
            settingsScrollOffset = scrollRatio * maxScroll;
            settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, maxScroll));
            return true;
        }

        if (draggingSlider || draggingNumber) {
            return true;
        }

        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        mx = scaleInput(mx);
        my = scaleInput(my);


        if (enchantmentPanel.isOpen()) {
            if (enchantmentPanel.handleRelease(mx, my, button)) {
                return true;
            }
        }


        if (colorPickerPanel.isOpen()) {
            if (colorPickerPanel.handleRelease(mx, my, button)) {
                return true;
            }
        }


        if (stringListPanel.isOpen()) {
            if (stringListPanel.handleRelease(mx, my, button)) {
                return true;
            }
        }

        if (button == 0) {
            if (draggingItemPanel) {
                itemPanelTargetX = itemPanelX;
                itemPanelTargetY = itemPanelY;
                itemPanelDragStartTime = System.currentTimeMillis();
            }

            if (draggingPanel) {
                panelTargetX = panelX;
                panelTargetY = panelY;
                panelDragStartTime = System.currentTimeMillis();
            }

            if (draggingCategory && dragCategory != null) {
                categoryTargetX.put(dragCategory, (float) categoryX.get(dragCategory));
                categoryTargetY.put(dragCategory, (float) categoryY.get(dragCategory));
                categoryDragStartTime.put(dragCategory, System.currentTimeMillis());
            }

            draggingItemPanel = false;
            draggingCategory = false;
            dragCategory = null;
            draggingPanel = false;
            draggingSlider = false;
            draggedSlider = null;
            draggingNumber = false;
            draggedNumber = null;
            draggingScrollBar = false;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        mouseX = scaleInput(mouseX);
        mouseY = scaleInput(mouseY);
        if (blockSelectionPanel.isOpen()) {
            if (blockSelectionPanel.handleScroll(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }

        if (enchantmentPanel.isOpen()) {
            if (enchantmentPanel.handleScroll(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }

        if (stringListPanel.isOpen()) {
            if (stringListPanel.handleScroll(mouseX, mouseY, horizontalAmount, verticalAmount)) {
                return true;
            }
        }

        if (!moduleSearchEditor.getText().isEmpty()) {
            List<Module> searchModules = getSearchModules();
            int maxVisibleModules = 7;
            int maxScroll = Math.max(0, searchModules.size() - maxVisibleModules);
            if (maxScroll > 0) {
                int searchBarY = 50;
                int searchBarHeight = 25;
                int searchContentHeight = maxVisibleModules * moduleHeight;
                if (isHovered((int) mouseX, (int) mouseY, 5, searchBarY + searchBarHeight, categoryWidth - 10, searchContentHeight)) {
                    searchScrollOffset -= (int) verticalAmount * 3;
                    searchScrollOffset = Math.max(0, Math.min(searchScrollOffset, maxScroll));
                    return true;
                }
            }
        }

        if (editingItem != null) {
            if (isHovered((int) mouseX, (int) mouseY, itemPanelX, itemPanelY, itemPanelWidth, itemPanelHeight)) {
                List<Item> allItems = Registries.ITEM.stream()
                        .filter(item -> item != Items.AIR && item.getName().getString().toLowerCase().contains(itemSearchEditor.getText().toLowerCase()))
                        .toList();

                int itemsHeight = itemPanelHeight - 80;
                int ITEM_SIZE = 20;
                int ITEM_PADDING = 3;
                int ITEMS_PER_ROW = 6;

                int totalRows = (int) Math.ceil((double) allItems.size() / ITEMS_PER_ROW);
                int visibleRows = itemsHeight / (ITEM_SIZE + ITEM_PADDING);
                int maxRowScroll = Math.max(0, totalRows - visibleRows);

                if (maxRowScroll > 0) {
                    itemScrollOffset -= (int) verticalAmount * ITEMS_PER_ROW;
                    itemScrollOffset = Math.max(0, Math.min(itemScrollOffset, maxRowScroll * ITEMS_PER_ROW));
                    return true;
                }
            }
        }

        if (selectedModule != null) {
            int contentTopPadding = 10;
            int totalSettingsHeight = (selectedModule.getSettings().size() + 1) * settingHeight + contentTopPadding;
            int visibleContentHeight = Math.min(totalSettingsHeight, maxPanelHeight);
            int contentY = panelY + panelHeaderHeight;
            if (isHovered((int) mouseX, (int) mouseY, panelX, contentY, panelWidth, visibleContentHeight)) {
                if (maxScroll > 0) {
                    settingsScrollOffset = (float) (settingsScrollOffset - verticalAmount * 10);
                    settingsScrollOffset = Math.max(0, Math.min(settingsScrollOffset, maxScroll));
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (blockSelectionPanel.isOpen()) {
            if (blockSelectionPanel.handleKeyPress(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (enchantmentPanel.isOpen()) {
            if (enchantmentPanel.handleKeyPress(keyCode, scanCode, modifiers)) {
                return true;
            }
        }


        if (colorPickerPanel.isOpen()) {
            if (colorPickerPanel.handleKeyPress(keyCode, scanCode, modifiers)) {
                return true;
            }
        }


        if (stringListPanel.isOpen()) {
            if (stringListPanel.handleKeyPress(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (moduleSearchEditor.isActive()) {
            if (moduleSearchEditor.handleKeyPress(keyCode, scanCode, modifiers)) {
                if (moduleSearchEditor.getText().isEmpty()) {
                    searchScrollOffset = 0;
                }
                return true;
            }
        }

        if (itemSearchEditor.isActive()) {
            if (itemSearchEditor.handleKeyPress(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        if (editingString != null && stringEditor.isActive()) {
            if (stringEditor.handleKeyPress(keyCode, scanCode, modifiers)) {
                if (keyCode == GLFW.GLFW_KEY_ENTER) {
                    editingString.setValue(stringEditor.getText());
                    editingString = null;
                    stringEditor.stopEditing();
                } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    editingString = null;
                    stringEditor.cancelEditing();
                }
                return true;
            }
        }

        if (listeningForKeybind && keybindModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (listeningKeybindSetting != null) {
                    listeningKeybindSetting.setValue(-1);
                } else {
                    keybindModule.setKeyBind(-1);
                }
            } else {
                if (listeningKeybindSetting != null) {
                    listeningKeybindSetting.setValue(keyCode);
                } else {
                    keybindModule.setKeyBind(keyCode);
                }
            }
            listeningForKeybind = false;
            listeningKeybindSetting = null;
            keybindModule = null;
            if (RadiumClient.getConfigManager() != null) {
                RadiumClient.getConfigManager().saveKeybinds();
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (blockSelectionPanel.isOpen()) {
            if (blockSelectionPanel.handleCharType(chr, modifiers)) {
                return true;
            }
        }

        if (enchantmentPanel.isOpen()) {
            if (enchantmentPanel.handleCharType(chr, modifiers)) {
                return true;
            }
        }


        if (colorPickerPanel.isOpen()) {
            if (colorPickerPanel.handleCharType(chr, modifiers)) {
                return true;
            }
        }


        if (stringListPanel.isOpen()) {
            if (stringListPanel.handleCharType(chr, modifiers)) {
                return true;
            }
        }

        if (moduleSearchEditor.isActive()) {
            if (moduleSearchEditor.handleCharType(chr, modifiers)) {
                searchScrollOffset = 0;
                return true;
            }
            return false;
        }

        if (itemSearchEditor.isActive()) {
            return itemSearchEditor.handleCharType(chr, modifiers);
        }

        if (editingString != null && stringEditor.isActive()) {
            return stringEditor.handleCharType(chr, modifiers);
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
