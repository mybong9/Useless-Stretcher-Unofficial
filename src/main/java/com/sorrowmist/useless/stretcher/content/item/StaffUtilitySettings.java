package com.sorrowmist.useless.stretcher.content.item;

import com.sorrowmist.useless.stretcher.init.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Item-local settings, shared by the UI, server packets and movement hooks. */
public final class StaffUtilitySettings {
    private static final String DISMANTLE="useless_stretcher_dismantle";
    private static final String SPEED="useless_stretcher_flight_speed";
    private static final String INERTIA="useless_stretcher_flight_inertia";
    private StaffUtilitySettings() {}
    public static boolean dismantle(ItemStack stack) {
        return stack.is(ModItems.WONDROUS_STAFF.get()) && stack.hasTag() && stack.getTag().getBoolean(DISMANTLE);
    }
    public static void setDismantle(ItemStack stack,boolean enabled) { stack.getOrCreateTag().putBoolean(DISMANTLE,enabled); }
    public static double speed(ItemStack stack) { return number(stack,SPEED,1,.1,10); }
    public static double inertia(ItemStack stack) { return number(stack,INERTIA,1,0,1); }
    private static double number(ItemStack stack,String key,double fallback,double min,double max) {
        double v=stack.hasTag() && stack.getTag().contains(key,99)?stack.getTag().getDouble(key):fallback;
        return Double.isFinite(v)?Math.max(min,Math.min(max,v)):fallback;
    }
    public static void flight(ItemStack stack,double speed,double inertia) {
        stack.getOrCreateTag().putDouble(SPEED,Double.isFinite(speed)?Math.max(.1,Math.min(10,speed)):1);
        stack.getOrCreateTag().putDouble(INERTIA,Double.isFinite(inertia)?Math.max(0,Math.min(1,inertia)):1);
    }
    public static ItemStack carried(Player player) {
        if (player==null) return ItemStack.EMPTY;
        if (player.getMainHandItem().is(ModItems.WONDROUS_STAFF.get())) return player.getMainHandItem();
        if (player.getOffhandItem().is(ModItems.WONDROUS_STAFF.get())) return player.getOffhandItem();
        for (ItemStack stack:player.getInventory().items) if (stack.is(ModItems.WONDROUS_STAFF.get())) return stack;
        return ItemStack.EMPTY;
    }
    public static float flightSpeed(Player player,float original) {
        ItemStack staff=carried(player);
        return staff.isEmpty() || !player.getAbilities().flying || player.isPassenger()
                ? original : (float)(original*speed(staff));
    }
}
