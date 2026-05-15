package com.radium.client.events.event;
// radium client

import com.radium.client.events.CancellableEvent;
import com.radium.client.events.Listener;

import java.util.ArrayList;

public interface BlockBreakingListener extends Listener {
    void onBlockBreaking(BlockBreakingEvent event);

    class BlockBreakingEvent extends CancellableEvent<BlockBreakingListener> {

        @Override
        public void fire(ArrayList<BlockBreakingListener> listeners) {
            listeners.forEach(e -> e.onBlockBreaking(this));
        }

        @Override
        public Class<BlockBreakingListener> getListenerType() {
            return BlockBreakingListener.class;
        }
    }
}

