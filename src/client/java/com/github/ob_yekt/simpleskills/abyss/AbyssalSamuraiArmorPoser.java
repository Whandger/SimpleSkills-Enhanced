package com.github.ob_yekt.simpleskills.abyss;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/**
 * Calcula a pose da armadura do Samurai para cada jogador.
 *
 * Usa um PlayerModel interno posado com o próprio estado de renderização. Como o
 * KatanaPlayerModelMixin já altera PlayerModel.setupAnim, a pose da katana entra aqui
 * sem nenhuma mudança nos arquivos da katana.
 *
 * Cada jogador tem as suas próprias instâncias das peças. Se fossem compartilhadas,
 * o render adiado desenharia todos os jogadores com a última pose copiada.
 */
public final class AbyssalSamuraiArmorPoser {
    private static final int MAX_CACHED_ENTITIES = 256;

    private static PlayerModel poser;
    private static final Map<Integer, EnumMap<AbyssalSamuraiArmorModel.Piece, AbyssalSamuraiArmorModel>> PER_ENTITY =
            new HashMap<>();
    private static final EnumMap<AbyssalSamuraiArmorModel.Piece, AbyssalSamuraiArmorModel> SHARED =
            new EnumMap<>(AbyssalSamuraiArmorModel.Piece.class);

    private AbyssalSamuraiArmorPoser() {
    }

    /** Peça posada para este jogador neste frame, ou null se ainda não estiver pronta. */
    public static AbyssalSamuraiArmorModel forPlayer(AbyssalSamuraiArmorModel.Piece piece, AvatarRenderState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getEntityModels() == null) return null;

        PlayerModel posed = poser(minecraft);
        resetPose(posed);
        posed.setupAnim(state);

        if (!PER_ENTITY.containsKey(state.id) && PER_ENTITY.size() >= MAX_CACHED_ENTITIES) {
            PER_ENTITY.clear();
        }
        EnumMap<AbyssalSamuraiArmorModel.Piece, AbyssalSamuraiArmorModel> pieces =
                PER_ENTITY.computeIfAbsent(state.id, id -> new EnumMap<>(AbyssalSamuraiArmorModel.Piece.class));
        AbyssalSamuraiArmorModel model = pieces.computeIfAbsent(piece, p -> bake(minecraft, p));
        if (model == null) return null;

        model.copyPoseFrom(posed);
        return model;
    }

    /** Para mobs com a armadura: copia a pose do HumanoidModel que o próprio mob já posou. */
    public static AbyssalSamuraiArmorModel forHumanoid(AbyssalSamuraiArmorModel.Piece piece, HumanoidModel<?> humanoid) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getEntityModels() == null) return null;

        AbyssalSamuraiArmorModel model = SHARED.computeIfAbsent(piece, p -> bake(minecraft, p));
        if (model == null) return null;

        model.copyPoseFrom(humanoid);
        return model;
    }

    private static PlayerModel poser(Minecraft minecraft) {
        if (poser == null) {
            poser = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        return poser;
    }

    private static AbyssalSamuraiArmorModel bake(Minecraft minecraft, AbyssalSamuraiArmorModel.Piece piece) {
        ModelPart root = minecraft.getEntityModels().bakeLayer(piece.layerLocation());
        return new AbyssalSamuraiArmorModel(root);
    }

    private static void resetPose(PlayerModel model) {
        model.head.resetPose();
        model.body.resetPose();
        model.rightArm.resetPose();
        model.leftArm.resetPose();
        model.rightLeg.resetPose();
        model.leftLeg.resetPose();
    }
}
