package com.sorrowmist.useless.stretcher.mixin;

import com.sorrowmist.useless.stretcher.content.ae.InstalledStretcherPatterns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The small furnace terminal inventory accepts stretcher items as pattern sources. */
@Mixin(targets = "com.sorrowmist.useless.content.blockentities.AdvancedAlloyFurnaceBlockEntity$2", remap = false)
public abstract class InstalledStretcherTerminalMixin {
    @Inject(method = "isItemValid", at = @At("HEAD"), cancellable = true, remap = false)
    private void uselessStretcher$accept(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (InstalledStretcherPatterns.isStretcher(stack)) cir.setReturnValue(true);
    }
}