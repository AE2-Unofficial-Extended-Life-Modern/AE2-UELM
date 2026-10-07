package appeng.helpers.filterterminal;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import appeng.api.filterterminal.FilterTerminalTargetMetadata;
import appeng.api.filterterminal.IFilterTerminalConfigView;
import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.filterterminal.IFilterTerminalTargetProvider;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.misc.SuperMEReplenisherBlockEntity;

/**
 * Adapts the Super ME Replenisher's desired and locally stocked stacks for the filter terminal.
 */
public final class SuperMEReplenisherFilterTerminalTargetProvider
        implements IFilterTerminalTargetProvider<SuperMEReplenisherBlockEntity> {

    public static final SuperMEReplenisherFilterTerminalTargetProvider INSTANCE = new SuperMEReplenisherFilterTerminalTargetProvider();

    private SuperMEReplenisherFilterTerminalTargetProvider() {
    }

    @Override
    public Class<SuperMEReplenisherBlockEntity> getTargetType() {
        return SuperMEReplenisherBlockEntity.class;
    }

    @Nullable
    @Override
    public IFilterTerminalTarget getTarget(SuperMEReplenisherBlockEntity host) {
        var level = host.getLevel();
        var gridNode = host.getActionableNode();
        if (level == null || gridNode == null) {
            return null;
        }

        var group = new PatternContainerGroup(AEItemKey.of(host.getMainMenuIcon()), host.getDisplayName(), List.of());
        var metadata = new FilterTerminalTargetMetadata(group, level.dimension(), host.getBlockPos(), null);
        return new Target(host, gridNode, metadata, new ConfigView(host));
    }

    private record Target(
            SuperMEReplenisherBlockEntity identity,
            IGridNode gridNode,
            FilterTerminalTargetMetadata metadata,
            IFilterTerminalConfigView configView) implements IFilterTerminalTarget {

        @Override
        public Object getIdentity() {
            return identity;
        }

        @Override
        public IGridNode getGridNode() {
            return gridNode;
        }

        @Override
        public FilterTerminalTargetMetadata getMetadata() {
            return metadata;
        }

        @Override
        public IFilterTerminalConfigView getConfigView() {
            return configView;
        }
    }

    private record ConfigView(SuperMEReplenisherBlockEntity host) implements IFilterTerminalConfigView {

        @Override
        public int size() {
            return host.getConfig().size();
        }

        @Nullable
        @Override
        public GenericStack getConfig(int slot) {
            return host.getConfig().getStack(slot);
        }

        @Nullable
        @Override
        public GenericStack getStock(int slot) {
            var configured = getConfig(slot);
            if (configured == null) {
                return null;
            }

            var amount = host.getStoredAmount(configured.what());
            return amount > 0 ? new GenericStack(configured.what(), amount) : null;
        }

        @Override
        public boolean canSetConfig(int slot, @Nullable GenericStack stack) {
            var config = host.getConfig();
            if (slot < 0 || slot >= config.size() || !config.isAllowed(stack)) {
                return false;
            }

            if (stack != null) {
                for (var otherSlot = 0; otherSlot < config.size(); otherSlot++) {
                    if (otherSlot != slot && Objects.equals(config.getKey(otherSlot), stack.what())) {
                        return false;
                    }
                }
            }
            return true;
        }

        @Override
        public void setConfig(int slot, @Nullable GenericStack stack) {
            if (canSetConfig(slot, stack)) {
                // Use the Replenisher inventory so duplicate checks and save/client-sync callbacks remain in force.
                host.getConfig().setStack(slot, stack);
            }
        }

        @Override
        public boolean canEditAmount(int slot) {
            return slot >= 0 && slot < size();
        }

        @Override
        public long getMaxAmount(int slot, AEKey key) {
            return host.getConfig().getMaxAmount(key);
        }
    }
}
