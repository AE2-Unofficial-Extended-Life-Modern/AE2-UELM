package appeng.api.filterterminal;

import org.jetbrains.annotations.Nullable;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

/**
 * A slot-based view of a filter terminal target's configuration and current stock.
 * <p>
 * Configured stacks describe what a slot is set to filter or maintain. Stocked stacks describe what the target
 * currently holds for that slot
 */
public interface IFilterTerminalConfigView {

    /**
     * @return the number of slots exposed by this view
     */
    int size();

    /**
     * @return the configured stack, or null if the slot is not configured
     */
    @Nullable
    GenericStack getConfig(int slot);

    /**
     * @return the stack currently stocked for the slot, or null if none is stocked
     */
    @Nullable
    GenericStack getStock(int slot);

    /**
     * Checks whether the configured stack can be changed. Must be rechecked on the server before applying a client
     * request.
     *
     * @param stack the proposed stack, or null to clear the slot
     */
    boolean canSetConfig(int slot, @Nullable GenericStack stack);

    /**
     * Changes the configured stack. Callers must first recheck the applicable permission on the server.
     *
     * @param stack the new stack, or null to clear the slot
     */
    void setConfig(int slot, @Nullable GenericStack stack);

    /**
     * Checks whether the configured amount can be changed without changing the key. Must be rechecked on the server
     * before applying a client request.
     */
    default boolean canEditAmount(int slot) {
        return false;
    }

    /**
     * @return the maximum configurable amount for the key in this slot
     */
    default long getMaxAmount(int slot, AEKey key) {
        return Long.MAX_VALUE;
    }
}
