package appeng.blockentity.misc;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import it.unimi.dsi.fastutil.objects.Reference2LongOpenHashMap;

import appeng.api.config.Actionable;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageCells;
import appeng.block.misc.SuperMEReplenisherBlock.Activity;
import appeng.blockentity.ServerTickingBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.capabilities.Capabilities;
import appeng.core.definitions.AEBlocks;
import appeng.core.localization.GuiText;
import appeng.helpers.IStockAmountHost;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.items.storage.BasicStorageCell;
import appeng.items.storage.CreativeCellItem;
import appeng.me.helpers.MachineSource;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.SuperMEReplenisherMenu;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;

public class SuperMEReplenisherBlockEntity extends AENetworkBlockEntity
        implements IGridTickable, ServerTickingBlockEntity, InternalInventoryHost, IStockAmountHost {
    public static final int CONFIG_SLOT_COUNT = 27;
    // maybe move these to aeconfig later?
    public static final int DEFAULT_TICK_RATE = 120;
    public static final int DEFAULT_THRESHOLD = 50;
    public static final int MAX_TICK_RATE = 10_000;

    private static final String TAG_CELLS = "cells";
    private static final String TAG_CONFIG = "config";
    private static final String TAG_STOCK = "stock";
    private static final String TAG_PENDING_INPUT = "pendingInput";
    private static final String TAG_TICK_RATE = "tickRate";
    private static final String TAG_THRESHOLD = "threshold";

    private final AppEngInternalInventory cells = new AppEngInternalInventory(this, 6);
    private final TargetInventory config = new TargetInventory();
    private final KeyCounter stock = new KeyCounter();
    private final KeyCounter pendingInput = new KeyCounter();
    private final IActionSource actionSource = new MachineSource(this);
    private final MEStorage exposedStorage = new ReplenisherStorage();
    private LazyOptional<MEStorage> exposedStorageCapability = LazyOptional.of(() -> exposedStorage);

    private long totalBytes;
    private long usedBytes;
    private int tickRate = DEFAULT_TICK_RATE;
    private int threshold = DEFAULT_THRESHOLD;
    private int ticksUntilWork = DEFAULT_TICK_RATE;
    private int activityTicks;
    private boolean clientSyncDirty;
    private Activity activity = Activity.IDLE;

    public SuperMEReplenisherBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);

        getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .addService(IGridTickable.class, this);

        cells.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return isValidCell(stack);
            }
        });
        for (int i = 0; i < cells.size(); i++) {
            cells.setMaxStackSize(i, 1);
        }
        cells.setEnableClientEvents(true);
    }

    /**
     * will allow addon cells to work that are of type {@link BasicStorageCell} or {@link CreativeCellItem} <br>
     * Tho in the future we might want to add an interface like ICellInfo that any addon cell can implement
     */
    public boolean isValidCell(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.getItem() instanceof BasicStorageCell
                        || stack.getItem() instanceof CreativeCellItem);
    }

    public boolean canReplaceCell(int slot, ItemStack replacement) {
        long capacity = 0;
        for (int i = 0; i < cells.size(); i++) {
            if (i != slot) {
                capacity = addCapacity(capacity, getCellBytes(cells.getStackInSlot(i)));
            }
        }
        capacity = addCapacity(capacity, getCellBytes(replacement));
        return capacity == Long.MAX_VALUE || capacity >= getUsedBytes();
    }

    private long addCapacity(long current, long additional) {
        if (current == Long.MAX_VALUE || additional == Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        if (Long.MAX_VALUE - current < additional) {
            return Long.MAX_VALUE;
        }
        return current + additional;
    }

    private long getCellBytes(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.getItem() instanceof CreativeCellItem) {
            return Long.MAX_VALUE;
        }
        if (stack.getItem() instanceof BasicStorageCell cell) {
            return cell.getBytes(stack);
        }
        return 0;
    }

    @Override
    public GenericStackInv getConfig() {
        return config;
    }

    public InternalInventory getCellInventory() {
        return cells;
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.CELLS)) {
            return cells;
        }
        return super.getSubInventory(id);
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        if (inv == cells) {
            updateCapacity();
            saveChanges();
        }
    }

    private void configChanged() {
        saveChanges();
        requestClientSync();
    }

    private void updateCapacity() {
        var previousTotalBytes = totalBytes;
        totalBytes = 0;
        for (var cell : cells) {
            totalBytes = addCapacity(totalBytes, getCellBytes(cell));
        }
        updateUsedBytes();
        if (totalBytes != previousTotalBytes) {
            requestClientSync();
        }

        var powerDraw = totalBytes == Long.MAX_VALUE
                ? 1
                : Math.max(1, Math.sqrt(Math.pow(totalBytes, 0.576D)));
        getMainNode().setIdlePowerUsage(powerDraw);
    }

    private void updateUsedBytes() {
        var newUsedBytes = calculateUsedBytes(stock, pendingInput);
        if (usedBytes != newUsedBytes) {
            usedBytes = newUsedBytes;
            requestClientSync();
        }
    }

    /**
     * will sync fields like config used bytes etc. to the client
     */
    private void requestClientSync() {
        if (level != null && !level.isClientSide) {
            clientSyncDirty = true;
        }
    }

    static long calculateUsedBytes(KeyCounter first, KeyCounter second) {
        var amountsByType = new Reference2LongOpenHashMap<AEKeyType>();
        addAmountsByType(amountsByType, first);
        addAmountsByType(amountsByType, second);

        long result = 0;
        for (var entry : amountsByType.reference2LongEntrySet()) {
            var amountPerByte = Math.max(1, entry.getKey().getAmountPerByte());
            var amount = entry.getLongValue();
            var bytes = amount / amountPerByte + (amount % amountPerByte == 0 ? 0 : 1);
            result = saturatingAdd(result, bytes);
        }
        return result;
    }

    private static void addAmountsByType(Reference2LongOpenHashMap<AEKeyType> amountsByType, KeyCounter counter) {
        for (var entry : counter) {
            if (entry.getLongValue() > 0) {
                var type = entry.getKey().getType();
                amountsByType.put(type, saturatingAdd(amountsByType.getLong(type), entry.getLongValue()));
            }
        }
    }

    private static long saturatingAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private long getAmountForType(AEKeyType type) {
        long amount = 0;
        for (var entry : stock) {
            if (entry.getKey().getType() == type) {
                amount = saturatingAdd(amount, entry.getLongValue());
            }
        }
        for (var entry : pendingInput) {
            if (entry.getKey().getType() == type) {
                amount = saturatingAdd(amount, entry.getLongValue());
            }
        }
        return amount;
    }

    private long getInsertableAmount(AEKey what, long requested) {
        if (requested <= 0) {
            return 0;
        }
        if (totalBytes == Long.MAX_VALUE) {
            return requested;
        }

        var freeBytes = Math.max(0, totalBytes - usedBytes);
        var amountPerByte = Math.max(1, what.getType().getAmountPerByte());
        var typeAmount = getAmountForType(what.getType());
        var remainder = typeAmount % amountPerByte;
        var unusedInPartialByte = remainder == 0 ? 0 : amountPerByte - remainder;

        long available;
        if (freeBytes > Long.MAX_VALUE / amountPerByte) {
            available = Long.MAX_VALUE;
        } else {
            available = saturatingAdd(freeBytes * amountPerByte, unusedInPartialByte);
        }
        return Math.min(requested, available);
    }

    private long insert(KeyCounter target, AEKey what, long amount, Actionable mode) {
        var inserted = getInsertableAmount(what, amount);
        if (inserted > 0 && mode == Actionable.MODULATE) {
            target.add(what, inserted);
            target.removeZeros();
            updateUsedBytes();
            saveChanges();
        }
        return inserted;
    }

    private long extract(KeyCounter target, AEKey what, long amount, Actionable mode) {
        var extracted = Math.min(amount, Math.max(0, target.get(what)));
        if (extracted > 0 && mode == Actionable.MODULATE) {
            target.remove(what, extracted);
            target.removeZeros();
            updateUsedBytes();
            saveChanges();
        }
        return extracted;
    }

    public long getStoredAmount(AEKey what) {
        return Math.max(0, stock.get(what));
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public int getTickRate() {
        return tickRate;
    }

    public void setTickRate(int tickRate) {
        var newTickRate = Mth.clamp(tickRate, 1, MAX_TICK_RATE);
        if (this.tickRate == newTickRate) {
            return;
        }
        this.tickRate = newTickRate;
        this.ticksUntilWork = Math.min(this.ticksUntilWork, this.tickRate);
        saveChanges();
        requestClientSync();
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        var newThreshold = Mth.clamp(threshold, 1, 100);
        if (this.threshold == newThreshold) {
            return;
        }
        this.threshold = newThreshold;
        saveChanges();
        requestClientSync();
    }

    public Activity getActivity() {
        return activity;
    }

    private void showActivity(Activity newActivity) {
        if (newActivity != activity) {
            activity = newActivity;
            markForUpdate();
        }
        activityTicks = 2;
    }

    @Override
    public void serverTick() {
        if (clientSyncDirty) {
            clientSyncDirty = false;
            markForUpdate();
        }
        if (activityTicks > 0 && --activityTicks == 0 && activity != Activity.IDLE) {
            activity = Activity.IDLE;
            markForUpdate();
        }
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeEnum(activity);
        data.writeVarLong(totalBytes);
        data.writeVarLong(usedBytes);
        data.writeVarInt(tickRate);
        data.writeVarInt(threshold);
        for (int i = 0; i < config.size(); i++) {
            GenericStack.writeBuffer(config.getStack(i), data);
        }
    }

    // very ugly code but whatever
    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        var changed = super.readFromStream(data);
        var newActivity = data.readEnum(Activity.class);
        changed |= newActivity != activity;
        activity = newActivity;
        var newTotalBytes = data.readVarLong();
        changed |= newTotalBytes != totalBytes;
        totalBytes = newTotalBytes;
        var newUsedBytes = data.readVarLong();
        changed |= newUsedBytes != usedBytes;
        usedBytes = newUsedBytes;
        var newTickRate = data.readVarInt();
        changed |= newTickRate != tickRate;
        tickRate = newTickRate;
        var newThreshold = data.readVarInt();
        changed |= newThreshold != threshold;
        threshold = newThreshold;
        changed |= config.readFromStream(data);
        return changed;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (--ticksUntilWork > 0) {
            return TickRateModulation.SAME;
        }
        ticksUntilWork = tickRate;

        var grid = node.getGrid();
        if (grid == null || !getMainNode().isOnline()) {
            return TickRateModulation.SAME;
        }

        var networkStorage = grid.getStorageService().getInventory();
        for (int i = 0; i < config.size(); i++) {
            var target = config.getStack(i);
            if (target == null || target.amount() <= 0) {
                continue;
            }

            var stored = getStoredAmount(target.what());
            if (stored > target.amount()) {
                returnToNetwork(networkStorage, stock, target.what(), stored - target.amount());
            } else if (stored * 100.0D / target.amount() < threshold) {
                requestFromNetwork(networkStorage, target.what(), target.amount() - stored);
            }
        }

        returnUnconfiguredStock(networkStorage);
        flushPendingInput(networkStorage);
        return TickRateModulation.SAME;
    }

    void returnUnconfiguredStock(MEStorage networkStorage) {
        for (var what : List.copyOf(stock.keySet())) {
            if (!isConfigured(what)) {
                returnToNetwork(networkStorage, stock, what, stock.get(what));
            }
        }
    }

    private boolean isConfigured(AEKey what) {
        for (int i = 0; i < config.size(); i++) {
            if (Objects.equals(config.getKey(i), what)) {
                return true;
            }
        }
        return false;
    }

    private void requestFromNetwork(MEStorage networkStorage, AEKey what, long amount) {
        var insertable = getInsertableAmount(what, amount);
        if (insertable <= 0) {
            return;
        }
        var extracted = networkStorage.extract(what, insertable, Actionable.MODULATE, actionSource);
        if (extracted > 0) {
            insert(stock, what, extracted, Actionable.MODULATE);
        }
    }

    private void flushPendingInput(MEStorage networkStorage) {
        var keys = List.copyOf(pendingInput.keySet());
        for (var what : keys) {
            returnToNetwork(networkStorage, pendingInput, what, pendingInput.get(what));
        }
    }

    private long returnToNetwork(MEStorage networkStorage, KeyCounter source, AEKey what, long amount) {
        if (amount <= 0) {
            return 0;
        }
        var inserted = networkStorage.insert(what, amount, Actionable.MODULATE, actionSource);
        if (inserted > 0) {
            extract(source, what, inserted, Actionable.MODULATE);
        }
        return inserted;
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        cells.writeToNBT(data, TAG_CELLS);
        config.writeToChildTag(data, TAG_CONFIG);
        data.put(TAG_STOCK, writeCounter(stock));
        data.put(TAG_PENDING_INPUT, writeCounter(pendingInput));
        data.putInt(TAG_TICK_RATE, tickRate);
        data.putInt(TAG_THRESHOLD, threshold);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        cells.readFromNBT(data, TAG_CELLS);
        config.readFromChildTag(data, TAG_CONFIG);
        readCounter(data.getList(TAG_STOCK, Tag.TAG_COMPOUND), stock);
        readCounter(data.getList(TAG_PENDING_INPUT, Tag.TAG_COMPOUND), pendingInput);
        tickRate = Mth.clamp(data.contains(TAG_TICK_RATE) ? data.getInt(TAG_TICK_RATE) : DEFAULT_TICK_RATE,
                1, MAX_TICK_RATE);
        threshold = Mth.clamp(data.contains(TAG_THRESHOLD) ? data.getInt(TAG_THRESHOLD) : DEFAULT_THRESHOLD,
                1, 100);
        ticksUntilWork = tickRate;
        updateCapacity();
    }

    private static ListTag writeCounter(KeyCounter counter) {
        var result = new ListTag();
        for (var entry : counter) {
            if (entry.getLongValue() > 0) {
                result.add(GenericStack.writeTag(new GenericStack(entry.getKey(), entry.getLongValue())));
            }
        }
        return result;
    }

    private static void readCounter(ListTag data, KeyCounter counter) {
        counter.clear();
        for (var rawTag : data) {
            var stack = GenericStack.readTag((CompoundTag) rawTag);
            if (stack != null && stack.amount() > 0) {
                counter.add(stack.what(), stack.amount());
            }
        }
        counter.removeZeros();
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);

        long toInsert = 0;
        long inserted = 0;
        var grid = getMainNode().getGrid();
        if (grid != null) {
            var networkStorage = grid.getStorageService().getInventory();
            for (var what : List.copyOf(stock.keySet())) {
                var amount = stock.get(what);
                toInsert += amount;
                inserted += returnToNetwork(networkStorage, stock, what, amount);
            }
            for (var what : List.copyOf(pendingInput.keySet())) {
                var amount = pendingInput.get(what);
                toInsert += amount;
                inserted += returnToNetwork(networkStorage, pendingInput, what, amount);
            }
        }
        // we try to fill the cells with the keys before discarding them if not grid is available
        if (inserted < toInsert) {
            for (var cell : cells) {
                var cellInventory = StorageCells.getCellInventory(cell, null);
                if (cellInventory != null) {
                    for (var what : List.copyOf(stock.keySet())) {
                        returnToNetwork(cellInventory, stock, what, stock.get(what));
                    }
                    for (var what : List.copyOf(pendingInput.keySet())) {
                        returnToNetwork(cellInventory, pendingInput, what, pendingInput.get(what));
                    }
                }
            }
        }

        for (var cell : cells) {
            if (!cell.isEmpty()) {
                drops.add(cell.copy());
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        cells.clear();
        config.clear();
        stock.clear();
        pendingInput.clear();
        updateCapacity();
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(SuperMEReplenisherMenu.TYPE, player, subMenu.getLocator());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return AEBlocks.SUPER_ME_REPLENISHER.stack();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
        if (capability == Capabilities.STORAGE) {
            return exposedStorageCapability.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        exposedStorageCapability.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        exposedStorageCapability = LazyOptional.of(() -> exposedStorage);
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.MEMORY_CARD) {
            this.setTickRate(input.getInt("tickRate"));
            this.setThreshold(input.getInt("threshold"));
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);

        if (mode == SettingsFrom.MEMORY_CARD) {
            output.putInt("tickRate", this.getTickRate());
            output.putInt("threshold", this.getThreshold());
        }
    }

    private boolean isOwnAction(IActionSource source) {
        return source.machine().orElse(null) == this;
    }

    private final class ReplenisherStorage implements MEStorage {
        @Override
        public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
            return !isOwnAction(source) && pendingInput.get(what) > 0;
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(what, amount, mode, source);
            if (isOwnAction(source)) {
                return 0;
            }

            var inserted = SuperMEReplenisherBlockEntity.this.insert(pendingInput, what, amount, mode);
            if (inserted > 0 && mode == Actionable.MODULATE) {
                showActivity(Activity.INSERTING);
            }
            return inserted;
        }

        @Override
        public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
            MEStorage.checkPreconditions(what, amount, mode, source);
            if (isOwnAction(source)) {
                return 0;
            }

            var extracted = SuperMEReplenisherBlockEntity.this.extract(stock, what, amount, mode);
            if (extracted > 0 && mode == Actionable.MODULATE) {
                showActivity(Activity.EXTRACTING);
            }
            return extracted;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            out.addAll(stock);
        }

        @Override
        public Component getDescription() {
            return GuiText.SuperMEReplenisher.text();
        }
    }

    private final class TargetInventory extends GenericStackInv {
        private TargetInventory() {
            super(SuperMEReplenisherBlockEntity.this::configChanged, Mode.CONFIG_STACKS, CONFIG_SLOT_COUNT);
        }

        @Override
        public long getMaxAmount(AEKey key) {
            return Long.MAX_VALUE;
        }

        @Override
        public void setStack(int slot, @Nullable GenericStack stack) {
            if (stack != null) {
                for (int i = 0; i < size(); i++) {
                    if (i != slot && Objects.equals(getKey(i), stack.what())) {
                        return;
                    }
                }
            }
            super.setStack(slot, stack);
        }

        private boolean readFromStream(FriendlyByteBuf data) {
            var changed = false;
            for (int i = 0; i < stacks.length; i++) {
                var stack = GenericStack.readBuffer(data);
                if (!Objects.equals(stacks[i], stack)) {
                    stacks[i] = stack;
                    changed = true;
                }
            }
            return changed;
        }
    }
}
