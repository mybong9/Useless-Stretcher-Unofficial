package com.sorrowmist.useless.stretcher;

import com.sorrowmist.useless.stretcher.config.StretcherConfig;
import com.sorrowmist.useless.stretcher.config.ServerConfigSync;
import com.sorrowmist.useless.stretcher.dimension.DimensionCompat;
import com.sorrowmist.useless.stretcher.init.ModBlockEntities;
import com.sorrowmist.useless.stretcher.init.ModBlocks;
import com.sorrowmist.useless.stretcher.init.ModCreativeTabs;
import com.sorrowmist.useless.stretcher.init.ModEntities;
import com.sorrowmist.useless.stretcher.init.ModItems;
import com.sorrowmist.useless.stretcher.init.ModMenuTypes;
import com.sorrowmist.uselesscore.api.component.UselessStretcherComponents;
import com.sorrowmist.useless.stretcher.network.Network;
import com.sorrowmist.uselesscore.api.network.CoreNetworkChannel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.MinecraftForge;

@Mod(UselessStretcherMod.MODID)
public final class UselessStretcherMod {
    public static final String MODID = "useless_stretcher";
    public static final CoreNetworkChannel NETWORK = new CoreNetworkChannel(new ResourceLocation(MODID, "network"), "4");

    public UselessStretcherMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModMenuTypes.MENUS.register(modBus);
        ModCreativeTabs.CREATIVE_TAB.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        com.sorrowmist.uselesscore.api.recipe.CoreRecipeHooks.registerMoldMatcher((required,actual) -> {
            if(actual==null || actual.isEmpty() || !actual.is(ModItems.OMNIVERSAL_MYRIAD.get()))return null;
            return required==null || required.isEmpty() || !com.sorrowmist.useless.stretcher.content.mold.MoldMatch.matchingIds(
                required,com.sorrowmist.useless.stretcher.content.mold.MyriadMoldData.readEnabledMolds(actual)).isEmpty();
        });
        Network.register();
        com.sorrowmist.useless.stretcher.network.Port160Network.register();
        com.sorrowmist.useless.stretcher.network.StaffUtilityNetwork.register();
        com.sorrowmist.useless.stretcher.network.CapsuleNetwork.register();
        com.sorrowmist.useless.stretcher.content.capsule.CapsuleDimensions.init(modBus);
        com.sorrowmist.useless.stretcher.network.RangeNetwork.register();
        com.sorrowmist.useless.stretcher.network.ApotheosisNetwork.register();
        UselessStretcherComponents.init(modBus);
        DimensionCompat.init(modBus);
        
        // All gameplay switches live in the server config. NeoForge synchronizes this
        // spec to every client connected to the world, so the host/server setting is authoritative.
        ServerConfigSync.migrateLegacy(FMLPaths.CONFIGDIR.get(), FMLPaths.GAMEDIR.get().resolve("defaultconfigs"));
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, StretcherConfig.SERVER_SPEC);
        modBus.addListener(ServerConfigSync::onReload);
        MinecraftForge.EVENT_BUS.addListener(ServerConfigSync::onLogin);
    }
}
