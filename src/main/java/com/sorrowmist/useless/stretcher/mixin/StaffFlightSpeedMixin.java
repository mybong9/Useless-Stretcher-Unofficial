package com.sorrowmist.useless.stretcher.mixin;
import com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class StaffFlightSpeedMixin {
    @Inject(method="getFlyingSpeed",at=@At("RETURN"),cancellable=true)
    private void staffSpeed(CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(StaffUtilitySettings.flightSpeed((Player)(Object)this,cir.getReturnValue()));
    }
}
