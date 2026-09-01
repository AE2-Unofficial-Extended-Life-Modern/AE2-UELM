package appeng.menu.implementations;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.ItemStack;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;

import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEKey;
import appeng.helpers.externalstorage.GenericStackInv;

final class FilterTerminalEditValidation {

    private FilterTerminalEditValidation() {
    }

    @Nullable
    static <T> T findTarget(Long2ObjectMap<T> targets, long id) {
        return targets.get(id);
    }

    static boolean isValidTarget(@Nullable IGrid terminalGrid, IActionHost target) {
        if (terminalGrid == null) {
            return false;
        }

        var targetNode = target.getActionableNode();
        return targetNode != null && targetNode.isActive() && targetNode.getGrid() == terminalGrid;
    }

    static boolean matchesExpected(GenericStackInv inventory, int slot, @Nullable AEKey expectedKey) {
        return slot >= 0 && slot < inventory.size()
                && Objects.equals(inventory.getKey(slot), expectedKey);
    }

    static boolean canEditAmount(boolean supportsAmountEditing, GenericStackInv inventory, int slot,
            @Nullable AEKey expectedKey) {
        return supportsAmountEditing && expectedKey != null && matchesExpected(inventory, slot, expectedKey);
    }

    static void setFilter(GenericStackInv inventory, int slot, ItemStack stack) {
        inventory.createMenuWrapper().setItemDirect(slot, stack);
    }

    static long clampAmount(GenericStackInv inventory, AEKey key, long amount) {
        return Math.max(0, Math.min(amount, inventory.getMaxAmount(key)));
    }
}
