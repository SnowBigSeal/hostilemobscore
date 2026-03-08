package com.snowbigdeal.hostilemobscore.block;

import com.snowbigdeal.hostilemobscore.items.ColoredSlimeBlockItem;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(BuiltInRegistries.BLOCK, HostileMobsCore.MODID);

    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(BuiltInRegistries.ITEM, HostileMobsCore.MODID);

    public static final Map<DyeColor, DeferredHolder<Block, ColoredSlimeBlock>> SLIME_BLOCKS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredHolder<Item, ColoredSlimeBlockItem>> SLIME_BLOCK_ITEMS = new EnumMap<>(DyeColor.class);

    static {
        for (DyeColor dye : DyeColor.values()) {
            DyeColor captured = dye;
            DeferredHolder<Block, ColoredSlimeBlock> block = BLOCKS.register(
                    dye.getName() + "_slime_block",
                    () -> new ColoredSlimeBlock(captured,
                            BlockBehaviour.Properties.of()
                                    .strength(0.3F)
                                    .sound(SoundType.SLIME_BLOCK)
                                    .noOcclusion()
                    )
            );
            SLIME_BLOCKS.put(dye, block);
            SLIME_BLOCK_ITEMS.put(dye, BLOCK_ITEMS.register(
                    dye.getName() + "_slime_block",
                    () -> new ColoredSlimeBlockItem(block.get(), captured, new Item.Properties())
            ));
        }
    }

    /** Convenience: get the block for a specific DyeColor. */
    public static ColoredSlimeBlock getSlimeBlock(DyeColor dye) {
        return SLIME_BLOCKS.get(dye).get();
    }

    /** Convenience: get the block item for a specific DyeColor. */
    public static ColoredSlimeBlockItem getSlimeBlockItem(DyeColor dye) {
        return SLIME_BLOCK_ITEMS.get(dye).get();
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ITEMS.register(eventBus);
    }
}
