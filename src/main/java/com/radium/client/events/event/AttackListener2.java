package com.radium.client.events.event;
// radium client

import com.radium.client.events.CancellableEvent;
import com.radium.client.events.Listener;
import net.minecraft.entity.Entity;

import java.util.ArrayList;

public interface AttackListener2 extends Listener {
    void onAttack(AttackEvent2 event);

    class AttackEvent2 extends CancellableEvent<AttackListener2> {
        public final Entity entity;

        public AttackEvent2(Entity entity) {
            this.entity = entity;
        }

        @Override
        public void fire(ArrayList<AttackListener2> listeners) {
            listeners.forEach(e -> e.onAttack(this));
        }

        @Override
        public Class<AttackListener2> getListenerType() {
            return AttackListener2.class;
        }
    }
}
