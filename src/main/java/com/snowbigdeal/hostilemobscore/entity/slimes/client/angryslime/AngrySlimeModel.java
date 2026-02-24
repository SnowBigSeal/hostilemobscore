package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AngrySlimeModel  extends GeoModel<AngrySlime> {
    private final ResourceLocation model = ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID,
            "geo/angryslime.geo.json");
    private final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID,
            "textures/angryslime.png");
    private final ResourceLocation animation = ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID,
            "animations/angryslime.animation.json");

    @Override
    public ResourceLocation getModelResource(AngrySlime animatable) {
        return model;
    }

    @Override
    public ResourceLocation getTextureResource(AngrySlime animatable) {
        return texture;
    }

    @Override
    public ResourceLocation getAnimationResource(AngrySlime animatable) {
        return animation;
    }
}
