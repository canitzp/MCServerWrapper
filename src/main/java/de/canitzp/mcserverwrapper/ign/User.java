package de.canitzp.mcserverwrapper.ign;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.canitzp.mcserverwrapper.MCServerWrapper;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class User {
    
    public static final User CONSOLE = new User() {
        @Override
        public boolean tellUser(MCServerWrapper wrapper, String message){
            wrapper.getLog().info("/tell", message);
            return true;
        }
    };
    private UUID uuid;
    private String name;
    private UserLevel level = UserLevel.NORMAL;
    private boolean bypassPlayerLimit = false;
    private Date expiresOn;

    static{
        CONSOLE.uuid = new UUID(0, 0);
        CONSOLE.name = "Server";
        CONSOLE.level = UserLevel.OWNER;
    }
    
    public UUID getUuid(){
        return uuid;
    }
    
    public String getName(){
        return name;
    }
    
    public UserLevel getLevel(){
        return level;
    }
    
    public boolean isBypassPlayerLimit(){
        return bypassPlayerLimit;
    }
    
    public Date getExpiresOn(){
        return expiresOn;
    }
    
    public boolean tellUser(MCServerWrapper wrapper, String message){
        return wrapper.RUN_MC_TASK.sendToConsole(String.format("/tell %s %s", this.name, message));
    }
    
    public static List<User> readUserFromSystem(File usercache, File ops){
        List<User> list = new ArrayList<>();
        
        // parse usercache.json
        try(FileReader fr = new FileReader(usercache)){
            JsonElement root = JsonParser.parseReader(fr);
            if(root instanceof JsonArray){
                for(JsonElement e : ((JsonArray) root)){
                    if(e instanceof JsonObject){
                        User user = new User();
                        for(String key : ((JsonObject) e).keySet()){
                            switch(key){
                                case "name":{
                                    user.name = ((JsonObject) e).get("name").getAsString();
                                    break;
                                }
                                case "uuid":{
                                    try{
                                        user.uuid = UUID.fromString(((JsonObject) e).get("uuid").getAsString());
                                    } catch(IllegalArgumentException ex){
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                                case "expiresOn":{
                                    try{
                                        user.expiresOn = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss Z").parse(((JsonObject) e).get("expiresOn").getAsString()); // 2020-03-10 15:11:04 +0100
                                    } catch(ParseException ex){
                                        ex.printStackTrace();
                                    }
                                    break;
                                }
                            }
                        }
                        list.add(user);
                    }
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        
        // parse ops.json
        try(FileReader fr = new FileReader(usercache)){
            JsonElement root = JsonParser.parseReader(fr);
            if(root instanceof JsonArray){
                for(JsonElement e : ((JsonArray) root)){
                    if(e instanceof JsonObject){
                        if(((JsonObject) e).has("uuid")){
                            try{
                                UUID uuid = UUID.fromString(((JsonObject) e).get("uuid").getAsString());
                                for(User u : list){
                                    if(uuid.equals(u.uuid)){
                                        if(((JsonObject) e).has("level")){
                                            u.level = UserLevel.values()[((JsonObject) e).get("level").getAsInt()];
                                        }
                                        if(((JsonObject) e).has("bypassesPlayerLimit")){
                                            u.bypassPlayerLimit = ((JsonObject) e).get("bypassesPlayerLimit").getAsBoolean();
                                        }
                                    }
                                }
                            } catch(IllegalArgumentException ex){
                                ex.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }
        
        return list;
    }
    
    public enum UserLevel {
        NORMAL, // normal user
        MODERATOR, // edit blocks in spawn
        GAMEMASTER, // run level 1 & 2 commands
        ADMIN, // run level 3 commands (e.g. kick, ban, op)
        OWNER, // run level 4 commands (e.g. stop)
    }
    
}
