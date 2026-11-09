package appeng.client.gui.me.filterterminal;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import appeng.core.network.serverbound.FilterTerminalSetFilterPacket;
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
        PacketDistributor.sendToServer(
                new FilterTerminalSetFilterPacket(machine.getServerId(), slot, stack,
                        machine.getInventory().getKey(slot)));
    }

    @Override
    public void set(ItemStack stack) {
    }
}
