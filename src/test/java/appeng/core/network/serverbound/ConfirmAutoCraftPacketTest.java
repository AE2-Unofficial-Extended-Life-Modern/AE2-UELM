package appeng.core.network.serverbound;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.network.connection.ConnectionType;

class ConfirmAutoCraftPacketTest {
    @Test
    void amountAboveIntMaxSurvivesTheWire() {
        var buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
        var sent = new ConfirmAutoCraftPacket(Long.MAX_VALUE, true, false);
        ConfirmAutoCraftPacket.STREAM_CODEC.encode(buf, sent);
        assertThat(ConfirmAutoCraftPacket.STREAM_CODEC.decode(buf)).isEqualTo(sent);
    }
}
