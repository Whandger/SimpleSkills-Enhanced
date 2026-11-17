package com.github.ob_yekt.simpleskills.fishing;

/** Registers all custom catchable species after their EntityTypes and Items exist. */
public final class FishingSpeciesRegistration {
    private static boolean registered;
    private FishingSpeciesRegistration() {}

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        FishSpecies.register(FishSpecies.builder("trout", FishingEntities.TROUT, () -> FishingItems.RAW_TROUT)
                .fight(1.05, 1.00).xp(1.05).minLevel(5)
                .size(new FishSizeModel.Profile(18, 55, 8.0e-6, 3.0))
                .spawn(FishSpawnProfile.of(2, 4, 12, false,
                        FishHabitat.RIVER, 1.0, FishHabitat.COLD_FRESHWATER, 0.8).withSchoolSize(3, 5))
                .ambientSpawn(true)
                .mouth(0.56, 0.48));

        FishSpecies.register(FishSpecies.builder("bass", FishingEntities.BASS, () -> FishingItems.RAW_BASS)
                .fight(1.20, 0.90).xp(1.15).minLevel(10)
                .size(new FishSizeModel.Profile(20, 55, 1.8e-5, 3.0))
                .spawn(FishSpawnProfile.of(2, 4, 10, false,
                        FishHabitat.RIVER, 0.8, FishHabitat.WARM_FRESHWATER, 1.0,
                        FishHabitat.TEMPERATE_FRESHWATER, 1.0).withSchoolSize(2, 4))
                .ambientSpawn(true)
                .mouth(0.52, 0.50));

        FishSpecies.register(FishSpecies.builder("catfish", FishingEntities.CATFISH, () -> FishingItems.RAW_CATFISH)
                .fight(1.35, 0.75).xp(1.35).minLevel(20)
                .size(new FishSizeModel.Profile(30, 110, 1.0e-5, 3.0))
                .spawn(FishSpawnProfile.of(4, 8, 5, true,
                        FishHabitat.RIVER, 0.65, FishHabitat.WARM_FRESHWATER, 1.0,
                        FishHabitat.TEMPERATE_FRESHWATER, 0.8).withSchoolSize(1, 2))
                .ambientSpawn(true)
                .mouth(0.50, 0.50));

        FishSpecies.register(FishSpecies.builder("pike", FishingEntities.PIKE, () -> FishingItems.RAW_PIKE)
                .fight(1.55, 0.82).xp(1.45).minLevel(30)
                .size(new FishSizeModel.Profile(35, 120, 8.0e-6, 3.0))
                .spawn(FishSpawnProfile.of(3, 7, 4, true,
                        FishHabitat.RIVER, 0.45, FishHabitat.COLD_FRESHWATER, 1.0,
                        FishHabitat.TEMPERATE_FRESHWATER, 0.4).withSchoolSize(1, 2))
                .ambientSpawn(true)
                .mouth(0.53, 0.46));

        FishSpecies.register(FishSpecies.builder("carp", FishingEntities.CARP, () -> FishingItems.RAW_CARP)
                .fight(1.15, 0.88).xp(1.10).minLevel(10)
                .size(new FishSizeModel.Profile(20, 90, 1.2e-5, 3.0))
                .spawn(FishSpawnProfile.of(2, 5, 10, false,
                        FishHabitat.RIVER, 0.7, FishHabitat.WARM_FRESHWATER, 0.9,
                        FishHabitat.TEMPERATE_FRESHWATER, 1.0).withSchoolSize(3, 5))
                .ambientSpawn(true)
                .mouth(0.50, 0.52));

        FishSpecies.register(FishSpecies.builder("tuna", FishingEntities.TUNA, () -> FishingItems.RAW_TUNA)
                .fight(1.65, 0.72).xp(1.8).minLevel(35)
                .size(new FishSizeModel.Profile(50, 250, 8.0e-6, 3.0))
                .spawn(FishSpawnProfile.of(8, 14, 3, true,
                        FishHabitat.WARM_OCEAN, 0.75, FishHabitat.TEMPERATE_OCEAN, 1.0).withSchoolSize(2, 3))
                .ambientSpawn(true)
                .mouth(0.53, 0.48));

        FishSpecies.register(FishSpecies.builder("swordfish", FishingEntities.SWORDFISH, () -> FishingItems.RAW_SWORDFISH)
                .fight(1.95, 0.68).xp(2.1).minLevel(55)
                .size(new FishSizeModel.Profile(90, 300, 6.0e-6, 3.0))
                .spawn(FishSpawnProfile.of(10, 18, 2, true,
                        FishHabitat.TEMPERATE_OCEAN, 0.9, FishHabitat.COLD_OCEAN, 0.8).withSchoolSize(1, 1))
                .ambientSpawn(true)
                .mouth(0.55, 0.48));

        FishSpecies.register(FishSpecies.builder("mahi_mahi", FishingEntities.MAHI_MAHI, () -> FishingItems.RAW_MAHI_MAHI)
                .fight(1.35, 0.84).xp(1.55).minLevel(30)
                .size(new FishSizeModel.Profile(30, 120, 1.2e-5, 3.0))
                .spawn(FishSpawnProfile.of(5, 9, 4, true, FishHabitat.WARM_OCEAN, 1.0).withSchoolSize(2, 4))
                .ambientSpawn(true)
                .mouth(0.52, 0.48));

        FishSpecies.register(FishSpecies.builder("reef_shark", FishingEntities.REEF_SHARK, () -> FishingItems.RAW_REEF_SHARK)
                .fight(2.0, 0.62).xp(2.3).minLevel(60)
                .size(new FishSizeModel.Profile(100, 220, 2.0e-5, 3.0))
                .spawn(FishSpawnProfile.of(8, 15, 2, true, FishHabitat.WARM_OCEAN, 0.65).withSchoolSize(1, 2))
                .ambientSpawn(true)
                .mouth(0.48, 0.47));

        FishSpecies.register(FishSpecies.builder("great_white_shark", FishingEntities.GREAT_WHITE_SHARK, () -> FishingItems.RAW_GREAT_WHITE_SHARK)
                .fight(2.65, 0.55).xp(2.8).minLevel(80)
                .size(new FishSizeModel.Profile(200, 550, 1.0e-5, 3.0))
                .spawn(FishSpawnProfile.of(12, 22, 1, true,
                        FishHabitat.TEMPERATE_OCEAN, 0.55, FishHabitat.COLD_OCEAN, 0.7).withSchoolSize(1, 1))
                .ambientSpawn(true)
                .mouth(0.46, 0.45));

        FishSpecies.register(FishSpecies.builder("whale_shark", FishingEntities.WHALE_SHARK, () -> FishingItems.RAW_WHALE_SHARK)
                .fight(2.8, 0.50).xp(3.0).minLevel(90)
                .size(new FishSizeModel.Profile(500, 1200, 1.0e-5, 3.0))
                .spawn(FishSpawnProfile.of(16, 26, 1, true, FishHabitat.WARM_OCEAN, 0.18).withSchoolSize(1, 1))
                .ambientSpawn(true)
                .mouth(0.45, 0.48));
    }
}
