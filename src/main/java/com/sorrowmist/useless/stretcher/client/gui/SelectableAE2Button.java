package com.sorrowmist.useless.stretcher.client.gui;

import com.sorrowmist.uselesscore.client.ui.UiDraw;
import com.sorrowmist.uselesscore.client.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Dark glass button with a persistent selected state for mutually exclusive choices. */
public final class SelectableAE2Button extends Button {
    private boolean selected;
    private net.minecraft.client.gui.components.Tooltip labelTooltip;

    public SelectableAE2Button(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        ButtonHelp.automatic(this, message);
        labelTooltip = getTooltip();
    }

    @Override public void setMessage(Component message) {
        super.setMessage(message);
        // Refresh state-derived help, without replacing explicit contextual tooltips.
        if (getTooltip() == labelTooltip) {
            ButtonHelp.automatic(this, message);
            labelTooltip = getTooltip();
        }
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        com.sorrowmist.useless.client.gui.StaffGlass.button(graphics, getX(), getY(), getWidth(), getHeight(), active, hovered, selected);
        var font = Minecraft.getInstance().font;
        int colour = active ? com.sorrowmist.useless.client.gui.StaffGlass.textColor()
                : com.sorrowmist.useless.client.gui.StaffGlass.mutedColor();
        var label = getMessage();
        if (font.width(label) > getWidth() - 10) label = Component.literal(
                font.plainSubstrByWidth(label.getString(), Math.max(1, getWidth() - 16)) + "…").withStyle(label.getStyle());
        if (com.sorrowmist.useless.client.gui.StaffGlass.aeStyle()) {
            com.sorrowmist.useless.client.gui.OriginalAeStaffStyle.buttonText(graphics, font, label,
                    getX(), getY(), getWidth(), getHeight(), true, selected);
            return;
        }
        graphics.drawString(font, label, getX() + (getWidth() - font.width(label)) / 2,
                getY() + (getHeight() - font.lineHeight + 1) / 2, colour, false);
    }
}