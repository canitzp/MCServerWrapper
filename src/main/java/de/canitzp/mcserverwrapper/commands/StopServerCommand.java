package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.Logger;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;

public class StopServerCommand implements IWrapperCommand {
    
    @Override
    public String[] triggerNames(){
        return new String[]{"stop"};
    }
    
    @Override
    public String helpDescription(){
        return "Stops the server, BUT keeps the wrapper running, instead of stopping it too. Run again to stop the wrapper too, or 'start' to start the server.";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.OWNER;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        if(wrapper.isLockMode()){
            wrapper.setLock(false);
        } else{
            wrapper.setLock(true);
            wrapper.RUN_MC_TASK.stopServer();
            wrapper.getLog().info(CommandHandler.LOG_NAME, "Server stopped. LOCKED MODE. The wrapper command prefix is optional in this mode. Type 'stop' to close this wrapper or use 'start' to start the server.", Logger.ANSICOLOR.GREEN);
        }
        return true;
    }
}
