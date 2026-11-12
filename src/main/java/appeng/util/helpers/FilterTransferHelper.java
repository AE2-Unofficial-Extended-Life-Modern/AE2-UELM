package appeng.util.helpers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.math.LongMath;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.RecipeTransferMode;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.core.AEConfig;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;

public class FilterTransferHelper<T extends UpgradeableMenu<? extends IUpgradeableObject>> {
    public static void addOrMerge(List<GenericStack> stacks, GenericStack newStack) {
        for (int i = 0; i < stacks.size(); i++) {
            var existingStack = stacks.get(i);
            if (Objects.equals(existingStack.what(), newStack.what())) {
                // Add the new amount onto the existing amount
                long newAmount = LongMath.saturatedAdd(existingStack.amount(), newStack.amount());
                stacks.set(i, new GenericStack(newStack.what(), newAmount));

                // Determine if the addition overflowed. If it did, add the remainder as a new
                // stack.
                long overflow = newStack.amount() - (newAmount - existingStack.amount());
                if (overflow > 0) {
                    stacks.add(new GenericStack(newStack.what(), overflow));
                }
                return;
            }
        }
        stacks.add(newStack);
    }

    private List<FakeSlot> getFakeSlots(T menu) {
        List<FakeSlot> slots = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot instanceof FakeSlot fs) {
                slots.add(fs);
            }
        }
        return slots;
    }

    void overwriteFilters(List<FakeSlot> interfaceConfig, List<GenericStack> inputsToSet) {
        int slotIdx = 0;
        for (FakeSlot slot : interfaceConfig) {
            var filter = (slotIdx < inputsToSet.size())
                    ? GenericStack.wrapInItemStack(inputsToSet.get(slotIdx))
                    : ItemStack.EMPTY;

            slot.setFilterTo(filter);
            slotIdx++;
        }
    }

    void addFilters(List<FakeSlot> interfaceConfig, List<GenericStack> inputsToSet) {
        int slotIdx = 0;
        for (FakeSlot slot : interfaceConfig) {
            if (slotIdx >= inputsToSet.size()) {
                break;
            }
            var filter = GenericStack.wrapInItemStack(inputsToSet.get(slotIdx));
            if (!slot.getItem().isEmpty()) {
                continue;
            }
            slot.setFilterTo(filter);
            slotIdx++;
        }
    }

    public void transfer(T menu, List<List<GenericStack>> recipeInputs) {
        ArrayList<GenericStack> inputsToSet = new ArrayList<>();
        for (List<GenericStack> genericIngredient : recipeInputs) {
            if (!genericIngredient.isEmpty()) {
                // Taking the first possible ingredient from the stack is a bit lazy, but
                // the number of use cases for autofilling tagged recipe inputs into an interface
                // has got to be in the single digits...

                // If someone else comes along later down the line and wants to make this better, the best thing
                // to do here would be to look at which of the candidate ingredients currently has the most stock
                // in the ME network connected to the device this screen belongs to.
                addOrMerge(inputsToSet, genericIngredient.get(0));
            }
        }
        List<FakeSlot> interfaceConfig = getFakeSlots(menu);
        RecipeTransferMode transferMode = AEConfig.instance().getRecipeTransferMode();
        switch (transferMode) {
            case OVERWRITE -> overwriteFilters(interfaceConfig, inputsToSet);
            case ADD -> addFilters(interfaceConfig, inputsToSet);
            case DO_NOTHING -> {
                if (interfaceConfig.stream().allMatch(filter -> filter.getItem().isEmpty())) {
                    addFilters(interfaceConfig, inputsToSet);
                }
            }
        }

    }
}
