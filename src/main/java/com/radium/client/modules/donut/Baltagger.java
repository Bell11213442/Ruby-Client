package com.radium.client.modules.donut;
// radium client

import com.radium.client.events.event.TickListener;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.ColorSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;
import com.radium.client.modules.render.BaltaggerRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.awt.*;

import static com.radium.client.client.RadiumClient.eventManager;

public class Baltagger extends Module implements TickListener {
    private final StringSetting apiKey = new StringSetting("API Key", "");
    private final BooleanSetting showSelf = new BooleanSetting("Show Self", false);
    private final ColorSetting moneyColor = new ColorSetting("Money Color", new Color(0x00FF00));

    private boolean checkedForApiKey = false;
    private int joinDelayTicks = 0;

    public Baltagger() {
        super("Nametags", "Appends hearts and money to nametags", Category.DONUT);
        this.addSettings(apiKey, showSelf, moneyColor);
        BaltaggerRenderer.register();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        eventManager.add(TickListener.class, this);
        checkedForApiKey = false;
        joinDelayTicks = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        eventManager.remove(TickListener.class, this);
        BaltaggerRenderer.getInstance().clearCache();
    }

    @Override
    public void onTick2() {
        if (mc.world == null || mc.player == null) {
            checkedForApiKey = false;
            joinDelayTicks = 0;
            return;
        }
        if (!isOnDonutSMP()) return;

        if (apiKey.getValue().isEmpty() && !checkedForApiKey) {
            if (joinDelayTicks < 60) {
                joinDelayTicks++;
                return;
            }

            checkedForApiKey = true;
            mc.player.networkHandler.sendChatCommand("api");
        }

        if (apiKey.getValue().isEmpty()) return;

        BaltaggerRenderer.getInstance().onTick(mc);
    }

    public boolean isOnDonutSMP() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo != null) {
            String address = serverInfo.address.toLowerCase();
            return address.contains("donutsmp.net");
        }
        return false;
    }

    public String getApiKey() {
        return apiKey.getValue();
    }

    public boolean shouldShowSelf() {
        return showSelf.getValue();
    }

    public int getMoneyColor() {
        return moneyColor.getValue().getRGB();
    }

    public StringSetting getApiKeySetting() {
        return apiKey;
    }
}


