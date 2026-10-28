package com.sorrowmist.useless.stretcher;

import com.sorrowmist.useless.stretcher.client.StretcherConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Registers the Stretcher config screen on Forge's client setup event. */
@Mod.EventBusSubscriber(modid = UselessStretcherMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class UselessStretcherClient {
    private UselessStretcherClient() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) ->
                        com.sorrowmist.useless.client.gui.UselessConfigScreen.forSpec(parent,
                                net.minecraft.network.chat.Component.translatable("useless_stretcher.configuration.server"),
                                com.sorrowmist.useless.stretcher.config.StretcherConfig.SERVER_SPEC,
                                com.sorrowmist.useless.stretcher.config.ServerConfigSync::broadcast)));
    }
}
