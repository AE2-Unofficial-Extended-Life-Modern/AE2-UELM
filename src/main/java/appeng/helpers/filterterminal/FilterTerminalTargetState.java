package appeng.helpers.filterterminal;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;

import appeng.api.filterterminal.FilterTerminalTargetMetadata;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.GenericStack;

/**
 * Complete client-relevant state for one filter terminal target.
 */
@ApiStatus.Internal
public record FilterTerminalTargetState(
        long serverId,
        int inventorySize,
        FilterTerminalTargetMetadata metadata,
        List<FilterTerminalSlotInfo> slotInfo,
        byte slotsPerRow,
        Int2ObjectMap<GenericStack> slots,
        Int2LongMap stockedAmounts) {

    public FilterTerminalTargetState {
        metadata = Objects.requireNonNull(metadata, "metadata");
        slotInfo = List.copyOf(slotInfo);
        slots = Int2ObjectMaps.unmodifiable(new Int2ObjectArrayMap<>(slots));
        stockedAmounts = Int2LongMaps.unmodifiable(new Int2LongArrayMap(stockedAmounts));
    }

    public static FilterTerminalTargetState read(FriendlyByteBuf buffer) {
        var serverId = buffer.readVarLong();
        var inventorySize = buffer.readVarInt();
        var metadata = readMetadata(buffer);
        var slotsPerRow = buffer.readByte();
        var slotInfo = FilterTerminalSyncSerialization.readSlotInfo(buffer);
        var slots = FilterTerminalSyncSerialization.readSlots(buffer);
        var stockedAmounts = FilterTerminalSyncSerialization.readStockedAmounts(buffer);
        return new FilterTerminalTargetState(serverId, inventorySize, metadata, slotInfo, slotsPerRow, slots,
                stockedAmounts);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarLong(serverId);
        buffer.writeVarInt(inventorySize);
        writeMetadata(buffer, metadata);
        buffer.writeByte(slotsPerRow);
        FilterTerminalSyncSerialization.writeSlotInfo(buffer, slotInfo);
        FilterTerminalSyncSerialization.writeSlots(buffer, slots);
        FilterTerminalSyncSerialization.writeStockedAmounts(buffer, stockedAmounts);
    }

    private static FilterTerminalTargetMetadata readMetadata(FriendlyByteBuf buffer) {
        var group = PatternContainerGroup.readFromPacket(buffer);
        var dimension = ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation());
        var pos = buffer.readBlockPos();
        var side = buffer.readBoolean() ? buffer.readEnum(Direction.class) : null;
        return new FilterTerminalTargetMetadata(group, dimension, pos, side);
    }

    private static void writeMetadata(FriendlyByteBuf buffer, FilterTerminalTargetMetadata metadata) {
        metadata.group().writeToPacket(buffer);
        buffer.writeResourceLocation(metadata.dimension().location());
        buffer.writeBlockPos(metadata.pos());
        writeNullableSide(buffer, metadata.side());
    }

    private static void writeNullableSide(FriendlyByteBuf buffer, @Nullable Direction side) {
        buffer.writeBoolean(side != null);
        if (side != null) {
            buffer.writeEnum(side);
        }
    }
}
