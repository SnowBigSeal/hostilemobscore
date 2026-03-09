package com.snowbigdeal.hostilemobscore.entity.slimes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.snowbigdeal.hostilemobscore.entity.slimes.BaseSlime;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
/**
 * Generic GeckoLib renderer for all slime-type entities.
 * Renders the {@code outer_cube} bone with an entity-translucent render type
 * so the outer jelly layer is transparent.
 */
public class SlimeRenderer<T extends BaseSlime<T>> extends GeoEntityRenderer<T> {

    public SlimeRenderer(EntityRendererProvider.Context context, SlimeModel<T> model) {
        super(context, model);
    }

    /** Factory method — avoids F-bounded type inference issues at the call site. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends BaseSlime<T>> EntityRendererProvider<T> factory(String slimeName) {
        return ctx -> new SlimeRenderer(ctx, new SlimeModel(slimeName));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, T animatable, GeoBone bone, RenderType renderType,
                                  MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, int colour) {
        if (bone.getName().equals("outer_cube")) {
            renderType = RenderType.entityTranslucent(this.getGeoModel().getTextureResource(animatable, this));
            buffer = bufferSource.getBuffer(renderType);
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer,
                isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
