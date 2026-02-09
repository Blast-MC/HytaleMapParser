package tech.blastmc.hytale.modules.loader;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import tech.blastmc.hytale.modules.api.DataPointType;
import tech.blastmc.hytale.modules.api.DisplayName;
import tech.blastmc.hytale.modules.api.ParserModule;
import tech.blastmc.hytale.modules.definitions.FieldDefinition;
import tech.blastmc.hytale.modules.definitions.FieldType;
import tech.blastmc.hytale.modules.definitions.ModuleDefinition;
import tech.blastmc.hytale.modules.definitions.TypeDefinition;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModuleLoader {

    private final ClassLoader parent;

    public ModuleLoader(ClassLoader parent) {
        this.parent = parent;
    }

    public ModuleDefinition read(Path jarPath) throws Exception {
        URL jarUrl = jarPath.toUri().toURL();

        try (URLClassLoader cl = new URLClassLoader(new URL[]{jarUrl}, parent);
            ScanResult scan = new ClassGraph()
                    .overrideClassLoaders(cl)
                    .enableClassInfo()
                    .ignoreClassVisibility()
                    .scan()) {

            ParserModule module = instantiateModule(scan);
            List<TypeDefinition> types = readTypes(scan, cl);

            return new ModuleDefinition(module.id(), module.displayName(), List.copyOf(types));

        }
    }

    private ParserModule instantiateModule(ScanResult scan) throws Exception {
        ClassInfoList mods = scan.getClassesImplementing(ParserModule.class.getName());

        if (mods.isEmpty())
            throw new IllegalStateException("No ParserModule implementation found in jar");
        if (mods.size() > 1)
            throw new IllegalStateException("Multiple ParserModule implementations found in jar: " + mods.getNames());

        Class<? extends ParserModule> moduleClass = mods.get(0).loadClass(ParserModule.class);
        Constructor<? extends ParserModule> ctor = moduleClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private List<TypeDefinition> readTypes(ScanResult scan, ClassLoader cl) throws Exception {
        ClassInfoList info = scan.getSubclasses(DataPointType.class.getName());

        List<TypeDefinition> types = new ArrayList<>();

        for (ClassInfo ci : info) {
            if (ci.isAbstract() || ci.isInterface()) continue;

            Class<? extends DataPointType> typeClass = ci.loadClass(DataPointType.class);

            DataPointType type = newInstance(typeClass);
            String displayName = annotatedDisplayNameOr(typeClass, type.displayName());

            List<FieldDefinition> fields = readFields(typeClass);

            types.add(new TypeDefinition(type.id(), displayName, fields));
        }

        return types;
    }

    private List<FieldDefinition> readFields(Class<?> typeClass) {
        List<FieldDefinition> fields = new ArrayList<>();

        for (Field f : typeClass.getDeclaredFields()) {
            int m = f.getModifiers();
            if (Modifier.isStatic(m) || Modifier.isTransient(m)) continue;

            String name = f.getName();

            String label = name.replaceAll("([a-z])([A-Z])", "$1 $2")
                    .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
            label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
            String display = annotatedDisplayNameOr(f, label);

            Class<?> clazz = f.getType();
            FieldType type = FieldType.of(clazz);

            fields.add(new FieldDefinition(name, display, type));
        }

        return fields;
    }

    private <T> T newInstance(Class<T> clazz) throws Exception {
        Constructor<T> ctor = clazz.getDeclaredConstructor();
        ctor.setAccessible(true);
        return ctor.newInstance();
    }

    private String annotatedDisplayNameOr(AnnotatedElement el, String fallback) {
        DisplayName dn = el.getAnnotation(DisplayName.class);
        return (dn != null && !dn.value().isBlank()) ? dn.value() : fallback;
    }

}
