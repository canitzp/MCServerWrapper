package de.canitzp.mcserverwrapper.plugins;

public abstract class PluginEvent {
    
    private boolean isCanceled = false;
    
    public void cancel(){
        this.isCanceled = true;
    }
    
}
