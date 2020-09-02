package de.canitzp.mcserverwrapper.plugins.event;

import de.canitzp.mcserverwrapper.ign.User;
import de.canitzp.mcserverwrapper.plugins.PluginEvent;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PlayerEvents {
    
    public static class Join extends PluginEvent {
        
        private Consumer<User> action;
        
        public Join(Consumer<User> action){
            this.action = action;
        }
        
        public void onPlayerJoin(User user){
            this.action.accept(user);
        }
    }
    
    public static class Leave extends PluginEvent {
        
        private Consumer<User> action;
        
        public Leave(Consumer<User> action){
            this.action = action;
        }
        
        public void onPlayerLeave(User user){
            this.action.accept(user);
        }
    }
    
    public static class Advancement extends PluginEvent {
        
        private BiConsumer<User, String> action;
        
        public Advancement(BiConsumer<User, String> action){
            this.action = action;
        }
        
        public void onPlayerAdvancement(User user, String advancementName){
            this.action.accept(user, advancementName);
        }
    }
    
    public static class Goal extends PluginEvent {
        
        private BiConsumer<User, String> action;
        
        public Goal(BiConsumer<User, String> action){
            this.action = action;
        }
        
        public void onPlayerReachedGoal(User user, String goalName){
            this.action.accept(user, goalName);
        }
    }
    
    public static class Challenge extends PluginEvent {
        
        private BiConsumer<User, String> action;
        
        public Challenge(BiConsumer<User, String> action){
            this.action = action;
        }
        
        public void onPlayerCompletedChallenge(User user, String challengeName){
            this.action.accept(user, challengeName);
        }
    }
    
}
