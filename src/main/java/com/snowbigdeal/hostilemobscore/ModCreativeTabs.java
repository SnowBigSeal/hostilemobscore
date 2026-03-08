package com.snowbigdeal.hostilemobscore;

import com.snowbigdeal.hostilemobscore.block.ModBlocks;
import com.snowbigdeal.hostilemobscore.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, HostileMobsCore.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HOSTILE_MOBS_TAB =
            TABS.register("hostile_mobs", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hostilemobscore.hostile_mobs"))
                    .icon(() -> new ItemStack(ModItems.getSlimeball(DyeColor.LIME)))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.ANGRY_SLIME_SPAWN_EGG.get());
                        for (DyeColor dye : DyeColor.values()) {
                            output.accept(ModItems.getSlimeball(dye));
                        }
                        for (DyeColor dye : DyeColor.values()) {
                            output.accept(ModBlocks.getSlimeBlockItem(dye));
                        }
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        TABS.register(eventBus);
    }
}
