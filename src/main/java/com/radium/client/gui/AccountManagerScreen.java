package com.radium.client.gui;
// radium client

import com.mojang.authlib.GameProfile;
import com.radium.client.client.RadiumClient;
import com.radium.client.gui.utils.TextEditor;
import com.radium.client.systems.accounts.Account;
import com.radium.client.systems.accounts.MicrosoftLogin;
import com.radium.client.systems.accounts.types.CrackedAccount;
import com.radium.client.systems.accounts.types.MicrosoftAccount;
import com.radium.client.utils.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class AccountManagerScreen extends Screen {
    private final Screen parent;
    private final List<Account<?>> accounts = new ArrayList<>();
    private final Map<Account<?>, Float> accountHoverProgress = new HashMap<>();
    private final Map<SideTab, Float> tabHoverProgress = new HashMap<>();
    private final int sidebarWidth = 90;
    private final int headerHeight = 20;
    private final int windowWidth = 500;
    private final int windowHeight = 280;
    private final int padding = 8;
    private final int accountCardWidth = 120;
    private final int accountCardHeight = 60;
    private final int cardPadding = 8;
    private final int tabButtonHeight = 18;
    private final TextEditor crackedNameEditor = new TextEditor();
    private Account<?> selectedAccount = null;
    private int windowX, windowY;
    private int scrollOffset = 0;
    private SideTab currentTab = SideTab.ACCOUNTS;
    private String statusMessage = "";
    private int statusColor = 0xFFFFFF;
    private int statusTicks = 0;
    private float animationProgress = 0;
    private final long startTime;
    private float windowHoverProgress = 0f;
    private Identifier discordAvatarTexture = null;
    private String cachedDiscordUsername = null;

    public AccountManagerScreen(Screen parent) {
        super(Text.literal("Account Manager"));
        this.parent = parent;
        this.startTime = System.currentTimeMillis();
        refreshAccounts();
    }

    private void refreshAccounts() {
        accounts.clear();
        if (RadiumClient.getAccountManager() != null) {
            for (Account<?> acc : RadiumClient.getAccountManager()) {
                accounts.add(acc);
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        windowX = (width - windowWidth) / 2;
        windowY = (height - windowHeight) / 2;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

        context.fill(0, 0, width, height, 0x66000000);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        long elapsed = System.currentTimeMillis() - startTime;
        animationProgress = Math.min(1.0f, elapsed / 500f);

        float animDelta = delta * 12f;
        boolean windowHovered = isHovered(mouseX, mouseY, windowX, windowY, windowWidth, windowHeight);
        windowHoverProgress = Math.max(0f, Math.min(1f, windowHoverProgress + (windowHovered ? animDelta : -animDelta)));

        if (statusTicks > 0) statusTicks--;
        if (statusTicks == 0) statusMessage = "";

        drawMainWindow(context, mouseX, mouseY, animationProgress);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawMainWindow(DrawContext context, int mouseX, int mouseY, float animationProgress) {
        int cornerRadius = 12;
        int accentBase = RadiumGuiTheme.getAccentColor();
        int darkAccent = RadiumGuiTheme.blendColors(accentBase, 0xFF000000, 0.8f);

        int windowBgColor = RadiumGuiTheme.applyAlpha(darkAccent, animationProgress * 0.4f);
        RenderUtils.fillRoundRect(context, windowX, windowY, windowWidth, windowHeight, cornerRadius, windowBgColor);

        int glowColor = RadiumGuiTheme.applyAlpha(accentBase, animationProgress * windowHoverProgress * 0.15f);
        if (glowColor != 0) {
            RenderUtils.drawRoundRect(context, windowX + 1, windowY + 1, windowWidth - 2, windowHeight - 2, cornerRadius - 1, glowColor);
        }

        int borderColor = RadiumGuiTheme.applyAlpha(accentBase, animationProgress * (0.7f + windowHoverProgress * 0.3f));
        RenderUtils.drawRoundRect(context, windowX, windowY, windowWidth, windowHeight, cornerRadius, borderColor);

        int headerBg = RadiumGuiTheme.applyAlpha(darkAccent, animationProgress * 0.4f);
        RenderUtils.fillRoundTabTop(context, windowX, windowY, windowWidth, headerHeight, cornerRadius, headerBg);

        String titleText = "Account Manager";
        int titleColor = RadiumGuiTheme.applyAlpha(0xFFFFFFFF, animationProgress);
        context.drawText(textRenderer, titleText, windowX + (windowWidth - textRenderer.getWidth(titleText)) / 2, windowY + (headerHeight - 8) / 2, titleColor, false);

        drawSidebar(context, mouseX, mouseY, animationProgress);
        drawContent(context, mouseX, mouseY, animationProgress);

        if (!statusMessage.isEmpty()) {
            int alpha = (int) (Math.min(1f, statusTicks / 20f) * 255 * animationProgress);
            int color = (alpha << 24) | (statusColor & 0xFFFFFF);
            int sw = textRenderer.getWidth(statusMessage);
            context.drawText(textRenderer, statusMessage, windowX + sidebarWidth + (windowWidth - sidebarWidth - sw) / 2, windowY + windowHeight - 15, color, false);
        }
    }

    private void drawSidebar(DrawContext context, int mouseX, int mouseY, float animationProgress) {
        int sx = windowX + 1;
        int sy = windowY + headerHeight + 1;
        int sh = windowHeight - headerHeight - 2;

        int sidebarBg = RadiumGuiTheme.applyAlpha(0x1A000000, animationProgress * 0.15f);
        RenderUtils.fillRoundRect(context, sx, sy, sidebarWidth - 1, sh, 12, sidebarBg);

        int buttonY = sy + padding;
        for (SideTab tab : SideTab.values()) {
            drawTabButton(context, tab, sx + padding / 2, buttonY, mouseX, mouseY, animationProgress);
            buttonY += tabButtonHeight + padding / 2;
        }

        drawPlayerInfo(context, sx, sy + sh - 30, animationProgress);
    }

    private void drawTabButton(DrawContext context, SideTab tab, int x, int y, int mouseX, int mouseY, float animationProgress) {
        int width = sidebarWidth - padding;
        boolean active = currentTab == tab;
        boolean hovered = isHovered(mouseX, mouseY, x, y, width, tabButtonHeight);

        float progress = tabHoverProgress.getOrDefault(tab, 0f);
        progress = Math.max(0, Math.min(1, progress + (hovered ? 0.2f : -0.2f)));
        tabHoverProgress.put(tab, progress);

        int baseColor = active ? RadiumGuiTheme.getAccentColor() : 0xFF2A2A2A;
        int btnColor = RadiumGuiTheme.blendColors(baseColor, RadiumGuiTheme.getHoverColor(), progress * 0.6f);
        btnColor = RadiumGuiTheme.applyAlpha(btnColor, animationProgress * 0.2f);

        RenderUtils.fillRoundRect(context, x, y, width, tabButtonHeight, 6, btnColor);

        if (active) {
            int indicatorColor = RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), animationProgress);
            RenderUtils.fillRoundRect(context, x, y, 2, tabButtonHeight, 1, indicatorColor);
        }

        int textColor = active ? 0xFFFFFFFF : RadiumGuiTheme.blendColors(0xFFCCCCCC, 0xFFFFFFFF, progress);
        context.drawCenteredTextWithShadow(textRenderer, tab.getName(), x + width / 2, y + (tabButtonHeight - 8) / 2, RadiumGuiTheme.applyAlpha(textColor, animationProgress));
    }

    private void drawContent(DrawContext context, int mouseX, int mouseY, float animationProgress) {
        int cx = windowX + sidebarWidth + 1;
        int cy = windowY + headerHeight + 1;
        int cw = windowWidth - sidebarWidth - 2;
        int ch = windowHeight - headerHeight - 2;

        int contentBg = RadiumGuiTheme.applyAlpha(0x1A000000, animationProgress * 0.15f);
        RenderUtils.fillRoundTabBottom(context, cx, cy, cw, ch, 12, contentBg);

        switch (currentTab) {
            case ACCOUNTS -> drawAccountsGrid(context, cx, cy, cw, ch, mouseX, mouseY, animationProgress);
            case ADD_CRACKED -> drawAddCracked(context, cx, cy, cw, ch, mouseX, mouseY, animationProgress);
            case ADD_MICROSOFT -> drawAddMicrosoft(context, cx, cy, cw, ch, mouseX, mouseY, animationProgress);
        }
    }

    private void drawAccountsGrid(DrawContext context, int x, int y, int w, int h, int mouseX, int mouseY, float anim) {
        context.enableScissor(x, y, x + w, y + h);

        int cardsPerRow = (w - padding * 2) / (accountCardWidth + cardPadding);
        cardsPerRow = Math.max(1, cardsPerRow);

        int startX = x + padding;
        int startY = y + padding - scrollOffset;

        for (int i = 0; i < accounts.size(); i++) {
            Account<?> acc = accounts.get(i);
            int col = i % cardsPerRow;
            int row = i / cardsPerRow;
            int tx = startX + col * (accountCardWidth + cardPadding);
            int ty = startY + row * (accountCardHeight + cardPadding);

            if (ty + accountCardHeight >= y && ty <= y + h) {
                drawAccountCard(context, acc, tx, ty, mouseX, mouseY, anim);
            }
        }

        context.disableScissor();

        int totalRows = (int) Math.ceil(accounts.size() / (double) cardsPerRow);
        int totalHeight = totalRows * (accountCardHeight + cardPadding) + padding;
        if (totalHeight > h) {
            int sbX = x + w - 4;
            int sbH = (int) ((h / (float) totalHeight) * h);
            int sbY = y + (int) ((scrollOffset / (float) (totalHeight - h)) * (h - sbH));
            RenderUtils.fillRoundRect(context, sbX, sbY, 2, sbH, 1, RadiumGuiTheme.applyAlpha(RadiumGuiTheme.getAccentColor(), anim * 0.5f));
        }
    }

    private void drawAccountCard(DrawContext context, Account<?> acc, int x, int y, int mx, int my, float anim) {
        boolean selected = acc == selectedAccount;
        boolean current = acc.getUsername().equals(MinecraftClient.getInstance().getSession().getUsername());
        boolean hovered = isHovered(mx, my, x, y, accountCardWidth, accountCardHeight);

        float progress = accountHoverProgress.getOrDefault(acc, 0f);
        progress = Math.max(0, Math.min(1, progress + (hovered ? 0.2f : -0.2f)));
        accountHoverProgress.put(acc, progress);

        int baseBg = 0xFF1E1E1E;
        int cardColor = RadiumGuiTheme.blendColors(baseBg, 0xFF2A2A2A, progress);
        RenderUtils.fillRoundRect(context, x, y, accountCardWidth, accountCardHeight, 6, RadiumGuiTheme.applyAlpha(cardColor, anim * 0.2f));

        int borderColor = selected ? RadiumGuiTheme.getAccentColor() : (current ? 0xFF2ECC71 : 0xFF3D3D60);
        RenderUtils.drawRoundRect(context, x, y, accountCardWidth, accountCardHeight, 6, RadiumGuiTheme.applyAlpha(borderColor, anim * (0.6f + progress * 0.4f)));

        SkinTextures skin = getSkinTextures(acc);
        RenderUtils.drawRoundedPlayerHead(context, skin, x + (accountCardWidth - 20) / 2, y + 5, 20, 4);

        String name = acc.getUsername();
        if (textRenderer.getWidth(name) > accountCardWidth - 10)
            name = textRenderer.trimToWidth(name, accountCardWidth - 20) + "..";
        context.drawCenteredTextWithShadow(textRenderer, name, x + accountCardWidth / 2, y + 28, RadiumGuiTheme.applyAlpha(0xFFFFFFFF, anim));

        String type = acc.getType().name();
        context.drawCenteredTextWithShadow(textRenderer, "Â§7" + type, x + accountCardWidth / 2, y + 38, RadiumGuiTheme.applyAlpha(0xFFFFFFFF, anim));

        if (current) {
            context.drawCenteredTextWithShadow(textRenderer, "Â§aÂ§lCONNECTED", x + accountCardWidth / 2, y + 48, RadiumGuiTheme.applyAlpha(0xFFFFFFFF, anim));
        }
    }

    private void drawAddCracked(DrawContext context, int x, int y, int w, int h, int mx, int my, float anim) {
        int iw = 150;
        int ix = x + (w - iw) / 2;
        int iy = y + 40;

        context.drawCenteredTextWithShadow(textRenderer, "Add Cracked Account", x + w / 2, y + 20, RadiumGuiTheme.applyAlpha(0xFFFFFFFF, anim));

        int inputBg = RadiumGuiTheme.applyAlpha(crackedNameEditor.isActive() ? 0xFF3A3A3E : 0xFF1A1A1D, anim * 0.4f);
        RenderUtils.fillRoundRect(context, ix, iy, iw, 20, 4, inputBg);
        RenderUtils.drawRoundRect(context, ix, iy, iw, 20, 4, RadiumGuiTheme.applyAlpha(crackedNameEditor.isActive() ? RadiumGuiTheme.getAccentColor() : 0xFF444444, anim));

        String text = crackedNameEditor.getText() + (crackedNameEditor.isActive() && (System.currentTimeMillis() / 500) % 2 == 0 ? "_" : "");
        if (text.isEmpty() && !crackedNameEditor.isActive()) text = "Â§8Username...";
        context.drawText(textRenderer, text, ix + 5, iy + 6, RadiumGuiTheme.applyAlpha(0xFFFFFFFF, anim), false);

        int bx = x + (w - 60) / 2;
        int by = iy + 30;
        boolean bh = isHovered(mx, my, bx, by, 60, 18);
        int bcol = RadiumGuiTheme.applyAlpha(bh ? RadiumGuiTheme.getAccentColor() : RadiumGuiTheme.getAccentColorDark(), anim);
        RenderUtils.fillRoundRect(context, bx, by, 60, 18, 4, bcol);
        context.drawCenteredTextWithShadow(textRenderer, "Add", bx + 30, by + 5, 0xFFFFFFFF);
    }

    private void drawAddMicrosoft(DrawContext context, int x, int y, int w, int h, int mx, int my, float anim) {
        context.drawCenteredTextWithShadow(textRenderer, "Microsoft Authentication", x + w / 2, y + 40, RadiumGuiTheme.applyAlpha(0xFFFFFFFF, anim));
        String info = "Clicking the button below will open your browser";
        context.drawCenteredTextWithShadow(textRenderer, "Â§7" + info, x + w / 2, y + 60, RadiumGuiTheme.applyAlpha(0xFFFFFFFF, anim));

        int bx = x + (w - 100) / 2;
        int by = y + 90;
        boolean bh = isHovered(mx, my, bx, by, 100, 24);
        int bcol = RadiumGuiTheme.applyAlpha(bh ? 0xFF00A4EF : 0xFF0078D4, anim);
        RenderUtils.fillRoundRect(context, bx, by, 100, 24, 6, bcol);
        context.drawCenteredTextWithShadow(textRenderer, "Login", bx + 50, by + 8, 0xFFFFFFFF);
    }

    private void drawPlayerInfo(DrawContext context, int x, int y, float anim) {

        String name;
        if (RadiumClient.username != null && !RadiumClient.username.isEmpty()) {
            name = RadiumClient.username;
        } else {
            name = client.getSession().getUsername();
        }


        Identifier avatarTexture = getDiscordAvatar();
        boolean avatarDrawn = false;

        if (avatarTexture != null) {
            int avatarSize = 20;
            int startX = x + (90 - (avatarSize + 4 + textRenderer.getWidth(name))) / 2;
            RenderUtils.drawRoundTexture(context, avatarTexture, startX, y + 30, avatarSize, avatarSize, 4);
            avatarDrawn = true;
        }


        if (!avatarDrawn) {
            UUID uuid = client.getSession().getUuidOrNull();
            GameProfile profile = new GameProfile(uuid != null ? uuid : UUID.randomUUID(), name);
            SkinTextures skin = client.getSkinProvider().getSkinTextures(profile);
            int startX = x + (90 - (20 + 4 + textRenderer.getWidth(name))) / 2;
            RenderUtils.drawRoundedPlayerHead(context, skin, startX, y + 30, 20, 4);
        }

        String shortName = textRenderer.getWidth(name) > 80 ? textRenderer.trimToWidth(name, 70) + ".." : name;
        int totalW = 20 + 4 + textRenderer.getWidth(shortName);
        int startX = x + (90 - totalW) / 2;
        int nameY = y + 6;
        context.drawText(textRenderer, shortName, startX + 24, nameY, RadiumGuiTheme.applyAlpha(0xFFFFFFFF, anim), false);

    }

    private Identifier getDiscordAvatar() {
        String discordUsername = RadiumClient.username;
        if (discordUsername == null || discordUsername.isEmpty()) {
            return null;
        }


        if (!discordUsername.equals(cachedDiscordUsername)) {
            discordAvatarTexture = null;
            cachedDiscordUsername = discordUsername;
            fetchDiscordAvatar(discordUsername);
        }

        return discordAvatarTexture;
    }

    private void fetchDiscordAvatar(String username) {
        CompletableFuture.runAsync(() -> {
            try {

                String avatarUrl = null;
                if (RadiumClient.discordId != null && !RadiumClient.discordId.isEmpty()) {

                    try {
                        String apiUrl = "https://discord.com/api/users/@me";
                        HttpURLConnection apiConnection = (HttpURLConnection) new URL(apiUrl).openConnection();
                        apiConnection.setRequestMethod("GET");
                        apiConnection.setRequestProperty("User-Agent", "RadiumClient/1.0");
                        apiConnection.setConnectTimeout(3000);
                        apiConnection.setReadTimeout(3000);

                        if (apiConnection.getResponseCode() == 200) {
                            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(apiConnection.getInputStream()));
                            StringBuilder response = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                response.append(line);
                            }
                            reader.close();


                            String json = response.toString();
                            int avatarIndex = json.indexOf("\"avatar\":\"");
                            if (avatarIndex != -1) {
                                int start = avatarIndex + 10;
                                int end = json.indexOf("\"", start);
                                if (end != -1) {
                                    String avatarHash = json.substring(start, end);
                                    if (avatarHash != null && !avatarHash.isEmpty() && !avatarHash.equals("null")) {

                                        avatarUrl = "https://cdn.discordapp.com/avatars/" + RadiumClient.discordId + "/" + avatarHash + ".png";
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {

                    }


                    if (avatarUrl == null) {
                        long discordIdLong = 0;
                        try {
                            discordIdLong = Long.parseLong(RadiumClient.discordId);
                        } catch (Exception e) {
                        }
                        int avatarIndex = (int) (discordIdLong % 5);
                        avatarUrl = "https://cdn.discordapp.com/embed/avatars/" + avatarIndex + ".png";
                    }
                } else {

                    avatarUrl = "https://cdn.discordapp.com/embed/avatars/0.png";
                }

                if (avatarUrl != null) {
                    HttpURLConnection connection = (HttpURLConnection) new URL(avatarUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", "RadiumClient/1.0");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);

                    if (connection.getResponseCode() == 200) {
                        InputStream inputStream = connection.getInputStream();
                        NativeImage image = NativeImage.read(inputStream);
                        inputStream.close();

                        Identifier textureId = Identifier.of("radium", "discord/avatar/" + username.toLowerCase().replaceAll("[^a-z0-9]", "_"));

                        client.execute(() -> {
                            try {
                                client.getTextureManager().registerTexture(textureId, new NativeImageBackedTexture(image));
                                discordAvatarTexture = textureId;
                            } catch (Exception e) {

                            }
                        });
                    }
                }
            } catch (Exception e) {

            }
        });
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;

        int bx = windowX + 1 + padding / 2;
        int by = windowY + headerHeight + 1 + padding;
        for (SideTab tab : SideTab.values()) {
            if (isHovered(mx, my, bx, by, sidebarWidth - padding, tabButtonHeight)) {
                if (tab == SideTab.DELETE) deleteAccount();
                else currentTab = tab;
                return true;
            }
            by += tabButtonHeight + padding / 2;
        }

        int cx = windowX + sidebarWidth + 1;
        int cy = windowY + headerHeight + 1;
        int cw = windowWidth - sidebarWidth - 2;

        if (currentTab == SideTab.ACCOUNTS) {
            int cardsPerRow = (cw - padding * 2) / (accountCardWidth + cardPadding);
            cardsPerRow = Math.max(1, cardsPerRow);
            int startX = cx + padding;
            int startY = cy + padding - scrollOffset;

            for (int i = 0; i < accounts.size(); i++) {
                int col = i % cardsPerRow;
                int row = i / cardsPerRow;
                int tx = startX + col * (accountCardWidth + cardPadding);
                int ty = startY + row * (accountCardHeight + cardPadding);
                if (isHovered(mx, my, tx, ty, accountCardWidth, accountCardHeight)) {
                    Account<?> clicked = accounts.get(i);
                    if (button == 0) {
                        if (selectedAccount == clicked) loginToAccount(clicked);
                        else selectedAccount = clicked;
                    }
                    return true;
                }
            }
        } else if (currentTab == SideTab.ADD_CRACKED) {
            int ix = cx + (cw - 150) / 2;
            int iy = cy + 40;
            if (isHovered(mx, my, ix, iy, 150, 20)) {
                crackedNameEditor.startEditing(crackedNameEditor.getText());
                return true;
            } else crackedNameEditor.stopEditing();

            int bbx = cx + (cw - 60) / 2;
            int bby = iy + 30;
            if (isHovered(mx, my, bbx, bby, 60, 18)) {
                addCrackedAccount();
                return true;
            }
        } else if (currentTab == SideTab.ADD_MICROSOFT) {
            int bbx = cx + (cw - 100) / 2;
            int bby = cy + 90;
            if (isHovered(mx, my, bbx, bby, 100, 24)) {
                addMicrosoftAccount();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (crackedNameEditor.isActive()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                addCrackedAccount();
                crackedNameEditor.stopEditing();
                return true;
            }
            crackedNameEditor.handleKeyPress(keyCode, scanCode, modifiers);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (crackedNameEditor.isActive()) {
            crackedNameEditor.handleCharType(chr, modifiers);
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) (verticalAmount * 15);
        int cardsPerRow = (windowWidth - sidebarWidth - 2 - padding * 2) / (accountCardWidth + cardPadding);
        cardsPerRow = Math.max(1, cardsPerRow);
        int totalHeight = (int) Math.ceil(accounts.size() / (double) cardsPerRow) * (accountCardHeight + cardPadding) + padding;
        scrollOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalHeight - (windowHeight - headerHeight - 2))));
        return true;
    }

    private void addCrackedAccount() {
        String name = crackedNameEditor.getText();
        if (name.isEmpty()) {
            setStatus("Â§cEnter username!", 0xFF5555);
            return;
        }
        CrackedAccount account = new CrackedAccount(name);
        if (account.fetchInfo()) {
            RadiumClient.getAccountManager().add(account);
            refreshAccounts();
            setStatus("Â§aAdded " + name, 0x55FF55);
            crackedNameEditor.setText("");
            currentTab = SideTab.ACCOUNTS;
        }
    }

    private void addMicrosoftAccount() {
        setStatus("Â§eChecking browser...", 0xFFFF55);
        MicrosoftLogin.getRefreshToken(token -> {
            if (token != null) {
                MicrosoftAccount acc = new MicrosoftAccount(token);
                if (acc.fetchInfo()) {
                    RadiumClient.getAccountManager().add(acc);
                    client.execute(() -> {
                        refreshAccounts();
                        setStatus("Â§aAdded MS Account", 0x55FF55);
                        currentTab = SideTab.ACCOUNTS;
                    });
                }
            } else {
                client.execute(() -> setStatus("Â§cFailed/Cancelled", 0xFF5555));
            }
        });
    }

    private void deleteAccount() {
        if (selectedAccount != null) {
            RadiumClient.getAccountManager().remove(selectedAccount);
            refreshAccounts();
            setStatus("Â§cDeleted Account", 0xFF5555);
            selectedAccount = null;
        } else {
            setStatus("Â§eSelect first!", 0xFFFF55);
        }
    }

    private void loginToAccount(Account<?> account) {
        if (account == null) return;
        setStatus("Â§eLogging in...", 0xFFFF55);
        new Thread(() -> {
            if (account.fetchInfo() && account.login()) {
                client.execute(() -> setStatus("Â§aLogged in!", 0x55FF55));
            } else {
                client.execute(() -> setStatus("Â§cFailed", 0xFF5555));
            }
        }).start();
    }

    private void setStatus(String msg, int color) {
        this.statusMessage = msg;
        this.statusColor = color;
        this.statusTicks = 100;
    }

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private SkinTextures getSkinTextures(Account<?> acc) {
        UUID uuid = parseUUID(acc.getCache().uuid);
        String name = acc.getUsername();
        GameProfile profile = new GameProfile(uuid, name);
        return client.getSkinProvider().getSkinTextures(profile);
    }

    private UUID parseUUID(String s) {
        try {
            return UUID.fromString(s);
        } catch (Exception e) {
            return UUID.randomUUID();
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    enum SideTab {
        ACCOUNTS("Accounts"),
        ADD_CRACKED("+ Cracked"),
        ADD_MICROSOFT("+ Microsoft"),
        DELETE("Delete Selected");
        private final String name;

        SideTab(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}

