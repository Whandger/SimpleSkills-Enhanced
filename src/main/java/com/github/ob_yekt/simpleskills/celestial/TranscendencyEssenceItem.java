package com.github.ob_yekt.simpleskills.celestial;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Rare catalyst used by the Celestial Altar. The permanent glint marks its Transcendent origin. */
public final class TranscendencyEssenceItem extends Item {
    public TranscendencyEssenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
