package appeng.menu.implementations;

import java.util.Arrays;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.blockentity.misc.SuperMEReplenisherBlockEntity;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.guisync.PacketWritable;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.RestrictedInputSlot;

public class SuperMEReplenisherMenu extends AEBaseMenu {
    private static final String ACTION_SET_TICK_RATE = "setTickRate";
    private static final String ACTION_SET_THRESHOLD = "setThreshold";
    private static final String ACTION_OPEN_SET_AMOUNT = "setAmount";

    public static final MenuType<SuperMEReplenisherMenu> TYPE = MenuTypeBuilder
            .create(SuperMEReplenisherMenu::new, SuperMEReplenisherBlockEntity.class)
            .build("super_me_replenisher");

    private final SuperMEReplenisherBlockEntity host;

    @GuiSync(1)
    public long totalBytes;
    @GuiSync(2)
    public long usedBytes;
    @GuiSync(3)
    public int tickRate = SuperMEReplenisherBlockEntity.DEFAULT_TICK_RATE;
    @GuiSync(4)
    public int threshold = SuperMEReplenisherBlockEntity.DEFAULT_THRESHOLD;
    @GuiSync(5)
    public StoredAmounts storedAmounts = new StoredAmounts();

    public SuperMEReplenisherMenu(int id, Inventory playerInventory, SuperMEReplenisherBlockEntity host) {
        super(TYPE, id, playerInventory, host);
        this.host = host;

        registerClientAction(ACTION_SET_TICK_RATE, Integer.class, this::setTickRate);
        registerClientAction(ACTION_SET_THRESHOLD, Integer.class, this::setThreshold);
        registerClientAction(ACTION_OPEN_SET_AMOUNT, Integer.class, this::openSetAmountMenu);

        var cells = host.getCellInventory();
        for (int i = 0; i < 3; i++) {
            addSlot(new RestrictedCellSlot(host, cells, i), SlotSemantics.LEFT_STORAGE_CELL);
        }

        var config = host.getConfig().createMenuWrapper();
        for (int i = 0; i < config.size(); i++) {
            addSlot(new FakeSlot(config, i), SlotSemantics.CONFIG);
        }

        for (int i = 3; i < 6; i++) {
            addSlot(new RestrictedCellSlot(host, cells, i), SlotSemantics.RIGHT_STORAGE_CELL);
        }

        createPlayerInventorySlots(playerInventory);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            totalBytes = host.getTotalBytes();
            usedBytes = host.getUsedBytes();
            tickRate = host.getTickRate();
            threshold = host.getThreshold();

            var amounts = new long[SuperMEReplenisherBlockEntity.CONFIG_SLOT_COUNT];
            for (int i = 0; i < amounts.length; i++) {
                var target = host.getConfig().getStack(i);
                if (target != null) {
                    amounts[i] = host.getStoredAmount(target.what());
                }
            }
            storedAmounts = new StoredAmounts(amounts);
        }
        super.broadcastChanges();
    }

    public void setTickRate(int value) {
        if (isClientSide()) {
            tickRate = value;
            sendClientAction(ACTION_SET_TICK_RATE, value);
        } else {
            host.setTickRate(value);
        }
    }

    public void setThreshold(int value) {
        if (isClientSide()) {
            threshold = value;
            sendClientAction(ACTION_SET_THRESHOLD, value);
        } else {
            host.setThreshold(value);
        }
    }

    public void openSetAmountMenu(int configSlot) {
        if (configSlot < 0 || configSlot >= host.getConfig().size()) {
            return;
        }

        if (isClientSide()) {
            sendClientAction(ACTION_OPEN_SET_AMOUNT, configSlot);
        } else {
            var target = host.getConfig().getStack(configSlot);
            if (target != null) {
                SetStockAmountMenu.open((ServerPlayer) getPlayer(), getLocator(), configSlot,
                        target.what(), target.amount());
            }
        }
    }

    public long getStoredAmount(int configSlot) {
        return storedAmounts.get(configSlot);
    }

    private static class RestrictedCellSlot extends RestrictedInputSlot {
        private final SuperMEReplenisherBlockEntity host;
        private final int cellSlot;

        RestrictedCellSlot(SuperMEReplenisherBlockEntity host, InternalInventory inventory, int slot) {
            super(PlacableItemType.STORAGE_CELLS, inventory, slot);
            this.host = host;
            this.cellSlot = slot;
            setStackLimit(1);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return host.isValidCell(stack) && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            var replacement = player.containerMenu.getCarried();
            if (!replacement.isEmpty() && !host.isValidCell(replacement)) {
                replacement = ItemStack.EMPTY;
            }
            return host.canReplaceCell(cellSlot, replacement) && super.mayPickup(player);
        }
    }

    public record StoredAmounts(long[] amounts) implements PacketWritable {

        public StoredAmounts() {
            this(new long[SuperMEReplenisherBlockEntity.CONFIG_SLOT_COUNT]);
        }

        public StoredAmounts(long[] amounts) {
            this.amounts = Arrays.copyOf(amounts, SuperMEReplenisherBlockEntity.CONFIG_SLOT_COUNT);
        }

        public StoredAmounts(FriendlyByteBuf data) {
            this(readFromPacket(data));
        }

        private static long[] readFromPacket(FriendlyByteBuf data) {
            var amounts = new long[SuperMEReplenisherBlockEntity.CONFIG_SLOT_COUNT];
            for (int i = 0; i < amounts.length; i++) {
                amounts[i] = data.readVarLong();
            }
            return amounts;
        }

        public long get(int slot) {
            return slot >= 0 && slot < amounts.length ? amounts[slot] : 0;
        }

        @Override
        public void writeToPacket(FriendlyByteBuf data) {
            for (var amount : amounts) {
                data.writeVarLong(amount);
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof StoredAmounts that && Arrays.equals(amounts, that.amounts);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(amounts);
        }
    }
}
