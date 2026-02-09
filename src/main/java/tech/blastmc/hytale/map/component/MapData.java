package tech.blastmc.hytale.map.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import tech.blastmc.hytale.MapParser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MapData implements Component<ChunkStore> {

    private static ComponentType<ChunkStore, MapData> componentType;
    public static final BuilderCodec<MapData> CODEC = BuilderCodec.builder(MapData.class, MapData::new)
            .append(new KeyedCodec<>("Name", Codec.STRING), (mapData, s) -> mapData.name = s, mapData -> mapData.name).add()
            .append(new KeyedCodec<>("Author", Codec.STRING), (mapData, s) -> mapData.author = s, mapData -> mapData.author).add()
            .append(new KeyedCodec<>("Type", Codec.STRING), (mapData, s) -> mapData.type = s, mapData -> mapData.type).add()
            .append(
                    new KeyedCodec<>("DataPoints", new ArrayCodec<>(Vector3i.CODEC, Vector3i[]::new)),
                    (worldDataPoints, vector3is) -> worldDataPoints.datapointPositions = new HashSet<>(Arrays.stream(vector3is).toList()),
                    worldDataPoints -> worldDataPoints.datapointPositions.toArray(new Vector3i[0])
            ).add()
            .build();

    public static ComponentType<ChunkStore, MapData> getComponentType() {
        return MapData.componentType;
    }

    public static MapData of(World world) {
        long chunkIndex = ChunkUtil.indexColumn(0, 0);
        Ref<ChunkStore> reference = world.getChunk(chunkIndex).getReference();
        return reference.getStore().ensureAndGetComponent(reference, MapData.getComponentType());
    }

    static {
        MapData.componentType = MapParser.getInstance().getChunkStoreRegistry().registerComponent(MapData.class,"MapData", MapData.CODEC);
    }

    private String name;
    private String author;
    private String type;
    private Set<Vector3i> datapointPositions = new HashSet<>();

    public MapData() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Set<Vector3i> getDatapointPositions() {
        return datapointPositions;
    }

    @NullableDecl
    @Override
    public Component<ChunkStore> clone() {
        MapData clone = new MapData();
        clone.name = this.name;
        clone.author = this.author;
        clone.type = this.type;
        clone.datapointPositions = new HashSet<>(this.datapointPositions);
        return clone;
    }

    public void save(World world) {
        long chunkIndex = ChunkUtil.indexColumn(0, 0);
        world.getChunk(chunkIndex).markNeedsSaving();
    }

}
