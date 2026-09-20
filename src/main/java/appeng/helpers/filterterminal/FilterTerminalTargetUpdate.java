package appeng.helpers.filterterminal;

import java.util.List;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.FriendlyByteBuf;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;

import appeng.api.stacks.GenericStack;

/**
 * Incrementally changeable client state for one filter terminal target.
 */
@ApiStatus.Internal
public record FilterTerminalTargetUpdate(
        long serverId,
        List<FilterTerminalSlotInfo> slotInfo,
        Int2ObjectMap<GenericStack> slots,
        Int2LongMap stockedAmounts) {

    public FilterTerminalTargetUpdate {
        slotInfo = List.copyOf(slotInfo);
        slots = Int2ObjectMaps.unmodifiable(new Int2ObjectArrayMap<>(slots));
        stockedAmounts = Int2LongMaps.unmodifiable(new Int2LongArrayMap(stockedAmounts));
    }

    public static FilterTerminalTargetUpdate read(FriendlyByteBuf buffer) {
        var serverId = buffer.readVarLong();
        var slotInfo = FilterTerminalSyncSerialization.readSlotInfo(buffer);
        var slots = FilterTerminalSyncSerialization.readSlots(buffer);
        var stockedAmounts = FilterTerminalSyncSerialization.readStockedAmounts(buffer);
        return new FilterTerminalTargetUpdate(serverId, slotInfo, slots, stockedAmounts);
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarLong(serverId);
        FilterTerminalSyncSerialization.writeSlotInfo(buffer, slotInfo);
        FilterTerminalSyncSerialization.writeSlots(buffer, slots);
        FilterTerminalSyncSerialization.writeStockedAmounts(buffer, stockedAmounts);
    }
}
