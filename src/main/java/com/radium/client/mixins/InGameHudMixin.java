package com.radium.client.mixins;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.client.HUD;
import com.radium.client.modules.donut.FakeScoreboard;
import com.radium.client.modules.donut.HideScoreboard;
import com.radium.client.modules.donut.SilentHome;
import com.radium.client.modules.misc.Compass;
import com.radium.client.modules.visual.CustomCrosshair;
import com.radium.client.modules.visual.TotemOverlay;
import com.radium.client.utils.ScoreboardUtils;
import com.radium.client.utils.ToastNotificationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective) {
        throw new AssertionError();
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void onRenderCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        CustomCrosshair module = CustomCrosshair.get();
        if (module != null && module.isEnabled()) {
            ci.cancel();

            int x = client.getWindow().getScaledWidth() / 2;
            int y = client.getWindow().getScaledHeight() / 2;

            float size = module.size.getValue().floatValue();
            float gap = module.gap.getValue().floatValue();
            float thickness = module.thickness.getValue().floatValue();
            int color = module.color.getValue().getRGB();

            // Horizontal left
            context.fill(
                (int)(x - gap - size), (int)(y - thickness / 2),
                (int)(x - gap), (int)(y + thickness / 2),
                color
            );
            // Horizontal right
            context.fill(
                (int)(x + gap), (int)(y - thickness / 2),
                (int)(x + gap + size), (int)(y + thickness / 2),
                color
            );
            // Vertical top
            context.fill(
                (int)(x - thickness / 2), (int)(y - gap - size),
                (int)(x + thickness / 2), (int)(y - gap),
                color
            );
            // Vertical bottom
            context.fill(
                (int)(x - thickness / 2), (int)(y + gap),
                (int)(x + thickness / 2), (int)(y + gap + size),
                color
            );
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, RenderTickCounter tickCounter, CallbackInfo callbackInfo) {
        if (this.client.getDebugHud().shouldShowDebugHud()) {
            return;
        }
        if (RadiumClient.moduleManager == null) return;
        RadiumClient.getModuleManager().getModule(TotemOverlay.class).render(context);
        RadiumClient.getModuleManager().getModule(HUD.class).render(context, tickCounter.getTickDelta(true));
        RadiumClient.getModuleManager().getModule(Compass.class).render(context);
        // RadiumClient.getModuleManager().getModule(com.radium.client.modules.visual.ArmorHUD.class).render(context);

        var mediaPlayer = RadiumClient.getModuleManager().getModule(com.radium.client.modules.client.MediaPlayer.class);
        if (mediaPlayer != null && mediaPlayer.isEnabled()) {
            mediaPlayer.render(context, tickCounter.getTickDelta(true));
        }

        // Render toast notifications
        ToastNotificationManager.getInstance().render(context, tickCounter.getTickDelta(true));
    }


    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void onSetOverlayMessage(Text message, boolean tinted, CallbackInfo ci) {
        if (RadiumClient.moduleManager == null) return;
        SilentHome silentHome = RadiumClient.moduleManager.getModule(SilentHome.class);

        if (silentHome != null && silentHome.isEnabled() && silentHome.shouldSuppressActionBar()) {
            if (message.getString().toLowerCase().contains("home")) {
                ci.cancel();
            }
        }
    }

    private Text parseColorCodes(String text) {
        if (text == null || text.isEmpty()) {
            return Text.literal("");
        }

        MutableText result = Text.literal("");
        Pattern pattern = Pattern.compile("&#([0-9A-Fa-f]{6})|([§&])([0-9a-fk-or])");
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        int currentColor = 0xFFFFFF;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strikethrough = false;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String segment = text.substring(lastEnd, matcher.start());
                MutableText segmentText = Text.literal(segment);
                final int color = currentColor;
                final boolean b = bold, i = italic, u = underline, s = strikethrough;
                segmentText.styled(style -> {
                    style = style.withColor(color);
                    if (b) style = style.withBold(true);
                    if (i) style = style.withItalic(true);
                    if (u) style = style.withUnderline(true);
                    if (s) style = style.withStrikethrough(true);
                    return style;
                });
                result.append(segmentText);
            }

            if (matcher.group(1) != null) {
                String hexColor = matcher.group(1);
                currentColor = Integer.parseInt(hexColor, 16);
                bold = italic = underline = strikethrough = false;
            } else if (matcher.group(2) != null && matcher.group(3) != null) {
                char c = matcher.group(3).charAt(0);
                Formatting formatting = Formatting.byCode(c);

                if (formatting != null) {
                    if (formatting.isColor()) {
                        Integer color = formatting.getColorValue();
                        currentColor = color != null ? color : 0xFFFFFF;
                        bold = italic = underline = strikethrough = false;
                    } else {
                        switch (c) {
                            case 'l':
                                bold = true;
                                break;
                            case 'o':
                                italic = true;
                                break;
                            case 'n':
                                underline = true;
                                break;
                            case 'm':
                                strikethrough = true;
                                break;
                            case 'r':
                                currentColor = 0xFFFFFF;
                                bold = italic = underline = strikethrough = false;
                                break;
                        }
                    }
                }
            }

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String segment = text.substring(lastEnd);
            MutableText segmentText = Text.literal(segment);
            final int color = currentColor;
            final boolean b = bold, i = italic, u = underline, s = strikethrough;
            segmentText.styled(style -> {
                style = style.withColor(color);
                if (b) style = style.withBold(true);
                if (i) style = style.withItalic(true);
                if (u) style = style.withUnderline(true);
                if (s) style = style.withStrikethrough(true);
                return style;
            });
            result.append(segmentText);
        }

        return result;
    }

    private void renderCustomScoreboard(DrawContext context, Text title, String[] lines) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int maxWidth = 0;
        for (String line : lines) {
            Text parsedLine = parseColorCodes(line + "3");
            int width = textRenderer.getWidth(parsedLine);
            if (width > maxWidth) {
                maxWidth = width;
            }
        }

        int titleWidth = textRenderer.getWidth(title);
        if (titleWidth > maxWidth) {
            maxWidth = titleWidth;
        }

        int boardWidth = maxWidth;
        int lineHeight = 9;
        int titleHeight = lineHeight;
        int boardHeight = lines.length * lineHeight;


        int x = screenWidth - boardWidth - 1;
        int y = screenHeight / 2 - (boardHeight + titleHeight) / 2 - titleHeight - 10;


        context.fill(x - 2, y, x + boardWidth, y + titleHeight, 0x66000000);


        context.drawText(textRenderer, title, x + boardWidth / 2 - titleWidth / 2, y + 1, 0xFFFFFF, false);


        context.fill(x - 2, y + titleHeight, x + boardWidth, y + titleHeight + boardHeight, 0x50000000);


        int currentY = y + titleHeight;
        for (String line : lines) {
            Text parsedLine = parseColorCodes(line);
            context.drawText(textRenderer, parsedLine, x, currentY, 0xFFFFFF, false);
            currentY += lineHeight;
        }
    }

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void onRenderScoreboard(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (RadiumClient.moduleManager == null) return;
        if (RadiumClient.moduleManager.getModule(HideScoreboard.class).isEnabled()) {
            ci.cancel();
        }

        FakeScoreboard module = FakeScoreboard.get();
        if (module == null || !module.isEnabled()) {
            return;
        }

        if (!ci.isCancelled()) {
            ci.cancel();
        }


        Text title = parseColorCodes("&#007cf9&lD&#0089f9&lo&#0096f9&ln&#00a3f9&lu&#00b0f9&lt&#00bdf9 &#00b0f9&lS&#00b7f9&lM&#00c6f9&lP");


        String[] lines = new String[]{
                "",
                "&#00FC00&l$ &fMoney &#00FC00" + (module.realMoney.getValue() ? ScoreboardUtils.getMoney() : module.money.getValue()),
                "&#A303F9★ &fShards &#A303F9" + module.shards.getValue(),
                "&#FC0000\uD83D\uDDE1 &fKills &#FC0000" + module.kills.getValue(),
                "&#F97603☠ &fDeaths &#F97603" + module.deaths.getValue(),
                "&#00A4FC⌛ &fKeyall &#00A4FC" + (module.realKey.getValue() ? ScoreboardUtils.getKeyallTimer() : module.keyAll.getValue()),
                "&#FCE300⌚ &fPlaytime &#FCE300" + module.playtime.getValue(),
                "&#00A4FC\uD83E\uDE93 &fTeam &#00A4FC" + module.team.getValue(),
                "",
                "&7" + ScoreboardUtils.getRegion(module.hideRegion.getValue()) + " &7(&#00A4FC" + ScoreboardUtils.getPing() + "ms&7)"
        };

        String[] noTeam = new String[]{
                "",
                "&#00FC00&l$ &fMoney &#00FC00" + (module.realMoney.getValue() ? ScoreboardUtils.getMoney() : module.money.getValue()),
                "&#A303F9★ &fShards &#A303F9" + module.shards.getValue(),
                "&#FC0000\uD83D\uDDE1 &fKills &#FC0000" + module.kills.getValue(),
                "&#F97603☠ &fDeaths &#F97603" + module.deaths.getValue(),
                "&#00A4FC⌛ &fKeyall &#00A4FC" + (module.realKey.getValue() ? ScoreboardUtils.getKeyallTimer() : module.keyAll.getValue()),
                "&#FCE300⌚ &fPlaytime &#FCE300" + module.playtime.getValue(),
                "",
                "&7" + ScoreboardUtils.getRegion(module.hideRegion.getValue()) + " &7(&#00A4FC" + ScoreboardUtils.getPing() + "ms&7)"
        };

        if (module.team.getValue().equals("")) {
            renderCustomScoreboard(context, title, noTeam);
        } else {
            renderCustomScoreboard(context, title, lines);
        }

    }
}

