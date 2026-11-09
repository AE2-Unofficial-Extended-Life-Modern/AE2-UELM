package appeng.me.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.NetworkStorage;
import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class NestedNetworkStorageTest {
    private final AEItemKey stone = AEItemKey.of(Items.STONE);
    private final AEItemKey dirt = AEItemKey.of(Items.DIRT);

    @Test
    void testParentSeesSubnetContents() {
        var parent = new StorageService();
        var subnet = new StorageService();
        mount(subnet, contents(stone, 5));
        mount(parent, subnet.getCachedView());

        assertThat(parent.getCachedInventory().get(stone)).isEqualTo(5);
    }

    @Test
    void testSubnetChangesReachTheParentWithoutWaitingForTheFullRebuild() {
        var parent = new StorageService();
        var subnet = new StorageService();
        mount(subnet, contents(stone, 5));
        mount(parent, subnet.getCachedView());
        tick(parent, subnet);

        subnet.getInventory().insert(stone, 3, Actionable.MODULATE, IActionSource.empty());
        tick(parent, subnet);

        assertThat(parent.getCachedInventory().get(stone)).isEqualTo(8);
    }

    @Test
    void testInsertThroughTheViewGoesToTheSubnet() {
        var parent = new StorageService();
        var subnet = new StorageService();
        var subnetContents = contents(stone, 5);
        mount(subnet, subnetContents);
        mount(parent, subnet.getCachedView());

        parent.getInventory().insert(stone, 2, Actionable.MODULATE, IActionSource.empty());

        assertThat(subnetContents.counter.get(stone)).isEqualTo(7);
    }

    @Test
    void testNetworksMountingEachOtherCountEachItemOnce() {
        var a = new StorageService();
        var b = new StorageService();
        mount(a, contents(stone, 5));
        mount(b, contents(dirt, 7));
        mount(a, b.getCachedView());
        mount(b, a.getCachedView());

        for (var i = 0; i < 50; i++) {
            a.getInventory().insert(stone, 0, Actionable.MODULATE, IActionSource.empty());
            a.markContentsChanged();
            tick(a, b);
            assertThat(a.getCachedInventory().get(stone)).isEqualTo(5);
            assertThat(a.getCachedInventory().get(dirt)).isEqualTo(7);
            assertThat(b.getCachedInventory().get(stone)).isEqualTo(5);
            assertThat(b.getCachedInventory().get(dirt)).isEqualTo(7);
        }
    }

    private static void tick(StorageService... services) {
        for (var service : services) {
            service.onServerEndTick();
        }
    }

    private static void mount(StorageService service, MEStorage storage) {
        ((NetworkStorage) service.getInventory()).mount(0, storage);
    }

    private static CounterStorage contents(AEKey key, long amount) {
        var storage = new CounterStorage();
        storage.counter.add(key, amount);
        return storage;
    }

    private static class CounterStorage implements MEStorage {
        final KeyCounter counter = new KeyCounter();

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (mode == Actionable.MODULATE) {
                counter.add(what, amount);
            }
            return amount;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            out.addAll(counter);
        }

        @Override
        public Component getDescription() {
            return Component.empty();
        }
    }
}
