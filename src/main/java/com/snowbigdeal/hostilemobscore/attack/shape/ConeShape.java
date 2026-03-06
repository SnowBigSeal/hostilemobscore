package com.snowbigdeal.hostilemobscore.attack.shape;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Sector (pizza-slice) AoE emanating from an apex.
 *
 * @param apex          Tip of the cone (usually the mob's position).
 * @param direction     Normalised horizontal direction the cone faces.
 * @param length        How far the cone extends from the apex.
 * @param halfAngleDeg  Half the opening angle in degrees (e.g. 45 = 90° total cone).
 */
public record ConeShape(Vec3 apex, Vec3 direction, float length, float halfAngleDeg)
        implements TelegraphAttackShape {

    /** Convenience ctor — normalises {@code direction} automatically. */
    public ConeShape(Vec3 apex, Vec3 direction, float length, float halfAngleDeg) {
        this.apex         = apex;
        this.direction    = direction.normalize();
        this.length       = length;
        this.halfAngleDeg = halfAngleDeg;
    }

    @Override
    public Vec3 center() { return apex; }

    @Override
    public boolean contains(Vec3 point) {
        Vec3 toPoint = new Vec3(point.x - apex.x, 0, point.z - apex.z);
        double dist = toPoint.length();
        if (dist > length) return false;
        if (dist < 1e-6) return true; // at apex
        double cosAngle = toPoint.normalize().dot(new Vec3(direction.x, 0, direction.z).normalize());
        double cosHalf  = Math.cos(Math.toRadians(halfAngleDeg));
        return cosAngle >= cosHalf;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(apex.x);      buf.writeDouble(apex.y);      buf.writeDouble(apex.z);
        buf.writeDouble(direction.x); buf.writeDouble(direction.y); buf.writeDouble(direction.z);
        buf.writeFloat(length);
        buf.writeFloat(halfAngleDeg);
    }

    static ConeShape readFields(FriendlyByteBuf buf) {
        Vec3  apex      = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        Vec3  dir       = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float length    = buf.readFloat();
        float halfAngle = buf.readFloat();
        return new ConeShape(apex, dir, length, halfAngle);
    }
}
