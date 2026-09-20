package appeng.menu.implementations;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.locator.MenuLocator;
import appeng.menu.slot.InaccessibleSlot;
import appeng.parts.reporting.FilterTerminalPart;
import appeng.util.inv.AppEngInternalInventory;

/**
 * basically an adapted copy of {@link appeng.menu.implementations.SetStockAmountMenu}
 * <p>
 * also see {@link appeng.client.gui.me.filterterminal.FilterTerminalSetAmountScreen}
 */
public class FilterTerminalSetAmountMenu extends AEBaseMenu implements ISubMenu {

    public static final String ACTION_SET_AMOUNT = "setAmount";

    public static final MenuType<FilterTerminalSetAmountMenu> TYPE = MenuTypeBuilder
            .create(FilterTerminalSetAmountMenu::new, FilterTerminalPart.class)
            .build("filter_terminal_set_amount");

    private final FilterTerminalPart host;
    private final Slot configuredStack;

    @Nullable
    private IFilterTerminalTarget target;
    @Nullable
    private AEKey expectedKey;
    private int targetSlot = -1;

    @GuiSync(1)
    private long initialAmount = -1;
    @GuiSync(2)
    private long maxAmount = -1;

    public FilterTerminalSetAmountMenu(int id, Inventory playerInventory, FilterTerminalPart host) {
        super(TYPE, id, playerInventory, host);
        this.host = host;
        this.configuredStack = new InaccessibleSlot(new AppEngInternalInventory(1), 0);
        addSlot(configuredStack, SlotSemantics.MACHINE_OUTPUT);
        registerClientAction(ACTION_SET_AMOUNT, Long.class, this::confirm);
    }

    public static void open(ServerPlayer player, MenuLocator terminalLocator, IFilterTerminalTarget target, int slot,
            GenericStack configured) {
        MenuOpener.open(TYPE, player, terminalLocator);
        if (player.containerMenu instanceof FilterTerminalSetAmountMenu menu) {
            menu.configureTarget(target, slot, configured);
            menu.broadcastChanges();
        }
    }

    private void configureTarget(IFilterTerminalTarget target, int slot, GenericStack configured) {
        this.target = Objects.requireNonNull(target);
        this.targetSlot = slot;
        this.expectedKey = configured.what();
        this.initialAmount = configured.amount();
        this.maxAmount = target.getConfigView().getMaxAmount(slot, configured.what());
        this.configuredStack.set(configured.what().wrapForDisplayOrFilter());
    }

    public void confirm(long amount) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_AMOUNT, amount);
            return;
        }

        var player = (ServerPlayer) getPlayer();
        if (!isTargetValid(player)) {
            host.returnToMainMenu(getPlayer(), this);
            return;
        }

        var view = target.getConfigView();
        if (!FilterTerminalEditValidation.canEditAmount(view, targetSlot, expectedKey)) {
            host.returnToMainMenu(getPlayer(), this);
            return;
        }

        amount = FilterTerminalEditValidation.clampAmount(view, targetSlot, expectedKey, amount);
        var resultingStack = amount <= 0 ? null : new GenericStack(expectedKey, amount);
        if (!FilterTerminalEditValidation.setConfig(view, targetSlot, expectedKey, resultingStack)) {
            host.returnToMainMenu(getPlayer(), this);
            return;
        }
        host.returnToMainMenu(getPlayer(), this);
    }

    private boolean isTargetValid(ServerPlayer player) {
        if (target == null || expectedKey == null || targetSlot < 0
                || targetSlot >= target.getConfigView().size()) {
            return false;
        }

        var terminalNode = host.getActionableNode();
        return terminalNode != null && terminalNode.isActive()
                && FilterTerminalEditValidation.isValidTarget(terminalNode.getGrid(), target)
                && target.canEdit(player);
    }

    @Override
    public FilterTerminalPart getHost() {
        return host;
    }

    public long getInitialAmount() {
        return initialAmount;
    }

    public long getMaxAmount() {
        return maxAmount;
    }

    @Nullable
    public AEKey getConfiguredKey() {
        var stack = GenericStack.fromItemStack(configuredStack.getItem());
        return stack == null ? null : stack.what();
    }
}
