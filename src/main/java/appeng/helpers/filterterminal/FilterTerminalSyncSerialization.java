package appeng.helpers.filterterminal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import appeng.api.stacks.GenericStack;

final class FilterTerminalSyncSerialization {

    private FilterTerminalSyncSerialization() {
    }

    static List<FilterTerminalSlotInfo> readSlotInfo(FriendlyByteBuf buffer) {
        var count = buffer.readVarInt();
        var slotInfo = new ArrayList<FilterTerminalSlotInfo>(count);
        for (var i = 0; i < count; i++) {
            slotInfo.add(FilterTerminalSlotInfo.read(buffer));
        }
        return slotInfo;
    }

    static void writeSlotInfo(FriendlyByteBuf buffer, List<FilterTerminalSlotInfo> slotInfo) {
        buffer.writeVarInt(slotInfo.size());
        for (var info : slotInfo) {
            info.write(buffer);
        }
    }

    static Int2ObjectMap<GenericStack> readSlots(FriendlyByteBuf buffer) {
        var count = buffer.readVarInt();
        Int2ObjectMap<GenericStack> slots = new Int2ObjectArrayMap<>(count);
        for (var i = 0; i < count; i++) {
            slots.put(buffer.readVarInt(), GenericStack.readBuffer(buffer));
        }
        return slots;
    }

    static void writeSlots(FriendlyByteBuf buffer, Int2ObjectMap<GenericStack> slots) {
        buffer.writeVarInt(slots.size());
        for (var entry : slots.int2ObjectEntrySet()) {
            buffer.writeVarInt(entry.getIntKey());
            GenericStack.writeBuffer(entry.getValue(), buffer);
        }
    }

    static Int2LongMap readStockedAmounts(FriendlyByteBuf buffer) {
        var count = buffer.readVarInt();
        Int2LongMap stockedAmounts = new Int2LongArrayMap(count);
        for (var i = 0; i < count; i++) {
            stockedAmounts.put(buffer.readVarInt(), buffer.readVarLong());
        }
        return stockedAmounts;
    }

    static void writeStockedAmounts(FriendlyByteBuf buffer, Int2LongMap stockedAmounts) {
        buffer.writeVarInt(stockedAmounts.size());
        for (var entry : stockedAmounts.int2LongEntrySet()) {
            buffer.writeVarInt(entry.getIntKey());
            buffer.writeVarLong(entry.getLongValue());
        }
    }
}
