package appeng.client.gui.me.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class PinnedKeysStorageTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsGenericKeysAndTimestamps() {
        var itemStack = new ItemStack(Items.DIAMOND_PICKAXE);
        itemStack.setDamageValue(12);
        var pins = List.of(
                new PinnedKeysStorage.StoredPin(AEItemKey.of(itemStack), Instant.ofEpochMilli(1234)),
                new PinnedKeysStorage.StoredPin(AEFluidKey.of(Fluids.WATER), Instant.ofEpochMilli(5678)));
        var file = temporaryDirectory.resolve("pins.dat");

        PinnedKeysStorage.save(file, pins);

        assertThat(PinnedKeysStorage.load(file)).containsExactlyElementsOf(pins);
    }

    @Test
    void usesDeterministicDistinctSafeFileNames() {
        var worldA = PinnedKeysStorage.getFile(temporaryDirectory, "world", "a");
        var worldAAgain = PinnedKeysStorage.getFile(temporaryDirectory, "world", "a");
        var worldB = PinnedKeysStorage.getFile(temporaryDirectory, "world", "b");
        var serverC = PinnedKeysStorage.getFile(temporaryDirectory, "server", "c.example:25565");

        assertThat(worldA).isEqualTo(worldAAgain);
        assertThat(worldA).isNotEqualTo(worldB).isNotEqualTo(serverC);
        assertThat(worldA.getFileName().toString()).matches("world-[0-9a-f]{64}\\.dat");
        assertThat(serverC.getFileName().toString()).matches("server-[0-9a-f]{64}\\.dat");
    }

    @Test
    void deletesFileWhenNoPinsRemain() {
        var file = temporaryDirectory.resolve("pins.dat");
        var pins = List.of(new PinnedKeysStorage.StoredPin(
                AEItemKey.of(Items.DIAMOND), Instant.ofEpochMilli(1234)));
        PinnedKeysStorage.save(file, pins);

        PinnedKeysStorage.save(file, List.of());

        assertThat(file).doesNotExist();
    }

    @Test
    void ignoresCorruptFiles() throws Exception {
        var file = temporaryDirectory.resolve("pins.dat");
        Files.writeString(file, "not nbt");

        assertThat(PinnedKeysStorage.load(file)).isEmpty();
    }
}
