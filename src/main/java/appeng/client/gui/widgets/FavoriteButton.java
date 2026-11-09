package appeng.client.gui.widgets;

import java.util.List;

import net.minecraft.network.chat.Component;

import appeng.client.gui.Icon;
import appeng.client.gui.me.common.TerminalFavorites;
import appeng.core.localization.ButtonToolTips;

public class FavoriteButton extends IconButton {
    private String favoriteKey = "";

    public FavoriteButton(Runnable onToggle) {
        super(button -> {
            TerminalFavorites.toggle(((FavoriteButton) button).favoriteKey);
            onToggle.run();
        });
    }

    public void setFavoriteKey(String favoriteKey) {
        this.favoriteKey = favoriteKey;
    }

    @Override
    protected Icon getIcon() {
        return TerminalFavorites.isFavorite(favoriteKey) ? Icon.FAVORITE_ON : Icon.FAVORITE_OFF;
    }

    @Override
    public List<Component> getTooltipMessage() {
        var title = TerminalFavorites.isFavorite(favoriteKey) ? ButtonToolTips.RemoveFavorite
                : ButtonToolTips.AddFavorite;
        return List.of(title.text(), ButtonToolTips.FavoriteHint.text());
    }
}
