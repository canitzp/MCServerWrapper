package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;

public class StartServerCommand implements IWrapperCommand {
    
    @Override
    public String[] triggerNames(){
        return new String[]{"start"};
    }
    
    @Override
    public String helpDescription(){
        return "Start the server is it isn't.";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.OWNER;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        wrapper.setLock(false);
        wrapper.startMinecraftServer();
        return true;
    }
}
