package com.snowbigdeal.hostilemobscore.mixin;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Placeholder — color blending removed since each color is now a distinct item type.
 * Different color slimeballs and slime blocks will not merge, which is the desired behavior.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
}
