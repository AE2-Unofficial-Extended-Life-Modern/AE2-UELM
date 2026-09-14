package appeng.api.config;

import java.util.Locale;

import net.minecraft.network.chat.Component;

/**
 * Clientside animation styles available for colorable controllers.
 */
public enum ControllerAnimation {
    RAINBOW("rainbow", "Rainbow"),
    CIRCUIT_TRACE("electronicsalt", "Circuit Trace"),
    WAVE("wave", "Wave"),
    COUNTERFLOW("counterflow", "Counterflow"),
    CORE_RIPPLE("coreripple", "Core Ripple"),
    BREATHING("spacebound", "Breathing"),
    SOFT_INTERFERENCE("softinterference", "Soft Interference"),
    SINGULARITY("singularity", "Singularity");

    private final String textureSuffix;
    private final String englishText;

    ControllerAnimation(String textureSuffix, String englishName) {
        this.textureSuffix = textureSuffix;
        this.englishText = englishName;
    }

    public ControllerAnimation next() {
        var values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public String textureSuffix() {
        return textureSuffix;
    }

    public String getEnglishText() {
        return englishText;
    }

    public String getTranslationKey() {
        return "controller_animation.ae2." + name().toLowerCase(Locale.ROOT);
    }

    public Component translated() {
        return Component.translatable(getTranslationKey());
    }
}
