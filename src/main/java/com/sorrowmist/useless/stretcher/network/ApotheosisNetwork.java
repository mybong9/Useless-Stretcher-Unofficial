package com.sorrowmist.useless.stretcher.network;

import com.sorrowmist.useless.stretcher.UselessStretcherMod;

import com.sorrowmist.useless.stretcher.content.apotheosis.ApotheosisStaffSettings;
import com.sorrowmist.useless.stretcher.content.apotheosis.ApotheosisTableData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import com.sorrowmist.uselesscore.api.network.CoreCustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Small settings acknowledgments; bound-table positions are sent only on request or a new binding. */
public final class ApotheosisNetwork {
    public static final int MAX_MARKERS = 4096;
    private ApotheosisNetwork() { }

    public record RequestPayload(boolean offhand) implements CoreCustomPacketPayload {
        public static final Type<RequestPayload> TYPE = new Type<>(id("apotheosis_request"));
        public static final CoreStreamCodec<FriendlyByteBuf, RequestPayload> CODEC = CoreStreamCodec.of(
                (buf, value) -> buf.writeBoolean(value.offhand()), buf -> new RequestPayload(buf.readBoolean()));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record SettingsPayload(boolean offhand, boolean selecting,
                                  ApotheosisTableData.Boost boost) implements CoreCustomPacketPayload {
        public static final Type<SettingsPayload> TYPE = new Type<>(id("apotheosis_state"));
        public static final CoreStreamCodec<FriendlyByteBuf, SettingsPayload> CODEC = CoreStreamCodec.of(
                (buf, value) -> {
                    buf.writeBoolean(value.offhand());
                    buf.writeBoolean(value.selecting());
                    buf.writeVarInt(value.boost().eterna());
                    buf.writeVarInt(value.boost().quanta());
                    buf.writeVarInt(value.boost().arcana());
                    buf.writeVarInt(value.boost().clues());
                }, buf -> new SettingsPayload(buf.readBoolean(), buf.readBoolean(),
                        new ApotheosisTableData.Boost(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                                buf.readVarInt()).normalized()));
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public record MarkersPayload(ResourceLocation dimension, List<BlockPos> tables) implements CoreCustomPacketPayload {
        public static final Type<MarkersPayload> TYPE = new Type<>(id("apotheosis_markers"));
        public static final CoreStreamCodec<FriendlyByteBuf, MarkersPayload> CODEC = CoreStreamCodec.of(
                (buf, value) -> {
                    buf.writeResourceLocation(value.dimension());
                    int count = Math.min(MAX_MARKERS, value.tables().size());
                    buf.writeVarInt(count);
                    for (int i = 0; i < count; i++) buf.writeBlockPos(value.tables().get(i));
                }, buf -> {
                    ResourceLocation dimension = buf.readResourceLocation();
                    int count = buf.readVarInt();
                    if (count < 0 || count > MAX_MARKERS) throw new IllegalArgumentException("Invalid table marker count");
                    List<BlockPos> tables = new ArrayList<>(count);
                    for (int i = 0; i < count; i++) tables.add(buf.readBlockPos());
                    return new MarkersPayload(dimension, List.copyOf(tables));
                });
        @Override public Type<? extends CoreCustomPacketPayload> type() { return TYPE; }
    }

    public static void register() {
        UselessStretcherMod.NETWORK.registerToServer(RequestPayload.class, RequestPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            var hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            if (!(player.getItemInHand(hand).getItem() instanceof
                    com.sorrowmist.useless.stretcher.content.item.WondrousStaffItem)) return;
            sendSettings(player, hand);
            sendMarkers(player);
        }));
        UselessStretcherMod.NETWORK.registerToClient(SettingsPayload.class, SettingsPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
                com.sorrowmist.useless.stretcher.client.ApotheosisClientState.accept(payload)));
        UselessStretcherMod.NETWORK.registerToClient(MarkersPayload.class, MarkersPayload.CODEC, (payload, context) -> context.enqueueWork(() ->
                com.sorrowmist.useless.stretcher.client.ApotheosisClientState.accept(payload)));
    }

    public static void requestState(InteractionHand hand) {
        UselessStretcherMod.NETWORK.sendToServer(new RequestPayload(hand == InteractionHand.OFF_HAND));
    }

    public static void sendSettings(ServerPlayer player, InteractionHand hand) {
        var staff = player.getItemInHand(hand);
        UselessStretcherMod.NETWORK.sendToPlayer(player, new SettingsPayload(hand == InteractionHand.OFF_HAND,
                ApotheosisStaffSettings.selectionMode(staff),
                ApotheosisStaffSettings.boost(staff)));
    }

    public static void sendMarkers(ServerPlayer player) {
        ResourceLocation dimension = player.level().dimension().location();
        var tables = ApotheosisTableData.get(player.getServer()).ownedTables(player.getUUID(), dimension, MAX_MARKERS);
        UselessStretcherMod.NETWORK.sendToPlayer(player, new MarkersPayload(dimension, tables));
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(UselessStretcherMod.MODID, path);
    }
}
