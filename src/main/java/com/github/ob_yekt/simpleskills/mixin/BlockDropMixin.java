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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

        Skills skill = BlockSkillClassifier.classify(state);
        ItemStack heldTool = player.getMainHandItem();
        boolean thermalSmelting = com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager
                .hasPerk(heldTool, "thermal_smelting");
        boolean isOre = skill == Skills.MINING
                && (state.is(BlockTags.ORES) || state.is(COMMON_ORES))
                && heldTool.is(ItemTags.PICKAXES);
        boolean isLog = skill == Skills.WOODCUTTING
                && state.is(BlockTags.LOGS)
                && tool.is(ItemTags.AXES);

        if (isOre) {
            com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager.triggerVeinResonance(player, pos);
        }

        // Thermal Smelting converts the block's normal drops before any duplication rolls.
        // This keeps both profession and equipment duplication consistent with the final output.
        List<ItemStack> baseDrops = originalDrops;
        if (thermalSmelting && (heldTool.is(ItemTags.PICKAXES) || heldTool.is(ItemTags.SHOVELS))) {
            List<ItemStack> smelted = new ArrayList<>(originalDrops.size());
            boolean changed = false;
            for (ItemStack drop : originalDrops) {
                ItemStack converted = thermalSmelt(drop, state);
                if (converted != null) {
                    smelted.add(converted);
                    changed = true;
                } else {
                    smelted.add(drop);
                }
            }
            if (changed) baseDrops = smelted;
        }

        List<ItemStack> extraDrops = new ArrayList<>();

        // Original gathering duplication remains unchanged and independent of equipment affixes.
        if (isOre || isLog) {
            int skillLevel = Math.max(0, Math.min(
                    GatheringSpeed.getSkillLevelFromCache(player.getUUID(), skill), XPManager.getMaxLevel()));
            float chance = Math.min((skillLevel / 10) * BONUS_PER_TEN_LEVELS, MAX_DUPLICATION_CHANCE);
            if (chance > 0.0f && player.getRandom().nextFloat() < chance) {
                for (ItemStack stack : baseDrops) {
                    if (!stack.isEmpty() && (isOre || stack.is(ItemTags.LOGS))) extraDrops.add(stack.copy());
                }
            }
        }

        // Equipment drop perks only duplicate resources belonging to their gathering action:
        // Mining duplicates ore drops; Woodcutting duplicates log/wood drops. They never
        // duplicate arbitrary blocks, crops, machine outputs, or unrelated modded drops.
        // The original drop list is used once, preventing recursive multiplication.
        if (isOre || isLog) {
            double fortuneChance = com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager
                    .getBonusDropChance(heldTool, "fortune_mastery");
            double prospectorChance = com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager
                    .getBonusDropChance(heldTool, "prospector");
            double worldshaperChance = com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager
                    .getBonusDropChance(heldTool, "worldshaper");
            double bonusChance = Math.min(0.45, fortuneChance + prospectorChance + worldshaperChance);
            if (bonusChance > 0.0 && player.getRandom().nextDouble() < bonusChance) {
                for (ItemStack stack : baseDrops) {
                    if (!stack.isEmpty() && (isOre || stack.is(ItemTags.LOGS))) {
                        extraDrops.add(stack.copy());
                    }
                }
            }
        }

        if (!extraDrops.isEmpty() || baseDrops != originalDrops) {
            List<ItemStack> updatedDrops = new ArrayList<>(baseDrops);
            updatedDrops.addAll(extraDrops);
            cir.setReturnValue(updatedDrops);
        }
    }

    /** Maps eligible natural block drops to their furnace-smelted outputs. */
    private static ItemStack thermalSmelt(ItemStack drop, BlockState sourceState) {
        if (drop == null || drop.isEmpty()) return null;
        var item = drop.getItem();
        net.minecraft.world.item.Item output = null;

        // Nether Gold Ore normally drops several nuggets rather than a smeltable ore item.
        // The thermal perk represents smelting the source block itself, so return its ingot output.
        if (sourceState.is(Blocks.NETHER_GOLD_ORE)
                && (item == Items.GOLD_NUGGET || item == Items.NETHER_GOLD_ORE)) {
            return new ItemStack(Items.GOLD_INGOT, 1);
        }
        if (item == Items.NETHER_GOLD_ORE) {
            output = Items.GOLD_INGOT;
        } else if (item == Items.RAW_IRON || item == Items.IRON_ORE || item == Items.DEEPSLATE_IRON_ORE) {
            output = Items.IRON_INGOT;
        } else if (item == Items.RAW_GOLD || item == Items.GOLD_ORE || item == Items.DEEPSLATE_GOLD_ORE) {
            output = Items.GOLD_INGOT;
        } else if (item == Items.RAW_COPPER || item == Items.COPPER_ORE
                || item == Items.DEEPSLATE_COPPER_ORE) {
            output = Items.COPPER_INGOT;
        } else if (item == Items.SAND || item == Items.RED_SAND) {
            output = Items.GLASS;
        } else if (item == Items.COBBLESTONE || item == Items.STONE) {
            output = Items.SMOOTH_STONE;
        } else if (item == Items.ANCIENT_DEBRIS) {
            output = Items.NETHERITE_SCRAP;
        }

        return output == null ? null : new ItemStack(output, drop.getCount());
    }
}
