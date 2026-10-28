package com.sorrowmist.useless.stretcher.content.mold;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.uselesscore.api.component.CoreItemStackData;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.sorrowmist.uselesscore.api.component.UselessStretcherComponents;
import com.sorrowmist.useless.stretcher.init.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.fml.util.thread.EffectiveSide;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Items carry references only. Legacy inline selections are read for automatic migration.
 */
public final class MyriadMoldData {
    private static final String KEY_ENABLED = "enabled_molds";
    private static final String KEY_PATTERN_REF = "pattern_ref";
    private static final String KEY_AE_REF = "ae_ref";
    private MyriadMoldData() {
    }

    public static Set<ResourceLocation> readEnabledMolds(ItemStack stack) {
        UUID ref = readMoldRef(stack);
        if (ref != null) {
            var server = ServerLifecycleHooks.getCurrentServer();
            return server != null && EffectiveSide.get().isServer()
                    ? MyriadMoldStore.get(server).resolve(ref) : Set.of();
        }
        net.minecraft.nbt.CompoundTag custom = CoreItemStackData.getCustomData(stack);
        if (custom.isEmpty()) return Set.of();
        ListTag list = custom.getList(KEY_ENABLED, Tag.TAG_STRING);
        Set<ResourceLocation> result = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) result.add(id);
        }
        return Set.copyOf(result);
    }

    public static void writeEnabledMolds(ItemStack stack, Collection<ResourceLocation> molds, ServerLevel level) {
        UUID ref = MyriadMoldStore.get(level.getServer()).snapshot(molds);
        if (ref == null) CoreItemComponents.remove(stack, UselessStretcherComponents.MYRIAD_MOLD_REF.get());
        else CoreItemComponents.set(stack, UselessStretcherComponents.MYRIAD_MOLD_REF.get(), ref.toString());
        if (ref == null) CoreItemComponents.remove(stack, UselessStretcherComponents.MYRIAD_MOLD_COUNT.get());
        else CoreItemComponents.set(stack, UselessStretcherComponents.MYRIAD_MOLD_COUNT.get(), molds.size());
        CoreItemStackData.updateCustomData(stack, tag -> tag.remove(KEY_ENABLED));
    }

    public static UUID readMoldRef(ItemStack stack) {
        String value = CoreItemComponents.get(stack, UselessStretcherComponents.MYRIAD_MOLD_REF.get());
        if (value == null) return null;
        try { return UUID.fromString(value); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public static boolean externalize(ServerLevel level, ItemStack stack) {
        if (!stack.is(ModItems.OMNIVERSAL_MYRIAD.get())) return false;
        if (!CoreItemStackData.getCustomData(stack).contains(KEY_ENABLED)) return false;
        writeEnabledMolds(stack, readEnabledMolds(stack), level);
        return true;
    }

    /** Cheap component-only check used by the global container migration guard. */
    public static boolean isLegacyInline(ItemStack stack) {
        if (!stack.is(ModItems.OMNIVERSAL_MYRIAD.get())) return false;
        net.minecraft.nbt.CompoundTag custom = CoreItemStackData.getCustomData(stack);
        return !custom.isEmpty() && custom.contains(KEY_ENABLED)
                && readMoldRef(stack) == null;
    }

    public static boolean hasEnabledMolds(ItemStack stack) {
        if (readMoldRef(stack) != null && EffectiveSide.get().isClient()) {
            return CoreItemComponents.getOrDefault(stack, UselessStretcherComponents.MYRIAD_MOLD_COUNT.get(), 0) > 0;
        }
        return !readEnabledMolds(stack).isEmpty();
    }

    public static boolean isMoldEnabled(ItemStack stack, ResourceLocation moldId) {
        return moldId != null && readEnabledMolds(stack).contains(moldId);
    }

    public static UUID readPatternRef(ItemStack stack) {
        net.minecraft.nbt.CompoundTag custom = CoreItemStackData.getCustomData(stack);
        if (custom.isEmpty()) return null;
        String value = custom.getString(KEY_PATTERN_REF);
        if (value.isEmpty()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void writePatternRef(ItemStack stack, UUID id) {
        CoreItemStackData.updateCustomData(stack, tag -> {
            if (id == null) tag.remove(KEY_PATTERN_REF);
            else tag.putString(KEY_PATTERN_REF, id.toString());
        });
    }

    public static UUID readAeRef(ItemStack stack) {
        net.minecraft.nbt.CompoundTag custom = CoreItemStackData.getCustomData(stack);
        if (custom.isEmpty()) return null;
        String value = custom.getString(KEY_AE_REF);
        if (value.isEmpty()) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void writeAeRef(ItemStack stack, UUID id) {
        CoreItemStackData.updateCustomData(stack, tag -> {
            if (id == null) tag.remove(KEY_AE_REF);
            else tag.putString(KEY_AE_REF, id.toString());
        });
    }
}
