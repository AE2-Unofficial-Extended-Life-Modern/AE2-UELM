package appeng.client.gui.me.filterterminal;

import net.minecraft.world.item.ItemStack;

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
    public void setFilterTo(ItemStack stack) {
        NetworkHandler.instance().sendToServer(
                new FilterTerminalSetFilterPacket(machine.getServerId(), slot, stack,
                        machine.getInventory().getKey(slot)));
    }

    @Override
    public void set(ItemStack stack) {
    }
}
