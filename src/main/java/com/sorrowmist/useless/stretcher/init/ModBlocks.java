package com.sorrowmist.useless.stretcher.init;

import com.sorrowmist.useless.stretcher.UselessStretcherMod;

import com.sorrowmist.useless.stretcher.content.block.OmniversalMyriadBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS = com.sorrowmist.uselesscore.api.registry.CoreRegistries.blocks(UselessStretcherMod.MODID);

    public static final RegistryObject<Block> OMNIVERSAL_MYRIAD = BLOCKS.register("omniversal_myriad",
            () -> new OmniversalMyriadBlock(BlockBehaviour.Properties.of()
                    .strength(0.8f)
                    .noOcclusion()));

    private ModBlocks() {
    }
}
