package com.sorrowmist.useless.stretcher.mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LocalPlayer.class)
public abstract class StaffFlightClientMixin {
    @Redirect(method="aiStep",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float verticalSpeed(net.minecraft.world.entity.player.Abilities abilities) { return StaffUtilitySettings.flightSpeed((LocalPlayer)(Object)this,abilities.getFlyingSpeed()); }
    @Inject(method="aiStep",at=@At("TAIL"))
    private void brakeFlight(CallbackInfo ci) {
        LocalPlayer player=(LocalPlayer)(Object)this;
        if (!player.getAbilities().flying || player.isPassenger()) return;
        var staff=StaffUtilitySettings.carried(player);
        if (staff.isEmpty()) return;
        double inertia=StaffUtilitySettings.inertia(staff);
        if (inertia==1) return;
        var velocity=player.getDeltaMovement();
        boolean horizontal=player.input.leftImpulse==0 && player.input.forwardImpulse==0;
        boolean vertical=!player.input.jumping && !player.input.shiftKeyDown;
        player.setDeltaMovement(horizontal?velocity.x*inertia:velocity.x,
                vertical?velocity.y*inertia:velocity.y,horizontal?velocity.z*inertia:velocity.z);
    }
}
