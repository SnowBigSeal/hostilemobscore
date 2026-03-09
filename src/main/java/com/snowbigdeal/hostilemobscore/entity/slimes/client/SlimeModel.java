package com.snowbigdeal.hostilemobscore.entity.slimes.client;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.entity.slimes.BaseSlime;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Generic GeckoLib model for all slime-type entities.
 * Pass the slime's asset name (e.g. {@code "angryslime"}) and the paths to
 * geo, texture, and animation files are derived automatically.
 */
public class SlimeModel<T extends BaseSlime<T>> extends GeoModel<T> {

    private final ResourceLocation model;
    private final ResourceLocation texture;
    private final ResourceLocation animation;

    public SlimeModel(String slimeName) {
        this.model     = ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "geo/" + slimeName + ".geo.json");
        this.texture   = ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "textures/" + slimeName + ".png");
        this.animation = ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "animations/" + slimeName + ".animation.json");
    }

    @Override public ResourceLocation getModelResource(T animatable)     { return model; }
    @Override public ResourceLocation getTextureResource(T animatable)   { return texture; }
    @Override public ResourceLocation getAnimationResource(T animatable) { return animation; }
}
