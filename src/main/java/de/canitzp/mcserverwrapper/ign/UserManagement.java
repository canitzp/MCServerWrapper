package de.canitzp.mcserverwrapper.ign;

import de.canitzp.mcserverwrapper.MCServerWrapper;

import java.util.UUID;

public class UserManagement {
    
    private final MCServerWrapper wrapper;
    
    public UserManagement(MCServerWrapper wrapper){
        this.wrapper = wrapper;
    }
    
    public boolean tellUser(User user, String message){
        if(user.getUuid().equals(new UUID(0, 0))){
            this.wrapper.getLog().info("User Management", message);
            return true;
        } else{
            String command = String.format("/tell %s %s", user.getName(), message);
            return this.wrapper.RUN_MC_TASK.sendToConsole(command);
        }
    }
}
