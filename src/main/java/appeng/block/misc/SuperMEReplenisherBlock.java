package appeng.block.misc;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.misc.SuperMEReplenisherBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.implementations.SuperMEReplenisherMenu;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;

public class SuperMEReplenisherBlock extends AEBaseEntityBlock<SuperMEReplenisherBlockEntity> {
    public static final EnumProperty<Activity> ACTIVITY = EnumProperty.create("activity", Activity.class);

    public SuperMEReplenisherBlock() {
        super(metalProps());
        registerDefaultState(defaultBlockState().setValue(ACTIVITY, Activity.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVITY);
    }

    @Override
    protected BlockState updateBlockStateFromBlockEntity(BlockState currentState,
            SuperMEReplenisherBlockEntity blockEntity) {
        return currentState.setValue(ACTIVITY, blockEntity.getActivity());
    }

    @Override
    public InteractionResult onActivated(Level level, BlockPos pos, Player player, InteractionHand hand,
            @Nullable ItemStack heldItem, BlockHitResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS;
        }

        var blockEntity = getBlockEntity(level, pos);
        if (blockEntity == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            MenuOpener.open(SuperMEReplenisherMenu.TYPE, player, MenuLocators.forBlockEntity(blockEntity));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public enum Activity implements StringRepresentable {
        IDLE("idle"),
        INSERTING("inserting"),
        EXTRACTING("extracting");

        private final String serializedName;

        Activity(String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String getSerializedName() {
            return serializedName;
        }
    }
}
