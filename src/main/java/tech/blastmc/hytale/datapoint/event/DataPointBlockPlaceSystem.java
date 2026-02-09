package tech.blastmc.hytale.datapoint.event;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import tech.blastmc.hytale.datapoint.component.DataPoint;
import tech.blastmc.hytale.map.component.MapData;

public class DataPointBlockPlaceSystem extends EntityEventSystem<EntityStore, PlaceBlockEvent> {

    public DataPointBlockPlaceSystem() {
        super(PlaceBlockEvent.class);
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl PlaceBlockEvent event) {
        if (event.getItemInHand() == null) return;
        if (event.getItemInHand().getBlockKey() == null) return;
        if (!event.getItemInHand().getBlockKey().startsWith("Datapoint_Block")) return;

        World world = store.getExternalData().getWorld();
        MapData mapData = MapData.of(world);

        mapData.getDatapointPositions().add(event.getTargetBlock());
        mapData.save(world);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
