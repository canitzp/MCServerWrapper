package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;

public class GarbageCollectCommand implements IWrapperCommand {
    
    @Override
    public String[] triggerNames(){
        return new String[]{"gc"};
    }
    
    @Override
    public String helpDescription(){
        return "Manual activation for a java garbage collect. This only applies to the wrapper not to the server!";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.OWNER;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        long freeMemoryBefore = Runtime.getRuntime().freeMemory();
        System.gc();
        long freeMemoryAfter = Runtime.getRuntime().freeMemory();
        long freedMemoryInMegaBytes = Math.round((freeMemoryAfter - freeMemoryBefore) / 1000000.0F);
        wrapper.getCommandHandler().info(user, CommandHandler.LOG_NAME, String.format("Free Memory before:'%dB and after: %dB (freed up: %dMB)", freeMemoryBefore, freeMemoryAfter, freedMemoryInMegaBytes));
        return true;
    }
}
