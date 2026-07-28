package appeng.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import appeng.api.client.AEKeyRendering;
import appeng.crafting.pattern.PatternTooltipComponent;

public record PatternClientTooltipComponent(PatternTooltipComponent component) implements ClientTooltipComponent {
    private static final int ICON_SIZE = 8;

    @Override
    public int getHeight() {
        return 0; // we render over the existing tooltip so no need for an extra area
    }

    @Override
    public int getWidth(Font font) {
        return ICON_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        x += 1; // center it between the border and text
        var mc = Minecraft.getInstance();
        int lineHeight = mc.font.lineHeight + 1;
        int currentY = y + lineHeight * 2;
        var poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.scale(.5f, .5f, 1); // halve the icon size
        for (var key : component.outputs()) {
            AEKeyRendering.drawInGui(mc, guiGraphics, x * 2, currentY * 2, key);
            currentY += lineHeight;
        }
        currentY += lineHeight;
        for (var key : component.inputs()) {
            AEKeyRendering.drawInGui(mc, guiGraphics, x * 2, currentY * 2, key);
            currentY += lineHeight;
        }
        poseStack.popPose();
    }
}
