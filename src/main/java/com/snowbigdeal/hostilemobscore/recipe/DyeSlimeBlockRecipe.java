package com.snowbigdeal.hostilemobscore.recipe;

import com.snowbigdeal.hostilemobscore.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Shapeless: 1 colored slime block + 1 dye -> 1 recolored slime block. */
public class DyeSlimeBlockRecipe extends CustomRecipe {

    public DyeSlimeBlockRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        boolean hasBlock = false;
        boolean hasDye = false;
        int nonEmpty = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            nonEmpty++;
            if (stack.getItem() instanceof com.snowbigdeal.hostilemobscore.items.ColoredSlimeBlockItem) {
                hasBlock = true;
            } else if (stack.getItem() instanceof DyeItem) {
                hasDye = true;
            } else {
                return false;
            }
        }
        return nonEmpty == 2 && hasBlock && hasDye;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        DyeColor targetColor = DyeColor.LIME;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof DyeItem dye) {
                targetColor = dye.getDyeColor();
                break;
            }
        }
        return new ItemStack(ModBlocks.getSlimeBlockItem(targetColor));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.DYE_SLIME_BLOCK.get();
    }
}
