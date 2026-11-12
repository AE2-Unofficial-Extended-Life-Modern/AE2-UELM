/*
 * This file is part of Applied Energistics 2.
 * Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.
 *
 * Applied Energistics 2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Applied Energistics 2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Applied Energistics 2.  If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.me.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.IStorageMounts;
import appeng.api.storage.IStorageProvider;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.InterestManager;
import appeng.me.helpers.StackWatcher;
import appeng.me.service.helpers.CraftingServiceStorage;
import appeng.me.storage.DelegatingMEInventory;
import appeng.me.storage.MEInventoryHandler;
import appeng.me.storage.NetworkStorage;

public class StorageService implements IStorageService, IGridServiceProvider {

    /**
     * Tracks the storage service's state for each grid node that provides storage to the network.
     */
    private final Map<IGridNode, ProviderState> nodeProviders = new IdentityHashMap<>();
    /**
     * Tracks state for storage providers that are provided by other grid services (i.e. crafting).
     */
    private final List<ProviderState> globalProviders = new ArrayList<>();
    private final SetMultimap<AEKey, StackWatcher<IStorageWatcherNode>> interests = HashMultimap.create();
    private final InterestManager<StackWatcher<IStorageWatcherNode>> interestManager = new InterestManager<>(
            this.interests);
    private final NetworkStorage storage;
    /**
     * Publicly exposed cached available stacks.
     */
    private final KeyCounter cachedAvailableStacks = new KeyCounter();
    private static final int FULL_REBUILD_INTERVAL = 20;
    private static final int REPORTED_REBUILD_INTERVAL = 200;
    /**
     * Private cached amounts, to ensure that we send correct change notifications even if
     * {@link #cachedAvailableStacks} is modified by mistake.
     */
    private Object2LongMap<AEKey> cachedAvailableAmounts = new Object2LongOpenHashMap<>();
    private boolean cachedStacksNeedUpdate = true;
    private boolean contentsChanged = true;
    private int ticksSinceRebuild;
    private boolean allChangesReported;
    /**
     * Tracks the stack watcher associated with a given grid node. Needed to clean up watchers when the node leaves the
     * grid.
     */
    private final Map<IGridNode, StackWatcher<IStorageWatcherNode>> watchers = new IdentityHashMap<>();

    private static final Deque<StorageService> BUILDING = new ArrayDeque<>();
    private final Set<StorageService> includedServices = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<StorageService> dependents = Collections.newSetFromMap(new WeakHashMap<>());
    private final MEStorage cachedView;
    private boolean partialBuild;

    private KeyCounter pendingDeltas = new KeyCounter();
    private KeyCounter spareDeltas = new KeyCounter();
    private boolean applyingDeltas;

    public StorageService() {
        this.storage = new NetworkStorage() {
            @Override
            public KeyCounter getAvailableStacks() {
                if (!BUILDING.isEmpty() || isInUse()) {
                    return super.getAvailableStacks();
                }
                var result = new KeyCounter();
                result.addAll(getUpToDateCache());
                return result;
            }
        };
        this.storage.setChangeListener(new NetworkStorage.ChangeListener() {
            @Override
            public boolean ignoresChangesOf(MEStorage inv) {
                return inv instanceof CraftingServiceStorage.Inventory || inv instanceof CachedView
                        || inv instanceof MEInventoryHandler handler && handler.wraps(CachedView.class);
            }

            @Override
            public void onExactChange(AEKey what, long delta) {
                pendingDeltas.add(what, delta);
            }

            @Override
            public void onOtherChange() {
                markContentsChanged();
            }
        });
        this.cachedView = new CachedView();
    }

    @Override
    public void onServerEndTick() {
        ticksSinceRebuild++;
        var rebuildInterval = allChangesReported ? REPORTED_REBUILD_INTERVAL : FULL_REBUILD_INTERVAL;
        if (contentsChanged || ticksSinceRebuild >= rebuildInterval
                || cachedStacksNeedUpdate && !interestManager.isEmpty()) {
            if (interestManager.isEmpty()) {
                // lazily rebuild cache list
                cachedStacksNeedUpdate = true;
                contentsChanged = false;
                ticksSinceRebuild = 0;
                discardPendingDeltas();
            } else {
                // rebuild the cache now to notify listeners
                updateCachedStacks();
            }
        } else if (!pendingDeltas.isEmpty()) {
            if (cachedStacksNeedUpdate) {
                discardPendingDeltas();
            } else {
                applyPendingDeltas();
            }
        }
    }

    private void discardPendingDeltas() {
        if (!pendingDeltas.isEmpty()) {
            pendingDeltas.clear();
            markDependentsChanged();
        }
    }

    private void applyPendingDeltas() {
        if (applyingDeltas) {
            return;
        }
        for (var entry : pendingDeltas) {
            if (cachedAvailableAmounts.getLong(entry.getKey()) + entry.getLongValue() < 0) {
                updateCachedStacks();
                return;
            }
        }

        var deltas = pendingDeltas;
        pendingDeltas = spareDeltas;
        spareDeltas = deltas;
        for (var entry : deltas) {
            var delta = entry.getLongValue();
            if (delta == 0) {
                continue;
            }
            var what = entry.getKey();
            var amount = cachedAvailableAmounts.getLong(what) + delta;
            if (amount == 0) {
                cachedAvailableStacks.remove(what);
                cachedAvailableAmounts.removeLong(what);
            } else {
                cachedAvailableStacks.set(what, amount);
                cachedAvailableAmounts.put(what, amount);
            }
        }

        applyingDeltas = true;
        try {
            for (var entry : deltas) {
                if (entry.getLongValue() != 0) {
                    postWatcherUpdate(entry.getKey(), cachedAvailableAmounts.getLong(entry.getKey()));
                }
            }
            if (!dependents.isEmpty()) {
                for (var dependent : List.copyOf(dependents)) {
                    if (includedServices.contains(dependent)) {
                        dependent.markContentsChanged();
                    } else {
                        dependent.onIncludedDeltas(this, deltas);
                    }
                }
            }
        } finally {
            deltas.clear();
            applyingDeltas = false;
        }
    }

    private void onIncludedDeltas(StorageService source, KeyCounter deltas) {
        if (isBuilding() || contentsChanged || cachedStacksNeedUpdate) {
            return;
        }
        var view = source.cachedView;
        var mounts = new ArrayList<MEStorage>();
        storage.forEachMount(mounts::add);
        var found = false;
        for (var mount : mounts) {
            if (mount == view) {
                found = true;
                pendingDeltas.addAll(deltas);
            } else if (mount instanceof MEInventoryHandler handler && handler.wraps(view)) {
                found = true;
                for (var entry : deltas) {
                    if (handler.listsKey(entry.getKey())) {
                        pendingDeltas.add(entry.getKey(), entry.getLongValue());
                    }
                }
            }
        }
        if (!found) {
            markContentsChanged();
        }
    }

    private void markDependentsChanged() {
        if (!dependents.isEmpty()) {
            for (var dependent : List.copyOf(dependents)) {
                dependent.markContentsChanged();
            }
        }
    }

    @Override
    public void markContentsChanged() {
        if (contentsChanged) {
            return;
        }
        contentsChanged = true;
        markDependentsChanged();
    }

    public MEStorage getCachedView() {
        return cachedView;
    }

    private void updateCachedStacks() {
        if (storage.isInUse()) {
            cachedStacksNeedUpdate = true;
            return;
        }

        cachedStacksNeedUpdate = false;
        contentsChanged = false;
        ticksSinceRebuild = 0;
        pendingDeltas.clear();

        for (var included : includedServices) {
            included.dependents.remove(this);
        }
        includedServices.clear();

        // Update cachedAvailableStacks to have the latest inventory contents
        cachedAvailableStacks.clear();
        partialBuild = false;
        BUILDING.push(this);
        try {
            storage.getAvailableStacks(cachedAvailableStacks);
        } finally {
            BUILDING.pop();
        }
        // clear() only clears the inner maps,
        // so ensure that the outer map gets cleaned up too
        cachedAvailableStacks.removeEmptySubmaps();

        if (partialBuild) {
            cachedStacksNeedUpdate = true;
            return;
        }

        // Process the contents of cachedAvailableStacks, send notifications, and compute a new cachedAvailableAmounts
        InventoryListener listener = new InventoryListener(cachedAvailableAmounts, cachedAvailableStacks.size());
        cachedAvailableStacks.forEach(listener);
        listener.sendRemovedStacksToWatchers();
        cachedAvailableAmounts = listener.currentContents();

        var wasReported = allChangesReported;
        allChangesReported = storage.allMountsReportAllChanges();

        if (listener.changed || wasReported != allChangesReported) {
            markDependentsChanged();
        }
    }

    private KeyCounter getUpToDateCache() {
        if (contentsChanged) {
            cachedStacksNeedUpdate = true;
        } else if (!cachedStacksNeedUpdate && !pendingDeltas.isEmpty()) {
            applyPendingDeltas();
        }
        return getCachedInventory();
    }

    private boolean isBuilding() {
        return BUILDING.contains(this);
    }

    private class CachedView extends DelegatingMEInventory {
        CachedView() {
            super(storage);
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            var reader = BUILDING.peek();
            if (isBuilding()) {
                if (reader != null && reader != StorageService.this) {
                    reader.partialBuild = true;
                }
                super.getAvailableStacks(out);
                return;
            }
            var cached = getUpToDateCache();
            if (reader != null) {
                reader.includedServices.add(StorageService.this);
                dependents.add(reader);
            }
            if (reader != null && includedServices.contains(reader)) {
                super.getAvailableStacks(out);
                return;
            }

            out.addAll(cached);
            if (reader != null) {
                reader.includedServices.addAll(includedServices);
            }
        }

        @Override
        public KeyCounter getAvailableStacks() {
            var result = new KeyCounter();
            getAvailableStacks(result);
            return result;
        }

        @Override
        public boolean reportsAllChanges() {
            return allChangesReported;
        }

        @Override
        public boolean anyAvailable(Predicate<AEKey> predicate) {
            if (!BUILDING.isEmpty()) {
                return super.anyAvailable(predicate);
            }
            for (var entry : getUpToDateCache()) {
                if (predicate.test(entry.getKey())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int getEstimatedStackCount() {
            return isBuilding() ? super.getEstimatedStackCount() : cachedAvailableStacks.size();
        }
    }

    /**
     * Listener which accepts and accumulates the current stacks in this storage, and sends notifications to watchers
     * for changes since this listener was created.
     */
    private class InventoryListener implements Consumer<Object2LongMap.Entry<AEKey>> {

        private final Object2LongMap<AEKey> previous;
        private final Object2LongMap<AEKey> next;
        private boolean changed;

        /**
         * @param previous The last known contents of this storage
         * @param nextSize The total number of items to expect in the current state
         */
        private InventoryListener(Object2LongMap<AEKey> previous, int nextSize) {
            this.previous = Objects.requireNonNull(previous);
            this.next = new Object2LongOpenHashMap<>(nextSize);
        }

        @Override
        public void accept(Object2LongMap.Entry<AEKey> entry) {
            // Post watcher update for currently available stacks
            var what = entry.getKey();
            var newAmount = entry.getLongValue();
            next.put(what, newAmount);
            // Remove the key from the previous state, to indicate that we've checked it and sent notifications
            if (newAmount != previous.removeLong(what)) {
                postWatcherUpdate(what, newAmount);
                changed = true;
            }
        }

        /**
         * After contents of the storage has been accepted, send notifications for items which are no longer present
         */
        public void sendRemovedStacksToWatchers() {
            // Post watcher update for removed stacks
            for (var what : previous.keySet()) {
                var newAmount = next.getOrDefault(what, 0);
                if (newAmount == 0) {
                    postWatcherUpdate(what, newAmount);
                    changed = true;
                }
            }
        }

        public Object2LongMap<AEKey> currentContents() {
            return next;
        }
    }

    private void postWatcherUpdate(AEKey what, long newAmount) {
        for (var watcher : interestManager.get(what)) {
            watcher.getHost().onStackChange(what, newAmount);
        }
        for (var watcher : interestManager.getAllStacksWatchers()) {
            watcher.getHost().onStackChange(what, newAmount);
        }
    }

    /**
     * When a node joins the grid, we automatically register provided {@link IStorageProvider} and
     * {@link IStorageWatcherNode}.
     */
    @Override
    public void addNode(IGridNode node, @Nullable CompoundTag savedData) {
        var storageProvider = node.getService(IStorageProvider.class);
        if (storageProvider != null) {
            ProviderState state = new ProviderState(storageProvider);
            this.nodeProviders.put(node, state);
            state.mount();
        }

        var watcher = node.getService(IStorageWatcherNode.class);
        if (watcher != null) {
            var iw = new StackWatcher<>(interestManager, watcher);
            this.watchers.put(node, iw);
            watcher.updateWatcher(iw);
        }
    }

    /**
     * When a node leaves the grid, we automatically unregister the previously registered {@link IStorageProvider} or
     * {@link IStorageWatcherNode}.
     */
    @Override
    public void removeNode(IGridNode node) {
        var watcher = this.watchers.remove(node);
        if (watcher != null) {
            watcher.destroy();
        }

        var providerState = this.nodeProviders.remove(node);
        if (providerState != null) {
            providerState.unmount();
        }
    }

    @Override
    public MEStorage getInventory() {
        return storage;
    }

    @Override
    public KeyCounter getCachedInventory() {
        if (cachedStacksNeedUpdate) {
            updateCachedStacks();
        }
        return cachedAvailableStacks;
    }

    @Override
    public void addGlobalStorageProvider(IStorageProvider provider) {
        var state = new ProviderState(provider);
        this.globalProviders.add(state);
        state.mount();
    }

    @Override
    public void removeGlobalStorageProvider(IStorageProvider provider) {
        var it = this.globalProviders.iterator();
        while (it.hasNext()) {
            var state = it.next();
            if (state.provider == provider) {
                it.remove();
                state.unmount();
            }
        }
    }

    @Override
    public void refreshNodeStorageProvider(IGridNode node) {
        var state = nodeProviders.get(node);
        if (state == null) {
            throw new IllegalArgumentException("The given node is not part of this grid or has no storage provider.");
        }
        state.update();
    }

    @Override
    public void refreshGlobalStorageProvider(IStorageProvider provider) {
        for (var state : globalProviders) {
            if (state.provider == provider) {
                state.update();
                return;
            }
        }

        throw new IllegalArgumentException("The given node is not part of this grid or has no storage provider.");
    }

    @Override
    public void invalidateCache() {
        cachedStacksNeedUpdate = true;
        markContentsChanged();
    }

    /**
     * A {@link IStorageProvider}-specific mount table facade which allows the provider to easily mount/remount its
     * storage.
     */
    private class ProviderState implements IStorageMounts {
        private final IStorageProvider provider;
        private final Set<MEStorage> inventories = new HashSet<>();
        private boolean mounted;

        public ProviderState(IStorageProvider provider) {
            this.provider = provider;
        }

        /**
         * Performs the first mount operation on this storage provider, which does not assume any of the provider's
         * inventories are currently mounted and need to be removed first.
         */
        private void mount() {
            Preconditions.checkState(!mounted, "Can't mount a provider's inventories when it's already mounted");

            mounted = true;
            provider.mountInventories(this);
        }

        @Override
        public void mount(MEStorage inventory, int priority) {
            Preconditions.checkState(mounted, "Cannot use StorageMounts after the storage has been unmounted.");

            if (!inventories.add(inventory)) {
                throw new IllegalStateException("Cannot mount the same inventory twice.");
            }

            // Mount this inventory into the network storage
            storage.mount(priority, inventory);
            markContentsChanged();
        }

        public void update() {
            unmount();
            mount();
        }

        public void unmount() {
            if (!mounted) {
                return;
            }
            mounted = false;

            for (var inventory : inventories) {
                unmount(inventory);
            }
            inventories.clear();
        }

        private void unmount(MEStorage inventory) {
            storage.unmount(inventory);
            markContentsChanged();
        }
    }
}
