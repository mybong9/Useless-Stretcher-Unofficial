package com.sorrowmist.useless.stretcher.content.mold;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import com.sorrowmist.uselesscore.api.component.OmniversalPatternData;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.uselesscore.api.network.codec.CoreAeCodecs;

import java.util.List;

/** Reads encoded outputs without resolving a recipe or replacing its binding metadata. */
public final class PatternOutputs {
    private PatternOutputs() { }

    public static GenericStack primary(AEItemKey pattern) {
        var outputs = CoreAeCodecs.patternData(pattern).map(CoreAeCodecs.PatternData::outputs).orElse(List.of());
        if (CoreAeCodecs.hasProcessingPatternPayload(pattern)) {
            GenericStack first = null;
            long amount = 0;
            for (GenericStack output : outputs) {
                if (output == null || output.amount() <= 0) continue;
                if (first == null) first = output;
                if (first.what().equals(output.what())) {
                    if (amount > Long.MAX_VALUE - output.amount()) return null;
                    amount += output.amount();
                }
            }
            return first == null ? null : new GenericStack(first.what(), amount);
        }
        return outputs.isEmpty() ? null : outputs.get(0);
    }

    public static AEItemKey withoutByproducts(AEItemKey pattern) {
        if (!CoreAeCodecs.hasProcessingPatternPayload(pattern)) return pattern;
        var encodedOutputs = CoreAeCodecs.processingOutputs(pattern);
        GenericStack primary = primary(pattern);
        if (primary == null || encodedOutputs.stream().noneMatch(
                output -> output != null && !primary.what().equals(output.what()))) return pattern;
        var replacement = pattern.toStack(1);
        if (!CoreAeCodecs.setProcessingPatternOutputs(replacement, List.of(primary))) return pattern;
        var data = CoreItemComponents.get(pattern, UselessComponents.OMNIVERSAL_PATTERN_DATA.get());
        if (data != null) {
            // Dynamic output flags refer to condensed output slots, not the recipe's identity.
            CoreItemComponents.set(replacement, UselessComponents.OMNIVERSAL_PATTERN_DATA.get(), new OmniversalPatternData(
                    data.version(), data.recipeId(), data.recipeFingerprint(), data.sourceId(),
                    data.requiresMold(), data.displayMold(), data.displayMolds(), data.tagInputSlots(),
                    data.fluidTagInputSlots(), data.moldTagInputSlots(), data.itemIdInputSlots(),
                    data.itemIdOutputSlots().contains(0) ? List.of(0) : List.of()));
        }
        return AEItemKey.of(replacement);
    }
}
