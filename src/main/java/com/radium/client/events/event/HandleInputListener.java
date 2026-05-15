package com.radium.client.events.event;
// radium client

import com.radium.client.events.Event;
import com.radium.client.events.Listener;

import java.util.ArrayList;

public interface HandleInputListener extends Listener {
    void onHandleInput();

    class HandleInput extends Event<HandleInputListener> {

        @Override
        public void fire(ArrayList<HandleInputListener> listeners) {
            listeners.forEach(HandleInputListener::onHandleInput);
        }

        @Override
        public Class<HandleInputListener> getListenerType() {
            return HandleInputListener.class;
        }
    }
}

