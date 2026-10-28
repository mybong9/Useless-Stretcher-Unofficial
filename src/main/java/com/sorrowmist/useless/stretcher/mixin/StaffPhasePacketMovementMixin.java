package com.sorrowmist.useless.stretcher.mixin;

import com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class StaffPhasePacketMovementMixin {
    @org.spongepowered.asm.mixin.injection.ModifyConstant(method="handleMovePlayer",constant={
            @org.spongepowered.asm.mixin.injection.Constant(floatValue=100.0F),
            @org.spongepowered.asm.mixin.injection.Constant(floatValue=300.0F)})
    private float allowConfiguredFlightSpeed(float original) {
        var player=((ServerGamePacketListenerImpl)(Object)this).player;
        if (!player.getAbilities().flying || player.isPassenger()) return original;
        var staff=com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings.carried(player);
        if (staff.isEmpty()) return original;
        double speed=com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings.speed(staff);
        return (float)(original*Math.max(1,speed*speed));
    }

    @Inject(method = "handleMovePlayer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER))
    private void permitPhaseMovement(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        StaffPhaseManager.movementTick(((ServerGamePacketListenerImpl) (Object) this).player);
    }
}
