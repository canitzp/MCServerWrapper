package de.canitzp.mcserverwrapper.plugins.event;

import de.canitzp.mcserverwrapper.plugins.PluginEvent;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ServerEvents {
    
    public static class Start extends PluginEvent {
        
        private Consumer<List<String>> action;
        
        public Start(Consumer<List<String>> action){
            this.action = action;
        }
        
        public void onServerStart(List<String> command){
            this.action.accept(command);
        }
    }
    
    public static class Started extends PluginEvent {
        
        private Consumer<Integer> action;
        
        public Started(Consumer<Integer> action){
            this.action = action;
        }
        
        public void onServerStarted(int startupTimeInMilliseconds){
            this.action.accept(startupTimeInMilliseconds);
        }
    }
    
    public static class Stop extends PluginEvent {
        
        private Runnable action;
        
        public Stop(Runnable action){
            this.action = action;
        }
        
        public void onServerStop(){
            this.action.run();
        }
    }
    
    public static class Stopped extends PluginEvent {
        
        private BiConsumer<Long, Integer> action;
        
        public Stopped(BiConsumer<Long, Integer> action){
            this.action = action;
        }
        
        public void onServerStopped(long uptime, int exitCode){
            this.action.accept(uptime, exitCode);
        }
    }
    
    public static class Saving extends PluginEvent {
        
        private Runnable action;
        
        public Saving(Runnable action){
            this.action = action;
        }
        
        public void onServerSaving(){
            this.action.run();
        }
    }
    
    public static class Saved extends PluginEvent {
        
        private Runnable action;
        
        public Saved(Runnable action){
            this.action = action;
        }
        
        public void onServerSaved(){
            this.action.run();
        }
    }
    
    public static class Message extends PluginEvent {
        
        private Function<String, Boolean> action;
        
        public Message(Function<String, Boolean> action){
            this.action = action;
        }
        
        public boolean onMessage(String msg){
            return this.action.apply(msg);
        }
    }
    
}
