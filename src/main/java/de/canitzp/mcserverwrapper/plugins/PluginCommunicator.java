package de.canitzp.mcserverwrapper.plugins;

public class PluginCommunicator{
    
    private final PluginManager manager;
    
    public PluginCommunicator(PluginManager manager){
        this.manager = manager;
    }
    
    // returns if the command was send to the server, not if the command runs successful
    public boolean runCommand(String command){
        return this.manager.getWrapper().RUN_MC_TASK.sendToConsole(command);
    }
    
}
