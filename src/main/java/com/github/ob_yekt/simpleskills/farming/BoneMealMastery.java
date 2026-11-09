package com.github.ob_yekt.simpleskills.farming;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bone Meal Mastery (Farming 5+): right-click a compatible plantation with ANY hoe to apply the
 * normal bone meal growth, without owning or consuming bone meal. The only cost is hunger
 * (see {@link FarmingPerks#boneMealHungerCost(int)}), and it is charged only when growth is applied.
 *
 * Runs on the logical server only. The growth itself is the vanilla {@link BonemealableBlock} code path
 * (isValidBonemealTarget -> isBonemealSuccess -> performBonemeal), exactly what BoneMealItem does,
 * so each block keeps its own rules (age caps, random success, mod compatibility).
 */
public final class BoneMealMastery {
    private BoneMealMastery() {}

    /** Same particle event vanilla bone meal triggers (1505 = bone meal growth particles). */
    private static final int BONE_MEAL_PARTICLES_EVENT = 1505;

    public static void register() {
        UseBlockCallback.EVENT.register(BoneMealMastery::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()
                || !(player instanceof ServerPlayer serverPlayer)
                || !(world instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        if (player.isSpectator() || !player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }

        ItemStack held = player.getItemInHand(hand);
        if (!held.is(ItemTags.HOES)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        // 1) Only plantations that can actually receive bone meal. Everything else (grass, dirt,
        //    mature crops...) falls through to the hoe's normal behaviour, with no hunger charged.
        if (!CropHelper.isBoneMealPlantation(state)
                || !(state.getBlock() instanceof BonemealableBlock bonemealable)
                || !bonemealable.isValidBonemealTarget(level, pos, state, BonemealSource.INTERACTION)) {
            return InteractionResult.PASS;
        }

        // 2) Unlock check (Farming 5+).
        int farmingLevel = FarmingPerks.getFarmingLevel(serverPlayer);
        int hungerCost = FarmingPerks.boneMealHungerCost(farmingLevel);
        if (hungerCost < 0) {
            return InteractionResult.PASS;
        }

        // 3) Respect the project's normal hoe level/prestige gate, and spawn protection.
        if (!meetsToolRequirement(serverPlayer, held) || !serverPlayer.mayInteract(level, pos)) {
            return InteractionResult.FAIL;
        }

        // 4) Enough hunger? (creative players have no hunger, so they never pay.)
        FoodData food = player.getFoodData();
        boolean pays = hungerCost > 0 && !player.getAbilities().instabuild;
        if (pays && food.getFoodLevel() < hungerCost) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.simpleskills.bone_meal_mastery.not_enough_hunger", hungerCost),
                    true);
            return InteractionResult.FAIL;
        }

        // 5) Apply the normal bone meal effect exactly once.
        boolean grew = false;
        if (bonemealable.isBonemealSuccess(level, level.getRandom(), pos, state, BonemealSource.INTERACTION)) {
            bonemealable.performBonemeal(level, level.getRandom(), pos, state, BonemealSource.INTERACTION);
            grew = true;
        }

        held.causeUseVibration(player, GameEvent.ITEM_INTERACT_FINISH);
        level.levelEvent(BONE_MEAL_PARTICLES_EVENT, pos, 15);

        // 6) Charge hunger only if growth was really applied (vanilla's random failure costs nothing).
        if (grew && pays) {
            food.setFoodLevel(Math.max(0, food.getFoodLevel() - hungerCost));
        }

        Simpleskills.LOGGER.debug("Bone Meal Mastery: {} (Farming {}) grew={} cost={} at {}",
                serverPlayer.getName().getString(), farmingLevel, grew, grew ? hungerCost : 0, pos);

        return InteractionResult.SUCCESS_SERVER;
    }

    /** Same check as BlockTransformerMixin / EventHandlers use for hoes, so tools stay level-gated. */
    private static boolean meetsToolRequirement(ServerPlayer player, ItemStack tool) {
        String toolId = BuiltInRegistries.ITEM.getKey(tool.getItem()).toString();
        SkillRequirement requirement = ConfigManager.getToolRequirement(toolId);
        if (requirement == null) {
            return true;
        }

        int level = XPManager.getSkillLevel(player.getStringUUID(), requirement.getSkill());
        if (level < requirement.getLevel()) {
            player.sendSystemMessage(Component.literal(String.format(
                    "§6[simpleskills]§f You need %s level %d to use this tool!",
                    requirement.getSkill().getDisplayName(), requirement.getLevel())), true);
            return false;
        }

        int requiredPrestige = requirement.getRequiredPrestige();
        if (requiredPrestige > 0
                && DatabaseManager.getInstance().getPrestige(player.getStringUUID()) < requiredPrestige) {
            player.sendSystemMessage(Component.literal(String.format(
                    "§6[simpleskills]§f You need Prestige ★%d to use this tool!", requiredPrestige)), true);
            return false;
        }
        return true;
    }
}
