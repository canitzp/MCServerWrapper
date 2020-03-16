package de.canitzp.mcserverwrapper.ign.save;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.util.List;

public class RegionLoader{
    
    private final File worldDir;
    
    public RegionLoader(File worldDir){
        this.worldDir = worldDir;
    }
    
    // dimension is the real folder name, the dimension uses. Overworld is "" ans nether is "DIM1"
    public List<Chunk> loadRegionFiles(String dimension){
        File regionFolder = new File(this.worldDir, dimension + File.separator + "region");
        if(regionFolder.exists()){
            File region = new File(regionFolder, "r.-1.-1.mca");
            if(region.exists()){
                try(FileInputStream fis = new FileInputStream(region)){
                    byte[] regionData = IOUtils.toByteArray(fis);
                    
                    byte[] locationData = new byte[4096];
                    System.arraycopy(regionData, 0, locationData, 0, 4096);
                    byte[] timestampData = new byte[4096];
                    System.arraycopy(regionData, 4096, timestampData, 0, 4096);
    
                    for(int chuckSelectIndex = 0; chuckSelectIndex < locationData.length; chuckSelectIndex += 4){
                        byte[] offset = new byte[]{locationData[chuckSelectIndex], locationData[chuckSelectIndex + 1], locationData[chuckSelectIndex + 2]};
                        byte sectorCount = locationData[chuckSelectIndex + 3];
                        if(sectorCount != 0){
                            int chunkOffset = ((offset[0] & 0xFF) << 16) | ((offset[1] & 0xFF) << 8) | (offset[2] & 0xFF);
            
                            byte[] chunkData = new byte[sectorCount * 4096];
                            System.arraycopy(regionData, 8192 + 4096 * chunkOffset, chunkData, 0, chunkData.length);
                            
                            /*
                            byte[] chunkExactLength = new byte[4];
                            byte compressionType = chunkData[4] & 0xFF;
                            byte[] compressedData = new byte[];
            
            
                             */
                        }
                    }
                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    
}
