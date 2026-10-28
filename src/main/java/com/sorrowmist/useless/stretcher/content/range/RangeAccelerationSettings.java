package com.sorrowmist.useless.stretcher.content.range;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.uselesscore.api.component.UselessStretcherComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Validated data-component access for the range placement settings stored on a staff. */
public final class RangeAccelerationSettings {
    public static final int DEFAULT_SIZE = 3;

    private RangeAccelerationSettings() {
    }

    public static boolean placementMode(ItemStack staff) {
        return CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_PLACEMENT_MODE.get(), false);
    }

    public static boolean filterMarkingMode(ItemStack staff) {
        return CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_FILTER_MARKING_MODE.get(), false);
    }

    public static boolean markSleepList(ItemStack staff) {
        return CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_MARK_SLEEP_LIST.get(), false);
    }

    public static int sizeX(ItemStack staff) {
        return RangeAccelerationSavedData.clampSize(
                CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_SIZE_X.get(), DEFAULT_SIZE));
    }

    public static int sizeY(ItemStack staff) {
        return RangeAccelerationSavedData.clampSize(
                CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_SIZE_Y.get(), DEFAULT_SIZE));
    }

    public static int sizeZ(ItemStack staff) {
        return RangeAccelerationSavedData.clampSize(
                CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_SIZE_Z.get(), DEFAULT_SIZE));
    }

    public static int offsetX(ItemStack staff) {
        return RangeAccelerationSavedData.clampOffset(
                CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_OFFSET_X.get(), 0));
    }

    public static int offsetY(ItemStack staff) {
        return RangeAccelerationSavedData.clampOffset(
                CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_OFFSET_Y.get(), 0));
    }

    public static int offsetZ(ItemStack staff) {
        return RangeAccelerationSavedData.clampOffset(
                CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_OFFSET_Z.get(), 0));
    }

    public static boolean idleThrottleDisabled(ItemStack staff) {
        return CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_IDLE_THROTTLE_DISABLED.get(), false);
    }

    /** Empty sleep blacklist = every target may sleep; empty sleep whitelist = no target may sleep. */
    public static boolean sleepWhitelistMode(ItemStack staff) {
        return idleThrottleDisabled(staff);
    }

    public static boolean whitelistMode(ItemStack staff) {
        return CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_WHITELIST_MODE.get(), false);
    }

    public static int accelerationMarkCount(ItemStack staff) {
        return Math.max(0, CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_ACCELERATION_MARK_COUNT.get(), 0));
    }

    public static int sleepMarkCount(ItemStack staff) {
        return Math.max(0, CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_SLEEP_MARK_COUNT.get(), 0));
    }

    public static void setMarkCounts(ItemStack staff, int accelerationCount, int sleepCount) {
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_ACCELERATION_MARK_COUNT.get(), Math.max(0, accelerationCount));
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_SLEEP_MARK_COUNT.get(), Math.max(0, sleepCount));
    }

    public static List<ResourceLocation> filters(ItemStack staff) {
        List<ResourceLocation> values = CoreItemComponents.getOrDefault(staff, UselessStretcherComponents.RANGE_FILTERS.get(), List.of());
        if (values.size() <= RangeAccelerationSavedData.MAX_FILTERS) return List.copyOf(values);
        return List.copyOf(values.subList(0, RangeAccelerationSavedData.MAX_FILTERS));
    }

    public static boolean toggleFilter(ItemStack staff, ResourceLocation id) {
        if (id == null) return false;
        List<ResourceLocation> values = new ArrayList<>(filters(staff));
        boolean added;
        if (values.remove(id)) {
            added = false;
        } else {
            if (values.size() >= RangeAccelerationSavedData.MAX_FILTERS) return false;
            values.add(id);
            added = true;
        }
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_FILTERS.get(), List.copyOf(values));
        return added;
    }

    public static void set(ItemStack staff, boolean placementMode, boolean filterMarkingMode,
                           boolean markSleepList,
                           int sizeX, int sizeY, int sizeZ,
                           int offsetX, int offsetY, int offsetZ,
                           boolean accelerationWhitelistMode, boolean sleepWhitelistMode) {
        // These are both exclusive interaction modes. Marking wins if malformed data enables both.
        if (filterMarkingMode) placementMode = false;
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_PLACEMENT_MODE.get(), placementMode);
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_FILTER_MARKING_MODE.get(), filterMarkingMode);
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_MARK_SLEEP_LIST.get(), markSleepList);
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_SIZE_X.get(), RangeAccelerationSavedData.clampSize(sizeX));
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_SIZE_Y.get(), RangeAccelerationSavedData.clampSize(sizeY));
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_SIZE_Z.get(), RangeAccelerationSavedData.clampSize(sizeZ));
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_OFFSET_X.get(), RangeAccelerationSavedData.clampOffset(offsetX));
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_OFFSET_Y.get(), RangeAccelerationSavedData.clampOffset(offsetY));
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_OFFSET_Z.get(), RangeAccelerationSavedData.clampOffset(offsetZ));
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_IDLE_THROTTLE_DISABLED.get(), sleepWhitelistMode);
        CoreItemComponents.set(staff, UselessStretcherComponents.RANGE_WHITELIST_MODE.get(), accelerationWhitelistMode);
    }

    /** Compatibility overload for callers that do not expose offsets. */
    public static void set(ItemStack staff, boolean placementMode, boolean filterMarkingMode,
                           boolean markSleepList,
                           int sizeX, int sizeY, int sizeZ,
                           boolean accelerationWhitelistMode, boolean sleepWhitelistMode) {
        set(staff, placementMode, filterMarkingMode, markSleepList,
                sizeX, sizeY, sizeZ, 0, 0, 0,
                accelerationWhitelistMode, sleepWhitelistMode);
    }
}
