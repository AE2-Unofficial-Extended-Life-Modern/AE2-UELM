package appeng.menu.implementations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.core.AELog;
import appeng.core.definitions.AEItems;
import appeng.core.sync.packets.ClearFilterTerminalPacket;
import appeng.core.sync.packets.FilterTerminalPacket;
import appeng.helpers.IConfigInvHost;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.InventoryAction;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.menu.AEBaseMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.AEBasePart;
import appeng.parts.automation.FormationPlanePart;
import appeng.parts.automation.IOBusPart;
import appeng.parts.automation.StorageLevelEmitterPart;
import appeng.parts.reporting.FilterTerminalPart;
import appeng.parts.storagebus.StorageBusPart;

/**
 * @see appeng.client.gui.me.filterterminal.FilterTerminalScreen
 */
public class FilterTerminalMenu extends AEBaseMenu {

    public static final MenuType<FilterTerminalMenu> TYPE = MenuTypeBuilder
            .create(FilterTerminalMenu::new, FilterTerminalPart.class)
            .build("filter_terminal");

    private static long inventorySerial = Long.MIN_VALUE;

    private final FilterTerminalPart host;
    private final Map<IConfigInvHost, TargetTracker> trackers = new IdentityHashMap<>();
    private final Long2ObjectOpenHashMap<TargetTracker> byId = new Long2ObjectOpenHashMap<>();

    public FilterTerminalMenu(int id, Inventory playerInventory, FilterTerminalPart host) {
        super(TYPE, id, playerInventory, host);
        this.host = host;
        createPlayerInventorySlots(playerInventory);
    }

    @Override
    public void broadcastChanges() {
        if (isClientSide()) {
            return;
        }

        super.broadcastChanges();

        var grid = getGrid();
        var currentTargets = findTargets(grid);
        var needsFullUpdate = currentTargets.size() != trackers.size();

        if (!needsFullUpdate) {
            for (var currentTarget : currentTargets) {
                var tracker = trackers.get(currentTarget.host);
                if (tracker == null || !tracker.matches(currentTarget)) {
                    needsFullUpdate = true;
                    break;
                }
            }
        }

        if (needsFullUpdate) {
            sendFullUpdate(currentTargets);
        } else {
            sendIncrementalUpdates();
        }
    }

    @Nullable
    private IGrid getGrid() {
        var node = host.getActionableNode();
        return node != null && node.isActive() ? node.getGrid() : null;
    }

    @SuppressWarnings("unchecked")
    private static List<TerminalTarget> findTargets(@Nullable IGrid grid) {
        if (grid == null) {
            return List.of();
        }

        Set<IConfigInvHost> uniqueHosts = Collections.newSetFromMap(new IdentityHashMap<>());
        var result = new ArrayList<TerminalTarget>();
        for (var machineClass : grid.getMachineClasses()) {
            if (!IConfigInvHost.class.isAssignableFrom(machineClass)) {
                continue;
            }

            var hostClass = (Class<? extends IConfigInvHost>) machineClass;
            for (var host : grid.getActiveMachines(hostClass)) {
                if (!uniqueHosts.add(host)) {
                    continue;
                }

                var target = TerminalTarget.from(host);
                if (target != null) {
                    result.add(target);
                }
            }
        }

        result.sort(Comparator.comparing(target -> target.metadata, HostMetadata.COMPARATOR));
        return result;
    }

    private void sendFullUpdate(List<TerminalTarget> currentTargets) {
        var previousTrackers = new IdentityHashMap<>(trackers);
        trackers.clear();
        byId.clear();
        sendPacketToClient(new ClearFilterTerminalPacket());

        for (var currentTarget : currentTargets) {
            var previousTracker = previousTrackers.get(currentTarget.host);
            var tracker = new TargetTracker(currentTarget,
                    previousTracker == null ? inventorySerial++ : previousTracker.serverId);
            trackers.put(currentTarget.host, tracker);
            byId.put(tracker.serverId, tracker);
            sendPacketToClient(tracker.createFullPacket());
        }
    }

    private void sendIncrementalUpdates() {
        for (var tracker : trackers.values()) {
            var packet = tracker.createUpdatePacket();
            if (packet != null) {
                sendPacketToClient(packet);
            }
        }
    }

    @Override
    public void doAction(ServerPlayer player, InventoryAction action, int slot, long id) {
        if (id == 0) {
            super.doAction(player, action, slot, id);
            return;
        }

        refreshTargets();
    }

    public void doRemoteAction(InventoryAction action, long id, int slot, @Nullable AEKey expectedKey) {
        var tracker = getValidTracker(id, slot);
        if (tracker == null
                || !FilterTerminalEditValidation.matchesExpected(tracker.target.config(), slot, expectedKey)) {
            refreshTargets();
            return;
        }

        var fakeSlot = new FakeSlot(tracker.target.config().createMenuWrapper(), slot);
        handleFakeSlotAction(fakeSlot, action);
    }

    public void setRemoteFilter(long id, int slot, ItemStack stack, @Nullable AEKey expectedKey) {
        var tracker = getValidTracker(id, slot);
        if (tracker == null
                || !FilterTerminalEditValidation.matchesExpected(tracker.target.config(), slot, expectedKey)) {
            refreshTargets();
            return;
        }

        FilterTerminalEditValidation.setFilter(tracker.target.config(), slot, stack);
    }

    public void openSetAmountMenu(long id, int slot, @Nullable AEKey expectedKey) {
        var tracker = getValidTracker(id, slot);
        if (tracker == null
                || !FilterTerminalEditValidation.canEditAmount(tracker.target.supportsAmountEditing(),
                        tracker.target.config(), slot, expectedKey)) {
            refreshTargets();
            return;
        }

        var configured = tracker.target.config().getStack(slot);
        if (configured == null) {
            return;
        }

        FilterTerminalSetAmountMenu.open((ServerPlayer) getPlayer(), getLocator(), tracker.target.interfaceHost, slot,
                configured);
    }

    private void refreshTargets() {
        sendFullUpdate(findTargets(getGrid()));
    }

    @Nullable
    private TargetTracker getValidTracker(long id, int slot) {
        var tracker = FilterTerminalEditValidation.findTarget(byId, id);
        if (tracker == null) {
            return null;
        }
        if (slot < 0 || slot >= tracker.target.usableSlots()) {
            AELog.warn("Client refers to invalid filter configuration slot {} of {}", slot, tracker.target.host);
            return null;
        }

        if (!FilterTerminalEditValidation.isValidTarget(getGrid(), tracker.target.actionHost)) {
            return null;
        }

        return tracker;
    }

    private record TargetTracker(long serverId, TerminalTarget target, HostMetadata metadata,
            GenericStack[] lastSent, long[] lastSentStockedAmounts) {

        private TargetTracker(TerminalTarget target, long serverId) {
            this(serverId, target, target.metadata, new GenericStack[target.usableSlots()],
                    new long[target.usableSlots()]);
        }

        private boolean matches(TerminalTarget currentTarget) {
            return target.host == currentTarget.host
                    && lastSent.length == currentTarget.usableSlots()
                    && target.supportsAmountEditing() == currentTarget.supportsAmountEditing()
                    && metadata.equals(currentTarget.metadata);
        }

        private FilterTerminalPacket createFullPacket() {
            Int2ObjectMap<GenericStack> slots = new Int2ObjectArrayMap<>();
            Int2LongMap stockedAmounts = new Int2LongArrayMap();
            for (var i = 0; i < lastSent.length; i++) {
                var stack = target.config().getStack(i);
                lastSent[i] = stack;
                if (stack != null) {
                    slots.put(i, stack);
                }

                var stockedAmount = getStockedAmount(i);
                lastSentStockedAmounts[i] = stockedAmount;
                if (stockedAmount > 0) {
                    stockedAmounts.put(i, stockedAmount);
                }
            }

            return FilterTerminalPacket.fullUpdate(serverId, lastSent.length, metadata.group,
                    metadata.dimension, metadata.pos, metadata.side, target.supportsAmountEditing(), slots,
                    stockedAmounts);
        }

        @Nullable
        private FilterTerminalPacket createUpdatePacket() {
            Int2ObjectMap<GenericStack> slots = null;
            Int2LongMap stockedAmounts = null;
            for (var i = 0; i < lastSent.length; i++) {
                var current = target.config().getStack(i);
                if (!Objects.equals(current, lastSent[i])) {
                    if (slots == null) {
                        slots = new Int2ObjectArrayMap<>();
                    }
                    lastSent[i] = current;
                    slots.put(i, current);
                }

                var currentStockedAmount = getStockedAmount(i);
                if (currentStockedAmount != lastSentStockedAmounts[i]) {
                    if (stockedAmounts == null) {
                        stockedAmounts = new Int2LongArrayMap();
                    }
                    lastSentStockedAmounts[i] = currentStockedAmount;
                    stockedAmounts.put(i, currentStockedAmount);
                }
            }

            if (slots == null && stockedAmounts == null) {
                return null;
            }

            return FilterTerminalPacket.incrementalUpdate(serverId,
                    slots == null ? new Int2ObjectArrayMap<>() : slots,
                    stockedAmounts == null ? new Int2LongArrayMap() : stockedAmounts);
        }

        private long getStockedAmount(int slot) {
            if (target.interfaceHost == null) {
                return 0;
            }
            var configured = target.config().getStack(slot);
            var stored = target.interfaceHost.getStorage().getStack(slot);
            return configured != null && stored != null && configured.what().equals(stored.what())
                    ? stored.amount()
                    : 0;
        }
    }

    private record TerminalTarget(IConfigInvHost host, IActionHost actionHost,
            @Nullable InterfaceLogicHost interfaceHost, HostMetadata metadata, int usableSlots) {

        @Nullable
        private static TerminalTarget from(IConfigInvHost host) {
            if (!(host instanceof IActionHost actionHost)) {
                return null;
            }

            if (host instanceof InterfaceLogicHost interfaceHost) {
                var icon = interfaceHost.getMainMenuIcon();
                var name = interfaceHost instanceof Nameable nameable ? nameable.getDisplayName()
                        : icon.getHoverName();
                var metadata = HostMetadata.from(interfaceHost.getBlockEntity(), interfaceHost, icon, name);
                return metadata == null ? null
                        : new TerminalTarget(host, actionHost, interfaceHost, metadata, getUsableSlotCount(host));
            }

            if (!(host instanceof AEBasePart part) || !isSupportedNativePart(part)) {
                return null;
            }

            var metadata = HostMetadata.from(part.getBlockEntity(), part, new ItemStack(part.getPartItem()),
                    part.getDisplayName());
            return metadata == null ? null
                    : new TerminalTarget(host, actionHost, null, metadata, getUsableSlotCount(host));
        }

        private static boolean isSupportedNativePart(AEBasePart part) {
            return part instanceof IOBusPart
                    || part instanceof StorageBusPart
                    || part instanceof FormationPlanePart
                    || part instanceof StorageLevelEmitterPart;
        }

        private static int getUsableSlotCount(IConfigInvHost host) {
            var configSize = host.getConfig().size();
            if (host instanceof InterfaceLogicHost) {
                return configSize;
            }
            if (host instanceof StorageLevelEmitterPart) {
                return Math.min(1, configSize);
            }
            if (host instanceof IUpgradeableObject upgradeable) {
                var capacityCards = upgradeable.getUpgrades().getInstalledUpgrades(AEItems.CAPACITY_CARD);
                return Math.min(18 + capacityCards * 9, configSize);
            }
            return 0;
        }

        private GenericStackInv config() {
            return host.getConfig();
        }

        private boolean supportsAmountEditing() {
            return interfaceHost != null;
        }
    }

    private record HostMetadata(PatternContainerGroup group, ResourceKey<Level> dimension, BlockPos pos,
            @Nullable Direction side) {

        private static final Comparator<HostMetadata> COMPARATOR = Comparator
                .comparing((HostMetadata metadata) -> metadata.group.name().getString(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(metadata -> metadata.dimension.location().toString())
                .thenComparingLong(metadata -> metadata.pos.asLong())
                .thenComparingInt(metadata -> metadata.side == null ? -1 : metadata.side.ordinal());

        @Nullable
        private static HostMetadata from(BlockEntity blockEntity, Object host, ItemStack iconStack, Component name) {
            if (blockEntity == null || blockEntity.getLevel() == null) {
                return null;
            }

            var icon = AEItemKey.of(iconStack);
            var group = new PatternContainerGroup(icon, name, List.of());
            var side = findPartSide(host, blockEntity);
            return new HostMetadata(group, blockEntity.getLevel().dimension(), blockEntity.getBlockPos(), side);
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
    }
}
