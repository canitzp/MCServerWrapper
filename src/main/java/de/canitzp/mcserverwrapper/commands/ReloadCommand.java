package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;

public class ReloadCommand implements IWrapperCommand{
    
    @Override
    public String[] triggerNames(){
        return new String[]{"reload"};
    }
    
    @Override
    public String helpDescription(){
        return "Reloads the configuration file for the wrapper";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.OWNER;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        wrapper.loadConfiguration(false);
        return true;
    }
}
