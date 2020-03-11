package de.canitzp.mcserverwrapper.tasks;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ConsoleInterpreter implements Runnable{
    
    private static final String LOG_NAME = "CONSOLE BUFFER";
    
    private final AtomicBoolean run = new AtomicBoolean(true);
    private final MCServerWrapper wrapper;
    
    private final List<AtomicReference<PrintWriter>> printWriters = new ArrayList<>();
    
    public ConsoleInterpreter(MCServerWrapper wrapper){
        this.wrapper = wrapper;
    }
    
    @Override
    public void run(){
        String wrapperCommandPrefix = this.wrapper.getSettings().getString("general.wrapper_command_prefix");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))){
            String line;
            while(this.run.get()) {
                this.wrapper.sleep(10);
                while((line = in.readLine()) != null) {
                    if(!line.isEmpty()){
                        if(!line.startsWith(wrapperCommandPrefix) && this.wrapper.RUN_MC_TASK.isRunning()){
                            if(!this.printWriters.isEmpty()){
                                for(AtomicReference<PrintWriter> writer : new ArrayList<>(this.printWriters)){
                                    if(writer.get() != null){
                                        PrintWriter w = writer.get();
                                        w.println(line);
                                        w.flush();
                                    }
                                }
                            } else {
                                this.wrapper.getLog().error(LOG_NAME, "Can't write to Server '" + line + "'");
                            }
                        } else {
                            // command for wrapper
                            this.wrapper.getCommandHandler().scheduleCommand(User.CONSOLE, line);
                            //this.wrapper.appendCommandToProcess(line);
                        }
                    }
                }
            }
        } catch (IOException var3) {
            var3.printStackTrace();
        }
    }
    
    public void appendPrintWriter(AtomicReference<PrintWriter> writer){
        this.printWriters.add(writer);
    }
    
    public void removePrintWriter(AtomicReference<PrintWriter> writer){
        this.printWriters.remove(writer);
    }
    
    public void stop(){
        this.run.set(false);
    }
    
}
