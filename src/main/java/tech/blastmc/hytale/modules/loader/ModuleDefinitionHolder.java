package tech.blastmc.hytale.modules.loader;

import tech.blastmc.hytale.modules.definitions.ModuleDefinition;

import java.util.List;

public class ModuleDefinitionHolder {

    private final List<ModuleDefinition> definition;

    public ModuleDefinitionHolder(List<ModuleDefinition> definition) {
        this.definition = definition;
    }

    public List<ModuleDefinition> getAll() {
        return definition;
    }

    public ModuleDefinition byId(String id) {
        return definition.stream().filter(mod -> mod.id().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

}
