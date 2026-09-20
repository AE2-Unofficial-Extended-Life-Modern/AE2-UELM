package appeng.client.gui.me.filterterminal;

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
import appeng.util.ConfigMenuInventory;

/**
 * we use {@link appeng.api.implementations.blockentities.PatternContainerGroup} since it is technically not specific to
 * pattern providers, so we do not need to reinvent the wheel.
 */
public final class FilterTerminalRecord implements Comparable<FilterTerminalRecord> {

    public static final byte CAN_EDIT_CONFIG = 0x1;
    public static final byte CAN_EDIT_AMOUNT = 0x2;

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
    private final byte[] slotPermissions;
    private final byte[][] acceptedKeyTypes;
    private final byte slotsPerRow;

    public FilterTerminalRecord(long serverId, int slots, PatternContainerGroup group,
            ResourceKey<Level> dimension, BlockPos pos, @Nullable Direction side,
            byte[] slotPermissions, byte[][] acceptedKeyTypes, byte slotsPerRow) {
        this.serverId = serverId;
        this.group = group;
        this.searchName = group.name().getString().toLowerCase(Locale.ROOT);
        this.dimension = dimension;
        this.pos = pos;
        this.side = side;
        this.inventory = new ClientInventory(slots);
        this.menuInventory = inventory.createMenuWrapper();
        this.stockedAmounts = new long[slots];
        this.slotPermissions = slotPermissions;
        this.acceptedKeyTypes = acceptedKeyTypes;
        this.slotsPerRow = slotsPerRow;
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

    void setSlotPermissions(byte[] permissions) {
        if (permissions.length != slotPermissions.length) {
            throw new IllegalArgumentException("Expected " + slotPermissions.length + " slot permissions, got "
                    + permissions.length);
        }
        System.arraycopy(permissions, 0, slotPermissions, 0, permissions.length);
    }

    void setAcceptedKeyTypes(byte[][] acceptedKeyTypes) {
        if (acceptedKeyTypes.length != this.acceptedKeyTypes.length) {
            throw new IllegalArgumentException(
                    "Expected " + this.acceptedKeyTypes.length
                            + " accepted-key-type entries, got "
                            + acceptedKeyTypes.length);
        }

        for (var slot = 0; slot < acceptedKeyTypes.length; slot++) {
            this.acceptedKeyTypes[slot] = acceptedKeyTypes[slot].clone();
        }
    }

    public boolean canEditConfig(int slot) {
        return slot >= 0
                && slot < slotPermissions.length
                && (slotPermissions[slot] & CAN_EDIT_CONFIG) != 0;
    }

    public boolean canEditAmount(int slot) {
        return slot >= 0
                && slot < slotPermissions.length
                && (slotPermissions[slot] & CAN_EDIT_AMOUNT) != 0;
    }

    public boolean acceptsKeyType(int slot, AEKeyType keyType) {
        if (slot < 0 || slot >= acceptedKeyTypes.length) {
            return false;
        }

        var rawId = keyType.getRawId();

        for (var accepted : acceptedKeyTypes[slot]) {
            if (accepted == rawId) {
                return true;
            }
        }

        return false;
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
