package com.radium.client.modules.misc;

import com.radium.client.modules.Module;

public class SnapTap extends Module {

    public static long LEFT_STRAFE_LAST_PRESS_TIME = 0;
    public static long RIGHT_STRAFE_LAST_PRESS_TIME = 0;

    public static long FORWARD_STRAFE_LAST_PRESS_TIME = 0;
    public static long BACKWARD_STRAFE_LAST_PRESS_TIME = 0;

    public SnapTap() {
        super("SnapTap", "Allows you to strafe freely", Category.MISC);
    }

}

