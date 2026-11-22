package com.github.ob_yekt.simpleskills.shields;

import com.github.ob_yekt.simpleskills.Simpleskills;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;

/**
 * Camadas de modelo 3D dos Escudos Reais de Glowstone:
 * - GLOWSTONE_SHIELD: estrutura metalica, coroa real, nervuras de ouro/lapis-lazuli e engastes.
 * - GLOWSTONE_SHIELD_CRYSTALS: pedras de glowstone extrudadas em 3D (renderizadas com brilho proprio).
 */
public final class GlowstoneShieldLayers {
    public static final ModelLayerLocation GLOWSTONE_SHIELD = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "glowstone_shield"),
            "main"
    );

    public static final ModelLayerLocation GLOWSTONE_SHIELD_CRYSTALS = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "glowstone_shield_crystals"),
            "main"
    );

    private GlowstoneShieldLayers() {
    }

    /** Corpo metalico, moldura em relevo, coroa real e filigranas de ouro / lapis-lazuli (64x64). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Placa base e empunhadura padrao (mantem compatibilidade total com BannerRenderer)
        root.addOrReplaceChild(
                "plate",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-6.0F, -11.0F, -2.0F, 12.0F, 22.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "handle",
                CubeListBuilder.create()
                        .texOffs(26, 0)
                        .addBox(-1.0F, -3.0F, -1.0F, 2.0F, 6.0F, 6.0F),
                PartPose.ZERO
        );

        // Moldura metalica superior e inferior em 3D
        root.addOrReplaceChild(
                "royal_rim_top",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-6.0F, -11.0F, -2.6F, 12.0F, 2.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "royal_rim_bottom",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-6.0F, 9.0F, -2.6F, 12.0F, 2.0F, 1.0F),
                PartPose.ZERO
        );

        // Trilhos laterais blindados em 3D
        root.addOrReplaceChild(
                "royal_rim_left",
                CubeListBuilder.create()
                        .texOffs(0, 28)
                        .addBox(-6.0F, -9.0F, -2.6F, 1.0F, 18.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "royal_rim_right",
                CubeListBuilder.create()
                        .texOffs(0, 28)
                        .addBox(5.0F, -9.0F, -2.6F, 1.0F, 18.0F, 1.0F),
                PartPose.ZERO
        );

        // Coroa real no topo e quilha inferior
        root.addOrReplaceChild(
                "royal_crown_center",
                CubeListBuilder.create()
                        .texOffs(6, 28)
                        .addBox(-2.0F, -13.0F, -2.7F, 4.0F, 2.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "royal_crown_left",
                CubeListBuilder.create()
                        .texOffs(6, 32)
                        .addBox(-5.0F, -12.0F, -2.6F, 2.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "royal_crown_right",
                CubeListBuilder.create()
                        .texOffs(6, 32)
                        .addBox(3.0F, -12.0F, -2.6F, 2.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "royal_keel",
                CubeListBuilder.create()
                        .texOffs(6, 35)
                        .addBox(-2.0F, 11.0F, -2.6F, 4.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        // Linhas / nervuras reais em relevo (ouro no ferro/netherite, lapis-lazuli no diamante)
        root.addOrReplaceChild(
                "filigree_top",
                CubeListBuilder.create()
                        .texOffs(18, 28)
                        .addBox(-1.0F, -9.0F, -2.45F, 2.0F, 6.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "filigree_bottom",
                CubeListBuilder.create()
                        .texOffs(18, 28)
                        .addBox(-1.0F, 3.0F, -2.45F, 2.0F, 6.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "filigree_left",
                CubeListBuilder.create()
                        .texOffs(18, 36)
                        .addBox(-5.0F, -1.0F, -2.45F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "filigree_right",
                CubeListBuilder.create()
                        .texOffs(18, 36)
                        .addBox(3.0F, -1.0F, -2.45F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO
        );

        // Engaste central e 4 engastes de canto para as pedras de glowstone
        root.addOrReplaceChild(
                "center_bezel",
                CubeListBuilder.create()
                        .texOffs(42, 0)
                        .addBox(-3.0F, -3.0F, -2.75F, 6.0F, 6.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "corner_tl",
                CubeListBuilder.create()
                        .texOffs(42, 14)
                        .addBox(-5.0F, -9.0F, -2.75F, 3.0F, 3.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "corner_tr",
                CubeListBuilder.create()
                        .texOffs(42, 14)
                        .addBox(2.0F, -9.0F, -2.75F, 3.0F, 3.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "corner_bl",
                CubeListBuilder.create()
                        .texOffs(42, 14)
                        .addBox(-5.0F, 6.0F, -2.75F, 3.0F, 3.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "corner_br",
                CubeListBuilder.create()
                        .texOffs(42, 14)
                        .addBox(2.0F, 6.0F, -2.75F, 3.0F, 3.0F, 1.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    /** Pedras e cristais 3D de glowstone (renderizados em fullbright no GlowstoneShieldSpecialRenderer). */
    public static LayerDefinition createCrystalLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Partes vazias exigidas pelo construtor de ShieldModel
        root.addOrReplaceChild("plate", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("handle", CubeListBuilder.create(), PartPose.ZERO);

        // Nucleo central escalonado em 2 niveis 3D
        root.addOrReplaceChild(
                "center_glowstone_base",
                CubeListBuilder.create()
                        .texOffs(42, 8)
                        .addBox(-2.0F, -2.0F, -3.35F, 4.0F, 4.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "center_glowstone_apex",
                CubeListBuilder.create()
                        .texOffs(53, 8)
                        .addBox(-1.0F, -1.0F, -3.85F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO
        );

        // Gema da coroa e da quilha inferior
        root.addOrReplaceChild(
                "crown_glowstone_gem",
                CubeListBuilder.create()
                        .texOffs(51, 17)
                        .addBox(-1.0F, -12.0F, -3.15F, 2.0F, 2.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "keel_glowstone_gem",
                CubeListBuilder.create()
                        .texOffs(57, 14)
                        .addBox(-1.0F, 10.0F, -3.15F, 2.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        // 4 pedras de glowstone incrustadas nos cantos
        root.addOrReplaceChild(
                "corner_gem_tl",
                CubeListBuilder.create()
                        .texOffs(51, 14)
                        .addBox(-4.0F, -8.0F, -3.15F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "corner_gem_tr",
                CubeListBuilder.create()
                        .texOffs(51, 14)
                        .addBox(3.0F, -8.0F, -3.15F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "corner_gem_bl",
                CubeListBuilder.create()
                        .texOffs(51, 14)
                        .addBox(-4.0F, 7.0F, -3.15F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "corner_gem_br",
                CubeListBuilder.create()
                        .texOffs(51, 14)
                        .addBox(3.0F, 7.0F, -3.15F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 64);
    }
}
