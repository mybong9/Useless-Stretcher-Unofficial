package com.sorrowmist.useless.stretcher.content.item;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;

import java.lang.reflect.Method;
import java.util.Optional;

/** Generic brush bridge: works with vanilla and compatible archaeology block entities. */
public final class StaffBrushHelper {
    private static final ClassValue<Optional<Method>> BRUSH_METHODS=new ClassValue<>() {
        @Override protected Optional<Method> computeValue(Class<?> type){return findBrush(type);}
    };
    private StaffBrushHelper() { }

    public static InteractionResult tryBrush(ItemStack staff, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockEntity target = level.getBlockEntity(context.getClickedPos());
        if (player == null || target == null) return InteractionResult.PASS;
        BrushableBlockEntity vanilla=target instanceof BrushableBlockEntity brushable?brushable:null;
        Method method=vanilla==null?BRUSH_METHODS.get(target.getClass()).orElse(null):null;
        if(vanilla==null && method==null)return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel)) return InteractionResult.PASS;
        Direction face = context.getClickedFace();
        long now = level.getGameTime();
        StaffBrushContext.run(() -> {
            try {
                // Vanilla requires ten brush advances. Passing future virtual times removes the
                // cooldown between them while the mixin resets the completed loot state.
                for (int i = 0; i < 64; i++) {
                    boolean complete = vanilla!=null ? vanilla.brush(now+(long)i*10L,player,face)
                            : Boolean.TRUE.equals(method.invoke(target, now + (long) i * 10L, player, face));
                    if (complete) break;
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // A third-party archaeology implementation is optional; normal brushing remains.
            }
        });
        return InteractionResult.SUCCESS;
    }

    private static Optional<Method> findBrush(Class<?> type) {
        while (type != null) {
            try {
                Method method = type.getDeclaredMethod("brush", long.class, Player.class, Direction.class);
                if(method.getReturnType()!=boolean.class && method.getReturnType()!=Boolean.class)return Optional.empty();
                method.setAccessible(true);
                return Optional.of(method);
            } catch (NoSuchMethodException ignored) {
                type = type.getSuperclass();
            } catch (RuntimeException | LinkageError ignored) {
                // Optional signatures may reference absent integrations (e.g. NuclearCraft / OC2).
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
