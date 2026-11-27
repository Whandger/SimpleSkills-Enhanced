package com.github.ob_yekt.simpleskills;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.github.ob_yekt.simpleskills.agility.AgilityActionPayload;
import com.github.ob_yekt.simpleskills.ui.RankTitleMenuActionPayload;
import com.github.ob_yekt.simpleskills.ui.SkillOverviewRequestPayload;
import com.github.ob_yekt.simpleskills.prayer.PrayerActionPayload;
import com.github.ob_yekt.simpleskills.prayer.PrayerClientState;
import com.github.ob_yekt.simpleskills.crafting.CraftingEquipmentManager;
import com.github.ob_yekt.simpleskills.abyss.AbyssalKatanaItems;
import com.github.ob_yekt.simpleskills.crafting.TranscendentToolTogglePayload;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public class Keybinds {
    public static final String KEY_CATEGORY = "key.categories.misc";

    /** Categoria própria na tela de Controles (aparece como "SimpleSkills", não em "Misc"). */
    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Simpleskills.MOD_ID, "controls"));

    public static final KeyMapping TOGGLE_HUD_KEY = new KeyMapping(
            "key.simpleskills.toggle_hud",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_H,
            CATEGORY
    );

    public static final KeyMapping CYCLE_HUD_POSITION_KEY = new KeyMapping(
            "key.simpleskills.cycle_hud_position",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_J,
            CATEGORY
    );

    public static final KeyMapping AGILITY_DASH_KEY = new KeyMapping(
            "key.simpleskills.agility_dash",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_V,
            CATEGORY
    );

    public static final KeyMapping OPEN_RANK_TITLES_KEY = new KeyMapping(
            "key.simpleskills.open_rank_titles",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_U,
            CATEGORY
    );

    public static final KeyMapping OPEN_PRAYER_BOOK_KEY = new KeyMapping(
            "key.simpleskills.prayer_book", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATEGORY);
    public static final KeyMapping CAST_PRAYER_KEY = new KeyMapping(
            "key.simpleskills.prayer_cast", InputConstants.Type.KEYBOARD, InputConstants.KEY_R, CATEGORY);
    public static final KeyMapping OPEN_PRAYER_WHEEL_KEY = new KeyMapping(
            "key.simpleskills.prayer_wheel", InputConstants.Type.KEYBOARD, InputConstants.KEY_C, CATEGORY);

    public static final KeyMapping OPEN_SKILL_OVERVIEW_KEY = new KeyMapping(
            "key.simpleskills.skill_overview", InputConstants.Type.KEYBOARD, InputConstants.KEY_K, CATEGORY);

    public static final KeyMapping TOGGLE_TRANSCENDENT_AREA_KEY = new KeyMapping(
            "key.simpleskills.toggle_transcendent_area",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_L,
            CATEGORY
    );

    private static boolean wasJumpDown;
    private static int doubleJumpsUsed;
    private static boolean wasOnGround;
    private static UUID trackedPlayer;

    public static void register() {
        KeyMappingHelper.registerKeyMapping(TOGGLE_HUD_KEY);
        KeyMappingHelper.registerKeyMapping(CYCLE_HUD_POSITION_KEY);
        KeyMappingHelper.registerKeyMapping(AGILITY_DASH_KEY);
        KeyMappingHelper.registerKeyMapping(OPEN_RANK_TITLES_KEY);
        KeyMappingHelper.registerKeyMapping(OPEN_PRAYER_BOOK_KEY);
        KeyMappingHelper.registerKeyMapping(CAST_PRAYER_KEY);
        KeyMappingHelper.registerKeyMapping(OPEN_PRAYER_WHEEL_KEY);
        KeyMappingHelper.registerKeyMapping(OPEN_SKILL_OVERVIEW_KEY);
        KeyMappingHelper.registerKeyMapping(TOGGLE_TRANSCENDENT_AREA_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                wasJumpDown = false;
                doubleJumpsUsed = 0;
                wasOnGround = false;
                trackedPlayer = null;
                return;
            }

            boolean onGround = client.player.onGround();
            if (!client.player.getUUID().equals(trackedPlayer)) {
                trackedPlayer = client.player.getUUID();
                wasOnGround = onGround;
                doubleJumpsUsed = 0;
                wasJumpDown = client.options.keyJump.isDown();
            }
            if (onGround) doubleJumpsUsed = 0;
            if (OPEN_RANK_TITLES_KEY.consumeClick()
                    && ClientPlayNetworking.canSend(RankTitleMenuActionPayload.TYPE)) {
                ClientPlayNetworking.send(new RankTitleMenuActionPayload(RankTitleMenuActionPayload.OPEN, ""));
            }
            if (OPEN_SKILL_OVERVIEW_KEY.consumeClick()) {
                if (client.gui.screen() instanceof SkillOverviewScreen) {
                    client.gui.setScreen(null);
                } else if (client.gui.screen() == null
                        && ClientPlayNetworking.canSend(SkillOverviewRequestPayload.TYPE)) {
                    ClientPlayNetworking.send(new SkillOverviewRequestPayload(SkillOverviewRequestPayload.OPEN));
                }
            }
            if (TOGGLE_TRANSCENDENT_AREA_KEY.consumeClick() && client.gui.screen() == null
                    && ClientPlayNetworking.canSend(TranscendentToolTogglePayload.TYPE)) {
                var held = client.player.getMainHandItem();
                if (AbyssalKatanaItems.isKatana(held.getItem())
                        || CraftingEquipmentManager.stackHasPerk(held, "worldbreaker")
                        || CraftingEquipmentManager.stackHasPerk(held, "buried_truth")
                        || CraftingEquipmentManager.stackHasPerk(held, "edens_dominion")) {
                    ClientPlayNetworking.send(new TranscendentToolTogglePayload(
                            TranscendentToolTogglePayload.TOGGLE_AREA_EFFECT));
                }
            }
            if (OPEN_PRAYER_BOOK_KEY.consumeClick() && client.gui.screen() == null && ClientPlayNetworking.canSend(PrayerActionPayload.TYPE)) {
                PrayerClientState.requestOpen();
                ClientPlayNetworking.send(new PrayerActionPayload(PrayerActionPayload.OPEN, ""));
            }
            if (OPEN_PRAYER_WHEEL_KEY.consumeClick() && client.gui.screen() == null && ClientPlayNetworking.canSend(PrayerActionPayload.TYPE)) {
                PrayerClientState.requestWheel();
                ClientPlayNetworking.send(new PrayerActionPayload(PrayerActionPayload.OPEN_WHEEL, ""));
            }
            if (CAST_PRAYER_KEY.consumeClick() && client.gui.screen() == null && !PrayerClientState.equipped().isEmpty()
                    && ClientPlayNetworking.canSend(PrayerActionPayload.TYPE)) {
                ClientPlayNetworking.send(new PrayerActionPayload(PrayerActionPayload.CAST, PrayerClientState.equipped()));
            }
            if (AGILITY_DASH_KEY.consumeClick() && AgilityClientState.getLevel() >= AgilityClientState.getDashUnlockLevel()
                    && ClientPlayNetworking.canSend(AgilityActionPayload.TYPE)) {
                ClientPlayNetworking.send(new AgilityActionPayload(AgilityActionPayload.DASH));
            }

            boolean jumpDown = client.options.keyJump.isDown();
            if (jumpDown && !wasJumpDown && !onGround && !wasOnGround
                    && !client.player.isFallFlying() && !client.player.isPassenger()
                    && doubleJumpsUsed < 1 + CraftingEquipmentManager.getAdditionalDoubleJumps(client.player)
                    && AgilityClientState.getLevel() >= AgilityClientState.getDoubleJumpUnlockLevel()
                    && client.player.getFoodData().getFoodLevel() > 0
                    && ClientPlayNetworking.canSend(AgilityActionPayload.TYPE)) {
                doubleJumpsUsed++;
                Vec3 velocity = client.player.getDeltaMovement();
                net.minecraft.world.phys.Vec2 movement = client.player.input.getMoveVector();
                double yaw = Math.toRadians(client.player.getYRot());
                Vec3 direction = new Vec3(
                        movement.x * Math.cos(yaw) - movement.y * Math.sin(yaw),
                        0.0,
                        movement.x * Math.sin(yaw) + movement.y * Math.cos(yaw));
                direction = new Vec3(direction.x, 0.0, direction.z);
                if (direction.lengthSqr() < 1.0E-4) {
                    Vec3 look = client.player.getLookAngle();
                    direction = new Vec3(look.x, 0.0, look.z);
                }
                if (direction.lengthSqr() < 1.0E-4) direction = new Vec3(0.0, 0.0, 1.0);
                direction = direction.normalize();
                int level = AgilityClientState.getLevel();
                int unlockLevel = AgilityClientState.getDoubleJumpUnlockLevel();
                double strength = Math.max(0.0, Math.min(1.0,
                        (level - unlockLevel) / (double) Math.max(1, 40 - unlockLevel)));
                double forwardImpulse = 1.2 * (1.0 + level * 0.005) / 3.0 * (0.5 + 0.5 * strength);
                client.player.setDeltaMovement(velocity.x + direction.x * forwardImpulse,
                        Math.max(velocity.y, 0.45 + 0.45 * strength),
                        velocity.z + direction.z * forwardImpulse);
                client.player.fallDistance = 0.0F;
                ClientPlayNetworking.send(new AgilityActionPayload(AgilityActionPayload.DOUBLE_JUMP));
            }
            wasJumpDown = jumpDown;
            wasOnGround = onGround;

            if (SimpleskillsClient.isMultiplayer()) {
                return;
            }

            while (TOGGLE_HUD_KEY.consumeClick()) {
                SkillHudRenderer.toggleHudVisibility();
            }

            while (CYCLE_HUD_POSITION_KEY.consumeClick()) {
                ClientConfig.cycleHudPosition();
                if (client.player != null) {
                    client.player.sendSystemMessage(
                            Component.literal("§6[simpleskills]§f HUD position set to " + ClientConfig.getHudPosition())
                    );
                }
            }
        });
    }
}
