package com.sorrowmist.useless.stretcher.network;

import com.sorrowmist.useless.stretcher.UselessStretcherMod;
import com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings;
import com.sorrowmist.useless.stretcher.init.*;
import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

public final class StaffUtilityNetwork {
    public record Settings(boolean offhand,boolean dismantle,double speed,double inertia) implements CoreCustomPacketPayload {
        public static final Type<Settings> TYPE=new Type<>(new ResourceLocation("useless_stretcher","staff_utility_settings"));
        public static final CoreStreamCodec<FriendlyByteBuf,Settings> CODEC=CoreStreamCodec.of(
                (b,p)->{b.writeBoolean(p.offhand);b.writeBoolean(p.dismantle);b.writeDouble(p.speed);b.writeDouble(p.inertia);},
                b->new Settings(b.readBoolean(),b.readBoolean(),b.readDouble(),b.readDouble()));
        public Type<? extends CoreCustomPacketPayload> type(){return TYPE;}
    }
    public record TogglePhase() implements CoreCustomPacketPayload {
        public static final Type<TogglePhase> TYPE=new Type<>(new ResourceLocation("useless_stretcher","toggle_staff_phase"));
        public static final CoreStreamCodec<FriendlyByteBuf,TogglePhase> CODEC=CoreStreamCodec.of((b,p)->{},b->new TogglePhase());
        public Type<? extends CoreCustomPacketPayload> type(){return TYPE;}
    }
    public static void register() {
        var channel=UselessStretcherMod.NETWORK;
        channel.registerToServer(Settings.class,Settings.CODEC,(p,c)->c.enqueueWork(()->{
            if (!(c.player() instanceof ServerPlayer player) || !Double.isFinite(p.speed) || !Double.isFinite(p.inertia)) return;
            var stack=player.getItemInHand(p.offhand?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND);
            if (!stack.is(ModItems.WONDROUS_STAFF.get())) return;
            StaffUtilitySettings.setDismantle(stack,p.dismantle);
            StaffUtilitySettings.flight(stack,p.speed,p.inertia);
            player.getInventory().setChanged();player.containerMenu.broadcastChanges();
        }));
        channel.registerToServer(TogglePhase.class,TogglePhase.CODEC,(p,c)->c.enqueueWork(()->{
            if (!(c.player() instanceof ServerPlayer player)) return;
            var staff=StaffUtilitySettings.carried(player);
            if (staff.isEmpty()) return;
            boolean enabled=com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager.state(player).phase();
            if (enabled) {
                for (var stack:player.getInventory().items) if (stack.is(ModItems.WONDROUS_STAFF.get()))
                    CoreItemComponents.set(stack,StretcherPhaseComponents.WONDROUS_STAFF_PHASE_ENABLED.get(),false);
                if (player.getOffhandItem().is(ModItems.WONDROUS_STAFF.get()))
                    CoreItemComponents.set(player.getOffhandItem(),StretcherPhaseComponents.WONDROUS_STAFF_PHASE_ENABLED.get(),false);
            } else CoreItemComponents.set(staff,StretcherPhaseComponents.WONDROUS_STAFF_PHASE_ENABLED.get(),true);
            player.getInventory().setChanged();player.containerMenu.broadcastChanges();
            com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager.apply(player);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gui.useless_stretcher.phase.enabled")
                    .append(": ").append(net.minecraft.network.chat.Component.translatable(enabled?"options.off":"options.on")),true);
        }));
    }
}
