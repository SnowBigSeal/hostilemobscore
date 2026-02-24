package com.snowbigdeal.hostilemobscore.network;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Sent server → client when a slam windup begins. The client manages all ring VFX locally. */
public record CircleAoePacket(Vec3 center, float radius, int lifetime) implements CustomPacketPayload {

    public static final Type<CircleAoePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "slam_aoe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CircleAoePacket> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeDouble(pkt.center().x);
                buf.writeDouble(pkt.center().y);
                buf.writeDouble(pkt.center().z);
                buf.writeFloat(pkt.radius());
                buf.writeInt(pkt.lifetime());
            },
            buf -> new CircleAoePacket(
                    new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                    buf.readFloat(),
                    buf.readInt()
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
