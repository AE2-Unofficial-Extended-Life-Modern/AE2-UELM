package appeng.blockentity.networking;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import appeng.api.implementations.blockentities.IColorableBlockEntity;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.events.GridControllerChange;
import appeng.api.networking.events.GridControllerStructureChanged;
import appeng.api.util.AEColor;
import appeng.block.networking.ColorableControllerBlock;
import appeng.block.networking.ControllerColor;
import appeng.core.definitions.AEBlocks;
import appeng.util.SettingsFrom;

public class ColorableControllerBlockEntity extends ControllerBlockEntity implements IColorableBlockEntity {

    private static final String TAG_COLOR = "color";

    private AEColor color = AEColor.TRANSPARENT;

    static {
        GridHelper.addNodeOwnerEventHandler(
                GridControllerChange.class,
                ColorableControllerBlockEntity.class,
                ColorableControllerBlockEntity::updateState);
    }

    public ColorableControllerBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState blockState) {
        super(blockEntityType, pos, blockState);
        if (blockState.hasProperty(ColorableControllerBlock.COLOR)) {
            this.color = blockState.getValue(ColorableControllerBlock.COLOR).getColor();
        }
        applyInitializationColor();
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains(TAG_COLOR)) {
            this.color = readColor(data);
        } else if (!data.contains("visual", Tag.TAG_COMPOUND)) {
            this.color = AEColor.TRANSPARENT;
        }
        applyInitializationColor();
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putString(TAG_COLOR, this.color.name());
    }

    @Override
    protected void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeEnum(this.color);
    }

    @Override
    protected boolean readFromStream(FriendlyByteBuf data) {
        var changed = super.readFromStream(data);
        var oldColor = this.color;
        this.color = data.readEnum(AEColor.class);
        return changed || oldColor != this.color;
    }

    @Override
    protected void saveVisualState(CompoundTag data) {
        super.saveVisualState(data);
        data.putString(TAG_COLOR, this.color.name());
    }

    @Override
    protected void loadVisualState(CompoundTag data) {
        super.loadVisualState(data);
        if (data.contains(TAG_COLOR)) {
            this.color = readColor(data);
        }
    }

    @Override
    public AEColor getColor() {
        return this.color;
    }

    @Override
    public boolean recolourBlock(Direction side, AEColor newColor, Player who) {
        if (newColor == null || this.color == newColor) {
            return false;
        }

        if (this.level != null && this.level.isClientSide) {
            this.color = newColor;
            synchronizeBlockState();
            this.requestModelDataUpdate();
            return true;
        }

        return changeColor(newColor);
    }

    public static AEColor readColor(@Nullable CompoundTag data) {
        if (data == null || !data.contains(TAG_COLOR, Tag.TAG_STRING)) {
            return AEColor.TRANSPARENT;
        }

        try {
            return AEColor.valueOf(data.getString(TAG_COLOR).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return AEColor.TRANSPARENT;
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            output.putString(TAG_COLOR, this.color.name());
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode != SettingsFrom.DISMANTLE_ITEM) {
            return;
        }

        var importedColor = readColor(input);
        if (this.color == importedColor) {
            return;
        }

        if (this.level != null && !this.level.isClientSide && this.getMainNode().isReady()) {
            changeColor(importedColor);
        } else {
            this.color = importedColor;
            applyInitializationColor();
            synchronizeBlockState();
        }
    }

    @Override
    public void onReady() {
        synchronizeBlockState();
        super.onReady();
        refreshRenderTypes();
    }

    private boolean changeColor(AEColor newColor) {
        if (newColor == null || this.color == newColor) {
            return false;
        }

        var affectedNodes = captureAffectedNodes();
        var adjacentControllers = captureAdjacentControllers(affectedNodes);

        this.color = newColor;
        synchronizeBlockState();
        applyGridColor();

        if (this.level != null) {
            postStructureChanged(affectedNodes, adjacentControllers);
            saveChanges();
            markForUpdate();
            refreshRenderTypes();
        }

        return true;
    }

    private void applyInitializationColor() {
        if (this.level == null || !this.level.isClientSide) {
            applyGridColor();
        }
    }

    private void applyGridColor() {
        try {
            this.getMainNode().setGridColor(this.color);
        } catch (IllegalStateException ignored) {
            // NO-OP
        }
    }

    private void synchronizeBlockState() {
        if (this.level == null || this.getBlockState().getBlock() != AEBlocks.COLORABLE_CONTROLLER.block()) {
            return;
        }

        var currentState = this.getBlockState();
        var desiredState = currentState.setValue(ColorableControllerBlock.COLOR,
                ControllerColor.fromAEColor(this.color));
        if (currentState != desiredState) {
            this.level.setBlock(this.worldPosition, desiredState, Block.UPDATE_CLIENTS);
        }
    }

    private Set<IGridNode> captureAffectedNodes() {
        Set<IGridNode> nodes = identitySet();
        var node = this.getGridNode();
        if (node == null) {
            return nodes;
        }

        nodes.add(node);
        for (var connection : node.getConnections()) {
            nodes.add(connection.getOtherSide(node));
        }
        return nodes;
    }

    private Set<ControllerBlockEntity> captureAdjacentControllers(Set<IGridNode> affectedNodes) {
        Set<ControllerBlockEntity> controllers = identitySet();
        if (this.level == null) {
            return controllers;
        }

        for (var direction : Direction.values()) {
            var pos = this.worldPosition.relative(direction);
            if (!this.level.hasChunkAt(pos)) {
                continue;
            }

            if (this.level.getBlockEntity(pos) instanceof ControllerBlockEntity controller) {
                controllers.add(controller);
                var node = controller.getGridNode();
                if (node != null) {
                    affectedNodes.add(node);
                }
            }
        }
        return controllers;
    }

    private void postStructureChanged(Set<IGridNode> affectedNodes, Set<ControllerBlockEntity> adjacentControllers) {
        Set<IGrid> grids = identitySet();
        for (var node : affectedNodes) {
            addCurrentGrid(grids, node);
        }
        for (var controller : adjacentControllers) {
            addCurrentGrid(grids, controller.getGridNode());
        }

        for (var grid : grids) {
            grid.postEvent(new GridControllerStructureChanged());
        }
    }

    private static void addCurrentGrid(Set<IGrid> grids, @Nullable IGridNode node) {
        if (node == null) {
            return;
        }

        try {
            var grid = node.getGrid();
            if (!grid.isEmpty()) {
                grids.add(grid);
            }
        } catch (IllegalStateException ignored) {
            // NO-OP
        }
    }

    private void refreshRenderTypes() {
        if (this.level == null) {
            return;
        }

        refreshRenderType(this.worldPosition);
        for (var direction : Direction.values()) {
            var neighborPos = this.worldPosition.relative(direction);
            if (this.level.hasChunkAt(neighborPos)) {
                refreshRenderType(neighborPos);
            }
        }
    }

    private void refreshRenderType(BlockPos pos) {
        if (this.level.getBlockState(pos).getBlock() instanceof ColorableControllerBlock block) {
            block.updateRenderType(this.level, pos);
        }
    }

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
