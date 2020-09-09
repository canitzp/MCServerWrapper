package de.canitzp.mcserverwrapper.ign;

import de.canitzp.mcserverwrapper.MCServerWrapper;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MinecraftConsoleReader {
    
    private final MCServerWrapper wrapper;
    private AtomicReference<Deque<String>> linesToRead = new AtomicReference<>(new ArrayDeque<>());
    private File usercache, ops;
    private List<User> activeUser = new ArrayList<>();
    
    public MinecraftConsoleReader(MCServerWrapper wrapper){
        this.wrapper = wrapper;
    }
    
    public void onConfigurationChange(){
        File root = wrapper.getSettings().getFile("general.jar_path").getParentFile();
        this.usercache = new File(root, "usercache.json");
        this.ops = new File(root, "ops.json");
    }
    
    public void tick(){
        while(!this.linesToRead.get().isEmpty()){
            String line = this.linesToRead.get().removeFirst();
            String[] splitByArea = line.split(":", 4);
            
            if(splitByArea.length == 4){
                String pureLine = splitByArea[3];
                if(!pureLine.isEmpty()){
                    pureLine = pureLine.substring(1);
                    this.interpretLine(pureLine);
                }
            }
        }
    }
    
    private void interpretLine(String pureLine){
        AtomicBoolean redirectToConsole = new AtomicBoolean(true);
        
        if(pureLine.matches("(\\[.*\\]|<.*>)\\s.*")){
            String[] split = pureLine.split("\\s", 2);
            if(split.length == 2){
                String caller = split[0].substring(1, split[0].length() - 1);
                String message = split[1];
                User user = this.activeUser.stream().filter(u -> caller.equals(u.getName())).findFirst().orElse(null);
                if(user == null && caller.equals("Server")){
                    user = User.CONSOLE;
                } else {
                    user = User.NULL;
                }
                if(message.startsWith(this.wrapper.getSettings().getString("general.wrapper_command_prefix"))){
                    this.wrapper.getCommandHandler().scheduleCommand(user, message);
                }
                redirectToConsole.lazySet(this.wrapper.getPluginManager().onChatMessage(user, message));
            }
        } else if(pureLine.matches(".*\\sjoined\\sthe\\sgame")){
            String playerName = pureLine.substring(0, pureLine.indexOf(" "));
            List<User> allUser = User.readUserFromSystem(this.usercache, this.ops);
            Optional<User> user = allUser.stream().filter(u -> playerName.equals(u.getName())).findFirst();
            user.ifPresent(user1 -> {
                this.activeUser.add(user1);
                this.wrapper.getPluginManager().onPlayerJoin(user1);
            });
        } else if(pureLine.matches(".*\\sleft\\sthe\\sgame")){
            String playerName = pureLine.substring(0, pureLine.indexOf(" "));
            Optional<User> user = this.activeUser.stream().filter(u -> playerName.equals(u.getName())).findFirst();
            user.ifPresent(user1 -> {
                this.activeUser.remove(user1);
                this.wrapper.getPluginManager().onPlayerLeave(user1);
            });
        } else if(pureLine.matches(".*\\shas\\smade\\sthe\\sadvancement\\s\\[.*]")){
            String playerName = pureLine.substring(0, pureLine.indexOf(" "));
            String advancementName = pureLine.substring(pureLine.lastIndexOf("[") + 1, pureLine.indexOf("]"));
            Optional<User> user = this.activeUser.stream().filter(u -> playerName.equals(u.getName())).findFirst();
            user.ifPresent(user1 -> {
                this.wrapper.getPluginManager().onPlayerAdvancement(user1, advancementName);
            });
        } else if(pureLine.matches(".*\\shas\\reached\\sthe\\sgoal\\s\\[.*]")){
            String playerName = pureLine.substring(0, pureLine.indexOf(" "));
            String goalName = pureLine.substring(pureLine.lastIndexOf("[") + 1, pureLine.indexOf("]"));
            Optional<User> user = this.activeUser.stream().filter(u -> playerName.equals(u.getName())).findFirst();
            user.ifPresent(user1 -> {
                this.wrapper.getPluginManager().onPlayerReachedGoal(user1, goalName);
            });
        } else if(pureLine.matches(".*\\shas\\scompleted\\sthe\\schallenge\\s\\[.*]")){
            String playerName = pureLine.substring(0, pureLine.indexOf(" "));
            String challengeName = pureLine.substring(pureLine.lastIndexOf("[") + 1, pureLine.indexOf("]"));
            Optional<User> user = this.activeUser.stream().filter(u -> playerName.equals(u.getName())).findFirst();
            user.ifPresent(user1 -> {
                this.wrapper.getPluginManager().onPlayerCompletedChallenge(user1, challengeName);
            });
        } else if(pureLine.matches("Done\\s\\(.*\\)!.*")){
            String unparsedTime = pureLine.substring(pureLine.indexOf("(") + 1, pureLine.indexOf(")") - 1);
            try{
                int startupTimeInMilliseconds = Math.round(Float.parseFloat(unparsedTime) * 1000);
                this.wrapper.getPluginManager().onServerStared(startupTimeInMilliseconds);
            } catch(NumberFormatException ignored){
            }
        } else if(pureLine.matches("Stopping\\sthe\\sserver")){
            this.wrapper.getPluginManager().onServerStop();
        } else if(pureLine.matches("Saving\\sthe\\sgame\\s.*")){
            this.wrapper.getPluginManager().onServerSaving();
        } else if(pureLine.matches("Saved\\sthe\\sgame")){
            this.wrapper.getPluginManager().onServerSaved();
        }
        
        redirectToConsole.lazySet(this.wrapper.getPluginManager().onServerMessage(pureLine));
        
        if(redirectToConsole.get()){
            this.wrapper.getLog().info("Minecraft", pureLine);
        }
    }
    
    public void scheduleLine(String line){
        this.linesToRead.get().addLast(line);
    }
    
    public List<User> getActiveUser(){
        return Collections.unmodifiableList(this.activeUser);
    }
}
