package com.github.ob_yekt.simpleskills.mixin;

import com.github.ob_yekt.simpleskills.GatheringSpeed;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.github.ob_yekt.simpleskills.utils.BlockSkillClassifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Block.class)
public abstract class BlockDropMixin {

    private static final TagKey<Block> COMMON_ORES =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("c", "ores")
            );

    private static final float BONUS_PER_TEN_LEVELS = 0.05f;
    private static final float MAX_DUPLICATION_CHANCE = 0.45f;

    @Inject(
            method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void simpleskills$duplicateGatheringDrops(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            BlockEntity blockEntity,
            Entity breaker,
            ItemInstance tool,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        if (!(breaker instanceof ServerPlayer player)) return;

        List<ItemStack> originalDrops = cir.getReturnValue();
        if (originalDrops == null || originalDrops.isEmpty()) return;

        List<ItemStack> extraDrops = new ArrayList<>();
        Skills skill = BlockSkillClassifier.classify(state);
        boolean isOre = skill == Skills.MINING
                && (state.is(BlockTags.ORES) || state.is(COMMON_ORES))
                && player.getMainHandItem().is(ItemTags.PICKAXES);
        boolean isLog = skill == Skills.WOODCUTTING
                && state.is(BlockTags.LOGS)
                && tool.is(ItemTags.AXES);

        // Original gathering duplication remains unchanged and independent of equipment affixes.
        if (isOre || isLog) {
            int skillLevel = Math.max(0, Math.min(
                    GatheringSpeed.getSkillLevelFromCache(player.getUUID(), skill), XPManager.getMaxLevel()));
            float chance = Math.min((skillLevel / 10) * BONUS_PER_TEN_LEVELS, MAX_DUPLICATION_CHANCE);
            if (chance > 0.0f && player.getRandom().nextFloat() < chance) {
                for (ItemStack stack : originalDrops) {
                    if (!stack.isEmpty() && (isOre || stack.is(ItemTags.LOGS))) extraDrops.add(stack.copy());
                }
            }
        }

        // Fortune Mastery and the Unique Prospector affix add an independent chance to duplicate
        // ordinary block loot. This uses only the original loot list, so the bonuses cannot multiply each other.
        ItemStack heldTool = player.getMainHandItem();
        if (heldTool.is(ItemTags.PICKAXES) || heldTool.is(ItemTags.SHOVELS)
                || heldTool.is(ItemTags.AXES) || heldTool.is(ItemTags.HOES)) {
            double fortuneChance = com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager
                    .getBonusDropChance(heldTool, "fortune_mastery");
            double prospectorChance = com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager
                    .getBonusDropChance(heldTool, "prospector");
            double worldshaperChance = com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager
                    .getBonusDropChance(heldTool, "worldshaper");
            double bonusChance = Math.min(0.45, fortuneChance + prospectorChance + worldshaperChance);
            if (bonusChance > 0.0 && player.getRandom().nextDouble() < bonusChance) {
                for (ItemStack stack : originalDrops) {
                    if (!stack.isEmpty()) extraDrops.add(stack.copy());
                }
            }
        }

        if (!extraDrops.isEmpty()) {
            List<ItemStack> updatedDrops = new ArrayList<>(originalDrops);
            updatedDrops.addAll(extraDrops);
            cir.setReturnValue(updatedDrops);
        }
    }
}
