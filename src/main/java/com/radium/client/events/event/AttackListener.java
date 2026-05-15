package com.radium.client.events.event;
// radium client

import com.radium.client.events.CancellableEvent;
import com.radium.client.events.Listener;

import java.util.ArrayList;

public interface AttackListener extends Listener {
    void onAttack(AttackEvent event);

    class AttackEvent extends CancellableEvent<AttackListener> {

        @Override
        public void fire(ArrayList<AttackListener> listeners) {
            listeners.forEach(e -> e.onAttack(this));
        }

        @Override
        public Class<AttackListener> getListenerType() {
            return AttackListener.class;
        }
    }
}

