package de.canitzp.mcserverwrapper.plugins.event;

import de.canitzp.mcserverwrapper.ign.User;
import de.canitzp.mcserverwrapper.plugins.PluginEvent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class ChatEvent extends PluginEvent {
    
    private BiFunction<User, String, Boolean> action;
    
    public ChatEvent(BiFunction<User, String, Boolean> action) {
        this.action = action;
    }
    
    public boolean onChatMessage(User user, String message){
        return this.action.apply(user, message);
    }
}
