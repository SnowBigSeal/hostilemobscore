package com.snowbigdeal.hostilemobscore.items;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ColoredSlimeballItem extends Item {

    private final DyeColor dyeColor;

    public ColoredSlimeballItem(DyeColor dyeColor, Properties properties) {
        super(properties);
        this.dyeColor = dyeColor;
    }

    public DyeColor getDyeColor() {
        return dyeColor;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("color.minecraft." + dyeColor.getName())
                .append(Component.translatable("item.hostilemobscore.slimeball.suffix"));
    }
}
