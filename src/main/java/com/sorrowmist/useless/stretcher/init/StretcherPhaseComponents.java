package com.sorrowmist.useless.stretcher.init;
import com.mojang.serialization.Codec;
import com.sorrowmist.uselesscore.api.component.*;
import com.sorrowmist.uselesscore.api.network.codec.CoreStreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
public final class StretcherPhaseComponents {
    public static final CoreDataComponentHolder<Boolean> WONDROUS_STAFF_PHASE_ENABLED = flag("wondrous_staff_phase_enabled");
    public static final CoreDataComponentHolder<Boolean> WONDROUS_STAFF_PHASE_INTERACTION = flag("wondrous_staff_phase_interaction");
    public static final CoreDataComponentHolder<String> CAPSULE_STRUCTURE = new CoreDataComponentHolder<>(CoreDataComponentType.<String>builder(new ResourceLocation("useless_stretcher","capsule_structure")).persistent(Codec.STRING).build());
    public static final CoreDataComponentHolder<Integer> CAPSULE_ROTATION = new CoreDataComponentHolder<>(CoreDataComponentType.<Integer>builder(new ResourceLocation("useless_stretcher","capsule_rotation")).persistent(Codec.INT).build());
    private static CoreDataComponentHolder<Boolean> flag(String key) {
        return new CoreDataComponentHolder<>(CoreDataComponentType.<Boolean>builder(new ResourceLocation("useless_stretcher",key))
            .persistent(Codec.BOOL).networkSynchronized(CoreStreamCodec.of(FriendlyByteBuf::writeBoolean,FriendlyByteBuf::readBoolean)).build());
    }
}
