package de.canitzp.mcserverwrapper;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

public class Logger{
    
    private final Deque<String> lines = new ArrayDeque<>();
    private int multipleEntryIndexNumber = 0;
    private boolean disableMultilineCheck = false;
    
    public void info(String caller, String message, ANSICOLOR... formatting){
        this.log(caller, message, formatting);
    }
    
    public void warn(String caller, String message, ANSICOLOR... formatting){
        this.log(caller, message, formatting.length > 0 ? formatting : new ANSICOLOR[]{ANSICOLOR.YELLOW});
    }
    
    public void error(String caller, String message, ANSICOLOR... formatting){
        this.log(caller, message, formatting.length > 0 ? formatting : new ANSICOLOR[]{ANSICOLOR.RED});
    }
    
    public void list(String caller, List<String> lines, ANSICOLOR... formatting){
        this.disableMultilineCheck = true;
        lines.forEach(s -> this.log(caller, s, formatting));
        this.disableMultilineCheck = false;
    }
    
    public void log(String caller, String msg, ANSICOLOR... formatting){
        StringBuilder coloring = new StringBuilder();
        Arrays.stream(formatting).forEach(ansicolor -> coloring.append(ansicolor.controlCode));
    
        String finalMessage;
        if(caller != null){
            finalMessage = String.format("%s[%s]: %s%s", coloring.toString(), caller, msg, ANSICOLOR.RESET.controlCode);
        } else {
            finalMessage = String.format("%s%s%s", coloring.toString(), msg, ANSICOLOR.RESET.controlCode);
        }
        
        if(!this.disableMultilineCheck && !this.lines.isEmpty() && finalMessage.equals(this.lines.getLast())){
            this.reprintLastLine();
        } else {
            this.multipleEntryIndexNumber = 0;
            this.lines.addLast(finalMessage);
            System.out.println(finalMessage);
        }
    }
    
    private void reprintLastLine(){
        System.out.print("\033[F"); // move one line up to the beginning.
        String msg = String.format("%dx %s", this.multipleEntryIndexNumber++ + 2, this.lines.getLast()); // Overwrite the old line
        System.out.println(msg);
    }
    
    public enum ANSICOLOR {
        RESET("0"),
        BLACK("0;30"),
        RED("0;31"),
        GREEN("0;32"),
        YELLOW("0;33"),
        BLUE("0;34"),
        PURPLE("0;35"),
        CYAN("0;36"),
        WHITE("0;37")
        ;
        
        private String controlCode;
        ANSICOLOR(String controlCode){
            this.controlCode = "\033[" + controlCode + "m";
        }
    }
    
}
