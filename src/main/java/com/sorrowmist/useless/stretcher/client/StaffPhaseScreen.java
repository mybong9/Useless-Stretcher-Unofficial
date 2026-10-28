package com.sorrowmist.useless.stretcher.client;

import com.sorrowmist.useless.stretcher.client.gui.*;
import com.sorrowmist.useless.stretcher.init.StretcherPhaseComponents;
import com.sorrowmist.useless.stretcher.network.Port160Network;
import com.sorrowmist.useless.stretcher.UselessStretcherMod;
import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public final class StaffPhaseScreen extends StaffScreen {
    private final Screen parent;
    private final InteractionHand hand;
    private boolean phase, interaction;
    public StaffPhaseScreen(Screen parent, InteractionHand hand) {
        super(Component.translatable("gui.useless_stretcher.phase.title"), StaffScreen.Tab.PHASE, parent, hand);
        this.parent = parent;
        this.hand = hand;
    }
    @Override protected void initContent() {
        if (minecraft.player == null) { onClose(); return; }
        var stack = minecraft.player.getItemInHand(hand);
        phase = CoreItemComponents.getOrDefault(stack, StretcherPhaseComponents.WONDROUS_STAFF_PHASE_ENABLED.get(), false);
        interaction = CoreItemComponents.getOrDefault(stack, StretcherPhaseComponents.WONDROUS_STAFF_PHASE_INTERACTION.get(), true);
        addToggle(0, phase, () -> { phase = !phase; save(); }, () -> phase);
        addToggle(1, interaction, () -> { interaction = !interaction; save(); }, () -> interaction);
        int buttonWidth = Math.min(110, contentWidth());
        addRenderableWidget(new SelectableAE2Button(contentLeft() + contentWidth() - buttonWidth,
                contentTop() + 126, buttonWidth, 22, Component.translatable("gui.done"), b -> onClose()));
        setContentExtent(152);
    }
    private void addToggle(int row, boolean enabled, Runnable action, java.util.function.BooleanSupplier value) {
        var button = new SelectableAE2Button(contentLeft() + contentWidth() - 67, contentTop() + 16 + row * 58,
                56, 24, state(enabled), b -> {
                    action.run();
                    b.setMessage(state(value.getAsBoolean()));
                    ((SelectableAE2Button) b).setSelected(value.getAsBoolean());
                });
        button.setSelected(enabled);
        addRenderableWidget(button);
    }
    private Component state(boolean enabled) { return Component.translatable(enabled ? "options.on" : "options.off"); }
    private void save() {
        if (minecraft.player == null) return;
        var stack = minecraft.player.getItemInHand(hand);
        if (!stack.is(com.sorrowmist.useless.stretcher.init.ModItems.WONDROUS_STAFF.get())) return;
        CoreItemComponents.set(stack, StretcherPhaseComponents.WONDROUS_STAFF_PHASE_ENABLED.get(), phase);
        CoreItemComponents.set(stack, StretcherPhaseComponents.WONDROUS_STAFF_PHASE_INTERACTION.get(), interaction);
        UselessStretcherMod.NETWORK.sendToServer(new Port160Network.PhaseSettings(phase, interaction, hand == InteractionHand.OFF_HAND));
    }
    @Override protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (int row = 0; row < 2; row++) {
            int y = contentTop() + 5 + row * 58;
            StretcherScreenStyle.drawInset(graphics, contentLeft(), y, contentLeft() + contentWidth(), y + 50);
            String key = row == 0 ? "gui.useless_stretcher.phase.enabled" : "gui.useless_stretcher.phase.interaction";
            var label = font.plainSubstrByWidth(Component.translatable(key).getString(), Math.max(1, contentWidth() - 92));
            graphics.drawString(font, label, contentLeft() + 10, y + 20, StretcherScreenStyle.textColor(), false);
        }
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
}
