package com.radium.client.events.event;
// radium client

import com.radium.client.events.Event;
import com.radium.client.events.Listener;

import java.util.ArrayList;

public interface TickListener extends Listener {
    void onTick2();

    class TickEvent extends Event<TickListener> {

        @Override
        public void fire(ArrayList<TickListener> listeners) {
            listeners.forEach(TickListener::onTick2);
        }

        @Override
        public Class<TickListener> getListenerType() {
            return TickListener.class;
        }
    }
}

