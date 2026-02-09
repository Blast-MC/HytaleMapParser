package tech.blastmc.hytale.modules.definitions;

public enum FieldType {
    STRING(String.class),
    INT(byte.class, short.class, int.class, long.class, Byte.class, Short.class, Integer.class, Long.class),
    DOUBLE(float.class, double.class, Float.class, Double.class, Number.class),
    BOOLEAN(boolean.class, Boolean.class);

    private Class<?>[] classes;

    FieldType(Class<?>... classes) {
        this.classes = classes;
    }

    public static FieldType of(Class<?> clazz) {
        for (FieldType type : values())
            for (Class<?> typeClazz : type.classes)
                if (typeClazz == clazz)
                    return type;
        throw new IllegalArgumentException("Unsupported field type: " + clazz.getSimpleName());
    }
}
