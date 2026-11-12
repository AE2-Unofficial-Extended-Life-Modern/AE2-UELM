package appeng.me.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.NetworkStorage;
import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class StorageServiceDeltaTest {
    private final AEItemKey stone = AEItemKey.of(Items.STONE);
    private final AEItemKey dirt = AEItemKey.of(Items.DIRT);

    private final ExactStorage exact = new ExactStorage();
    private final List<Map.Entry<AEKey, Long>> changes = new ArrayList<>();
    private StorageService service;

    @BeforeEach
    void setUp() {
        service = new StorageService();
        ((NetworkStorage) service.getInventory()).mount(0, exact);

        var node = mock(IGridNode.class);
        when(node.getService(IStorageWatcherNode.class)).thenReturn(new IStorageWatcherNode() {
            @Override
            public void updateWatcher(IStackWatcher newWatcher) {
                newWatcher.setWatchAll(true);
            }

            @Override
            public void onStackChange(AEKey what, long amount) {
                changes.add(Map.entry(what, amount));
            }
        });
        service.addNode(node, null);

        exact.contents.add(stone, 10);
        tick();
    }

    @Test
    void testExactChangesAreAppliedWithoutListing() {
        var listings = exact.listings;

        insert(stone, 5);
        insert(dirt, 3);
        extract(stone, 2);

        assertThat(tick()).containsExactlyInAnyOrder(Map.entry(stone, 13L), Map.entry(dirt, 3L));
        assertThat(exact.listings).isEqualTo(listings);
        assertThat(service.getCachedInventory().get(stone)).isEqualTo(13);
        assertThat(service.getCachedInventory().get(dirt)).isEqualTo(3);
    }

    @Test
    void testExtractingEverythingRemovesTheKey() {
        extract(stone, 10);

        assertThat(tick()).containsExactly(Map.entry(stone, 0L));
        assertThat(service.getCachedInventory().get(stone)).isZero();
        assertThat(service.getCachedInventory().isEmpty()).isTrue();
    }

    @Test
    void testChangesThatCancelOutAreNotReported() {
        insert(stone, 4);
        extract(stone, 4);

        assertThat(tick()).isEmpty();
    }

    @Test
    void testMismatchFallsBackToAFullRebuild() {
        exact.contents.add(dirt, 6);
        extract(dirt, 2);

        assertThat(tick()).containsExactly(Map.entry(dirt, 4L));
        assertThat(service.getCachedInventory().get(dirt)).isEqualTo(4);
    }

    @Test
    void testChangesToOtherStoragesStillRebuild() {
        var other = new ExactStorage();
        other.exact = false;
        ((NetworkStorage) service.getInventory()).mount(1, other);
        tick();
        var listings = exact.listings;

        insert(dirt, 1);

        assertThat(tick()).containsExactly(Map.entry(dirt, 1L));
        assertThat(exact.listings).isGreaterThan(listings);
    }

    @Test
    void testReadingTheCacheDuringAnInsertDoesNotEmptyIt() {
        var reentrant = new ExactStorage() {
            @Override
            public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
                service.getCachedInventory();
                return super.insert(what, amount, mode, source);
            }
        };
        reentrant.exact = false;
        ((NetworkStorage) service.getInventory()).mount(1, reentrant);
        tick();

        service.invalidateCache();
        insert(dirt, 1);

        assertThat(service.getCachedInventory().get(stone)).isEqualTo(10);
        assertThat(service.getCachedInventory().get(dirt)).isEqualTo(1);
    }

    private void insert(AEKey what, long amount) {
        assertThat(service.getInventory().insert(what, amount, Actionable.MODULATE, IActionSource.empty()))
                .isEqualTo(amount);
    }

    private void extract(AEKey what, long amount) {
        assertThat(service.getInventory().extract(what, amount, Actionable.MODULATE, IActionSource.empty()))
                .isEqualTo(amount);
    }

    private List<Map.Entry<AEKey, Long>> tick() {
        changes.clear();
        service.onServerEndTick();
        return List.copyOf(changes);
    }

    private static class ExactStorage implements MEStorage {
        final KeyCounter contents = new KeyCounter();
        boolean exact = true;
        int listings;

        @Override
        public boolean reportsExactChanges() {
            return exact;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (mode == Actionable.MODULATE) {
                contents.add(what, amount);
            }
            return amount;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            var extracted = Math.min(amount, contents.get(what));
            if (mode == Actionable.MODULATE) {
                contents.remove(what, extracted);
                contents.removeZeros();
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            listings++;
            out.addAll(contents);
        }

        @Override
        public Component getDescription() {
            return Component.empty();
        }
    }
}
