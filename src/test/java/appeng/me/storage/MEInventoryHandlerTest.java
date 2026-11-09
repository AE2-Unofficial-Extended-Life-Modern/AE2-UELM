package appeng.me.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import appeng.api.config.IncludeExclude;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.util.BootstrapMinecraft;
import appeng.util.prioritylist.IPartitionList;

@BootstrapMinecraft
class MEInventoryHandlerTest {
    private final AEItemKey stone = AEItemKey.of(Items.STONE);
    private final AEItemKey dirt = AEItemKey.of(Items.DIRT);
    private MEInventoryHandler handler;

    @BeforeEach
    void setUp() {
        var contents = new KeyCounter();
        contents.add(stone, 10);
        contents.add(dirt, 20);
        handler = new MEInventoryHandler(new MEStorage() {
            @Override
            public void getAvailableStacks(KeyCounter out) {
                out.addAll(contents);
            }

            @Override
            public Component getDescription() {
                return Component.empty();
            }
        });
        handler.setExtractFiltering(true, true);
    }

    @Test
    void testEmptyPartitionListListsEverything() {
        assertThat(handler.getAvailableStacks().get(stone)).isEqualTo(10);
        assertThat(handler.getAvailableStacks().get(dirt)).isEqualTo(20);
    }

    @Test
    void testWhitelistListsOnlyListedKeys() {
        handler.setPartitionList(partitionList(stone));
        handler.setWhitelist(IncludeExclude.WHITELIST);

        var available = handler.getAvailableStacks();
        assertThat(available.get(stone)).isEqualTo(10);
        assertThat(available.get(dirt)).isZero();
    }

    @Test
    void testBlacklistHidesListedKeys() {
        handler.setPartitionList(partitionList(stone));
        handler.setWhitelist(IncludeExclude.BLACKLIST);

        var available = handler.getAvailableStacks();
        assertThat(available.get(stone)).isZero();
        assertThat(available.get(dirt)).isEqualTo(20);
    }

    @Test
    void testNothingListedWithoutExtraction() {
        handler.setAllowExtraction(false);

        assertThat(handler.getAvailableStacks().isEmpty()).isTrue();
    }

    private static IPartitionList partitionList(AEItemKey key) {
        var builder = IPartitionList.builder();
        builder.add(key);
        return builder.build();
    }
}
