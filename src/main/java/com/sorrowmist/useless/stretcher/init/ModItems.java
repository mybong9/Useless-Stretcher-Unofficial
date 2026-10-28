package com.sorrowmist.useless.stretcher.init;

import com.sorrowmist.useless.stretcher.UselessStretcherMod;

import com.sorrowmist.useless.stretcher.content.item.UselessStretcherItem;
import com.sorrowmist.useless.stretcher.content.item.WondrousStaffItem;
import com.sorrowmist.useless.stretcher.content.item.RangeReclaimerItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = com.sorrowmist.uselesscore.api.registry.CoreRegistries.items(UselessStretcherMod.MODID);

    public static final RegistryObject<BlockItem> OMNIVERSAL_MYRIAD = ITEMS.register("omniversal_myriad",
            () -> new BlockItem(ModBlocks.OMNIVERSAL_MYRIAD.get(), new Item.Properties()));

    public static final RegistryObject<UselessStretcherItem> USELESS_STRETCHER = ITEMS.register("useless_stretcher",
            () -> new UselessStretcherItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<WondrousStaffItem> WONDROUS_STAFF = ITEMS.register("wondrous_staff",
            WondrousStaffItem::new);

    /** No recipe: intended for server operators and normally obtained with /give. */
    public static final RegistryObject<RangeReclaimerItem> RANGE_RECLAIMER = ITEMS.register("range_reclaimer",
            () -> new RangeReclaimerItem(new Item.Properties()));

    public static final RegistryObject<com.sorrowmist.useless.stretcher.content.item.DimensionCapsuleItem> DIMENSION_CAPSULE =
            ITEMS.register("dimension_capsule", () -> new com.sorrowmist.useless.stretcher.content.item.DimensionCapsuleItem(new Item.Properties()));
    private ModItems() {
    }
}
