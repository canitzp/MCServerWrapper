package de.canitzp.mcserverwrapper.plugins;

import com.mojang.brigadier.CommandDispatcher;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.Settings;
import de.canitzp.mcserverwrapper.commands.CommandHandler;
import de.canitzp.mcserverwrapper.ign.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class DefaultPlugin {
    
    protected Settings pluginSettings;
    protected MCServerWrapper wrapper;
    private String id, name, version;
    private List<PluginEvent> events = new ArrayList<>();
    
    public DefaultPlugin(String id, String name, String version){
        this.id = id;
        this.name = name;
        this.version = version;
    }
    
    public String getId(){
        return id;
    }
    
    public String getName(){
        return name;
    }
    
    public String getVersion(){
        return version;
    }
    
    public void setPluginSettings(Settings pluginSettings){
        this.pluginSettings = pluginSettings;
    }
    
    public void setWrapper(MCServerWrapper wrapper){
        this.wrapper = wrapper;
    }
    
    protected void registerEvent(PluginEvent event){
        this.events.add(event);
    }
    
    <T extends PluginEvent> Optional<T> getEvent(Class<T> c){
        for(PluginEvent event : this.events){
            if(event.getClass().equals(c)){
                return (Optional<T>) Optional.of(event);
            }
        }
        return Optional.empty();
    }
    
    protected abstract void init();
    
    protected abstract void stop();
    
    protected void registerCommand(CommandHandler commandHandler){
    }
    
    protected void registerCommand(CommandDispatcher<User> dispatcher){
    }
}
