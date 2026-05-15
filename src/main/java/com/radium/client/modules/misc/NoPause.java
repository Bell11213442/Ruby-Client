package com.radium.client.modules.misc;
// radium client

import com.radium.client.client.RadiumClient;
import com.radium.client.modules.Module;

public class NoPause extends Module {

    public NoPause() {
        super("NoPause", "Alt TAB Without Pausing", Category.MISC);
    }

    public static NoPause get() {
        return RadiumClient.moduleManager.getModule(NoPause.class);
    }
}

