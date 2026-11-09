package appeng.client.gui.me.filterterminal;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.core.localization.GuiText;
import appeng.menu.implementations.FilterTerminalSetAmountMenu;

public class FilterTerminalSetAmountScreen extends AEBaseScreen<FilterTerminalSetAmountMenu> {

    private final NumberEntryWidget amount;
    private boolean amountInitialized;

    public FilterTerminalSetAmountScreen(FilterTerminalSetAmountMenu menu, Inventory playerInventory,
            Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        widgets.addButton("save", GuiText.Set.text(), this::confirm);
        AESubScreen.addBackButton(menu, "back", widgets);

        amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.UNITLESS);
        amount.setLongValue(1);
        amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        amount.setMinValue(0);
        amount.setHideValidationIcon(true);
        amount.setOnConfirm(this::confirm);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!amountInitialized) {
            var key = menu.getConfiguredKey();
            if (key != null) {
                amount.setType(NumberEntryType.of(key));
                amount.setLongValue(menu.getInitialAmount());
                amount.setMaxValue(menu.getMaxAmount());
                amountInitialized = true;
            }
        }
    }

    private void confirm() {
        amount.getLongValue().ifPresent(menu::confirm);
    }
}
