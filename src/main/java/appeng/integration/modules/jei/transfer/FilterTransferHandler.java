package appeng.integration.modules.jei.transfer;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.integration.modules.jei.GenericEntryStackHelper;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.util.helpers.FilterTransferHelper;

public class FilterTransferHandler<T extends UpgradeableMenu<? extends IUpgradeableObject>>
        extends AbstractTransferHandler implements IRecipeTransferHandler<T, Object> {

    private final Class<T> menuClass;

    public FilterTransferHandler(MenuType<T> menuType, Class<T> menuClass, IRecipeTransferHandlerHelper helper) {

        this.menuClass = menuClass;

    }

    @Nullable
    @Override
    public IRecipeTransferError transferRecipe(T menu, Object recipeBase, IRecipeSlotsView slotsView, Player player,
            boolean maxTransfer, boolean doTransfer) {
        if (doTransfer) {
            var recipeInputs = GenericEntryStackHelper.ofInputs(slotsView);
            FilterTransferHelper<T> helper = new FilterTransferHelper<>();
            helper.transfer(menu, recipeInputs);
        }
        return null;
    }

    // Returning empty means the handler will not limit itself to a single MenuType from any given container class.
    @Override
    public Optional<MenuType<T>> getMenuType() {
        return Optional.empty();
    }

    // Just as with the EncodePatternTransferHelper, this will never be used
    // due to being registered as a universal transfer handler.
    @Override
    public RecipeType<Object> getRecipeType() {
        return null;
    }

    @Override
    public Class<? extends T> getContainerClass() {
        return menuClass;
    }
}
