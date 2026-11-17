package com.github.ob_yekt.simpleskills.fishing;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** Registration of custom fish and ambient marine mammals. */
public final class FishingEntities {
    public static final EntityType<FishingFishEntity> TROUT = fish("trout", 0.70F, 0.50F);
    public static final EntityType<FishingFishEntity> BASS = fish("bass", 0.85F, 0.58F);
    public static final EntityType<FishingFishEntity> CATFISH = fish("catfish", 0.95F, 0.62F);
    public static final EntityType<FishingFishEntity> PIKE = fish("pike", 1.00F, 0.56F);
    public static final EntityType<FishingFishEntity> CARP = fish("carp", 0.90F, 0.60F);
    public static final EntityType<FishingFishEntity> TUNA = fish("tuna", 1.50F, 0.95F);
    public static final EntityType<FishingFishEntity> SWORDFISH = fish("swordfish", 1.80F, 0.90F);
    public static final EntityType<FishingFishEntity> MAHI_MAHI = fish("mahi_mahi", 1.40F, 0.82F);
    public static final EntityType<FishingFishEntity> REEF_SHARK = fish("reef_shark", 2.10F, 1.25F);
    public static final EntityType<FishingFishEntity> GREAT_WHITE_SHARK = fish("great_white_shark", 2.80F, 1.55F);
    public static final EntityType<FishingFishEntity> WHALE_SHARK = fish("whale_shark", 4.00F, 2.20F);

    public static final EntityType<MarineMammalEntity> HUMPBACK_WHALE = mammal("humpback_whale", 6.8F, 3.2F);
    public static final EntityType<MarineMammalEntity> ORCA = mammal("orca", 3.2F, 1.8F);

    private FishingEntities() {}

    public static void register() {
        // Static initialization above performs the registrations before ConfigManager reads FishSpecies.
    }

    private static EntityType<FishingFishEntity> fish(String path, float width, float height) {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path);
        EntityType<FishingFishEntity> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
                EntityType.Builder.<FishingFishEntity>of(FishingFishEntity::new, MobCategory.WATER_CREATURE)
                        .sized(width, height).clientTrackingRange(10).updateInterval(3)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
        FabricDefaultAttributeRegistry.register(type, FishingFishEntity.createAttributes());
        return type;
    }

    private static EntityType<MarineMammalEntity> mammal(String path, float width, float height) {
        Identifier id = Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, path);
        EntityType<MarineMammalEntity> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, id,
                EntityType.Builder.<MarineMammalEntity>of(MarineMammalEntity::new, MobCategory.WATER_CREATURE)
                        .sized(width, height).clientTrackingRange(12).updateInterval(3)
                        .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
        FabricDefaultAttributeRegistry.register(type, MarineMammalEntity.createAttributes());
        return type;
    }
}
