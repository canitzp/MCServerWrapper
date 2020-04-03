package de.canitzp.mcserverwrapper.tasks;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class BackupManager implements Runnable{
    
    private final static String LOG_NAME = "Backup";
    
    private final AtomicBoolean run = new AtomicBoolean(true);
    private final Deque<String> scheduledBackup = new ArrayDeque<>(2);
    private final MCServerWrapper wrapper;
    
    private final AtomicBoolean backupInProgress = new AtomicBoolean(false);
    
    public BackupManager(MCServerWrapper wrapper){
        this.wrapper = wrapper;
    }
    
    private long nextBackupTime = 0;
    public void tick(){
        if(this.nextBackupTime <= 0){
            this.nextBackupTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(this.wrapper.getSettings().getInt("backup.backup_interval"));
        } else if(this.nextBackupTime <= System.currentTimeMillis()) {
            this.nextBackupTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(this.wrapper.getSettings().getInt("backup.backup_interval"));
            this.wrapper.getLog().info(LOG_NAME, "Automated backup has started.");
            if(this.wrapper.getSettings().getBoolean("backup.backup_server_stop")){
                this.wrapper.getLog().info(LOG_NAME, "Stopping server for backup.");
                boolean wasRunning = this.wrapper.RUN_MC_TASK.isRunning();
                this.wrapper.RUN_MC_TASK.stopServer();
                if(this.scheduleBackup("automatic")){
                    this.waitForBackupFree();
                    if(wasRunning){
                        this.wrapper.getLog().info(LOG_NAME, "Starting server after backup.");
                        this.wrapper.startMinecraftServer();
                    } else {
                        this.wrapper.getLog().info(LOG_NAME, "Automatic backup has finished.");
                    }
                }
            } else {
                this.scheduleBackup("automatic");
                this.waitForBackupFree();
            }
        }
    }
    
    @Override
    public void run(){
        while(this.run.get()){
            this.wrapper.sleep(10000);
            
            if(!this.scheduledBackup.isEmpty()){
                this.backupInProgress.lazySet(true);
                String backupName = this.scheduledBackup.removeFirst();
                this.runBackup(backupName);
                this.backupInProgress.lazySet(false);
            }
        }
    }
    
    private void runBackup(String backupName){
        File minecraftRoot = this.wrapper.getSettings().getFile("general.jar_path").getParentFile();
        File backupRoot = this.wrapper.getSettings().getFile("backup.backup_directory");
        if(backupRoot != null){
            if(!backupRoot.exists()){
                backupRoot.mkdirs();
            }
            File backup = new File(backupRoot, backupName + "_" + (new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss")).format(new Date()));
            this.backupWorldFolder(minecraftRoot, backup);
            this.backupFolderExceptWorldAndBackup(minecraftRoot, backup);
            this.backupFiles(minecraftRoot, backup);
        }
    }
    
    private void backupWorldFolder(File minecraftRoot, File backupDir) {
        File worldDir = new File(minecraftRoot, "world");
        if (worldDir.exists()) {
            //this.wrapper.getLog().info(LOG_NAME, "Backing up world.");
            File dir = new File(backupDir, "world");
            dir.mkdirs();
            
            try {
                FileUtils.copyDirectory(worldDir, dir, true);
            } catch (IOException var5) {
                var5.printStackTrace();
            }
            
            //this.wrapper.getLog().info(LOG_NAME, "Backing up world finished.");
        } else {
            this.wrapper.getLog().info(LOG_NAME, "No world directory found to backup.");
        }
        
    }
    
    private void backupFolderExceptWorldAndBackup(File minecraftRoot, File backupDir) {
        //this.wrapper.getLog().info(LOG_NAME, "Backing up directories.");
        File[] files = minecraftRoot.listFiles();
        if (files != null) {
            for(File file : files){
                if(file.isDirectory()){
                    try{
                        if(!file.getName().equals("backup") && !file.getName().equals("world") && !file.getName().equals(".")){
                            //this.wrapper.getLog().info(LOG_NAME, file.getName());
                            FileUtils.copyDirectoryToDirectory(file, backupDir);
                        }
                    }catch(IOException var9){
                        var9.printStackTrace();
                    }
                }
            }
        }
        
        //this.wrapper.getLog().info(LOG_NAME, "Backing up directories finished.");
    }
    
    private void backupFiles(File minecraftRoot, File backupDir) {
        //this.wrapper.getLog().info(LOG_NAME, "Backing up single files.");
        
        for(File file : FileUtils.listFiles(minecraftRoot, null, false)){
            try{
                if(!file.getName().equals("updater.jar")){
                    //this.wrapper.getLog().info(LOG_NAME, file.getName());
                    FileUtils.copyFileToDirectory(file, backupDir, true);
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
        
        //this.wrapper.getLog().info(LOG_NAME, "Backing up single files finished.");
    }
    
    public boolean scheduleBackup(String backupName){
        try{
            this.scheduledBackup.addLast(backupName);
            this.backupInProgress.lazySet(true);
            return true;
        } catch(IllegalStateException e){
            this.wrapper.getLog().error(LOG_NAME, "Couldn't schedule backup, since the queue is full!");
            return false;
        }
    }
    
    public void waitForBackupFree(){
        while(this.backupInProgress.get()){
            this.wrapper.sleep(100);
        }
    }
}