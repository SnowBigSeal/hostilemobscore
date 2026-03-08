package com.snowbigdeal.hostilemobscore.recipe;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, HostileMobsCore.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<ColoredSlimeBlockRecipe>> COLORED_SLIME_BLOCK =
            SERIALIZERS.register("colored_slime_block",
                    () -> new SimpleCraftingRecipeSerializer<>(ColoredSlimeBlockRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<DyeSlimeBlockRecipe>> DYE_SLIME_BLOCK =
            SERIALIZERS.register("dye_slime_block",
                    () -> new SimpleCraftingRecipeSerializer<>(DyeSlimeBlockRecipe::new));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<UncraftSlimeBlockRecipe>> UNCRAFT_SLIME_BLOCK =
            SERIALIZERS.register("uncraft_slime_block",
                    () -> new SimpleCraftingRecipeSerializer<>(UncraftSlimeBlockRecipe::new));

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
