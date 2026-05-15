package com.radium.client.modules.misc;
// radium client

import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.NumberSetting;
import com.radium.client.gui.settings.Setting;
import com.radium.client.modules.Module;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public class AutoReconnect extends Module {
    private final NumberSetting time = new NumberSetting("Delay", 3.5, 0.0, 60.0, 0.1);

    private final BooleanSetting hideButtons = new BooleanSetting("Hide Buttons", false);

    public ServerAddress lastServerAddress;
    public ServerInfo lastServerInfo;

    public AutoReconnect() {
        super("AutoReconnect", "Automatically reconnects when disconnected from a server", Module.Category.MISC);
        Setting<?>[] settingArray = new Setting<?>[]{this.time, this.hideButtons};
        this.addSettings(settingArray);
    }

    public void setLastServer(ServerAddress address, ServerInfo info) {
        this.lastServerAddress = address;
        this.lastServerInfo = info;
    }

    public double getTime() {
        return this.time.getValue();
    }

    public boolean shouldHideButtons() {
        return this.hideButtons.getValue();
    }

    public boolean hasLastServer() {
        return this.lastServerAddress != null;
    }
}
