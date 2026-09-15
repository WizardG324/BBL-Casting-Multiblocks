package com.benbenlaw.castingmb.network.packets;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.screen.MBMixerMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public record SelectMixerRecipePacket(@Nullable Identifier recipeId) implements CustomPacketPayload {

    public static final Type<SelectMixerRecipePacket> TYPE = new Type<>(CastingMB.identifier("select_mixer_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectMixerRecipePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(Identifier.STREAM_CODEC), payload -> Optional.ofNullable(payload.recipeId()),
            idOpt -> new SelectMixerRecipePacket(idOpt.orElse(null))
    );

    public static final IPayloadHandler<SelectMixerRecipePacket> HANDLER = (packet, context) -> {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof MBMixerMenu menu) {
                menu.setSelectedRecipe(packet.recipeId());
            }
        });
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
