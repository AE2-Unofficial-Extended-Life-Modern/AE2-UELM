package appeng.core.sync.packets;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.client.gui.me.filterterminal.FilterTerminalScreen;
import appeng.core.sync.BasePacket;
import appeng.helpers.filterterminal.FilterTerminalTargetState;
import appeng.helpers.filterterminal.FilterTerminalTargetUpdate;

public class FilterTerminalPacket extends BasePacket {

    private final FilterTerminalPacketData data;

    public FilterTerminalPacket(FriendlyByteBuf stream) {
        data = FilterTerminalPacketData.read(stream);
    }

    private FilterTerminalPacket(FilterTerminalPacketData data) {
        this.data = data;
        var buffer = new FriendlyByteBuf(Unpooled.buffer(2048));
        buffer.writeInt(getPacketID());
        data.write(buffer);
        configureWrite(buffer);
    }

    public static FilterTerminalPacket fullUpdate(FilterTerminalTargetState state) {
        return new FilterTerminalPacket(new FilterTerminalPacketData.FullUpdate(state));
    }

    public static FilterTerminalPacket incrementalUpdate(FilterTerminalTargetUpdate update) {
        return new FilterTerminalPacket(new FilterTerminalPacketData.IncrementalUpdate(update));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientPacketData(Player player) {
        if (!(Minecraft.getInstance().screen instanceof FilterTerminalScreen screen)) {
            return;
        }

        if (data instanceof FilterTerminalPacketData.FullUpdate fullUpdate) {
            screen.postFullUpdate(fullUpdate.state());
        } else if (data instanceof FilterTerminalPacketData.IncrementalUpdate incrementalUpdate) {
            screen.postIncrementalUpdate(incrementalUpdate.update());
        }
    }

    private sealed interface FilterTerminalPacketData
            permits FilterTerminalPacketData.FullUpdate, FilterTerminalPacketData.IncrementalUpdate {

        static FilterTerminalPacketData read(FriendlyByteBuf buffer) {
            return buffer.readBoolean()
                    ? new FullUpdate(FilterTerminalTargetState.read(buffer))
                    : new IncrementalUpdate(FilterTerminalTargetUpdate.read(buffer));
        }

        void write(FriendlyByteBuf buffer);

        record IncrementalUpdate(FilterTerminalTargetUpdate update) implements FilterTerminalPacketData {

            @Override
            public void write(FriendlyByteBuf buffer) {
                buffer.writeBoolean(false);
                update.write(buffer);
            }
        }

        record FullUpdate(FilterTerminalTargetState state) implements FilterTerminalPacketData {

            @Override
            public void write(FriendlyByteBuf buffer) {
                buffer.writeBoolean(true);
                state.write(buffer);
            }
        }
    }

}
