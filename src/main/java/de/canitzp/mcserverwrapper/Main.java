package de.canitzp.mcserverwrapper;

import org.apache.commons.cli.*;

public class Main {
    
    public static void main(String[] args){
        Options options = new Options();
        Option startInLockedMode = new Option("w", "wrapper", false, "start without starting the server");
        startInLockedMode.setRequired(false);
        options.addOption(startInLockedMode);
        
        CommandLineParser cmp = new DefaultParser();
        HelpFormatter hf = new HelpFormatter();
        CommandLine cmd = null;
        
        try{
            cmd = cmp.parse(options, args);
        } catch(ParseException e){
            hf.printHelp("server wrapper", options);
            System.exit(-1);
        }
        
        if(cmd != null){
            MCServerWrapper mcsw = new MCServerWrapper(cmd.hasOption("wrapper"));
        }
    }
    
}
