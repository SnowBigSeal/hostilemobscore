package com.snowbigdeal.hostilemobscore.recipe;

import com.snowbigdeal.hostilemobscore.block.ModBlocks;
import com.snowbigdeal.hostilemobscore.items.ColoredSlimeballItem;
import com.snowbigdeal.hostilemobscore.items.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class ColoredSlimeBlockRecipe extends CustomRecipe {

    public ColoredSlimeBlockRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.width() != 3 || input.height() != 3) return false;
        DyeColor required = null;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getItem(i);
            if (!(stack.getItem() instanceof ColoredSlimeballItem ball)) return false;
            DyeColor dye = ball.getDyeColor();
            if (required == null) required = dye;
            else if (required != dye) return false;
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = input.getItem(i);
            if (stack.getItem() instanceof ColoredSlimeballItem ball) {
                return new ItemStack(ModBlocks.getSlimeBlockItem(ball.getDyeColor()));
            }
        }
        return new ItemStack(ModBlocks.getSlimeBlockItem(DyeColor.LIME));
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width == 3 && height == 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.COLORED_SLIME_BLOCK.get();
    }
}
