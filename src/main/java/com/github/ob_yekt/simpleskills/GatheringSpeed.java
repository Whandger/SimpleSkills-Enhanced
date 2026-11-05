
package com.github.ob_yekt.simpleskills;

import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.github.ob_yekt.simpleskills.utils.BlockSkillClassifier;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gathering Speed: +1% block-breaking speed per level for Mining (pickaxe),
 * Woodcutting (axe) and Excavating (shovel), applied once on top of the vanilla speed.
 *
 * Levels are kept in tiny in-memory caches so the per-tick break-speed calculation never touches
 * the database (DatabaseManager.getAllSkills pings the JDBC connection on every call):
 *  - server side: UUID -> levels, written by AttributeManager.updatePlayerAttributes
 *  - client side: levels received through LevelsPayload
 */
public final class GatheringSpeed {
    private GatheringSpeed() {}

    public static final float BONUS_PER_LEVEL = 0.01f;

    // Index order inside the level arrays: [MINING, WOODCUTTING, EXCAVATING]
    private static final Map<UUID, int[]> SERVER_LEVELS = new ConcurrentHashMap<>();
    private static volatile int[] clientLevels = new int[3];

    // ---------------------------------------------------------------- networking

    public record LevelsPayload(int mining, int woodcutting, int excavating)
            implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<LevelsPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        Identifier.fromNamespaceAndPath(
                                Simpleskills.MOD_ID,
                                "gathering_speed_levels"
                        )
                );

        public static final StreamCodec<RegistryFriendlyByteBuf, LevelsPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, LevelsPayload::mining,
                        ByteBufCodecs.VAR_INT, LevelsPayload::woodcutting,
                        ByteBufCodecs.VAR_INT, LevelsPayload::excavating,
                        LevelsPayload::new
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Registers the server-to-client payload type.
     * Call from the common initializer.
     */
    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(
                LevelsPayload.TYPE,
                LevelsPayload.CODEC
        );
    }

    /** Called on the client when the server sends new levels. */
    public static void setClientLevels(
            int mining,
            int woodcutting,
            int excavating
    ) {
        clientLevels = new int[]{mining, woodcutting, excavating};
    }

    public static void resetClientLevels() {
        clientLevels = new int[3];
    }

    // ---------------------------------------------------------------- server cache

    /**
     * Loads all three gathering skill levels from the database once when the player joins.
     * Stores the levels in memory and synchronizes them with the client.
     */
    public static void initPlayerLevels(ServerPlayer player) {
        if (player == null) return;

        String uuid = player.getStringUUID();
        DatabaseManager db = DatabaseManager.getInstance();

        Map<String, DatabaseManager.SkillData> skills = db.getAllSkills(uuid);

        int mining = skills.getOrDefault(
                Skills.MINING.getId(),
                new DatabaseManager.SkillData(0, 1)
        ).level();

        int woodcutting = skills.getOrDefault(
                Skills.WOODCUTTING.getId(),
                new DatabaseManager.SkillData(0, 1)
        ).level();

        int excavating = skills.getOrDefault(
                Skills.EXCAVATING.getId(),
                new DatabaseManager.SkillData(0, 1)
        ).level();

        int[] levels = SERVER_LEVELS.computeIfAbsent(
                player.getUUID(),
                id -> new int[3]
        );

        synchronized (levels) {
            levels[0] = mining;
            levels[1] = woodcutting;
            levels[2] = excavating;
        }

        sync(player, levels);
    }

    private static int indexOf(Skills skill) {
        return switch (skill) {
            case MINING -> 0;
            case WOODCUTTING -> 1;
            case EXCAVATING -> 2;
            default -> -1;
        };
    }

    /** Updates the cached level of one skill and syncs the client when the value changed. */
    public static void setServerLevel(
            ServerPlayer player,
            Skills skill,
            int level
    ) {
        int index = indexOf(skill);

        if (player == null || index < 0) return;

        int[] levels = SERVER_LEVELS.computeIfAbsent(
                player.getUUID(),
                id -> new int[3]
        );

        boolean changed;

        synchronized (levels) {
            changed = levels[index] != level;
            levels[index] = level;
        }

        if (changed) {
            sync(player, levels);
        }
    }

    private static void sync(ServerPlayer player, int[] levels) {
        // Only send to clients that have the mod.
        if (player.connection == null
                || !ServerPlayNetworking.canSend(player, LevelsPayload.TYPE)) {
            return;
        }

        int mining;
        int woodcutting;
        int excavating;

        synchronized (levels) {
            mining = levels[0];
            woodcutting = levels[1];
            excavating = levels[2];
        }

        ServerPlayNetworking.send(
                player,
                new LevelsPayload(mining, woodcutting, excavating)
        );
    }

    public static void clearServerLevels(UUID playerUuid) {
        if (playerUuid != null) {
            SERVER_LEVELS.remove(playerUuid);
        }
    }

    /**
     * Returns a gathering skill level directly from the server cache.
     * Does not perform database queries.
     */
    public static int getSkillLevelFromCache(UUID playerUuid, Skills skill) {
        if (playerUuid == null || skill == null) {
            return 0;
        }

        int index = indexOf(skill);
        if (index < 0) {
            return 0;
        }

        int[] levels = SERVER_LEVELS.get(playerUuid);
        if (levels == null) {
            return 0;
        }

        synchronized (levels) {
            return levels[index];
        }
    }

    // ---------------------------------------------------------------- calculation

    private static int getLevel(Player player, Skills skill) {
        int index = indexOf(skill);

        if (index < 0) return 0;

        if (player.level().isClientSide()) {
            return clientLevels[index];
        }

        int[] levels = SERVER_LEVELS.get(player.getUUID());

        if (levels == null) return 0;

        synchronized (levels) {
            return levels[index];
        }
    }

    private static boolean isCorrectTool(Skills skill, ItemStack stack) {
        if (stack.isEmpty()) return false;

        return switch (skill) {
            case MINING -> stack.is(ItemTags.PICKAXES);
            case WOODCUTTING -> stack.is(ItemTags.AXES);
            case EXCAVATING -> stack.is(ItemTags.SHOVELS);
            default -> false;
        };
    }

    /**
     * @return 1.0f + level * 0.01f when the block's skill (shared classification with XP) is
     *         Mining/Woodcutting/Excavating and the main-hand tool matches it; otherwise 1.0f.
     */
    public static float getMultiplier(Player player, BlockState state) {
        Skills skill = BlockSkillClassifier.classify(state);

        if (skill == null || indexOf(skill) < 0) {
            return 1.0f;
        }

        if (!isCorrectTool(skill, player.getMainHandItem())) {
            return 1.0f;
        }

        int level = Math.max(
                0,
                Math.min(getLevel(player, skill), XPManager.getMaxLevel())
        );

        return 1.0f + (level * BONUS_PER_LEVEL);
    }

    /**
 * Returns the durability protection chance for the matching gathering skill.
 * Each level provides 1% protection, up to 99% at level 99.
 */
public static float getDurabilityProtectionChance(
        Player player,
        BlockState state,
        ItemStack stack
) {
    if (player == null || state == null || stack == null || stack.isEmpty()) {
        return 0.0f;
    }

    Skills skill = BlockSkillClassifier.classify(state);

    if (skill == null || indexOf(skill) < 0) {
        return 0.0f;
    }

    if (!isCorrectTool(skill, stack)) {
        return 0.0f;
    }

    int level = Math.max(
            0,
            Math.min(getLevel(player, skill), XPManager.getMaxLevel())
    );

    return Math.min(level * BONUS_PER_LEVEL, 0.99f);
}

    /**
     * Determines whether this block-breaking action should avoid durability loss.
     */
    public static boolean shouldPreventDurabilityLoss(
            Player player,
            BlockState state,
            ItemStack stack
    ) {
        float chance = getDurabilityProtectionChance(player, state, stack);

        return chance > 0.0f && player.getRandom().nextFloat() < chance;
    }
}
