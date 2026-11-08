package appeng.hotkeys;

import net.minecraft.world.entity.player.Player;

import appeng.api.features.HotkeyAction;

public class PinHotkeyAction implements HotkeyAction {
    @Override
    public boolean run(Player player) {
        // no-op
        return false;
    }
}
