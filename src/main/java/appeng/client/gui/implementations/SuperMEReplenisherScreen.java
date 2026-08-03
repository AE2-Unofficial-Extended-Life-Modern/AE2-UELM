package appeng.client.gui.implementations;

import java.awt.Color;
import java.text.NumberFormat;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;
import appeng.menu.implementations.SuperMEReplenisherMenu;
import appeng.menu.slot.FakeSlot;
import appeng.util.NumberUtil;

public class SuperMEReplenisherScreen extends AEBaseScreen<SuperMEReplenisherMenu> {
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getIntegerInstance();

    private final AETextField tickRateField;
    private final AETextField thresholdField;
    private boolean fieldsInitialized;

    public SuperMEReplenisherScreen(SuperMEReplenisherMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);

        tickRateField = widgets.addTextField("tickRate");
        tickRateField.setMaxLength(5);
        thresholdField = widgets.addTextField("threshold");
        thresholdField.setMaxLength(3);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!fieldsInitialized) {
            tickRateField.setValue(Integer.toString(menu.tickRate));
            thresholdField.setValue(Integer.toString(menu.threshold));
            fieldsInitialized = true;
        } else {
            if (!tickRateField.isFocused()) {
                updateField(tickRateField, menu.tickRate);
            }
            if (!thresholdField.isFocused()) {
                updateField(thresholdField, menu.threshold);
            }
        }

        setTextContent("bytes_total", GuiText.SuperMEReplenisherBytesTotal.text(
                menu.totalBytes == Long.MAX_VALUE
                        ? rainbow(GuiText.SuperMEReplenisherUnlimited.getLocal())
                        : format(menu.totalBytes).copy().withStyle(ChatFormatting.RED)));
        setTextContent("bytes_used_total", GuiText.SuperMEReplenisherBytesUsed.text(
                NumberUtil.coloredText(format(menu.usedBytes), (double) menu.usedBytes / menu.totalBytes)));

        var target = getHoveredTarget();
        if (target != null && hoveredSlot != null) {
            var stored = menu.getStoredAmount(hoveredSlot.getSlotIndex());
            var amountPerByte = Math.max(1, target.what().getType().getAmountPerByte());
            var bytes = stored / amountPerByte + (stored % amountPerByte == 0 ? 0 : 1);
            setTextContent("target", GuiText.SuperMEReplenisherTarget.text(format(target.amount())
                    .copy().withStyle(ChatFormatting.DARK_PURPLE)));
            setTextContent("stored", GuiText.SuperMEReplenisherStored.text(format(stored)
                    .copy()
                    .withStyle(stored >= target.amount() ? ChatFormatting.DARK_PURPLE : ChatFormatting.LIGHT_PURPLE)));
            setTextContent("bytes_used_target", GuiText.SuperMEReplenisherBytesUsed.text(
                    NumberUtil.coloredText(format(bytes), (double) bytes / menu.totalBytes)));
        } else {
            setTextContent("target", Component.empty());
            setTextContent("stored", Component.empty());
            setTextContent("bytes_used_target", Component.empty());
        }
    }

    private static void updateField(AETextField field, int value) {
        var text = Integer.toString(value);
        if (!field.getValue().equals(text)) {
            field.setValue(text);
        }
    }

    @Nullable
    private GenericStack getHoveredTarget() {
        if (!(hoveredSlot instanceof FakeSlot)
                || menu.getSlotSemantic(hoveredSlot) != SlotSemantics.CONFIG) {
            return null;
        }
        return GenericStack.fromItemStack(hoveredSlot.getItem());
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_MIDDLE
                && slot instanceof FakeSlot
                && menu.getSlotSemantic(slot) == SlotSemantics.CONFIG
                && slot.hasItem()) {
            menu.openSetAmountMenu(slot.getSlotIndex());
            return;
        }
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER)
                && (tickRateField.isFocused() || thresholdField.isFocused())) {
            saveSettings();
            tickRateField.setFocused(false);
            thresholdField.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        saveSettings();
        super.onClose();
    }

    private void saveSettings() {
        menu.setTickRate(parse(tickRateField.getValue(), 1, 10_000));
        menu.setThreshold(parse(thresholdField.getValue(), 1, 100));
    }

    private static int parse(String value, int minimum, int maximum) {
        try {
            return Math.max(minimum, Math.min(maximum, Integer.parseInt(value)));
        } catch (NumberFormatException ignored) {
            return minimum;
        }
    }

    private static Component format(long value) {
        return Component.literal(NUMBER_FORMAT.format(value));
    }

    // funny colors
    public static Component rainbow(String text) {
        float offset = 1.0F - (System.currentTimeMillis() % 5000L) / 5000.0F;
        MutableComponent result = Component.empty();

        int visibleIndex = 0;

        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);

            if (Character.isWhitespace(character)) {
                result.append(Component.literal(String.valueOf(character)));
                continue;
            }

            float hue = (offset + visibleIndex * 0.08F) % 1.0F;
            int rgb = Color.HSBtoRGB(hue, 1.0F, 1.0F) & 0xFFFFFF;

            result.append(
                    Component.literal(String.valueOf(character))
                            .withStyle(Style.EMPTY.withColor(rgb)));

            visibleIndex++;
        }

        return result;
    }
}
