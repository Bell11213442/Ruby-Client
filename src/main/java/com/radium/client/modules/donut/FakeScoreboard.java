package com.radium.client.modules.donut;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.gui.settings.BooleanSetting;
import com.radium.client.gui.settings.StringSetting;
import com.radium.client.modules.Module;

public class FakeScoreboard extends Module {
    public final StringSetting money = new StringSetting("Money", "6b");
    public final StringSetting shards = new StringSetting("Shards", "2.3K");
    public final StringSetting kills = new StringSetting("Kills", "503");
    public final StringSetting deaths = new StringSetting("Deaths", "421");
    public final StringSetting keyAll = new StringSetting("Key All", "67m 67s");
    public final StringSetting playtime = new StringSetting("Playtime", "22d 9h");
    public final StringSetting team = new StringSetting("Team", "RadiumClient");
    public final BooleanSetting realMoney = new BooleanSetting("Show Real Money", false);
    public final BooleanSetting realKey = new BooleanSetting("Show Real KeyAll", true);
    public final BooleanSetting hideRegion = new BooleanSetting("Hide the region", false);
    int waitTicks = 0;

    public FakeScoreboard() {
        super(
                ("FakeScoreboard"),
                ("Shows a fake scoreboard."),
                Category.DONUT
        );

        this.addSettings(money, shards, kills, deaths, keyAll, playtime, team, realMoney, realKey, hideRegion);
    }

    public static FakeScoreboard get() {

        return RadiumClient.moduleManager.getModule(FakeScoreboard.class);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        waitTicks = 0;
    }
}
