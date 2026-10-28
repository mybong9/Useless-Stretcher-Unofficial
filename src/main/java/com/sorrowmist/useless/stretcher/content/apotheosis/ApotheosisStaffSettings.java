package com.sorrowmist.useless.stretcher.content.apotheosis;

import com.sorrowmist.useless.stretcher.UselessStretcherMod;



import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.mojang.logging.LogUtils;
import com.sorrowmist.uselesscore.api.component.UselessStretcherComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraftforge.fml.ModList;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;

/** Optional Apotheosis bridge. Reflection keeps Apotheosis and Apothic Enchanting runtime-optional. */
public final class ApotheosisStaffSettings {

    private ApotheosisStaffSettings() { }

    public static boolean isAvailable() {
        return isApotheosisLoaded() || isEnchantingAvailable();
    }

    /** The table boost only needs Apothic Enchanting, which is a standalone mod. */
    public static boolean isEnchantingAvailable() {
        return ModList.get().isLoaded("apotheosis");
    }

    public static boolean isApotheosisLoaded() {
        return ModList.get().isLoaded("apotheosis");
    }

    public static ApotheosisTableData.Boost boost(ItemStack stack) {
        return new ApotheosisTableData.Boost(
                CoreItemComponents.getOrDefault(stack, UselessStretcherComponents.APOTHEOSIS_ETERNA.get(), 0),
                CoreItemComponents.getOrDefault(stack, UselessStretcherComponents.APOTHEOSIS_QUANTA.get(), 0),
                CoreItemComponents.getOrDefault(stack, UselessStretcherComponents.APOTHEOSIS_ARCANA.get(), 0),
                CoreItemComponents.getOrDefault(stack, UselessStretcherComponents.APOTHEOSIS_CLUES.get(), 0)).normalized();
    }

    public static void setBoost(ItemStack stack, ApotheosisTableData.Boost boost) {
        ApotheosisTableData.Boost value = boost.normalized();
        CoreItemComponents.set(stack, UselessStretcherComponents.APOTHEOSIS_ETERNA.get(), value.eterna());
        CoreItemComponents.set(stack, UselessStretcherComponents.APOTHEOSIS_QUANTA.get(), value.quanta());
        CoreItemComponents.set(stack, UselessStretcherComponents.APOTHEOSIS_ARCANA.get(), value.arcana());
        CoreItemComponents.set(stack, UselessStretcherComponents.APOTHEOSIS_CLUES.get(), value.clues());
    }

    public static boolean selectionMode(ItemStack stack) {
        return CoreItemComponents.getOrDefault(stack, UselessStretcherComponents.APOTHEOSIS_TABLE_SELECTION.get(), false);
    }

    public static void setSelectionMode(ItemStack stack, boolean enabled) {
        CoreItemComponents.set(stack, UselessStretcherComponents.APOTHEOSIS_TABLE_SELECTION.get(), enabled);
    }

    public static boolean isTable(BlockState state) {
        return state.getBlock() instanceof EnchantmentTableBlock;
    }

    public static boolean markTable(ServerPlayer player, net.minecraft.world.InteractionHand hand,
                                    net.minecraft.core.BlockPos pos) {
        ItemStack held = player.getItemInHand(hand);
        if (!com.sorrowmist.useless.stretcher.config.StretcherConfig.enableApotheosisCompat()
                || !isEnchantingAvailable()
                || !(held.getItem() instanceof com.sorrowmist.useless.stretcher.content.item.WondrousStaffItem)
                || !selectionMode(held) || !player.level().hasChunkAt(pos)
                || player.distanceToSqr(pos.getCenter()) > 64.0D || !isTable(player.level().getBlockState(pos))) return false;
        var data = ApotheosisTableData.get(player.getServer());
        var dimension = player.level().dimension().location();
        var previousOwner = data.owner(dimension, pos);
        boolean changed = data.set(player.getUUID(), dimension, pos, boost(held));
        if (changed) ApotheosisTableData.refreshOpenMenus(player.getServer());
        if (changed || !player.getUUID().equals(previousOwner)) {
            com.sorrowmist.useless.stretcher.network.ApotheosisNetwork.sendMarkers(player);
            if (previousOwner != null && !previousOwner.equals(player.getUUID())) {
                var previousPlayer = player.getServer().getPlayerList().getPlayer(previousOwner);
                if (previousPlayer != null) com.sorrowmist.useless.stretcher.network.ApotheosisNetwork.sendMarkers(previousPlayer);
            }
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                "msg.useless_stretcher.apotheosis.table_set"), true);
        return true;
    }

}
