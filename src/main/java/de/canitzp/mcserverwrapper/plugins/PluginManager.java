package de.canitzp.mcserverwrapper.plugins;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MissingMethodException;
import groovy.lang.Script;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PluginManager implements Runnable{
    
    private static final String LOG_NAME = "Plugin Manager";
    private static final int TICK_TIME = 1;//50;
    
    private final MCServerWrapper wrapper;
    
    private boolean shouldRun = true;
    private final List<Plugin> plugins = new ArrayList<>();
    
    public PluginManager(MCServerWrapper wrapper){
        this.wrapper = wrapper;
    }
    
    // load/reload all plugins
    public void reload(){
        this.plugins.clear();
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
    }
    
    private void loadPlugin(File pluginDirecotry){
        File pluginMain = new File(pluginDirecotry, "main.groovy");
        if(!pluginMain.exists()){
            this.wrapper.getLog().warn(LOG_NAME, "Plugin directory without 'main.groovy' found! '" + pluginDirecotry.getAbsolutePath() + "'");
            return;
        }
    
        GroovyShell shell = new GroovyShell();
        try{
            Script main = shell.parse(pluginMain);
            Object id = PluginManager.runScriptMethod(main, null, "id", null);
            Object name = PluginManager.runScriptMethod(main, null, "name", null);
            
            if(id instanceof String){
                if(name instanceof String){
                    this.wrapper.getLog().info(LOG_NAME, "Loaded plugin: '" + name + "'");
                    this.plugins.add(new Plugin(((String) id), ((String) name), pluginDirecotry, main));
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
    
    @Override
    public void run(){
        long nextTick = System.currentTimeMillis();
        while(this.shouldRun){
            if(nextTick <= System.currentTimeMillis()){
                nextTick = System.currentTimeMillis() + (1000 / TICK_TIME);
            }
        }
    }
    
    public void stop(){
        this.shouldRun = false;
    }
    
    private static Object runScriptMethod(Script script, Binding binding, String name, Object args){
        try{
            if(binding != null){
                script.setBinding(binding);
            }
            return script.invokeMethod(name, args);
        }catch(MissingMethodException e){
            return null;
        }
    }
    
    public void onChatMessage(User user, String message){
        this.plugins.forEach(plugin -> plugin.onChatMessage(user, message));
    }
    
    class Plugin {
        private String id;
        private String name;
        private File directory;
        private Script main;
        
        private Binding binding = new Binding();
    
        public Plugin(String id, String name, File directory, Script main){
            this.id = id;
            this.name = name;
            this.directory = directory;
            this.main = main;
        }
        
        public void preInit(){
            PluginManager.runScriptMethod(this.main, this.binding, "preInit", null);
        }
        
        public void onChatMessage(User user, String message){
            PluginManager.runScriptMethod(this.main, this.binding, "onChatMessage", new Object[]{user, message});
        }
    }
}
