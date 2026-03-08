package com.snowbigdeal.hostilemobscore.recipe;

import com.snowbigdeal.hostilemobscore.block.ColoredSlimeBlock;
import com.snowbigdeal.hostilemobscore.items.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Shapeless: 1 colored slime block -> 9 slimeballs of matching color. */
public class UncraftSlimeBlockRecipe extends CustomRecipe {

    public UncraftSlimeBlockRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int nonEmpty = 0;
        boolean hasBlock = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            nonEmpty++;
            if (stack.getItem() instanceof com.snowbigdeal.hostilemobscore.items.ColoredSlimeBlockItem) {
                hasBlock = true;
            } else {
                return false;
            }
        }
        return nonEmpty == 1 && hasBlock;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof com.snowbigdeal.hostilemobscore.items.ColoredSlimeBlockItem blockItem) {
                DyeColor dye = ((ColoredSlimeBlock) blockItem.getBlock()).dyeColor;
                return new ItemStack(ModItems.getSlimeball(dye), 9);
            }
        }
        return new ItemStack(ModItems.getSlimeball(DyeColor.LIME), 9);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.UNCRAFT_SLIME_BLOCK.get();
    }
}
