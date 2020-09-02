package de.canitzp.mcserverwrapper.commands;

import de.canitzp.mcserverwrapper.Logger;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.ign.User;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DebugCommand implements IWrapperCommand {
    
    private static final int TIME_TO_WAIT = 5000;
    private final Options options = new Options().addOption("a", "all", false, "").addOption("t", "time", true, "");
    
    @Override
    public String[] triggerNames(){
        return new String[]{"debug"};
    }
    
    @Override
    public Options options(){
        return this.options;
    }
    
    @Override
    public String helpDescription(){
        return "Command to debug minecraft (Version 1.8 and above). This runs the debug command for some seconds and then prints the current tps and the most tick consuming tasks. Shows current tps. Append --all to view the main tps killer";
    }
    
    @Override
    public User.UserLevel minUserLevel(){
        return User.UserLevel.OWNER;
    }
    
    @Override
    public boolean execute(MCServerWrapper wrapper, User user, CommandLine cmd){
        boolean viewAll = false;
        int time = TIME_TO_WAIT;
        if(cmd != null){
            viewAll = cmd.hasOption("--all");
            if(cmd.hasOption("--time")){
                String timeRaw = cmd.getOptionValue("time");
                try{
                    time = Integer.parseInt(timeRaw);
                } catch(NumberFormatException ignored){
                    wrapper.getLog().error(CommandHandler.LOG_NAME, "--time is specified without a valid number");
                    return true;
                }
            }
        }
        int finalTime = time;
        boolean finalViewAll = viewAll;
        Executors.newSingleThreadExecutor().execute(() -> {
            debugMinecraft(wrapper, finalViewAll, finalTime, s -> {
                wrapper.getLog().info(CommandHandler.LOG_NAME, s, Logger.ANSICOLOR.GREEN);
            });
        });
        return true;
    }
    
    public static void debugMinecraft(MCServerWrapper wrapper, boolean viewAll, int milliseconds, Consumer<String> messageHandler){
        if(wrapper.RUN_MC_TASK.isRunning()){
            wrapper.RUN_MC_TASK.sendToConsole("/debug start");
            wrapper.sleep(milliseconds);
            wrapper.RUN_MC_TASK.sendToConsole("/debug stop");
            wrapper.sleep(500); // time to let mc write the debug log
            try{
                interpretDebugLog(wrapper, viewAll, milliseconds, messageHandler);
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }
    
    private static void interpretDebugLog(MCServerWrapper wrapper, boolean viewAll, int time, Consumer<String> messageHandler) throws IOException{
        File debugFolder = new File(wrapper.getSettings().getFile("general.jar_path").getParentFile(), "debug");
        if(debugFolder.exists()){
            List<File> orderedFiles = FileUtils.listFiles(debugFolder, null, false).stream().sorted((o1, o2) -> (int) (o2.lastModified() - o1.lastModified())).collect(Collectors.toList());
            if(!orderedFiles.isEmpty()){
                File latestDebugLog = orderedFiles.get(0);
                if(latestDebugLog.exists()){
                    StringBuilder message = new StringBuilder();
                    int highestIndex = 0;
                    for(String line : FileUtils.readLines(latestDebugLog, StandardCharsets.UTF_8)){
                        if(line.startsWith("Tick span: ")){
                            float tickSpan = Float.parseFloat(line.substring(11, line.length() - 6));
                            float tps = (tickSpan / (time * 1.0F)) * 1000;
                            messageHandler.accept(String.format("Current TPS: %.2f", tps));
                        }
                        if(viewAll && line.startsWith("[")){
                            int index = Integer.parseInt(line.substring(1, 3));
                            
                            if(index == 0){
                                highestIndex = 0;
                            } else if(highestIndex > index){
                                // ignore entry
                                continue;
                            }
                            highestIndex = index;
                            
                            try{
                                int nameStartIndex = ("|   ".length() * index) + "[xx] ".length();
                                String name = line.substring(nameStartIndex, line.indexOf("(", nameStartIndex));
                                int firstTickTimeStartIndex = nameStartIndex + name.length() + 1;
                                String firstTickTimeString = line.substring(firstTickTimeStartIndex, line.indexOf("/", firstTickTimeStartIndex));
                                int secondTickTimeStartIndex = nameStartIndex + name.length() + firstTickTimeString.length() + 2;
                                String secondTickTimeString = line.substring(secondTickTimeStartIndex, line.indexOf(")", secondTickTimeStartIndex));
                                float percentageRelative = Float.parseFloat(line.substring(line.indexOf("-") + 1, line.indexOf("%")));
                                float percentageAbsolute = Float.parseFloat(line.substring(line.indexOf("%/") + 2, line.lastIndexOf("%")));
                                
                                message.append(String.format("%s%s %.2f%%", new String(new char[index]).replace("\0", "|   "), name, percentageAbsolute));
                                message.append("\n");
                            } catch(Exception ignored){
                                //message.append(String.format("Error in parsing line: \"%s\"", line));
                                //message.append("\n");
                            }
                        }
                    }
                    if(message.length() > 0){
                        message.deleteCharAt(message.length() - 1);
                        messageHandler.accept(message.toString());
                    }
                }
            }
        }
    }
    
}
