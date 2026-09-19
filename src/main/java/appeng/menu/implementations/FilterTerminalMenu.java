package appeng.menu.implementations;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.ints.Int2LongArrayMap;
import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import appeng.api.filterterminal.FilterTerminalTargetMetadata;
import appeng.api.filterterminal.IFilterTerminalConfigView;
import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.AELog;
import appeng.core.sync.packets.ClearFilterTerminalPacket;
import appeng.core.sync.packets.FilterTerminalPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.AEBaseMenu;
import appeng.menu.slot.FakeSlot;
import appeng.parts.reporting.FilterTerminalPart;

/**
 * @see appeng.client.gui.me.filterterminal.FilterTerminalScreen
 */
public class FilterTerminalMenu extends AEBaseMenu {

    public static final MenuType<FilterTerminalMenu> TYPE = MenuTypeBuilder
            .create(FilterTerminalMenu::new, FilterTerminalPart.class)
            .build("filter_terminal");

    private static long inventorySerial = Long.MIN_VALUE;

    private final FilterTerminalPart host;
    private final Map<Object, TargetTracker> trackers = new IdentityHashMap<>();
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
        var currentTargets = FilterTerminalTargetDiscovery.findTargets(grid);
        var needsFullUpdate = currentTargets.size() != trackers.size();

        if (!needsFullUpdate) {
            for (var currentTarget : currentTargets) {
                var tracker = trackers.get(currentTarget.getIdentity());
                if (tracker == null || !tracker.matches(currentTarget)) {
                    needsFullUpdate = true;
                    break;
                }
                tracker.updateTarget(currentTarget);
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

    private void sendFullUpdate(List<IFilterTerminalTarget> currentTargets) {
        var previousTrackers = new IdentityHashMap<>(trackers);
        trackers.clear();
        byId.clear();
        sendPacketToClient(new ClearFilterTerminalPacket());

        for (var currentTarget : currentTargets) {
            var identity = currentTarget.getIdentity();
            var previousTracker = previousTrackers.get(identity);
            var tracker = new TargetTracker(currentTarget,
                    previousTracker == null ? inventorySerial++ : previousTracker.serverId);
            trackers.put(identity, tracker);
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
        if (tracker == null) {
            refreshTargets();
            return;
        }

        var view = tracker.target.getConfigView();
        if (!FilterTerminalEditValidation.matchesExpected(view, slot, expectedKey)) {
            refreshTargets();
            return;
        }

        var workingInventory = FilterTerminalEditValidation.createWorkingInventory(view, slot);
        var fakeSlot = new FakeSlot(workingInventory.createMenuWrapper(), 0);
        handleFakeSlotAction(fakeSlot, action);
        if (!FilterTerminalEditValidation.setConfig(view, slot, expectedKey, workingInventory.getStack(0))) {
            refreshTargets();
        }
    }

    public void setRemoteFilter(long id, int slot, ItemStack stack, @Nullable AEKey expectedKey) {
        var tracker = getValidTracker(id, slot);
        if (tracker == null
                || !FilterTerminalEditValidation.setFilter(tracker.target.getConfigView(), slot, stack, expectedKey)) {
            refreshTargets();
        }
    }

    public void openSetAmountMenu(long id, int slot, @Nullable AEKey expectedKey) {
        var tracker = getValidTracker(id, slot);
        if (tracker == null) {
            refreshTargets();
            return;
        }

        var view = tracker.target.getConfigView();
        if (!FilterTerminalEditValidation.canEditAmount(view, slot, expectedKey)) {
            refreshTargets();
            return;
        }

        var configured = view.getConfig(slot);
        if (configured != null) {
            FilterTerminalSetAmountMenu.open((ServerPlayer) getPlayer(), getLocator(), tracker.target, slot,
                    configured);
        }
    }

    private void refreshTargets() {
        sendFullUpdate(FilterTerminalTargetDiscovery.findTargets(getGrid()));
    }

    @Nullable
    private TargetTracker getValidTracker(long id, int slot) {
        var tracker = FilterTerminalEditValidation.findTarget(byId, id);
        if (tracker == null) {
            return null;
        }
        if (slot < 0 || slot >= tracker.target.getConfigView().size()) {
            AELog.warn("Client refers to invalid filter configuration slot {} of {}", slot,
                    tracker.target.getIdentity());
            return null;
        }

        if (!FilterTerminalEditValidation.isValidTarget(getGrid(), tracker.target)) {
            return null;
        }

        return tracker;
    }

    private static final class TargetTracker {

        private final long serverId;
        private IFilterTerminalTarget target;
        private final FilterTerminalTargetMetadata metadata;
        private final boolean supportsAmountEditing;
        private final GenericStack[] lastSent;
        private final long[] lastSentStockedAmounts;

        private TargetTracker(IFilterTerminalTarget target, long serverId) {
            this.serverId = serverId;
            this.target = target;
            this.metadata = target.getMetadata();

            var view = target.getConfigView();
            this.supportsAmountEditing = supportsAmountEditing(view);
            this.lastSent = new GenericStack[view.size()];
            this.lastSentStockedAmounts = new long[view.size()];
        }

        private boolean matches(IFilterTerminalTarget currentTarget) {
            var currentView = currentTarget.getConfigView();
            return target.getIdentity() == currentTarget.getIdentity()
                    && lastSent.length == currentView.size()
                    && supportsAmountEditing == supportsAmountEditing(currentView)
                    && metadata.equals(currentTarget.getMetadata());
        }

        private void updateTarget(IFilterTerminalTarget currentTarget) {
            target = currentTarget;
        }

        private FilterTerminalPacket createFullPacket() {
            Int2ObjectMap<GenericStack> slots = new Int2ObjectArrayMap<>();
            Int2LongMap stockedAmounts = new Int2LongArrayMap();
            var view = target.getConfigView();
            for (var i = 0; i < lastSent.length; i++) {
                var stack = view.getConfig(i);
                lastSent[i] = stack;
                if (stack != null) {
                    slots.put(i, stack);
                }

                var stockedAmount = getStockedAmount(view, i);
                lastSentStockedAmounts[i] = stockedAmount;
                if (stockedAmount > 0) {
                    stockedAmounts.put(i, stockedAmount);
                }
            }

            return FilterTerminalPacket.fullUpdate(serverId, lastSent.length, metadata.group(),
                    metadata.dimension(), metadata.pos(), metadata.side(), supportsAmountEditing, slots,
                    stockedAmounts);
        }

        @Nullable
        private FilterTerminalPacket createUpdatePacket() {
            Int2ObjectMap<GenericStack> slots = null;
            Int2LongMap stockedAmounts = null;
            var view = target.getConfigView();
            for (var i = 0; i < lastSent.length; i++) {
                var current = view.getConfig(i);
                if (!Objects.equals(current, lastSent[i])) {
                    if (slots == null) {
                        slots = new Int2ObjectArrayMap<>();
                    }
                    lastSent[i] = current;
                    slots.put(i, current);
                }

                var currentStockedAmount = getStockedAmount(view, i);
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

        private static long getStockedAmount(IFilterTerminalConfigView view, int slot) {
            var configured = view.getConfig(slot);
            var stocked = view.getStock(slot);
            return configured != null && stocked != null && configured.what().equals(stocked.what())
                    ? stocked.amount()
                    : 0;
        }

        private static boolean supportsAmountEditing(IFilterTerminalConfigView view) {
            for (var slot = 0; slot < view.size(); slot++) {
                if (view.canEditAmount(slot)) {
                    return true;
                }
            }
            return false;
        }
    }
}
