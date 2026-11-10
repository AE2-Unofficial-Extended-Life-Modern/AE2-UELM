package appeng.core.network.serverbound;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEKey;
import appeng.core.network.CustomAppEngPayload;
import appeng.core.network.ServerboundPacket;
import appeng.menu.implementations.FilterTerminalMenu;

public record FilterTerminalSetFilterPacket(long inventoryId,
        int slot,
        ItemStack stack,
        @Nullable AEKey expectedKey) implements ServerboundPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterTerminalSetFilterPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    FilterTerminalSetFilterPacket::write,
                    FilterTerminalSetFilterPacket::decode);

    public static final Type<FilterTerminalSetFilterPacket> TYPE = CustomAppEngPayload
            .createType("filter_terminal_set_filter");

    public FilterTerminalSetFilterPacket {
        stack = stack.copy();
    }

    @Override
    public Type<FilterTerminalSetFilterPacket> type() {
        return TYPE;
    }

    public static FilterTerminalSetFilterPacket decode(RegistryFriendlyByteBuf stream) {
        var inventoryId = stream.readVarLong();
        var slot = stream.readVarInt();
        var stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(stream);
        var expectedKey = AEKey.readOptionalKey(stream);
        return new FilterTerminalSetFilterPacket(inventoryId, slot, stack, expectedKey);
    }

    public void write(RegistryFriendlyByteBuf data) {
        data.writeVarLong(inventoryId);
        data.writeVarInt(slot);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(data, stack);
        AEKey.writeOptionalKey(data, expectedKey);
    }

    @Override
    public void handleOnServer(ServerPlayer player) {
        if (player.containerMenu instanceof FilterTerminalMenu menu) {
            menu.setRemoteFilter(player, inventoryId, slot, stack, expectedKey);
        }
    }
}
