package com.snowbigdeal.hostilemobscore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Renders slam AoE zone as flat textured ground decals. */
@EventBusSubscriber(modid = HostileMobsCore.MODID, value = Dist.CLIENT)
public class SlamVfxManager {

    private static final ResourceLocation TEX_INDICATOR =
            ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "textures/vfx/damage_aoe.png");
    private static final ResourceLocation TEX_RING =
            ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "textures/vfx/damage_aoe_ring.png");

    private static final float DECAL_Y_OFFSET   = 0.05f;
    private static final float MIN_RING_RADIUS   = 0.05f;

    // Indicator disc — warm orange, semi-transparent
    private static final float INDICATOR_R = 1.0f, INDICATOR_G = 0.5f, INDICATOR_B = 0.1f, INDICATOR_A = 0.45f;
    // Expanding ring — bright red-orange, mostly opaque
    private static final float RING_R      = 1.0f, RING_G      = 0.2f, RING_B      = 0.0f, RING_A      = 0.8f;

    private static final List<SlamVfxInstance> ACTIVE = new ArrayList<>();

    public static void spawn(Vec3 center, float radius, int windupTicks) {
        ACTIVE.add(new SlamVfxInstance(center, radius, windupTicks));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ACTIVE.removeIf(inst -> --inst.remaining <= 0);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (ACTIVE.isEmpty()) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        for (SlamVfxInstance inst : ACTIVE) {
            float progress    = 1.0f - ((float) inst.remaining / inst.totalTicks);
            float ringRadius  = inst.radius * progress;

            poseStack.pushPose();
            poseStack.translate(
                    inst.center.x - cam.x,
                    inst.center.y - cam.y + DECAL_Y_OFFSET,
                    inst.center.z - cam.z);

            renderGroundQuad(poseStack, inst.radius, TEX_INDICATOR, INDICATOR_R, INDICATOR_G, INDICATOR_B, INDICATOR_A);
            if (ringRadius >= MIN_RING_RADIUS) {
                renderGroundQuad(poseStack, ringRadius, TEX_RING, RING_R, RING_G, RING_B, RING_A);
            }

            poseStack.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private static void renderGroundQuad(PoseStack poseStack, float radius,
                                         ResourceLocation texture,
                                         float r, float g, float b, float a) {
        RenderSystem.setShaderTexture(0, texture);
        Matrix4f matrix = poseStack.last().pose();

        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        buffer.addVertex(matrix, -radius, 0, -radius).setUv(0, 0).setColor(r, g, b, a);
        buffer.addVertex(matrix, -radius, 0,  radius).setUv(0, 1).setColor(r, g, b, a);
        buffer.addVertex(matrix,  radius, 0,  radius).setUv(1, 1).setColor(r, g, b, a);
        buffer.addVertex(matrix,  radius, 0, -radius).setUv(1, 0).setColor(r, g, b, a);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    static class SlamVfxInstance {
        final Vec3  center;
        final float radius;
        final int   totalTicks;
        int         remaining;

        SlamVfxInstance(Vec3 center, float radius, int windupTicks) {
            this.center     = center;
            this.radius     = radius;
            this.totalTicks = windupTicks;
            this.remaining  = windupTicks;
        }
    }
}
