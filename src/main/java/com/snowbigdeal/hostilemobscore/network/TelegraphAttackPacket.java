package com.snowbigdeal.hostilemobscore.network;

import com.snowbigdeal.hostilemobscore.HostileMobsCore;
import com.snowbigdeal.hostilemobscore.attack.shape.TelegraphAttackShape;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Sent server → client when a telegraph attack windup begins.
 * Carries the full shape geometry so the client can render the correct VFX.
 */
public record TelegraphAttackPacket(TelegraphAttackShape shape, int lifetimeTicks)
        implements CustomPacketPayload {

    public static final Type<TelegraphAttackPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HostileMobsCore.MODID, "telegraph_attack"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TelegraphAttackPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        TelegraphAttackShape.writeFull(buf, pkt.shape());
                        buf.writeInt(pkt.lifetimeTicks());
                    },
                    buf -> {
                        byte type = buf.readByte();
                        TelegraphAttackShape shape = TelegraphAttackShape.read(type, buf);
                        int lifetime = buf.readInt();
                        return new TelegraphAttackPacket(shape, lifetime);
                    }
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
