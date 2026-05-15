package com.radium.client.modules.client;
// radium client

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.modules.Module;
import net.minecraft.client.network.ServerInfo;

import java.time.OffsetDateTime;

public class DiscordPresence extends Module {
    private static final int RECONNECT_COOLDOWN = 100;
    private static final String APPLICATION_ID = "1134723440650223706";
    private static final int TEXT_SWITCH_INTERVAL = 100;
    private final BooleanSetting showServerIp = new BooleanSetting("Show Server IP", true);
    private final BooleanSetting showElapsedTime = new BooleanSetting("Show Elapsed Time", true);
    private IPCClient client;
    private long startTimestamp;
    private boolean isConnected = false;
    private int reconnectDelay = 0;
    private int textSwitchCounter = 0;

    public DiscordPresence() {
        super("DiscordRPC", "Shows your Minecraft activity on Discord", Module.Category.CLIENT);
        Setting<?>[] settingArray = new Setting<?>[]{
                this.showServerIp, this.showElapsedTime
        };
        this.addSettings(settingArray);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.startTimestamp = System.currentTimeMillis();
        this.textSwitchCounter = 0;
        this.connectToDiscord();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.disconnectFromDiscord();
    }

    @Override
    public void onTick() {
        if (this.reconnectDelay > 0) {
            --this.reconnectDelay;
            return;
        }

        if (!this.isConnected) {
            this.connectToDiscord();
            this.reconnectDelay = RECONNECT_COOLDOWN;
            return;
        }

        this.textSwitchCounter++;
        if (this.textSwitchCounter >= TEXT_SWITCH_INTERVAL) {
            this.textSwitchCounter = 0;
        }

        this.updatePresence();
    }

    private void connectToDiscord() {
        try {
            if (this.client != null) {
                this.disconnectFromDiscord();
            }

            this.client = new IPCClient(Long.parseLong(APPLICATION_ID));
            this.client.setListener(new IPCListener() {
                @Override
                public void onReady(IPCClient client) {
                    isConnected = true;
                    updatePresence();
                }

                public void onClose(IPCClient client, com.google.gson.JsonObject data) {
                    isConnected = false;
                }

                @Override
                public void onDisconnect(IPCClient client, Throwable t) {
                    isConnected = false;
                }
            });

            this.client.connect();
        } catch (NoDiscordClientException e) {
            this.isConnected = false;
        } catch (Exception e) {
            this.isConnected = false;
        }
    }

    private void disconnectFromDiscord() {
        if (this.client != null && this.isConnected) {
            try {
                this.client.close();
            } catch (Exception e) {

            }
            this.client = null;
        }
        this.isConnected = false;
    }

    private void updatePresence() {
        if (!this.isConnected || this.client == null) {
            return;
        }

        try {
            RichPresence.Builder builder = new RichPresence.Builder();

            String details = "Using Radium Client";
            String state = "";

            if (this.showServerIp.getValue()) {
                boolean showFirstText = (this.textSwitchCounter < TEXT_SWITCH_INTERVAL / 2);

                if (mc.player != null && mc.getCurrentServerEntry() != null) {
                    ServerInfo serverInfo = mc.getCurrentServerEntry();
                    if (showFirstText) {
                        details = "Using Radium Client";
                    } else {
                        details = "Playing on " + serverInfo.address;
                    }
                } else if (mc.player != null && mc.isInSingleplayer()) {
                    if (showFirstText) {
                        details = "Using Radium Client";
                    } else {
                        details = "Playing Singleplayer";
                    }
                } else if (mc.currentScreen != null) {
                    if (showFirstText) {
                        details = "Using Radium Client";
                    } else {
                        details = "In Main Menu";
                    }
                }
            }

            builder.setDetails(details);
            if (!state.isEmpty()) {
                builder.setState(state);
            }

            if (this.showElapsedTime.getValue()) {
                builder.setStartTimestamp(OffsetDateTime.now().minusSeconds((System.currentTimeMillis() - this.startTimestamp) / 1000));
            }

            builder.setLargeImage("radium_logo", "Radium Client");

            this.client.sendRichPresence(builder.build());
        } catch (Exception e) {
            this.isConnected = false;
        }
    }
}
