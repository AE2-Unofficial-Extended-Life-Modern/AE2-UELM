package appeng.core.sync.packets;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.AEKey;
import appeng.core.sync.BasePacket;
import appeng.menu.implementations.FilterTerminalMenu;

public class FilterTerminalSetFilterPacket extends BasePacket {

    private final long inventoryId;
    private final int slot;
    private final ItemStack stack;
    @Nullable
    private final AEKey expectedKey;

    public FilterTerminalSetFilterPacket(FriendlyByteBuf stream) {
        inventoryId = stream.readVarLong();
        slot = stream.readVarInt();
        stack = stream.readItem();
        expectedKey = AEKey.readOptionalKey(stream);
    }

    public FilterTerminalSetFilterPacket(long inventoryId, int slot, ItemStack stack, @Nullable AEKey expectedKey) {
        this.inventoryId = inventoryId;
        this.slot = slot;
        this.stack = stack.copy();
        this.expectedKey = expectedKey;

        var data = new FriendlyByteBuf(Unpooled.buffer());
        data.writeInt(getPacketID());
        data.writeVarLong(inventoryId);
        data.writeVarInt(slot);
        data.writeItem(this.stack);
        AEKey.writeOptionalKey(data, expectedKey);
        configureWrite(data);
    }

    @Override
    public void serverPacketData(ServerPlayer player) {
        if (player.containerMenu instanceof FilterTerminalMenu menu) {
            menu.setRemoteFilter(inventoryId, slot, stack, expectedKey);
        }
    }
}
