package appeng.core.network.serverbound;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import appeng.api.stacks.AEKey;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.ServerboundPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.implementations.FilterTerminalMenu;

public record FilterTerminalActionPacket(boolean openAmount,
        @Nullable InventoryAction action,
        long inventoryId,
        int slot,
        @Nullable AEKey expectedKey) implements ServerboundPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterTerminalActionPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    FilterTerminalActionPacket::write,
                    FilterTerminalActionPacket::decode);

    public static final Type<FilterTerminalActionPacket> TYPE = CustomAppEngPayload
            .createType("filter_terminal_action");

    public FilterTerminalActionPacket(InventoryAction action, long inventoryId, int slot,
            @Nullable AEKey expectedKey) {
        this(false, action, inventoryId, slot, expectedKey);
    }

    public static FilterTerminalActionPacket openAmount(long inventoryId, int slot, AEKey expectedKey) {
        return new FilterTerminalActionPacket(true, null, inventoryId, slot, expectedKey);
    }

    @Override
    public Type<FilterTerminalActionPacket> type() {
        return TYPE;
    }

    public static FilterTerminalActionPacket decode(RegistryFriendlyByteBuf stream) {
        var openAmount = stream.readBoolean();
        var action = openAmount ? null : stream.readEnum(InventoryAction.class);
        var inventoryId = stream.readVarLong();
        var slot = stream.readVarInt();
        var expectedKey = AEKey.readOptionalKey(stream);
        return new FilterTerminalActionPacket(openAmount, action, inventoryId, slot, expectedKey);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeBoolean(openAmount);
        if (!openAmount) {
            data.writeEnum(action);
        }
        data.writeVarLong(inventoryId);
        data.writeVarInt(slot);
        AEKey.writeOptionalKey(data, expectedKey);
    }

    @Override
    public void handleOnServer(ServerPlayer player) {
        if (!(player.containerMenu instanceof FilterTerminalMenu menu)) {
            return;
        }

        if (openAmount) {
            menu.openSetAmountMenu(player, inventoryId, slot, expectedKey);
        } else {
            menu.doRemoteAction(player, action, inventoryId, slot, expectedKey);
        }
    }
}
