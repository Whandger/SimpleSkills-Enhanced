
package com.github.ob_yekt.simpleskills.runes;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Shared block implementation for both Rune Ores.
 * CHARGE controls the mining textures and progressively increases
 * the vanilla block-light emission.
 */
public final class RuneOreBlock extends Block {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 9);

    private static final int BASE_LIGHT = 9;
    private static final int MAX_LIGHT = 15;

    public RuneOreBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(state -> lightForCharge(state.getValue(CHARGE))));
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGE, 0));
    }

    private static int lightForCharge(int charge) {
        if (charge <= 0) {
            return BASE_LIGHT;
        }

        // Progressively increases from light level 9 to 15.
        return BASE_LIGHT + (int) Math.ceil(
                (double) charge * (MAX_LIGHT - BASE_LIGHT) / 9.0
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGE);
    }
}
