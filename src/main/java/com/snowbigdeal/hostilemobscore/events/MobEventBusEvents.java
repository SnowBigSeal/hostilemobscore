package com.snowbigdeal.hostilemobscore.events;

import com.snowbigdeal.hostilemobscore.ServerConfig;
import com.snowbigdeal.hostilemobscore.entity.ModEntities;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime.AngrySlime;
import com.snowbigdeal.hostilemobscore.entity.slimes.client.sleepyslime.SleepySlime;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.items.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid = HostileMobsCore.MODID)
public class MobEventBusEvents {

    @SubscribeEvent
    public static void entityAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ANGRY_SLIME.get(), AngrySlime.createAttributes().build());
        event.put(ModEntities.SLEEPY_SLIME.get(), SleepySlime.createAttributes().build());
    }

    @SubscribeEvent
    public static void onMobSpawnCheck(MobSpawnEvent.PositionCheck event) {
        EntityType<?> type = event.getEntity().getType();

        // Only act on vanilla hostile mobs
        if (type.getCategory() != MobCategory.MONSTER) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (!id.getNamespace().equals("minecraft")) return;

        if (!ServerConfig.VANILLA_HOSTILE_MOBS_ENABLED.get()) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            return;
        }

        boolean blacklisted = ServerConfig.VANILLA_HOSTILE_MOB_BLACKLIST.get()
                .stream().anyMatch(entry -> entry.equals(id.toString()));
        if (blacklisted) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onVanillaSlimeDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Slime)) return;
        // Don't intercept our own AngrySlime (it extends Slime indirectly)
        if (event.getEntity() instanceof AngrySlime) return;

        int lootingLevel = 0;
        if (event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity killer) {
            lootingLevel = net.minecraft.world.item.enchantment.EnchantmentHelper
                    .getItemEnchantmentLevel(
                            event.getEntity().level().registryAccess()
                                    .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                                    .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),
                            killer.getMainHandItem());
        }

        // Remove any vanilla slimeball drops
        event.getDrops().removeIf(itemEntity ->
                itemEntity.getItem().is(net.minecraft.world.item.Items.SLIME_BALL));

        // Add 1-3 lime colored slimeballs (plus looting bonus)
        int count = 1 + event.getEntity().getRandom().nextInt(3) + lootingLevel;
        ItemStack stack = new ItemStack(ModItems.getSlimeball(DyeColor.LIME), count);
        double x = event.getEntity().getX();
        double y = event.getEntity().getY();
        double z = event.getEntity().getZ();
        event.getDrops().add(new ItemEntity(event.getEntity().level(), x, y, z, stack));
    }
}
