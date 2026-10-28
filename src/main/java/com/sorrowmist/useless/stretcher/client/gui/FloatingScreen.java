package com.sorrowmist.useless.stretcher.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** Common input and clipping; subclasses lay out real widgets without scaling fonts. */
public abstract class FloatingScreen extends Screen {
    private final FloatingWindow window;
    private int contentScroll, contentExtent;
    private boolean contentInitialized;
    private boolean scrollbarDragging;
    private double scrollbarGrab;

    protected FloatingScreen(Component title, String key, int defaultWidth, int defaultHeight, int minWidth, int minHeight) {
        super(title);
        window = new FloatingWindow(key, defaultWidth, defaultHeight, minWidth, minHeight);
    }
    public final FloatingWindow floatingWindow() { return window; }
    protected final int contentLeft() { return window.bodyLeft() + sidebarWidth(); }
    protected final int contentTop() { return window.bodyTop() - contentScroll; }
    protected final int contentWidth() { return Math.max(1, window.bodyWidth() - 5 - sidebarWidth()); }

    /** Optional left tab strip; the shared chrome in {@link StaffScreen} uses it. */
    protected int sidebarWidth() { return 0; }
    protected void initChrome() {}
    protected void renderChrome(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}
    protected final int contentHeight() { return window.bodyHeight(); }
    protected final void setContentExtent(int height) {
        contentExtent = Math.max(0, height);
        contentScroll = Mth.clamp(contentScroll, 0, maxScroll());
    }
    protected abstract void initContent();
    protected abstract void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
    protected void renderContentTooltip(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override protected final void init() {
        window.init(width, height);
        window.consumeLayoutChanged();
        relayoutContent();
        contentInitialized = true;
    }
    private void relayoutContent() {
        var focused = getFocused();
        int focusedIndex = children().indexOf(focused);
        clearWidgets();
        setFocused(null);
        int oldScroll = contentScroll;
        initContent();
        initChrome();
        if (oldScroll != contentScroll) { clearWidgets(); initContent(); initChrome(); }
        restoreFocus(focused, focusedIndex);
    }
    @Override protected void rebuildWidgets() {
        var focused = getFocused();
        int focusedIndex = children().indexOf(focused);
        super.rebuildWidgets();
        restoreFocus(focused, focusedIndex);
    }
    private void restoreFocus(GuiEventListener focused, int focusedIndex) {
        Component focusLabel = focused instanceof AbstractWidget widget ? widget.getMessage() : null;
        if (focused != null && children().contains(focused)) setFocused(focused);
        else if (focusLabel != null && focusedIndex >= 0 && focusedIndex < children().size()
                && children().get(focusedIndex) instanceof AbstractWidget widget
                && widget.getClass() == focused.getClass() && widget.getMessage().equals(focusLabel)) {
            setFocused(widget);
        }
        else if (focusLabel != null) for (var child : children()) {
            if (child instanceof AbstractWidget widget && child.getClass() == focused.getClass() && widget.getMessage().equals(focusLabel)) {
                setFocused(child); break;
            }
        }
    }

    @Override public final void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // A server page can replace the screen during its opening callback. Initialize
        // that newly active screen before touching its font, widgets, or clip bounds.
        if (font == null || minecraft == null || !contentInitialized) {
            var client = net.minecraft.client.Minecraft.getInstance();
            if (client.screen != this || client.font == null) return;
            init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
            return;
        }
        if (com.sorrowmist.useless.client.gui.StaffGlass.aeStyle()) graphics.fill(0, 0, width, height, 0x33000000);
        else com.sorrowmist.uselesscore.client.ui.UiRender.captureScene(graphics);
        window.renderFrame(graphics, font, title, mouseX, mouseY);
        boolean inside = window.containsBody(mouseX, mouseY) && !window.capturing();
        int contentMouseX = inside ? mouseX : -10000, contentMouseY = inside ? mouseY : -10000;
        graphics.enableScissor(contentLeft(), window.bodyTop(), contentLeft() + contentWidth() + 5, window.bodyTop() + window.bodyHeight());
        try {
            renderContent(graphics, contentMouseX, contentMouseY, partialTick);
            super.render(graphics, contentMouseX, contentMouseY, partialTick);
        } finally { graphics.disableScissor(); }
        renderChrome(graphics, mouseX, mouseY, partialTick);
        if (maxScroll() > 0) {
            int x = contentLeft() + contentWidth() + 2;
            graphics.fill(x, window.bodyTop(), x + 2, window.bodyTop() + contentHeight(), StretcherScreenStyle.shadowColor());
            graphics.fill(x, thumbTop(), x + 2, thumbTop() + thumbHeight(), StretcherScreenStyle.activeColor());
        }
        if (inside) renderContentTooltip(graphics, mouseX, mouseY, partialTick);
        window.renderTooltip(graphics, font, mouseX, mouseY);
    }
    @Override public final void renderBackground(GuiGraphics graphics) {}

    @Override public final boolean mouseClicked(double x, double y, int button) {
        if (window.mouseClicked(x, y, button)) {
            if (window.consumeLayoutChanged()) { contentScroll = 0; relayoutContent(); }
            if (window.consumeCloseRequested()) onClose();
            return true;
        }
        if (!window.containsBody(x, y)) return false;
        if (x < contentLeft() && super.mouseClicked(x, y, button)) return true;
        if (button == 0 && maxScroll() > 0 && x >= window.bodyLeft() + window.bodyWidth() - 5) {
            scrollbarDragging = true;
            scrollbarGrab = y >= thumbTop() && y <= thumbTop() + thumbHeight() ? y - thumbTop() : thumbHeight() / 2.0;
            dragScrollbar(y);
            return true;
        }
        return clickContent(x, y, button);
    }
    protected boolean clickContent(double x, double y, int button) { return super.mouseClicked(x, y, button); }
    protected boolean dragContent(double x, double y, int button, double dx, double dy) { return false; }
    protected boolean releaseContent(double x, double y, int button) { return false; }
    @Override public final boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (window.mouseDragged(x, y, button)) { if (window.consumeLayoutChanged()) relayoutContent(); return true; }
        if (scrollbarDragging && button == 0) { dragScrollbar(y); return true; }
        if (window.containsBody(x, y) && dragContent(x, y, button, dx, dy)) return true;
        return super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public final boolean mouseReleased(double x, double y, int button) {
        if (window.mouseReleased(button)) return true;
        if (x < contentLeft() && super.mouseReleased(x, y, button)) return true;
        if (button == 0 && scrollbarDragging) { scrollbarDragging = false; return true; }
        if (releaseContent(x, y, button)) return true;
        return super.mouseReleased(x, y, button);
    }
    @Override public final boolean mouseScrolled(double x, double y, double dy) {
        return window.containsBody(x, y) && !window.capturing() && scrollContent(x, y, 0, dy);
    }
    protected boolean scrollContent(double x, double y, double dx, double dy) {
        if (super.mouseScrolled(x, y, dy)) return true;
        int next = Mth.clamp(contentScroll - (int) Math.round(dy * 20), 0, maxScroll());
        if (next != contentScroll) { contentScroll = next; relayoutContent(); return true; }
        return false;
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (handled && minecraft.screen == this && maxScroll() > 0 && getFocused() instanceof AbstractWidget widget) {
            int delta = widget.getY() < window.bodyTop() ? widget.getY() - window.bodyTop()
                    : Math.max(0, (widget.getY() + widget.getHeight()) - window.bodyTop() - contentHeight());
            int next = Mth.clamp(contentScroll + delta, 0, maxScroll());
            if (next != contentScroll) { contentScroll = next; relayoutContent(); }
        }
        return handled;
    }
    private int maxScroll() { return Math.max(0, contentExtent - contentHeight()); }
    private int thumbHeight() { return Math.min(contentHeight(), Math.max(16, contentHeight() * contentHeight() / Math.max(1, contentExtent))); }
    private int thumbTop() { return window.bodyTop() + (contentHeight() - thumbHeight()) * contentScroll / Math.max(1, maxScroll()); }
    private void dragScrollbar(double y) {
        int next = Mth.clamp((int) Math.round((y - window.bodyTop() - scrollbarGrab) * maxScroll() / Math.max(1, contentHeight() - thumbHeight())), 0, maxScroll());
        if (next != contentScroll) { contentScroll = next; relayoutContent(); }
    }
    @Override public void removed() { window.save(); com.sorrowmist.uselesscore.client.ui.UiRender.releaseScene(); super.removed(); }
    @Override public boolean isPauseScreen() { return false; }
}
