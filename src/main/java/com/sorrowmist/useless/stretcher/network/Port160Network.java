package com.sorrowmist.useless.stretcher.network;
import com.sorrowmist.useless.stretcher.UselessStretcherMod;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
public final class Port160Network {
 public record DimensionCapsuleCatalogPayload(List<String> entries,boolean offhand,int part,int parts) implements CoreCustomPacketPayload {
  public static final Type<DimensionCapsuleCatalogPayload> TYPE=new Type<>(new ResourceLocation("useless_stretcher","dimension_capsule_catalog"));
  public static final CoreStreamCodec<FriendlyByteBuf,DimensionCapsuleCatalogPayload> CODEC=CoreStreamCodec.of((b,p)->{
   b.writeCollection(p.entries,(buf,s)->buf.writeUtf(s,4096));b.writeBoolean(p.offhand);b.writeVarInt(p.part);b.writeVarInt(p.parts);
  },b->{int size=b.readVarInt();if(size<0||size>128)throw new IllegalArgumentException("Capsule catalogue size");
   java.util.List<String> entries=new java.util.ArrayList<>(size);for(int i=0;i<size;i++)entries.add(b.readUtf(4096));
   return new DimensionCapsuleCatalogPayload(List.copyOf(entries),b.readBoolean(),b.readVarInt(),b.readVarInt());});
  public Type<? extends CoreCustomPacketPayload> type(){return TYPE;}
 }
 public record StaffPhaseStatePayload(boolean phaseEnabled,boolean interactionEnabled) implements CoreCustomPacketPayload {
  public static final Type<StaffPhaseStatePayload> TYPE=new Type<>(new ResourceLocation("useless_stretcher","staff_phase_state"));
  public static final CoreStreamCodec<FriendlyByteBuf,StaffPhaseStatePayload> CODEC=CoreStreamCodec.of((b,p)->{b.writeBoolean(p.phaseEnabled);b.writeBoolean(p.interactionEnabled);},b->new StaffPhaseStatePayload(b.readBoolean(),b.readBoolean()));
  public Type<? extends CoreCustomPacketPayload> type(){return TYPE;}
 }
 public record PhaseSettings(boolean phase,boolean interaction,boolean offhand) implements CoreCustomPacketPayload {
  public static final Type<PhaseSettings> TYPE=new Type<>(new ResourceLocation("useless_stretcher","staff_phase_settings"));
  public static final CoreStreamCodec<FriendlyByteBuf,PhaseSettings> CODEC=CoreStreamCodec.of((b,p)->{b.writeBoolean(p.phase);b.writeBoolean(p.interaction);b.writeBoolean(p.offhand);},b->new PhaseSettings(b.readBoolean(),b.readBoolean(),b.readBoolean()));
  public Type<? extends CoreCustomPacketPayload> type(){return TYPE;}
 }
 public record Teleport() implements CoreCustomPacketPayload {
  public static final Type<Teleport> TYPE=new Type<>(new ResourceLocation("useless_stretcher","standalone_teleport"));
  public static final CoreStreamCodec<FriendlyByteBuf,Teleport> CODEC=CoreStreamCodec.of((b,p)->{},b->new Teleport());
  public Type<? extends CoreCustomPacketPayload> type(){return TYPE;}
 }
 public static void register(){
  var ch=UselessStretcherMod.NETWORK;
  ch.registerToClient(DimensionCapsuleCatalogPayload.class,DimensionCapsuleCatalogPayload.CODEC,(p,c)->c.enqueueWork(()->Port160Client.handleDimensionCapsule(p)));
  ch.registerToClient(StaffPhaseStatePayload.class,StaffPhaseStatePayload.CODEC,(p,c)->c.enqueueWork(()->Port160Client.handleStaffPhase(p)));
  ch.registerToServer(Teleport.class,Teleport.CODEC,(p,c)->c.enqueueWork(()->{if(c.player() instanceof ServerPlayer player)com.sorrowmist.useless.stretcher.content.travel.StandaloneTeleport.activate(player);}));
  ch.registerToServer(PhaseSettings.class,PhaseSettings.CODEC,(p,c)->c.enqueueWork(()->{
   if(!(c.player() instanceof ServerPlayer player))return;
   var stack=player.getItemInHand(p.offhand?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND);
   if(!stack.is(com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get()))return;
   com.sorrowmist.uselesscore.api.component.CoreItemComponents.set(stack,com.sorrowmist.useless.stretcher.init.StretcherPhaseComponents.WONDROUS_STAFF_PHASE_ENABLED.get(),p.phase);
   com.sorrowmist.uselesscore.api.component.CoreItemComponents.set(stack,com.sorrowmist.useless.stretcher.init.StretcherPhaseComponents.WONDROUS_STAFF_PHASE_INTERACTION.get(),p.interaction);
   player.getInventory().setChanged();player.containerMenu.broadcastChanges();
   com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager.apply(player);
  }));
 }
 public static void sendStaffPhaseState(ServerPlayer p,boolean phase,boolean interaction){UselessStretcherMod.NETWORK.sendToPlayer(p,new StaffPhaseStatePayload(phase,interaction));}
 public static void sendDimensionCapsuleCatalog(ServerPlayer player,boolean offhand){
  var entries=com.sorrowmist.useless.stretcher.content.capsule.DimensionCapsuleCatalog.scan(player).stream().map(com.sorrowmist.useless.stretcher.content.capsule.DimensionCapsuleCatalog.Entry::wire).toList();
  int parts=Math.max(1,(entries.size()+127)/128);
  for(int part=0;part<parts;part++)UselessStretcherMod.NETWORK.sendToPlayer(player,new DimensionCapsuleCatalogPayload(entries.subList(part*128,Math.min(entries.size(),(part+1)*128)),offhand,part,parts));
 }
 public static void sendStandaloneTeleport(){UselessStretcherMod.NETWORK.sendToServer(new Teleport());}
}
