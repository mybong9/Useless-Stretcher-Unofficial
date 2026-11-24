package com.sorrowmist.useless.stretcher.mixin;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Pose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class StaffPhasePlayerRendererMixin {
    @Inject(method = "setModelProperties", at = @At("RETURN"))
    private void useFlightSneakPose(AbstractClientPlayer player, CallbackInfo ci) {
        if (player.getPose() == Pose.CROUCHING) {
            ((PlayerRenderer) (Object) this).getModel().crouching = true;
        }
    }
}
