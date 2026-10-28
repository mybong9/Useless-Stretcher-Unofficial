package com.sorrowmist.useless.stretcher.client.gui;

import com.sorrowmist.uselesscore.client.ui.UiDraw;
import com.sorrowmist.uselesscore.client.ui.UiTheme;
import net.minecraft.client.gui.GuiGraphics;

/** Shared dark glass palette matching the multiblock machine interfaces. */
public final class StretcherScreenStyle {
    public static final int PANEL_COLOR = 0xFF353E49;
    public static final int HIGHLIGHT_COLOR = 0xFF4A5664;
    public static final int SLOT_COLOR = 0x99303A46;
    public static final int SLOT_SHADOW_COLOR = 0xCC202A33;
    public static final int TEXT_COLOR = 0xFFEDF3FA;
    public static final int MUTED_TEXT_COLOR = 0xFFBBC7D3;
    public static final int SUBTLE_TEXT_COLOR = 0xFF97A6B5;
    public static final int ACTIVE_COLOR = 0xFF3299FF;
    public static final int SUCCESS_COLOR = 0xFF4CC38A;
    public static final int WARNING_COLOR = 0xFFFFB454;

    public static int textColor() { return com.sorrowmist.useless.client.gui.StaffGlass.textColor(); }
    public static int mutedColor() { return com.sorrowmist.useless.client.gui.StaffGlass.mutedColor(); }
    public static int subtleColor() { return com.sorrowmist.useless.client.gui.StaffGlass.aeStyle() ? 0xFF6D7287 : mutedColor(); }
    public static int panelColor() { return com.sorrowmist.useless.client.gui.StaffGlass.aeStyle() ? 0xFFCBCCD4 : UiTheme.DARK.panel(); }
    public static int highlightColor() { return com.sorrowmist.useless.client.gui.StaffGlass.aeStyle() ? 0xFFF2F2F2 : UiTheme.DARK.selected(); }
    public static int slotColor() { return com.sorrowmist.useless.client.gui.StaffGlass.aeStyle() ? 0xFFADB0C4 : UiTheme.DARK.inset(); }
    public static int shadowColor() { return com.sorrowmist.useless.client.gui.StaffGlass.aeStyle() ? 0xFF9A9FB4 : 0x55303A46; }
    public static int activeColor() { return com.sorrowmist.useless.client.gui.StaffGlass.aeStyle() ? 0xFF517497 : ACTIVE_COLOR; }
    public static int successColor() { return com.sorrowmist.useless.client.gui.StaffGlass.aeStyle() ? 0xFF3B7652 : SUCCESS_COLOR; }
    public static int warningColor() { return com.sorrowmist.useless.client.gui.StaffGlass.aeStyle() ? 0xFF9A6B18 : WARNING_COLOR; }
    private StretcherScreenStyle() {
    }

    public static void drawPanel(GuiGraphics graphics, int left, int top, int width, int height) {
        com.sorrowmist.useless.client.gui.StaffGlass.panel(graphics, left, top, width, height);
    }

    public static void drawInset(GuiGraphics graphics, int left, int top, int right, int bottom) {
        com.sorrowmist.useless.client.gui.StaffGlass.card(graphics, left, top, right - left, bottom - top);
    }
}