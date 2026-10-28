package com.sorrowmist.useless.stretcher.mixin;

import net.minecraftforge.fml.loading.FMLLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Optional-target gating for the Forge port, which already supplies native capacity and paging. */
public final class UselessStretcherMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) { }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(".ApotheosisEnchantingMenuMixin")
                || mixinClassName.endsWith(".ApotheosisEnchantingStatsMixin")) {
            return isModLoadedEarly("apotheosis");
        }
        return true;
    }

    private static boolean isModLoadedEarly(String modId) {
        try {
            var loadingList = FMLLoader.getLoadingModList();
            if (loadingList != null) {
                return loadingList.getMods().stream().anyMatch(mod -> modId.equals(mod.getModId()));
            }
        } catch (RuntimeException | LinkageError ignored) {
            // Continue to the normal list when bootstrap has already completed.
        }
        try {
            var modList = net.minecraftforge.fml.ModList.get();
            return modList != null && modList.isLoaded(modId);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) { }
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) { }
}
