package de.canitzp.mcserverwrapper.ign;

import de.canitzp.mcserverwrapper.MCServerWrapper;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class MinecraftConsoleReader{
    
    private AtomicReference<Deque<String>> linesToRead = new AtomicReference<>(new ArrayDeque<>());
    
    private File usercache, ops;
    
    private final MCServerWrapper wrapper;
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
        this.wrapper.getLog().info("Minecraft", pureLine);

        if(pureLine.matches("\\[.*]\\s.*")){
            String[] split = pureLine.split("\\s", 2);
            if(split.length == 2){
                String caller = split[0].substring(1, split[0].length() - 1);
                String message = split[1];
                if(message.startsWith(this.wrapper.getSettings().getString("general.wrapper_command_prefix"))){
                    Optional<User> user = this.activeUser.stream().filter(u -> caller.equals(u.getName())).findFirst();
                    user.ifPresent(user1 -> this.wrapper.getCommandHandler().scheduleCommand(user1, message));
                }
            }
        } else if(pureLine.matches(".*\\sjoined\\sthe\\sgame")){
            String playerName = pureLine.substring(0, pureLine.indexOf(" "));
            List<User> allUser = User.readUserFromSystem(this.usercache, this.ops);
            Optional<User> user = allUser.stream().filter(u -> playerName.equals(u.getName())).findFirst();
            user.ifPresent(user1 -> this.activeUser.add(user1));
        } else if(pureLine.matches(".*\\sleft\\sthe\\sgame")){
            String playerName = pureLine.substring(0, pureLine.indexOf(" "));
            Optional<User> user = this.activeUser.stream().filter(u -> playerName.equals(u.getName())).findFirst();
            user.ifPresent(user1 -> this.activeUser.remove(user1));
        }
    }
    
    public void scheduleLine(String line){
        this.linesToRead.get().addLast(line);
    }
}
