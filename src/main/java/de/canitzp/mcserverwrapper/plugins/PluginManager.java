package de.canitzp.mcserverwrapper.plugins;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.Settings;
import de.canitzp.mcserverwrapper.ign.User;
import de.canitzp.mcserverwrapper.plugins.event.ChatEvent;
import de.canitzp.mcserverwrapper.plugins.event.TickEvent;
import de.canitzp.mcserverwrapper.plugins.internal.DiscordChatBridge;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PluginManager implements Runnable{
    
    private static final String LOG_NAME = "Plugin Manager";
    private static final int TICK_TIME = 1;//50;
    
    private final MCServerWrapper wrapper;
    private Path pluginDirectory;
    private Path pluginSettingsDirectory;
    
    private boolean shouldRun = true;
    private final List<DefaultPlugin> plugins = new ArrayList<>();
    
    //private final PluginCommunicator pluginCommunicator = new PluginCommunicator(this);
    
    public PluginManager(MCServerWrapper wrapper){
        this.wrapper = wrapper;
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
        
        this.plugins.forEach(DefaultPlugin::init);
        
        /*this.plugins.clear();
        File pluginRoot = new File(".", "plugins");
        if(!pluginRoot.exists()){
            pluginRoot.mkdirs();
        }
    
        List<File> pluginDirectories = new ArrayList<>();
        File[] allFiles = pluginRoot.listFiles();
        if(allFiles != null){
            Arrays.stream(allFiles).filter(File::isDirectory).forEach(pluginDirectories::add);
        }
        for(File dir : pluginDirectories){
            loadPlugin(dir);
        }
         */
    }
    
    /*
    private void loadPlugin(File pluginDirecotry){
        File pluginMain = new File(pluginDirecotry, "main.groovy");
        if(!pluginMain.exists()){
            this.wrapper.getLog().warn(LOG_NAME, "Plugin directory without 'main.groovy' found! '" + pluginDirecotry.getAbsolutePath() + "'");
            return;
        }
    
        GroovyShell shell = new GroovyShell();
        try{
            Script main = shell.parse(pluginMain);
            Object id = runScriptMethod(main, null, "id", null);
            Object name = runScriptMethod(main, null, "name", null);
            
            if(id instanceof String){
                if(name instanceof String){
                    this.wrapper.getLog().info(LOG_NAME, "Loaded plugin: '" + name + "'");
                    this.plugins.add(new Plugin(wrapper, ((String) id), ((String) name), pluginDirecotry, main));
                } else {
                    this.wrapper.getLog().warn(LOG_NAME, "The plugin with id '" + id + "' has no name specified. This is mandatory and the plugin is not loaded!");
                }
            } else {
                this.wrapper.getLog().warn(LOG_NAME, "The plugin in the '" + pluginDirecotry.getName() + "' folder has no id specified. This is mandatory and the plugin is not loaded!");
            }
        }catch(IOException e){
            e.printStackTrace();
        }
    
        this.wrapper.submitRunnable(() -> {
            this.plugins.forEach(Plugin::preInit);
        });
    }
    
     */
    
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
    
    public void stop(){
        this.shouldRun = false;
        this.plugins.forEach(DefaultPlugin::stop);
    }
    
    /*
    private Object runScriptMethod(Script script, Binding binding, String name, Object args){
        try{
            if(binding != null){
                script.setBinding(binding);
            }
            return script.invokeMethod(name, args);
        }catch(MissingMethodException e){
            return null;
        }catch(InvokerInvocationException e){
            this.wrapper.getLog().error(LOG_NAME, "Script error! The script '" + script.getClass().getClass() + "' errored during its parsing!");
            e.printStackTrace();
            return null;
        }
    }
    
     */
    
    public List<DefaultPlugin> getPlugins(){
        return plugins;
    }
    
    public MCServerWrapper getWrapper(){
        return wrapper;
    }
    
    /*
    public PluginCommunicator getPluginCommunicator(){
        return pluginCommunicator;
    }
    
     */
    
    public void onChatMessage(User user, String message){
        //this.plugins.forEach(plugin -> plugin.onChatMessage(this.pluginCommunicator, user, message));
        for(DefaultPlugin plugin : this.plugins){
            plugin.getEvent(ChatEvent.class).ifPresent(chatEvent -> chatEvent.onChatMessage(user, message));
        }
    }
    
    public void onPlayerJoin(User user){
    
    }
    
    public void onPlayerLeave(User user){
    
    }
    
    /*
    public static class Plugin {
        private final MCServerWrapper wrapper;
        private String id;
        private String name;
        private File directory;
        private Script main;
        
        private Binding binding = new Binding();
    
        public Plugin(MCServerWrapper wrapper, String id, String name, File directory, Script main){
            this.wrapper = wrapper;
            this.id = id;
            this.name = name;
            this.directory = directory;
            this.main = main;
        }
    
        public String getId(){
            return id;
        }
    
        public String getName(){
            return name;
        }
    
        public File getDirectory(){
            return directory;
        }
    
        public Script getMain(){
            return main;
        }
    
        public Binding getBinding(){
            return binding;
        }
    
        public Object runMethod(String name, Object... parameter){
            this.main.setBinding(this.binding);
            try{
                return this.main.invokeMethod(name, parameter);
            }catch(MissingMethodException e){
                return null;
            }catch(InvokerInvocationException e){
                this.wrapper.getLog().error(LOG_NAME, "Script error! The script '" + this.id + "' errored during its parsing!");
                e.printStackTrace();
                return null;
            }
        }
        
        public void preInit(){
            this.runMethod("preInit");
        }
        
        public void onChatMessage(PluginCommunicator com, User user, String message){
            this.runMethod("onChatMessage", com, user, message);
        }
    }
    
     */
}
