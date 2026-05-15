package com.radium.client.modules.donut;
// radium client

import com.radium.client.modules.Module;
import net.minecraft.client.MinecraftClient;

public class HideScoreboard extends Module {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public HideScoreboard() {
        super("HideScoreboard", "Hides the sidebar scoreboard", Category.DONUT);
    }

}
