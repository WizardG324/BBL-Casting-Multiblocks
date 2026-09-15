package com.benbenlaw.castingmb.network.packets;

import com.benbenlaw.casting.screen.SolidifierMenu;
import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.network.CastingMBNetworking;
import com.benbenlaw.castingmb.screen.MBSolidifierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record ChangeMoldPagePacket(BlockPos pos, int page) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ChangeMoldPagePacket> TYPE = new CustomPacketPayload.Type(CastingMB.identifier("change_mold_page"));
    public static final IPayloadHandler<ChangeMoldPagePacket> HANDLER = (packet, context) -> context.enqueueWork(() -> {
            AbstractContainerMenu patt0$temp = context.player().containerMenu;
            if (patt0$temp instanceof MBSolidifierMenu menu) {
                menu.setMoldPage(packet.page());
            }

        });
    public static final StreamCodec<RegistryFriendlyByteBuf, ChangeMoldPagePacket> STREAM_CODEC;

    public CustomPacketPayload.Type<ChangeMoldPagePacket> type() {
        return TYPE;
    }

    static {
        STREAM_CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ChangeMoldPagePacket::pos, ByteBufCodecs.INT, ChangeMoldPagePacket::page, ChangeMoldPagePacket::new);
    }
}