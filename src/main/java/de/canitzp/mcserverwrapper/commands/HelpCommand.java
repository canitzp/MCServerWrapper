package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.Logger;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelpCommand implements IWrapperCommand{
    
    @Override
    public String[] triggerNames(){
        return new String[]{"help"};
    }
    
    @Override
    public String helpDescription(){
        return "Show this help";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.MODERATOR;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        List<String> helpList = new ArrayList<>();
        helpList.add("Minecraft Server Wrapper help list:");
        
        List<IWrapperCommand> allCommands = wrapper.getCommandHandler().getAllCommands();
        for(IWrapperCommand command : allCommands){
            StringBuilder triggerString = new StringBuilder();
            Arrays.stream(command.triggerNames()).forEach(s -> triggerString.append("'").append(s).append("'"));
            
            helpList.add(String.format("\t%s: %s", triggerString.toString(), command.helpDescription()));
        }
        
        for(String line : helpList){
            wrapper.getCommandHandler().info(user, null, line, Logger.ANSICOLOR.GREEN);
        }
        
        //wrapper.getLog().list(null, helpList, Logger.ANSICOLOR.GREEN);
        return true;
    }
}
