package appeng.menu.implementations;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.IStockAmountHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuLocator;
import appeng.menu.slot.InaccessibleSlot;
import appeng.util.inv.AppEngInternalInventory;

/**
 * Allows precisely setting the amount to stock for an interface slot.
 *
 * @see appeng.client.gui.me.crafting.SetStockAmountScreen
 */
public class SetStockAmountMenu extends AEBaseMenu implements ISubMenu {

    public static final MenuType<SetStockAmountMenu> TYPE = MenuTypeBuilder
            .create(SetStockAmountMenu::new, IStockAmountHost.class)
            .build("set_stock_amount");

    public static final String ACTION_SET_STOCK_AMOUNT = "setStockAmount";

    /**
     * This slot is used to synchronize a visual representation of what is to be stocked to the client.
     */
    private final Slot stockedItem;

    /**
     * This item (server-only) indicates what should actually be crafted.
     */
    private AEKey whatToStock;

    @GuiSync(1)
    private long initialAmount = -1;

    @GuiSync(2)
    private long maxAmount = -1;

    private int slot;

    private final IStockAmountHost host;

    public SetStockAmountMenu(int id, Inventory ip, IStockAmountHost host) {
        super(TYPE, id, ip, host);
        registerClientAction(ACTION_SET_STOCK_AMOUNT, Long.class, this::confirm);
        this.host = host;
        this.stockedItem = new InaccessibleSlot(new AppEngInternalInventory(1), 0);
        this.addSlot(this.stockedItem, SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    public IStockAmountHost getHost() {
        return host;
    }

    /**
     * Opens the screen to enter the stocked amount for the given player.
     */
    public static void open(ServerPlayer player, MenuLocator locator,
            int slot,
            AEKey whatToStock, long initialAmount) {
        MenuOpener.open(SetStockAmountMenu.TYPE, player, locator);

        if (player.containerMenu instanceof SetStockAmountMenu cca) {
            cca.setWhatToStock(slot, whatToStock, initialAmount);
            cca.broadcastChanges();
        }
    }

    public Level getLevel() {
        return this.getPlayerInventory().player.level();
    }

    private void setWhatToStock(int slot, AEKey whatToStock, long initialAmount) {
        this.slot = slot;
        this.whatToStock = Objects.requireNonNull(whatToStock, "whatToStock");
        this.initialAmount = initialAmount;
        this.maxAmount = host.getConfig().getMaxAmount(whatToStock);
        this.stockedItem.set(whatToStock.wrapForDisplayOrFilter());
    }

    public long getMaxAmount() {
        return maxAmount;
    }

    /**
     * Changes the amount to be stocked.
     *
     * @param amount The number of items to stock.
     */
    public void confirm(long amount) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_STOCK_AMOUNT, amount);
            return;
        }

        var config = host.getConfig();

        // In case the config changed don't set anything
        if (!Objects.equals(config.getKey(this.slot), whatToStock)) {
            host.returnToMainMenu(getPlayer(), this);
            return;
        }

        amount = Math.min(amount, config.getMaxAmount(whatToStock));

        if (amount <= 0) {
            config.setStack(slot, null);
        } else {
            config.setStack(slot, new GenericStack(whatToStock, amount));
        }
        host.returnToMainMenu(getPlayer(), this);
    }

    public long getInitialAmount() {
        return initialAmount;
    }

    @Nullable
    public AEKey getWhatToStock() {
        var stack = GenericStack.fromItemStack(stockedItem.getItem());
        return stack != null ? stack.what() : null;
    }
}
