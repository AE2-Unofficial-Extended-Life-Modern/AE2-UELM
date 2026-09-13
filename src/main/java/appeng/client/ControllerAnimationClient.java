package appeng.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import appeng.core.AEConfig;
import appeng.core.localization.PlayerMessages;

@OnlyIn(Dist.CLIENT)
public final class ControllerAnimationClient {

    public static final String HOTKEY_ID = "cycle_controller_animation";

    private ControllerAnimationClient() {
    }

    public static void cycle() {
        var config = AEConfig.instance();

        var next = config
                .getControllerAnimation()
                .next();

        config.setControllerAnimation(next);

        var player = Minecraft.getInstance().player;

        if (player != null) {
            player.displayClientMessage(
                    PlayerMessages.ControllerAnimation.text(next.getTranslationKey()),
                    true);
        }
    }
}
