package appeng.client.gui.me.filterterminal;

import net.minecraft.world.item.ItemStack;

import appeng.api.stacks.GenericStack;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.FilterTerminalSetFilterPacket;
import appeng.menu.slot.FakeSlot;

public class FilterTerminalSlot extends FakeSlot {

    private final FilterTerminalRecord machine;

    public FilterTerminalSlot(FilterTerminalRecord machine, int machineSlot, int x, int y) {
        super(machine.getMenuInventory(), machineSlot);
        this.machine = machine;
        this.x = x;
        this.y = y;
    }

    public FilterTerminalRecord getMachine() {
        return machine;
    }

    @Override
    public boolean canSetFilterTo(ItemStack stack) {
        return machine.canEditConfig(slot) && super.canSetFilterTo(stack);
    }

    @Override
    public void setFilterTo(ItemStack stack) {
        if (!machine.canEditConfig(slot)) {
            return;
        }
        NetworkHandler.instance().sendToServer(
                new FilterTerminalSetFilterPacket(machine.getServerId(), slot, stack,
                        machine.getInventory().getKey(slot)));
    }

    @Override
    public void set(ItemStack stack) {
    }

    @Override
    public ItemStack getDisplayStack() {
        var configured = machine.getInventory().getStack(slot);

        if (configured != null && !machine.canEditAmount(slot)) {
            var stocked = machine.getStockedAmount(slot);

            if (stocked > 0) {
                return GenericStack.wrapInItemStack(configured.what(), stocked);
            }
        }

        return super.getDisplayStack();
    }
}
