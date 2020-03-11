package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public interface IWrapperCommand{
    
    Options EMPTY_OPTIONS = new Options();
    
    String[] triggerNames();
    
    default Options options(){
        return EMPTY_OPTIONS;
    }
    
    String helpDescription();
    
    User.UserLevel minUserLevel();
    
    // return if the execution was successful
    boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd);
    
}
