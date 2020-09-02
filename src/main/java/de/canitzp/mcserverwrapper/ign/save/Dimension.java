package de.canitzp.mcserverwrapper.ign.save;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.plugins.internal.WebMap;
import net.querz.mca.MCAFile;
import net.querz.mca.MCAUtil;
import net.querz.nbt.tag.CompoundTag;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dimension {
    
    public static final Pattern mcaFilePattern = Pattern.compile("^.*r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.mca$");
    
    private String name;
    private Map<BlockPos, MCBlock> blockMap = new HashMap<>();
    
    public Dimension(String name){
        this.name = name;
    }
    
    public void readFromDisk(MCServerWrapper wrapper, File worldDir, File tileFolder){
        if(worldDir.exists()){
            long allStartTime = System.currentTimeMillis();
            // 12 Region files:
            // 1: Doesn't work@128MB 47696ms@256MB 43470ms@512MB 43800ms@1024MB 44067ms@2048MB 44804ms@4096MB
            // 2: Doesn't work@128MB Doesn't work@256MB 27100ms@512MB 28667ms@1024MB 30279ms@2048MB 29871ms@4096MB
            // 3: Doesn't work@128MB Doesn't work@256MB 23264ms@512MB 23589ms@1024MB 25389ms@2048MB 24981ms@4096MB
            // 4: Doesn't work@128MB Doesn't work@256MB 21306ms@512MB 21606ms@1024MB 21939ms@2048MB 22580ms@4096MB
            // 5: Doesn't work@128MB Doesn't work@256MB 37672ms@512MB 35035ms@1024MB 22505ms@2048MB 22213ms@4096MB
            // 18 Region files:
            // 4: 28922ms@1024MB
            ThreadPoolExecutor pool = (ThreadPoolExecutor) Executors.newFixedThreadPool(3);
            List<MCAInterpreter.ReturnState> returnStates = new ArrayList<>();
            for(File file : FileUtils.listFiles(new File(worldDir, "region"), new String[]{"mca"}, false)){
                try{
                    MCAInterpreter mcaInterpreter = new MCAInterpreter(file, tileFolder, returnStates);
                    pool.submit(mcaInterpreter);
                    //new MCAInterpreter(file, tileFolder).run();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
            while(!pool.getQueue().isEmpty() || pool.getActiveCount() > 0){
                try{
                    Thread.sleep(500);
                } catch(InterruptedException e){
                    e.printStackTrace();
                }
            }
            System.gc();
            //System.out.println("Finished after " + (System.currentTimeMillis() - allStartTime) + "ms");
            long errored = returnStates.stream().filter(returnState -> returnState.equals(MCAInterpreter.ReturnState.ERROR)).count();
            long skipped = returnStates.stream().filter(returnState -> returnState.equals(MCAInterpreter.ReturnState.SKIP)).count();
            long success = returnStates.stream().filter(returnState -> returnState.equals(MCAInterpreter.ReturnState.DONE)).count();
            wrapper.getLog().info(WebMap.CALLER, "Finished World mapping:");
            wrapper.getLog().info(WebMap.CALLER, "\tSkipped: " + skipped);
            wrapper.getLog().info(WebMap.CALLER, "\tErrored: " + errored);
            wrapper.getLog().info(WebMap.CALLER, "\tSuccess: " + success);
        }
    }
    
    private static class MCAInterpreter implements Runnable {
        
        private final File mcaFile;
        private final File imageDir;
        
        private final List<ReturnState> allStates;
        
        public MCAInterpreter(File mcaFile, File imageDir, List<ReturnState> returnStates){
            this.mcaFile = mcaFile;
            this.imageDir = imageDir;
            this.allStates = returnStates;
        }
        
        @Override
        public void run(){
            if(this.mcaFile.exists()){
                //long startTime = System.currentTimeMillis();
                //System.out.println("Starting region file " + this.mcaFile.getName());
                Matcher m = mcaFilePattern.matcher(this.mcaFile.getName());
                if(m.find()){
                    int regionX = Integer.parseInt(m.group("regionX"));
                    int regionZ = Integer.parseInt(m.group("regionZ"));
                    File imageFile = new File(this.imageDir, "r." + regionX + "." + regionZ + ".png");
                    if(imageFile.exists()){
                        if(imageFile.lastModified() > this.mcaFile.lastModified()){
                            //System.out.println("Skipping file " + this.mcaFile + " due to no changes.");
                            this.allStates.add(ReturnState.SKIP);
                            return;
                        }
                    }
                    try{
                        MCAFile mcaFile = MCAUtil.read(this.mcaFile);
                        int drawX = 0;
                        int drawZ = 0;
                        BufferedImage bufferedImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D g = bufferedImage.createGraphics();
                        Deque<Block> renderBlocks = new ArrayDeque<>();
                        for(int x = 0; x < 512; x++){
                            for(int z = 0; z < 512; z++){
                                for(int y = 255; y > 0; y--){
                                    try{
                                        CompoundTag blockStateAt = mcaFile.getBlockStateAt(x, y, z);
                                        if(blockStateAt != null){
                                            if(blockStateAt.containsKey("Name")){
                                                String namespace = blockStateAt.getString("Name");
                                                Block block = Block.getFromNamespace(namespace);
                                                if(!block.isTransparent()){
                                                    renderBlocks.addFirst(block);
                                                }
                                                if(block.isFullBlock()){
                                                    break;
                                                }
                                            }
                                        }
                                    } catch(Exception e){
                                        //System.out.println(String.format("Error @ x:%d y:%d z:%d (%s)", x, y, z, e.getLocalizedMessage()));
                                        //e.printStackTrace();
                                    }
                                }
                                for(Block renderBlock : renderBlocks){
                                    renderBlock.render(g, drawX, drawZ, 1);
                                }
                                renderBlocks.clear();
                                drawZ += 1;
                            }
                            drawX += 1;
                            drawZ = 0;
                        }
                        //g.dispose();
                        imageFile.getParentFile().mkdirs();
                        if(imageFile.exists()){
                            imageFile.delete();
                        }
                        ImageIO.write(bufferedImage, "png", imageFile);
                        this.allStates.add(ReturnState.DONE);
                        //System.out.println("Finished region(" + regionX + " " + regionZ + ") file after " + (System.currentTimeMillis() - startTime) + "ms");
                    } catch(IOException e){
                        System.out.println("exception");
                        e.printStackTrace();
                        this.allStates.add(ReturnState.ERROR);
                    }
                } else{
                    System.out.println("couldn match");
                    this.allStates.add(ReturnState.ERROR);
                }
            } else{
                System.out.println("file not exist");
                this.allStates.add(ReturnState.ERROR);
            }
            System.gc();
        }
        
        enum ReturnState {
            UNDEFINED,
            DONE,
            ERROR,
            SKIP
        }
    }
}