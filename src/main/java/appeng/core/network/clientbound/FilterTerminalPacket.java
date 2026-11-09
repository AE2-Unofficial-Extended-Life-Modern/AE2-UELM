package appeng.core.network.clientbound;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import appeng.client.gui.me.filterterminal.FilterTerminalScreen;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;
import appeng.helpers.filterterminal.FilterTerminalTargetState;
import appeng.helpers.filterterminal.FilterTerminalTargetUpdate;

/**
 * Sends the content for a single filter terminal target shown in the filter terminal to the client.
 */
public record FilterTerminalPacket(FilterTerminalPacketData data) implements ClientboundPacket {

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
        return new FilterTerminalPacket(FilterTerminalPacketData.read(stream));
    }

    public void write(RegistryFriendlyByteBuf stream) {
        data.write(stream);
    }

    public static FilterTerminalPacket fullUpdate(FilterTerminalTargetState state) {
        return new FilterTerminalPacket(new FilterTerminalPacketData.FullUpdate(state));
    }

    public static FilterTerminalPacket incrementalUpdate(FilterTerminalTargetUpdate update) {
        return new FilterTerminalPacket(new FilterTerminalPacketData.IncrementalUpdate(update));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof FilterTerminalScreen screen)) {
            return;
        }

        if (data instanceof FilterTerminalPacketData.FullUpdate fullUpdate) {
            screen.postFullUpdate(fullUpdate.state());
        } else if (data instanceof FilterTerminalPacketData.IncrementalUpdate incrementalUpdate) {
            screen.postIncrementalUpdate(incrementalUpdate.update());
        }
    }

    public sealed interface FilterTerminalPacketData
            permits FilterTerminalPacketData.FullUpdate, FilterTerminalPacketData.IncrementalUpdate {

        static FilterTerminalPacketData read(RegistryFriendlyByteBuf buffer) {
            return buffer.readBoolean()
                    ? new FullUpdate(FilterTerminalTargetState.STREAM_CODEC.decode(buffer))
                    : new IncrementalUpdate(FilterTerminalTargetUpdate.STREAM_CODEC.decode(buffer));
        }

        void write(RegistryFriendlyByteBuf buffer);

        record IncrementalUpdate(FilterTerminalTargetUpdate update) implements FilterTerminalPacketData {

            @Override
            public void write(RegistryFriendlyByteBuf buffer) {
                buffer.writeBoolean(false);
                FilterTerminalTargetUpdate.STREAM_CODEC.encode(buffer, update);
            }
        }

        record FullUpdate(FilterTerminalTargetState state) implements FilterTerminalPacketData {

            @Override
            public void write(RegistryFriendlyByteBuf buffer) {
                buffer.writeBoolean(true);
                FilterTerminalTargetState.STREAM_CODEC.encode(buffer, state);
            }
        }
    }
}
