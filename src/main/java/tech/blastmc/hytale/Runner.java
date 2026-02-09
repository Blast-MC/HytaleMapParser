package tech.blastmc.hytale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.system.WorldConfigSaveSystem;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkSavingSystems;
import com.hypixel.hytale.server.core.util.io.FileUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.FileUtils;
import org.bson.BsonBinary;
import org.bson.BsonBinarySubType;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.RawBsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import tech.blastmc.hytale.datapoint.component.DataPoint;
import tech.blastmc.hytale.map.component.MapData;
import tech.blastmc.hytale.modules.definitions.FieldDefinition;
import tech.blastmc.hytale.modules.definitions.FieldType;
import tech.blastmc.hytale.modules.definitions.ModuleDefinition;
import tech.blastmc.hytale.modules.definitions.TypeDefinition;
import tech.blastmc.hytale.modules.loader.ModuleDefinitionHolder;
import tech.blastmc.hytale.modules.loader.Modules;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

public class Runner {

    private final World world;

    public Runner(World world) {
        this.world = world;
    }

    public void log(String log) {
        MapParser.getInstance().getLogger().atInfo().log(log);
    }

    public CompletableFuture<Integer> run() {
        log("Starting parse of world " + this.world.getSavePath());
        AtomicInteger points = new AtomicInteger(0);

        try {
            return copyWorld().thenCompose(duplicate -> {
                return processDataPoints(points)
                    .thenCompose(json -> writeJsonFile(duplicate, json))
                    .thenCompose(v -> cleanDuplicate(duplicate))
                    .thenCompose(v -> waitMillis(500))
                    .thenCompose(v -> zipAndMove(duplicate))
                    .thenCompose(v -> deleteWorld(duplicate))
                    .thenApply(v -> {
                        log("Parsing complete!");
                        return points.get();
                    });
            });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<JsonObject> processDataPoints(AtomicInteger points) {
        log("Processing datapoints...");
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        executeOnWorldThread(this.world, () -> {
            JsonObject out = new JsonObject();
            MapData data = MapData.of(this.world);

            JsonObject meta = new JsonObject();
            meta.addProperty("name", data.getName());
            meta.addProperty("author", data.getAuthor());
            meta.addProperty("type", data.getType());
            out.add("meta", meta);

            JsonObject datapoints = new JsonObject();
            ModuleDefinition holder = Modules.getDefinitions().byId(data.getType());

            data.getDatapointPositions().forEach(vec -> {
                try {
                    DataPoint dataPoint = DataPoint.of(this.world, vec);
                    TypeDefinition typeDefinition = holder.byId(dataPoint.getType());

                    JsonObject jsonObject = new JsonObject();
                    for (int i = 0; i < typeDefinition.fieldDefinitions().size(); i++) {
                        FieldDefinition fieldDefinition = typeDefinition.fieldDefinitions().get(i);
                        addTyped(jsonObject, fieldDefinition.name(),
                                dataPoint.getValues().get(i),
                                fieldDefinition.type());
                    }
                    JsonObject finalObj = new JsonObject();
                    finalObj.add("position", toJsonPosition(vec));
                    finalObj.add("data", jsonObject);

                    JsonArray arr = datapoints.has(typeDefinition.id())
                            ? datapoints.getAsJsonArray(typeDefinition.id())
                            : new JsonArray();

                    arr.add(finalObj);
                    datapoints.add(typeDefinition.id(), arr);

                    points.incrementAndGet();
                } catch (Exception e) {
                    log("Could not parse datapoint at position: " + vec);
                    e.printStackTrace();
                }
            });

            out.add("datapoints", datapoints);
            log("Processed " + points.get() + " datapoints successfully");
            future.complete(out);
        });
        return future;
    }

    private CompletableFuture<Void> cleanDuplicate(World duplicate) {
        log("Cleaning duplicate world...");
        AtomicReference<MapData> data = new AtomicReference<>();
        return executeOnWorldThread(this.world, () -> data.set(MapData.of(this.world)))
                .thenCompose(v -> executeOnWorldThread(duplicate, () ->
                        data.get().getDatapointPositions().forEach(vec -> {
                            duplicate.breakBlock(vec.x, vec.y, vec.z, 16);
                        })))
                .thenCompose(v -> CompletableFuture.runAsync(() -> WorldConfigSaveSystem.saveWorldConfigAndResources(duplicate), duplicate))
                .thenCompose(v -> CompletableFuture.runAsync(() -> ChunkSavingSystems.saveChunksInWorld(duplicate.getChunkStore().getStore()), duplicate));
    }

    private CompletableFuture<Void> executeOnWorldThread(World w, Runnable r) {
        CompletableFuture<Void> cf = new CompletableFuture<>();
        w.execute(() -> {
            try {
                r.run();
                cf.complete(null);
            } catch (Throwable t) {
                cf.completeExceptionally(t);
            }
        });
        return cf;
    }

    private CompletableFuture<Void> deleteWorld(World duplicate) {
        return executeOnWorldThread(Universe.get().getDefaultWorld(), () -> {
            try {
                MapParser.getInstance().getLogger().at(Level.INFO).log("Deleting temp world...");
                Universe.get().removeWorld(duplicate.getName());
                MapParser.getInstance().getLogger().at(Level.INFO).log("Removing world files...");
                FileUtil.deleteDirectory(duplicate.getSavePath());
                MapParser.getInstance().getLogger().at(Level.INFO).log("Temp world deleted");
            } catch (Exception e) {
                MapParser.getInstance().getLogger().at(Level.WARNING).log("There was an error while deleting temp world");
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<Void> zipAndMove(World world) {
        Path parsedPath = world.getSavePath().getParent().getParent().resolve("parsed");
        File parsedDir = parsedPath.toFile();

        if (!parsedDir.exists())
            parsedDir.mkdir();

        AtomicReference<MapData> data = new AtomicReference<>();
        return executeOnWorldThread(this.world, () -> data.set(MapData.of(this.world)))
                .thenCompose(v -> executeOnWorldThread(world, () -> {
                    File typeDir = parsedPath.resolve(data.get().getType()).toFile();

                    if (!typeDir.exists())
                        typeDir.mkdir();

                    Path outputTarGz = typeDir.toPath().resolve(data.get().getName().toLowerCase().replace(" ", "_") + ".tar.gz");
                    log("Zipping duplicate world to " + outputTarGz);

                    try (
                            OutputStream fileOut = Files.newOutputStream(outputTarGz);
                            BufferedOutputStream bufferedOut = new BufferedOutputStream(fileOut);
                            GzipCompressorOutputStream gzipOut = new GzipCompressorOutputStream(bufferedOut);
                            TarArchiveOutputStream tarOut = new TarArchiveOutputStream(gzipOut)
                    ) {
                        tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                        Path src = world.getSavePath();
                        Files.walk(src).forEach(path -> {
                            try {
                                if (Files.isDirectory(path)) return;

                                Path relative = src.relativize(path);
                                TarArchiveEntry entry = new TarArchiveEntry(path.toFile(), relative.toString().replace("\\", "/"));

                                tarOut.putArchiveEntry(entry);
                                Files.copy(path, tarOut);
                                tarOut.closeArchiveEntry();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }));
    }

    private JsonElement toJsonPosition(Vector3i vec) {
        JsonObject obj = new JsonObject();
        obj.addProperty("x", vec.x);
        obj.addProperty("y", vec.y);
        obj.addProperty("z", vec.z);
        return obj;
    }

    private void addTyped(JsonObject obj, String key, String raw, FieldType type) {
        String s = raw.trim();
        if (s.isEmpty())
            throw new IllegalArgumentException("Value cannot be null: " + key);

        switch (type) {
            case STRING -> obj.addProperty(key, s);
            case BOOLEAN -> obj.addProperty(key, s.equalsIgnoreCase("true"));
            case INT -> obj.addProperty(key, Long.parseLong(s));
            case DOUBLE -> obj.addProperty(key, Double.parseDouble(s));
        }
    }

    private CompletableFuture<Void> writeJsonFile(World world, JsonObject json) {
        log("Writing data.json");
        Path path = world.getSavePath().resolve("data.json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (Writer writer = Files.newBufferedWriter(path)) {
            gson.toJson(json, writer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<World> copyWorld() throws IOException {
        log("Duplicating world...");
        MapData mapData = MapData.of(this.world);
        Path src = this.world.getSavePath();
        String newMapName = "Parsed-" + mapData.getName().toLowerCase().replace(" ", "_");
        Path dest = this.world.getSavePath().getParent().resolve(newMapName);

        Files.createDirectories(dest);

        try (var stream = Files.walk(src)) {
            stream.forEach(path -> {
                try {
                    Path relative = src.relativize(path);
                    Path target = dest.resolve(relative);

                    if (Files.isDirectory(path))
                        Files.createDirectories(target);
                    else
                        Files.copy(
                                path,
                                target,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES
                        );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        log("Files successfully copied");

        try {
            updateConfigUUID(dest.resolve("config.json"));
            updateConfigUUID(dest.resolve("instance.bson"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return Universe.get().loadWorld(newMapName).whenComplete((w, ex) -> {
            if (ex != null) {
                log("There was an error while duplicating the world:");
                ex.printStackTrace();
            }
            else
                log("World successfully duplicated and loaded");
        });
    }

    private void updateConfigUUID(Path configJson) throws Exception {
        UUID newUuid = UUID.randomUUID();

        JsonObject root = JsonParser.parseString(Files.readString(configJson)).getAsJsonObject();
        JsonObject uuidObj = root.getAsJsonObject("UUID");
        if (uuidObj == null) throw new IllegalStateException("config.json missing UUID field");

        uuidObj.addProperty("$binary", uuidToBsonBase64(newUuid));
        uuidObj.addProperty("$type", "04");

        Files.writeString(configJson, new GsonBuilder().setPrettyPrinting().create().toJson(root));
    }

    private String uuidToBsonBase64(UUID uuid) {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return Base64.getEncoder().encodeToString(bb.array());
    }

    private CompletableFuture<Void> waitMillis(int millis) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        new Thread(() -> {
            try {
                Thread.sleep(millis);
                future.complete(null);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();
        return future;
    }

}
