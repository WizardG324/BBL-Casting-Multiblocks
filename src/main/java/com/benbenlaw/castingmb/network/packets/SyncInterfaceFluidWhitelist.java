package com.benbenlaw.castingmb.network.packets;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.screen.MBInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

public record SyncInterfaceFluidWhitelist(BlockPos pos, boolean whitelist) implements CustomPacketPayload {

    public static final Type<SyncInterfaceFluidWhitelist> TYPE = new Type<>(CastingMB.identifier("sync_interface_fluid_whitelist"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncInterfaceFluidWhitelist> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SyncInterfaceFluidWhitelist::pos,
            ByteBufCodecs.BOOL, SyncInterfaceFluidWhitelist::whitelist,
            SyncInterfaceFluidWhitelist::new
    );

    public static final IPayloadHandler<SyncInterfaceFluidWhitelist> HANDLER = (packet, context) ->
            context.enqueueWork(() -> {
                if (context.player().containerMenu instanceof MBInterfaceMenu menu && menu.blockPos.equals(packet.pos())) {
                    menu.blockEntity.setFluidWhitelist(packet.whitelist());
                }
            });

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
