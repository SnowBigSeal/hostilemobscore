package com.snowbigdeal.hostilemobscore.attack.shape;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/** Circular AoE centred on a ground position. */
public record CircleShape(Vec3 center, float radius) implements TelegraphAttackShape {

    @Override
    public boolean contains(Vec3 point) {
        double dx = point.x - center.x;
        double dz = point.z - center.z;
        return (dx * dx + dz * dz) <= (double)(radius * radius);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeDouble(center.x);
        buf.writeDouble(center.y);
        buf.writeDouble(center.z);
        buf.writeFloat(radius);
    }

    static CircleShape readFields(FriendlyByteBuf buf) {
        Vec3  center = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        float radius = buf.readFloat();
        return new CircleShape(center, radius);
    }
}
