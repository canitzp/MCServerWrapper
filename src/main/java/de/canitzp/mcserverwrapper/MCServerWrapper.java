package de.canitzp.mcserverwrapper;

import de.canitzp.mcserverwrapper.commands.CommandHandler;
import de.canitzp.mcserverwrapper.ign.MinecraftConsoleReader;
import de.canitzp.mcserverwrapper.ign.UserManagement;
import de.canitzp.mcserverwrapper.ign.save.RegionLoader;
import de.canitzp.mcserverwrapper.plugins.PluginManager;
import de.canitzp.mcserverwrapper.tasks.ConsoleInterpreter;
import de.canitzp.mcserverwrapper.tasks.BackupManager;
import de.canitzp.mcserverwrapper.tasks.RunMinecraftTask;
import de.canitzp.mcserverwrapper.tasks.RunMinecraftUpdate;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class MCServerWrapper{
    
    private static final String LOG_NAME = "Main";
    
    //private final List<String> LOG_LINES = new ArrayList<>();
    private final Path configFile = Paths.get("./wrapper-settings.conf");
    private final AtomicBoolean LOCK_APPLICATION = new AtomicBoolean(false);
    
    public final ConsoleInterpreter CONSOLE_INTERPRETER = new ConsoleInterpreter(this);
    public final BackupManager BACKUP_MANAGER = new BackupManager(this);
    
    public final ThreadPoolExecutor THREADS = (ThreadPoolExecutor) Executors.newCachedThreadPool();
    public final RunMinecraftTask RUN_MC_TASK = new RunMinecraftTask(this);
    public final RunMinecraftUpdate RUN_MC_UPDATE = new RunMinecraftUpdate(this);
    
    private final Logger LOGGER = new Logger();
    private final CommandHandler commandHandler = new CommandHandler(this);
    private final MinecraftConsoleReader minecraftConsoleReader = new MinecraftConsoleReader(this);
    private final UserManagement userManagement = new UserManagement(this);
    private final PluginManager pluginManager = new PluginManager(this);
    
    private Settings settings;
    
    public MCServerWrapper(boolean startInLockedMode){
        
        //new RegionLoader(new File("./server/world")).loadRegionFiles("");
        
        //System.exit(0);
        /*
        * Important configuration reading and writhing!
        */
        this.loadConfiguration(true);
    
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (this.RUN_MC_TASK.isRunning()) {
                this.RUN_MC_TASK.sendToConsole("stop");
            }
            this.CONSOLE_INTERPRETER.stop();
            this.pluginManager.stop();
            this.getLog().info(LOG_NAME, "Shutting down.");
        }));
        
        // Start the console interpreter. Bridge from System.in to server console or wrapper command
        Executors.newSingleThreadExecutor().submit(this.CONSOLE_INTERPRETER);
        
        // Start the plugin manager and all plugins
        Executors.newSingleThreadExecutor().submit(this.pluginManager);
        
        // Start the backup manager
        Executors.newSingleThreadExecutor().submit(this.BACKUP_MANAGER);
        
        if(!startInLockedMode){
            this.startMinecraftUpdate();
            this.waitForUpdate();
    
            this.startMinecraftServer();
        } else {
            this.LOCK_APPLICATION.lazySet(true);
            this.getLog().info(LOG_NAME, "Application started in LOCKED mode.");
        }
    
        while(this.LOCK_APPLICATION.get() || THREADS.getActiveCount() != 0) {
            //System.gc();
            sleep(250L);
            try{
                this.tick();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        
        System.exit(0);
    }
    
    public void loadConfiguration(boolean overwrite){
        boolean killAfterConfigCreation = !this.configFile.toFile().exists();
        this.settings = new Settings(this.configFile.toFile(), "wrapper-settings.default.conf");
        this.getMinecraftConsoleReader().onConfigurationChange();
        this.pluginManager.reload(overwrite);
        if(overwrite){
            this.settings.overwriteCurrentConfig(); // to overwrite the old or non existent configuration file.
        }
        if(killAfterConfigCreation){
            this.getLog().info(LOG_NAME, "There was no conf file found, so it is created. Please check it before running this application again! The name is: '" + this.configFile.getFileName() + "'");
            System.exit(0);
        }
        this.getLog().info(LOG_NAME, "Resource loading successful.");
    }
    
    // main thread loop until all thread have finished and the application can be exited
    // called 4 times per second (every 250ms)
    private void tick(){
        // ticks should be disabled, when the application is in wrapper only mode
        if(!this.LOCK_APPLICATION.get()){
            // tick the updater. this check if it needs to auto update after a time interval
            this.RUN_MC_UPDATE.tick();
    
            // tick the backup manager to do automated backups if needed
            this.BACKUP_MANAGER.tick();
            
        }
        
        if(this.RUN_MC_TASK.isRunning()){
            this.minecraftConsoleReader.tick();
        }
        
        this.commandHandler.tick();
    }
    
    public void submitRunnable(Runnable runnable){
        this.THREADS.submit(runnable);
    }
    
    public void startMinecraftServer(){
        if(this.LOCK_APPLICATION.get()){
            this.getLog().error(LOG_NAME, "Server won't be started in LOCKED mode!");
            return;
        }
        if(!this.RUN_MC_TASK.isRunning()){
            this.THREADS.submit(this.RUN_MC_TASK);
            this.sleep(1000); // wait for the process to start
        }
    }
    
    public void startMinecraftUpdate(){
        if(!this.RUN_MC_UPDATE.isRunning()){
            this.THREADS.submit(this.RUN_MC_UPDATE);
            this.sleep(1000); // wait for the process to start
        }
    }
    
    public void waitForUpdate(){
        while(this.RUN_MC_UPDATE.isRunning()){
            sleep(100);
        }
    }
    
    public void sleep(long millis){
        try{
            Thread.sleep(millis);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
    }
    
    private void log(String caller, String message){
        String log;
        if(caller != null){
            log = String.format("[%s]: %s", caller, message);
        } else {
            log = String.format("%s", message);
        }
        //LOG_LINES.add(log);
        System.out.println(log);
    }
    
    @Deprecated
    public void game(String msg){
        String caller = "Minecraft console";
        this.log(caller, msg);
    }
    
    @Deprecated
    public void raw(String msg){
        this.log(null, msg);
    }
    
    public Settings getSettings(){
        return settings;
    }
    
    public Logger getLog(){
        return LOGGER;
    }
    
    public PluginManager getPluginManager(){
        return pluginManager;
    }
    
    public void setLock(boolean locked){
        this.LOCK_APPLICATION.lazySet(locked);
    }
    
    public boolean isLockMode(){
        return this.LOCK_APPLICATION.get();
    }
    
    public CommandHandler getCommandHandler(){
        return commandHandler;
    }
    
    public MinecraftConsoleReader getMinecraftConsoleReader(){
        return minecraftConsoleReader;
    }
    
    public UserManagement getUserManagement(){
        return userManagement;
    }
    
}
