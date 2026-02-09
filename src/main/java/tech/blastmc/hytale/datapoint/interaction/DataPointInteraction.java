package tech.blastmc.hytale.datapoint.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import tech.blastmc.hytale.datapoint.component.DataPoint;
import tech.blastmc.hytale.datapoint.ui.DataPointUI;
import tech.blastmc.hytale.map.component.MapData;
import tech.blastmc.hytale.modules.definitions.ModuleDefinition;
import tech.blastmc.hytale.modules.loader.Modules;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public class DataPointInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<DataPointInteraction> CODEC = BuilderCodec.builder(
            DataPointInteraction.class, DataPointInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@NonNullDecl InteractionType interactionType, @NonNullDecl InteractionContext interactionContext, @NonNullDecl CooldownHandler cooldownHandler) {
        if (interactionType != InteractionType.Use)
            return;

        Ref<EntityStore> ref = interactionContext.getEntity();
        Player player = ref.getStore().getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = ref.getStore().getComponent(ref, PlayerRef.getComponentType());
        World world = player.getWorld();
        BlockPosition pos = interactionContext.getTargetBlock();

        if (world == null || playerRef == null || pos == null)
            return;

        MapData mapData = MapData.of(world);
        if (mapData.getType() == null) {
            player.sendMessage(Message.raw("You must first set a Map Type using /mapdata").color(Color.RED));
            return;
        }

        ModuleDefinition moduleDefinition = Modules.getDefinitions().byId(mapData.getType());
        if (moduleDefinition == null) {
            player.sendMessage(Message.raw("Could not load saved module: " + mapData.getType()).color(Color.RED));
            return;
        }

        if (moduleDefinition.typeDefinitions().isEmpty()) {
            player.sendMessage(Message.raw("The currently set module has no type defintions").color(Color.RED));
            return;
        }

        CompletableFuture.runAsync(() -> {
            CustomUIPage page = player.getPageManager().getCustomPage();
            if (page == null) {
                page = new DataPointUI(playerRef, world, pos);
                player.getPageManager().openCustomPage(ref, ref.getStore(), page);
            }
        }, world);

    }
}
