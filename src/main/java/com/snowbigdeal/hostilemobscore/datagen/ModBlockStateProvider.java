package com.snowbigdeal.hostilemobscore.datagen;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, HostileMobsCore.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        ModelFile model = new ModelFile.UncheckedModelFile(
                ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "block/colored_slime_block"));
        for (DyeColor dye : DyeColor.values()) {
            simpleBlock(ModBlocks.getSlimeBlock(dye), new ConfiguredModel(model));
        }
    }
}
