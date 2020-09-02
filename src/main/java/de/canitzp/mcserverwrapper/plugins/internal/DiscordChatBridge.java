package de.canitzp.mcserverwrapper.plugins.internal;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.commands.DebugCommand;
import de.canitzp.mcserverwrapper.ign.User;
import de.canitzp.mcserverwrapper.plugins.DefaultPlugin;
import de.canitzp.mcserverwrapper.plugins.event.ChatEvent;
import de.canitzp.mcserverwrapper.plugins.event.PlayerEvents;
import de.canitzp.mcserverwrapper.plugins.event.ServerEvents;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.channel.Channel;
import discord4j.core.object.entity.channel.MessageChannel;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.regex.Matcher;

public class DiscordChatBridge extends DefaultPlugin implements Runnable {
    
    private AtomicReference<GatewayDiscordClient> discord = new AtomicReference<>();
    private AtomicReference<String> lastMessage = new AtomicReference<>();
    
    public DiscordChatBridge(){
        super("discord_bridge", "Discord Chat Bridge", "1.0.0");
    }
    
    @Override
    public void setWrapper(MCServerWrapper wrapper){
        super.setWrapper(wrapper);
        Logger logger = new DiscordLoggerWrapper(wrapper);
        Loggers.useCustomLoggers(s -> logger);
    }
    
    @Override
    protected void init(){
        Executors.newSingleThreadExecutor().submit(this);
        
        List<Long> allowedChannel = this.pluginSettings.getList(Long.class, "general.channel_ids");
        
        this.registerEvent(new ServerEvents.Started(serverStartupTimeInMilliseconds -> {
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("Server started (%dms)", serverStartupTimeInMilliseconds)).block();
                }
            }
        }));
        this.registerEvent(new ServerEvents.Stopped((uptime, exitCode) -> {
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("Server stopped (Uptime: %dMinutes, Exit code: %d)", TimeUnit.MILLISECONDS.toMinutes(uptime), exitCode)).block();
                }
            }
        }));
        this.registerEvent(new ChatEvent((user, msg) -> {
            if(lastMessage.get() != null && lastMessage.get().equals(msg)){
                return true;
            }
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("<%s> %s", user.getName(), msg)).block();
                }
            }
            return true;
        }));
        this.registerEvent(new PlayerEvents.Join(user -> {
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("%s joined the game", user.getName())).block();
                }
            }
        }));
        this.registerEvent(new PlayerEvents.Leave(user -> {
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("%s left the game", user.getName())).block();
                }
            }
        }));
        this.registerEvent(new PlayerEvents.Advancement((user, advancementName) -> {
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("%s has made the advancement [%s]", user.getName(), advancementName)).block();
                }
            }
        }));
        this.registerEvent(new PlayerEvents.Goal((user, goalName) -> {
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("%s has reached the goal [%s]", user.getName(), goalName)).block();
                }
            }
        }));
        this.registerEvent(new PlayerEvents.Challenge((user, challengeName) -> {
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("%s has completed the challenge [%s]", user.getName(), challengeName)).block();
                }
            }
        }));
    }
    
    @Override
    protected void stop(){
        GatewayDiscordClient client = this.discord.get();
        if(client != null){
            this.discord.get().logout().block();
        }
    }
    
    @Override
    public void run(){
        String token = this.pluginSettings.getString("general.client_token");
        if(token == null || token.isEmpty()){
            System.out.println("No discord token found");
            return;
        }
        List<Long> allowedChannel = this.pluginSettings.getList(Long.class, "general.channel_ids");
        if(allowedChannel.isEmpty()){
            System.out.println("No allowed discord channel found");
            return;
        }
        
        DiscordClientBuilder.create(token).build().withGateway(client -> {
            this.discord.lazySet(client);
            client.getEventDispatcher().on(ReadyEvent.class).subscribe(readyEvent -> {
                System.out.println("Discord Bot logged in as " + readyEvent.getSelf().getUsername());
            });
            
            client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
                Message message = event.getMessage();
                if(allowedChannel.contains(message.getChannelId().asLong())){
                    message.getAuthor().ifPresent(author -> {
                        String s = message.getContent();
                        s = s.replace("\n", " ").replace("\r", "");
                        if(!author.isBot()){
                            if(s.startsWith("!")){
                                this.interpretCommand(s.substring(1), message.getAuthorAsMember().block(), () -> message.getChannel().block());
                            } else{
                                String msg = String.format("<%s> %s", author.getUsername(), s);
                                lastMessage.lazySet(msg);
                                this.wrapper.RUN_MC_TASK.sendToConsole("/say " + msg);
                            }
                        }
                    });
                }
            });
            return client.onDisconnect();
        }).block();
    }
    
    private void interpretCommand(String command, Member author, Supplier<MessageChannel> channelSupplier){
        switch(command){
            case "list":{
                boolean canUse = this.pluginSettings.getBoolean("commands.list.allow_all") || this.pluginSettings.getList(Long.class, "commands.list.roles").stream().anyMatch(id -> author.getRoleIds().stream().anyMatch(snowflake -> snowflake.asLong() == id));
                if(canUse){
                    StringBuilder builder = new StringBuilder();
                    List<User> activeUser = this.wrapper.getMinecraftConsoleReader().getActiveUser();
                    if(!activeUser.isEmpty()){
                        builder.append("Online player:\n");
                        activeUser.forEach(user -> builder.append(user.getName()).append(" "));
                    } else{
                        builder.append("Nobody online.");
                    }
                    channelSupplier.get().createMessage(builder.toString()).block();
                }
                break;
            }
            case "tps":
            case "tps all":{
                boolean canUse = this.pluginSettings.getBoolean("commands.tps.allow_all") || this.pluginSettings.getList(Long.class, "commands.tps.roles").stream().anyMatch(id -> author.getRoleIds().stream().anyMatch(snowflake -> snowflake.asLong() == id));
                if(canUse){
                    channelSupplier.get().createMessage("Calculating TPS... (This takes 2 seconds)").block();
                    DebugCommand.debugMinecraft(this.wrapper, "tps all".equals(command), 2000, s -> {
                        
                        StringBuilder builder = new StringBuilder();
                        for(String part : s.split("\\r?\\n")){
                            if(builder.length() > 1000){
                                channelSupplier.get().createMessage(builder.toString()).block();
                                builder = new StringBuilder();
                            } else{
                                builder.append(part).append("\n");
                            }
                        }
                        if(builder.length() > 0){
                            channelSupplier.get().createMessage(builder.toString()).block();
                        }
                    });
                }
            }
        }
    }
    
    private static class DiscordLoggerWrapper implements Logger {
        
        private static String CALLER = "Discord Bridge";
        private de.canitzp.mcserverwrapper.Logger log;
        
        public DiscordLoggerWrapper(MCServerWrapper wrapper){
            this.log = wrapper.getLog();
        }
        
        @Override
        public String getName(){
            return "Discord Wrapper Logger";
        }
        
        @Override
        public boolean isTraceEnabled(){
            return false;
        }
        
        @Override
        public void trace(String msg){
        }
        
        @Override
        public void trace(String format, Object... arguments){
        }
        
        @Override
        public void trace(String msg, Throwable t){
        }
        
        @Override
        public boolean isDebugEnabled(){
            return false;
        }
        
        @Override
        public void debug(String msg){
        }
        
        @Override
        public void debug(String format, Object... arguments){
        }
        
        @Override
        public void debug(String msg, Throwable t){
        }
        
        @Override
        public boolean isInfoEnabled(){
            return true;
        }
        
        @Override
        public void info(String msg){
            this.log.info(CALLER, msg);
        }
        
        @Override
        public void info(String format, Object... arguments){
            this.log.info(CALLER, this.format(format, arguments));
        }
        
        @Override
        public void info(String msg, Throwable t){
            this.log.info(CALLER, msg);
            t.printStackTrace();
        }
        
        @Override
        public boolean isWarnEnabled(){
            return true;
        }
        
        @Override
        public void warn(String msg){
            this.log.warn(CALLER, msg);
        }
        
        @Override
        public void warn(String format, Object... arguments){
            this.log.warn(CALLER, this.format(format, arguments));
        }
        
        @Override
        public void warn(String msg, Throwable t){
            this.log.warn(CALLER, msg);
            t.printStackTrace();
        }
        
        @Override
        public boolean isErrorEnabled(){
            return true;
        }
        
        @Override
        public void error(String msg){
            this.log.error(CALLER, msg);
        }
        
        @Override
        public void error(String format, Object... arguments){
            this.log.error(CALLER, this.format(format, arguments));
        }
        
        @Override
        public void error(String msg, Throwable t){
            this.log.error(CALLER, msg);
            t.printStackTrace();
        }
        
        @Nullable
        final String format(@Nullable String from, @Nullable Object... arguments){
            if(from != null){
                String computed = from;
                if(arguments != null && arguments.length != 0){
                    for(Object argument : arguments){
                        computed = computed.replaceFirst("\\{\\}", Matcher.quoteReplacement(String.valueOf(argument)));
                    }
                }
                return computed;
            }
            return null;
        }
    }
}
