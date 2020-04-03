package de.canitzp.mcserverwrapper.plugins.internal;

import de.canitzp.mcserverwrapper.plugins.DefaultPlugin;
import de.canitzp.mcserverwrapper.plugins.event.ChatEvent;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class DiscordChatBridge extends DefaultPlugin implements Runnable{
    
    private AtomicReference<DiscordClient> discord = new AtomicReference<>();
    private AtomicReference<String> lastMessage = new AtomicReference<>();
    
    public DiscordChatBridge() {
        super("discord_bridge", "Discord Chat Bridge", "1.0.0");
    }
    
    @Override
    protected void init() {
        Executors.newSingleThreadExecutor().submit(this);
    
        List<Long> allowedChannel = this.pluginSettings.getList(Long.class, "general.channel_ids");
        this.registerEvent(new ChatEvent((user, msg) -> {
            if(lastMessage.get() != null && lastMessage.get().equals(msg)){
                return;
            }
            for(long channelId : allowedChannel){
                Channel channel = this.discord.get().getChannelById(Snowflake.of(channelId)).block();
                if(channel instanceof MessageChannel){
                    ((MessageChannel) channel).createMessage(String.format("%s: %s", user.getName(), msg)).block();
                }
            }
        }));
    }
    
    @Override
    protected void stop() {
        this.discord.get().logout().block();
    }
    
    @Override
    public void run() {
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
        
        DiscordClient client = new DiscordClientBuilder(token).build();
        this.discord.lazySet(client);
    
        client.getEventDispatcher().on(ReadyEvent.class).subscribe(readyEvent -> {
            System.out.println("Discord Bot logged in as " + readyEvent.getSelf().getUsername());
        });
        
        client.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            Message message = event.getMessage();
            if(allowedChannel.contains(message.getChannelId().asLong())){
                message.getAuthor().ifPresent(author -> {
                    message.getContent().ifPresent(s -> {
                        if(!author.isBot()){
                            String msg = String.format("%s: %s", author.getUsername(), s);
                            lastMessage.lazySet(msg);
                            this.wrapper.RUN_MC_TASK.sendToConsole("/say " + msg);
                        }
                    });
                });
            }
        });
    
        client.login().block();
    }
}
