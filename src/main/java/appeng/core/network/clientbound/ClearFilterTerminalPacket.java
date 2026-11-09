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

/**
 * Clears all data from the filter terminal before a full reset.
 */
public record ClearFilterTerminalPacket() implements ClientboundPacket {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClearFilterTerminalPacket> STREAM_CODEC = StreamCodec
            .ofMember(
                    ClearFilterTerminalPacket::write,
                    ClearFilterTerminalPacket::decode);

    public static final Type<ClearFilterTerminalPacket> TYPE = CustomAppEngPayload
            .createType("clear_filter_terminal");

    @Override
    public Type<ClearFilterTerminalPacket> type() {
        return TYPE;
    }

    public static ClearFilterTerminalPacket decode(RegistryFriendlyByteBuf data) {
        return new ClearFilterTerminalPacket();
    }

    public void write(RegistryFriendlyByteBuf data) {
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void handleOnClient(Player player) {
        if (Minecraft.getInstance().screen instanceof FilterTerminalScreen screen) {
            screen.clear();
        }
    }
}
