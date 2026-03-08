package com.snowbigdeal.hostilemobscore.datagen;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.block.ModBlocks;
import com.snowbigdeal.hostilemobscore.items.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, String modid, ExistingFileHelper existingFileHelper) {
        super(output, modid, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        withExistingParent(ModItems.ANGRY_SLIME_SPAWN_EGG.getId().getPath(),
                ResourceLocation.fromNamespaceAndPath("minecraft", "item/template_spawn_egg"));

        ModelFile blockModel = new ModelFile.UncheckedModelFile(
                ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "block/colored_slime_block").toString());

        for (DyeColor dye : DyeColor.values()) {
            withExistingParent(dye.getName() + "_slimeball",
                    ResourceLocation.fromNamespaceAndPath("minecraft", "item/generated"))
                    .texture("layer0", ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "item/slimeball"))
                    .texture("layer1", ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "item/slimeball_highlight"));

            getBuilder(dye.getName() + "_slime_block").parent(blockModel);
        }
    }
}
