package appeng.client.gui.me.filterterminal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.google.common.collect.HashMultimap;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.guidebook.document.LytRect;
import appeng.client.guidebook.render.SimpleRenderContext;
import appeng.client.render.HighlightManager;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.core.localization.Tooltips;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.FilterTerminalActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.implementations.FilterTerminalMenu;

/**
 * most of the drawing logic and layout is copied from
 * {@link appeng.client.gui.me.patternaccess.PatternAccessTermScreen} with some adapatations for the two search bars
 */
public class FilterTerminalScreen extends AEBaseScreen<FilterTerminalMenu> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterTerminalScreen.class);

    private static final int GUI_WIDTH = 195;
    private static final int GUI_TOP_AND_BOTTOM_PADDING = 54;

    private static final int GUI_PADDING_X = 8;
    private static final int GUI_PADDING_Y = 6;

    private static final int GUI_HEADER_HEIGHT = 29;
    private static final int GUI_FOOTER_HEIGHT = 97;
    private static final int COLUMNS = 9;

    private static final int NAME_MARGIN_X = 2;
    private static final int ROW_HEIGHT = 18;
    private static final int SLOT_SIZE = 18;
    private static final int HIGHLIGHT_BUTTON_X = 152;
    private static final int TEXT_MAX_WIDTH = HIGHLIGHT_BUTTON_X - GUI_PADDING_X - NAME_MARGIN_X - 4;

    private static final Rect2i HEADER_BBOX = new Rect2i(0, 0, GUI_WIDTH, GUI_HEADER_HEIGHT);
    private static final Rect2i ROW_TEXT_TOP_BBOX = new Rect2i(0, 29, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_TEXT_MIDDLE_BBOX = new Rect2i(0, 65, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_TEXT_BOTTOM_BBOX = new Rect2i(0, 101, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_TOP_BBOX = new Rect2i(0, 47, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_MIDDLE_BBOX = new Rect2i(0, 83, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i ROW_INVENTORY_BOTTOM_BBOX = new Rect2i(0, 119, GUI_WIDTH, ROW_HEIGHT);
    private static final Rect2i FOOTER_BBOX = new Rect2i(0, 137, GUI_WIDTH, GUI_FOOTER_HEIGHT);

    private static final Comparator<PatternContainerGroup> GROUP_COMPARATOR = Comparator
            .comparing((PatternContainerGroup group) -> group.name().getString().toLowerCase(Locale.ROOT))
            .thenComparing(group -> group.icon() == null ? "" : group.icon().toTagGeneric().toString());

    private final FilterTerminalClientState state = new FilterTerminalClientState();
    private final HashMultimap<PatternContainerGroup, FilterTerminalRecord> byGroup = HashMultimap.create();
    private final List<PatternContainerGroup> groups = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private final List<FilterTerminalSlot> clientSlots = new ArrayList<>();
    private final List<HighlightButton> highlightButtons = new ArrayList<>();

    private final Scrollbar scrollbar;
    private final AETextField nameSearchField;
    private final AETextField configuredSearchField;
    private int visibleRows;

    public FilterTerminalScreen(FilterTerminalMenu menu, Inventory playerInventory, Component title,
            ScreenStyle style) {
        super(menu, playerInventory, title, style);
        scrollbar = widgets.addScrollBar("scrollbar");
        imageWidth = GUI_WIDTH;

        var terminalStyle = AEConfig.instance().getTerminalStyle();
        addToLeftToolbar(new SettingToggleButton<>(Settings.TERMINAL_STYLE, terminalStyle, this::toggleTerminalStyle));

        nameSearchField = widgets.addTextField("nameSearch");
        nameSearchField.setResponder(value -> refreshList());
        nameSearchField.setPlaceholder(GuiText.SearchPlaceholder.text());
        nameSearchField.setTooltipMessage(List.of(GuiText.FilterTerminalNameSearch.text()));

        configuredSearchField = widgets.addTextField("configuredSearch");
        configuredSearchField.setResponder(value -> refreshList());
        configuredSearchField.setPlaceholder(GuiText.SearchPlaceholder.text());
        configuredSearchField.setTooltipMessage(List.of(GuiText.FilterTerminalConfiguredSearch.text()));
    }

    @Override
    public void init() {
        clearClientSlots();
        highlightButtons.clear();

        visibleRows = config.getTerminalStyle().getRows(
                (height - GUI_HEADER_HEIGHT - GUI_FOOTER_HEIGHT - GUI_TOP_AND_BOTTOM_PADDING) / ROW_HEIGHT);
        imageHeight = GUI_HEADER_HEIGHT + GUI_FOOTER_HEIGHT + visibleRows * ROW_HEIGHT;

        super.init();

        for (var row = 0; row < visibleRows; row++) {
            var highlight = new HighlightButton();
            highlight.setVisibility(false);
            highlightButtons.add(addRenderableWidget(highlight));
        }

        setInitialFocus(nameSearchField);
        resetScrollbar();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        highlightButtons.forEach(button -> button.setVisibility(false));
        var scrollLevel = scrollbar.getCurrentScroll();
        for (var visibleRow = 0; visibleRow < visibleRows; visibleRow++) {
            var rowIndex = scrollLevel + visibleRow;
            if (rowIndex >= rows.size()) {
                break;
            }

            if (rows.get(rowIndex) instanceof GroupHeaderRow headerRow) {
                var button = highlightButtons.get(visibleRow);
                button.setTarget(headerRow.container);
                button.setPosition(leftPos + HIGHLIGHT_BUTTON_X,
                        topPos + GUI_HEADER_HEIGHT + visibleRow * ROW_HEIGHT + 1);
                button.setVisibility(true);
            }
        }
    }

    @Override
    public void drawFG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY) {
        clearClientSlots();

        var textColor = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        var scrollLevel = scrollbar.getCurrentScroll();

        for (var visibleRow = 0; visibleRow < visibleRows; visibleRow++) {
            var rowIndex = scrollLevel + visibleRow;
            if (rowIndex >= rows.size()) {
                break;
            }

            var row = rows.get(rowIndex);
            if (row instanceof SlotsRow slotsRow) {
                for (var column = 0; column < slotsRow.slots; column++) {
                    var logicalSlot = slotsRow.offset + column;
                    var slot = new FilterTerminalSlot(slotsRow.container, logicalSlot,
                            column * SLOT_SIZE + GUI_PADDING_X, (visibleRow + 1) * SLOT_SIZE + 12);
                    menu.addClientSideSlot(slot, null);
                    clientSlots.add(slot);
                }
            } else if (row instanceof GroupHeaderRow headerRow) {
                var group = headerRow.group;
                if (group.icon() != null) {
                    var renderContext = new SimpleRenderContext(LytRect.empty(), guiGraphics);
                    renderContext.renderItem(group.icon().getReadOnlyStack(),
                            GUI_PADDING_X + NAME_MARGIN_X, GUI_PADDING_Y + GUI_HEADER_HEIGHT + visibleRow * ROW_HEIGHT,
                            8, 8);
                }

                FormattedText displayName = group.name();
                var text = Language.getInstance().getVisualOrder(font.substrByWidth(displayName, TEXT_MAX_WIDTH - 10));
                guiGraphics.drawString(font, text, GUI_PADDING_X + NAME_MARGIN_X + 10,
                        GUI_PADDING_Y + GUI_HEADER_HEIGHT + visibleRow * ROW_HEIGHT, textColor, false);
            }
        }
    }

    @Override
    public void drawBG(GuiGraphics guiGraphics, int offsetX, int offsetY, int mouseX, int mouseY,
            float partialTicks) {
        blit(guiGraphics, offsetX, offsetY, HEADER_BBOX);

        var scrollLevel = scrollbar.getCurrentScroll();
        var currentY = offsetY + GUI_HEADER_HEIGHT;
        blit(guiGraphics, offsetX, currentY + visibleRows * ROW_HEIGHT, FOOTER_BBOX);

        for (var visibleRow = 0; visibleRow < visibleRows; visibleRow++) {
            var firstLine = visibleRow == 0;
            var lastLine = visibleRow == visibleRows - 1;
            var bbox = selectRowBackgroundBox(false, firstLine, lastLine);
            blit(guiGraphics, offsetX, currentY, bbox);

            var rowIndex = scrollLevel + visibleRow;
            if (rowIndex < rows.size() && rows.get(rowIndex) instanceof SlotsRow slotsRow) {
                var inventoryBox = selectRowBackgroundBox(true, firstLine, lastLine);
                bbox = new Rect2i(inventoryBox.getX(), inventoryBox.getY(),
                        GUI_PADDING_X + SLOT_SIZE * slotsRow.slots - 1, inventoryBox.getHeight());
                blit(guiGraphics, offsetX, currentY, bbox);
            }
            currentY += ROW_HEIGHT;
        }
    }

    @Override
    protected void slotClicked(@Nullable Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (slot instanceof FilterTerminalSlot interfaceSlot) {
            var expectedKey = interfaceSlot.getMachine().getInventory().getKey(interfaceSlot.slot);
            if (mouseButton == GLFW.GLFW_MOUSE_BUTTON_MIDDLE && expectedKey != null
                    && interfaceSlot.getMachine().supportsAmountEditing()) {
                NetworkHandler.instance().sendToServer(FilterTerminalActionPacket.openAmount(
                        interfaceSlot.getMachine().getServerId(), interfaceSlot.slot, expectedKey));
                return;
            }

            InventoryAction action = null;
            if (mouseButton == 1 && getEmptyingAction(slot, menu.getCarried()) != null) {
                action = InventoryAction.EMPTY_ITEM;
            } else if (clickType == ClickType.PICKUP) {
                action = mouseButton == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE
                        : InventoryAction.PICKUP_OR_SET_DOWN;
            }

            if (action != null) {
                NetworkHandler.instance().sendToServer(new FilterTerminalActionPacket(action,
                        interfaceSlot.getMachine().getServerId(), interfaceSlot.slot,
                        expectedKey));
            }
            return;
        }

        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 1 && nameSearchField.isMouseOver(x, y)) {
            nameSearchField.setValue("");
        } else if (button == 1 && configuredSearchField.isMouseOver(x, y)) {
            configuredSearchField.setValue("");
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean charTyped(char character, int key) {
        if (character == ' ' && (nameSearchField.isFocused() && nameSearchField.getValue().isEmpty()
                || configuredSearchField.isFocused() && configuredSearchField.getValue().isEmpty())) {
            return true;
        }
        return super.charTyped(character, key);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (this.hoveredSlot instanceof FilterTerminalSlot slot) {
            var configured = GenericStack.fromItemStack(slot.getItem());
            if (configured != null && slot.getMachine().supportsAmountEditing()) {
                var tooltip = new ArrayList<>(getTooltipFromContainerItem(slot.getItem()));
                tooltip.add(Tooltips.getAmountTooltip(ButtonToolTips.FilterTerminalStocked,
                        configured.what(), slot.getMachine().getStockedAmount(slot.slot)));
                tooltip.add(Tooltips.getSetAmountTooltip());
                drawTooltip(guiGraphics, x, y, tooltip);
                return;
            }
        }

        super.renderTooltip(guiGraphics, x, y);
    }

    public void clear() {
        state.clear();
        refreshList();
    }

    public void postFullUpdate(long inventoryId, int inventorySize, PatternContainerGroup group,
            ResourceKey<Level> dimension, BlockPos pos, @Nullable Direction side,
            boolean supportsAmountEditing, Int2ObjectMap<GenericStack> slots, Int2LongMap stockedAmounts) {
        state.putFull(inventoryId, inventorySize, group, dimension, pos, side, supportsAmountEditing, slots,
                stockedAmounts);
        refreshList();
    }

    public void postIncrementalUpdate(long inventoryId, Int2ObjectMap<GenericStack> slots,
            Int2LongMap stockedAmounts) {
        if (!state.applyIncremental(inventoryId, slots, stockedAmounts)) {
            LOGGER.warn("Ignoring incremental update for unknown inventory id {}", inventoryId);
            return;
        }
        refreshList();
    }

    private void refreshList() {
        byGroup.clear();
        var nameSearch = nameSearchField.getValue();
        var configuredSearch = configuredSearchField.getValue();

        for (var record : state.records()) {
            if (FilterTerminalSearch.matches(record, nameSearch, configuredSearch)) {
                byGroup.put(record.getGroup(), record);
            }
        }

        groups.clear();
        groups.addAll(byGroup.keySet());
        groups.sort(GROUP_COMPARATOR);

        rows.clear();
        for (var group : groups) {
            var containers = new ArrayList<>(byGroup.get(group));
            Collections.sort(containers);
            for (var container : containers) {
                rows.add(new GroupHeaderRow(group, container));
                for (var offset = 0; offset < container.getInventory().size(); offset += COLUMNS) {
                    var slots = Math.min(container.getInventory().size() - offset, COLUMNS);
                    rows.add(new SlotsRow(container, offset, slots));
                }
            }
        }
        resetScrollbar();
    }

    private void resetScrollbar() {
        scrollbar.setHeight(visibleRows * ROW_HEIGHT - 2);
        scrollbar.setRange(0, rows.size() - visibleRows, 2);
    }

    private void clearClientSlots() {
        for (var i = clientSlots.size() - 1; i >= 0; i--) {
            menu.removeClientSideSlot(clientSlots.get(i));
        }
        clientSlots.clear();
    }

    private Rect2i selectRowBackgroundBox(boolean inventory, boolean first, boolean last) {
        if (inventory) {
            return first ? ROW_INVENTORY_TOP_BBOX : last ? ROW_INVENTORY_BOTTOM_BBOX : ROW_INVENTORY_MIDDLE_BBOX;
        }
        return first ? ROW_TEXT_TOP_BBOX : last ? ROW_TEXT_BOTTOM_BBOX : ROW_TEXT_MIDDLE_BBOX;
    }

    private void blit(GuiGraphics guiGraphics, int offsetX, int offsetY, Rect2i source) {
        var texture = AppEng.makeId("textures/guis/filterterminal.png");
        guiGraphics.blit(texture, offsetX, offsetY, source.getX(), source.getY(), source.getWidth(),
                source.getHeight());
    }

    private void reinitialize() {
        children().removeAll(renderables);
        renderables.clear();
        init();
    }

    private void toggleTerminalStyle(SettingToggleButton<TerminalStyle> button, boolean backwards) {
        var next = button.getNextValue(backwards);
        AEConfig.instance().setTerminalStyle(next);
        button.set(next);
        reinitialize();
    }

    private sealed interface Row {
    }

    private record GroupHeaderRow(PatternContainerGroup group, FilterTerminalRecord container) implements Row {
    }

    private record SlotsRow(FilterTerminalRecord container, int offset, int slots) implements Row {
    }

    private static final class HighlightButton extends IconButton {
        @Nullable
        private FilterTerminalRecord target;

        private HighlightButton() {
            super(button -> ((HighlightButton) button).highlight());
            setMessage(ButtonToolTips.FilterTerminalHighlight.text());
        }

        private void setTarget(FilterTerminalRecord target) {
            this.target = target;
        }

        private void highlight() {
            var player = Minecraft.getInstance().player;
            if (target == null || player == null) {
                return;
            }

            HighlightManager.highlight(target.getDimension(), target.getPos(), target.getSide());
            player.displayClientMessage(PlayerMessages.FilterTerminalHighlighted.text(
                    target.getPos().toShortString(), target.getDimension().location().toString()), false);

        }

        @Override
        protected Icon getIcon() {
            return Icon.HIGHLIGHT;
        }
    }
}
