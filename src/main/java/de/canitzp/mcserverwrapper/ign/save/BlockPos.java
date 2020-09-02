package de.canitzp.mcserverwrapper.ign.save;

public class BlockPos {
    
    private int x, y, z;
    
    public BlockPos(int x, int y, int z){
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    @Override
    public boolean equals(Object obj){
        return obj instanceof BlockPos && ((BlockPos) obj).is(this.getX(), this.getY(), this.getZ());
    }
    
    @Override
    public String toString(){
        return String.format("BlockPos{x=%d; y=%d; z=%d}", this.x, this.y, this.z);
    }
    
    public int getX(){
        return x;
    }
    
    public void setX(int x){
        this.x = x;
    }
    
    public int getY(){
        return y;
    }
    
    public void setY(int y){
        this.y = y;
    }
    
    public int getZ(){
        return z;
    }
    
    public void setZ(int z){
        this.z = z;
    }
    
    public boolean is(int x, int y, int z){
        return this.getX() == x && this.getY() == y && this.getZ() == z;
    }
}
