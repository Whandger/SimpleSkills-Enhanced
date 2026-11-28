package com.github.ob_yekt.simpleskills.abyss;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates the remains of the dense, multi-tiered settlement that once surrounded the Abyssal Castle.
 * All decorative and structural blocks come from the custom Abyss palette. The layout is
 * deterministic from the castle seed and every write is clipped to the current chunk box.
 */
final class AbyssalRuinedKingdom {
    private AbyssalRuinedKingdom() {}

    /** Facing constants: 0 = NORTH (-Z), 1 = WEST (-X), 2 = SOUTH (+Z), 3 = EAST (+X). */
    private static final int FACING_NORTH = 0;
    private static final int FACING_WEST = 1;
    private static final int FACING_SOUTH = 2;
    private static final int FACING_EAST = 3;

    /**
     * Each lot: x, z, half-width, half-depth, wall height, architectural style, facing direction.
     * Organized into tight urban blocks and residential clusters separated by streets and narrow alleys.
     * Styles:
     *   0 = Stone Cottage
     *   1 = 2-Story Merchant Shop / Townhouse
     *   2 = Gilded Noble Manor (Hipped Roof + Portico)
     *   3 = L-Shaped Courtyard House
     *   4 = Tall Narrow Tower-House (Crenellated Roof)
     *   5 = Ruined Chapel / Sanctuary
     *   6 = Fortified Watchtower / Gatehouse
     */
    private static final int[][] HOUSE_LOTS = {
            // --- North Quarter: Inner Clusters (between Inner Ring and North Cross-Street) ---
            {-16, -98, 4, 3, 5, 2, FACING_SOUTH},
            {-28, -98, 4, 3, 5, 1, FACING_SOUTH},
            {-16, -110, 4, 3, 5, 1, FACING_NORTH},
            {-28, -110, 4, 3, 5, 0, FACING_NORTH},

            {16, -98, 4, 3, 5, 2, FACING_SOUTH},
            {28, -98, 4, 3, 5, 1, FACING_SOUTH},
            {16, -110, 4, 3, 5, 1, FACING_NORTH},
            {28, -110, 4, 3, 5, 0, FACING_NORTH},

            // --- North Quarter: Outer Clusters (between North Cross-Street and Outer Ring) ---
            {-16, -127, 4, 3, 5, 2, FACING_SOUTH},
            {-28, -127, 4, 3, 5, 1, FACING_SOUTH},
            {-16, -141, 4, 4, 5, 3, FACING_NORTH},
            {-28, -141, 4, 4, 5, 0, FACING_NORTH},

            {16, -127, 4, 3, 5, 2, FACING_SOUTH},
            {28, -127, 4, 3, 5, 1, FACING_SOUTH},
            {16, -141, 4, 4, 5, 3, FACING_NORTH},
            {28, -141, 4, 4, 5, 0, FACING_NORTH},

            // --- North Quarter: Mid-North Flanking Clusters ---
            {-45, -100, 4, 4, 5, 1, FACING_EAST},
            {-45, -111, 4, 3, 5, 0, FACING_SOUTH},
            {-59, -106, 4, 4, 5, 3, FACING_SOUTH},
            {-46, -128, 4, 4, 5, 2, FACING_NORTH},
            {-60, -130, 4, 4, 5, 4, FACING_NORTH},

            {45, -100, 4, 4, 5, 1, FACING_WEST},
            {45, -111, 4, 3, 5, 0, FACING_SOUTH},
            {59, -106, 4, 4, 5, 3, FACING_SOUTH},
            {46, -128, 4, 4, 5, 2, FACING_NORTH},
            {60, -130, 4, 4, 5, 4, FACING_NORTH},

            // --- Northwest & Northeast Clusters (around Diagonal Quarter Avenues) ---
            {-96, -114, 4, 3, 5, 2, FACING_SOUTH},
            {-84, -104, 4, 3, 5, 1, FACING_WEST},
            {-126, -96, 4, 4, 5, 0, FACING_EAST},
            {-126, -82, 4, 4, 5, 3, FACING_EAST},
            {-98, -80, 4, 3, 5, 1, FACING_SOUTH},
            {-96, -58, 4, 3, 5, 0, FACING_NORTH},
            {-130, -58, 4, 3, 5, 1, FACING_NORTH},

            {96, -114, 4, 3, 5, 2, FACING_SOUTH},
            {84, -104, 4, 3, 5, 1, FACING_EAST},
            {126, -96, 4, 4, 5, 0, FACING_WEST},
            {126, -82, 4, 4, 5, 3, FACING_WEST},
            {98, -80, 4, 3, 5, 1, FACING_SOUTH},
            {96, -58, 4, 3, 5, 0, FACING_NORTH},
            {130, -58, 4, 3, 5, 1, FACING_NORTH},

            // --- West Artisans' Ward: Dense Back-to-Back Blocks with Central Alleys ---
            // Block W1 (south of West Plaza, z = -26)
            {-92, -26, 4, 3, 5, 1, FACING_WEST},
            {-106, -26, 4, 3, 5, 0, FACING_EAST},
            {-123, -26, 4, 3, 5, 1, FACING_WEST},
            {-138, -26, 4, 3, 5, 3, FACING_EAST},

            // Block W2 (north of West Trade Avenue, z = -7)
            {-92, -7, 4, 4, 5, 1, FACING_WEST},
            {-106, -7, 4, 4, 5, 4, FACING_EAST},
            {-123, -7, 4, 4, 5, 1, FACING_WEST},
            {-138, -7, 4, 4, 5, 0, FACING_EAST},

            // Block W3 (south of West Trade Avenue, z = 20)
            {-92, 20, 4, 4, 5, 1, FACING_WEST},
            {-106, 20, 4, 4, 5, 3, FACING_EAST},
            {-123, 20, 4, 4, 5, 0, FACING_WEST},
            {-138, 20, 4, 4, 5, 1, FACING_EAST},

            // Block W4 (mid-south West Ward, z = 46)
            {-92, 46, 4, 4, 5, 3, FACING_WEST},
            {-106, 46, 4, 4, 5, 0, FACING_EAST},
            {-123, 46, 4, 4, 5, 4, FACING_WEST},
            {-138, 46, 4, 4, 5, 0, FACING_EAST},

            // Block W5 (south-west corner cluster near watchtower, z = 72)
            {-96, 72, 4, 4, 5, 1, FACING_EAST},
            {-112, 72, 4, 4, 5, 0, FACING_WEST},

            // --- East Noble & Residential Ward: Dense Back-to-Back Blocks with Central Alleys ---
            // Block E1 (south of East Noble Plaza, z = -26)
            {92, -26, 4, 3, 5, 2, FACING_EAST},
            {106, -26, 4, 3, 5, 1, FACING_WEST},
            {123, -26, 4, 3, 5, 2, FACING_EAST},
            {138, -26, 4, 3, 5, 3, FACING_WEST},

            // Block E2 (north of East Trade Avenue, z = -7)
            {92, -7, 4, 4, 5, 1, FACING_EAST},
            {106, -7, 4, 4, 5, 4, FACING_WEST},
            {123, -7, 4, 4, 5, 2, FACING_EAST},
            {138, -7, 4, 4, 5, 0, FACING_WEST},

            // Block E3 (south of East Trade Avenue, z = 20)
            {92, 20, 4, 4, 5, 1, FACING_EAST},
            {106, 20, 4, 4, 5, 2, FACING_WEST},
            {123, 20, 4, 4, 5, 3, FACING_EAST},
            {138, 20, 4, 4, 5, 1, FACING_WEST},

            // Block E4 (mid-south East Ward, z = 46)
            {92, 46, 4, 4, 5, 3, FACING_EAST},
            {106, 46, 4, 4, 5, 1, FACING_WEST},
            {123, 46, 4, 4, 5, 0, FACING_EAST},
            {138, 46, 4, 4, 5, 2, FACING_WEST},

            // Block E5 (south-east corner cluster, z = 72)
            {96, 72, 4, 4, 5, 4, FACING_WEST},
            {112, 72, 4, 4, 5, 1, FACING_EAST},
            {128, 72, 4, 4, 5, 2, FACING_WEST},

            // --- South Market & Merchant District (Dense Rowhouses & Alley Blocks) ---
            {-24, 111, 4, 4, 5, 1, FACING_EAST},
            {-24, 133, 4, 4, 5, 1, FACING_EAST},
            {-24, 146, 4, 3, 5, 0, FACING_EAST},

            {24, 111, 4, 4, 5, 1, FACING_WEST},
            {24, 133, 4, 4, 5, 1, FACING_WEST},
            {24, 146, 4, 3, 5, 0, FACING_WEST},

            {-49, 111, 4, 4, 5, 1, FACING_SOUTH},
            {-49, 134, 4, 4, 5, 3, FACING_NORTH},
            {-49, 147, 4, 3, 5, 0, FACING_SOUTH},

            {49, 111, 4, 4, 5, 1, FACING_SOUTH},
            {49, 134, 4, 4, 5, 3, FACING_NORTH},
            {49, 147, 4, 3, 5, 0, FACING_SOUTH},

            {-74, 112, 4, 4, 5, 1, FACING_SOUTH},
            {-74, 134, 4, 4, 5, 0, FACING_NORTH},
            {-88, 126, 4, 4, 5, 4, FACING_EAST},

            {74, 112, 4, 4, 5, 1, FACING_SOUTH},
            {74, 134, 4, 4, 5, 0, FACING_NORTH},
            {88, 126, 4, 4, 5, 4, FACING_WEST},

            // --- Outer Ring Gate-Hamlets (Clustered Suburbs outside the Outer Ring Avenue) ---
            // North Gate Hamlet
            {-26, -164, 4, 3, 5, 1, FACING_SOUTH},
            {-38, -164, 4, 3, 5, 0, FACING_SOUTH},
            {-26, -175, 4, 3, 5, 0, FACING_NORTH},
            {26, -164, 4, 3, 5, 1, FACING_SOUTH},
            {38, -164, 4, 3, 5, 0, FACING_SOUTH},
            {26, -175, 4, 3, 5, 0, FACING_NORTH},

            // South Gate Hamlet
            {-26, 174, 4, 3, 5, 1, FACING_NORTH},
            {-38, 174, 4, 3, 5, 0, FACING_NORTH},
            {-26, 185, 4, 3, 5, 0, FACING_SOUTH},
            {26, 174, 4, 3, 5, 1, FACING_NORTH},
            {38, 174, 4, 3, 5, 0, FACING_NORTH},
            {26, 185, 4, 3, 5, 0, FACING_SOUTH},

            // West Gate Hamlet
            {-162, -12, 4, 4, 5, 1, FACING_EAST},
            {-162, -26, 4, 4, 5, 3, FACING_EAST},
            {-174, -18, 4, 4, 5, 0, FACING_WEST},
            {-162, 24, 4, 4, 5, 1, FACING_EAST},
            {-162, 38, 4, 4, 5, 0, FACING_EAST},
            {-174, 30, 4, 4, 5, 0, FACING_WEST},

            // East Gate Hamlet
            {162, -12, 4, 4, 5, 1, FACING_WEST},
            {162, -26, 4, 4, 5, 3, FACING_WEST},
            {174, -18, 4, 4, 5, 0, FACING_EAST},
            {162, 24, 4, 4, 5, 1, FACING_WEST},
            {162, 38, 4, 4, 5, 0, FACING_WEST},
            {174, 30, 4, 4, 5, 0, FACING_EAST}
    };

    /**
     * Major arterial avenues & district streets: {x1, z1, x2, z2, halfWidth, salt, elevationOffset}.
     * Elevation offsets create stepped terrace levels across the mountain relief.
     */
    private static final int[][] ARTERIAL_ROADS = {
            // North Pilgrims' Way (connects Inner Ring -> Cathedral -> Outer Ring -> North Gate)
            {0, -88, 0, -104, 2, 401, 1},
            {0, -128, 0, -180, 2, 402, 0},

            // West Trade Avenue (stepped terrace from Inner Ring to West Hamlet)
            {-82, 6, -114, 6, 2, 403, 1},
            {-114, 6, -148, 6, 2, 404, 0},
            {-148, 6, -180, 6, 2, 405, 0},

            // East Trade Avenue (stepped terrace from Inner Ring to East Hamlet)
            {82, 6, 114, 6, 2, 406, 1},
            {114, 6, 148, 6, 2, 407, 0},
            {148, 6, 180, 6, 2, 408, 0},

            // Diagonal Quarter Avenues (connect Inner Ring -> Mid-Terraces -> Outer Ring)
            {-56, -62, -84, -90, 1, 410, 1},
            {-84, -90, -108, -114, 1, 411, 0},
            {-108, -114, -128, -134, 1, 412, 0},

            {56, -62, 84, -90, 1, 413, 1},
            {84, -90, 108, -114, 1, 414, 0},
            {108, -114, 128, -134, 1, 415, 0},

            {-56, 74, -84, 102, 1, 416, 1},
            {-84, 102, -108, 126, 1, 417, 0},
            {-108, 126, -128, 146, 1, 418, 0},

            {56, 74, 84, 102, 1, 419, 1},
            {84, 102, 108, 126, 1, 420, 0},
            {108, 126, 128, 146, 1, 421, 0},

            // North District Terrace & Cross-Streets
            {-76, -118, -10, -118, 1, 422, 1},
            {10, -118, 76, -118, 1, 423, 1},
            {-36, -90, -36, -152, 1, 424, 0},
            {36, -90, 36, -152, 1, 425, 0},

            // Northwest & Northeast Neighborhood Streets
            {-142, -68, -78, -68, 1, 426, 0},
            {78, -68, 142, -68, 1, 427, 0},
            {-114, -92, -114, -49, 1, 428, 0},
            {114, -92, 114, -49, 1, 429, 0},

            // West Artisans' Ward Mid-Terrace Avenue & Cross-Streets
            {-114, -35, -114, 60, 1, 430, 1},
            {-114, 60, -104, 86, 1, 431, 0},
            {-146, -18, -84, -18, 1, 432, 0},
            {-146, 32, -84, 32, 1, 433, 0},
            {-144, 60, -84, 60, 1, 434, 0},

            // East Noble Ward Mid-Terrace Avenue & Cross-Streets
            {114, -35, 114, 60, 1, 435, 1},
            {114, 60, 104, 86, 1, 436, 0},
            {84, -18, 146, -18, 1, 437, 0},
            {84, 32, 146, 32, 1, 438, 0},
            {84, 60, 144, 60, 1, 439, 0},

            // South Market District Streets & South Gate Avenue
            {-78, 124, -14, 124, 1, 440, 1},
            {14, 124, 78, 124, 1, 441, 1},
            {-36, 98, -36, 154, 1, 442, 0},
            {36, 98, 36, 154, 1, 443, 0},
            {-62, 92, -62, 146, 1, 444, 0},
            {62, 92, 62, 146, 1, 445, 0},
            {0, 143, 0, 188, 2, 446, 0},

            // Suburban Hamlet Access Lanes
            {-44, -158, 44, -158, 1, 447, 0},
            {-44, 168, 44, 168, 1, 448, 0},
            {-156, -34, -156, 46, 1, 449, 0},
            {156, -34, 156, 46, 1, 450, 0}
    };

    /**
     * Narrow residential alleys & vielas cutting through the dense housing clusters:
     * {x1, z1, x2, z2, salt}. Rendered with stone gutters and overhead stone arches.
     */
    private static final int[][] ALLEY_ROADS = {
            // North Quarter alleys between front & back house rows
            {-34, -104, -10, -104, 601},
            {10, -104, 34, -104, 602},
            {-34, -134, -10, -134, 603},
            {10, -134, 34, -134, 604},
            {-52, -134, -52, -96, 605},
            {52, -134, 52, -96, 606},

            // NW & NE Quarter alleys
            {-128, -89, -115, -89, 607},
            {-105, -82, -105, -56, 608},
            {115, -89, 128, -89, 609},
            {105, -82, 105, -56, 610},

            // West Artisans' Ward narrow back-alleys
            {-99, -32, -99, 56, 611},
            {-131, -32, -131, 56, 612},
            {-104, 66, -104, 78, 613},

            // East Noble Ward narrow back-alleys
            {99, -32, 99, 56, 614},
            {131, -32, 131, 56, 615},
            {104, 66, 104, 78, 616},

            // South Merchant District alleys
            {-60, 141, -38, 141, 617},
            {38, 141, 60, 141, 618},
            {-81, 118, -81, 136, 619},
            {81, 118, 81, 136, 620},

            // Suburban Hamlet alleys
            {-42, -169, -20, -169, 621},
            {20, -169, 42, -169, 622},
            {-42, 179, -20, 179, 623},
            {20, 179, 42, 179, 624},
            {-168, -30, -168, -8, 625},
            {-168, 20, -168, 42, 626},
            {168, -30, 168, -8, 627},
            {168, 20, 168, 42, 628}
    };

    /** Precomputed road & alley anchor points so every building threshold links to the nearest street. */
    private static final int[][] ROAD_NODES;

    static {
        List<int[]> nodes = new ArrayList<>();
        // Inner Ring nodes
        for (int i = 0; i < 40; i++) {
            double angle = (Math.PI * 2.0 * i) / 40.0;
            nodes.add(new int[]{
                    (int) Math.round(Math.sin(angle) * 82.0),
                    6 + (int) Math.round(Math.cos(angle) * 94.0)
            });
        }
        // Outer Ring nodes
        for (int i = 0; i < 48; i++) {
            double angle = (Math.PI * 2.0 * i) / 48.0;
            nodes.add(new int[]{
                    (int) Math.round(Math.sin(angle) * 148.0),
                    6 + (int) Math.round(Math.cos(angle) * 160.0)
            });
        }
        // Sample points along all arterial and district roads
        for (int[] road : ARTERIAL_ROADS) {
            sampleNodesAlongSegment(nodes, road[0], road[1], road[2], road[3], 6.0);
        }
        // Sample points along all narrow alleys
        for (int[] alley : ALLEY_ROADS) {
            sampleNodesAlongSegment(nodes, alley[0], alley[1], alley[2], alley[3], 5.0);
        }
        // South Royal Approach & Market perimeter nodes
        nodes.add(new int[]{0, 103});
        nodes.add(new int[]{0, 114});
        nodes.add(new int[]{-14, 132});
        nodes.add(new int[]{14, 132});
        ROAD_NODES = nodes.toArray(new int[0][]);
    }

    private static void sampleNodesAlongSegment(List<int[]> nodes, int x1, int z1, int x2, int z2, double spacing) {
        int samples = Math.max(2, (int) (Math.hypot(x2 - x1, z2 - z1) / spacing));
        for (int s = 0; s <= samples; s++) {
            double t = s / (double) samples;
            nodes.add(new int[]{
                    (int) Math.round(x1 + (x2 - x1) * t),
                    (int) Math.round(z1 + (z2 - z1) * t)
            });
        }
    }

    static void generate(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle) {
        // 1. Plazas and public squares (leveled terraces).
        buildMarketSquare(level, box, castle);
        buildNeighborhoodPlaza(level, box, castle, -114, -42, 7, true, 520);
        buildNeighborhoodPlaza(level, box, castle, 114, -42, 7, false, 540);

        // 2. Dense residential, commercial, and noble districts (119 clustered buildings).
        for (int i = 0; i < HOUSE_LOTS.length; i++) {
            int[] lot = HOUSE_LOTS[i];
            buildHouse(level, box, castle, lot[0], lot[1], lot[2], lot[3], lot[4], lot[5], lot[6], i);
        }

        // 3. Civic & defensive landmarks.
        // Ruined northern sanctuary/cathedral
        buildHouse(level, box, castle, 0, -116, 8, 11, 7, 5, FACING_SOUTH, 190);
        // Western inner watch post
        buildHouse(level, box, castle, -128, 72, 5, 5, 7, 6, FACING_EAST, 191);
        // North & South Outer Gatehouse Bastions
        buildHouse(level, box, castle, -14, -168, 4, 4, 7, 6, FACING_SOUTH, 192);
        buildHouse(level, box, castle, 14, -168, 4, 4, 7, 6, FACING_SOUTH, 193);
        buildHouse(level, box, castle, -14, 174, 4, 4, 7, 6, FACING_NORTH, 194);
        buildHouse(level, box, castle, 14, 174, 4, 4, 7, 6, FACING_NORTH, 195);

        // 4. Stepped ring avenues, district streets, narrow arched alleys, and south viaduct ramp.
        // Built after houses so curbs, stairs, and retaining walls stay clean and unobstructed.
        buildEllipseRing(level, box, castle, 82.0, 94.0, 40, 2, 100, 1);
        buildEllipseRing(level, box, castle, 148.0, 160.0, 48, 2, 200, 0);
        buildArterialAndAlleyNetwork(level, box, castle);
        buildSouthApproach(level, box, castle);
    }

    private static void buildEllipseRing(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                         double radiusX, double radiusZ, int segments, int halfWidth,
                                         int saltBase, int elevationOffset) {
        int previousX = (int) Math.round(Math.sin(0) * radiusX);
        int previousZ = 6 + (int) Math.round(Math.cos(0) * radiusZ);
        for (int i = 1; i <= segments; i++) {
            double angle = (Math.PI * 2.0 * i) / segments;
            int x = (int) Math.round(Math.sin(angle) * radiusX);
            int z = 6 + (int) Math.round(Math.cos(angle) * radiusZ);
            buildRoadSegment(level, box, castle, previousX, previousZ, x, z,
                    halfWidth, saltBase + i, elevationOffset, false);
            previousX = x;
            previousZ = z;
        }
    }

    private static void buildSouthApproach(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle) {
        // The old royal road descends from the viaduct in a shallow, supported stair-ramp.
        int[][] points = {{0, 94}, {1, 103}, {-1, 112}, {0, 121}, {0, 132}};
        for (int i = 0; i < points.length - 1; i++) {
            int x1 = points[i][0];
            int z1 = points[i][1];
            int x2 = points[i + 1][0];
            int z2 = points[i + 1][1];
            if (!intersectsXZ(box, Math.min(x1, x2) - 5, Math.max(x1, x2) + 5,
                    Math.min(z1, z2) - 5, Math.max(z1, z2) + 5, castle)) continue;
            double dx = x2 - x1;
            double dz = z2 - z1;
            double length = Math.max(1.0, Math.hypot(dx, dz));
            int steps = Math.max(1, (int) Math.ceil(length * 2.0));
            double nx = -dz / length;
            double nz = dx / length;

            int[] profile = computeRoadProfile(castle, x1, z1, x2, z2, steps, 1);

            for (int step = 0; step <= steps; step++) {
                double t = step / (double) steps;
                int cx = (int) Math.round(x1 + dx * t);
                int cz = (int) Math.round(z1 + dz * t);
                int prevY = step > 0 ? profile[step - 1] : profile[step];
                int nextY = step < steps ? profile[step + 1] : profile[step];
                int roadY = profile[step];

                for (int w = -3; w <= 3; w++) {
                    int x = cx + (int) Math.round(nx * w);
                    int z = cz + (int) Math.round(nz * w);
                    int groundY = castle.getCitySurfaceY(x, z);
                    int effectiveY = Math.max(groundY + 1, roadY);

                    fillRoadSupport(level, box, castle, x, z, groundY, effectiveY, Math.abs(w) == 3);
                    clearRoadHeadroom(level, box, castle, x, z, effectiveY, 3);

                    BlockState road;
                    if (Math.abs(w) <= 1 && effectiveY > prevY && effectiveY == nextY) {
                        road = tileStairs(vectorToDirection(dx, dz), Half.BOTTOM);
                    } else if (Math.abs(w) <= 1 && effectiveY > nextY && effectiveY == prevY) {
                        road = tileStairs(vectorToDirection(-dx, -dz), Half.BOTTOM);
                    } else if (Math.abs(w) <= 1) {
                        road = AbyssContent.ABYSSAL_TILES.defaultBlockState();
                    } else {
                        road = (castle.cityHash(x, 202 + i, z, 4) == 0
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    }
                    place(level, box, castle, x, effectiveY, z, road);

                    if (Math.abs(w) == 3 && effectiveY > groundY + 1) {
                        place(level, box, castle, x, effectiveY + 1, z,
                                AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void buildArterialAndAlleyNetwork(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle) {
        // 1. Build all arterial avenues and district streets.
        for (int[] road : ARTERIAL_ROADS) {
            buildRoadSegment(level, box, castle, road[0], road[1], road[2], road[3],
                    road[4], road[5], road[6], false);
        }

        // 2. Build all narrow residential alleys with overhead stone arches.
        for (int[] alley : ALLEY_ROADS) {
            buildRoadSegment(level, box, castle, alley[0], alley[1], alley[2], alley[3],
                    0, alley[4], 0, true);
        }

        // 3. Connect every house's front threshold cleanly to its adjacent street or alley.
        for (int i = 0; i < HOUSE_LOTS.length; i++) {
            int[] lot = HOUSE_LOTS[i];
            int doorX = lot[0] + rotX(0, lot[3] + 2, lot[6]);
            int doorZ = lot[1] + rotZ(0, lot[3] + 2, lot[6]);
            int nearest = 0;
            double best = Double.MAX_VALUE;
            for (int p = 0; p < ROAD_NODES.length; p++) {
                double dx = ROAD_NODES[p][0] - doorX;
                double dz = ROAD_NODES[p][1] - doorZ;
                double distance = dx * dx + dz * dz;
                if (distance < best) {
                    best = distance;
                    nearest = p;
                }
            }
            if (best <= 14.0 * 14.0) {
                buildRoadSegment(level, box, castle, doorX, doorZ,
                        ROAD_NODES[nearest][0], ROAD_NODES[nearest][1], 0, 300 + i, 0, false);
            }
        }
    }

    private static void buildRoadSegment(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                         int x1, int z1, int x2, int z2, int halfWidth, int salt,
                                         int elevationOffset, boolean isAlley) {
        int margin = halfWidth + 3;
        if (!intersectsXZ(box, Math.min(x1, x2) - margin, Math.max(x1, x2) + margin,
                Math.min(z1, z2) - margin, Math.max(z1, z2) + margin, castle)) return;

        double dx = x2 - x1;
        double dz = z2 - z1;
        double length = Math.max(1.0, Math.hypot(dx, dz));
        int steps = Math.max(1, (int) Math.ceil(length * 2.0));
        double nx = -dz / length;
        double nz = dx / length;

        int[] profile = computeRoadProfile(castle, x1, z1, x2, z2, steps, elevationOffset);
        Direction climbForward = vectorToDirection(dx, dz);
        Direction climbBackward = vectorToDirection(-dx, -dz);

        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int cx = (int) Math.round(x1 + dx * t);
            int cz = (int) Math.round(z1 + dz * t);
            int roadY = profile[step];
            int prevY = step > 0 ? profile[step - 1] : roadY;
            int nextY = step < steps ? profile[step + 1] : roadY;

            boolean stepUpForward = (roadY > prevY && roadY == nextY);
            boolean stepUpBackward = (roadY > nextY && roadY == prevY);

            int outerW = isAlley ? 1 : (halfWidth + 1);
            for (int w = -outerW; w <= outerW; w++) {
                int x = cx + (int) Math.round(nx * w);
                int z = cz + (int) Math.round(nz * w);
                int groundY = castle.getCitySurfaceY(x, z);
                int effectiveY = Math.max(groundY + 1, roadY);

                boolean isCurb = !isAlley && (Math.abs(w) == outerW);
                boolean isCenterLane = isAlley ? (w == 0) : (Math.abs(w) <= Math.max(0, halfWidth - 1));

                // Retaining wall support under elevated road terraces & causeways
                fillRoadSupport(level, box, castle, x, z, groundY, effectiveY, isCurb);
                clearRoadHeadroom(level, box, castle, x, z, effectiveY, isAlley ? 3 : 4);

                BlockState road;
                if (!isCurb && stepUpForward) {
                    road = tileStairs(climbForward, Half.BOTTOM);
                } else if (!isCurb && stepUpBackward) {
                    road = tileStairs(climbBackward, Half.BOTTOM);
                } else if (isCenterLane) {
                    if (isAlley) {
                        road = castle.cityHash(x, salt, z, 3) == 0
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                    } else {
                        road = castle.cityHash(x, salt, z, 100) < 18
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.ABYSSAL_TILES.defaultBlockState();
                    }
                } else if (isCurb) {
                    road = (castle.cityHash(x, salt + 1, z, 4) == 0)
                            ? AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState()
                            : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                } else {
                    road = (castle.cityHash(x, salt + 1, z, 5) == 0)
                            ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                            : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
                }
                place(level, box, castle, x, effectiveY, z, road);

                // Retaining parapets on elevated road drops & weathered curb walls
                if (isCurb) {
                    boolean elevatedDrop = effectiveY > groundY + 1;
                    if (elevatedDrop && castle.cityHash(x, salt + 2, z, 10) != 0) {
                        place(level, box, castle, x, effectiveY + 1, z,
                                AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                    } else if (!elevatedDrop && halfWidth >= 2 && castle.cityHash(x, salt + 2, z, 8) == 0) {
                        place(level, box, castle, x, effectiveY + 1, z,
                                AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                    }

                    // Streetlamp brazier posts along major avenues every ~28 half-steps (14 blocks)
                    if (halfWidth >= 2 && w == outerW && step > 2 && step < steps - 2
                            && step % 28 == 0 && castle.cityHash(x, salt + 9, z, 100) < 65) {
                        place(level, box, castle, x, effectiveY + 1, z,
                                AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
                        place(level, box, castle, x, effectiveY + 2, z,
                                AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                        place(level, box, castle, x, effectiveY + 3, z,
                                AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
                    }
                }
            }

            // Overhead stone arches & hanging soul lanterns spanning narrow alleys every ~10 blocks
            if (isAlley && step > 3 && step < steps - 3 && step % 20 == 10
                    && castle.cityHash(cx, salt + 15, cz, 100) < 75) {
                buildAlleyArch(level, box, castle, cx, cz, nx, nz, roadY, salt + step);
            }
        }
    }

    /**
     * Computes a smoothed, terraced elevation profile along a road centerline so streets follow
     * the mountain relief cleanly and step by at most 1 block per horizontal block.
     */
    private static int[] computeRoadProfile(AbyssalCastlePiece castle, int x1, int z1, int x2, int z2,
                                            int steps, int elevationOffset) {
        int[] raw = new int[steps + 1];
        double dx = x2 - x1;
        double dz = z2 - z1;
        for (int s = 0; s <= steps; s++) {
            double t = s / (double) steps;
            int cx = (int) Math.round(x1 + dx * t);
            int cz = (int) Math.round(z1 + dz * t);
            raw[s] = castle.getCitySurfaceY(cx, cz) + 1 + elevationOffset;
        }

        // 5-point moving average to remove single-block terrain noise jitter
        int[] smooth = new int[steps + 1];
        for (int s = 0; s <= steps; s++) {
            int sum = 0;
            int count = 0;
            for (int k = -2; k <= 2; k++) {
                int idx = Math.max(0, Math.min(steps, s + k));
                sum += raw[idx];
                count++;
            }
            smooth[s] = (int) Math.round(sum / (double) count);
        }

        // Eliminate 1-step spikes/dips so staircases always have clean landings
        for (int pass = 0; pass < 2; pass++) {
            for (int s = 1; s < steps; s++) {
                if (smooth[s] > smooth[s - 1] && smooth[s] > smooth[s + 1]) {
                    smooth[s]--;
                } else if (smooth[s] < smooth[s - 1] && smooth[s] < smooth[s + 1]) {
                    smooth[s]++;
                }
            }
        }

        // Enforce max 1-block elevation change per 2 half-steps (1 full block) for walkable stairs
        for (int s = 2; s <= steps; s++) {
            if (smooth[s] > smooth[s - 2] + 1) smooth[s] = smooth[s - 2] + 1;
            if (smooth[s] < smooth[s - 2] - 1) smooth[s] = smooth[s - 2] - 1;
        }
        return smooth;
    }

    private static void buildAlleyArch(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                       int cx, int cz, double nx, double nz, int roadY, int salt) {
        for (int side : new int[]{-1, 1}) {
            int px = cx + (int) Math.round(nx * side);
            int pz = cz + (int) Math.round(nz * side);
            for (int y = 1; y <= 3; y++) {
                place(level, box, castle, px, roadY + y, pz,
                        y == 3 ? AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState()
                               : AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
            }
            place(level, box, castle, px, roadY + 4, pz, AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
        }
        place(level, box, castle, cx, roadY + 4, cz, AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
        if (castle.cityHash(cx, salt, cz, 100) < 40) {
            place(level, box, castle, cx, roadY + 3, cz,
                    Blocks.SOUL_LANTERN.defaultBlockState().setValue(BlockStateProperties.HANGING, true));
        }
    }

    private static void fillRoadSupport(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                        int dx, int dz, int groundY, int roadY, boolean isEdge) {
        for (int y = groundY + 1; y < roadY; y++) {
            BlockState support;
            if (isEdge) {
                support = (y == roadY - 1)
                        ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                        : AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
            } else {
                support = AbyssContent.ABYSSAL_STONE.defaultBlockState();
            }
            place(level, box, castle, dx, y, dz, support);
        }
    }

    private static void clearRoadHeadroom(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                          int dx, int dz, int roadY, int height) {
        for (int h = 1; h <= height; h++) {
            place(level, box, castle, dx, roadY + h, dz, Blocks.AIR.defaultBlockState());
        }
    }

    private static Direction vectorToDirection(double dx, double dz) {
        if (Math.abs(dx) >= Math.abs(dz)) {
            return dx >= 0 ? Direction.EAST : Direction.WEST;
        }
        return dz >= 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static BlockState tileStairs(Direction facing, Half half) {
        return AbyssContent.ABYSSAL_TILE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, facing)
                .setValue(BlockStateProperties.HALF, half);
    }

    private static void buildNeighborhoodPlaza(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                               int cx, int cz, int radius, boolean artisanWell, int salt) {
        if (!intersectsXZ(box, cx - radius - 2, cx + radius + 2, cz - radius - 2, cz + radius + 2, castle)) return;

        int baseY = highestSurface(castle, cx, cz, radius, radius);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radius * radius + 3) continue;
                int wx = cx + x;
                int wz = cz + z;
                int groundY = castle.getCitySurfaceY(wx, wz);
                fillSupport(level, box, castle, wx, wz, groundY, baseY, x, z);

                boolean cross = x == 0 || z == 0;
                BlockState floor = cross
                        ? (artisanWell ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                                       : AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState())
                        : (castle.cityHash(x, salt, z, 6) == 0
                            ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                            : AbyssContent.ABYSSAL_TILES.defaultBlockState());
                place(level, box, castle, wx, baseY, wz, floor);
            }
        }

        // Centerpiece: ruined stone well (Artisan Plaza) or broken gilded fountain/obelisk (Noble Plaza).
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                boolean rim = Math.abs(x) == 2 || Math.abs(z) == 2;
                if (rim) {
                    place(level, box, castle, cx + x, baseY + 1, cz + z,
                            AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
                }
            }
        }
        if (artisanWell) {
            for (int x : new int[]{-1, 1}) {
                for (int y = 1; y <= 3; y++) {
                    place(level, box, castle, cx + x, baseY + y, cz, AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                }
            }
            place(level, box, castle, cx, baseY + 3, cz, AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
        } else {
            for (int y = 1; y <= 5; y++) {
                place(level, box, castle, cx, baseY + y, cz,
                        y == 1 || y == 5 ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                                         : AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
            }
            place(level, box, castle, cx, baseY + 6, cz, AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        }
    }

    private static void buildMarketSquare(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle) {
        final int cx = 0;
        final int cz = 132;
        if (!intersectsXZ(box, -15, 15, 118, 146, castle)) return;

        int baseY = highestSurface(castle, cx, cz, 13, 12);
        for (int x = -13; x <= 13; x++) {
            for (int z = -11; z <= 11; z++) {
                int wx = cx + x;
                int wz = cz + z;
                int groundY = castle.getCitySurfaceY(wx, wz);
                fillSupport(level, box, castle, wx, wz, groundY, baseY, x, z);
                BlockState floor = (Math.abs(x) <= 1 || Math.abs(z) <= 1)
                        ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                        : (castle.cityHash(x, 501, z, 7) == 0
                            ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                            : AbyssContent.ABYSSAL_TILES.defaultBlockState());
                place(level, box, castle, wx, baseY, wz, floor);

                if ((Math.abs(x) == 13 || Math.abs(z) == 11)
                        && castle.cityHash(x, 502, z, 4) != 0) {
                    place(level, box, castle, wx, baseY + 1, wz, AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                }
            }
        }

        // Broken royal monument at the center of the former marketplace.
        for (int y = 1; y <= 7; y++) {
            BlockState core = y == 1 || y == 7
                    ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                    : AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
            place(level, box, castle, cx, baseY + y, cz, core);
            if (y <= 4) {
                place(level, box, castle, cx + 1, baseY + y, cz, AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
                place(level, box, castle, cx - 1, baseY + y, cz, AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
            }
        }
        place(level, box, castle, cx, baseY + 8, cz, AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());

        // Four abandoned market stalls, built as stone counters and collapsed awnings.
        for (int sideX : new int[]{-8, 8}) {
            for (int sideZ : new int[]{-6, 6}) {
                for (int x = -2; x <= 2; x++) {
                    place(level, box, castle, cx + sideX + x, baseY + 1, cz + sideZ,
                            AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                }
                for (int x : new int[]{-2, 2}) {
                    place(level, box, castle, cx + sideX + x, baseY + 2, cz + sideZ,
                            AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                    place(level, box, castle, cx + sideX + x, baseY + 3, cz + sideZ,
                            AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
                }
                if (castle.cityHash(sideX, 503, sideZ, 3) != 0) {
                    place(level, box, castle, cx + sideX, baseY + 3, cz + sideZ,
                            AbyssContent.ABYSSAL_TILE_SLAB.defaultBlockState());
                }
            }
        }
    }

    private static void buildHouse(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                   int cx, int cz, int halfWidth, int halfDepth, int wallHeight,
                                   int style, int facing, int id) {
        int margin = Math.max(halfWidth, halfDepth) + 3;
        int roofHeight = halfWidth + 3;
        int upperStory = (style == 1 || style == 4)
                ? 1
                : (style == 2 && castle.cityHash(cx, id, cz, 100) < 55 ? 1
                : (style == 0 && castle.cityHash(cx, id, cz, 100) < 25 ? 1 : 0));
        int extraStories = (style == 4 && castle.cityHash(cx, id + 5, cz, 100) < 50) ? 2 : upperStory;
        int totalWallHeight = wallHeight + extraStories * 3;
        if (!intersectsXZ(box, cx - margin, cx + margin, cz - margin, cz + margin, castle)) return;

        int baseY = highestSurface(castle, cx, cz, halfWidth + 1, halfDepth + 1);
        int topY = baseY + totalWallHeight + roofHeight + 4;
        if (box.maxY() < baseY - 20 || box.minY() > topY) return;

        boolean isLShaped = (style == 3 && halfWidth >= 4 && halfDepth >= 4);

        // 1. Terraced foundation & courtyard floor (in local space: +Z is front door, -Z is rear).
        for (int lx = -halfWidth - 1; lx <= halfWidth + 1; lx++) {
            for (int lz = -halfDepth - 1; lz <= halfDepth + 1; lz++) {
                boolean inCutout = isLShaped && lx > 0 && lz > 0;
                int wx = cx + rotX(lx, lz, facing);
                int wz = cz + rotZ(lx, lz, facing);
                int groundY = castle.getCitySurfaceY(wx, wz);
                fillSupport(level, box, castle, wx, wz, groundY, baseY, lx, lz);

                if (inCutout) {
                    // Courtyard paving and low ruined perimeter wall
                    place(level, box, castle, wx, baseY, wz,
                            (castle.cityHash(wx, id + 2, wz, 4) == 0)
                                    ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                    : AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    boolean courtBorder = (lx == halfWidth + 1 || lz == halfDepth + 1);
                    if (courtBorder && !(lx == halfWidth - 1 && lz == halfDepth + 1)
                            && castle.cityHash(wx, id + 3, wz, 5) != 0) {
                        place(level, box, castle, wx, baseY + 1, wz,
                                AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                    }
                } else {
                    boolean edge = Math.abs(lx) == halfWidth + 1 || Math.abs(lz) == halfDepth + 1;
                    BlockState foundation = edge
                            ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                            : AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
                    place(level, box, castle, wx, baseY, wz, foundation);
                }
            }
        }

        // 2. Interior floors & upper story slabs.
        for (int lx = -halfWidth; lx <= halfWidth; lx++) {
            for (int lz = -halfDepth; lz <= halfDepth; lz++) {
                if (isLShaped && lx > 0 && lz > 0) continue;
                int wx = cx + rotX(lx, lz, facing);
                int wz = cz + rotZ(lx, lz, facing);
                place(level, box, castle, wx, baseY + 1, wz,
                        (castle.cityHash(lx, id + 10, lz, 8) == 0)
                                ? AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState()
                                : AbyssContent.ABYSSAL_TILES.defaultBlockState());

                for (int story = 1; story <= extraStories; story++) {
                    int floorY = baseY + 1 + story * 3 + (wallHeight - 3);
                    if (story == 1) floorY = baseY + wallHeight + 1;
                    if (!isFootprintBoundary(lx, lz, halfWidth, halfDepth, isLShaped)) {
                        if (lx == 0 || lz == 0 || castle.cityHash(lx, id + 11 + story, lz, 9) < 5) {
                            place(level, box, castle, wx, floorY, wz,
                                    AbyssContent.ABYSSAL_BRICK_SLAB.defaultBlockState());
                        }
                    }
                }
            }
        }

        // 3. Exterior & L-wing walls with style-specific windows, shopfronts, and seeded ruin breaches.
        int ruin = castle.cityHash(cx, id + 20, cz, 100);
        int doorCenterX = isLShaped ? -halfWidth / 2 : 0;

        for (int y = 1; y <= totalWallHeight; y++) {
            for (int lx = -halfWidth; lx <= halfWidth; lx++) {
                for (int lz = -halfDepth; lz <= halfDepth; lz++) {
                    if (isLShaped && lx > 0 && lz > 0) continue;
                    if (!isFootprintBoundary(lx, lz, halfWidth, halfDepth, isLShaped)) continue;

                    int wx = cx + rotX(lx, lz, facing);
                    int wz = cz + rotZ(lx, lz, facing);

                    boolean corner = isFootprintCorner(lx, lz, halfWidth, halfDepth, isLShaped);
                    boolean doorway = (lz == halfDepth && Math.abs(lx - doorCenterX) <= 1 && y <= 2)
                            || (isLShaped && lx == 0 && lz == halfDepth / 2 && y <= 2);

                    // Merchant Shop (style 1) gets a wide open ground-floor market counter window
                    boolean shopWindow = (style == 1 && lz == halfDepth && Math.abs(lx) >= 2
                            && Math.abs(lx) <= halfWidth - 1 && y == 2);

                    boolean sideWindow = (y >= 2 && y <= 3)
                            && ((Math.abs(lx) == halfWidth && Math.abs(lz) <= 1)
                            || (Math.abs(lz) == halfDepth && Math.abs(lx) >= 2 && Math.abs(lx) <= 3))
                            && !(style == 4 && Math.abs(lx) + Math.abs(lz) > halfWidth + 1);

                    // Upper-story windows on 2+ story houses
                    boolean upperWindow = (extraStories >= 1 && y == wallHeight + 2)
                            && ((Math.abs(lz) == halfDepth && Math.abs(lx) <= 1)
                            || (Math.abs(lx) == halfWidth && lz == 0));

                    if (doorway || shopWindow || sideWindow || upperWindow) {
                        if (shopWindow) {
                            // Stone shop counter sill
                            place(level, box, castle, wx, baseY + 2, wz,
                                    AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                        }
                        continue;
                    }

                    // Seeded structural breaches on ruined buildings
                    if (ruin >= 66 && y >= totalWallHeight - 1 && !corner
                            && castle.cityHash(wx, y + id, wz, 100) < (ruin - 58)) continue;
                    if (ruin >= 84 && y >= 2 && castle.cityHash(wx, y + id + 30, wz, 100) < 9) continue;

                    BlockState wall;
                    boolean beltCourse = (y == 1) || (extraStories >= 1 && y == wallHeight + 1);
                    if (corner || beltCourse) {
                        wall = (style == 2 && beltCourse && !corner)
                                ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                                : AbyssContent.ABYSSAL_PILLAR.defaultBlockState();
                    } else {
                        wall = houseMasonry(castle, wx, y + id, wz, style);
                    }
                    place(level, box, castle, wx, baseY + 1 + y, wz, wall);
                }
            }
        }

        // Merchant shop cantilevered awning overhang above the ground floor
        if (style == 1) {
            int awningY = baseY + 4;
            for (int lx = -halfWidth; lx <= halfWidth; lx++) {
                int wx = cx + rotX(lx, halfDepth + 1, facing);
                int wz = cz + rotZ(lx, halfDepth + 1, facing);
                if (castle.cityHash(wx, id + 77, wz, 5) != 0) {
                    place(level, box, castle, wx, awningY, wz,
                            AbyssContent.ABYSSAL_TILE_SLAB.defaultBlockState());
                }
            }
        }

        // Noble Manor (style 2) entrance portico columns
        if (style == 2) {
            for (int side : new int[]{-2, 2}) {
                int wx = cx + rotX(side, halfDepth + 1, facing);
                int wz = cz + rotZ(side, halfDepth + 1, facing);
                for (int y = 1; y <= 3; y++) {
                    place(level, box, castle, wx, baseY + y, wz, AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                }
            }
            for (int lx = -2; lx <= 2; lx++) {
                int wx = cx + rotX(lx, halfDepth + 1, facing);
                int wz = cz + rotZ(lx, halfDepth + 1, facing);
                place(level, box, castle, wx, baseY + 4, wz, AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
            }
        }

        // 4. Roofs tailored to each architectural style.
        if (style == 4 || style == 6) {
            // Flat battlement / crenellated roof for Tower-Houses and Watchtowers
            int roofY = baseY + totalWallHeight + 1;
            for (int lx = -halfWidth; lx <= halfWidth; lx++) {
                for (int lz = -halfDepth; lz <= halfDepth; lz++) {
                    int wx = cx + rotX(lx, lz, facing);
                    int wz = cz + rotZ(lx, lz, facing);
                    place(level, box, castle, wx, roofY, wz,
                            AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState());
                    boolean edge = Math.abs(lx) == halfWidth || Math.abs(lz) == halfDepth;
                    if (edge && ((lx + lz) & 1) == 0
                            && !(ruin >= 70 && castle.cityHash(wx, roofY, wz, 100) < 35)) {
                        place(level, box, castle, wx, roofY + 1, wz,
                                AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState());
                    }
                }
            }
        } else if (style == 2) {
            // Tiered hipped roof for Noble Manors
            int maxStep = Math.min(halfWidth, halfDepth) + 1;
            for (int step = 0; step <= maxStep; step++) {
                int y = baseY + totalWallHeight + step + 1;
                int rx = halfWidth + 1 - step;
                int rz = halfDepth + 1 - step;
                for (int lx = -rx; lx <= rx; lx++) {
                    for (int lz = -rz; lz <= rz; lz++) {
                        boolean shell = Math.abs(lx) == rx || Math.abs(lz) == rz || step == maxStep;
                        if (!shell) continue;
                        int wx = cx + rotX(lx, lz, facing);
                        int wz = cz + rotZ(lx, lz, facing);
                        if (ruin >= 75 && step >= 2 && castle.cityHash(wx, y, wz, 100) < 20) continue;
                        place(level, box, castle, wx, y, wz,
                                step == maxStep ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                                                : AbyssContent.ABYSSAL_TILE_SLAB.defaultBlockState());
                    }
                }
            }
        } else {
            // Pitched gabled roof (with L-wing cutout support when style == 3)
            for (int step = 0; step <= halfWidth + 1; step++) {
                int y = baseY + totalWallHeight + step + 1;
                int westX = -halfWidth - 1 + step;
                int eastX = halfWidth + 1 - step;
                for (int lz = -halfDepth - 1; lz <= halfDepth + 1; lz++) {
                    boolean westGone = ruin >= 62 && step >= 2
                            && castle.cityHash(westX, step + id, lz, 100) < (ruin - 48) / 2;
                    boolean eastGone = ruin >= 78 && step >= 1
                            && castle.cityHash(eastX, step + id + 40, lz, 100) < (ruin - 68) / 2;

                    if (!westGone && !(isLShaped && westX > 0 && lz > 0)) {
                        int wx = cx + rotX(westX, lz, facing);
                        int wz = cz + rotZ(westX, lz, facing);
                        place(level, box, castle, wx, y, wz,
                                houseRoof(castle, westX, y - baseY, lz, id, facing));
                    }
                    if (eastX != westX && !eastGone && !(isLShaped && eastX > 0 && lz > 0)) {
                        int wx = cx + rotX(eastX, lz, facing);
                        int wz = cz + rotZ(eastX, lz, facing);
                        place(level, box, castle, wx, y, wz,
                                houseRoof(castle, eastX, y - baseY, lz, id + 1, facing));
                    }
                }
            }

            // Gable end masonry
            for (int step = 1; step <= halfWidth; step++) {
                int y = baseY + totalWallHeight + step;
                for (int lx = -halfWidth + step; lx <= halfWidth - step; lx++) {
                    for (int lz : new int[]{-halfDepth, halfDepth}) {
                        if (isLShaped && lx > 0 && lz > 0) continue;
                        if (ruin >= 85 && castle.cityHash(lx, y, lz + id, 100) < 18) continue;
                        int wx = cx + rotX(lx, lz, facing);
                        int wz = cz + rotZ(lx, lz, facing);
                        place(level, box, castle, wx, y, wz,
                                houseMasonry(castle, wx, y, wz, style));
                    }
                }
            }
        }

        // 5. Interior furnishings, hearths, chimneys, and landmark features.
        int hearthX = cx + rotX(-halfWidth + 1, -halfDepth + 1, facing);
        int hearthZ = cz + rotZ(-halfWidth + 1, -halfDepth + 1, facing);
        place(level, box, castle, hearthX, baseY + 2, hearthZ,
                AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
        place(level, box, castle, hearthX, baseY + 3, hearthZ,
                AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());

        // Stone chimney rising through the rear corner on cottages & manors
        if (style == 0 || style == 2 || style == 3) {
            for (int y = 2; y <= totalWallHeight + halfWidth + 2; y++) {
                place(level, box, castle, hearthX, baseY + y, hearthZ,
                        y == totalWallHeight + halfWidth + 2
                                ? AbyssContent.ABYSSAL_BRICK_WALL.defaultBlockState()
                                : AbyssContent.ABYSSAL_BRICKS.defaultBlockState());
            }
        }

        // Courtyard well/brazier on L-shaped houses
        if (isLShaped) {
            int wellX = cx + rotX(halfWidth - 2, halfDepth - 2, facing);
            int wellZ = cz + rotZ(halfWidth - 2, halfDepth - 2, facing);
            place(level, box, castle, wellX, baseY + 1, wellZ,
                    AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState());
            place(level, box, castle, wellX, baseY + 2, wellZ,
                    AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        }

        // Sanctuary / Cathedral interior nave & gilded altar (style 5)
        if (style == 5) {
            for (int lz = -halfDepth + 3; lz <= halfDepth - 2; lz += 4) {
                for (int side : new int[]{-5, 5}) {
                    int px = cx + rotX(side, lz, facing);
                    int pz = cz + rotZ(side, lz, facing);
                    for (int y = 2; y <= Math.min(totalWallHeight, 6); y++) {
                        place(level, box, castle, px, baseY + y, pz,
                                AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
                    }
                }
            }
            int altarX = cx + rotX(0, -halfDepth + 2, facing);
            int altarZ = cz + rotZ(0, -halfDepth + 2, facing);
            place(level, box, castle, altarX, baseY + 2, altarZ,
                    AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState());
            place(level, box, castle, altarX, baseY + 3, altarZ,
                    AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        }

        // Watchtower / Gatehouse signal spire (style 6)
        if (style == 6) {
            for (int y = totalWallHeight + 2; y <= totalWallHeight + 5; y++) {
                place(level, box, castle, cx, baseY + y, cz,
                        AbyssContent.ABYSSAL_PILLAR.defaultBlockState());
            }
            place(level, box, castle, cx, baseY + totalWallHeight + 6, cz,
                    AbyssContent.ABYSSAL_BRAZIER.defaultBlockState());
        }

        // 6. Interior life: charred-timber fittings, empty black shelves, black beds and stone storage.
        buildHouseInterior(level, box, castle, cx, cz, halfWidth, halfDepth, baseY,
                style, facing, id, totalWallHeight);

        // 7. Compact rubble fan outside the main entrance (roads generated afterwards stay clear).
        if (ruin >= 48) {
            for (int lx = -2; lx <= 2; lx++) {
                for (int lz = 1; lz <= 2; lz++) {
                    int wx = cx + rotX(doorCenterX + lx, halfDepth + lz, facing);
                    int wz = cz + rotZ(doorCenterX + lx, halfDepth + lz, facing);
                    if (castle.cityHash(wx, id + 60, wz, 100) > Math.min(78, ruin)) continue;
                    int groundY = castle.getCitySurfaceY(wx, wz);
                    int pile = 1 + castle.cityHash(wx, id + 61, wz, 2);
                    for (int h = 0; h < pile; h++) {
                        place(level, box, castle, wx, groundY + 1 + h, wz,
                                h == pile - 1 ? AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState()
                                              : AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState());
                    }
                }
            }
        }
    }

    private static void buildHouseInterior(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                            int cx, int cz, int halfWidth, int halfDepth, int baseY,
                                            int style, int facing, int id, int wallHeight) {
        // Every room gets a dark-stone work surface; the block silhouette stays low enough
        // to preserve circulation from the front door into the house.
        int tableLX = 0;
        int tableLZ = 0;
        int tableX = cx + rotX(tableLX, tableLZ, facing);
        int tableZ = cz + rotZ(tableLX, tableLZ, facing);
        BlockState workSurface = style == 2
                ? AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState()
                : (style == 1 || style == 5
                    ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                    : AbyssContent.ABYSSAL_CHARRED_TIMBER.defaultBlockState());
        place(level, box, castle, tableX, baseY + 2, tableZ, workSurface);

        // Empty, black shelves: the city archives survived as stone furniture, but their books did not.
        // Keep the rear-right corner free for the storage chest and the rear-left corner for the hearth.
        if (style == 0 || style == 1 || style == 2 || style == 4 || style == 5) {
            int shelfLZ = -halfDepth + 1;
            int[] shelfColumns = halfWidth >= 4 ? new int[]{-1, 0} : new int[]{0};
            for (int shelfLX : shelfColumns) {
                int shelfX = cx + rotX(shelfLX, shelfLZ, facing);
                int shelfZ = cz + rotZ(shelfLX, shelfLZ, facing);
                BlockState shelf = AbyssContent.ABYSSAL_BLACK_BOOKSHELF.defaultBlockState()
                        .setValue(AbyssalBookshelfBlock.FACING, directionForLocalFacing(facing));
                place(level, box, castle, shelfX, baseY + 2, shelfZ, shelf);
                if (wallHeight >= 5) {
                    place(level, box, castle, shelfX, baseY + 3, shelfZ, shelf);
                }
            }
        }

        // Residential rooms retain a real sleeping place, but no red wool or clean timber.
        if (style == 0 || style == 2 || style == 3 || style == 4) {
            Direction bedFacing = directionForLocalFacing(facing);
            int bedLX = -halfWidth + 1;
            int bedLZ = halfDepth - 2;
            int bedX = cx + rotX(bedLX, bedLZ, facing);
            int bedZ = cz + rotZ(bedLX, bedLZ, facing);
            BlockPos footPos = new BlockPos(bedX, baseY + 2, bedZ);
            BlockPos headPos = footPos.relative(bedFacing);
            if (box.isInside(footPos) && box.isInside(headPos)) {
                BlockState bed = BuiltInRegistries.BLOCK.getValue(
                        Identifier.fromNamespaceAndPath("minecraft", "black_bed")).defaultBlockState()
                        .setValue(net.minecraft.world.level.block.BedBlock.FACING, bedFacing)
                        .setValue(net.minecraft.world.level.block.BedBlock.OCCUPIED, false);
                place(level, box, castle, bedX, baseY + 2, bedZ,
                        bed.setValue(net.minecraft.world.level.block.BedBlock.PART,
                                net.minecraft.world.level.block.state.properties.BedPart.FOOT));
                place(level, box, castle, headPos.getX(), baseY + 2,
                        headPos.getZ(),
                        bed.setValue(net.minecraft.world.level.block.BedBlock.PART,
                                net.minecraft.world.level.block.state.properties.BedPart.HEAD));
            }
        }

        // Charred beams and blackstone benches replace ordinary oak/dark-oak furniture.
        if (halfWidth >= 4 && halfDepth >= 3) {
            int benchLX = -halfWidth + 1;
            int benchLZ = 0;
            int benchX = cx + rotX(benchLX, benchLZ, facing);
            int benchZ = cz + rotZ(benchLX, benchLZ, facing);
            place(level, box, castle, benchX, baseY + 2, benchZ,
                    AbyssContent.ABYSSAL_TILE_STAIRS.defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, directionForLocalFacing(facing))
                            .setValue(BlockStateProperties.HALF, Half.BOTTOM));
        }

        // Merchant shops and noble homes always receive storage. Other houses receive it by seed,
        // creating a believable mix of private strongboxes rather than putting one in every doorway.
        boolean hasStorage = style == 1 || style == 2 || style == 6
                || castle.cityHash(cx, id + 91, cz, 100) < 42;
        if (hasStorage) {
            int chestLX = halfWidth - 1;
            int chestLZ = -halfDepth + 1;
            int chestX = cx + rotX(chestLX, chestLZ, facing);
            int chestZ = cz + rotZ(chestLX, chestLZ, facing);
            BlockPos chestPos = new BlockPos(castle.getCityWorldX(chestX), baseY + 2,
                    castle.getCityWorldZ(chestZ));
            if (box.isInside(chestPos)) {
                level.setBlock(chestPos, AbyssContent.ABYSSAL_CHEST.defaultBlockState()
                        .setValue(AbyssalChestBlock.FACING, directionForLocalFacing(facing)), 2);
                BlockEntity be = level.getBlockEntity(chestPos);
                if (be instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {
                    String lootPath = (style == 2 || style == 5) ? "chests/abyssal_noble"
                            : (style == 1 ? "chests/abyssal_merchant" : "chests/abyssal_house");
                    long chestSeed = ((long) cx * 341873128712L) ^ ((long) cz * 42317861L)
                            ^ ((long) id * 132897987541L) ^ baseY;
                    ResourceKey<net.minecraft.world.level.storage.loot.LootTable> loot =
                            ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                                    net.minecraft.resources.Identifier.fromNamespaceAndPath("simpleskills", lootPath));
                    container.setLootTable(loot, chestSeed);
                }
            }
        }
    }

    private static Direction directionForLocalFacing(int facing) {
        return switch (facing) {
            case FACING_NORTH -> Direction.NORTH;
            case FACING_WEST -> Direction.WEST;
            case FACING_EAST -> Direction.EAST;
            default -> Direction.SOUTH;
        };
    }

    private static boolean isFootprintBoundary(int lx, int lz, int halfWidth, int halfDepth, boolean isLShaped) {
        if (Math.abs(lx) == halfWidth || Math.abs(lz) == halfDepth) return true;
        return isLShaped && ((lx == 0 && lz >= 0) || (lz == 0 && lx >= 0));
    }

    private static boolean isFootprintCorner(int lx, int lz, int halfWidth, int halfDepth, boolean isLShaped) {
        if (Math.abs(lx) == halfWidth && Math.abs(lz) == halfDepth) return true;
        return isLShaped && ((lx == 0 && lz == 0)
                || (lx == 0 && lz == halfDepth)
                || (lx == halfWidth && lz == 0));
    }

    private static int rotX(int lx, int lz, int facing) {
        return switch (facing) {
            case FACING_WEST -> -lz;
            case FACING_NORTH -> -lx;
            case FACING_EAST -> lz;
            default -> lx; // FACING_SOUTH
        };
    }

    private static int rotZ(int lx, int lz, int facing) {
        return switch (facing) {
            case FACING_WEST -> lx;
            case FACING_NORTH -> -lz;
            case FACING_EAST -> -lx;
            default -> lz; // FACING_SOUTH
        };
    }

    private static Direction rotSlopeDirection(boolean localEastSlope, int facing) {
        return switch (facing) {
            case FACING_WEST -> localEastSlope ? Direction.SOUTH : Direction.NORTH;
            case FACING_NORTH -> localEastSlope ? Direction.WEST : Direction.EAST;
            case FACING_EAST -> localEastSlope ? Direction.NORTH : Direction.SOUTH;
            default -> localEastSlope ? Direction.EAST : Direction.WEST;
        };
    }

    private static BlockState houseMasonry(AbyssalCastlePiece castle, int x, int y, int z, int style) {
        int roll = castle.cityHash(x, y + style * 17, z, 100);
        if (style == 2 && roll < 14) return AbyssContent.GILDED_ABYSSAL_BRICKS.defaultBlockState();
        if (roll < 14) return AbyssContent.CHISELED_ABYSSAL_STONE.defaultBlockState();
        if (roll < 35) return AbyssContent.CRACKED_ABYSSAL_BRICKS.defaultBlockState();
        if (roll < 48) return AbyssContent.CRACKED_ABYSSAL_STONE.defaultBlockState();
        if (roll < 57) return AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState();
        return AbyssContent.ABYSSAL_BRICKS.defaultBlockState();
    }

    private static BlockState houseRoof(AbyssalCastlePiece castle, int localX, int y, int localZ,
                                        int salt, int facing) {
        int roll = castle.cityHash(localX, y + salt, localZ, 100);
        if (roll < 14) return AbyssContent.CRACKED_ABYSSAL_TILES.defaultBlockState();
        if (roll < 30) return AbyssContent.ABYSSAL_TILE_SLAB.defaultBlockState();
        Direction stairFacing = rotSlopeDirection(localX < 0, facing);
        return AbyssContent.ABYSSAL_TILE_STAIRS.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, stairFacing)
                .setValue(BlockStateProperties.HALF, Half.BOTTOM);
    }

    private static int highestSurface(AbyssalCastlePiece castle, int cx, int cz, int rx, int rz) {
        int highest = Integer.MIN_VALUE;
        for (int x = -rx; x <= rx; x++) {
            for (int z = -rz; z <= rz; z++) {
                highest = Math.max(highest, castle.getCitySurfaceY(cx + x, cz + z) + 1);
            }
        }
        return highest == Integer.MIN_VALUE ? castle.getCitySurfaceY(cx, cz) + 1 : highest;
    }

    private static void fillSupport(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                                    int dx, int dz, int groundY, int baseY, int localX, int localZ) {
        for (int y = groundY + 1; y < baseY; y++) {
            BlockState support = (y == baseY - 1 && (Math.abs(localX) > 3 || Math.abs(localZ) > 3))
                    ? AbyssContent.POLISHED_ABYSSAL_STONE.defaultBlockState()
                    : AbyssContent.ABYSSAL_STONE.defaultBlockState();
            place(level, box, castle, dx, y, dz, support);
        }
    }

    private static boolean intersectsXZ(BoundingBox box, int minDx, int maxDx, int minDz, int maxDz,
                                        AbyssalCastlePiece castle) {
        int worldMinX = castle.getCityWorldX(minDx);
        int worldMaxX = castle.getCityWorldX(maxDx);
        int worldMinZ = castle.getCityWorldZ(minDz);
        int worldMaxZ = castle.getCityWorldZ(maxDz);
        return box.maxX() >= worldMinX && box.minX() <= worldMaxX
                && box.maxZ() >= worldMinZ && box.minZ() <= worldMaxZ;
    }

    private static void place(WorldGenLevel level, BoundingBox box, AbyssalCastlePiece castle,
                              int dx, int y, int dz, BlockState state) {
        BlockPos pos = new BlockPos(castle.getCityWorldX(dx), y, castle.getCityWorldZ(dz));
        if (box.isInside(pos) && y >= level.getMinY() + 1) {
            level.setBlock(pos, state, 2);
        }
    }
}