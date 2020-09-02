package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.Logger;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;

import java.util.Arrays;


public class PluginCommand implements IWrapperCommand {
    
    @Override
    public String[] triggerNames(){
        return new String[]{"plugin"};
    }
    
    @Override
    public String helpDescription(){
        return "Tool to manage plugins";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.OWNER;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        boolean displayHelp = false;
        String[] args = cmd != null ? cmd.getArgs() : null;
        if(args != null && args.length > 0){
            String type = args[0];
            switch(type){
                case "list":{
                    wrapper.getLog().info(null, "List of running installed plugins.", Logger.ANSICOLOR.GREEN);
                    wrapper.getPluginManager().getPlugins().forEach(plugin -> {
                        wrapper.getLog().info(null, String.format("\t%s(%s)", plugin.getName(), plugin.getId()), Logger.ANSICOLOR.GREEN);
                    });
                    break;
                }
                default:{
                    wrapper.getLog().info(null, "The command you specified is unknown. Use one of the following:", Logger.ANSICOLOR.GREEN);
                    displayHelp = true;
                    break;
                }
            }
        } else{
            wrapper.getLog().info(null, "You need to specify at least on of the following sub commands:", Logger.ANSICOLOR.GREEN);
            displayHelp = true;
        }
        if(displayHelp){
            wrapper.getLog().list(null, Arrays.asList("\t'list': List all installed plugins"), Logger.ANSICOLOR.GREEN);
        }
        return true;
    }
}
