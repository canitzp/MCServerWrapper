package de.canitzp.mcserverwrapper.plugins.internal;

import com.google.gson.*;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import de.canitzp.mcserverwrapper.MCServerWrapper;
import de.canitzp.mcserverwrapper.commands.CommandHandler;
import de.canitzp.mcserverwrapper.commands.IWrapperCommand;
import de.canitzp.mcserverwrapper.ign.User;
import de.canitzp.mcserverwrapper.ign.save.Block;
import de.canitzp.mcserverwrapper.ign.save.Dimension;
import de.canitzp.mcserverwrapper.plugins.DefaultPlugin;
import de.canitzp.mcserverwrapper.plugins.event.ServerEvents;
import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;
import io.javalin.http.staticfiles.Location;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.canitzp.mcserverwrapper.commands.CommandHandler.*;

public class WebMap extends DefaultPlugin {
    
    public static final String CALLER = "Webmap";
    
    private Javalin javalin;
    private ConcurrentLinkedDeque<SseClient> connectedClients = new ConcurrentLinkedDeque<>();
    
    public WebMap() {
        super("webmap", "WebMap", "1.0.0");
    }
    
    @Override
    protected void init() {
        Block.reloadBlocksFromFile(this.wrapper);
        
        File tilesFolder = new File(new File(this.wrapper.getSettings().getFile("general.jar_path").getParentFile(), "webmap"), "tiles");
        tilesFolder.mkdirs();
        this.registerEvent(new ServerEvents.Saved(this::reloadWorld));
        this.javalin = Javalin.create(javalinConfig -> {
            javalinConfig.addStaticFiles("/webmap/public");
            javalinConfig.addStaticFiles(tilesFolder.getAbsolutePath(), Location.EXTERNAL);
        });
        this.javalin.start(this.pluginSettings.getInt("general.port"));
        /*this.javalin.sse("/sse", sseClient -> {
            sseClient.sendEvent("connected", "Connection successful");
            this.connectedClients.add(sseClient);
            sseClient.onClose(() -> this.connectedClients.remove(sseClient));
        });*/
        this.javalin.get("/region_files", ctx -> {
            JsonObject answerRoot = new JsonObject();
            if(tilesFolder.exists()){
                JsonArray regionCoords = new JsonArray();
                Pattern fileNamePattern = Pattern.compile("^.*r\\.(?<regionX>-?\\d+)\\.(?<regionZ>-?\\d+)\\.png$");
                FileUtils.listFiles(tilesFolder, new String[]{"png"}, false).forEach(file -> {
                    Matcher m = fileNamePattern.matcher(file.getName());
                    if (m.find()) {
                        JsonObject region = new JsonObject();
                        region.addProperty("regionX", m.group("regionX"));
                        region.addProperty("regionZ", m.group("regionZ"));
                        regionCoords.add(region);
                    }
                });
                answerRoot.add("regions", regionCoords);
            }
            ctx.contentType("application/json");
            ctx.result(answerRoot.toString());
        });
        this.javalin.get("/marker", ctx -> {
            ctx.contentType("application/json");
            File markerFile = new File(new File(this.wrapper.getSettings().getFile("general.jar_path").getParentFile(), "webmap"), "marker.json");
            if(markerFile.exists()){
                ctx.result(FileUtils.readFileToString(markerFile, StandardCharsets.UTF_8));
            }
        });
    }
    
    @Override
    protected void stop() {
        if(this.javalin != null){
            this.javalin.stop();
        }
    }
    
    @Override
    protected void registerCommand(CommandDispatcher<User> dispatcher) {
        dispatcher.register(literal("webmap")
            .then(literal("remap").executes(context -> {
                this.reloadWorld();
                return 0;
            }))
            .then(literal("marker")
                .then(argument("name", StringArgumentType.string())
                    .then(argument("color", StringArgumentType.word())
                        .then(argument("x", IntegerArgumentType.integer())
                            .then(argument("z", IntegerArgumentType.integer())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    String colorString = StringArgumentType.getString(context, "color");
                                    int x = IntegerArgumentType.getInteger(context, "x");
                                    int z = IntegerArgumentType.getInteger(context, "z");
                    
                                    try{
                                        Long.decode("#".concat(colorString));
                                    }catch(NumberFormatException ignored){
                                        context.getSource().tellUser(this.wrapper, "Supplied color '" + colorString + "' is not valid! Should be 'RRGGBBAA' or 'RRGGBB'");
                                        return 1;
                                    }
                    
                                    File markerFile = new File(new File(this.wrapper.getSettings().getFile("general.jar_path").getParentFile(), "webmap"), "marker.json");
                                    JsonArray markers = new JsonArray();
                                    if(markerFile.exists()){
                                        try{
                                            JsonElement jsonElement = JsonParser.parseString(FileUtils.readFileToString(markerFile, StandardCharsets.UTF_8));
                                            if(jsonElement.isJsonArray()){
                                                markers.addAll((JsonArray) jsonElement);
                                            }
                                        }catch(IOException e){
                                            e.printStackTrace();
                                            return -1;
                                        }
                                    }
                                    JsonObject newMarker = new JsonObject();
                                    newMarker.addProperty("name", name);
                                    newMarker.addProperty("color", "#".concat(colorString));
                                    newMarker.addProperty("x", x);
                                    newMarker.addProperty("z", z);
                                    markers.add(newMarker);
    
                                    try(FileWriter fw = new FileWriter(markerFile)){
                                        new GsonBuilder().setPrettyPrinting().create().toJson(markers, fw);
                                    }catch(IOException e){
                                        e.printStackTrace();
                                    }
                    
                                    return 0;
                                }))
                            .executes(context -> {
                                context.getSource().tellUser(this.wrapper, "Missing argument z");
                                return 1;
                            }))
                        .executes(context -> {
                            context.getSource().tellUser(this.wrapper, "Missing arguments x and z");
                            return 1;
                        }))
                    .executes(context -> {
                        context.getSource().tellUser(this.wrapper, "Missing arguments color, x and z");
                        return 1;
                    }))
                .executes(context -> {
                    context.getSource().tellUser(this.wrapper, "Missing arguments name, color, x and z");
                    return 1;
                }))
            .executes(context -> {
                context.getSource().tellUser(this.wrapper, "Valid commands:");
                context.getSource().tellUser(this.wrapper, " remap - Reloads all the world files and creates new map images. Can take a lot of time!");
                context.getSource().tellUser(this.wrapper, " marker <name> <x> <z> <color as 'RRGGBB' or 'RRGGBBAA'> - Adds a waypoint at the specified location.");
                return 0;
            }));
    }
    
    private void reloadWorld(){
        this.wrapper.getLog().info(CALLER, "Started to reload world files from disk.");
        File rootDir = this.wrapper.getSettings().getFile("general.jar_path").getParentFile();
        Dimension dimension = new Dimension("");
        dimension.readFromDisk(this.wrapper, new File(rootDir, "world"), new File(new File(rootDir, "webmap"), "tiles"));
        this.wrapper.getLog().info(CALLER, "Finished to reload world files from disk.");
    }
    
}
