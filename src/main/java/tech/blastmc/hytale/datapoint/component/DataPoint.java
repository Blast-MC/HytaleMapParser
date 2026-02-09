package tech.blastmc.hytale.datapoint.component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import tech.blastmc.hytale.MapParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataPoint implements Component<ChunkStore> {

    private static ComponentType<ChunkStore, DataPoint> componentType;
    private static final BuilderCodec<DataPoint> CODEC = BuilderCodec.builder(DataPoint.class, DataPoint::new)
            .append(new KeyedCodec<>("Type", Codec.STRING), (data, s) -> data.type = s, data -> data.type).add()
            .append(new KeyedCodec<>("Values", Codec.STRING_ARRAY), (data, list) -> data.values = new ArrayList<>(Arrays.asList(list)), data -> data.values.toArray(new String[0])).add()
            .build();

    public static ComponentType<ChunkStore, DataPoint> getComponentType() {
        return componentType;
    }

    public static DataPoint of(World world, Vector3i pos) {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk == null) return null;
        BlockComponentChunk block = chunk.getBlockComponentChunk();
        if (block == null) return null;

        Ref<ChunkStore> ref = block.getEntityReference(ChunkUtil.indexBlockInColumn(pos.x, pos.y, pos.z));
        if (ref == null) return null;

        DataPoint dataPoint = ref.getStore().ensureAndGetComponent(ref, getComponentType());

        dataPoint.world = world;
        dataPoint.position = pos;
        return dataPoint;
    }

    static {
        componentType = MapParser.getInstance().getChunkStoreRegistry().registerComponent(DataPoint.class, "DataPoint", CODEC);
    }

    public DataPoint() {
    }

    private World world;
    private Vector3i position;
    private String type;
    private List<String> values = new ArrayList<>();

    public World getWorld() {
        return world;
    }

    public Vector3i getPosition() {
        return position;
    }

    public List<String> getValues() {
        return values;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @NullableDecl
    @Override
    public Component<ChunkStore> clone() {
        DataPoint clone = new DataPoint();
        clone.type = this.type;
        clone.values = new ArrayList<>(this.values);
        return clone;
    }

    public void save() {
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(position.x, position.z));
        if (chunk == null) return;
        BlockComponentChunk block = chunk.getBlockComponentChunk();
        block.markNeedsSaving();
        chunk.markNeedsSaving();
    }

    @Override
    public String toString() {
        return "DataPoint{" +
                "type='" + type + '\'' +
                ", values=" + String.join(",", values) +
                '}';
    }
}
