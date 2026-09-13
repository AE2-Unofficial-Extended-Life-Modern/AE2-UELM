/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2026, TeamAppliedEnergistics, All rights reserved.
 */

package appeng.block.networking;

import net.minecraft.util.StringRepresentable;

import appeng.api.util.AEColor;

/**
 * Blockstate representation of the colorable controller's persisted {@link AEColor}.
 *
 * <p>
 * AEColor is an API enum and is not a StringRepresentable blockstate value.
 * </p>
 */
public enum ControllerColor implements StringRepresentable {
    WHITE("white", AEColor.WHITE),
    ORANGE("orange", AEColor.ORANGE),
    MAGENTA("magenta", AEColor.MAGENTA),
    LIGHT_BLUE("light_blue", AEColor.LIGHT_BLUE),
    YELLOW("yellow", AEColor.YELLOW),
    LIME("lime", AEColor.LIME),
    PINK("pink", AEColor.PINK),
    GRAY("gray", AEColor.GRAY),
    LIGHT_GRAY("light_gray", AEColor.LIGHT_GRAY),
    CYAN("cyan", AEColor.CYAN),
    PURPLE("purple", AEColor.PURPLE),
    BLUE("blue", AEColor.BLUE),
    BROWN("brown", AEColor.BROWN),
    GREEN("green", AEColor.GREEN),
    RED("red", AEColor.RED),
    BLACK("black", AEColor.BLACK),
    TRANSPARENT("transparent", AEColor.TRANSPARENT);

    private final String serializedName;
    private final AEColor color;

    ControllerColor(String serializedName, AEColor color) {
        this.serializedName = serializedName;
        this.color = color;
    }

    @Override
    public String getSerializedName() {
        return this.serializedName;
    }

    public AEColor getColor() {
        return this.color;
    }

    public static ControllerColor fromAEColor(AEColor color) {
        for (var value : values()) {
            if (value.color == color) {
                return value;
            }
        }
        return TRANSPARENT;
    }
}
