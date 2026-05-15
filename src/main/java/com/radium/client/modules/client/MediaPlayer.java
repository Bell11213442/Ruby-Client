package com.radium.client.modules.client;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.modules.Module;
import com.radium.client.themes.Theme;
import com.radium.client.themes.ThemeManager;

import com.radium.client.utils.render.RenderUtils;
import com.radium.util.VString;
import de.labystudio.spotifyapi.SpotifyAPI;
import de.labystudio.spotifyapi.SpotifyAPIFactory;
import de.labystudio.spotifyapi.SpotifyListener;
import de.labystudio.spotifyapi.model.MediaKey;
import de.labystudio.spotifyapi.model.Track;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class MediaPlayer extends Module {

    private final BooleanSetting showAlbumArt = new BooleanSetting(new VString("Show Album Art").toString(), true);
    private final BooleanSetting showProgress = new BooleanSetting(new VString("Show Progress Bar").toString(), true);
    private final BooleanSetting editPosition = new BooleanSetting(new VString("Edit Position").toString(), false);
    private final NumberSetting panelWidth = new NumberSetting(new VString("Width").toString(), 280, 200, 500, 1);

    private final NumberSetting posX = new NumberSetting(new VString("X").toString(), 500, 0, 4000, 1);
    private final NumberSetting posY = new NumberSetting(new VString("Y").toString(), 500, 0, 2000, 1);
    private final Object trackLock = new Object();
    private SpotifyAPI spotifyAPI;
    private Track currentTrack;
    private int currentPosition = 0;
    private boolean isPlaying = false;
    private boolean isConnected = false;
    private boolean initialized = false;
    private long lastUpdateTime = 0;
    private volatile boolean spotifyOperationInProgress = false;

    private Identifier albumArtTexture = null;
    private boolean textureNeedsUpdate = false;
    private BufferedImage pendingAlbumArt = null;

    public MediaPlayer() {
        super("SpotiPlay", "Shows currently playing Spotify track", Category.CLIENT);
        addSettings(showAlbumArt, showProgress, editPosition, panelWidth, posX, posY);
    }

    @Override
    public void onTick() {
        if (editPosition.getValue()) {
            editPosition.setValue(false);
            if (RadiumClient.mc != null) {
                RadiumClient.mc.execute(() -> RadiumClient.mc.setScreen(new com.radium.client.gui.HudEditorScreen()));
            }
        }
    }

    public NumberSetting getPosX() {
        return posX;
    }

    public NumberSetting getPosY() {
        return posY;
    }

    @Override
    public void onEnable() {
        if (!initialized) {
            initialized = true;
            new Thread(() -> {
                try {
                    initializeSpotifyAPI();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "Spotify-Init").start();
        }
    }

    @Override
    public void onDisable() {
        if (spotifyAPI != null) {
            try {
                spotifyAPI.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeSpotifyAPI() {
        try {
            spotifyAPI = SpotifyAPIFactory.create();
            spotifyAPI.registerListener(new SpotifyListener() {
                @Override
                public void onConnect() {
                    synchronized (trackLock) {
                        isConnected = true;
                    }
                }

                @Override
                public void onTrackChanged(Track track) {
                    synchronized (trackLock) {
                        currentTrack = track;
                        lastUpdateTime = System.currentTimeMillis();
                    }

                    if (track != null && track.getCoverArt() != null) {
                        pendingAlbumArt = track.getCoverArt();
                        textureNeedsUpdate = true;
                    }
                }

                @Override
                public void onPositionChanged(int position) {
                    synchronized (trackLock) {
                        currentPosition = position;
                        lastUpdateTime = System.currentTimeMillis();
                    }
                }

                @Override
                public void onPlayBackChanged(boolean playing) {
                    synchronized (trackLock) {
                        isPlaying = playing;
                        lastUpdateTime = System.currentTimeMillis();
                    }
                }

                @Override
                public void onSync() {
                }

                @Override
                public void onDisconnect(Exception exception) {
                    synchronized (trackLock) {
                        isConnected = false;
                    }
                }
            });
            spotifyAPI.initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void render(DrawContext context, float tickDelta) {
        if (!isEnabled() || RadiumClient.mc.player == null) return;

        if (textureNeedsUpdate && pendingAlbumArt != null) {
            updateAlbumArtTexture(pendingAlbumArt);
            textureNeedsUpdate = false;
            pendingAlbumArt = null;
        }

        RenderUtils.unscaledProjection();

        // Get theme from ThemeManager to match HUD
        Theme theme = ThemeManager.getHudTheme();
        HUD hud = RadiumClient.getModuleManager().getModule(HUD.class);
        int alphaValue = hud != null ? hud.alpha.getValue().intValue() : 80;
        int boxPadding = theme.getBoxPadding();
        float textScale = theme.getTextScale();
        int backgroundColor = theme.getBackgroundColor(alphaValue);
        int shadowColor = theme.getShadowColor();
        int borderColor = theme.getBorderColor();
        int textColor = theme.getTextColor(hud);
        int secondaryTextColor = theme.getSecondaryTextColor(0, 0, hud);
        int radius = theme.getRadius(3);
        boolean useShadows = theme.useShadows();
        boolean useBorders = theme.useBorders();

        int x = posX.getValue().intValue();
        int y = posY.getValue().intValue();
        int width = panelWidth.getValue().intValue();
        int artSize = 48; // Slightly smaller to match HUD scale
        int padding = boxPadding;
        int height = artSize + padding * 2 + 20;

        String trackName;
        String artistName;
        int duration;
        int position;
        boolean playing;

        synchronized (trackLock) {
            if (currentTrack != null) {
                trackName = currentTrack.getName();
                artistName = currentTrack.getArtist();
                duration = currentTrack.getLength() / 1000;
                position = currentPosition / 1000;
                playing = isPlaying;

                if (playing) {
                    long elapsed = System.currentTimeMillis() - lastUpdateTime;
                    position = Math.min(position + (int) (elapsed / 1000), duration);
                }
            } else {
                trackName = "Not Playing";
                artistName = "Open Spotify";
                duration = 0;
                position = 0;
                playing = false;
            }
        }

        // Use theme background with shadows and borders
        if (useShadows) {
            RenderUtils.fillRoundRect(context, x + 1, y + 1, width, height, radius, radius, radius, radius, shadowColor);
        }
        RenderUtils.fillRoundRect(context, x, y, width, height, radius, radius, radius, radius, backgroundColor);
        if (useBorders) {
            RenderUtils.drawRoundRect(context, x, y, width, height, radius, borderColor);
        }

        int headerY = y + 4; // Top left alignment with small padding
        // Use smaller font size and top left alignment
        // Use smaller font size and top left alignment
        context.drawText(RadiumClient.mc.textRenderer, "Listening to Spotify", x + 4, headerY, secondaryTextColor, false);

        int artX = x + padding;
        int artY = headerY + 12 + 4;

        // Album art background with theme colors
        int albumArtBg = theme.getBackgroundColor((int) (alphaValue * 0.8f));
        RenderUtils.fillRoundRect(context, artX, artY, artSize, artSize, radius - 1, albumArtBg);

        if (albumArtTexture != null && showAlbumArt.getValue()) {
            try {
                context.drawTexture(albumArtTexture, artX, artY, 0, 0, artSize, artSize, artSize, artSize);
            } catch (Exception e) {
            }
        }

        int textX = artX + artSize + padding;
        int textAreaWidth = width - artSize - padding * 3;

        int trackY = artY + 4;
        int artistY = trackY + 14 + 2;

        // Use theme font and colors, matching HUD style
        context.drawText(RadiumClient.mc.textRenderer, trackName, textX, trackY, textColor, false);
        context.drawText(RadiumClient.mc.textRenderer, artistName, textX, artistY, secondaryTextColor, false);

        if (showProgress.getValue()) {
            String timePos = formatTime(position);
            String timeDur = formatTime(duration);


            int timeY = artistY + 14 + 4;

            // Use theme font for time display
            int timePosWidth, timeDurWidth;
            timePosWidth = RadiumClient.mc.textRenderer.getWidth(timePos);
            timeDurWidth = RadiumClient.mc.textRenderer.getWidth(timeDur);
            context.drawText(RadiumClient.mc.textRenderer, timePos, textX, timeY, secondaryTextColor, false);

            int barX = textX + timePosWidth + 8;
            int barEndX = x + width - padding - timeDurWidth - 8;
            int barWidth = barEndX - barX;
            int barY = timeY + 4;
            int barHeight = 3;

            if (barWidth > 20) {
                // Use theme colors for progress bar
                int progressBarBg = theme.getHealthBarBgColor();
                RenderUtils.fillRoundRect(context, barX, barY, barWidth, barHeight, 2, progressBarBg);

                if (duration > 0) {
                    float progress = (float) position / duration;
                    int filledWidth = Math.max(2, (int) (barWidth * progress));
                    int progressBarColor = theme.getHealthBarColor(hud);
                    RenderUtils.fillRoundRect(context, barX, barY, filledWidth, barHeight, 2, progressBarColor);
                }
            }

            context.drawText(RadiumClient.mc.textRenderer, timeDur, barEndX + 8, timeY, secondaryTextColor, false);
        }

        RenderUtils.scaledProjection();
    }

    public boolean handleClick(double mouseX, double mouseY, int button) {
        if (!isEnabled() || button != 0) return false;

        int x = posX.getValue().intValue();
        int y = posY.getValue().intValue();
        int width = panelWidth.getValue().intValue();
        int artSize = 64;
        int padding = 12;
        int height = artSize + padding * 2 + 8;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            if (spotifyAPI != null && !spotifyOperationInProgress) {
                spotifyOperationInProgress = true;
                new Thread(() -> {
                    try {
                        spotifyAPI.pressMediaKey(MediaKey.PLAY_PAUSE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        spotifyOperationInProgress = false;
                    }
                }, "Spotify-MediaKey").start();
                return true;
            }
        }

        return false;
    }

    private boolean isInBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private int getHudColor() {
        ClickGUI clickGUI = RadiumClient.moduleManager.getModule(ClickGUI.class);
        if (clickGUI != null) {
            return clickGUI.getHudColor(0);
        }
        return 0xFFFF4444;
    }

    private void updateAlbumArtTexture(BufferedImage image) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();

            NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(bytes));
            NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);

            Identifier newId = Identifier.of("radium", "media_player_art_" + System.currentTimeMillis());
            RadiumClient.mc.getTextureManager().registerTexture(newId, texture);

            if (albumArtTexture != null) {
                try {
                    RadiumClient.mc.getTextureManager().destroyTexture(albumArtTexture);
                } catch (Exception ignored) {
                }
            }

            albumArtTexture = newId;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isConnectedToSpotify() {
        synchronized (trackLock) {
            return isConnected;
        }
    }

    public boolean isCurrentlyPlaying() {
        synchronized (trackLock) {
            return isPlaying;
        }
    }
}

