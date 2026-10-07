package appeng.menu.implementations;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import appeng.api.filterterminal.IFilterTerminalConfigView;
import appeng.api.filterterminal.IFilterTerminalTarget;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.helpers.externalstorage.GenericStackInv;

final class FilterTerminalEditValidation {

    private FilterTerminalEditValidation() {
    }

    @Nullable
    static <T> T findTarget(Long2ObjectMap<T> targets, long id) {
        return targets.get(id);
    }

    static boolean isValidTarget(@Nullable IGrid terminalGrid, IFilterTerminalTarget target) {
        if (terminalGrid == null) {
            return false;
        }

        var targetNode = target.getGridNode();
        return targetNode != null && targetNode.isActive() && targetNode.getGrid() == terminalGrid;
    }

    static boolean matchesExpected(IFilterTerminalConfigView view, int slot, @Nullable AEKey expectedKey) {
        if (slot < 0 || slot >= view.size()) {
            return false;
        }

        var configured = view.getConfig(slot);
        return Objects.equals(configured == null ? null : configured.what(), expectedKey);
    }

    static boolean canEditAmount(IFilterTerminalConfigView view, int slot, @Nullable AEKey expectedKey) {
        return expectedKey != null && matchesExpected(view, slot, expectedKey) && view.canEditAmount(slot);
    }

    static boolean setFilter(IFilterTerminalConfigView view, int slot, ItemStack stack,
            @Nullable AEKey expectedKey) {
        var workingInventory = createWorkingInventory(view, slot);
        workingInventory.createMenuWrapper().setItemDirect(0, stack);
        return setConfig(view, slot, expectedKey, workingInventory.getStack(0));
    }

    static boolean setConfig(IFilterTerminalConfigView view, int slot, @Nullable AEKey expectedKey,
            @Nullable GenericStack stack) {
        // Permissions and the expected key are server-authoritative and must be checked immediately before writing.
        if (!matchesExpected(view, slot, expectedKey) || !view.canSetConfig(slot, stack)) {
            return false;
        }

        view.setConfig(slot, stack);
        return true;
    }

    static GenericStackInv createWorkingInventory(IFilterTerminalConfigView view, int slot) {
        var mode = view.canEditAmount(slot) ? GenericStackInv.Mode.CONFIG_STACKS : GenericStackInv.Mode.CONFIG_TYPES;
        return new WorkingInventory(view, slot, mode);
    }

    static long clampAmount(IFilterTerminalConfigView view, int slot, AEKey key, long amount) {
        return Math.max(0, Math.min(amount, view.getMaxAmount(slot, key)));
    }

    private static final class WorkingInventory extends GenericStackInv {

        private final IFilterTerminalConfigView view;
        private final int targetSlot;

        private WorkingInventory(IFilterTerminalConfigView view, int targetSlot, Mode mode) {
            super(null, mode, 1);
            this.view = view;
            this.targetSlot = targetSlot;
            stacks[0] = view.getConfig(targetSlot);
        }

        @Override
        public long getMaxAmount(AEKey key) {
            return view.getMaxAmount(targetSlot, key);
        }
    }
}
