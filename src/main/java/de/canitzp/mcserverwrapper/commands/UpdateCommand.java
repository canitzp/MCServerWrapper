package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;

public class UpdateCommand implements IWrapperCommand{
    
    @Override
    public String[] triggerNames(){
        return new String[]{"update"};
    }
    
    @Override
    public String helpDescription(){
        return "Updates the server if available";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.OWNER;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        boolean wasRunning = wrapper.RUN_MC_TASK.isRunning();
        wrapper.startMinecraftUpdate();
        wrapper.waitForUpdate();
        if(wasRunning) {
            wrapper.startMinecraftServer();
        }
        return true;
    }
}
