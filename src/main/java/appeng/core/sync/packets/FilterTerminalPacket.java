package appeng.core.sync.packets;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.filterterminal.FilterTerminalScreen;
import appeng.core.sync.BasePacket;

public class FilterTerminalPacket extends BasePacket {

    private final FilterTerminalPacketData data;

    public FilterTerminalPacket(FriendlyByteBuf stream) {
        data = FilterTerminalPacketData.read(stream);
    }

    private FilterTerminalPacket(FilterTerminalPacketData data) {
        this.data = data;
        var buffer = new FriendlyByteBuf(Unpooled.buffer(2048));
        buffer.writeInt(getPacketID());
        this.data.write(buffer);
        configureWrite(buffer);
    }

    public static FilterTerminalPacket fullUpdate(long inventoryId, int inventorySize,
            PatternContainerGroup group, ResourceKey<Level> dimension,
            BlockPos pos, @Nullable Direction side,
            boolean supportsAmountEditing, Int2ObjectMap<GenericStack> slots, Int2LongMap stockedAmounts) {
        return new FilterTerminalPacket(new FilterTerminalPacketData(inventoryId, true, inventorySize,
                group, dimension, pos, side, supportsAmountEditing, slots, stockedAmounts));
    }

    public static FilterTerminalPacket incrementalUpdate(long inventoryId,
            Int2ObjectMap<GenericStack> slots, Int2LongMap stockedAmounts) {
        return new FilterTerminalPacket(new FilterTerminalPacketData(inventoryId, false, 0,
                null, null, null, null, false, slots, stockedAmounts));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientPacketData(Player player) {
        if (!(Minecraft.getInstance().screen instanceof FilterTerminalScreen screen)) {
            return;
        }

        if (data.fullUpdate()) {
            screen.postFullUpdate(data.inventoryId(), data.inventorySize(), data.group(), data.dimension(), data.pos(),
                    data.side(), data.supportsAmountEditing(), data.slots(), data.stockedAmounts());
        } else {
            screen.postIncrementalUpdate(data.inventoryId(), data.slots(), data.stockedAmounts());
        }
    }

    private record FilterTerminalPacketData(long inventoryId, boolean fullUpdate, int inventorySize,
            @Nullable PatternContainerGroup group, @Nullable ResourceKey<Level> dimension,
            @Nullable BlockPos pos, @Nullable Direction side, boolean supportsAmountEditing,
            Int2ObjectMap<GenericStack> slots,
            Int2LongMap stockedAmounts) {

        static FilterTerminalPacketData read(FriendlyByteBuf stream) {
            var inventoryId = stream.readVarLong();
            var fullUpdate = stream.readBoolean();
            var inventorySize = 0;
            PatternContainerGroup group = null;
            ResourceKey<Level> dimension = null;
            BlockPos pos = null;
            Direction side = null;
            var supportsAmountEditing = false;
            if (fullUpdate) {
                inventorySize = stream.readVarInt();
                group = PatternContainerGroup.readFromPacket(stream);
                dimension = ResourceKey.create(Registries.DIMENSION, stream.readResourceLocation());
                pos = stream.readBlockPos();
                side = stream.readBoolean() ? stream.readEnum(Direction.class) : null;
                supportsAmountEditing = stream.readBoolean();
            }

            var slotCount = stream.readVarInt();
            Int2ObjectMap<GenericStack> slots = new Int2ObjectArrayMap<>(slotCount);
            for (var i = 0; i < slotCount; i++) {
                slots.put(stream.readVarInt(), GenericStack.readBuffer(stream));
            }

            var stockedAmountCount = stream.readVarInt();
            Int2LongMap stockedAmounts = new Int2LongArrayMap(stockedAmountCount);
            for (var i = 0; i < stockedAmountCount; i++) {
                stockedAmounts.put(stream.readVarInt(), stream.readVarLong());
            }

            return new FilterTerminalPacketData(inventoryId, fullUpdate, inventorySize, group, dimension, pos, side,
                    supportsAmountEditing, slots, stockedAmounts);
        }

        void write(FriendlyByteBuf stream) {
            stream.writeVarLong(inventoryId);
            stream.writeBoolean(fullUpdate);
            if (fullUpdate) {
                stream.writeVarInt(inventorySize);
                Objects.requireNonNull(group).writeToPacket(stream);
                stream.writeResourceLocation(Objects.requireNonNull(dimension).location());
                stream.writeBlockPos(Objects.requireNonNull(pos));
                stream.writeBoolean(side != null);
                if (side != null) {
                    stream.writeEnum(side);
                }
                stream.writeBoolean(supportsAmountEditing);
            }

            stream.writeVarInt(slots.size());
            for (var entry : slots.int2ObjectEntrySet()) {
                stream.writeVarInt(entry.getIntKey());
                GenericStack.writeBuffer(entry.getValue(), stream);
            }

            stream.writeVarInt(stockedAmounts.size());
            for (var entry : stockedAmounts.int2LongEntrySet()) {
                stream.writeVarInt(entry.getIntKey());
                stream.writeVarLong(entry.getLongValue());
            }
        }
    }
}
