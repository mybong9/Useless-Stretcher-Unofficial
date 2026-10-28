package com.sorrowmist.useless.stretcher.content.ae;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A thread-local snapshot of the AE network used while writing omniversal patterns. When a recipe
 * input is a tag, {@link #pickPreferred} prioritizes craftable members, then highest stock,
 * and finally the source recipe representative. Selection is shared with terminal encoding in Core.
 */
public final class AeMaterialContext {
    private static final ThreadLocal<AeMaterialContext> CURRENT = new ThreadLocal<>();

    private final com.sorrowmist.uselesscore.api.ae.CoreAeMaterialContext delegate;
    private static final ThreadLocal<java.util.Deque<com.sorrowmist.uselesscore.api.ae.CoreAeMaterialContext.Scope>> SCOPES =
            ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private static final Direction[] DIRECTIONS = Direction.values();
    private AeMaterialContext(IGrid grid) {
        delegate=com.sorrowmist.uselesscore.api.ae.CoreAeMaterialContext.fromGrid(grid);
    }

    public static AeMaterialContext fromNearbyGrid(Level level, BlockPos center, int radius) {
        if (level == null || center == null) return null;
        IGrid grid = findGrid(level, center, radius);
        if (grid == null) return null;
        return new AeMaterialContext(grid);
    }

    /** Resolves the grid of a specific (previously bound) AE node position. */
    public static AeMaterialContext fromBoundNode(Level level, BlockPos nodePos) {
        if (level == null || nodePos == null || !level.isLoaded(nodePos)) return null;
        BlockEntity blockEntity = level.getBlockEntity(nodePos);
        if (!(blockEntity instanceof IInWorldGridNodeHost host)) return null;
        for (Direction direction : DIRECTIONS) {
            IGridNode node = host.getGridNode(direction);
            if (node != null && node.getGrid() != null) {
                IGrid grid = node.getGrid();
                return new AeMaterialContext(grid);
            }
        }
        return null;
    }

    public static void push(AeMaterialContext context) {
        CURRENT.set(context);
        SCOPES.get().push(com.sorrowmist.uselesscore.api.ae.CoreAeMaterialContext.enter(context==null?null:context.delegate));
    }
    public static void pop() {
        var stack=SCOPES.get();
        if(!stack.isEmpty())stack.pop().close();
        if(stack.isEmpty())SCOPES.remove();
        CURRENT.remove();
    }
    public static AeMaterialContext current() { return CURRENT.get(); }
    public ItemStack pickPreferred(Ingredient ingredient, ItemStack fallback) {
        return delegate.pickItem(ingredient,fallback);
    }

    private static IGrid findGrid(Level level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isLoaded(cursor)) continue;
                    BlockEntity blockEntity = level.getBlockEntity(cursor);
                    if (!(blockEntity instanceof IInWorldGridNodeHost host)) continue;
                    for (Direction direction : DIRECTIONS) {
                        IGridNode node = host.getGridNode(direction);
                        if (node != null && node.getGrid() != null) return node.getGrid();
                    }
                }
            }
        }
        return null;
    }
}
