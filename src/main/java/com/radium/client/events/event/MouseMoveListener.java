package com.radium.client.events.event;
// radium client

import com.radium.client.events.CancellableEvent;
import com.radium.client.events.Listener;

import java.util.ArrayList;

public interface MouseMoveListener extends Listener {
    void onMouseMove(MouseMoveEvent event);

    class MouseMoveEvent extends CancellableEvent<MouseMoveListener> {
        public double x, y;

        public MouseMoveEvent(double x, double y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public void fire(ArrayList<MouseMoveListener> listeners) {
            listeners.forEach(listener -> listener.onMouseMove(this));
        }

        @Override
        public Class<MouseMoveListener> getListenerType() {
            return MouseMoveListener.class;
        }
    }
}

