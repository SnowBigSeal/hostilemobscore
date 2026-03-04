package com.snowbigdeal.hostilemobscore.attack;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * Immutable snapshot of entities captured within a radius at a specific point in time.
 * Used to commit attack targets at the moment an attack is "locked in", so that
 * landing/impact logic damages exactly who was in the zone at commit time.
 *
 * @param <T> the entity type to snapshot
 */
public final class AttackSnapshot<T extends Entity> {

    private final Vec3    center;
    private final float   radius;
    private final List<T> targets;

    private AttackSnapshot(Vec3 center, float radius, List<T> targets) {
        this.center  = center;
        this.radius  = radius;
        this.targets = List.copyOf(targets);
    }

    /**
     * Captures all entities of {@code entityClass} within {@code radius} of {@code center}
     * that satisfy {@code filter}, using a pure distance check (no hitboxes).
     */
    public static <T extends Entity> AttackSnapshot<T> capture(
            Level level,
            Vec3 center,
            float radius,
            Class<T> entityClass,
            Predicate<T> filter) {

        // Use a conservative AABB only for the initial broad-phase query,
        // then confirm with an exact distance check so the effective zone is a true circle.
        double r = radius;
        List<T> targets = level.getEntitiesOfClass(
                entityClass,
                new net.minecraft.world.phys.AABB(
                        center.x - r, center.y - r, center.z - r,
                        center.x + r, center.y + r, center.z + r),
                entity -> filter.test(entity)
                        && entity.position().distanceTo(center) <= radius);

        return new AttackSnapshot<>(center, radius, targets);
    }

    public Vec3    center()  { return center;  }
    public float   radius()  { return radius;  }
    public List<T> targets() { return targets; }
}
