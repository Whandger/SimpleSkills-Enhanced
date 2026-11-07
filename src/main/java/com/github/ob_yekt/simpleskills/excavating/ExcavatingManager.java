package com.github.ob_yekt.simpleskills.excavating;

import com.github.ob_yekt.simpleskills.GatheringSpeed;
import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.excavating.ExcavatingLoot.Reward;
import com.github.ob_yekt.simpleskills.managers.ConfigManager;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import com.github.ob_yekt.simpleskills.managers.XPManager;
import com.github.ob_yekt.simpleskills.requirements.SkillRequirement;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excavating treasure hunting: hold right click on Sand with a shovel to investigate it.
 *
 * <ul>
 *   <li>The server owns everything: start, progress, cancel, completion, loot roll and XP.
 *       The client only receives {@link InvestigationPayload} to draw the progress bar.</li>
 *   <li>"Holding" is detected through the repeated use-on-block packets the vanilla client sends
 *       every few ticks while the use key is held. If they stop (key released, aiming away,
 *       attacking, ...) for {@link #HOLD_GRACE_TICKS} ticks the investigation is cancelled.</li>
 *   <li>Completing an investigation turns the Sand into {@code simpleskills:investigated_sand},
 *       which can never be investigated again - the block is the permanent mark.</li>
 *   <li>A cancelled investigation gives nothing: no item, no XP, no durability, and the block
 *       stays Sand.</li>
 * </ul>
 */
public final class ExcavatingManager {
    private ExcavatingManager() {}

    /** Ticks without a use packet before the investigation is considered abandoned. */
    private static final int HOLD_GRACE_TICKS = 10;
    /** The server sends the progress to the client every N ticks. */
    private static final int PROGRESS_SYNC_INTERVAL_TICKS = 2;
    /** Brushing sound / particles interval while digging. */
    private static final int EFFECT_INTERVAL_TICKS = 6;

    /** One running investigation. */
    private static final class Session {
        final UUID playerId;
        final ResourceKey<Level> dimension;
        final BlockPos pos;
        final int hotbarSlot;
        final int durationTicks;
        Direction face;
        int elapsedTicks = 0;
        int ticksSinceUse = 0;

        Session(UUID playerId, ResourceKey<Level> dimension, BlockPos pos, Direction face, int hotbarSlot, int durationTicks) {
            this.playerId = playerId;
            this.dimension = dimension;
            this.pos = pos;
            this.face = face;
            this.hotbarSlot = hotbarSlot;
            this.durationTicks = durationTicks;
        }

        GlobalPos globalPos() {
            return GlobalPos.of(dimension, pos);
        }

        boolean isAt(ResourceKey<Level> otherDimension, BlockPos otherPos) {
            return dimension.equals(otherDimension) && pos.equals(otherPos);
        }
    }

    /** At most one investigation per player. */
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();
    /** At most one investigation per block: block -> player investigating it. */
    private static final Map<GlobalPos, UUID> BLOCK_OWNERS = new ConcurrentHashMap<>();

    /** False when the block is missing or the loot table failed validation. */
    private static volatile boolean enabled = false;

    // ------------------------------------------------------------------ setup

    /** Registers the server-to-client payload. Call from the common initializer. */
    public static void registerPayloads() {
        PayloadTypeRegistry.clientboundPlay().register(InvestigationPayload.TYPE, InvestigationPayload.CODEC);
    }

    /** Registers the events. Call from the common initializer, after {@link ModBlocks#register()}. */
    public static void register() {
        String problem = ExcavatingLoot.validate();
        if (problem != null) {
            Simpleskills.LOGGER.error("Excavating loot table is invalid, investigation disabled: {}", problem);
            enabled = false;
        } else {
            enabled = ModBlocks.INVESTIGATED_SAND != null;
        }

        UseBlockCallback.EVENT.register(ExcavatingManager::onUseBlock);
        ServerTickEvents.END_SERVER_TICK.register(ExcavatingManager::onServerTick);

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            if (player != null) {
                cancelFor(player.getUUID(), null);
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            SESSIONS.clear();
            BLOCK_OWNERS.clear();
        });
    }

    // ------------------------------------------------------------------ levels

    /**
     * Current Excavating level. Uses the in-memory cache that Gathering Speed already keeps, and
     * falls back to the database only if the cache has no entry yet.
     */
    public static int getLevel(ServerPlayer player) {
        int cached = GatheringSpeed.getSkillLevelFromCache(player.getUUID(), Skills.EXCAVATING);
        int level = cached > 0 ? cached : XPManager.getSkillLevel(player.getStringUUID(), Skills.EXCAVATING);
        return ExcavatingRank.clampLevel(level);
    }

    /** Tells the player when an Excavating level-up reached a new rank. */
    public static void notifyRankUp(ServerPlayer player, int oldLevel, int newLevel) {
        ExcavatingRank before = ExcavatingRank.forLevel(oldLevel);
        ExcavatingRank after = ExcavatingRank.forLevel(newLevel);

        if (before == after) {
            return;
        }

        player.sendSystemMessage(Component.literal(
                "§6[simpleskills]§f Excavating rank up! You are now " + after.getColoredName()
                        + "§f. New treasure: §7" + after.getUnlocks() + "§f."), false);
    }

    // ------------------------------------------------------------------ start / hold

    private static InteractionResult onUseBlock(net.minecraft.world.entity.player.Player player,
                                                Level world,
                                                InteractionHand hand,
                                                net.minecraft.world.phys.BlockHitResult hit) {
        // Server only; the client just sends its normal use packet and receives the progress.
        if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)
                || !(world instanceof ServerLevel level)) {
            return InteractionResult.PASS;
        }

        if (!enabled) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);

        // Only the original Sand can be investigated. Investigated Sand never starts again.
        if (!state.is(Blocks.SAND)) {
            return InteractionResult.PASS;
        }

        ItemStack held = serverPlayer.getMainHandItem();
        if (!held.is(ItemTags.SHOVELS)) {
            return InteractionResult.PASS;
        }

        // Sneaking with something in the off hand keeps the normal "place a block" behaviour.
        if (serverPlayer.isSecondaryUseActive() && !serverPlayer.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        // While holding right click the client also sends the off-hand use. The shovel in the
        // main hand is digging, so the off hand must not place a block at the same time.
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.CONSUME;
        }

        if (serverPlayer.isSpectator() || !serverPlayer.mayBuild()) {
            return InteractionResult.PASS;
        }

        UUID playerId = serverPlayer.getUUID();
        ResourceKey<Level> dimension = level.dimension();

        // Same block: this is the "keep holding" packet.
        Session current = SESSIONS.get(playerId);
        if (current != null) {
            if (current.isAt(dimension, pos)) {
                current.ticksSinceUse = 0;
                current.face = hit.getDirection();
                return InteractionResult.SUCCESS_SERVER; // server-driven arm swing
            }

            // Changed target: the old investigation is cancelled, a new one starts below.
            cancel(current, serverPlayer);
        }

        // Existing tool requirement (same rule as breaking blocks with this tool).
        if (!meetsToolRequirement(serverPlayer, held)) {
            return InteractionResult.CONSUME;
        }

        // Another player is already investigating this exact block.
        GlobalPos globalPos = GlobalPos.of(dimension, pos);
        UUID owner = BLOCK_OWNERS.get(globalPos);
        if (owner != null && !owner.equals(playerId)) {
            Session ownerSession = SESSIONS.get(owner);
            if (ownerSession != null && ownerSession.isAt(dimension, pos)) {
                serverPlayer.sendSystemMessage(Component.literal(
                        "§6[simpleskills]§f Someone is already investigating this sand."), true);
                return InteractionResult.CONSUME;
            }
            BLOCK_OWNERS.remove(globalPos, owner); // stale entry
        }

        int durationTicks = ExcavatingRank.investigationTicks(getLevel(serverPlayer));
        Session session = new Session(
                playerId,
                dimension,
                pos.immutable(),
                hit.getDirection(),
                serverPlayer.getInventory().getSelectedSlot(),
                durationTicks
        );

        SESSIONS.put(playerId, session);
        BLOCK_OWNERS.put(globalPos, playerId);

        sendProgress(serverPlayer, 0, durationTicks);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Mirrors the tool gate used when breaking blocks (skill level and prestige). */
    private static boolean meetsToolRequirement(ServerPlayer player, ItemStack tool) {
        String toolId = BuiltInRegistries.ITEM.getKey(tool.getItem()).toString();
        SkillRequirement requirement = ConfigManager.getToolRequirement(toolId);

        if (requirement == null) {
            return true;
        }

        int playerLevel = XPManager.getSkillLevel(player.getStringUUID(), requirement.getSkill());
        if (playerLevel < requirement.getLevel()) {
            player.sendSystemMessage(Component.literal(String.format(
                    "§6[simpleskills]§f You need %s level %d to use this tool!",
                    requirement.getSkill().getDisplayName(),
                    requirement.getLevel())), true);
            return false;
        }

        int requiredPrestige = requirement.getRequiredPrestige();
        if (requiredPrestige > 0) {
            int playerPrestige = DatabaseManager.getInstance().getPrestige(player.getStringUUID());
            if (playerPrestige < requiredPrestige) {
                player.sendSystemMessage(Component.literal(String.format(
                        "§6[simpleskills]§f You need Prestige ★%d to use this tool!",
                        requiredPrestige)), true);
                return false;
            }
        }

        return true;
    }

    // ------------------------------------------------------------------ tick

    private static void onServerTick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) {
            return;
        }

        for (Session session : new ArrayList<>(SESSIONS.values())) {
            tickSession(server, session);
        }
    }

    private static void tickSession(MinecraftServer server, Session session) {
        ServerPlayer player = server.getPlayerList().getPlayer(session.playerId);

        if (player == null || player.isRemoved() || !player.isAlive() || player.isSpectator()) {
            cancel(session, player);
            return;
        }

        ServerLevel level = player.level();

        // Must still be in the same dimension and looking at the same, still-uninvestigated Sand.
        if (!level.dimension().equals(session.dimension)) {
            cancel(session, player);
            return;
        }

        BlockState state = level.getBlockState(session.pos);
        if (!state.is(Blocks.SAND)) {
            cancel(session, player); // broken, moved, replaced or already investigated
            return;
        }

        // Must keep using a valid shovel in the same hotbar slot.
        ItemStack held = player.getMainHandItem();
        if (!held.is(ItemTags.SHOVELS) || player.getInventory().getSelectedSlot() != session.hotbarSlot) {
            cancel(session, player);
            return;
        }

        // Must stay close to the block.
        if (!player.isWithinBlockInteractionRange(session.pos, 1.0)) {
            cancel(session, player);
            return;
        }

        // Must keep holding: the use packets stopped arriving.
        session.ticksSinceUse++;
        if (session.ticksSinceUse > HOLD_GRACE_TICKS) {
            cancel(session, player);
            return;
        }

        session.elapsedTicks++;

        if (session.elapsedTicks >= session.durationTicks) {
            complete(player, level, session, state);
            return;
        }

        if (session.elapsedTicks % EFFECT_INTERVAL_TICKS == 0) {
            playDiggingEffects(level, session, state);
        }

        if (session.elapsedTicks % PROGRESS_SYNC_INTERVAL_TICKS == 0) {
            sendProgress(player, session.elapsedTicks, session.durationTicks);
        }
    }

    private static void playDiggingEffects(ServerLevel level, Session session, BlockState sandState) {
        level.playSound(null, session.pos, SoundEvents.BRUSH_SAND, SoundSource.BLOCKS, 0.7F, 1.0F);

        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, sandState),
                session.pos.getX() + 0.5 + session.face.getStepX() * 0.55,
                session.pos.getY() + 0.5 + session.face.getStepY() * 0.55,
                session.pos.getZ() + 0.5 + session.face.getStepZ() * 0.55,
                6,
                0.25, 0.25, 0.25,
                0.05
        );
    }

    // ------------------------------------------------------------------ cancel

    /** Cancels a session: nothing is given, no XP, no durability, the block stays Sand. */
    private static void cancel(Session session, ServerPlayer playerOrNull) {
        removeSession(session);

        if (playerOrNull != null) {
            stopClientBar(playerOrNull);
        }
    }

    private static void cancelFor(UUID playerId, ServerPlayer playerOrNull) {
        Session session = SESSIONS.get(playerId);
        if (session != null) {
            cancel(session, playerOrNull);
        }
    }

    private static void removeSession(Session session) {
        SESSIONS.remove(session.playerId, session);
        BLOCK_OWNERS.remove(session.globalPos(), session.playerId);
    }

    // ------------------------------------------------------------------ complete

    /**
     * Completes an investigation. Runs exactly once per session (the session is removed first)
     * and entirely on the server.
     */
    private static void complete(ServerPlayer player, ServerLevel level, Session session, BlockState sandState) {
        // Remove first: a finished investigation can never be repeated or double-rewarded.
        removeSession(session);
        stopClientBar(player);

        // The block is the permanent mark. If it cannot be transformed, nothing is granted.
        boolean transformed = level.setBlock(
                session.pos,
                ModBlocks.INVESTIGATED_SAND.defaultBlockState(),
                Block.UPDATE_ALL
        );

        if (!transformed) {
            return;
        }

        // Level at the moment of completion; one single roll, server side only.
        int excavatingLevel = getLevel(player);
        Reward reward = ExcavatingLoot.roll(excavatingLevel, level.getRandom().nextDouble());

        // Durability: exactly 1 point per completed investigation, through the EXISTING
        // Excavating durability-protection perk (no second chance, no new formula).
        ItemStack tool = player.getMainHandItem();
        if (!GatheringSpeed.shouldPreventDurabilityLoss(player, sandState, tool)) {
            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }

        // Reward item.
        ItemStack found = ItemStack.EMPTY;
        if (reward != null) {
            try {
                found = createRewardStack(reward, level);
            } catch (Exception e) {
                Simpleskills.LOGGER.error("Failed to create Excavating reward {}", reward, e);
            }

            if (!found.isEmpty()) {
                Block.popResourceFromFace(level, session.pos, session.face, found);
            }
        }

        // XP through the existing progression system: 40 base, plus the rarity bonus of the item.
        int xp = (reward != null && !found.isEmpty())
                ? reward.getTotalXp()
                : ExcavatingRarity.BASE_XP;
        XPManager.addXPWithNotification(player, Skills.EXCAVATING, xp);

        playCompletionEffects(level, session, sandState, reward, found);
    }

    private static void playCompletionEffects(ServerLevel level, Session session, BlockState sandState,
                                              Reward reward, ItemStack found) {
        level.playSound(null, session.pos, SoundEvents.BRUSH_SAND_COMPLETED, SoundSource.BLOCKS, 1.0F, 1.0F);

        level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, sandState),
                session.pos.getX() + 0.5 + session.face.getStepX() * 0.55,
                session.pos.getY() + 0.5 + session.face.getStepY() * 0.55,
                session.pos.getZ() + 0.5 + session.face.getStepZ() * 0.55,
                14,
                0.3, 0.3, 0.3,
                0.08
        );

        if (reward == null || found.isEmpty()) {
            return;
        }

        ExcavatingRarity rarity = reward.getRarity();
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(session.playerId);

        // Very Rare and above are announced to the finder.
        if (player != null && rarity.ordinal() >= ExcavatingRarity.VERY_RARE.ordinal()) {
            player.sendSystemMessage(Component.literal(
                            "§6[simpleskills]§f You found " + rarity.getColorCode() + "[" + rarity.getDisplayName() + "]§f ")
                    .append(found.getHoverName())
                    .append(Component.literal("§f!")), false);
        }

        // Legendary and Mythic finds get a fanfare.
        if (rarity.ordinal() >= ExcavatingRarity.LEGENDARY.ordinal()) {
            level.playSound(null, session.pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    session.pos.getX() + 0.5, session.pos.getY() + 1.2, session.pos.getZ() + 0.5,
                    20, 0.4, 0.4, 0.4, 0.05);
        }
    }

    // ------------------------------------------------------------------ rewards

    private static ItemStack createRewardStack(Reward reward, ServerLevel level) {
        RandomSource random = level.getRandom();

        return switch (reward) {
            case DIRT -> new ItemStack(Items.DIRT);
            case FLINT -> new ItemStack(Items.FLINT);
            case BONE_MEAL -> new ItemStack(Items.BONE_MEAL);
            case DEAD_BUSH -> new ItemStack(Items.DEAD_BUSH);
            case IRON_NUGGET -> new ItemStack(Items.IRON_NUGGET);
            case IRON_INGOT -> new ItemStack(Items.IRON_INGOT);
            case DIAMOND -> new ItemStack(Items.DIAMOND);
            case EMERALD -> new ItemStack(Items.EMERALD);
            case REDSTONE -> new ItemStack(Items.REDSTONE);
            case GOLD_NUGGET -> new ItemStack(Items.GOLD_NUGGET);
            case GOLD_INGOT -> new ItemStack(Items.GOLD_INGOT);
            case COAL -> new ItemStack(Items.COAL);
            case GLOWSTONE_DUST -> new ItemStack(Items.GLOWSTONE_DUST);
            case COPPER_INGOT -> new ItemStack(Items.COPPER_INGOT);
            case AMETHYST_SHARD -> new ItemStack(Items.AMETHYST_SHARD);
            case LAPIS_LAZULI -> new ItemStack(Items.LAPIS_LAZULI);
            case PRISMARINE_SHARD -> new ItemStack(Items.PRISMARINE_SHARD);
            case NAUTILUS_SHELL -> new ItemStack(Items.NAUTILUS_SHELL);
            case ENCHANTED_BOOK -> createEnchantedBook(level, random);
            case POTTERY_SHERD -> createPotterySherd(random);
            case ECHO_SHARD -> new ItemStack(Items.ECHO_SHARD);
            case GOLDEN_APPLE -> new ItemStack(Items.GOLDEN_APPLE);
            case HEART_OF_THE_SEA -> new ItemStack(Items.HEART_OF_THE_SEA);
            case ANCIENT_DEBRIS -> new ItemStack(Items.ANCIENT_DEBRIS);
            case NETHERITE_SCRAP -> new ItemStack(Items.NETHERITE_SCRAP);
            case NETHERITE_INGOT -> new ItemStack(Items.NETHERITE_INGOT);
            case TOTEM_OF_UNDYING -> new ItemStack(Items.TOTEM_OF_UNDYING);
            case ENCHANTED_GOLDEN_APPLE -> new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        };
    }

    /** A random decorated-pot sherd from the vanilla tag. */
    private static ItemStack createPotterySherd(RandomSource random) {
        List<Holder<Item>> sherds = new ArrayList<>();
        for (Holder<Item> sherd : BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.DECORATED_POT_SHERDS)) {
            sherds.add(sherd);
        }

        if (sherds.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(sherds.get(random.nextInt(sherds.size())));
    }

    /**
     * An Enchanted Book with a valid vanilla enchantment (random-loot enchantments, no curses)
     * at a level between 1 and that enchantment's maximum level.
     */
    private static ItemStack createEnchantedBook(ServerLevel level, RandomSource random) {
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        List<Holder<Enchantment>> pool = new ArrayList<>();
        enchantments.get(EnchantmentTags.ON_RANDOM_LOOT).ifPresent(set -> set.stream()
                .filter(holder -> !holder.is(EnchantmentTags.CURSE))
                .forEach(pool::add));

        if (pool.isEmpty()) {
            enchantments.listElements()
                    .filter(holder -> !holder.is(EnchantmentTags.CURSE))
                    .forEach(pool::add);
        }

        if (pool.isEmpty()) {
            return ItemStack.EMPTY;
        }

        Holder<Enchantment> enchantment = pool.get(random.nextInt(pool.size()));
        int enchantmentLevel = 1 + random.nextInt(Math.max(1, enchantment.value().getMaxLevel()));

        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.enchant(enchantment, enchantmentLevel);
        return book;
    }

    // ------------------------------------------------------------------ networking

    private static void sendProgress(ServerPlayer player, int elapsedTicks, int durationTicks) {
        // Only clients that have the mod understand the payload.
        if (player.connection != null && ServerPlayNetworking.canSend(player, InvestigationPayload.TYPE)) {
            ServerPlayNetworking.send(player, new InvestigationPayload(elapsedTicks, durationTicks));
        }
    }

    private static void stopClientBar(ServerPlayer player) {
        if (player.connection != null && ServerPlayNetworking.canSend(player, InvestigationPayload.TYPE)) {
            ServerPlayNetworking.send(player, InvestigationPayload.stop());
        }
    }
}
