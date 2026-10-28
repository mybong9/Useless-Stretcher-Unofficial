package com.sorrowmist.useless.stretcher.client;

import com.sorrowmist.useless.stretcher.client.render.StaffLeafRewardRenderer;
import com.sorrowmist.useless.stretcher.client.render.TimeFlowRenderer;
import com.sorrowmist.useless.stretcher.client.render.WondrousStaffAccelerationRenderer;
import com.sorrowmist.useless.stretcher.init.ModEntities;
import com.sorrowmist.useless.stretcher.init.ModMenuTypes;
import com.sorrowmist.useless.stretcher.screen.OmniversalMyriadScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import com.sorrowmist.useless.stretcher.UselessStretcherMod;

@EventBusSubscriber(modid = UselessStretcherMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerScreens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(ModMenuTypes.OMNIVERSAL_MYRIAD.get(), OmniversalMyriadScreen::new));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.WONDROUS_STAFF_ACCELERATION.get(),
                WondrousStaffAccelerationRenderer::new);
        event.registerEntityRenderer(ModEntities.STAFF_LEAF_REWARD.get(), StaffLeafRewardRenderer::new);
        event.registerEntityRenderer(ModEntities.TIME_FLOW.get(), TimeFlowRenderer::new);
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(StretcherKeyBindings.WONDROUS_STAFF_MODE);
        event.register(StretcherKeyBindings.STANDALONE_TELEPORT);
        event.register(StretcherKeyBindings.STAFF_PHASE);
        event.register(StretcherKeyBindings.WONDROUS_STAFF_TIME_STOP);
    }
}
