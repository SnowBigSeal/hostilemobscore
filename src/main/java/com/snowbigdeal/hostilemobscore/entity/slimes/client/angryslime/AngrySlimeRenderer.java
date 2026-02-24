package com.snowbigdeal.hostilemobscore.entity.slimes.client.angryslime;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AngrySlimeRenderer extends GeoEntityRenderer<AngrySlime> {
    public AngrySlimeRenderer(EntityRendererProvider.Context context) {
        super(context, new AngrySlimeModel());
    }

    @Override
    public void renderRecursively(PoseStack poseStack, AngrySlime animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        // Check if this is the outer_cube bone and apply translucent rendering
        if(bone.getName().equals("outer_cube")) {
            renderType = RenderType.entityTranslucent(this.getGeoModel().getTextureResource(animatable, this));
            buffer = bufferSource.getBuffer(renderType);
            packedLight = LightTexture.FULL_BRIGHT;
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }
}
