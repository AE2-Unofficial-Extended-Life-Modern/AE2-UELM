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

import appeng.api.networking.IGridNode;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.NetworkStorage;
import appeng.util.BootstrapMinecraft;

@BootstrapMinecraft
class StorageServiceTest {
    private final AEItemKey stone = AEItemKey.of(Items.STONE);
    private final AEItemKey dirt = AEItemKey.of(Items.DIRT);
    private final AEItemKey diamond = AEItemKey.of(Items.DIAMOND);
    private final AEItemKey stick = AEItemKey.of(Items.STICK);

    private final KeyCounter contents = new KeyCounter();
    private final List<Map.Entry<AEKey, Long>> changes = new ArrayList<>();
    private StorageService service;

    @BeforeEach
    void setUp() {
        service = new StorageService();
        ((NetworkStorage) service.getInventory()).mount(0, new MEStorage() {
            @Override
            public void getAvailableStacks(KeyCounter out) {
                out.addAll(contents);
            }

            @Override
            public Component getDescription() {
                return Component.empty();
            }
        });

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
    }

    @Test
    void testAddedKeysAreReportedOnce() {
        contents.add(stone, 1);
        contents.add(dirt, 2);

        assertThat(tick()).containsExactlyElementsOf(inCachedOrder(List.of(stone, dirt)));
        assertThat(tick()).isEmpty();
    }

    @Test
    void testChangedKeysAreReportedOnce() {
        contents.add(stone, 1);
        contents.add(dirt, 2);
        contents.add(diamond, 3);
        tick();

        contents.add(stone, 4);
        contents.add(diamond, 1);
        contents.add(stick, 7);

        assertThat(tick()).containsExactlyElementsOf(inCachedOrder(List.of(stone, diamond, stick)));
        assertThat(tick()).isEmpty();
    }

    @Test
    void testRemovedKeysAreReportedOnceAfterChanges() {
        contents.add(stone, 1);
        contents.add(dirt, 2);
        contents.add(diamond, 3);
        tick();

        contents.remove(stone);
        contents.remove(diamond);
        contents.removeZeros();
        contents.add(dirt, 1);
        contents.add(stick, 5);

        var reported = tick();
        assertThat(reported).hasSize(4);
        assertThat(reported.subList(0, 2)).containsExactlyElementsOf(inCachedOrder(List.of(dirt, stick)));
        assertThat(reported.subList(2, 4)).containsExactlyInAnyOrder(Map.entry(stone, 0L), Map.entry(diamond, 0L));
        assertThat(tick()).isEmpty();
    }

    private List<Map.Entry<AEKey, Long>> tick() {
        changes.clear();
        service.onServerEndTick();
        return List.copyOf(changes);
    }

    /**
     * Changes for keys that are still available are reported in the iteration order of the cached inventory.
     */
    private List<Map.Entry<AEKey, Long>> inCachedOrder(List<AEKey> keys) {
        var result = new ArrayList<Map.Entry<AEKey, Long>>();
        for (var entry : service.getCachedInventory()) {
            if (keys.contains(entry.getKey())) {
                result.add(Map.entry(entry.getKey(), entry.getLongValue()));
            }
        }
        return result;
    }
}
