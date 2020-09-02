package de.canitzp.mcserverwrapper.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RunMinecraftUpdate implements Runnable {
    
    private static final String LOG_NAME = "Updater";
    private final MCServerWrapper wrapper;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    private long nextUpdateTime = 0;
    
    public RunMinecraftUpdate(MCServerWrapper wrapper){
        this.wrapper = wrapper;
    }
    
    @Override
    public void run(){
        this.isRunning.lazySet(true);
        File jarFile = new File(this.wrapper.getSettings().getString("general.jar_path"));
        this.wrapper.getLog().info(LOG_NAME, "Checking for Updates.");
        
        // check if update is needed
        boolean doSnapshotUpdates = this.wrapper.getSettings().getBoolean("update.use_snapshots");
        String versionToKeep = this.wrapper.getSettings().getString("update.keep_single_version");
        String updateVersion = null;
        if(jarFile.exists()){
            String currentHash = this.createSha1(jarFile);
            if(versionToKeep != null){
                String currentVersionHash = this.getVersionSHA(versionToKeep);
                if(currentVersionHash != null){
                    if(!currentVersionHash.equalsIgnoreCase(currentHash)){
                        updateVersion = versionToKeep; // update whenever the current installed version is different to the specified "keep" version
                    }
                } else{
                    this.wrapper.getLog().error(LOG_NAME, "The specified 'keep_single_version' doesn't exists! Value: '" + versionToKeep + "'");
                    this.isRunning.lazySet(false);
                    return;
                }
            } else{ // no specific version is set. Using the latest one
                String versionString = this.getLatestVersionString(doSnapshotUpdates);
                String currentVersionHash = this.getVersionSHA(versionString);
                if(currentVersionHash != null){
                    if(!currentVersionHash.equalsIgnoreCase(currentHash)){
                        updateVersion = versionString; // update whenever the current installed version is different to the latest available version
                    }
                }
            }
        } else{
            updateVersion = versionToKeep != null ? versionToKeep : this.getLatestVersionString(doSnapshotUpdates);
        }
        
        if(updateVersion != null){
            this.wrapper.getLog().info(LOG_NAME, "Update found and initiated.");
            if(this.wrapper.RUN_MC_TASK.stopServer()){
                this.wrapper.getLog().info(LOG_NAME, "Server stopped. Starting Update.");
                
                // starting backup via manager
                if(this.wrapper.getSettings().getBoolean("backup.backup_on_update")){
                    if(this.wrapper.BACKUP_MANAGER.scheduleBackup("update_to_" + updateVersion)){
                        this.wrapper.BACKUP_MANAGER.waitForBackupFree();
                    } else{
                        // todo abort update and restore
                    }
                } else{
                    this.wrapper.getLog().info(LOG_NAME, "Suppressing backup due to configuration.");
                }
                
                this.wrapper.getLog().info(LOG_NAME, "Downloading " + updateVersion + " version as '" + jarFile.getName() + "'");
                URL downloadURL = this.getVersionDownloadURL(updateVersion);
                try{
                    FileUtils.copyURLToFile(downloadURL, jarFile);
                    this.wrapper.getLog().info(LOG_NAME, "File downloaded successfully. Update done. Server is getting started.");
                } catch(IOException e){
                    // todo abort update and restore
                    e.printStackTrace();
                }
            } else{
                this.wrapper.getLog().error(LOG_NAME, "Server couldn't be stopped. Updated aborted.");
            }
        } else{
            this.wrapper.getLog().info(LOG_NAME, "No updates available.");
        }
        
        this.isRunning.lazySet(false);
    }
    
    public void tick(){
        if(this.nextUpdateTime <= 0){
            this.nextUpdateTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(this.wrapper.getSettings().getInt("update.check_delay"));
        } else if(this.nextUpdateTime <= System.currentTimeMillis()){
            this.nextUpdateTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(this.wrapper.getSettings().getInt("update.check_delay"));
            this.wrapper.startMinecraftUpdate();
        }
    }
    
    private String createSha1(File file){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            InputStream fis = new FileInputStream(file);
            int n = 0;
            byte[] buffer = new byte[8192];
            
            while(n != -1){
                n = fis.read(buffer);
                if(n > 0){
                    digest.update(buffer, 0, n);
                }
            }
            
            byte[] data = digest.digest();
            StringBuilder hexString = new StringBuilder();
            
            for(byte b : data){
                String hex = Integer.toHexString(255 & b);
                if(hex.length() == 1){
                    hexString.append('0');
                }
                
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch(IOException | NoSuchAlgorithmException var12){
            var12.printStackTrace();
            return null;
        }
    }
    
    private JsonObject getVersionManifest(){
        try{
            URL url = new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json");
            return JsonParser.parseReader(new InputStreamReader(url.openStream())).getAsJsonObject();
        } catch(UnknownHostException uhe){
            this.wrapper.getLog().error(LOG_NAME, "A internet connection could not be established.");
        } catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }
    
    private String getLatestVersionString(boolean snapshot){
        JsonObject manifest = this.getVersionManifest();
        if(manifest != null){
            return manifest.getAsJsonObject("latest").get(snapshot ? "snapshot" : "release").getAsString();
        }
        return null;
    }
    
    private JsonObject getVersion(String versionString){
        JsonObject manifest = this.getVersionManifest();
        if(manifest != null){
            JsonArray versions = manifest.getAsJsonArray("versions");
            for(JsonElement entry : versions){
                if(versionString.equalsIgnoreCase(entry.getAsJsonObject().get("id").getAsString())){
                    try{
                        URL versionUrl = new URL(entry.getAsJsonObject().get("url").getAsString());
                        return JsonParser.parseReader(new InputStreamReader(versionUrl.openStream())).getAsJsonObject();
                    } catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }
    
    private URL getVersionDownloadURL(String versionString){
        JsonObject versionData = this.getVersion(versionString);
        if(versionData != null && versionData.has("downloads")){
            JsonObject downloads = versionData.get("downloads").getAsJsonObject();
            if(downloads.has("server")){
                JsonObject server = downloads.get("server").getAsJsonObject();
                if(server.has("url")){
                    try{
                        return new URL(server.get("url").getAsString());
                    } catch(MalformedURLException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;
    }
    
    private String getVersionSHA(String versionString){
        JsonObject versionData = this.getVersion(versionString);
        if(versionData != null && versionData.has("downloads")){
            JsonObject downloads = versionData.get("downloads").getAsJsonObject();
            if(downloads.has("server")){
                JsonObject server = downloads.get("server").getAsJsonObject();
                if(server.has("sha1")){
                    return server.get("sha1").getAsString();
                }
            }
        }
        return null;
    }
    
    public boolean isRunning(){
        return this.isRunning.get();
    }
}
