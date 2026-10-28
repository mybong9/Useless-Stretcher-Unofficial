package com.sorrowmist.useless.stretcher.mixin;
import com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class StaffPhaseInteractionPacketMixin {
 @Inject(method={"handleUseItemOn","handleUseItem","handleInteract"},at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
 private void blockUse(CallbackInfo ci){if(StaffPhaseManager.blocksInteraction(((ServerGamePacketListenerImpl)(Object)this).player))ci.cancel();}
 @Inject(method="handlePlayerAction",at=@At(value="INVOKE",target="Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",shift=At.Shift.AFTER),cancellable=true)
 private void blockMining(ServerboundPlayerActionPacket packet,CallbackInfo ci){
  if(!StaffPhaseManager.blocksInteraction(((ServerGamePacketListenerImpl)(Object)this).player))return;
  switch(packet.getAction()){case START_DESTROY_BLOCK,STOP_DESTROY_BLOCK,ABORT_DESTROY_BLOCK -> ci.cancel();default -> {}}
 }
}
