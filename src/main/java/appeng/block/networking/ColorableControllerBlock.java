package appeng.block.networking;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.HitResult;

import appeng.api.util.AEColor;
import appeng.blockentity.networking.ColorableControllerBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.localization.PlayerMessages;
import appeng.util.SettingsFrom;

/**
 * A controller whose color is part of the physical controller structure while retaining grid color compatibility.
 */
public class ColorableControllerBlock extends ControllerBlock<ColorableControllerBlockEntity> {

    public static final EnumProperty<ControllerColor> COLOR = EnumProperty.create("color", ControllerColor.class);

    public ColorableControllerBlock() {
        super();
        this.registerDefaultState(this.defaultBlockState().setValue(COLOR, ControllerColor.TRANSPARENT));
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var color = ColorableControllerBlockEntity.readColor(context.getItemInHand().getTag());
        var state = defaultBlockState().setValue(COLOR, ControllerColor.fromAEColor(color));
        return getControllerType(state, context.getLevel(), context.getClickedPos());
    }

    @Override
    protected boolean isController(BlockState centerState, LevelAccessor level, BlockPos centerPos,
            BlockPos neighborPos) {
        var neighborState = level.getBlockState(neighborPos);
        return neighborState.is(AEBlocks.COLORABLE_CONTROLLER.block())
                && neighborState.getValue(COLOR) == centerState.getValue(COLOR);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState, ColorableControllerBlockEntity be) {
        return super.updateBlockStateFromBlockEntity(currentState, be)
                .setValue(COLOR, ControllerColor.fromAEColor(be.getColor()));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter level, List<Component> tooltip,
            TooltipFlag flag) {
        var color = ColorableControllerBlockEntity.readColor(stack.getTag());
        var colorName = color == AEColor.TRANSPARENT
                ? PlayerMessages.Uncolored.text()
                : Component.translatable(color.translationKey);
        tooltip.add(PlayerMessages.Color.text(colorName));
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos,
            Player player) {
        return createItemStack(level, pos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return createItemStack(level, pos);
    }

    private ItemStack createItemStack(BlockGetter level, BlockPos pos) {
        var stack = new ItemStack(this);
        var blockEntity = getBlockEntity(level, pos);
        if (blockEntity != null) {
            var settings = new CompoundTag();
            blockEntity.exportSettings(SettingsFrom.DISMANTLE_ITEM, settings, null);
            if (!settings.isEmpty()) {
                stack.setTag(settings);
            }
        }
        return stack;
    }
}
