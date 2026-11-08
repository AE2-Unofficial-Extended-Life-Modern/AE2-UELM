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

import org.jetbrains.annotations.Nullable;

import appeng.api.config.Actionable;
import appeng.api.config.IncludeExclude;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StickyClaim;
import appeng.util.prioritylist.DefaultPriorityList;
import appeng.util.prioritylist.IPartitionList;
import appeng.util.prioritylist.PrecisePriorityList;

public class MEInventoryHandler extends DelegatingMEInventory {

    private IPartitionList partitionList = DefaultPriorityList.INSTANCE;
    private IncludeExclude partitionListMode = IncludeExclude.WHITELIST;
    private boolean filterOnExtraction;
    private boolean filterAvailableContents;
    private boolean allowExtraction = true;
    private boolean allowInsertion = true;
    private boolean voidOverflow;
    private boolean sticky;

    private boolean gettingAvailableContent = false;

    public MEInventoryHandler(MEStorage inventory) {
        super(inventory);
    }

    public void setAllowExtraction(boolean allowExtraction) {
        this.allowExtraction = allowExtraction;
    }

    public void setAllowInsertion(boolean allowInsertion) {
        this.allowInsertion = allowInsertion;
    }

    protected IncludeExclude getWhitelist() {
        return this.partitionListMode;
    }

    public void setWhitelist(IncludeExclude myWhitelist) {
        this.partitionListMode = myWhitelist;
    }

    protected IPartitionList getPartitionList() {
        return this.partitionList;
    }

    public void setPartitionList(IPartitionList myPartitionList) {
        this.partitionList = myPartitionList;
    }

    public void setExtractFiltering(boolean filterOnExtraction, boolean filterAvailableContents) {
        this.filterOnExtraction = filterOnExtraction;
        this.filterAvailableContents = filterAvailableContents;
    }

    public void setVoidOverflow(boolean voidOverflow) {
        this.voidOverflow = voidOverflow;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (!this.allowInsertion || !passesBlackOrWhitelist(what)) {
            return 0;
        }

        final var inserted = super.insert(what, amount, mode, source);
        return this.voidOverflow ? amount : inserted;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        if (this.filterOnExtraction && !canExtract(what)) {
            return 0;
        }

        return super.extract(what, amount, mode, source);
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        if (this.gettingAvailableContent) {
            // Prevent recursion in case the internal inventory somehow calls this when the available items are queried.
            // This is handled by the NetworkInventoryHandler when the initial query is coming from the network.
            // However, this function might be called from the storage bus code directly,
            // so we have to do this check manually.
            return;
        }

        this.gettingAvailableContent = true;
        try {
            if (!this.filterAvailableContents) {
                super.getAvailableStacks(out);
            } else {
                if (!this.allowExtraction) {
                    return;
                }

                if (this.partitionList.isEmpty()) {
                    super.getAvailableStacks(out);
                    return;
                }

                var whitelist = this.partitionListMode == IncludeExclude.WHITELIST;
                for (var entry : getDelegate().getAvailableStacks()) {
                    if (this.partitionList.isListed(entry.getKey()) == whitelist) {
                        out.add(entry.getKey(), entry.getLongValue());
                    }
                }
            }
        } finally {
            this.gettingAvailableContent = false;
        }
    }

    public boolean wraps(MEStorage storage) {
        return getDelegate() == storage;
    }

    public boolean wraps(Class<? extends MEStorage> type) {
        return type.isInstance(getDelegate());
    }

    public boolean listsKey(AEKey key) {
        if (!this.filterAvailableContents) {
            return true;
        }
        if (!this.allowExtraction) {
            return false;
        }
        return this.partitionList.isEmpty()
                || this.partitionList.isListed(key) == (this.partitionListMode == IncludeExclude.WHITELIST);
    }

    @Override
    public boolean reportsExactChanges() {
        return getClass() == MEInventoryHandler.class && filtersKeepChangesExact();
    }

    protected final boolean filtersKeepChangesExact() {
        if (this.voidOverflow) {
            return false;
        }
        if (this.filterAvailableContents && (!this.allowExtraction || !this.filterOnExtraction)) {
            return false;
        }
        return getDelegate().reportsExactChanges();
    }

    @Override
    public boolean reportsAllChanges() {
        return getDelegate().reportsAllChanges();
    }

    @Override
    public KeyCounter getAvailableStacks() {
        var result = new KeyCounter(getEstimatedStackCount());
        getAvailableStacks(result);
        return result;
    }

    @Override
    public int getEstimatedStackCount() {
        if (this.gettingAvailableContent) {
            // Prevent recursion in case the internal inventory somehow calls this when the available items are queried.
            // This is handled by the NetworkInventoryHandler when the initial query is coming from the network.
            // However, this function might be called from the storage bus code directly,
            // so we have to do this check manually.
            return 0;
        }

        this.gettingAvailableContent = true;
        try {
            if (!this.filterAvailableContents) {
                return super.getEstimatedStackCount();
            } else {
                if (!this.allowExtraction) {
                    return 0;
                }

                return super.getEstimatedStackCount();
            }
        } finally {
            this.gettingAvailableContent = false;
        }
    }

    @Override
    public boolean isPreferredStorageFor(AEKey input, IActionSource source) {
        if (this.partitionListMode == IncludeExclude.WHITELIST) {
            if (this.partitionList.isListed(input)) {
                return true;
            }
        }

        // Inventories that already contain some equal stack are also preferred
        // we use a copy of size 1 here to prevent inventories from attempting to query multiple sub-inventories
        if (super.extract(input, 1, Actionable.SIMULATE, source) > 0) {
            return true;
        }

        return super.isPreferredStorageFor(input, source);
    }

    @Nullable
    @Override
    public StickyClaim getStickyClaim() {
        if (!this.sticky || !this.allowInsertion) {
            return null;
        }
        return this.partitionListMode == IncludeExclude.WHITELIST
                && this.partitionList instanceof PrecisePriorityList precisePriorityList
                        ? new StickyClaim(precisePriorityList.getItems())
                        : StickyClaim.UNINDEXED;
    }

    @Override
    public boolean acceptsSticky(AEKey what) {
        return this.sticky && this.allowInsertion && passesBlackOrWhitelist(what);
    }

    protected boolean canExtract(AEKey request) {
        return allowExtraction && passesBlackOrWhitelist(request);
    }

    // Applies the black/whitelist, but only if any item is listed at all
    private boolean passesBlackOrWhitelist(AEKey input) {
        return this.partitionList.matchesFilter(input, this.partitionListMode);
    }
}
