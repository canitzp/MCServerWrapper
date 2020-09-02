package de.canitzp.mcserverwrapper.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.RootCommandNode;
import de.canitzp.mcserverwrapper.Logger;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandHandler {
    
    public static final String LOG_NAME = "Command Manager";
    private final AtomicReference<Deque<CommandSchedule>> scheduledCommands = new AtomicReference<>(new ArrayDeque<>());
    private final List<IWrapperCommand> commands = new ArrayList<>();
    private final List<IWrapperCommand> pluginCommands = new ArrayList<>();
    private final MCServerWrapper wrapper;
    private CommandDispatcher<User> commandDispatcher;
    
    public CommandHandler(MCServerWrapper wrapper){
        this.wrapper = wrapper;
        this.commandDispatcher = new CommandDispatcher<>(new RootCommandNode<>());
        this.registerPermanent(new BackupCommand());
        this.registerPermanent(new DebugCommand());
        this.registerPermanent(new GarbageCollectCommand());
        this.registerPermanent(new HelpCommand());
        this.registerPermanent(new MemoryCommand());
        this.registerPermanent(new PluginCommand());
        this.registerPermanent(new ReloadCommand());
        this.registerPermanent(new StartServerCommand());
        this.registerPermanent(new StopServerCommand());
        this.registerPermanent(new UpdateCommand());
    }
    
    private void registerPermanent(IWrapperCommand command){
        this.commands.add(command);
    }
    
    public void clearCommands(){
        this.pluginCommands.clear();
    }
    
    public CommandDispatcher<User> getCommandDispatcher(){
        return commandDispatcher;
    }
    
    public Stream<IWrapperCommand> mergedCommands(){
        List<IWrapperCommand> commands = new ArrayList<>();
        commands.addAll(this.commands);
        commands.addAll(this.pluginCommands);
        return commands.stream();
    }
    
    public Optional<IWrapperCommand> findMatchingCommand(String name){
        return this.mergedCommands().filter(command -> Arrays.asList(command.triggerNames()).contains(name)).findFirst();
    }
    
    public List<IWrapperCommand> getAllCommands(){
        return Collections.unmodifiableList(this.mergedCommands().collect(Collectors.toList()));
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
            ParseResults<User> parse = this.commandDispatcher.parse(cs.command, cs.user);
            try{
                int result = this.commandDispatcher.execute(parse);
            } catch(CommandSyntaxException ignored){
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
                        } catch(ParseException e){
                            e.printStackTrace();
                        }
                        CommandLine finalCmd = cmd;
                        this.wrapper.submitRunnable(() -> {
                            try{
                                c.get().execute(this.wrapper, User.CONSOLE, finalCmd);
                            } catch(Exception e){
                                e.printStackTrace();
                            }
                        });
                    } else{
                        // todo can't run this command
                    }
                } else{
                    this.wrapper.getLog().info(LOG_NAME, "No command found! '" + cs.command + "'");
                }
            }
        }
    }
    
    public void info(User user, String caller, String message, Logger.ANSICOLOR... formatting){
        if(user != null && !Objects.equals(user, User.CONSOLE)){
            this.wrapper.getUserManagement().tellUser(user, message);
        } else{
            this.wrapper.getLog().info(caller, message, formatting);
        }
    }
    
    public static LiteralArgumentBuilder<User> literal(String name){
        return LiteralArgumentBuilder.literal(name);
    }
    
    public static <T> RequiredArgumentBuilder<User, T> argument(String name, ArgumentType<T> type){
        return RequiredArgumentBuilder.argument(name, type);
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
