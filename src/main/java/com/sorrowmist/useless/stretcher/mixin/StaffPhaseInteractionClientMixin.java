package com.sorrowmist.useless.stretcher.mixin;
import com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public abstract class StaffPhaseInteractionClientMixin {
 @Inject(method={"startUseItem","continueAttack"},at=@At("HEAD"),cancellable=true)
 private void blockUse(CallbackInfo ci){if(StaffPhaseManager.blocksInteraction(Minecraft.getInstance().player))ci.cancel();}
 @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
 private void blockAttack(CallbackInfoReturnable<Boolean> cir){if(StaffPhaseManager.blocksInteraction(Minecraft.getInstance().player))cir.setReturnValue(false);}
}
