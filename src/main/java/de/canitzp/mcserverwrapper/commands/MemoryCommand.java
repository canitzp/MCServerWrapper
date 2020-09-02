package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;

public class MemoryCommand implements IWrapperCommand {
    
    @Override
    public String[] triggerNames() {
        return new String[]{"mem", "memory"};
    }
    
    @Override
    public String helpDescription() {
        return "Shows the current memory usage of the wrapper. Does not show any data of the server!";
    }
    
    @Override
    public User.UserLevel minUserLevel() {
        return User.UserLevel.OWNER;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd) {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long maxMemoryInMegabytes = Math.round(maxMemory / 1000000F);
        long usedMemory = maxMemory - Runtime.getRuntime().freeMemory();
        long usedMemoryInMegabytes = Math.round(usedMemory / 1000000F);
        long usedPercentage = Math.round(usedMemory / (maxMemory * 1.0F) * 100);
        wrapper.getCommandHandler().info(user, CommandHandler.LOG_NAME, String.format("Memory usage: %dMB/%dMB (%d%%)", usedMemoryInMegabytes, maxMemoryInMegabytes, usedPercentage));
        return true;
    }
}
