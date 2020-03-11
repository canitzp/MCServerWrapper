package de.canitzp.mcserverwrapper;

import de.canitzp.mcserverwrapper.commands.CommandHandler;
import de.canitzp.mcserverwrapper.ign.MinecraftConsoleReader;
import de.canitzp.mcserverwrapper.ign.UserManagement;
import de.canitzp.mcserverwrapper.tasks.ConsoleInterpreter;
import de.canitzp.mcserverwrapper.tasks.BackupManager;
import de.canitzp.mcserverwrapper.tasks.RunMinecraftTask;
import de.canitzp.mcserverwrapper.tasks.RunMinecraftUpdate;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

public class MCServerWrapper{
    
    private static final String LOG_NAME = "Main";
    
    private final Deque<String> commandsToProcess = new ArrayDeque<>();
    private final List<String> LOG_LINES = new ArrayList<>();
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
    
    private Settings settings;
    
    public MCServerWrapper(boolean startInLockedMode){
        /*
        * Important configuration reading and writhing!
        */
        this.loadConfiguration(true);
    
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (this.RUN_MC_TASK.isRunning()) {
                this.RUN_MC_TASK.sendToConsole("stop");
            }
            this.CONSOLE_INTERPRETER.stop();
            this.getLog().info(LOG_NAME, "Shutting down.");
        }));
        
        // Start the console interpreter. Bridge from System.in to server console or wrapper command
        Executors.newSingleThreadExecutor().submit(this.CONSOLE_INTERPRETER);
        
        // Start the backup manager
        Executors.newSingleThreadExecutor().submit(this.BACKUP_MANAGER);
        
        if(!startInLockedMode){
            this.startMinecraftUpdate();
            this.waitForUpdate();
    
            this.startMinecraftServer();
        } else {
            this.LOCK_APPLICATION.set(true);
            this.getLog().info(LOG_NAME, "Application started in LOCKED mode.");
        }
    
        while(this.LOCK_APPLICATION.get() || THREADS.getActiveCount() != 0) {
            sleep(250L);
            this.tick();
        }
        
        System.exit(0);
    }
    
    public void loadConfiguration(boolean overwrite){
        boolean killAfterConfigCreation = !this.configFile.toFile().exists();
        this.settings = new Settings(this.configFile.toFile());
        this.getMinecraftConsoleReader().onConfigurationChange();
        if(overwrite){
            this.settings.overwriteCurrentConfig(); // to overwrite the old or non existent configuration file.
        }
        if(killAfterConfigCreation){
            this.getLog().info(LOG_NAME, "There was no conf file found, so it is created. Please check it before running this application again! The name is: '" + this.configFile.getFileName() + "'");
            System.exit(0);
        }
        this.getLog().info(LOG_NAME, "Settings loaded successful.");
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
        
        // process command inputs
        if(!this.commandsToProcess.isEmpty()){
            this.processCommand(this.commandsToProcess.getFirst());
            this.commandsToProcess.removeFirst();
        }
    }
    
    private void processCommand(String command){
        String wrapperCommandPrefix = this.getSettings().getString("general.wrapper_command_prefix");
        if(command.startsWith(wrapperCommandPrefix)){
            command = command.substring(wrapperCommandPrefix.length());
        }
        
        switch(command){
            case "start": {
                this.LOCK_APPLICATION.set(false);
                this.startMinecraftServer();
                break;
            }
            case "stop": {
                if(this.LOCK_APPLICATION.get()){
                    this.LOCK_APPLICATION.set(false);
                } else {
                    this.LOCK_APPLICATION.set(true);
                    this.RUN_MC_TASK.stopServer();
                    this.getLog().info(LOG_NAME, "Server stopped. LOCKED MODE. The wrapper command prefix is optional in this mode. Type 'stop' to close this wrapper or use 'start' to start the server.", Logger.ANSICOLOR.GREEN);
                }
                break;
            }
            case "backup": {
                this.getLog().info(LOG_NAME, "Live backup started.");
                if(this.BACKUP_MANAGER.scheduleBackup("manual")){
                    this.BACKUP_MANAGER.waitForBackupFree();
                    this.getLog().info(LOG_NAME, "Backup done.");
                } else {
                    this.getLog().error(LOG_NAME, "Backup hasn't finished!");
                }
                break;
            }
            case "backup --stop": {
                if(this.RUN_MC_TASK.stopServer()){
                    this.getLog().info(LOG_NAME, "Server stopped. Starting backup.");
                    if(this.BACKUP_MANAGER.scheduleBackup("manual")){
                        this.BACKUP_MANAGER.waitForBackupFree();
                        this.getLog().info(LOG_NAME, "Backup done.");
                        this.startMinecraftServer();
                    } else {
                        this.getLog().error(LOG_NAME, "Backup hasn't finished!");
                    }
                }
                break;
            }
            case "reload": {
                this.loadConfiguration(false);
                break;
            }
            case "update": {
                boolean wasRunning = this.RUN_MC_TASK.isRunning();
                this.startMinecraftUpdate();
                this.waitForUpdate();
                if(wasRunning) {
                    this.startMinecraftServer();
                }
                break;
            }
            case "restart": {
                this.getLog().info(LOG_NAME, "Minecraft server restart scheduled.");
                if(this.RUN_MC_TASK.sendToConsole("stop")){
                    while(this.RUN_MC_TASK.isRunning()){
                        sleep(100);
                    }
                    this.startMinecraftServer();
                } else {
                    this.getLog().error(LOG_NAME, "The server couldn't be restarted. Is it even running? Use the 'start' command if it isn't!");
                }
                break;
            }
            case "help": default: {
                List<String> helpList = new ArrayList<>();
                helpList.add("Prefix for wrapper command: '" + wrapperCommandPrefix + "' (Not needed when LOCKED mode is active. Currently LOCKED: '" + this.LOCK_APPLICATION.get() + "')");
                helpList.add("Minecraft Server Wrapper Help:");
                helpList.add("\t'backup [--stop]': Manual backup of all server files. Use --stop to stop the server before backup");
                helpList.add("\t'help': Display this help");
                helpList.add("\t'reload': Reloads the configuration file for the wrapper");
                helpList.add("\t'restart': Restarts the server gracefully");
                helpList.add("\t'start': Start the server is it isn't.");
                helpList.add("\t'stop': Stops the server, BUT keeps the wrapper running, instead of stopping it too. Run again to stop the wrapper too, or 'start' to start the server.");
                helpList.add("\t'update': Updates the server if available");
                this.getLog().list(null, helpList, Logger.ANSICOLOR.GREEN);
                break;
            }
        }
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
        LOG_LINES.add(log);
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
    
    public void setLock(boolean locked){
        this.LOCK_APPLICATION.set(locked);
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
