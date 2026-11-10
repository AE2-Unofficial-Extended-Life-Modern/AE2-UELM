package appeng.integration.modules.rei.transfer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.item.crafting.RecipeHolder;

import me.shedaniel.rei.api.common.display.Display;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.integration.modules.rei.GenericEntryStackHelper;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.util.helpers.FilterTransferHelper;

/**
 * Sets the config slots of storage buses, interfaces and export buses to the inputs of a recipe shown in REI.
 */
public class FilterTransferHandler<T extends UpgradeableMenu<? extends IUpgradeableObject>>
        extends AbstractTransferHandler<T> {

    public FilterTransferHandler(Class<T> containerClass) {
        super(containerClass);
    }

    @Override
    protected Result transferRecipe(T menu, @Nullable RecipeHolder<?> holder, Display display, boolean doTransfer) {
        if (doTransfer) {
            new FilterTransferHelper<T>().transfer(menu, GenericEntryStackHelper.ofInputs(display));
        }

        return Result.createSuccessful().blocksFurtherHandling();
    }
}
