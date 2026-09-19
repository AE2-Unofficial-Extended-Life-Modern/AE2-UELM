package appeng.api.filterterminal;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import appeng.api.implementations.blockentities.PatternContainerGroup;

/**
 * Display, grouping, and location information for a filter terminal target.
 *
 * @param group     the group and display information shown by the terminal
 * @param dimension the dimension containing the target
 * @param pos       the target's block position
 * @param side      the cable bus side containing the target, or null for a fullblock target
 */
public record FilterTerminalTargetMetadata(
        PatternContainerGroup group,
        ResourceKey<Level> dimension,
        BlockPos pos,
        @Nullable Direction side) {

    public FilterTerminalTargetMetadata {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(dimension, "dimension");
        pos = Objects.requireNonNull(pos, "pos").immutable();
    }
}
