package com.sorrowmist.useless.stretcher.mixin;

import com.sorrowmist.useless.stretcher.content.item.WondrousStaffLootRefresh;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableContainerLootMemoryMixin implements com.sorrowmist.useless.stretcher.content.item.LootTableSource {
    @org.spongepowered.asm.mixin.Shadow protected net.minecraft.resources.ResourceLocation lootTable;

    @Override
    public net.minecraft.resources.ResourceLocation uselessStretcher$getLootTable() { return lootTable; }
    @Inject(method = "unpackLootTable", at = @At("HEAD"))
    private void uselessStretcher$rememberBeforeOpening(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        WondrousStaffLootRefresh.remember((RandomizableContainerBlockEntity) (Object) this, lootTable);
    }
    @Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceLocation;J)V", at = @At("TAIL"))
    private void uselessStretcher$rememberLootTable(net.minecraft.resources.ResourceLocation lootTable, long seed, CallbackInfo ci) {
        WondrousStaffLootRefresh.remember((RandomizableContainerBlockEntity) (Object) this,
                lootTable);
    }
}
