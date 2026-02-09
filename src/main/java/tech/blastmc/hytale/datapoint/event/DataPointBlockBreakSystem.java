package tech.blastmc.hytale.datapoint.event;

import com.hypixel.hytale.component.Archetype;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;
import tech.blastmc.hytale.map.component.MapData;

public class DataPointBlockBreakSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {

    public DataPointBlockBreakSystem() {
        super(BreakBlockEvent.class);
    }

    @Override
    public void handle(int i, @NonNullDecl ArchetypeChunk<EntityStore> chunk, @NonNullDecl Store<EntityStore> store, @NonNullDecl CommandBuffer<EntityStore> commandBuffer, @NonNullDecl BreakBlockEvent event) {
        if (!event.getBlockType().getId().startsWith("Datapoint_Block")) return;

        World world = store.getExternalData().getWorld();
        MapData mapData = MapData.of(world);

        mapData.getDatapointPositions().remove(event.getTargetBlock());
        mapData.save(world);
    }

    @NullableDecl
    @Override
    public Query<EntityStore> getQuery() {
        return Archetype.empty();
    }
}
