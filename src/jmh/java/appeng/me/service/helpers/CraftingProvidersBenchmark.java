package appeng.me.service.helpers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import net.minecraft.world.item.Items;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.simulation.helpers.ProcessingPatternBuilder;
import appeng.util.BootstrapMinecraftExtension;

@State(Scope.Thread)
public class CraftingProvidersBenchmark {

    private NetworkCraftingProviders craftingProviders;
    private AEItemKey output;

    @Setup
    public void setup() throws Exception {
        new BootstrapMinecraftExtension().beforeAll(null);
        craftingProviders = new NetworkCraftingProviders();
        output = AEItemKey.of(Items.ANDESITE);
        for (int i = 0; i < 100; i++) {
            var pattern = new ProcessingPatternBuilder(new GenericStack(output, 1)).build();
            var node = mock(IGridNode.class);
            when(node.getService(ICraftingProvider.class)).thenReturn(new Provider(pattern, i % 10));
            craftingProviders.addProvider(node);
        }
    }

    @Benchmark
    public void getCraftingFor_manyProviders(Blackhole bh) {
        bh.consume(craftingProviders.getCraftingFor(output));
    }

    private record Provider(IPatternDetails pattern, int priority) implements ICraftingProvider {
        @Override
        public List<IPatternDetails> getAvailablePatterns() {
            return List.of(pattern);
        }

        @Override
        public int getPatternPriority() {
            return priority;
        }

        @Override
        public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
            return false;
        }

        @Override
        public boolean isBusy() {
            return false;
        }
    }
}
