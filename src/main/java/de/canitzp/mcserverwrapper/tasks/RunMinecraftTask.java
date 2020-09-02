package de.canitzp.mcserverwrapper.tasks;

import com.sun.management.OperatingSystemMXBean;
import de.canitzp.mcserverwrapper.MCServerWrapper;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class RunMinecraftTask implements Runnable{
    
    private static final String LOG_NAME = "Launcher";
    
    private final MCServerWrapper wrapper;
    private volatile AtomicBoolean isRunning = new AtomicBoolean(false);
    private AtomicReference<PrintWriter> processWriter = new AtomicReference<>(null);
    
    public RunMinecraftTask(MCServerWrapper wrapper){
        this.wrapper = wrapper;
        
        // start System.in to minecraft redirection. As long as the reference isn't null, it will redirect the System.in stream to the console
        this.wrapper.CONSOLE_INTERPRETER.appendPrintWriter(this.processWriter);
    }
    
    @Override
    public void run(){
        this.innerRunTask();
    }
    
    private void innerRunTask(){
        File jarFile = new File(this.wrapper.getSettings().getString("general.jar_path"));
        // check if jar file exists before continuing
        if(!jarFile.exists()){
            this.wrapper.getLog().error(LOG_NAME, "The minecraft server jar file specified in the configuration file does not exist!");
            return;
        }
        
        this.isRunning.set(true);
        
        try {
            List<String> command = new ArrayList<>();
            command.add(this.wrapper.getSettings().getString("general.java_path"));
    
            int ram = this.wrapper.getSettings().getInt("general.minecraft_ram_maximum");
            command.add("-Xmx" + ram + "M");
            command.add("-Xms" + ram + "M");
    
            command.addAll(this.wrapper.getSettings().getList(String.class, "general.additional_vm_parameter"));
    
            if(this.wrapper.getSettings().getBoolean("general.pass_parameter")){
                List<String> ignoredArgs = this.wrapper.getSettings().getList(String.class, "general.ignored_startup_parameter");
                for(String inputArgument : ManagementFactory.getRuntimeMXBean().getInputArguments()){
                    if(ignoredArgs.stream().noneMatch(inputArgument::startsWith)){
                        command.add(inputArgument);
                    }
                }
            }
            
            command.add("-jar");
            command.add(jarFile.getName());
            command.addAll(this.wrapper.getSettings().getList(String.class, "general.additional_startup_parameter"));
            
            long systemRamBytes = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
            long systemRamMegabytes = Math.round(systemRamBytes / 1000000F);
            List<String> lines = new ArrayList<>();
            lines.add("========== Starting Server ==========");
            lines.add("Server file:   '" + jarFile.getName() + "'");
            lines.add("Server Memory: '" + ram + "MB'");
            lines.add("System Memory: '" + systemRamMegabytes + "MB' ('" + systemRamBytes + "Bytes')");
            lines.add("Directory:     '" + jarFile.getParentFile().getCanonicalPath() + File.separator + "'");
            lines.add("Command:       '" + String.join(" ", command) + "'");
            lines.add("=====================================");
            this.wrapper.getLog().list(LOG_NAME, lines);
            
            this.wrapper.getPluginManager().onServerStart(command);
            long startupTime = System.currentTimeMillis();
            
            Process p = new ProcessBuilder(command).directory(jarFile.getParentFile()).start();
            
            // redirect text to minecraft
            this.processWriter.set(new PrintWriter(p.getOutputStream()));
            
            // redirect minecraft console to us
            try(InputStreamReader isr = new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8)){
                try(BufferedReader br = new BufferedReader(isr)){
                    String line;
                    while(p.isAlive()) {
                        this.wrapper.sleep(1);
                        while((line = br.readLine()) != null) {
                            this.wrapper.getMinecraftConsoleReader().scheduleLine(line);
                        }
                    }
                }
            }
            
            // close console writer after minecraft is closed
            this.processWriter.get().close();
            this.processWriter.set(null);
            
            long serverUptime = System.currentTimeMillis() - startupTime;
            
            this.wrapper.getPluginManager().onServerStopped(serverUptime, p.exitValue());
            
            // evaluate exit code
            if (p.exitValue() != 0) {
                this.wrapper.getLog().error(LOG_NAME, "Minecraft server exited with an error! '" + p.exitValue() + "'");
                // open error stream to get the exception and print it to logger
                try(InputStreamReader isr = new InputStreamReader(p.getErrorStream(), StandardCharsets.UTF_8)){
                    String line;
                    try(BufferedReader br = new BufferedReader(isr)){
                        while((line = br.readLine()) != null) {
                            this.wrapper.getLog().warn(LOG_NAME, line);
                        }
                    }
                }
            } else {
                this.wrapper.getLog().info(LOG_NAME, "Minecraft server exited fine.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        this.isRunning.set(false);
    }
    
    public boolean sendToConsole(String msg){
        if(!msg.isEmpty()){
            if(this.isRunning()){
                if(this.processWriter.get() != null){
                    this.processWriter.get().println(msg);
                    this.processWriter.get().flush();
                    return true;
                } else {
                    this.wrapper.getLog().error(LOG_NAME, "Can't send to console, since minecraft has a communication problem! '" + msg + "'");
                }
            } else {
                this.wrapper.getLog().error(LOG_NAME, "Can't send to console, since minecraft isn't running! '" + msg + "'");
            }
        }
        return false;
    }
    
    public boolean isRunning(){
        return isRunning.get();
    }
    
    public boolean stopServer(){
        if(this.isRunning()){
            this.sendToConsole("stop");
            int killTime = 10*60; // one minute before it decides to kill the loop
            while(this.isRunning()){
                this.wrapper.sleep(100);
                killTime--;
                if(killTime <= 0){
                    this.wrapper.getLog().error(LOG_NAME, "Couldn't stop the server!");
                    return false;
                }
            }
        }
        return true;
    }
    
}
