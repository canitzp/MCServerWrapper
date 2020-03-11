package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class BackupCommand implements IWrapperCommand{
    
    private Options options = new Options().addOption(new Option("s", "stop", false, ""));
    
    @Override
    public String[] triggerNames(){
        return new String[]{"backup"};
    }
    
    @Override
    public Options options(){
        return this.options;
    }
    
    @Override
    public String helpDescription(){
        return "Manual backup of all server files. Use --stop to stop the server before backup.";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.ADMIN;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        boolean shouldStopServer = cmd != null && cmd.hasOption("stop");
        if(shouldStopServer){
            boolean wasRunning = wrapper.RUN_MC_TASK.isRunning();
            if(wrapper.RUN_MC_TASK.stopServer()){
                wrapper.getLog().info(CommandHandler.LOG_NAME, "Server stopped. Starting backup.");
                if(wrapper.BACKUP_MANAGER.scheduleBackup("manual")){
                    wrapper.BACKUP_MANAGER.waitForBackupFree();
                    wrapper.getLog().info(CommandHandler.LOG_NAME, "Backup done.");
                    if(wasRunning){
                        wrapper.startMinecraftServer();
                    }
                } else {
                    wrapper.getLog().error(CommandHandler.LOG_NAME, "Backup hasn't finished!");
                }
            }
        } else {
            wrapper.getLog().info(CommandHandler.LOG_NAME, "Live backup started.");
            if(wrapper.BACKUP_MANAGER.scheduleBackup("manual")){
                wrapper.BACKUP_MANAGER.waitForBackupFree();
                wrapper.getLog().info(CommandHandler.LOG_NAME, "Backup done.");
            } else {
                wrapper.getLog().error(CommandHandler.LOG_NAME, "Backup hasn't finished!");
            }
        }
        return true;
    }
}
