package com.snowbigdeal.hostilemobscore.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class ColoredSlimeBlock extends SlimeBlock {

    public final DyeColor dyeColor;

    @SuppressWarnings("unchecked")
    public static final MapCodec<SlimeBlock> CODEC = (MapCodec<SlimeBlock>) (MapCodec<?>) simpleCodec(
            props -> new ColoredSlimeBlock(DyeColor.LIME, props));

    public ColoredSlimeBlock(DyeColor dyeColor, Properties properties) {
        super(properties);
        this.dyeColor = dyeColor;
    }

    @Override
    public MapCodec<SlimeBlock> codec() {
        return CODEC;
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.NORMAL;
    }

    @Override
    public boolean isSlimeBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean isStickyBlock(BlockState state) {
        return true;
    }

    @Override
    public boolean canStickTo(BlockState state, BlockState other) {
        return !(other.getBlock() instanceof HoneyBlock);
    }
}
