package de.canitzp.mcserverwrapper.ign.save;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class Block {
    
    private static Map<String, Block> BLOCKS = new HashMap<>();
    
    private String namespace;
    private Color color;
    private float opacity;
    private String textureName;
    
    private Block(String namespace, Color color, float opacity, String textureName){
        this.namespace = namespace;
        if(color != null){
            this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * opacity));
        }
        this.opacity = opacity;
        this.textureName = textureName;
        //System.out.println("Loaded: " + this);
    }
    
    @Override
    public String toString(){
        return new StringJoiner(", ", Block.class.getSimpleName() + "[", "]")
            .add("namespace='" + namespace + "'")
            .add("color=" + color)
            .add("opacity=" + opacity)
            .add("textureName='" + textureName + "'")
            .toString();
    }
    
    // return if it should stop looking for more blocks
    public void render(Graphics2D g, int x, int y, int size){
        if(this.color != null){
            this.renderColor(g, x, y, size);
        }
    }
    
    public boolean isFullBlock(){
        return this.opacity >= 1.0F;
    }
    
    public boolean isTransparent(){
        return this.opacity <= 0.0F;
    }
    
    private void renderColor(Graphics2D g, int x, int y, int size){
        g.setColor(this.color);
        g.fillRect(x, y, size, size);
    }
    
    public static Block getFromNamespace(String namespace){
        return BLOCKS.getOrDefault(namespace, BLOCKS.get("minecraft:air"));
    }
    
    private static void registerBlock(Block block){
        BLOCKS.put(block.namespace, block);
    }
    
    public static void reloadBlocksFromFile(MCServerWrapper wrapper){
        BLOCKS.clear();
        File blockDefinitionJson = new File(new File(wrapper.getSettings().getFile("general.jar_path").getParentFile(), "webmap"), "block_colors.json");
        if(!blockDefinitionJson.exists()){
            try{
                FileUtils.copyInputStreamToFile(Block.class.getResourceAsStream("/webmap/block_colors.json"), blockDefinitionJson);
            } catch(IOException e){
                e.printStackTrace();
                return;
            }
        }
        
        try(FileReader fr = new FileReader(blockDefinitionJson)){
            JsonElement rootElement = JsonParser.parseReader(fr);
            if(rootElement.isJsonObject()){
                for(Map.Entry<String, JsonElement> entry : ((JsonObject) rootElement).entrySet()){
                    String namespace = entry.getKey();
                    if(entry.getValue().isJsonObject()){
                        JsonObject jo = (JsonObject) entry.getValue();
                        float opacity = 1.0F;
                        try{
                            opacity = jo.has("opacity") ? jo.get("opacity").getAsFloat() : 1.0F; // todo type save float
                        } catch(NumberFormatException e){
                            e.printStackTrace();
                        }
                        if(jo.has("hex")){
                            String hexString = jo.get("hex").getAsString();
                            try{
                                registerBlock(new Block(namespace, Color.decode(hexString), opacity, null));
                            } catch(NumberFormatException e){
                                e.printStackTrace();
                            }
                        } else if(jo.has("texture")){
                            String texture = jo.get("texture").getAsString();
                            registerBlock(new Block(namespace, null, opacity, texture));
                        }
                    } else{
                        String value = entry.getValue().getAsString();
                        try{
                            registerBlock(new Block(namespace, Color.decode(value), 1.0F, null)); // register with color
                        } catch(NumberFormatException ignored){
                            registerBlock(new Block(namespace, null, 1.0F, value)); // register with texture
                        }
                    }
                }
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }
    
}
