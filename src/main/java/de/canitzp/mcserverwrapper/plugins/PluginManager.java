package de.canitzp.mcserverwrapper.plugins;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.Settings;
import de.canitzp.mcserverwrapper.Util;
import de.canitzp.mcserverwrapper.ign.User;
import de.canitzp.mcserverwrapper.plugins.event.ChatEvent;
import de.canitzp.mcserverwrapper.plugins.event.PlayerEvents;
import de.canitzp.mcserverwrapper.plugins.event.ServerEvents;
import de.canitzp.mcserverwrapper.plugins.event.TickEvent;
import de.canitzp.mcserverwrapper.plugins.internal.DiscordChatBridge;
import de.canitzp.mcserverwrapper.plugins.internal.WebMap;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public class PluginManager implements Runnable {
    
    private static final String LOG_NAME = "Plugin Manager";
    private static final int TICK_TIME = 1;//50;
    
    private final MCServerWrapper wrapper;
    private final List<DefaultPlugin> plugins = new ArrayList<>();
    private Path pluginDirectory;
    private Path pluginSettingsDirectory;
    private boolean shouldRun = true;
    
    //private final PluginCommunicator pluginCommunicator = new PluginCommunicator(this);
    
    public PluginManager(MCServerWrapper wrapper){
        this.wrapper = wrapper;
    }
    
    @Override
    public void run(){
        long nextTick = System.currentTimeMillis();
        while(this.shouldRun && !this.plugins.isEmpty()){
            if(nextTick <= System.currentTimeMillis()){
                nextTick = System.currentTimeMillis() + (1000 / TICK_TIME);
                for(DefaultPlugin plugin : this.plugins){
                    plugin.getEvent(TickEvent.class).ifPresent(TickEvent::tick);
                }
            }
            long sleepTime = nextTick - System.currentTimeMillis();
            if(sleepTime > 0){
                this.wrapper.sleep(sleepTime - 1);
            } else if(sleepTime < 0){
                this.wrapper.getLog().warn(LOG_NAME, "One plugin tick took longer than allowed! This can lead to slower ticks fpr plugins! Time since last tick: " + (-sleepTime) + "ms, should be zero or negative!");
            }
        }
    }
    
    // load/reload all plugins
    public void reload(boolean overwriteSettings){
        this.pluginDirectory = Paths.get(wrapper.getSettings().getFile("general.jar_path").getParent(), "plugins");
        this.pluginSettingsDirectory = Paths.get(pluginDirectory.toFile().getAbsolutePath(), "settings");
        if(!this.pluginSettingsDirectory.toFile().exists()){
            this.pluginSettingsDirectory.toFile().mkdirs();
        }
        
        this.plugins.forEach(DefaultPlugin::stop);
        this.plugins.clear();
        
        if(this.wrapper.getSettings().getBoolean("internal_plugins.enable_discord_bridge")){
            Settings settings = new Settings(new File(pluginSettingsDirectory.toFile(), "discord_bridge.conf"), "discord_bridge.default.conf");
            if(overwriteSettings){
                settings.overwriteCurrentConfig();
            }
            DiscordChatBridge dcb = new DiscordChatBridge();
            dcb.setPluginSettings(settings);
            dcb.setWrapper(this.wrapper);
            this.plugins.add(dcb);
        }
        
        if(this.wrapper.getSettings().getBoolean("internal_plugins.enable_webmap")){
            Settings settings = new Settings(new File(pluginSettingsDirectory.toFile(), "webmap.conf"), "webmap.default.conf");
            if(overwriteSettings){
                settings.overwriteCurrentConfig();
            }
            WebMap webMap = new WebMap();
            webMap.setPluginSettings(settings);
            webMap.setWrapper(this.wrapper);
            this.plugins.add(webMap);
        }
        
        this.wrapper.getCommandHandler().clearCommands();
        this.plugins.forEach(defaultPlugin -> defaultPlugin.registerCommand(this.wrapper.getCommandHandler()));
        this.plugins.forEach(defaultPlugin -> defaultPlugin.registerCommand(this.wrapper.getCommandHandler().getCommandDispatcher()));
        
        this.plugins.forEach(DefaultPlugin::init);
    }
    
    public void stop(){
        this.shouldRun = false;
        this.plugins.forEach(DefaultPlugin::stop);
    }
    
    public List<DefaultPlugin> getPlugins(){
        return plugins;
    }
    
    public MCServerWrapper getWrapper(){
        return wrapper;
    }
    
    private <T extends PluginEvent> void tryToFireEvent(Class<T> eventType, Consumer<T> consumer){
        this.plugins.forEach(defaultPlugin -> defaultPlugin.getEvent(eventType).ifPresent(consumer));
    }
    
    private <T extends PluginEvent, R> R tryToFireEventWithReturn(Class<T> eventType, Function<T, R> function){
        for(DefaultPlugin plugin : this.plugins){
            Optional<T> event = plugin.getEvent(eventType);
            return event.map(function).orElse(null);
        }
        return null;
    }
    
    public void onServerStart(List<String> startupCommand){
        this.tryToFireEvent(ServerEvents.Start.class, start -> start.onServerStart(startupCommand));
    }
    
    public void onServerStared(int serverStartupTimeMilliseconds){
        this.tryToFireEvent(ServerEvents.Started.class, started -> started.onServerStarted(serverStartupTimeMilliseconds));
    }
    
    public void onServerStop(){
        this.tryToFireEvent(ServerEvents.Stop.class, ServerEvents.Stop::onServerStop);
    }
    
    public void onServerStopped(long uptime, int exitCode){
        this.tryToFireEvent(ServerEvents.Stopped.class, stopped -> stopped.onServerStopped(uptime, exitCode));
    }
    
    public void onServerSaving(){
        this.tryToFireEvent(ServerEvents.Saving.class, ServerEvents.Saving::onServerSaving);
    }
    
    public void onServerSaved(){
        this.tryToFireEvent(ServerEvents.Saved.class, ServerEvents.Saved::onServerSaved);
    }
    
    public boolean onServerMessage(String msg){
        return Util.saveBooleanUnboxing(this.tryToFireEventWithReturn(ServerEvents.Message.class, message -> message.onMessage(msg)), true);
    }
    
    public boolean onChatMessage(User user, String message){
        return Util.saveBooleanUnboxing(this.tryToFireEventWithReturn(ChatEvent.class, chatEvent -> chatEvent.onChatMessage(user, message)), true);
    }
    
    public void onPlayerJoin(User user){
        this.tryToFireEvent(PlayerEvents.Join.class, join -> join.onPlayerJoin(user));
    }
    
    public void onPlayerLeave(User user){
        this.tryToFireEvent(PlayerEvents.Leave.class, leave -> leave.onPlayerLeave(user));
    }
    
    public void onPlayerAdvancement(User user, String advancementName){
        this.tryToFireEvent(PlayerEvents.Advancement.class, advancement -> advancement.onPlayerAdvancement(user, advancementName));
    }
    
    public void onPlayerReachedGoal(User user, String goalName){
        this.tryToFireEvent(PlayerEvents.Goal.class, goal -> goal.onPlayerReachedGoal(user, goalName));
    }
    
    public void onPlayerCompletedChallenge(User user, String challengeName){
        this.tryToFireEvent(PlayerEvents.Challenge.class, challenge -> challenge.onPlayerCompletedChallenge(user, challengeName));
    }
    
}
