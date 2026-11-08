package com.github.ob_yekt.simpleskills.mixin.FISHING;

import com.github.ob_yekt.simpleskills.fishing.FishAttributeHolder;
import com.github.ob_yekt.simpleskills.fishing.FishAttributes;
import com.github.ob_yekt.simpleskills.fishing.FishSpecies;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gives every fish of a registered species its own weight and length, stored on the entity and saved
 * with it.
 *
 * <p>The size is rolled once, the first time the fish is ticked on the server, unless it was already
 * loaded from NBT. Mobs that are not registered fish only pay one boolean check, once.</p>
 */
@Mixin(Mob.class)
public abstract class MobFishAttributesMixin implements FishAttributeHolder {

    @Unique private @Nullable FishAttributes simpleskills$attributes;
    @Unique private boolean simpleskills$checked;

    @Inject(method = "tick", at = @At("HEAD"))
    private void simpleskills$initFishAttributes(CallbackInfo ci) {
        if (this.simpleskills$checked) return;
        this.simpleskills$checked = true;
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide() || this.simpleskills$attributes != null) return;
        FishSpecies species = FishSpecies.of(self);
        if (species != null) {
            this.simpleskills$attributes = FishAttributes.generate(species, self.getRandom());
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simpleskills$saveFishAttributes(ValueOutput output, CallbackInfo ci) {
        FishAttributes attributes = this.simpleskills$attributes;
        if (attributes != null) {
            output.putFloat(FishAttributes.NBT_WEIGHT, attributes.weightKg());
            output.putFloat(FishAttributes.NBT_LENGTH, attributes.lengthCm());
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simpleskills$loadFishAttributes(ValueInput input, CallbackInfo ci) {
        FishAttributes loaded = new FishAttributes(
                input.getFloatOr(FishAttributes.NBT_WEIGHT, 0.0F),
                input.getFloatOr(FishAttributes.NBT_LENGTH, 0.0F));
        if (loaded.isValid()) {
            this.simpleskills$attributes = loaded;
        }
    }

    @Override
    public @Nullable FishAttributes simpleskills$getFishAttributes() {
        return this.simpleskills$attributes;
    }

    @Override
    public void simpleskills$setFishAttributes(@Nullable FishAttributes attributes) {
        this.simpleskills$attributes = attributes;
        this.simpleskills$checked = true;
    }
}
