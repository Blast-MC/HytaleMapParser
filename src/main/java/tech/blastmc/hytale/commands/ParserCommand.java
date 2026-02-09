package tech.blastmc.hytale.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncPlayerCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;
import com.hypixel.hytale.server.core.permissions.HytalePermissions;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import tech.blastmc.hytale.Runner;
import tech.blastmc.hytale.map.component.MapData;
import tech.blastmc.hytale.modules.loader.Modules;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class ParserCommand extends AbstractCommandCollection {

    public ParserCommand() {
        super("parser", "The core command to interact with the parser");
        addSubCommand(new ParserReloadCommand());
        addSubCommand(new ParserRunCommand());
    }

    private static class ParserReloadCommand extends AbstractAsyncCommand {

        public ParserReloadCommand() {
            super("reload", "Reloads the modules of the parser");
            requirePermission(HytalePermissions.fromCommand("parser.reload"));
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext) {
            Modules.load();
            commandContext.sendMessage(Message.raw("Successfully loaded all modules"));
            return CompletableFuture.completedFuture(null);
        }
    }

    private static class ParserRunCommand extends AbstractAsyncPlayerCommand {

        public ParserRunCommand() {
            super("run", "Runs the parser for the current world");
            requirePermission(HytalePermissions.fromCommand("parser.run"));
        }

        @NonNullDecl
        @Override
        protected CompletableFuture<Void> executeAsync(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world) {
            return new Runner(world).run().whenComplete((i, e) -> {
                if (e == null) {
                    world.execute(() -> {
                        playerRef.sendMessage(Message.raw("Parsed " + i + " datapoints"));
                        MapData data = MapData.of(world);
                        playerRef.sendMessage(Message.raw("Parsed map stored in Universe/parsed/" + data.getType() + "/" + data.getName().toLowerCase().replace(" ", "_")));
                    });
                }
                else {
                    playerRef.sendMessage(Message.raw("There was an error while attempting to parse that world. See console for more details").color(Color.RED));

                    Throwable cause = (e instanceof CompletionException ce && ce.getCause() != null)
                            ? ce.getCause()
                            : e;

                    cause.printStackTrace();
                }
            }).thenAccept(v -> {});
        }
    }

}
