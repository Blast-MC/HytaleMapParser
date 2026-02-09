package tech.blastmc.hytale.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import tech.blastmc.hytale.map.ui.MapDataUI;

import java.util.concurrent.CompletableFuture;

public class MapDataCommand extends AbstractAsyncPlayerCommand {

    public MapDataCommand() {
        super("mapdata", "Configure the map data for the current world");
    }

    @NonNullDecl
    @Override
    protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
        Player player  = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
        CompletableFuture.runAsync(() -> {
            CustomUIPage page = player.getPageManager().getCustomPage();
            if (page == null) {
                page = new MapDataUI(playerRef, world);
                player.getPageManager().openCustomPage(ref, ref.getStore(), page);
            }
        }, world);
        return CompletableFuture.completedFuture(null);
    }
}
