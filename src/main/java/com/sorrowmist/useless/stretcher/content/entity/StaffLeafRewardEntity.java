package com.sorrowmist.useless.stretcher.content.entity;

import com.sorrowmist.useless.stretcher.init.ModEntities;
import com.sorrowmist.useless.stretcher.init.ModItems;
import com.sorrowmist.uselesscore.api.component.UselessStretcherComponents;
import com.sorrowmist.uselesscore.api.enums.tool.EnchantMode;
import com.sorrowmist.uselesscore.api.enums.tool.ToolTypeMode;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import com.sorrowmist.uselesscore.api.component.CoreItemStackData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** Untouchable visual carrier used while a player's one-time leaf reward is waiting for space. */
public final class StaffLeafRewardEntity extends Entity {
    private UUID ownerId;
    private final ItemStack displayStack = new ItemStack(ModItems.WONDROUS_STAFF.get());


    public StaffLeafRewardEntity(EntityType<? extends StaffLeafRewardEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public StaffLeafRewardEntity(Level level, UUID ownerId) {
        this(ModEntities.STAFF_LEAF_REWARD.get(), level);
        this.ownerId = ownerId;
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
        this.setDeltaMovement(Vec3.ZERO);
        this.clearFire();
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public ItemStack getDisplayStack() {
        return displayStack;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ownerId = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (ownerId != null) tag.putUUID("owner", ownerId);
    }
}
