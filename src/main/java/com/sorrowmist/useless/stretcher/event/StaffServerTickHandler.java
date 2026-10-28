package com.sorrowmist.useless.stretcher.event;

import com.sorrowmist.useless.stretcher.UselessStretcherMod;

import com.sorrowmist.useless.stretcher.content.entity.PlayerAccelerationManager;
import com.sorrowmist.useless.stretcher.content.entity.TimeStopManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;

@EventBusSubscriber(modid = UselessStretcherMod.MODID)
public final class StaffServerTickHandler {
    private StaffServerTickHandler() { }

    @SubscribeEvent
    public static void tick(ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager.tick(event.getServer());
        TimeStopManager.tick(event.getServer());
        PlayerAccelerationManager.tick(event.getServer(), TimeStopManager.remaining(event.getServer()) > 0);
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager.clear();
        // Weak maps release the server; this hook intentionally has no player-side cleanup.
    }
}
