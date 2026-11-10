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

package appeng.me.storage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StickyClaim;
import appeng.core.localization.GuiText;

/**
 * Manages all available {@link MEStorage} on the network.
 */
public class NetworkStorage implements MEStorage {
    private static final Comparator<Integer> PRIORITY_SORTER = (o1, o2) -> Integer.compare(o2, o1);

    // This flag prevents both concurrent modifications of the mounted storage while
    // they're being iterated, and recursive extract/insert/list operations.
    private boolean mountsInUse;
    private ChangeListener changeListener = ChangeListener.NONE;

    private final NavigableMap<Integer, List<MEStorage>> priorityInventory;
    private final Map<AEKey, List<MEStorage>> stickyClaims = new HashMap<>();
    private final Map<MEStorage, List<AEKey>> stickyClaimsByInventory = new IdentityHashMap<>();
    private final List<MEStorage> unindexedStickyInventories = new ArrayList<>();
    private final List<MEStorage> secondPassInventories = new ArrayList<>();

    // Queued mount/unmount operations that occurred while an insert/extract was ongoing
    // Is only non-null if something is queued
    @Nullable
    private List<QueuedOperation> queuedOperations;

    public NetworkStorage() {
        this.priorityInventory = new TreeMap<>(PRIORITY_SORTER);
    }

    public void forEachMount(Consumer<MEStorage> consumer) {
        for (var list : this.priorityInventory.values()) {
            for (var inv : list) {
                consumer.accept(inv);
            }
        }
    }

    public boolean isInUse() {
        return mountsInUse;
    }

    public boolean allMountsReportAllChanges() {
        for (var list : this.priorityInventory.values()) {
            for (var inv : list) {
                if (!inv.reportsAllChanges()) {
                    return false;
                }
            }
        }
        return true;
    }

    public void mount(int priority, MEStorage inventory) {
        if (mountsInUse) {
            if (queuedOperations == null) {
                queuedOperations = new ArrayList<>();
            }
            queuedOperations.add(new MountOperation(priority, inventory));
        } else {
            this.priorityInventory.computeIfAbsent(priority, k -> new ArrayList<>())
                    .add(inventory);
            var stickyClaim = inventory.getStickyClaim();
            if (stickyClaim != null) {
                addStickyClaims(inventory, stickyClaim);
            }
        }
    }

    public void unmount(MEStorage inventory) {
        if (mountsInUse) {
            if (queuedOperations == null) {
                queuedOperations = new ArrayList<>();
            }
            queuedOperations.add(new UnmountOperation(inventory));
        } else {
            var prioIt = this.priorityInventory.entrySet().iterator();
            while (prioIt.hasNext()) {
                var prioEntry = prioIt.next();

                var inventories = prioEntry.getValue();
                if (inventories.remove(inventory) && inventories.isEmpty()) {
                    prioIt.remove();
                }
            }
            removeStickyClaims(inventory);
        }
    }

    private void addStickyClaims(MEStorage inventory, StickyClaim stickyClaim) {
        var stickyKeys = stickyClaim.keys();
        if (stickyKeys == null) {
            this.unindexedStickyInventories.add(inventory);
            return;
        }

        var keys = List.copyOf(stickyKeys);
        this.stickyClaimsByInventory.put(inventory, keys);
        for (var key : keys) {
            this.stickyClaims.computeIfAbsent(key, k -> new ArrayList<>(1)).add(inventory);
        }
    }

    private void removeStickyClaims(MEStorage inventory) {
        var keys = this.stickyClaimsByInventory.remove(inventory);
        if (keys == null) {
            this.unindexedStickyInventories.remove(inventory);
            return;
        }

        for (var key : keys) {
            var claimants = this.stickyClaims.get(key);
            if (claimants != null && claimants.remove(inventory) && claimants.isEmpty()) {
                this.stickyClaims.remove(key);
            }
        }
    }

    public long insert(AEKey what, long amount, Actionable type, IActionSource src) {
        if (mountsInUse) {
            return 0; // Prevent recursive use
        }

        var remaining = amount;

        mountsInUse = true;
        try {
            var sticky = isClaimedBySticky(what);

            for (var invList : this.priorityInventory.values()) {
                if (remaining <= 0) {
                    break;
                }

                secondPassInventories.clear();

                // First give every inventory a chance to accept the item if it's preferential storage for the given
                // stack
                var ii = invList.iterator();
                while (ii.hasNext() && remaining > 0) {
                    var inv = ii.next();

                    if (sticky && !inv.acceptsSticky(what) || isQueuedForRemoval(inv)) {
                        continue;
                    }

                    if (inv.isPreferredStorageFor(what, src)) {
                        remaining -= insertInto(inv, what, remaining, type, src);
                    } else {
                        secondPassInventories.add(inv);
                    }
                }

                // Then give every remaining inventory a chance
                for (var inv : secondPassInventories) {
                    if (remaining <= 0) {
                        break;
                    }

                    if (isQueuedForRemoval(inv)) {
                        continue;
                    }

                    remaining -= insertInto(inv, what, remaining, type, src);
                }
            }

        } finally {
            mountsInUse = false;
        }

        flushQueuedOperations();

        return amount - remaining;
    }

    private long insertInto(MEStorage inv, AEKey what, long amount, Actionable type, IActionSource src) {
        var inserted = inv.insert(what, amount, type, src);
        if (inserted > 0 && type == Actionable.MODULATE) {
            reportChange(inv, what, inserted);
        }
        return inserted;
    }

    private long extractFrom(MEStorage inv, AEKey what, long amount, Actionable mode, IActionSource src) {
        var extracted = inv.extract(what, amount, mode, src);
        if (extracted > 0 && mode == Actionable.MODULATE) {
            reportChange(inv, what, -extracted);
        }
        return extracted;
    }

    private void reportChange(MEStorage inv, AEKey what, long delta) {
        if (changeListener.ignoresChangesOf(inv)) {
            return;
        }
        if (inv.reportsExactChanges()) {
            changeListener.onExactChange(what, delta);
        } else {
            changeListener.onOtherChange();
        }
    }

    public interface ChangeListener {
        ChangeListener NONE = new ChangeListener() {
            @Override
            public boolean ignoresChangesOf(MEStorage inv) {
                return false;
            }

            @Override
            public void onExactChange(AEKey what, long delta) {
            }

            @Override
            public void onOtherChange() {
            }
        };

        boolean ignoresChangesOf(MEStorage inv);

        void onExactChange(AEKey what, long delta);

        void onOtherChange();
    }

    public void setChangeListener(ChangeListener changeListener) {
        this.changeListener = Objects.requireNonNull(changeListener);
    }

    private boolean isClaimedBySticky(AEKey what) {
        if (!this.stickyClaims.isEmpty()) {
            var claimants = this.stickyClaims.get(what);
            if (claimants != null && anyClaims(claimants, what)) {
                return true;
            }
        }
        return !this.unindexedStickyInventories.isEmpty() && anyClaims(this.unindexedStickyInventories, what);
    }

    private boolean anyClaims(List<MEStorage> claimants, AEKey what) {
        for (int i = 0, size = claimants.size(); i < size; i++) {
            var inv = claimants.get(i);
            if (inv.acceptsSticky(what) && !isQueuedForRemoval(inv)) {
                return true;
            }
        }
        return false;
    }

    private void flushQueuedOperations() {
        Preconditions.checkState(!this.mountsInUse);
        var queuedOperations = this.queuedOperations;
        if (queuedOperations != null) {
            this.queuedOperations = null;
            for (var op : queuedOperations) {
                if (op instanceof MountOperation mountOp) {
                    mount(mountOp.priority, mountOp.storage);
                } else if (op instanceof UnmountOperation unmountOp) {
                    unmount(unmountOp.storage);
                } else {
                    throw new IllegalStateException("Unknown operation: " + op);
                }
            }
        }
    }

    private boolean isQueuedForRemoval(MEStorage inv) {
        if (queuedOperations != null) {
            for (var queuedOperation : queuedOperations) {
                if (queuedOperation instanceof UnmountOperation unmountOperation && unmountOperation.storage == inv) {
                    return true;
                }
            }
        }
        return false;
    }

    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (mountsInUse) {
            return 0; // Prevent recursive use
        }

        var extracted = 0L;

        mountsInUse = true;
        try {
            for (var invList : this.priorityInventory.descendingMap().values()) {
                var ii = invList.iterator();
                while (ii.hasNext() && extracted < amount) {
                    var inv = ii.next();

                    if (isQueuedForRemoval(inv)) {
                        continue;
                    }

                    extracted += extractFrom(inv, what, amount - extracted, mode, source);
                }
            }
        } finally {
            mountsInUse = false;
        }

        flushQueuedOperations();

        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        if (mountsInUse) {
            return; // Prevent recursive use
        }

        mountsInUse = true;
        try {
            for (var i : this.priorityInventory.values()) {
                for (var j : i) {
                    j.getAvailableStacks(out);
                }
            }
        } finally {
            mountsInUse = false;
        }
    }

    @Override
    public int getEstimatedStackCount() {
        if (mountsInUse) {
            return 0; // Prevent recursive use
        }

        mountsInUse = true;
        int total = 0;
        try {
            for (var i : this.priorityInventory.values()) {
                for (var j : i) {
                    total += j.getEstimatedStackCount();
                }
            }
        } finally {
            mountsInUse = false;
        }
        return total;
    }

    @Override
    public Component getDescription() {
        return GuiText.MENetworkStorage.text();
    }

    sealed interface QueuedOperation permits MountOperation, UnmountOperation {
    }

    private record MountOperation(int priority, MEStorage storage) implements QueuedOperation {
    }

    private record UnmountOperation(MEStorage storage) implements QueuedOperation {
    }
}
