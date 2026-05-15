package com.radium.client.events.event;
// radium client

import com.radium.client.events.CancellableEvent;
import com.radium.client.events.Listener;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;

public interface OpenScreenListener extends Listener {
    void onOpenScreen(OpenScreenEvent event);

    class OpenScreenEvent extends CancellableEvent<OpenScreenListener> {
        public Screen screen;

        public OpenScreenEvent(Screen screen) {
            this.screen = screen;
        }

        @Override
        public void fire(ArrayList<OpenScreenListener> listeners) {
            listeners.forEach(e -> e.onOpenScreen(this));
        }

        @Override
        public Class<OpenScreenListener> getListenerType() {
            return OpenScreenListener.class;
        }
    }
}

