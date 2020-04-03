package de.canitzp.mcserverwrapper.plugins.event;

import de.canitzp.mcserverwrapper.plugins.PluginEvent;

public class TickEvent extends PluginEvent {
    
    private Runnable action;
    
    public TickEvent(Runnable action) {
        this.action = action;
    }
    
    public void tick(){
        this.action.run();
    }
}
