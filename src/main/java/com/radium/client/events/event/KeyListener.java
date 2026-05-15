package com.radium.client.events.event;
// radium client

import com.radium.client.events.CancellableEvent;
import com.radium.client.events.Listener;

import java.util.ArrayList;

public interface KeyListener extends Listener {
    void onKey(final KeyEvent event);

    class KeyEvent extends CancellableEvent<KeyListener> {
        public int key;
        public int mode;
        public long window;

        public KeyEvent(int key, int mode, long window) {
            this.key = key;
            this.mode = mode;
            this.window = window;
        }

        @Override
        public void fire(ArrayList<KeyListener> listeners) {
            listeners.forEach(e -> e.onKey(this));
        }

        @Override
        public Class<KeyListener> getListenerType() {
            return KeyListener.class;
        }
    }
}
