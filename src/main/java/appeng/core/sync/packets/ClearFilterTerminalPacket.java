package appeng.core.sync.packets;

import io.netty.buffer.Unpooled;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.client.gui.me.filterterminal.FilterTerminalScreen;
import appeng.core.sync.BasePacket;

public class ClearFilterTerminalPacket extends BasePacket {

    public ClearFilterTerminalPacket(FriendlyByteBuf stream) {
    }

    public ClearFilterTerminalPacket() {
        var data = new FriendlyByteBuf(Unpooled.buffer());
        data.writeInt(getPacketID());
        configureWrite(data);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientPacketData(Player player) {
        if (Minecraft.getInstance().screen instanceof FilterTerminalScreen screen) {
            screen.clear();
        }
    }
}
