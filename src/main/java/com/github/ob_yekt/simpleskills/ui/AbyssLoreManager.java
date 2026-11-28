package com.github.ob_yekt.simpleskills.ui;

import com.github.ob_yekt.simpleskills.Simpleskills;
import com.github.ob_yekt.simpleskills.abyss.AbyssalBridgePiece;
import com.github.ob_yekt.simpleskills.abyss.AbyssalCastlePiece;
import com.github.ob_yekt.simpleskills.abyss.AbyssWorldgen;
import com.github.ob_yekt.simpleskills.Skills;
import com.github.ob_yekt.simpleskills.managers.DatabaseManager;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Supplies the lore screen with the real general skill level, exact boss positions stored in
 * generated bridge/castle pieces, and the deterministic X/Z center of the nearest crimson region.
 * Locations are searched once per world and cached for the lifetime of that ServerLevel.
 */
public final class AbyssLoreManager {
    private static final int BRIDGE_UNLOCK_LEVEL = 110;
    private static final int CASTLE_UNLOCK_LEVEL = 140;
    private static final int COLOSSUS_UNLOCK_LEVEL = 170;
    private static final int LOCATE_RADIUS_CHUNKS = 512;
    private static final Map<ServerLevel, WorldLocations> LOCATION_CACHE = new WeakHashMap<>();

    private AbyssLoreManager() {}

    private record WorldLocations(BlockPos bridge, BlockPos castle, BlockPos colossusRegionCenter) {}

    public static void registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(
                AbyssLoreProgressRequestPayload.TYPE, AbyssLoreProgressRequestPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                AbyssLoreProgressDataPayload.TYPE, AbyssLoreProgressDataPayload.CODEC);
    }

    public static void registerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(AbyssLoreProgressRequestPayload.TYPE,
                (payload, context) -> context.server().execute(() -> send(context.player())));
    }

    public static void send(ServerPlayer player) {
        if (player == null || !ServerPlayNetworking.canSend(player, AbyssLoreProgressDataPayload.TYPE)) return;

        try {
            DatabaseManager db = DatabaseManager.getInstance();
            String uuid = player.getStringUUID();
            if (db.getAllSkills(uuid).isEmpty()) db.initializePlayer(uuid);

            // Match the K menu exactly: sum all 15 professions, treating any missing row as level 1.
            var skills = db.getAllSkills(uuid);
            int totalLevel = 0;
            for (Skills skill : Skills.values()) {
                totalLevel += skills.getOrDefault(skill.getId(), new DatabaseManager.SkillData(0, 1)).level();
            }
            int unknown = AbyssLoreProgressDataPayload.UNKNOWN_COORDINATE;
            int bridgeX = unknown, bridgeY = unknown, bridgeZ = unknown;
            int castleX = unknown, castleY = unknown, castleZ = unknown;
            BlockPos colossusCenter = AbyssLoreProgressDataPayload.UNKNOWN_BLOCK_POS;

            if (totalLevel >= BRIDGE_UNLOCK_LEVEL || totalLevel >= CASTLE_UNLOCK_LEVEL
                    || totalLevel >= COLOSSUS_UNLOCK_LEVEL) {
                ServerLevel overworld = player.level().getServer().overworld();
                WorldLocations locations = locationsFor(overworld);
                if (totalLevel >= BRIDGE_UNLOCK_LEVEL && locations.bridge() != null) {
                    bridgeX = locations.bridge().getX();
                    bridgeY = locations.bridge().getY();
                    bridgeZ = locations.bridge().getZ();
                }
                if (totalLevel >= CASTLE_UNLOCK_LEVEL && locations.castle() != null) {
                    castleX = locations.castle().getX();
                    castleY = locations.castle().getY();
                    castleZ = locations.castle().getZ();
                }
                if (totalLevel >= COLOSSUS_UNLOCK_LEVEL && locations.colossusRegionCenter() != null) {
                    colossusCenter = locations.colossusRegionCenter();
                }
            }

            ServerPlayNetworking.send(player, new AbyssLoreProgressDataPayload(
                    totalLevel, bridgeX, bridgeY, bridgeZ, castleX, castleY, castleZ, colossusCenter));
        } catch (Exception exception) {
            Simpleskills.LOGGER.error("Could not send Abyss lore progress to {}: {}",
                    player.getName().getString(), exception.getMessage(), exception);
        }
    }

    private static WorldLocations locationsFor(ServerLevel level) {
        synchronized (LOCATION_CACHE) {
            WorldLocations cached = LOCATION_CACHE.get(level);
            if (cached != null) return cached;

            BlockPos origin = level.getRespawnData().pos();
            BlockPos bridge = locateSpawnPosition(level, "abyssal_bridge", origin, piece ->
                    piece instanceof AbyssalBridgePiece bridgePiece
                            ? bridgePiece.getSamuraiSpawnPosition() : null);
            BlockPos castle = locateSpawnPosition(level, "abyssal_castle", origin, piece ->
                    piece instanceof AbyssalCastlePiece castlePiece
                            ? castlePiece.getFallenKingSpawnPosition() : null);
            AbyssWorldgen.CrimsonCrystalRegion crimsonRegion = AbyssWorldgen.nearestCrimsonCrystalRegion(
                    origin.getX(), origin.getZ(), level.getSeed());
            // X/Z are the exact feature center checked by CrimsonCrystalHeartFeature. Y depends on
            // the natural cavern floor and is therefore described as a depth range on the page.
            BlockPos colossusCenter = crimsonRegion == null ? null
                    : new BlockPos(crimsonRegion.centerX(), 0, crimsonRegion.centerZ());
            WorldLocations found = new WorldLocations(bridge, castle, colossusCenter);
            LOCATION_CACHE.put(level, found);
            return found;
        }
    }

    /**
     * findNearestMapStructure returns a map marker (its Y is normally 0), not the entity's
     * position. Use that marker only to identify the start chunk, then read the generated
     * structure piece and ask it for the exact boss spawn block.
     */
    private static BlockPos locateSpawnPosition(ServerLevel level, String path, BlockPos origin,
                                                Function<StructurePiece, BlockPos> positionResolver) {
        try {
            Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path);
            ResourceKey<Structure> key = ResourceKey.create(Registries.STRUCTURE, id);
            Holder<Structure> holder = level.registryAccess()
                    .lookupOrThrow(Registries.STRUCTURE).get(key).orElse(null);
            if (holder == null) {
                Simpleskills.LOGGER.warn("Abyss lore could not locate structure registry entry {}", id);
                return null;
            }

            BlockPos locateMarker = level.findNearestMapStructure(
                    HolderSet.direct(holder), origin, LOCATE_RADIUS_CHUNKS, false);
            if (locateMarker == null) return null;

            // Structure locate markers use Y=0 by design. The start data is stored in the
            // chunk identified by X/Z, so load only up to STRUCTURE_STARTS (not full terrain).
            ChunkAccess chunk = level.getChunk(
                    locateMarker.getX() >> 4,
                    locateMarker.getZ() >> 4,
                    ChunkStatus.STRUCTURE_STARTS,
                    true);
            if (chunk == null) {
                Simpleskills.LOGGER.warn("Abyss lore could not load structure start chunk for {} at {}",
                        id, locateMarker);
                return null;
            }

            StructureStart structureStart = chunk.getStartForStructure(holder.value());
            if (structureStart == null || !structureStart.isValid()) {
                Simpleskills.LOGGER.warn("Abyss lore found locate marker for {} but no valid structure start at {}",
                        id, locateMarker);
                return null;
            }

            for (StructurePiece piece : structureStart.getPieces()) {
                BlockPos spawnPosition = positionResolver.apply(piece);
                if (spawnPosition != null) return spawnPosition;
            }

            Simpleskills.LOGGER.warn("Abyss lore could not find the expected spawn piece for {} at {}",
                    id, locateMarker);
            return null;
        } catch (RuntimeException exception) {
            Simpleskills.LOGGER.warn("Abyss lore could not find {}: {}", path, exception.getMessage());
            return null;
        }
    }
}
