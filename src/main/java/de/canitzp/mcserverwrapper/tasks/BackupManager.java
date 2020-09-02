package de.canitzp.mcserverwrapper.tasks;

import de.canitzp.mcserverwrapper.MCServerWrapper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupManager implements Runnable {
    
    private final static String LOG_NAME = "Backup";
    
    private final AtomicBoolean run = new AtomicBoolean(true);
    private final Deque<String> scheduledBackup = new ArrayDeque<>(2);
    private final MCServerWrapper wrapper;
    
    private final AtomicBoolean backupInProgress = new AtomicBoolean(false);
    private long nextBackupTime = 0;
    
    public BackupManager(MCServerWrapper wrapper){
        this.wrapper = wrapper;
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
    
    public void tick(){
        if(this.nextBackupTime <= 0){
            this.nextBackupTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(this.wrapper.getSettings().getInt("backup.backup_interval"));
        } else if(this.nextBackupTime <= System.currentTimeMillis()){
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
                    } else{
                        this.wrapper.getLog().info(LOG_NAME, "Automatic backup has finished.");
                    }
                }
            } else{
                this.scheduleBackup("automatic");
                this.waitForBackupFree();
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
            this.deleteBackups(backupRoot);
            
            File backup = new File(backupRoot, backupName + "_" + (new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss")).format(new Date()) + ".zip");
            try(ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backup))){
                this.backupWorldFolder(minecraftRoot, zos);
                if(!this.wrapper.getSettings().getBoolean("backup.backup_world_only")){
                    this.backupExceptWorldAndBackup(minecraftRoot, zos);
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }
    
    private void backupWorldFolder(File minecraftRoot, ZipOutputStream zip) throws IOException{
        File worldDir = new File(minecraftRoot, "world");
        if(worldDir.exists()){
            this.zipFile(zip, worldDir.getName(), worldDir);
        } else{
            this.wrapper.getLog().info(LOG_NAME, "No world directory found to backup.");
        }
        
    }
    
    private void backupExceptWorldAndBackup(File minecraftRoot, ZipOutputStream zip) throws IOException{
        File[] files = minecraftRoot.listFiles();
        if(files != null){
            for(File file : files){
                if(!file.getName().equals("backup") && !file.getName().equals("world") && !file.getName().equals(".")){
                    this.zipFile(zip, file.getName(), file);
                }
            }
        }
    }
    
    private void zipFile(ZipOutputStream zip, String fileName, File file) throws IOException{
        if(file.isHidden()){
            return;
        }
        if(file.isDirectory()){
            if(fileName.endsWith("/")){
                zip.putNextEntry(new ZipEntry(fileName));
            } else{
                zip.putNextEntry(new ZipEntry(fileName + "/"));
            }
            zip.closeEntry();
            File[] children = file.listFiles();
            if(children != null){
                for(File childFile : children){
                    zipFile(zip, fileName + "/" + childFile.getName(), childFile);
                }
            }
            return;
        }
        FileInputStream fis = new FileInputStream(file);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zip.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while((length = fis.read(bytes)) >= 0){
            zip.write(bytes, 0, length);
        }
        fis.close();
    }
    
    private void deleteBackups(File backupRoot){
        // decrement by one because we create a new backup
        int keepBackupAmount = this.wrapper.getSettings().getInt("backup.backup_keep_amount") - 1;
        List<File> orderedFiles = FileUtils.listFiles(backupRoot, null, false).stream().sorted((o1, o2) -> Long.compare(o2.lastModified(), o1.lastModified())).collect(Collectors.toList());
        if(orderedFiles.size() > keepBackupAmount){
            for(int i = keepBackupAmount; i < orderedFiles.size(); i++){
                File toDelete = orderedFiles.get(i);
                this.wrapper.getLog().info(LOG_NAME, "Deleting old backup: " + toDelete.getName());
                FileUtils.deleteQuietly(toDelete);
            }
        }
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