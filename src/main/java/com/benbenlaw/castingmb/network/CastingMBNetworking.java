package com.benbenlaw.castingmb.network;

import com.benbenlaw.castingmb.CastingMB;
import com.benbenlaw.castingmb.network.packets.SelectMixerRecipePacket;
import com.benbenlaw.castingmb.network.packets.SyncFuelTanks;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CastingMBNetworking {

    public static void registerNetworking(final RegisterPayloadHandlersEvent event) {

        final PayloadRegistrar registrar = event.registrar(CastingMB.MOD_ID);

        registrar.playToClient(SyncFuelTanks.TYPE, SyncFuelTanks.STREAM_CODEC, SyncFuelTanks.HANDLER);

        registrar.playToServer(SelectMixerRecipePacket.TYPE, SelectMixerRecipePacket.STREAM_CODEC, SelectMixerRecipePacket.HANDLER);
    }

}
