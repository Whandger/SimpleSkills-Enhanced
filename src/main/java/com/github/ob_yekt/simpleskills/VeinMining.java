package com.github.ob_yekt.simpleskills;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mining's Vein Mining ability.
 *
 * While sneaking, a player with Mining level 15 or higher can break additional
 * blocks of the exact same ore block type that are connected in any of the 26
 * surrounding directions. Additional blocks are broken through the player's
 * normal ServerPlayerGameMode so the regular block-break events, loot tables,
 * XP handling, tool durability, and other mod integrations remain in effect.
 */
public final class VeinMining {
    private VeinMining() {}

    private static final int FIRST_UNLOCK_LEVEL = 15;
    private static final int LEVEL_INTERVAL = 15;
    private static final int ADDITIONAL_BLOCKS_PER_TIER = 2;
    private static final int MAX_ADDITIONAL_BLOCKS = 12;
    private static final int LAST_UNLOCK_LEVEL = 90;

    private static final TagKey<Block> COMMON_ORES =
            TagKey.create(
                    Registries.BLOCK,
                    Identifier.fromNamespaceAndPath("c", "ores")
            );

    /**
     * Prevents additional blocks broken by this ability from starting another
     * Vein Mining chain for the same player.
     */
    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

    /**
     * Returns the maximum number of additional connected ore blocks unlocked
     * at the supplied Mining level. The manually broken starting block is not
     * included in this value.
     */
    public static int getAdditionalBlockLimit(int miningLevel) {
        if (miningLevel < FIRST_UNLOCK_LEVEL) {
            return 0;
        }

        int unlockedTiers = miningLevel / LEVEL_INTERVAL;
        return Math.min(
                unlockedTiers * ADDITIONAL_BLOCKS_PER_TIER,
                MAX_ADDITIONAL_BLOCKS
        );
    }

    /**
     * Notifies the player whenever Mining progression crosses a Vein Mining
     * unlock milestone. Iterating all milestones also handles XP awards that
     * advance more than one level at once.
     */
    public static void notifyUnlockedMilestones(
            ServerPlayer player,
            int previousLevel,
            int newLevel
    ) {
        if (player == null || newLevel <= previousLevel) {
            return;
        }

        for (int milestone = FIRST_UNLOCK_LEVEL;
                milestone <= LAST_UNLOCK_LEVEL;
                milestone += LEVEL_INTERVAL) {
            if (previousLevel < milestone && newLevel >= milestone) {
                int additionalBlocks = getAdditionalBlockLimit(milestone);
                String translationKey = milestone == FIRST_UNLOCK_LEVEL
                        ? "message.simpleskills.vein_mining.unlocked"
                        : "message.simpleskills.vein_mining.improved";

                player.sendSystemMessage(
                        Component.translatable(translationKey, additionalBlocks)
                                .withStyle(ChatFormatting.GOLD),
                        false
                );
            }
        }
    }

    /**
     * Attempts to mine up to the player's unlocked number of additional ores.
     * This method must be called from the server-side AFTER block-break event.
     */
    public static void tryActivate(
            ServerPlayer player,
            ServerLevel level,
            BlockPos origin,
            BlockState originState
    ) {
        if (player == null
                || level == null
                || origin == null
                || originState == null
                || !player.isShiftKeyDown()
                || !player.getMainHandItem().is(ItemTags.PICKAXES)
                || !isOre(originState)) {
            return;
        }

        int miningLevel = GatheringSpeed.getSkillLevelFromCache(
                player.getUUID(),
                Skills.MINING
        );

        int additionalBlockLimit = getAdditionalBlockLimit(miningLevel);
        if (additionalBlockLimit <= 0) {
            return;
        }

        UUID playerUuid = player.getUUID();
        if (!ACTIVE_PLAYERS.add(playerUuid)) {
            return;
        }

        try {
            collectAndBreakConnectedOres(
                    player,
                    level,
                    origin.immutable(),
                    originState.getBlock(),
                    additionalBlockLimit
            );
        } finally {
            ACTIVE_PLAYERS.remove(playerUuid);
        }
    }

    private static void collectAndBreakConnectedOres(
            ServerPlayer player,
            ServerLevel level,
            BlockPos origin,
            Block targetBlock,
            int maximumAdditionalBlocks
    ) {
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> connectedOres = new ArrayList<>(maximumAdditionalBlocks);

        frontier.add(origin);
        visited.add(origin);

        // Breadth-first search over all 26 neighboring positions.
        while (!frontier.isEmpty()
                && connectedOres.size() < maximumAdditionalBlocks) {
            BlockPos current = frontier.removeFirst();

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }

                        BlockPos candidate = current.offset(x, y, z);
                        if (!visited.add(candidate)) {
                            continue;
                        }

                        // Do not force-load chunks just to search for a vein.
                        if (!level.hasChunkAt(candidate)) {
                            continue;
                        }

                        BlockState candidateState = level.getBlockState(candidate);
                        if (candidateState.getBlock() != targetBlock
                                || !isOre(candidateState)) {
                            continue;
                        }

                        // Match vanilla interaction protections for automatically
                        // selected blocks: reach, claims/interaction permission,
                        // and world spawn protection.
                        if (!player.isWithinBlockInteractionRange(candidate, 1.0)
                                || !level.mayInteract(player, candidate)
                                || level.getServer().isUnderSpawnProtection(level, candidate, player)) {
                            continue;
                        }

                        connectedOres.add(candidate);
                        frontier.addLast(candidate);

                        if (connectedOres.size() >= maximumAdditionalBlocks) {
                            break;
                        }
                    }

                    if (connectedOres.size() >= maximumAdditionalBlocks) {
                        break;
                    }
                }

                if (connectedOres.size() >= maximumAdditionalBlocks) {
                    break;
                }
            }
        }

        // Use the player's normal block-breaking path for every extra block.
        // This preserves vanilla tool checks, loot tables, durability and
        // PlayerBlockBreakEvents (including SimpleSkills' XP and drop logic).
        for (BlockPos orePos : connectedOres) {
            if (level.getBlockState(orePos).getBlock() == targetBlock) {
                player.gameMode.destroyBlock(orePos);
            }
        }
    }

    private static boolean isOre(BlockState state) {
        return state.is(BlockTags.ORES) || state.is(COMMON_ORES);
    }
}
