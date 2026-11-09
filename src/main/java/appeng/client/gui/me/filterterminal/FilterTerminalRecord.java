package appeng.client.gui.me.filterterminal;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.helpers.filterterminal.FilterTerminalSlotInfo;
import appeng.helpers.filterterminal.FilterTerminalTargetState;
import appeng.util.ConfigMenuInventory;

/**
 * we use {@link appeng.api.implementations.blockentities.PatternContainerGroup} since it is technically not specific to
 * pattern providers, so we do not need to reinvent the wheel.
 */
public final class FilterTerminalRecord implements Comparable<FilterTerminalRecord> {

    private final long serverId;
    private final PatternContainerGroup group;
    private final String searchName;
    private final ResourceKey<Level> dimension;
    private final BlockPos pos;
    @Nullable
    private final Direction side;
    private final GenericStackInv inventory;
    private final ConfigMenuInventory menuInventory;
    private final long[] stockedAmounts;
    private List<FilterTerminalSlotInfo> slotInfo;
    private final byte slotsPerRow;

    public FilterTerminalRecord(FilterTerminalTargetState state) {
        var metadata = state.metadata();
        this.serverId = state.serverId();
        this.group = metadata.group();
        this.searchName = group.name().getString().toLowerCase(Locale.ROOT);
        this.dimension = metadata.dimension();
        this.pos = metadata.pos();
        this.side = metadata.side();
        this.inventory = new ClientInventory(state.inventorySize());
        this.menuInventory = inventory.createMenuWrapper();
        this.stockedAmounts = new long[state.inventorySize()];
        this.slotInfo = state.slotInfo();
        this.slotsPerRow = state.slotsPerRow();

        for (var entry : state.slots().int2ObjectEntrySet()) {
            inventory.setStack(entry.getIntKey(), entry.getValue());
        }
        for (var entry : state.stockedAmounts().int2LongEntrySet()) {
            stockedAmounts[entry.getIntKey()] = entry.getLongValue();
        }
    }

    public long getServerId() {
        return serverId;
    }

    public PatternContainerGroup getGroup() {
        return group;
    }

    public String getSearchName() {
        return searchName;
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    public BlockPos getPos() {
        return pos;
    }

    @Nullable
    public Direction getSide() {
        return side;
    }

    public GenericStackInv getInventory() {
        return inventory;
    }

    public ConfigMenuInventory getMenuInventory() {
        return menuInventory;
    }

    public long getStockedAmount(int slot) {
        return stockedAmounts[slot];
    }

    void setStockedAmount(int slot, long amount) {
        stockedAmounts[slot] = amount;
    }

    public byte getSlotsPerRow() {
        return slotsPerRow;
    }

    void setSlotInfo(List<FilterTerminalSlotInfo> slotInfo) {
        if (slotInfo.size() != this.slotInfo.size()) {
            throw new IllegalArgumentException("Expected " + this.slotInfo.size() + " slot-info entries, got "
                    + slotInfo.size());
        }
        this.slotInfo = List.copyOf(slotInfo);
    }

    public boolean canEditConfig(int slot) {
        return slot >= 0
                && slot < slotInfo.size()
                && slotInfo.get(slot).canEditConfig();
    }

    public boolean canEditAmount(int slot) {
        return slot >= 0
                && slot < slotInfo.size()
                && slotInfo.get(slot).canEditAmount();
    }

    public boolean acceptsKeyType(int slot, AEKeyType keyType) {
        return slot >= 0
                && slot < slotInfo.size()
                && slotInfo.get(slot).acceptsKeyType(keyType);
    }

    @Override
    public int compareTo(FilterTerminalRecord other) {
        var dimensionComparison = dimension.location().compareTo(other.dimension.location());
        if (dimensionComparison != 0) {
            return dimensionComparison;
        }

        var positionComparison = Long.compare(pos.asLong(), other.pos.asLong());
        if (positionComparison != 0) {
            return positionComparison;
        }

        return Integer.compare(side == null ? -1 : side.ordinal(), other.side == null ? -1 : other.side.ordinal());
    }

    private static final class ClientInventory extends GenericStackInv {
        private ClientInventory(int slots) {
            super(null, Mode.CONFIG_STACKS, slots);
        }

        @Override
        public long getMaxAmount(AEKey key) {
            return Long.MAX_VALUE;
        }
    }
}
