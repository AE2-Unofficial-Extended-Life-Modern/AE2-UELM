package appeng.client.gui.me.filterterminal;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.GenericStack;

final class FilterTerminalClientState {

    private final Long2ObjectMap<FilterTerminalRecord> records = new Long2ObjectOpenHashMap<>();

    void clear() {
        records.clear();
    }

    FilterTerminalRecord putFull(long inventoryId, int inventorySize, PatternContainerGroup group,
            ResourceKey<Level> dimension, BlockPos pos, @Nullable Direction side,
            boolean supportsAmountEditing, Int2ObjectMap<GenericStack> slots, Int2LongMap stockedAmounts) {
        var record = new FilterTerminalRecord(inventoryId, inventorySize, group, dimension, pos, side,
                supportsAmountEditing);
        records.put(inventoryId, record);
        apply(record, slots, stockedAmounts);
        return record;
    }

    boolean applyIncremental(long inventoryId, Int2ObjectMap<GenericStack> slots, Int2LongMap stockedAmounts) {
        var record = records.get(inventoryId);
        if (record == null) {
            return false;
        }
        apply(record, slots, stockedAmounts);
        return true;
    }

    Collection<FilterTerminalRecord> records() {
        return records.values();
    }

    @Nullable
    FilterTerminalRecord get(long inventoryId) {
        return records.get(inventoryId);
    }

    private static void apply(FilterTerminalRecord record, Int2ObjectMap<GenericStack> slots,
            Int2LongMap stockedAmounts) {
        for (var entry : slots.int2ObjectEntrySet()) {
            record.getInventory().setStack(entry.getIntKey(), entry.getValue());
        }
        for (var entry : stockedAmounts.int2LongEntrySet()) {
            record.setStockedAmount(entry.getIntKey(), entry.getLongValue());
        }
    }
}
