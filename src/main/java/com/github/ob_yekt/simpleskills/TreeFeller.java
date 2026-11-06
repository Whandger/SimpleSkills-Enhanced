
package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.github.ob_yekt.simpleskills.utils.BlockSkillClassifier;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Woodcutting's Tree Feller ability.
 *
 * At Woodcutting level 5, sneaking and breaking a log with an axe starts a
 * progressive tree scan. Connected logs are discovered in all 26 directions,
 * including diagonal branches. Natural leaves are discovered using vanilla's
 * six-direction leaf-distance gradient.
 *
 * Each falling block is processed exactly once when it lands.
 */
public final class TreeFeller {
    private TreeFeller() {}

    public static final int UNLOCK_LEVEL = 5;

    private static final int BLOCKS_PER_TICK = 20;
    private static final int MAX_SCANNED_BLOCKS = 100_000;

    private static final Map<UUID, TreeJob> ACTIVE_JOBS = new ConcurrentHashMap<>();
    private static final Map<UUID, FallingBlockTask> FALLING_BLOCKS = new ConcurrentHashMap<>();

    public static boolean isUnlocked(int woodcuttingLevel) {
        return woodcuttingLevel >= UNLOCK_LEVEL;
    }

    public static void register() {
        ServerTickEvents.END_LEVEL_TICK.register(TreeFeller::tick);
    }

    public static void notifyUnlocked(ServerPlayer player, int previousLevel, int newLevel) {
        if (player == null
                || previousLevel >= UNLOCK_LEVEL
                || newLevel < UNLOCK_LEVEL) {
            return;
        }

        player.sendSystemMessage(
                Component.translatable("message.simpleskills.tree_feller.unlocked")
                        .withStyle(ChatFormatting.GOLD),
                false
        );
    }

    /**
     * Called after the manually broken starting log has already been processed
     * by Minecraft and SimpleSkills' normal block-break handlers.
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
                || player.preventsBlockDrops()
                || !player.getMainHandItem().is(ItemTags.AXES)
                || !originState.is(BlockTags.LOGS)) {
            return;
        }

        int woodcuttingLevel = GatheringSpeed.getSkillLevelFromCache(
                player.getUUID(),
                Skills.WOODCUTTING
        );

        if (!isUnlocked(woodcuttingLevel)) {
            return;
        }

        UUID playerId = player.getUUID();
        if (ACTIVE_JOBS.containsKey(playerId)) {
            return;
        }

        ItemStack tool = player.getMainHandItem();

        TreeJob job = new TreeJob(
                player,
                level,
                origin.immutable(),
                tool,
                tool.copy()
        );

        seedNeighbors(job);

        if (job.logQueue.isEmpty() && job.leafQueue.isEmpty()) {
            return;
        }

        ACTIVE_JOBS.put(playerId, job);
    }

    /**
     * Seeds the scan around the manually broken starting log.
     *
     * Logs use all 26 neighboring positions so diagonal branches are found.
     * Leaves use the six face directions because vanilla leaf DISTANCE is
     * calculated through face-adjacent blocks.
     */
    private static void seedNeighbors(TreeJob job) {
        enqueueLogNeighbors(job, job.origin);

        for (Direction direction : Direction.values()) {
            enqueueAttachedLeaf(job, job.origin.relative(direction));
        }
    }

    /**
     * Searches every neighboring position around a log.
     *
     * This includes:
     * - Vertical connections.
     * - Horizontal branches.
     * - Diagonal branches.
     * - Branches that descend and later rise again.
     *
     * No ground-column rejection is performed: a connected secondary trunk
     * remains part of the main tree.
     */
    private static void enqueueLogNeighbors(TreeJob job, BlockPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    enqueueLog(job, center.offset(dx, dy, dz));
                }
            }
        }
    }

    private static void enqueueLog(TreeJob job, BlockPos pos) {
        if (!job.level.hasChunkAt(pos)) {
            return;
        }

        BlockPos immutablePos = pos.immutable();

        if (job.visitedLogs.contains(immutablePos)) {
            return;
        }

        BlockState state = job.level.getBlockState(immutablePos);

        if (!isWoodcuttingLog(state) || state.hasBlockEntity()) {
            return;
        }

        job.visitedLogs.add(immutablePos);
        job.logQueue.addLast(immutablePos);
    }

    private static void enqueueAttachedLeaf(TreeJob job, BlockPos pos) {
        if (!job.level.hasChunkAt(pos)) {
            return;
        }

        BlockPos immutablePos = pos.immutable();

        if (job.visitedLeaves.contains(immutablePos)) {
            return;
        }

        BlockState state = job.level.getBlockState(immutablePos);

        if (!isNaturalLeaf(state) || state.hasBlockEntity()) {
            return;
        }

        // Vanilla leaves use distance 1 when directly connected to a log.
        if (state.hasProperty(BlockStateProperties.DISTANCE)
                && state.getValue(BlockStateProperties.DISTANCE) != 1) {
            return;
        }

        job.visitedLeaves.add(immutablePos);
        job.leafQueue.addLast(immutablePos);
    }

    private static void enqueueLeafOutward(
            TreeJob job,
            BlockPos pos,
            int parentDistance
    ) {
        if (!job.level.hasChunkAt(pos)) {
            return;
        }

        BlockPos immutablePos = pos.immutable();

        if (job.visitedLeaves.contains(immutablePos)) {
            return;
        }

        BlockState state = job.level.getBlockState(immutablePos);

        if (!isNaturalLeaf(state) || state.hasBlockEntity()) {
            return;
        }

        if (state.hasProperty(BlockStateProperties.DISTANCE)) {
            int distance = state.getValue(BlockStateProperties.DISTANCE);

            // Follow the vanilla leaf-distance gradient away from the logs.
            // This prevents touching canopies from automatically connecting
            // two otherwise independent trees.
            if (distance != parentDistance + 1 || distance > 6) {
                return;
            }
        }

        job.visitedLeaves.add(immutablePos);
        job.leafQueue.addLast(immutablePos);
    }

    private static boolean isWoodcuttingLog(BlockState state) {
        return state.is(BlockTags.LOGS)
                && BlockSkillClassifier.classify(state) == Skills.WOODCUTTING;
    }

    private static boolean isNaturalLeaf(BlockState state) {
        if (!state.is(BlockTags.LEAVES)) {
            return false;
        }

        if (state.hasProperty(BlockStateProperties.PERSISTENT)
                && state.getValue(BlockStateProperties.PERSISTENT)) {
            return false;
        }

        return !state.hasProperty(BlockStateProperties.DISTANCE)
                || state.getValue(BlockStateProperties.DISTANCE) < 7;
    }

    private static void tick(ServerLevel level) {
        for (TreeJob job : List.copyOf(ACTIVE_JOBS.values())) {
            if (job.level != level) {
                continue;
            }

            if (job.player.isRemoved() || !job.player.isAlive()) {
                cancelJob(job, null);
                continue;
            }

            if (!job.cancelled && !job.scanComplete) {
                scanBatch(job);
            }

            if (!job.cancelled && job.scanComplete && !job.spawningComplete) {
                spawnBatch(job);
            }

            if (job.pendingXp > 0 && !job.player.isRemoved()) {
                int xp = job.pendingXp;
                job.pendingXp = 0;

                XPManager.addXPWithNotification(
                        job.player,
                        Skills.WOODCUTTING,
                        xp
                );
            }

            if (job.scanComplete
                    && job.spawningComplete
                    && job.pendingFallingBlocks == 0) {
                ACTIVE_JOBS.remove(job.player.getUUID(), job);
            }
        }
    }

    private static void scanBatch(TreeJob job) {
        int scannedThisTick = 0;

        while (scannedThisTick < BLOCKS_PER_TICK) {
            if (!job.scanningLeaves && !job.logQueue.isEmpty()) {
                BlockPos current = job.logQueue.removeFirst();
                scannedThisTick++;

                if (!job.level.hasChunkAt(current)) {
                    continue;
                }

                BlockState state = job.level.getBlockState(current);

                if (!isWoodcuttingLog(state) || state.hasBlockEntity()) {
                    continue;
                }

                job.blocks.add(new TreeBlock(current, state, true));
                job.logCount++;

                // Explore all 26 directions for more logs and branches.
                enqueueLogNeighbors(job, current);

                // Leaves are still seeded only through face-adjacent blocks.
                for (Direction direction : Direction.values()) {
                    enqueueAttachedLeaf(
                            job,
                            current.relative(direction)
                    );
                }

            } else if (!job.scanningLeaves) {
                // Finish the entire connected log structure before following
                // the natural leaf-distance gradient.
                job.scanningLeaves = true;

            } else if (!job.leafQueue.isEmpty()) {
                BlockPos current = job.leafQueue.removeFirst();
                scannedThisTick++;

                if (!job.level.hasChunkAt(current)) {
                    continue;
                }

                BlockState state = job.level.getBlockState(current);

                if (!isNaturalLeaf(state) || state.hasBlockEntity()) {
                    continue;
                }

                job.blocks.add(new TreeBlock(current, state, false));

                if (current.getY() >= job.origin.getY() - 1) {
                    job.hasNaturalLeaves = true;
                }

                int distance = state.hasProperty(BlockStateProperties.DISTANCE)
                        ? state.getValue(BlockStateProperties.DISTANCE)
                        : 0;

                // Vanilla leaf distance propagates through the six faces.
                for (Direction direction : Direction.values()) {
                    enqueueLeafOutward(
                            job,
                            current.relative(direction),
                            distance
                    );
                }

            } else {
                break;
            }

            if (job.visitedLogs.size() + job.visitedLeaves.size()
                    > MAX_SCANNED_BLOCKS) {
                cancelJob(
                        job,
                        "Tree scan stopped: the tree exceeded the safety limit."
                );
                return;
            }
        }

        if (job.logQueue.isEmpty() && job.leafQueue.isEmpty()) {
            job.scanComplete = true;

            if (job.logCount == 0 || !job.hasNaturalLeaves) {
                cancelJob(job, null);
                return;
            }

            // Remove the base first and work upward. This clears the path below
            // later falling blocks so they can reach the ground.
            job.blocks.sort(
                    Comparator.comparingInt(
                            (TreeBlock block) -> block.pos().getY()
                    )
            );
        }
    }

    private static void spawnBatch(TreeJob job) {
        int spawnedThisTick = 0;

        while (job.nextBlockIndex < job.blocks.size()
                && spawnedThisTick < BLOCKS_PER_TICK) {

            TreeBlock treeBlock = job.blocks.get(job.nextBlockIndex++);
            BlockPos pos = treeBlock.pos();

            if (!job.level.hasChunkAt(pos)
                    || !job.level.mayInteract(job.player, pos)
                    || job.level.getServer().isUnderSpawnProtection(
                            job.level,
                            pos,
                            job.player
                    )) {
                continue;
            }

            BlockState currentState = job.level.getBlockState(pos);

            if (currentState.getBlock() != treeBlock.state().getBlock()
                    || currentState.hasBlockEntity()) {
                continue;
            }

            Block block = currentState.getBlock();

            BlockState adjustedState = block.playerWillDestroy(
                    job.level,
                    pos,
                    currentState,
                    job.player
            );

            FallingBlockEntity fallingBlock = FallingBlockEntity.fall(
                    job.level,
                    pos,
                    adjustedState
            );

            block.destroy(job.level, pos, adjustedState);

            // The entity is visual only. TreeFeller handles loot, XP and
            // durability exactly once when the entity reaches the ground.
            fallingBlock.dropItem = false;

            FALLING_BLOCKS.put(
                    fallingBlock.getUUID(),
                    new FallingBlockTask(
                            job,
                            adjustedState,
                            treeBlock.isLog(),
                            pos
                    )
            );

            job.pendingFallingBlocks++;
            spawnedThisTick++;
        }

        if (job.nextBlockIndex >= job.blocks.size()) {
            job.spawningComplete = true;
        }
    }

    /**
     * Called by the FallingBlockEntity mixin before vanilla landing behavior.
     *
     * @return true when TreeFeller owns this entity's tick and vanilla handling
     *         must be cancelled.
     */
    public static boolean handleFallingBlockTick(FallingBlockEntity entity) {
        FallingBlockTask task = FALLING_BLOCKS.get(entity.getUUID());

        if (task == null) {
            return false;
        }

        boolean landed = entity.onGround();

        boolean timedOut = entity.time > 600
                || entity.blockPosition().getY() <= entity.level().getMinY();

        if (!landed && !timedOut) {
            return false;
        }

        if (!FALLING_BLOCKS.remove(entity.getUUID(), task)) {
            return true;
        }

        entity.discard();

        TreeJob job = task.job();

        if (entity.level() instanceof ServerLevel level) {
            processLandedBlock(
                    job,
                    level,
                    entity.blockPosition(),
                    task
            );
        }

        job.pendingFallingBlocks = Math.max(
                0,
                job.pendingFallingBlocks - 1
        );

        return true;
    }

    private static void processLandedBlock(
            TreeJob job,
            ServerLevel level,
            BlockPos landingPos,
            FallingBlockTask task
    ) {
        BlockState state = task.state();
        ServerPlayer player = job.player;

        if (player.isRemoved() || !player.isAlive()) {
            Block.dropResources(state, level, landingPos, null);
            return;
        }

        ItemStack destroyedWith = job.toolSnapshot.copy();

        // Only logs consume durability and grant Woodcutting XP.
        // Leaves retain their normal loot-table behavior.
        if (task.isLog()) {
            damageAxeForLog(
                    job,
                    level,
                    state,
                    task.sourcePos()
            );

            Integer configuredXp = ConfigManager.getBlockXPOverride(
                    state.getBlock().getDescriptionId()
            );

            int xp = configuredXp != null
                    ? configuredXp
                    : Math.max(
                            0,
                            Math.round(
                                    state.getDestroySpeed(
                                            level,
                                            task.sourcePos()
                                    ) * (100.0f / 1.5f)
                            )
                    );

            if (BlockSkillClassifier.classify(state) == Skills.WOODCUTTING) {
                job.pendingXp += xp;
            }
        }

        // Show the block breaking effect where the falling block lands.
        state.getBlock().spawnDestroyByEntityParticles(
                level,
                player,
                landingPos,
                state
        );

        // Block.playerDestroy invokes the normal loot table, tool-aware drops,
        // block stats, exhaustion and SimpleSkills' duplication mixin.
        state.getBlock().playerDestroy(
                level,
                player,
                landingPos,
                state,
                null,
                destroyedWith
        );
    }

    private static void damageAxeForLog(
            TreeJob job,
            ServerLevel level,
            BlockState state,
            BlockPos pos
    ) {
        ItemStack tool = job.toolStack;

        if (tool.isEmpty() || !tool.isDamageableItem()) {
            return;
        }

        // Keep the existing GatheringDurabilityMixin path when the original
        // axe is still in the main hand.
        if (tool == job.player.getMainHandItem()) {
            tool.mineBlock(
                    level,
                    state,
                    pos,
                    job.player
            );
            return;
        }

        if (GatheringSpeed.shouldPreventDurabilityLoss(
                job.player,
                state,
                tool
        )) {
            return;
        }

        tool.hurtAndBreak(
                1,
                level,
                job.player,
                brokenStack -> job.player.onEquippedItemBroken(
                        brokenStack,
                        EquipmentSlot.MAINHAND
                )
        );
    }

    private static void cancelJob(TreeJob job, String message) {
        job.cancelled = true;

        ACTIVE_JOBS.remove(
                job.player.getUUID(),
                job
        );

        if (message != null && !job.player.isRemoved()) {
            job.player.sendSystemMessage(
                    Component.literal("[SimpleSkills] " + message)
                            .withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    private record TreeBlock(
            BlockPos pos,
            BlockState state,
            boolean isLog
    ) {}

    private record FallingBlockTask(
            TreeJob job,
            BlockState state,
            boolean isLog,
            BlockPos sourcePos
    ) {}

    private static final class TreeJob {
        private final ServerPlayer player;
        private final ServerLevel level;
        private final BlockPos origin;
        private final ItemStack toolStack;
        private final ItemStack toolSnapshot;

        private final ArrayDeque<BlockPos> logQueue = new ArrayDeque<>();
        private final ArrayDeque<BlockPos> leafQueue = new ArrayDeque<>();

        private final Set<BlockPos> visitedLogs = new HashSet<>();
        private final Set<BlockPos> visitedLeaves = new HashSet<>();

        private final List<TreeBlock> blocks = new ArrayList<>();

        private boolean scanningLeaves;
        private boolean hasNaturalLeaves;
        private boolean cancelled;
        private boolean scanComplete;
        private boolean spawningComplete;

        private int logCount = 1;
        private int nextBlockIndex;
        private int pendingFallingBlocks;
        private int pendingXp;

        private TreeJob(
                ServerPlayer player,
                ServerLevel level,
                BlockPos origin,
                ItemStack toolStack,
                ItemStack toolSnapshot
        ) {
            this.player = player;
            this.level = level;
            this.origin = origin;
            this.toolStack = toolStack;
            this.toolSnapshot = toolSnapshot;
        }
    }
}
