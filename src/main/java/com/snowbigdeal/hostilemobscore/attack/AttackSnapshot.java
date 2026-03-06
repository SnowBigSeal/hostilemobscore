package com.snowbigdeal.hostilemobscore.attack;

import com.snowbigdeal.hostilemobscore.attack.shape.CircleShape;
import com.snowbigdeal.hostilemobscore.attack.shape.ConeShape;
import com.snowbigdeal.hostilemobscore.attack.shape.LineShape;
import com.snowbigdeal.hostilemobscore.attack.shape.TelegraphAttackShape;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Predicate;

/**
 * Immutable snapshot of entities captured within a shape at a specific point in time.
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
     * Captures entities within a {@link TelegraphAttackShape}.
     * Uses AABB broad-phase then {@link TelegraphAttackShape#contains} for precise hit detection.
     */
    public static <T extends Entity> AttackSnapshot<T> capture(
            Level level,
            TelegraphAttackShape shape,
            Class<T> entityClass,
            Predicate<T> filter) {

        Vec3   center = shape.center();
        double bound  = boundingRadius(shape);
        List<T> targets = level.getEntitiesOfClass(
                entityClass,
                new AABB(center.x - bound, center.y - bound, center.z - bound,
                         center.x + bound, center.y + bound, center.z + bound),
                entity -> filter.test(entity) && shape.contains(entity.position()));

        return new AttackSnapshot<>(center, (float) bound, targets);
    }

    /**
     * Legacy overload using a simple circle radius check.
     * Prefer the {@link TelegraphAttackShape} overload for new attacks.
     */
    public static <T extends Entity> AttackSnapshot<T> capture(
            Level level,
            Vec3 center,
            float radius,
            Class<T> entityClass,
            Predicate<T> filter) {

        double r = radius;
        List<T> targets = level.getEntitiesOfClass(
                entityClass,
                new AABB(center.x - r, center.y - r, center.z - r,
                         center.x + r, center.y + r, center.z + r),
                entity -> filter.test(entity)
                        && entity.position().distanceTo(center) <= radius);

        return new AttackSnapshot<>(center, radius, targets);
    }

    /** Conservative bounding radius that fully contains the shape for AABB broad-phase. */
    private static double boundingRadius(TelegraphAttackShape shape) {
        return switch (shape) {
            case CircleShape c -> c.radius();
            case ConeShape   c -> c.length();
            case LineShape   l -> l.length() + l.width() * 0.5;
        };
    }

    public Vec3    center()  { return center;  }
    public float   radius()  { return radius;  }
    public List<T> targets() { return targets; }
}

