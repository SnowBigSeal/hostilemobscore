package com.snowbigdeal.hostilemobscore.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.attack.shape.CircleShape;
import com.snowbigdeal.hostilemobscore.attack.shape.ConeShape;
import com.snowbigdeal.hostilemobscore.attack.shape.LineShape;
import com.snowbigdeal.hostilemobscore.attack.shape.TelegraphAttackShape;
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

/** Renders telegraph attack VFX; dispatches per-shape (circle, cone, line). */
@EventBusSubscriber(modid = HostileMobsCore.MODID, value = Dist.CLIENT)
public class TelegraphAttackVfxManager {

    // ---- Textures ----
    private static final ResourceLocation TEX_INDICATOR =
            ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "textures/vfx/damage_aoe.png");
    private static final ResourceLocation TEX_RING =
            ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "textures/vfx/damage_aoe_ring.png");

    // ---- Shared constants ----
    private static final float DECAL_Y_OFFSET  = 0.05f;
    private static final float MIN_RING_RADIUS = 0.05f;
    private static final int   SEGMENTS        = 32;

    // ---- Circle colours ----
    private static final float IND_R = 1.0f, IND_G = 0.5f, IND_B = 0.1f, IND_A = 0.45f;
    private static final float RNG_R = 1.0f, RNG_G = 0.2f, RNG_B = 0.0f, RNG_A = 0.8f;
    private static final float CYL_R = 1.0f, CYL_G = 0.55f, CYL_B = 0.15f;

    // ---- Cone colours ----
    private static final float CONE_FILL_R = 1.0f, CONE_FILL_G = 0.4f, CONE_FILL_B = 0.0f, CONE_FILL_A = 0.4f;
    private static final float CONE_EDGE_R = 1.0f, CONE_EDGE_G = 0.15f, CONE_EDGE_B = 0.0f, CONE_EDGE_A = 0.75f;
    private static final float CONE_WALL_R = 1.0f, CONE_WALL_G = 0.4f, CONE_WALL_B = 0.0f;

    // ---- Line colours ----
    private static final float LINE_FILL_R = 0.9f, LINE_FILL_G = 0.3f, LINE_FILL_B = 0.0f, LINE_FILL_A = 0.4f;
    private static final float LINE_EDGE_R = 1.0f, LINE_EDGE_G = 0.1f, LINE_EDGE_B = 0.0f, LINE_EDGE_A = 0.8f;
    private static final float LINE_WALL_R = 0.9f, LINE_WALL_G = 0.35f, LINE_WALL_B = 0.0f;

    private static final List<VfxInstance> ACTIVE = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static void spawn(TelegraphAttackShape shape, int lifetimeTicks) {
        ACTIVE.add(new VfxInstance(shape, lifetimeTicks));
    }

    // -------------------------------------------------------------------------
    // Events
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ACTIVE.removeIf(inst -> --inst.remaining <= 0);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (ACTIVE.isEmpty()) return;

        Vec3      cam       = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();

        for (VfxInstance inst : ACTIVE) {
            float progress = 1.0f - ((float) inst.remaining / inst.totalTicks);

            poseStack.pushPose();
            Vec3 c = inst.shape.center();
            poseStack.translate(c.x - cam.x, c.y - cam.y + DECAL_Y_OFFSET, c.z - cam.z);

            switch (inst.shape) {
                case CircleShape cs -> renderCircle(poseStack, cs, progress);
                case ConeShape   cs -> renderCone(poseStack, cs, progress);
                case LineShape   ls -> renderLine(poseStack, ls, progress);
            }

            poseStack.popPose();
        }

        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    // -------------------------------------------------------------------------
    // Circle renderer
    // -------------------------------------------------------------------------

    private static void renderCircle(PoseStack ps, CircleShape shape, float progress) {
        float radius     = shape.radius();
        float ringRadius = radius * progress;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        renderGroundQuad(ps, radius, TEX_INDICATOR, IND_R, IND_G, IND_B, IND_A);
        if (ringRadius >= MIN_RING_RADIUS) {
            renderGroundQuad(ps, ringRadius, TEX_RING, RNG_R, RNG_G, RNG_B, RNG_A);
        }
        renderCylinder(ps, radius, radius * progress);
    }

    // -------------------------------------------------------------------------
    // Cone renderer
    // -------------------------------------------------------------------------

    private static void renderCone(PoseStack ps, ConeShape shape, float progress) {
        float length      = shape.length();
        float halfAngle   = (float) Math.toRadians(shape.halfAngleDeg());
        float dirAngle    = (float) Math.atan2(shape.direction().z, shape.direction().x);
        float sweepLength = length * progress; // advancing sweep from apex

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mx = ps.last().pose();

        // ---- Filled sector (ground indicator) ----
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(mx, 0, 0, 0).setColor(CONE_FILL_R, CONE_FILL_G, CONE_FILL_B, CONE_FILL_A);
        for (int i = 0; i <= SEGMENTS; i++) {
            float angle = dirAngle - halfAngle + (2 * halfAngle * i / SEGMENTS);
            buf.addVertex(mx, (float)(Math.cos(angle) * length), 0, (float)(Math.sin(angle) * length))
               .setColor(CONE_FILL_R, CONE_FILL_G, CONE_FILL_B, 0.0f);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());

        // ---- Advancing arc (sweep from apex outward) ----
        if (sweepLength >= 0.05f) {
            BufferBuilder arcBuf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
            float innerR = Math.max(0, sweepLength - 0.5f);
            for (int i = 0; i <= SEGMENTS; i++) {
                float angle = dirAngle - halfAngle + (2 * halfAngle * i / SEGMENTS);
                float cx = (float) Math.cos(angle), cz = (float) Math.sin(angle);
                arcBuf.addVertex(mx, cx * sweepLength, 0, cz * sweepLength).setColor(CONE_EDGE_R, CONE_EDGE_G, CONE_EDGE_B, CONE_EDGE_A);
                arcBuf.addVertex(mx, cx * innerR, 0, cz * innerR).setColor(CONE_EDGE_R, CONE_EDGE_G, CONE_EDGE_B, 0.0f);
            }
            BufferUploader.drawWithShader(arcBuf.buildOrThrow());
        }

        // ---- Edge walls along the two straight sides ----
        float wallH = length * progress * 0.6f;
        if (wallH > 0.01f) {
            BufferBuilder wallBuf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int side = -1; side <= 1; side += 2) {
                float angle = dirAngle + side * halfAngle;
                float ex = (float)(Math.cos(angle) * length), ez = (float)(Math.sin(angle) * length);
                wallBuf.addVertex(mx, 0,     0,     0    ).setColor(CONE_WALL_R, CONE_WALL_G, CONE_WALL_B, 0.55f);
                wallBuf.addVertex(mx, 0,     wallH, 0    ).setColor(CONE_WALL_R, CONE_WALL_G, CONE_WALL_B, 0.0f);
                wallBuf.addVertex(mx, ex,    wallH, ez   ).setColor(CONE_WALL_R, CONE_WALL_G, CONE_WALL_B, 0.0f);
                wallBuf.addVertex(mx, ex,    0,     ez   ).setColor(CONE_WALL_R, CONE_WALL_G, CONE_WALL_B, 0.55f);
            }
            BufferUploader.drawWithShader(wallBuf.buildOrThrow());
        }
    }

    // -------------------------------------------------------------------------
    // Line renderer
    // -------------------------------------------------------------------------

    private static void renderLine(PoseStack ps, LineShape shape, float progress) {
        float length    = shape.length();
        float halfWidth = shape.width() * 0.5f;
        float dirAngle  = (float) Math.atan2(shape.direction().z, shape.direction().x);
        float sweepLen  = length * progress;

        // Rotate the pose stack to align with the line direction
        ps.pushPose();
        ps.mulPose(new org.joml.Quaternionf().rotationY(-dirAngle));
        Matrix4f mx = ps.last().pose();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        // ---- Full rectangle indicator ----
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.addVertex(mx, 0,       0, -halfWidth).setColor(LINE_FILL_R, LINE_FILL_G, LINE_FILL_B, LINE_FILL_A);
        buf.addVertex(mx, 0,       0,  halfWidth).setColor(LINE_FILL_R, LINE_FILL_G, LINE_FILL_B, LINE_FILL_A);
        buf.addVertex(mx, length,  0,  halfWidth).setColor(LINE_FILL_R, LINE_FILL_G, LINE_FILL_B, 0.0f);
        buf.addVertex(mx, length,  0, -halfWidth).setColor(LINE_FILL_R, LINE_FILL_G, LINE_FILL_B, 0.0f);
        BufferUploader.drawWithShader(buf.buildOrThrow());

        // ---- Advancing front edge ----
        if (sweepLen >= 0.05f) {
            float innerLen = Math.max(0, sweepLen - 0.5f);
            BufferBuilder edgeBuf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            edgeBuf.addVertex(mx, innerLen,  0, -halfWidth).setColor(LINE_EDGE_R, LINE_EDGE_G, LINE_EDGE_B, 0.0f);
            edgeBuf.addVertex(mx, innerLen,  0,  halfWidth).setColor(LINE_EDGE_R, LINE_EDGE_G, LINE_EDGE_B, 0.0f);
            edgeBuf.addVertex(mx, sweepLen,  0,  halfWidth).setColor(LINE_EDGE_R, LINE_EDGE_G, LINE_EDGE_B, LINE_EDGE_A);
            edgeBuf.addVertex(mx, sweepLen,  0, -halfWidth).setColor(LINE_EDGE_R, LINE_EDGE_G, LINE_EDGE_B, LINE_EDGE_A);
            BufferUploader.drawWithShader(edgeBuf.buildOrThrow());
        }

        // ---- Side walls along the two long edges ----
        float wallH = shape.width() * progress * 0.4f;
        if (wallH > 0.01f) {
            BufferBuilder wallBuf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (int side = -1; side <= 1; side += 2) {
                float wz = side * halfWidth;
                wallBuf.addVertex(mx, 0,      0,     wz).setColor(LINE_WALL_R, LINE_WALL_G, LINE_WALL_B, 0.55f);
                wallBuf.addVertex(mx, 0,      wallH, wz).setColor(LINE_WALL_R, LINE_WALL_G, LINE_WALL_B, 0.0f);
                wallBuf.addVertex(mx, length, wallH, wz).setColor(LINE_WALL_R, LINE_WALL_G, LINE_WALL_B, 0.0f);
                wallBuf.addVertex(mx, length, 0,     wz).setColor(LINE_WALL_R, LINE_WALL_G, LINE_WALL_B, 0.55f);
            }
            BufferUploader.drawWithShader(wallBuf.buildOrThrow());
        }

        ps.popPose();
    }

    // -------------------------------------------------------------------------
    // Shared helpers
    // -------------------------------------------------------------------------

    private static void renderGroundQuad(PoseStack ps, float radius,
                                         ResourceLocation texture,
                                         float r, float g, float b, float a) {
        RenderSystem.setShaderTexture(0, texture);
        Matrix4f mx = ps.last().pose();
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buf.addVertex(mx, -radius, 0, -radius).setUv(0, 0).setColor(r, g, b, a);
        buf.addVertex(mx, -radius, 0,  radius).setUv(0, 1).setColor(r, g, b, a);
        buf.addVertex(mx,  radius, 0,  radius).setUv(1, 1).setColor(r, g, b, a);
        buf.addVertex(mx,  radius, 0, -radius).setUv(1, 0).setColor(r, g, b, a);
        BufferUploader.drawWithShader(buf.buildOrThrow());
    }

    private static void renderCylinder(PoseStack ps, float radius, float height) {
        if (height < 0.01f) return;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f mx = ps.last().pose();
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i < SEGMENTS; i++) {
            double a1 = i       * 2.0 * Math.PI / SEGMENTS;
            double a2 = (i + 1) * 2.0 * Math.PI / SEGMENTS;
            float x1 = (float)(Math.cos(a1) * radius), z1 = (float)(Math.sin(a1) * radius);
            float x2 = (float)(Math.cos(a2) * radius), z2 = (float)(Math.sin(a2) * radius);
            buf.addVertex(mx, x1, 0,      z1).setColor(CYL_R, CYL_G, CYL_B, 0.55f);
            buf.addVertex(mx, x1, height, z1).setColor(CYL_R, CYL_G, CYL_B, 0.0f);
            buf.addVertex(mx, x2, height, z2).setColor(CYL_R, CYL_G, CYL_B, 0.0f);
            buf.addVertex(mx, x2, 0,      z2).setColor(CYL_R, CYL_G, CYL_B, 0.55f);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
    }

    // -------------------------------------------------------------------------
    // Instance
    // -------------------------------------------------------------------------

    static class VfxInstance {
        final TelegraphAttackShape shape;
        final int totalTicks;
        int remaining;

        VfxInstance(TelegraphAttackShape shape, int totalTicks) {
            this.shape      = shape;
            this.totalTicks = totalTicks;
            this.remaining  = totalTicks;
        }
    }
}
