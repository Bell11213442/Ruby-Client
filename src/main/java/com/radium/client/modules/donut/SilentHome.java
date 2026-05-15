package com.radium.client.modules.donut;
// radium client

import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.KeybindSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.utils.KeyUtils;
import com.radium.client.utils.WebhookUtils;
import net.minecraft.client.util.ScreenshotRecorder;

import java.io.File;

import static com.radium.client.client.RadiumClient.eventManager;

public class SilentHome extends Module implements TickListener {
    private final StringSetting webhookUrl = new StringSetting("Webhook URL", "");
    private final BooleanSetting screenshot = new BooleanSetting("Screenshot", true);
    private final KeybindSetting triggerKey = new KeybindSetting("Trigger Key", 71);
    private final BooleanSetting delHome = new BooleanSetting("Delete Previous Home", true);
    private final NumberSetting home = new NumberSetting("Home Slot", 1, 1, 5, 1);

    private boolean wasKeyPressed = false;
    private boolean suppressActionBar = false;
    private int suppressTicks = 0;
    private boolean suppressChatMessages = false;
    private int suppressChatTicks = 0;
    private long lastTriggerTime = 0;

    private int delayTicks = 0;
    private boolean waitingToSnap = false;

    public SilentHome() {
        super("SilentHomeSetter", "Sets a home at your current coordinates without saying 'Home Set' in the chat or anywhere else.", Category.DONUT);
        addSettings(webhookUrl, screenshot, triggerKey, delHome, home);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        eventManager.add(TickListener.class, this);
        wasKeyPressed = false;
        lastTriggerTime = 0;
        delayTicks = 0;
        waitingToSnap = false;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        eventManager.remove(TickListener.class, this);
        wasKeyPressed = false;
        suppressActionBar = false;
        suppressTicks = 0;
        suppressChatMessages = false;
        suppressChatTicks = 0;
        lastTriggerTime = 0;
        delayTicks = 0;
        waitingToSnap = false;
    }

    @Override
    public void onTick2() {
        if (mc.currentScreen != null) {
            return;
        }

        if (mc.player == null) {
            return;
        }

        if (suppressActionBar) {
            suppressTicks--;
            if (suppressTicks <= 0) {
                suppressActionBar = false;
            }
        }

        if (suppressChatMessages) {
            suppressChatTicks--;
            if (suppressChatTicks <= 0) {
                suppressChatMessages = false;
            }
        }

        if (waitingToSnap) {
            delayTicks--;
            if (delayTicks <= 0) {
                homeCoords();
                waitingToSnap = false;
            }
        }

        int key = triggerKey.getValue();
        if (key == -1) {
            return;
        }

        boolean isKeyPressed = KeyUtils.isKeyPressed(key);

        if (isKeyPressed && !wasKeyPressed) {
            suppressActionBar = true;
            suppressTicks = 40;

            suppressChatMessages = true;
            suppressChatTicks = 40;

            if (delHome.getValue()) {
                mc.getNetworkHandler().sendChatCommand("delhome " + home.getValue().intValue());
            }

            delayTicks = 10;
            waitingToSnap = true;
        }

        wasKeyPressed = isKeyPressed;
    }

    private void homeCoords() {
        if (mc.player == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTriggerTime < 500) {
            return;
        }
        lastTriggerTime = currentTime;

        mc.getNetworkHandler().sendChatCommand("sethome " + home.getValue());

        String url = webhookUrl.getValue();
        if (url.trim().isEmpty() || !url.startsWith("https://")) {
            return;
        }

        final String screenshotName = "home_" + (int) Math.round(mc.player.getX()) + "_" + (int) Math.round(mc.player.getY()) + "_" + (int) Math.round(mc.player.getZ()) + "_" + System.currentTimeMillis();

        if (!screenshot.getValue()) {
            new WebhookUtils(url)
                    .setTitle("Home Snapped")
                    .addCoords()
                    .addServer()
                    .addTime()
                    .send();
            return;
        }

        try {
            ScreenshotRecorder.saveScreenshot(mc.runDirectory, screenshotName + ".png", mc.getFramebuffer(), (text) -> {
            });
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        File screenshotFile = new File(mc.runDirectory, "screenshots/" + screenshotName + ".png");
        if (screenshotFile.exists()) {
            new WebhookUtils(url)
                    .setTitle("Home Snapped")
                    .addCoords()
                    .addServer()
                    .addTime()
                    .setScreenshot(screenshotFile)
                    .send();
        }
    }

    public boolean shouldSuppressActionBar() {
        return suppressActionBar;
    }

    public boolean shouldSuppressChatMessages() {
        return suppressChatMessages;
    }
}
