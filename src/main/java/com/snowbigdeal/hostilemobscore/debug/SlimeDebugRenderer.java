package com.snowbigdeal.hostilemobscore.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.snowbigdeal.hostilemobscore.entity.HostileMob;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.*;

/**
 * Draws debug wireframes in-world.
 * <ul>
 *   <li><b>tether</b> – orange circle at entity's Y showing the 64-block tether radius, plus a
 *       line back to the tether anchor.</li>
 *   <li><b>party</b> – coloured lines connecting party members to each other.</li>
 * </ul>
 * Toggle via {@code /hmcdebug tether}, {@code /hmcdebug party}, or {@code /hmcdebug off}.
 */
public class SlimeDebugRenderer {

    public static final Set<String> ACTIVE_MODES = new HashSet<>();

    public static void toggle(String mode) {
        if (!ACTIVE_MODES.remove(mode)) ACTIVE_MODES.add(mode);
    }

    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (ACTIVE_MODES.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        var camPos = event.getCamera().getPosition();
        double camX = camPos.x, camY = camPos.y, camZ = camPos.z;

        AABB searchBox = mc.player.getBoundingBox().inflate(128);
        @SuppressWarnings("unchecked")
        List<HostileMob<?>> slimes = mc.level.getEntitiesOfClass(
                (Class<HostileMob<?>>) (Class<?>) HostileMob.class, searchBox);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        PoseStack poseStack = event.getPoseStack();

        if (ACTIVE_MODES.contains("tether")) {
            for (HostileMob<?> slime : slimes) {
                BlockPos anchor = slime.getSyncedTetherCenter();
                double ax = anchor.getX() + 0.5 - camX;
                double ay = anchor.getY() - camY;
                double az = anchor.getZ() + 0.5 - camZ;
                double sy = slime.getY() - camY;

                // Circle at entity's Y centred on tether anchor
                drawCircle(poseStack, lines, ax, sy, az, 32.0, 1.0f, 0.45f, 0.0f, 1.0f, 48);
                // Line from anchor to entity
                drawLine(poseStack, lines,
                        ax, ay, az,
                        slime.getX() - camX, slime.getY() - camY, slime.getZ() - camZ,
                        1.0f, 0.45f, 0.0f, 0.5f);
            }
        }

        if (ACTIVE_MODES.contains("party")) {
            Map<UUID, List<HostileMob<?>>> parties = new HashMap<>();
            for (HostileMob<?> slime : slimes) {
                slime.getSyncedPartyId().ifPresent(id ->
                        parties.computeIfAbsent(id, k -> new ArrayList<>()).add(slime));
            }
            for (Map.Entry<UUID, List<HostileMob<?>>> entry : parties.entrySet()) {
                float[] col = partyColor(entry.getKey());
                List<HostileMob<?>> members = entry.getValue();
                for (int i = 0; i < members.size(); i++) {
                    for (int j = i + 1; j < members.size(); j++) {
                        HostileMob<?> a = members.get(i);
                        HostileMob<?> b = members.get(j);
                        drawLine(poseStack, lines,
                                a.getX() - camX, a.getY() + 1.0 - camY, a.getZ() - camZ,
                                b.getX() - camX, b.getY() + 1.0 - camY, b.getZ() - camZ,
                                col[0], col[1], col[2], 1.0f);
                    }
                }
            }
        }

        buffers.endBatch(RenderType.lines());
    }

    // -------------------------------------------------------------------------
    // Geometry helpers
    // -------------------------------------------------------------------------

    private static void drawCircle(PoseStack poseStack, VertexConsumer consumer,
                                    double cx, double cy, double cz, double radius,
                                    float r, float g, float b, float a, int segments) {
        PoseStack.Pose pose = poseStack.last();
        for (int i = 0; i < segments; i++) {
            double a1 = 2.0 * Math.PI * i / segments;
            double a2 = 2.0 * Math.PI * (i + 1) / segments;
            float x1 = (float) (cx + radius * Math.cos(a1));
            float z1 = (float) (cz + radius * Math.sin(a1));
            float x2 = (float) (cx + radius * Math.cos(a2));
            float z2 = (float) (cz + radius * Math.sin(a2));
            consumer.addVertex(pose, x1, (float) cy, z1).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
            consumer.addVertex(pose, x2, (float) cy, z2).setColor(r, g, b, a).setNormal(pose, 0, 1, 0);
        }
    }

    private static void drawLine(PoseStack poseStack, VertexConsumer consumer,
                                  double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  float r, float g, float b, float a) {
        PoseStack.Pose pose = poseStack.last();
        float dx = (float) (x2 - x1), dy = (float) (y2 - y1), dz = (float) (z2 - z1);
        float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.001f) return;
        consumer.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(pose, dx / len, dy / len, dz / len);
        consumer.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(pose, dx / len, dy / len, dz / len);
    }

    private static float[] partyColor(UUID id) {
        int hash = id.hashCode();
        float r = ((hash >> 16) & 0xFF) / 255.0f;
        float g = ((hash >> 8) & 0xFF) / 255.0f;
        float b = (hash & 0xFF) / 255.0f;
        // Boost towards a saturated, visible colour
        float max = Math.max(r, Math.max(g, b));
        if (max < 0.4f) { r += 0.4f; g += 0.4f; b += 0.4f; }
        return new float[]{Math.min(r, 1), Math.min(g, 1), Math.min(b, 1)};
    }
}
