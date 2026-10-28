package com.sorrowmist.useless.stretcher.network;
import net.minecraft.client.Minecraft;
public final class Port160Client {
 private static final java.util.List<String> capsuleEntries = new java.util.ArrayList<>();
 private static int capsuleNextPart, capsuleParts;
 private static boolean capsuleOffhand, staffPhaseEnabled, staffPhaseInteraction;
public static void handleCapsuleReady(CapsuleNetwork.Complete payload) {
        com.sorrowmist.useless.stretcher.client.CapsuleReadyToast.show(payload.id(), payload.dimension(), payload.instanceId());
    }
public static void handleStaffPhase(Port160Network.StaffPhaseStatePayload payload) {
        staffPhaseEnabled = payload.phaseEnabled();
        staffPhaseInteraction = payload.interactionEnabled();
        applyStaffPhase();
    }
public static void tickStaffPhase() {
        if (Minecraft.getInstance().level == null) {
            staffPhaseEnabled = false;
            staffPhaseInteraction = true;
            com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager.clientTick(null);
            return;
        }
        applyStaffPhase();
    }
public static boolean staffPhaseInteractionEnabled() {
        return staffPhaseInteraction;
    }
private static void applyStaffPhase() {
        // Movement owns collision/flight for the entire vanilla tick. Reapplying a
        // second snapshot here could capture our own true flag and restore it on disable.
        var player = Minecraft.getInstance().player;
        com.sorrowmist.useless.stretcher.content.entity.StaffPhaseManager.clientTick(player);
    }
public static void handleDimensionCapsule(Port160Network.DimensionCapsuleCatalogPayload payload) {
        if (payload.part() == 0) {
            capsuleEntries.clear();
            capsuleNextPart = 0;
            capsuleParts = payload.parts();
            capsuleOffhand = payload.offhand();
        }
        if (payload.part() != capsuleNextPart || payload.parts() != capsuleParts
                || payload.offhand() != capsuleOffhand) return;
        capsuleEntries.addAll(payload.entries());
        capsuleNextPart++;
        if (capsuleNextPart != capsuleParts) return;
        Minecraft.getInstance().setScreen(new com.sorrowmist.useless.stretcher.client.DimensionCapsuleScreen(
                java.util.List.copyOf(capsuleEntries), payload.offhand()));
        capsuleEntries.clear();
    }
public static void handleNativeCapsule(CapsuleNetwork.State payload) {
        if (Minecraft.getInstance().screen instanceof com.sorrowmist.useless.stretcher.client.DimensionCapsuleScreen screen)
            screen.accept(payload);
    }
}
