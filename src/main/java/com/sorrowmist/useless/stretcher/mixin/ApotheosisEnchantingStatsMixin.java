package com.sorrowmist.useless.stretcher.mixin;

import com.sorrowmist.useless.stretcher.config.StretcherConfig;
import com.sorrowmist.useless.stretcher.content.apotheosis.ApotheosisTableData;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds the selected table's saved virtual-bookshelf bonuses after Apothic Enchanting gathers
 * shelves. The gatherStats ABI moved between releases: the 1.5 line and 1.6.0 pass a
 * table-level int, while earlier and 1.6.1+ releases do not. Both descriptors are declared
 * with {@code require = 0}, so whichever signature the installed version actually has is
 * boosted and the missing one is skipped silently. No version-string detection is involved.
 */
@Pseudo
@Mixin(targets = "dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu", remap = false)
public abstract class ApotheosisEnchantingStatsMixin {
    /** Apothic Enchanting 1.5.x through 1.6.0 added the table-level int argument. */
    @Inject(method = "gatherStats(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantmentMenu$TableStats;",
            at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void uselessStretcher$addMarkedTableStats(Level level, BlockPos pos,
            int tableLevel, CallbackInfoReturnable<TableStats> callback) {
        uselessStretcher$boostMarkedTable(level, pos, callback);
    }

    private static void uselessStretcher$boostMarkedTable(Level level, BlockPos pos,
            CallbackInfoReturnable<TableStats> callback) {
        if (!StretcherConfig.enableApotheosisCompat() || !(level instanceof ServerLevel serverLevel)) return;
        ApotheosisTableData.Boost bonus = ApotheosisTableData.get(serverLevel, pos);
        if (bonus.equals(ApotheosisTableData.Boost.ZERO)) return;
        TableStats current = callback.getReturnValue();
        callback.setReturnValue(new TableStats(
                current.eterna() + bonus.eterna(), current.quanta() + bonus.quanta(),
                current.arcana() + bonus.arcana(), current.rectification(), current.clues() + bonus.clues(),
                current.blacklist(), current.treasure()));
    }

    @Inject(method = "gatherStats(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantmentMenu$TableStats;",
            at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private static void uselessStretcher$addMarkedTableStatsWithoutLevel(Level level, BlockPos pos,
            CallbackInfoReturnable<TableStats> callback) {
        uselessStretcher$boostMarkedTable(level, pos, callback);
    }
}
