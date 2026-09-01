package appeng.core.sync.packets;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import appeng.api.stacks.AEKey;
import appeng.core.sync.BasePacket;
import appeng.helpers.InventoryAction;
import appeng.menu.implementations.FilterTerminalMenu;

public class FilterTerminalActionPacket extends BasePacket {

    private final boolean openAmount;
    @Nullable
    private final InventoryAction action;
    private final long inventoryId;
    private final int slot;
    @Nullable
    private final AEKey expectedKey;

    public FilterTerminalActionPacket(FriendlyByteBuf stream) {
        openAmount = stream.readBoolean();
        action = openAmount ? null : stream.readEnum(InventoryAction.class);
        inventoryId = stream.readVarLong();
        slot = stream.readVarInt();
        expectedKey = AEKey.readOptionalKey(stream);
    }

    public FilterTerminalActionPacket(InventoryAction action, long inventoryId, int slot,
            @Nullable AEKey expectedKey) {
        this(false, action, inventoryId, slot, expectedKey);
    }

    public static FilterTerminalActionPacket openAmount(long inventoryId, int slot, AEKey expectedKey) {
        return new FilterTerminalActionPacket(true, null, inventoryId, slot, expectedKey);
    }

    private FilterTerminalActionPacket(boolean openAmount, @Nullable InventoryAction action,
            long inventoryId, int slot, @Nullable AEKey expectedKey) {
        this.openAmount = openAmount;
        this.action = action;
        this.inventoryId = inventoryId;
        this.slot = slot;
        this.expectedKey = expectedKey;

        var data = new FriendlyByteBuf(Unpooled.buffer());
        data.writeInt(getPacketID());
        data.writeBoolean(openAmount);
        if (!openAmount) {
            data.writeEnum(action);
        }
        data.writeVarLong(inventoryId);
        data.writeVarInt(slot);
        AEKey.writeOptionalKey(data, expectedKey);
        configureWrite(data);
    }

    @Override
    public void serverPacketData(ServerPlayer player) {
        if (!(player.containerMenu instanceof FilterTerminalMenu menu)) {
            return;
        }

        if (openAmount) {
            menu.openSetAmountMenu(inventoryId, slot, expectedKey);
        } else {
            menu.doRemoteAction(action, inventoryId, slot, expectedKey);
        }
    }
}
