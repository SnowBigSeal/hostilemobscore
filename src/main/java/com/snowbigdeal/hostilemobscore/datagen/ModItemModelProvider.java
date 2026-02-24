package com.snowbigdeal.hostilemobscore.datagen;

import com.snowbigdeal.hostilemobscore.items.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        withExistingParent(ModItems.ANGRY_SLIME_SPAWN_EGG.getId().getPath(), ResourceLocation.fromNamespaceAndPath("minecraft","item/template_spawn_egg"));
    }
}
