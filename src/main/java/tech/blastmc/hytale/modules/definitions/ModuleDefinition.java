package tech.blastmc.hytale.modules.definitions;

import java.util.List;

public record ModuleDefinition(String id, String displayName, List<TypeDefinition> typeDefinitions) {

    public TypeDefinition byId(String id) {
        return typeDefinitions.stream().filter(type -> type.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

}
