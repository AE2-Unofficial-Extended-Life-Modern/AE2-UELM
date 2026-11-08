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
import appeng.me.storage.MEInventoryHandler;
import appeng.me.storage.NetworkStorage;
import appeng.util.BootstrapMinecraft;
import appeng.util.prioritylist.IPartitionList;

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

    @Test
    void testSubnetChangesReachTheParentAsDeltas() {
        var parent = new StorageService();
        var subnet = new StorageService();
        var parentContents = exactContents(dirt, 2);
        var subnetContents = exactContents(stone, 5);
        mount(parent, parentContents);
        mount(subnet, subnetContents);
        mount(parent, subnet.getCachedView());
        tick(parent, subnet);
        parent.getCachedInventory();
        var parentListings = parentContents.listings;
        var subnetListings = subnetContents.listings;

        subnet.getInventory().insert(stone, 3, Actionable.MODULATE, IActionSource.empty());
        tick(subnet, parent);

        assertThat(parent.getCachedInventory().get(stone)).isEqualTo(8);
        assertThat(parent.getCachedInventory().get(dirt)).isEqualTo(2);
        assertThat(parentContents.listings).isEqualTo(parentListings);
        assertThat(subnetContents.listings).isEqualTo(subnetListings);
    }

    @Test
    void testParentChangesThroughTheViewAreCountedOnce() {
        for (var parentTicksFirst : new boolean[] { true, false }) {
            var parent = new StorageService();
            var subnet = new StorageService();
            var subnetContents = exactContents(stone, 5);
            mount(subnet, subnetContents);
            mount(parent, subnet.getCachedView());
            tick(parent, subnet);
            parent.getCachedInventory();
            var subnetListings = subnetContents.listings;

            parent.getInventory().insert(stone, 2, Actionable.MODULATE, IActionSource.empty());
            if (parentTicksFirst) {
                tick(parent, subnet, parent);
            } else {
                tick(subnet, parent, subnet);
            }

            assertThat(parent.getCachedInventory().get(stone)).isEqualTo(7);
            assertThat(subnet.getCachedInventory().get(stone)).isEqualTo(7);
            assertThat(subnetContents.listings).isEqualTo(subnetListings);
        }
    }

    @Test
    void testFilteredStorageBusOnlyReceivesListedKeys() {
        var parent = new StorageService();
        var subnet = new StorageService();
        mount(subnet, exactContents(stone, 5));
        var bus = new MEInventoryHandler(subnet.getCachedView());
        var filter = IPartitionList.builder();
        filter.add(stone);
        bus.setPartitionList(filter.build());
        bus.setExtractFiltering(true, true);
        mount(parent, bus);
        tick(parent, subnet);
        parent.getCachedInventory();

        subnet.getInventory().insert(dirt, 4, Actionable.MODULATE, IActionSource.empty());
        subnet.getInventory().insert(stone, 1, Actionable.MODULATE, IActionSource.empty());
        tick(subnet, parent);

        assertThat(parent.getCachedInventory().get(stone)).isEqualTo(6);
        assertThat(parent.getCachedInventory().get(dirt)).isZero();
        assertThat(subnet.getCachedInventory().get(dirt)).isEqualTo(4);
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

    private static CounterStorage exactContents(AEKey key, long amount) {
        var storage = contents(key, amount);
        storage.exact = true;
        return storage;
    }

    private static class CounterStorage implements MEStorage {
        final KeyCounter counter = new KeyCounter();
        boolean exact;
        int listings;

        @Override
        public boolean reportsExactChanges() {
            return exact;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (mode == Actionable.MODULATE) {
                counter.add(what, amount);
            }
            return amount;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            listings++;
            out.addAll(counter);
        }

        @Override
        public Component getDescription() {
            return Component.empty();
        }
    }
}
