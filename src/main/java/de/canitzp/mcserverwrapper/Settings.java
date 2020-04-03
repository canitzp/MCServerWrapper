package de.canitzp.mcserverwrapper;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class Settings{
    
    private final Config config;
    private final File configFile;
    
    public Settings(File configFile, String defaultConfigPath){
        this.configFile = configFile;
        this.config = ConfigFactory.parseFile(configFile).withFallback(ConfigFactory.parseResources(defaultConfigPath));
    }
    
    public void overwriteCurrentConfig(){
        try{
            FileUtils.write(this.configFile, this.config.root().render(ConfigRenderOptions.defaults().setOriginComments(false)), StandardCharsets.UTF_8);
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public Config getConfig(){
        return config;
    }
    
    public String getString(String path){
        return this.config.hasPath(path) ? this.config.getString(path) : null;
    }
    
    public File getFile(String path){
        return this.config.hasPath(path) ? new File(this.config.getString(path)) : null;
    }
    
    public boolean getBoolean(String path){
        return this.config.hasPath(path) && this.config.getBoolean(path);
    }
    
    public int getInt(String path){
        return this.config.hasPath(path) ? this.config.getInt(path) : 0;
    }
    
    public <T> List<T> getList(Class<T> type, String path){
        return this.config.hasPath(path) ? (List<T>) this.config.getList(path).unwrapped() : null;
    }
}
