package de.canitzp.mcserverwrapper.plugins.event;

import de.canitzp.mcserverwrapper.ign.User;
import de.canitzp.mcserverwrapper.plugins.PluginEvent;

import java.util.function.BiConsumer;

public class ChatEvent extends PluginEvent {
    
    private BiConsumer<User, String> action;
    
    public ChatEvent(BiConsumer<User, String> action) {
        this.action = action;
    }
    
    public void onChatMessage(User user, String message){
        this.action.accept(user, message);
    }
}
