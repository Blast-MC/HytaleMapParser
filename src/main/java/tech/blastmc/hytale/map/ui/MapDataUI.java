package tech.blastmc.hytale.map.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import tech.blastmc.hytale.map.component.MapData;
import tech.blastmc.hytale.map.ui.MapDataUI.MapDataUIData;
import tech.blastmc.hytale.modules.loader.Modules;

public class MapDataUI extends InteractiveCustomUIPage<MapDataUIData> {

    private World world;
    private String name, author, type;
    private MapData mapData;

    public MapDataUI(@NonNullDecl PlayerRef playerRef, World world) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, MapDataUIData.CODEC);
        this.world = world;
        loadMapData();
    }

    private void loadMapData() {
        try {
            this.mapData = MapData.of(this.world);
            this.name = this.mapData.getName();
            this.author = this.mapData.getAuthor();
            this.type = this.mapData.getType();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        uiCommandBuilder.append("MapDataUI.ui");

        var types = Modules.getDefinitions().getAll().stream().map(module -> new DropdownEntryInfo(LocalizableString.fromString(module.displayName()), module.id())).toList();
        uiCommandBuilder.set("#TypeDropdown.Entries", types);
        if (this.type != null)
            uiCommandBuilder.set("#TypeDropdown.Value", this.type);

        if (this.name != null)
            uiCommandBuilder.set("#NameInput.Value", this.name);
        if (this.author != null)
            uiCommandBuilder.set("#AuthorInput.Value", this.author);

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#NameInput", EventData.of("@NameInput", "#NameInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#AuthorInput", EventData.of("@AuthorInput", "#AuthorInput.Value"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TypeDropdown", EventData.of("@TypeInput", "#TypeDropdown.Value"), false);

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Button", "Close"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Button", "Save"), false);
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl MapDataUIData data) {
        if (data.type != null)
            this.type = data.type;

        if (data.name != null)
            this.name = data.name;

        if (data.author != null)
            this.author = data.author;

        if (data.button != null) {
            if (data.button.equalsIgnoreCase("Save")) {
                this.mapData.setName(this.name);
                this.mapData.setAuthor(this.author);
                this.mapData.setType(this.type);
                this.mapData.save(this.world);
            }
            close();
            return;
        }

        sendUpdate();
    }

    public static class MapDataUIData {

        public static final BuilderCodec<MapDataUIData> CODEC = BuilderCodec.builder(MapDataUIData.class, MapDataUIData::new)
                .append(new KeyedCodec<>("@NameInput", Codec.STRING), (data, s) -> data.name = s, data -> data.name).add()
                .append(new KeyedCodec<>("@AuthorInput", Codec.STRING), (data, s) -> data.author = s, data -> data.author).add()
                .append(new KeyedCodec<>("@TypeInput", Codec.STRING), (data, s) -> data.type = s, data -> data.type).add()
                .append(new KeyedCodec<>("Button", Codec.STRING), (data, s) -> data.button = s, data -> data.button).add()
                .build();

        private String name;
        private String author;
        private String type;
        private String button;
    }

}
