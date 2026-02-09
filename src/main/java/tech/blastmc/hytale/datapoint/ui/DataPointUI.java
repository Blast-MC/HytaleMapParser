package tech.blastmc.hytale.datapoint.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import tech.blastmc.hytale.datapoint.component.DataPoint;
import tech.blastmc.hytale.datapoint.ui.DataPointUI.CustomUIData;
import tech.blastmc.hytale.map.component.MapData;
import tech.blastmc.hytale.modules.api.DataPointType;
import tech.blastmc.hytale.modules.definitions.FieldDefinition;
import tech.blastmc.hytale.modules.definitions.ModuleDefinition;
import tech.blastmc.hytale.modules.definitions.TypeDefinition;
import tech.blastmc.hytale.modules.loader.Modules;

import java.util.ArrayList;
import java.util.List;

public class DataPointUI extends InteractiveCustomUIPage<CustomUIData> {

    private final BlockPosition blockPosition;
    private final World world;
    private final ModuleDefinition mapDefinition;
    private TypeDefinition typeDefinition;
    private List<String> values;

    public DataPointUI(@NonNullDecl PlayerRef playerRef, World world, BlockPosition blockPosition) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, CustomUIData.CODEC);
        this.world = world;
        this.blockPosition = blockPosition;

        String mapType = MapData.of(world).getType();
        this.mapDefinition = Modules.getDefinitions().byId(mapType);
        if (this.mapDefinition == null)
            throw new RuntimeException("Could not find module definition for " + mapType);

        DataPoint dataPoint = DataPoint.of(world, new Vector3i(blockPosition.x, blockPosition.y, blockPosition.z));
        if (dataPoint.getType() == null)
            this.typeDefinition = mapDefinition.typeDefinitions().stream().findFirst().orElse(null);
        else
            this.typeDefinition = this.mapDefinition.byId(dataPoint.getType());

        this.values = new ArrayList<>() {{
            for (int i = 0; i < typeDefinition.fieldDefinitions().size(); i++)
                add(null);
        }};
        for (int i = 0; i < dataPoint.getValues().size(); i++)
            this.values.set(i, dataPoint.getValues().get(i));
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store) {
        try {
            uiCommandBuilder.append("DataPointUI.ui");

            updateColorButtons(uiCommandBuilder, uiEventBuilder);
            updateTypeDropdown(uiCommandBuilder, uiEventBuilder);
            updateFields(uiCommandBuilder, uiEventBuilder);

            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", EventData.of("Button", "Save"), false);
            uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", EventData.of("Button", "Close"), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateTypeDropdown(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        var types = this.mapDefinition.typeDefinitions().stream().map(type -> new DropdownEntryInfo(LocalizableString.fromString(type.displayName()), type.id())).toList();
        uiCommandBuilder.set("#TypeDropdown.Entries", types);
        uiCommandBuilder.set("#TypeDropdown.Value", this.typeDefinition.id());

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#TypeDropdown", EventData.of("@TypeInput", "#TypeDropdown.Value"), false);
    }

    private void updateFields(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        uiCommandBuilder.clear("#FieldCards");

        for (int i = 0; i < this.typeDefinition.fieldDefinitions().size(); i++) {
            FieldDefinition fieldDefinition = this.typeDefinition.fieldDefinitions().get(i);

            uiCommandBuilder.append("#FieldCards", "DataPointField.ui");
            uiCommandBuilder.set("#FieldCards[%s] #FieldLabel.TextSpans".formatted(i), Message.raw(fieldDefinition.displayName()));

            if (this.values.size() - 1 >= i && this.values.get(i) != null)
                uiCommandBuilder.set("#FieldCards[%s] #FieldInput.Value".formatted(i), this.values.get(i));

            uiEventBuilder.addEventBinding(CustomUIEventBindingType.ValueChanged, "#FieldCards[%s] #FieldInput".formatted(i),
                    EventData.of("@Value", "#FieldCards[%s] #FieldInput.Value".formatted(i))
                    .append("Index", "%s".formatted(i)));
        }
    }

    private void updateColorButtons(UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder) {
        Player player = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
        WorldChunk chunk = player.getWorld().getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z));

        if (chunk == null)
            return;

        BlockType blockType = chunk.getBlockType(blockPosition.x, blockPosition.y, blockPosition.z);
        String color = blockType.getStateForBlock(blockType);

        if (color == null) {
            String type = blockType.getId();
            color = type.substring(type.lastIndexOf("_") + 1);
        }

        uiCommandBuilder.set("#Red.Visible", true);
        uiCommandBuilder.set("#Yellow.Visible", true);
        uiCommandBuilder.set("#Blue.Visible", true);
        uiCommandBuilder.set("#Green.Visible", true);

        uiCommandBuilder.set("#RedActive.Visible", false);
        uiCommandBuilder.set("#YellowActive.Visible", false);
        uiCommandBuilder.set("#BlueActive.Visible", false);
        uiCommandBuilder.set("#GreenActive.Visible", false);

        switch (color) {
            case "Red" -> {
                uiCommandBuilder.set("#Red.Visible", false);
                uiCommandBuilder.set("#RedActive.Visible", true);
            }
            case "Yellow" -> {
                uiCommandBuilder.set("#Yellow.Visible", false);
                uiCommandBuilder.set("#YellowActive.Visible", true);
            }
            case "Blue" -> {
                uiCommandBuilder.set("#Blue.Visible", false);
                uiCommandBuilder.set("#BlueActive.Visible", true);
            }
            case "Green" -> {
                uiCommandBuilder.set("#Green.Visible", false);
                uiCommandBuilder.set("#GreenActive.Visible", true);
            }
        }

        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Red", EventData.of("Button", "Red"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Yellow", EventData.of("Button", "Yellow"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Blue", EventData.of("Button", "Blue"), false);
        uiEventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#Green", EventData.of("Button", "Green"), false);
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl CustomUIData data) {
        if (data.type != null) {
            System.out.println(data.type);
            this.typeDefinition = this.mapDefinition.byId(data.type);
            this.values = new ArrayList<>() {{
                for (int i = 0; i < typeDefinition.fieldDefinitions().size(); i++)
                    add(null);
            }};

            UICommandBuilder commandBuilder = new UICommandBuilder();
            UIEventBuilder eventBuilder = new UIEventBuilder();
            updateTypeDropdown(commandBuilder, eventBuilder);
            updateFields(commandBuilder, eventBuilder);
            sendUpdate(commandBuilder, eventBuilder, false);
            return;
        }

        if (data.value != null && data.index != -1) {
            this.values.set(data.index, data.value);
        }

        if (data.button != null) {
            switch (data.button) {
                case "Red", "Yellow", "Blue", "Green" -> {
                    updateDataPointColor(data.button);
                    UICommandBuilder commandBuilder = new UICommandBuilder();
                    UIEventBuilder eventBuilder = new UIEventBuilder();
                    updateColorButtons(commandBuilder, eventBuilder);
                    sendUpdate(commandBuilder, eventBuilder, false);
                }
                case "Close" -> close();
                case "Save" -> {
                    DataPoint dataPoint = DataPoint.of(world, new Vector3i(blockPosition.x, blockPosition.y, blockPosition.z));
                    dataPoint.setType(this.typeDefinition.id());
                    dataPoint.getValues().clear();
                    dataPoint.getValues().addAll(this.values);
                    dataPoint.save();
                    close();
                }
            }
        }

        sendUpdate();
    }

    public void updateDataPointColor(String color) {
        Player player = playerRef.getReference().getStore().getComponent(playerRef.getReference(), Player.getComponentType());
        WorldChunk chunk = player.getWorld().getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z));

        if (chunk == null)
            return;

        BlockType blockType = chunk.getBlockType(blockPosition.x, blockPosition.y, blockPosition.z);
        String newBlock = blockType.getBlockKeyForState(color);

        int newBlockId = BlockType.getAssetMap().getIndex(newBlock);
        BlockType newBlockAsset = BlockType.getAssetMap().getAsset(newBlockId);

        chunk.setBlock(blockPosition.x, blockPosition.y, blockPosition.z, newBlockId, newBlockAsset, 0, 0, 260);
    }

    public static class CustomUIData {
        public static final BuilderCodec<CustomUIData> CODEC = BuilderCodec.builder(CustomUIData.class, CustomUIData::new)
                .append(new KeyedCodec<>("@TypeInput", Codec.STRING), (data, s) -> data.type = s, data -> data.type).add()
                .append(new KeyedCodec<>("@Value", Codec.STRING), (data, s) -> data.value = s, data -> data.value).add()
                .append(new KeyedCodec<>("Button", Codec.STRING), (data, s) -> data.button = s, data -> data.button).add()
                .append(new KeyedCodec<>("Index", Codec.STRING), (data, s) -> data.index = Integer.parseInt(s), data -> "" + data.index).add()
                .build();

        private String type;
        private String value;
        private String button;
        private int index = -1;
    }

}
