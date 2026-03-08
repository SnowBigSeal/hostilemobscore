package com.snowbigdeal.hostilemobscore.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class ColoredSlimeBlockItem extends BlockItem {

    private final DyeColor dyeColor;

    public ColoredSlimeBlockItem(Block block, DyeColor dyeColor, Properties properties) {
        super(block, properties);
        this.dyeColor = dyeColor;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("color.minecraft." + dyeColor.getName())
                .append(Component.translatable("block.hostilemobscore.colored_slime_block.suffix"));
    }
}
