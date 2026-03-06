package com.snowbigdeal.hostilemobscore.attack.shape;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

/**
 * Sealed geometry contract for all telegraph attack shapes.
 * <p>
 * Each shape knows how to:
 * <ul>
 *   <li>Test whether a world position falls inside it ({@link #contains}).</li>
 *   <li>Report its visual centre for VFX anchoring ({@link #center}).</li>
 *   <li>Serialise / deserialise itself over the network.</li>
 * </ul>
 */
public sealed interface TelegraphAttackShape
        permits CircleShape, ConeShape, LineShape {

    /** Returns {@code true} if {@code point} lies within this shape. */
    boolean contains(Vec3 point);

    /** Visual centre used by the client VFX renderer. */
    Vec3 center();

    /** Write shape-specific fields to the buffer (type byte already written by caller). */
    void write(FriendlyByteBuf buf);

    // -------------------------------------------------------------------------
    // Type tags
    // -------------------------------------------------------------------------

    byte TYPE_CIRCLE = 0;
    byte TYPE_CONE   = 1;
    byte TYPE_LINE   = 2;

    /** Read and reconstruct a shape from the buffer (type byte already consumed by caller). */
    static TelegraphAttackShape read(byte type, FriendlyByteBuf buf) {
        return switch (type) {
            case TYPE_CIRCLE -> CircleShape.readFields(buf);
            case TYPE_CONE   -> ConeShape.readFields(buf);
            case TYPE_LINE   -> LineShape.readFields(buf);
            default -> throw new IllegalArgumentException("Unknown telegraph shape type: " + type);
        };
    }

    /** Write a shape including its type byte — use this on the send side. */
    static void writeFull(FriendlyByteBuf buf, TelegraphAttackShape shape) {
        buf.writeByte(switch (shape) {
            case CircleShape ignored -> TYPE_CIRCLE;
            case ConeShape   ignored -> TYPE_CONE;
            case LineShape   ignored -> TYPE_LINE;
        });
        shape.write(buf);
    }
}
