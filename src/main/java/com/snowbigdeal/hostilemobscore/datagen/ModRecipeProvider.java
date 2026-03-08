package com.snowbigdeal.hostilemobscore.datagen;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.recipe.DyeSlimeBlockRecipe;
import com.snowbigdeal.hostilemobscore.recipe.ModRecipes;
import com.snowbigdeal.hostilemobscore.recipe.UncraftSlimeBlockRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        output.accept(
                ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "dye_slime_block"),
                new DyeSlimeBlockRecipe(CraftingBookCategory.MISC),
                null
        );
        output.accept(
                ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "uncraft_slime_block"),
                new UncraftSlimeBlockRecipe(CraftingBookCategory.MISC),
                null
        );
    }


}
