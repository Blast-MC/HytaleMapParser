package tech.blastmc.hytale.modules.api;

public abstract class DataPointType {

    public String id() {
        return this.getClass().getSimpleName().toLowerCase();
    }

    public String displayName() {
        String label = this.getClass().getSimpleName().replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2");
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

}
