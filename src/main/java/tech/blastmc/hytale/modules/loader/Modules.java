package tech.blastmc.hytale.modules.loader;

import tech.blastmc.hytale.MapParser;
import tech.blastmc.hytale.modules.definitions.ModuleDefinition;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class Modules {

    private static final Path MODULES_DIR = MapParser.getInstance().getDataDirectory().resolve("modules");

    private static final ModuleLoader moduleLoader = new ModuleLoader(Modules.class.getClassLoader());
    private static ModuleDefinitionHolder definitions;

    public static void load() {
        File file = MODULES_DIR.toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.mkdirs();
        }

        List<ModuleDefinition> definitions = new ArrayList<>();

        try (var stream = Files.list(MODULES_DIR)) {
            for (Path jar : stream.filter(p -> p.toString().endsWith(".jar")).toList()) {
                MapParser.getInstance().getLogger().at(Level.INFO).log("Loading module: " + jar);
                try {
                    definitions.add(moduleLoader.read(jar));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        MapParser.getInstance().getLogger().at(Level.INFO).log("Loaded " + definitions.size() + " modules");
        Modules.definitions = new ModuleDefinitionHolder(definitions);
    }

    public static ModuleDefinitionHolder getDefinitions() {
        return definitions;
    }
}
