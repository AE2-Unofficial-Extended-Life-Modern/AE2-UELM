package appeng.client.gui.me.filterterminal;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import appeng.helpers.filterterminal.FilterTerminalTargetState;
import appeng.helpers.filterterminal.FilterTerminalTargetUpdate;

final class FilterTerminalClientState {

    private final Long2ObjectMap<FilterTerminalRecord> records = new Long2ObjectOpenHashMap<>();

    void clear() {
        records.clear();
    }

    FilterTerminalRecord putFull(FilterTerminalTargetState state) {
        var record = new FilterTerminalRecord(state);
        records.put(state.serverId(), record);
        return record;
    }

    boolean applyIncremental(FilterTerminalTargetUpdate update) {
        var record = records.get(update.serverId());
        if (record == null) {
            return false;
        }
        if (!update.slotInfo().isEmpty()) {
            record.setSlotInfo(update.slotInfo());
        }
        apply(record, update);
        return true;
    }

    Collection<FilterTerminalRecord> records() {
        return records.values();
    }

    @Nullable
    FilterTerminalRecord get(long inventoryId) {
        return records.get(inventoryId);
    }

    private static void apply(FilterTerminalRecord record, FilterTerminalTargetUpdate update) {
        for (var entry : update.slots().int2ObjectEntrySet()) {
            record.getInventory().setStack(entry.getIntKey(), entry.getValue());
        }
        for (var entry : update.stockedAmounts().int2LongEntrySet()) {
            record.setStockedAmount(entry.getIntKey(), entry.getLongValue());
        }
    }
}
