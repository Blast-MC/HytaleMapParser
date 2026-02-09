package tech.blastmc.hytale;

import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import tech.blastmc.hytale.commands.MapDataCommand;
import tech.blastmc.hytale.commands.ParserCommand;
import tech.blastmc.hytale.datapoint.component.DataPoint;
import tech.blastmc.hytale.datapoint.event.DataPointBlockBreakSystem;
import tech.blastmc.hytale.datapoint.event.DataPointBlockPlaceSystem;
import tech.blastmc.hytale.datapoint.interaction.DataPointInteraction;
import tech.blastmc.hytale.map.component.MapData;
import tech.blastmc.hytale.modules.loader.Modules;

public class MapParser extends JavaPlugin {

    private static MapParser instance;

    public MapParser(@NonNullDecl JavaPluginInit init) {
        super(init);
        MapParser.instance = this;
    }

    public static MapParser getInstance() {
        return instance;
    }

    @Override
    protected void setup() {
        MapData.getComponentType();
        DataPoint.getComponentType();
        MapParser.getInstance().getCodecRegistry(Interaction.CODEC).register("datapoint_interaction", DataPointInteraction.class, DataPointInteraction.CODEC);
        MapParser.getInstance().getEntityStoreRegistry().registerSystem(new DataPointBlockPlaceSystem());
        MapParser.getInstance().getEntityStoreRegistry().registerSystem(new DataPointBlockBreakSystem());

        Modules.load();

        getCommandRegistry().registerCommand(new MapDataCommand());
        getCommandRegistry().registerCommand(new ParserCommand());
    }

}
