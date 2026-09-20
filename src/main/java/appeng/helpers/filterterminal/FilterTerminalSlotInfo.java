package appeng.helpers.filterterminal;

import java.util.Arrays;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.network.FriendlyByteBuf;

import appeng.api.stacks.AEKeyType;

/**
 * Client-relevant state for one filter terminal slot.
 */
@ApiStatus.Internal
public record FilterTerminalSlotInfo(byte permissions, byte[] acceptedKeyTypes) {

    public static final byte CAN_EDIT_CONFIG = 0x1;
    public static final byte CAN_EDIT_AMOUNT = 0x2;

    public FilterTerminalSlotInfo {
        acceptedKeyTypes = acceptedKeyTypes.clone();
    }

    public static FilterTerminalSlotInfo of(boolean canEditConfig, boolean canEditAmount, byte[] acceptedKeyTypes) {
        byte permissions = 0;
        if (canEditConfig) {
            permissions |= CAN_EDIT_CONFIG;
        }
        if (canEditAmount) {
            permissions |= CAN_EDIT_AMOUNT;
        }
        return new FilterTerminalSlotInfo(permissions, acceptedKeyTypes);
    }

    public static FilterTerminalSlotInfo read(FriendlyByteBuf buffer) {
        return new FilterTerminalSlotInfo(buffer.readByte(), buffer.readByteArray());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeByte(permissions);
        buffer.writeByteArray(acceptedKeyTypes);
    }

    /**
     * Overrides the accessor to prevent modification of the returned array.
     */
    @Override
    public byte[] acceptedKeyTypes() {
        return acceptedKeyTypes.clone();
    }

    public boolean canEditConfig() {
        return (permissions & CAN_EDIT_CONFIG) != 0;
    }

    public boolean canEditAmount() {
        return (permissions & CAN_EDIT_AMOUNT) != 0;
    }

    public boolean acceptsKeyType(AEKeyType keyType) {
        var rawId = keyType.getRawId();
        for (var accepted : acceptedKeyTypes) {
            if (accepted == rawId) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof FilterTerminalSlotInfo that
                        && permissions == that.permissions
                        && Arrays.equals(acceptedKeyTypes, that.acceptedKeyTypes);
    }

    @Override
    public int hashCode() {
        return 31 * Byte.hashCode(permissions) + Arrays.hashCode(acceptedKeyTypes);
    }
}
