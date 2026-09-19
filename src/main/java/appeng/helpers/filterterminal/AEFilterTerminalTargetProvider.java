package appeng.helpers.filterterminal;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.filterterminal.FilterTerminalTargetMetadata;
import appeng.api.filterterminal.IFilterTerminalConfigView;
import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.filterterminal.IFilterTerminalTargetProvider;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.core.definitions.AEItems;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.parts.AEBasePart;
import appeng.parts.automation.FormationPlanePart;
import appeng.parts.automation.IOBusPart;
import appeng.parts.automation.StorageLevelEmitterPart;
import appeng.parts.storagebus.StorageBusPart;

/**
 * Adapts the native AE2 configuration hosts supported by the filter terminal.
 */
public final class AEFilterTerminalTargetProvider implements IFilterTerminalTargetProvider<IConfigInvHost> {

    public static final AEFilterTerminalTargetProvider INSTANCE = new AEFilterTerminalTargetProvider();

    private AEFilterTerminalTargetProvider() {
    }

    @Override
    public Class<IConfigInvHost> getTargetType() {
        return IConfigInvHost.class;
    }

    @Nullable
    @Override
    public IFilterTerminalTarget getTarget(IConfigInvHost host) {
        if (!(host instanceof IActionHost actionHost)) {
            return null;
        }

        var gridNode = actionHost.getActionableNode();
        if (gridNode == null) {
            return null;
        }

        if (host instanceof InterfaceLogicHost interfaceHost) {
            return createInterfaceTarget(host, interfaceHost, gridNode);
        }

        if (!(host instanceof AEBasePart part) || !isSupportedPart(part)) {
            return null;
        }

        return createPartTarget(host, part, gridNode);
    }

    @Nullable
    private static IFilterTerminalTarget createInterfaceTarget(IConfigInvHost identity,
            InterfaceLogicHost interfaceHost, IGridNode gridNode) {
        var icon = interfaceHost.getMainMenuIcon();
        var name = interfaceHost instanceof Nameable nameable ? nameable.getDisplayName() : icon.getHoverName();
        var metadata = createMetadata(interfaceHost.getBlockEntity(), interfaceHost, icon, name);
        if (metadata == null) {
            return null;
        }

        var config = interfaceHost.getConfig();
        var view = new ConfigView(config, interfaceHost.getStorage(), config.size(), true);
        return new Target(identity, gridNode, metadata, view);
    }

    @Nullable
    private static IFilterTerminalTarget createPartTarget(IConfigInvHost identity, AEBasePart part,
            IGridNode gridNode) {
        var metadata = createMetadata(part.getBlockEntity(), part, new ItemStack(part.getPartItem()),
                part.getDisplayName());
        if (metadata == null) {
            return null;
        }

        var config = identity.getConfig();
        var view = new ConfigView(config, null, getUsableSlotCount(identity), false);
        return new Target(identity, gridNode, metadata, view);
    }

    private static boolean isSupportedPart(AEBasePart part) {
        return part instanceof IOBusPart
                || part instanceof StorageBusPart
                || part instanceof FormationPlanePart
                || part instanceof StorageLevelEmitterPart;
    }

    private static int getUsableSlotCount(IConfigInvHost host) {
        var configSize = host.getConfig().size();
        if (host instanceof StorageLevelEmitterPart) {
            return Math.min(1, configSize);
        }
        if (host instanceof IUpgradeableObject upgradeable) {
            var capacityCards = upgradeable.getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD);
            return Math.min(18 + capacityCards * 9, configSize);
        }
        return 0;
    }

    @Nullable
    private static FilterTerminalTargetMetadata createMetadata(BlockEntity blockEntity, Object host,
            ItemStack iconStack, Component name) {
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return null;
        }

        var group = new PatternContainerGroup(AEItemKey.of(iconStack), name, List.of());
        var side = findPartSide(host, blockEntity);
        return new FilterTerminalTargetMetadata(group, blockEntity.getLevel().dimension(),
                blockEntity.getBlockPos(), side);
    }

    @Nullable
    private static Direction findPartSide(Object host, BlockEntity blockEntity) {
        if (host instanceof IPart part && blockEntity instanceof IPartHost partHost) {
            for (var side : Direction.values()) {
                if (partHost.getPart(side) == part) {
                    return side;
                }
            }
        }
        return null;
    }

    private record Target(Object identity, IGridNode gridNode, FilterTerminalTargetMetadata metadata,
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

    private record ConfigView(
            GenericStackInv config, @Nullable GenericStackInv stock,
            int size, boolean amountEditable) implements IFilterTerminalConfigView {

        @Nullable
        @Override
        public GenericStack getConfig(int slot) {
            return config.getStack(slot);
        }

        @Nullable
        @Override
        public GenericStack getStock(int slot) {
            if (stock == null) {
                return null;
            }

            var configured = getConfig(slot);
            var stocked = stock.getStack(slot);
            return configured != null && stocked != null && configured.what().equals(stocked.what())
                    ? stocked
                    : null;
        }

        @Override
        public boolean canSetConfig(int slot, @Nullable GenericStack stack) {
            return isValidSlot(slot) && config.isAllowed(stack);
        }

        @Override
        public void setConfig(int slot, @Nullable GenericStack stack) {
            if (canSetConfig(slot, stack)) {
                config.setStack(slot, stack);
            }
        }

        @Override
        public boolean canEditAmount(int slot) {
            return amountEditable && isValidSlot(slot);
        }

        @Override
        public long getMaxAmount(int slot, AEKey key) {
            return config.getMaxAmount(key);
        }

        private boolean isValidSlot(int slot) {
            return slot >= 0 && slot < size;
        }
    }
}
