package com.sorrowmist.useless.stretcher.dimension.init;

import com.sorrowmist.useless.stretcher.dimension.DimensionCompat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister<net.minecraft.world.item.Item> ITEMS = com.sorrowmist.uselesscore.api.registry.CoreRegistries.items(DimensionCompat.MODID);

    /** 方块物品，堆叠上限按规格 64（BlockItem 默认 stacksTo=64）。 */
    public static final RegistryObject<BlockItem> QUAD_CHUNK_DIMENSION_BLOCK_ITEM = ITEMS.register(
            "quad_chunk_dimension_block",
            () -> new BlockItem(ModBlocks.QUAD_CHUNK_DIMENSION_BLOCK.get(), new Item.Properties())
    );

    /** 九联区块维度传送方块物品，同样 64 堆叠。 */
    public static final RegistryObject<BlockItem> NINE_CHUNK_DIMENSION_BLOCK_ITEM = ITEMS.register(
            "nine_chunk_dimension_block",
            () -> new BlockItem(ModBlocks.NINE_CHUNK_DIMENSION_BLOCK.get(), new Item.Properties())
    );

    /** 四联区块奇数中心维度传送方块物品，64 堆叠。 */
    public static final RegistryObject<BlockItem> QUAD_CHUNK_ODD_DIMENSION_BLOCK_ITEM = ITEMS.register(
            "quad_chunk_odd_dimension_block",
            () -> new BlockItem(ModBlocks.QUAD_CHUNK_ODD_DIMENSION_BLOCK.get(), new Item.Properties())
    );

    /** 九联区块奇数中心维度传送方块物品，64 堆叠。 */
    public static final RegistryObject<BlockItem> NINE_CHUNK_ODD_DIMENSION_BLOCK_ITEM = ITEMS.register(
            "nine_chunk_odd_dimension_block",
            () -> new BlockItem(ModBlocks.NINE_CHUNK_ODD_DIMENSION_BLOCK.get(), new Item.Properties())
    );

    private ModItems() {
    }
}
