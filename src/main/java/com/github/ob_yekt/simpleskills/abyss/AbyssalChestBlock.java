package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Heavy Abyssal-stone chest. Each half stores 54 slots; a joined pair shares all 108 slots. */
public final class AbyssalChestBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<ChestType> TYPE = BlockStateProperties.CHEST_TYPE;

    public AbyssalChestBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(TYPE, ChestType.SINGLE));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AbyssalChestBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();

        // Prefer the orientation of a neighboring single Abyssal chest. This avoids
        // mismatched facings when the second half is placed from another side.
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState neighbour = context.getLevel().getBlockState(context.getClickedPos().relative(direction));
            if (!neighbour.is(AbyssContent.ABYSSAL_CHEST)
                    || neighbour.getValue(TYPE) != ChestType.SINGLE) {
                continue;
            }

            Direction neighbourFacing = neighbour.getValue(FACING);
            if (direction == neighbourFacing.getClockWise()
                    || direction == neighbourFacing.getCounterClockWise()) {
                facing = neighbourFacing;
                break;
            }
        }

        return defaultBlockState().setValue(FACING, facing).setValue(TYPE, ChestType.SINGLE);
    }

    /** Pair only with a free single chest. Existing pairs cannot be extended into triples. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        Direction facing = state.getValue(FACING);
        for (Direction side : new Direction[]{facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos otherPos = pos.relative(side);
            BlockState otherState = level.getBlockState(otherPos);
            if (!isUnpairedChest(otherState, facing)) continue;

            ChestType thisType = side == facing.getClockWise() ? ChestType.LEFT : ChestType.RIGHT;
            level.setBlock(pos, state.setValue(TYPE, thisType), 3);
            level.setBlock(otherPos, otherState.setValue(TYPE, thisType.getOpposite()), 3);
            return;
        }
    }

    private static boolean isUnpairedChest(BlockState state, Direction facing) {
        return state.is(AbyssContent.ABYSSAL_CHEST)
                && state.getValue(FACING) == facing
                && state.getValue(TYPE) == ChestType.SINGLE;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                                    Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof AbyssalChestBlockEntity chest) {
            AbyssalChestBlockEntity partner = findValidPartner(level, pos, state);
            if (state.getValue(TYPE) != ChestType.SINGLE && partner == null) {
                // Recover gracefully if a neighbor was removed by an unusual update path.
                state = state.setValue(TYPE, ChestType.SINGLE);
                level.setBlock(pos, state, 3);
            }
            if (partner == null) {
                player.openMenu(chest);
            } else {
                Container combined = state.getValue(TYPE) == ChestType.LEFT
                        ? new CompoundContainer(chest, partner)
                        : new CompoundContainer(partner, chest);
                player.openMenu(new SimpleMenuProvider(
                        (containerId, inventory, openingPlayer) ->
                                new AbyssalChestMenu(containerId, inventory, combined),
                        Component.translatable("block.simpleskills.abyssal_chest")
                ));
            }
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Nullable
    private static AbyssalChestBlockEntity findValidPartner(Level level, BlockPos pos, BlockState state) {
        ChestType type = state.getValue(TYPE);
        if (type == ChestType.SINGLE) return null;

        Direction facing = state.getValue(FACING);
        Direction side = type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        BlockPos candidatePos = pos.relative(side);
        BlockState candidateState = level.getBlockState(candidatePos);
        if (!candidateState.is(AbyssContent.ABYSSAL_CHEST)
                || candidateState.getValue(FACING) != facing
                || candidateState.getValue(TYPE) != type.getOpposite()) {
            return null;
        }
        BlockEntity candidate = level.getBlockEntity(candidatePos);
        return candidate instanceof AbyssalChestBlockEntity chest ? chest : null;
    }

    /** A surviving half becomes a normal single chest when its partner is removed. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        BlockState updated = super.updateShape(state, level, ticks, pos, directionToNeighbour,
                neighbourPos, neighbourState, random);
        if (state.getValue(TYPE) != ChestType.SINGLE && !hasValidPartner(level, pos, state)) {
            return updated.setValue(TYPE, ChestType.SINGLE);
        }
        return updated;
    }

    private static boolean hasValidPartner(LevelReader level, BlockPos pos, BlockState state) {
        ChestType type = state.getValue(TYPE);
        if (type == ChestType.SINGLE) return false;
        Direction facing = state.getValue(FACING);
        Direction side = type == ChestType.LEFT ? facing.getClockWise() : facing.getCounterClockWise();
        BlockState other = level.getBlockState(pos.relative(side));
        return other.is(AbyssContent.ABYSSAL_CHEST)
                && other.getValue(FACING) == facing
                && other.getValue(TYPE) == type.getOpposite();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state.rotate(mirror.getRotation(state.getValue(FACING)));
        ChestType type = mirrored.getValue(TYPE);
        return type == ChestType.SINGLE ? mirrored : mirrored.setValue(TYPE, type.getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, TYPE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
