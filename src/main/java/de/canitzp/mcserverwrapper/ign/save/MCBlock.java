package de.canitzp.mcserverwrapper.ign.save;

import java.awt.*;

public enum MCBlock {
    
    AIR("minecraft:air", Color.WHITE, 0.0F),
    CAVE_AIR("minecraft:cave_air", Color.WHITE, 0.0F),
    
    // underground blocks
    BEDROCK("minecraft:bedrock", Color.BLACK),
    STONE("minecraft:stone", new Color(116, 116, 116)),
    COBBLESTONE("minecraft:cobblestone", new Color(183, 183, 183)),
    ANDESITE("minecraft:andesite", Color.LIGHT_GRAY.darker().darker()),
    DIORITE("minecraft:diorite", Color.LIGHT_GRAY),
    GRANITE("minecraft:granite", Color.RED.darker()),
    TERRACOTTA("minecraft:terracotta", new Color(216, 111, 53)),
    
    // overground blocks
    DIRT("minecraft:dirt", new Color(139, 69, 19)),
    COARSE_DIRT("minecraft:coarse_dirt", new Color(139, 69, 19)),
    PODZOL("minecraft:podzol", new Color(129, 84, 0)),
    MYCELIUM("minecraft:mycelium", new Color(138, 127, 146)),
    SAND("minecraft:sand", new Color(216, 208, 155)),
    RED_SAND("minecraft:red_sand", new Color(225, 123, 38)),
    GRAVEL("minecraft:gravel", Color.LIGHT_GRAY),
    GRASS_BLOCK("minecraft:grass_block", Color.GREEN),
    GRASS("minecraft:grass", new Color(88, 201, 70), 0.5F),
    FARMLAND("minecraft:farmland", Color.decode("#301A08")),
    GRASS_PATH("minecraft:grass_path", Color.YELLOW),
    CLAY("minecraft:clay", Color.decode("#9AA0AC")),
    
    // tree stuff
    OAK_LOG("minecraft:oak_log", new Color(186, 135, 38)),
    BIRCH_LOG("minecraft:birch_log", new Color(221, 178, 97)),
    SPREUCE_LOG("minecraft:spruce_log", new Color(178, 109, 4).brighter()),
    JUNGLE_LOG("minecraft:jungle_log", Color.decode("#3D3312")),
    ACACIA_LOG("minecraft:acacia_log", Color.decode("#6C665C")),
    DARK_OAK_LOG("minecraft:dark_oak_log", new Color(178, 109, 4)),
    OAK_LEAVES("minecraft:oak_leaves", new Color(74, 208, 69), 0.5F),
    BIRCH_LEAVES("minecraft:birch_leaves", new Color(74, 208, 69), 0.5F),
    SPREUCE_LEAVES("minecraft:spruce_leaves", new Color(74, 208, 69).darker(), 0.5F),
    JUNGLE_LEAVES("minecraft:jungle_leaves", Color.decode("#2E6316"), 0.5F),
    ACACIA_LEAVES("minecraft:acacia_leaves", Color.decode("#455B25"), 0.5F),
    DARK_OAK_LEAVES("minecraft:dark_oak_leaves", new Color(74, 208, 69), 0.5F),
    
    // cold blocks
    SNOW("minecraft:snow", Color.WHITE),
    SNOW_BLOCK("minecraft:snow_block", Color.WHITE),
    ICE("minecraft:ice", Color.decode("#A8C5FB"), 0.5F),
    PACKED_ICE("minecraft:packed_ice", Color.decode("#86AFF8")),
    BLUE_ICE("minecraft:blue_ice", Color.decode("#3E5D93")),
    
    // building blocks
    STONE_BRICKS("minecraft:stone_bricks", Color.GRAY),
    
    // mushrooms
    BROWN_MUSHROOM_BLOCK("minecraft:brown_mushroom_block", Color.decode("#946D52")),
    RED_MUSHROOM_BLOCK("minecraft:red_mushroom_block", Color.decode("#C32826")),
    MUSHROOM_STEM("minecraft:mushroom_stem", Color.decode("#C2BCAC")),
    
    // flowers
    CACTUS("minecraft:cactus", Color.decode("#598A25")),
    
    // liquids
    WATER("minecraft:water", new Color(35, 137, 218), 0.75F),
    LAVA("minecraft:lava", new Color(81, 6, 13), 0.95F),
    ;
    
    private String namespace;
    private Color color;
    private float opacity = 1.0F;
    
    MCBlock(String namespace, Color color, float opacity){
        this.namespace = namespace;
        this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(255 * opacity));
        this.opacity = opacity;
    }
    
    MCBlock(String namespace, Color color){
        this.namespace = namespace;
        this.color = color;
    }
    
    public String getNamespace(){
        return namespace;
    }
    
    public boolean shouldBeRendered(){
        return this.opacity > 0.0F;
    }
    
    public boolean isFullBlock(){
        return this.opacity >= 1F;
    }
    
    public void drawBlock(Graphics2D g, int x, int y){
        if(this.shouldBeRendered()){
            g.setColor(this.color);
            g.fillRect(x, y, 1, 1);
        }
    }
    
}
