package appeng.core.network.clientbound;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.me.filterterminal.FilterTerminalScreen;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;

/**
 * Sends the content for a single filter configuration host shown in the filter terminal to the client.
 */
public record FilterTerminalPacket(
        long inventoryId,
        boolean fullUpdate,
        int inventorySize, // Only valid if fullUpdate
        @Nullable PatternContainerGroup group, // Only valid if fullUpdate
        @Nullable ResourceKey<Level> dimension, // Only valid if fullUpdate
        @Nullable BlockPos pos, // Only valid if fullUpdate
        @Nullable Direction side, // Only valid if fullUpdate
        boolean supportsAmountEditing, // Only valid if fullUpdate
        Int2ObjectMap<GenericStack> slots,
        Int2LongMap stockedAmounts) implements ClientboundPacket {

    public static final StreamCodec<RegistryFriendlyByteBuf, FilterTerminalPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    FilterTerminalPacket::write,
                    FilterTerminalPacket::decode);

    public static final Type<FilterTerminalPacket> TYPE = CustomAppEngPayload.createType("filter_terminal");

    @Override
    public Type<FilterTerminalPacket> type() {
        return TYPE;
    }

    public static FilterTerminalPacket decode(RegistryFriendlyByteBuf stream) {
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
            slots.put(stream.readVarInt(), GenericStack.STREAM_CODEC.decode(stream));
        }

        var stockedAmountCount = stream.readVarInt();
        Int2LongMap stockedAmounts = new Int2LongArrayMap(stockedAmountCount);
        for (var i = 0; i < stockedAmountCount; i++) {
            stockedAmounts.put(stream.readVarInt(), stream.readVarLong());
        }

        return new FilterTerminalPacket(inventoryId, fullUpdate, inventorySize, group, dimension, pos, side,
                supportsAmountEditing, slots, stockedAmounts);
    }

    public void write(RegistryFriendlyByteBuf stream) {
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
            GenericStack.STREAM_CODEC.encode(stream, entry.getValue());
        }

        stream.writeVarInt(stockedAmounts.size());
        for (var entry : stockedAmounts.int2LongEntrySet()) {
            stream.writeVarInt(entry.getIntKey());
            stream.writeVarLong(entry.getLongValue());
        }
    }

    public static FilterTerminalPacket fullUpdate(long inventoryId, int inventorySize,
            PatternContainerGroup group, ResourceKey<Level> dimension,
            BlockPos pos, @Nullable Direction side,
            boolean supportsAmountEditing, Int2ObjectMap<GenericStack> slots, Int2LongMap stockedAmounts) {
        return new FilterTerminalPacket(inventoryId, true, inventorySize, group, dimension, pos, side,
                supportsAmountEditing, slots, stockedAmounts);
    }

    public static FilterTerminalPacket incrementalUpdate(long inventoryId,
            Int2ObjectMap<GenericStack> slots, Int2LongMap stockedAmounts) {
        return new FilterTerminalPacket(inventoryId, false, 0, null, null, null, null, false, slots,
                stockedAmounts);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof FilterTerminalScreen screen)) {
            return;
        }

        if (fullUpdate) {
            screen.postFullUpdate(inventoryId, inventorySize, group, dimension, pos, side, supportsAmountEditing,
                    slots, stockedAmounts);
        } else {
            screen.postIncrementalUpdate(inventoryId, slots, stockedAmounts);
        }
    }
}
