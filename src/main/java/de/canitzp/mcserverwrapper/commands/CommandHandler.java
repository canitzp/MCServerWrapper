package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.Logger;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class CommandHandler{
    
    public static final String LOG_NAME = "Command Manager";
    
    private final AtomicReference<Deque<CommandSchedule>> scheduledCommands = new AtomicReference<>(new ArrayDeque<>());
    
    private final List<IWrapperCommand> commands = new ArrayList<>();
    private final MCServerWrapper wrapper;
    
    public CommandHandler(MCServerWrapper wrapper){
        this.wrapper = wrapper;
        this.registerCommand(new BackupCommand());
        this.registerCommand(new HelpCommand());
        this.registerCommand(new ReloadCommand());
        this.registerCommand(new StartServerCommand());
        this.registerCommand(new StopServerCommand());
        this.registerCommand(new UpdateCommand());
    }
    
    public void registerCommand(IWrapperCommand command){
        this.commands.add(command);
    }
    
    public Optional<IWrapperCommand> findMatchingCommand(String name){
        return this.commands.stream().filter(command -> Arrays.asList(command.triggerNames()).contains(name)).findFirst();
    }
    
    public List<IWrapperCommand> getAllCommands(){
        return Collections.unmodifiableList(this.commands);
    }
    
    public void scheduleCommand(User user, String command){
        String wrapperCommandPrefix = this.wrapper.getSettings().getString("general.wrapper_command_prefix");
        if(command.startsWith(wrapperCommandPrefix)){
            command = command.substring(wrapperCommandPrefix.length());
        }
        this.scheduledCommands.get().addLast(new CommandSchedule(user, command));
    }
    
    public void tick(){
        if(!this.scheduledCommands.get().isEmpty()){
            CommandSchedule cs = this.scheduledCommands.get().removeFirst();
            String[] commandSplit = cs.command.split(" ", 2);
            String commandName = commandSplit[0];
            Optional<IWrapperCommand> c = this.findMatchingCommand(commandName);
            if(c.isPresent()){
                if(c.get().minUserLevel().ordinal() <= cs.user.getLevel().ordinal()){
                    CommandLineParser cmp = new DefaultParser();
                    CommandLine cmd = null;
                    try{
                        if(commandSplit.length == 2){
                            cmd = cmp.parse(c.get().options(), commandSplit[1].split(" "));
                        }
                    }catch(ParseException e){
                        e.printStackTrace();
                    }
                    CommandLine finalCmd = cmd;
                    this.wrapper.submitRunnable(() -> c.get().execute(this.wrapper, User.CONSOLE, finalCmd));
                } else {
                    // todo can't run this command
                }
            } else {
                this.wrapper.getLog().info(LOG_NAME, "No command found! '" + cs.command + "'");
            }
        }
    }
    
    public void info(User user, String caller, String message, Logger.ANSICOLOR... formatting){
        if(user != null && !Objects.equals(user, User.CONSOLE)){
            this.wrapper.getUserManagement().tellUser(user, message);
        } else {
            this.wrapper.getLog().info(caller, message, formatting);
        }
    }
    
    static class CommandSchedule {
        private User user;
        private String command;
    
        public CommandSchedule(User user, String command){
            this.user = user;
            this.command = command;
        }
    }
    
}
