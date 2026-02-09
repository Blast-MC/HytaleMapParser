package tech.blastmc.hytale.modules.definitions;

import java.util.List;

public record TypeDefinition(String id, String displayName, List<FieldDefinition> fieldDefinitions) { }
