package com.snowbigdeal.hostilemobscore.attack.shape;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Rectangular AoE extending along a direction axis.
 *
 * @param origin    Start of the line (typically the mob's position).
 * @param direction Normalised horizontal direction.
 * @param length    How far the rectangle extends from origin.
 * @param width     Full width of the rectangle (half-width on each side).
 */
public record LineShape(Vec3 origin, Vec3 direction, float length, float width)
        implements TelegraphAttackShape {

    public LineShape(Vec3 origin, Vec3 direction, float length, float width) {
        this.origin    = origin;
        this.direction = direction.normalize();
        this.length    = length;
        this.width     = width;
    }

    @Override
    public Vec3 center() { return origin; }

    @Override
    public boolean contains(Vec3 point) {
        Vec3 toPoint = new Vec3(point.x - origin.x, 0, point.z - origin.z);
        // Project onto the direction axis
        double along   = toPoint.dot(new Vec3(direction.x, 0, direction.z));
        if (along < 0 || along > length) return false;
        // Lateral distance (perpendicular component)
        Vec3   lateral     = toPoint.subtract(new Vec3(direction.x, 0, direction.z).scale(along));
        double lateralDist = lateral.length();
        return lateralDist <= (width * 0.5);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(origin.x);    buf.writeDouble(origin.y);    buf.writeDouble(origin.z);
        buf.writeDouble(direction.x); buf.writeDouble(direction.y); buf.writeDouble(direction.z);
        buf.writeFloat(length);
        buf.writeFloat(width);
    }

    static LineShape readFields(FriendlyByteBuf buf) {
        Vec3  origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3  dir    = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float length = buf.readFloat();
        float width  = buf.readFloat();
        return new LineShape(origin, dir, length, width);
    }
}
