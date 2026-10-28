package com.sorrowmist.useless.stretcher.client.gui;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

/** Client-only bounds; dragging never writes gameplay settings or sends packets. */
public final class FloatingWindow {
    private static final int MARGIN = 4, EDGE = 4, TITLE_HEIGHT = 23;
    private static final int LEFT = 1, RIGHT = 2, TOP = 4, BOTTOM = 8, MOVE = 16;
    private static final Map<String, Saved> SAVED = new HashMap<>();
    private static Path loadedPath;
    private final String key;
    private final int defaultWidth, defaultHeight, minWidth, minHeight;
    private int left, top, width, height, viewportWidth, viewportHeight;
    private int operation, startLeft, startTop, startWidth, startHeight;
    private double startX, startY;
    private boolean initialized, changed, closeRequested, dirty;

    public FloatingWindow(String key, int defaultWidth, int defaultHeight, int minWidth, int minHeight) {
        this.key = key;
        this.defaultWidth = defaultWidth;
        this.defaultHeight = defaultHeight;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    public void init(int viewportWidth, int viewportHeight) {
        if (initialized && this.viewportWidth == viewportWidth && this.viewportHeight == viewportHeight) return;
        double relativeX = initialized ? fraction(left - MARGIN, this.viewportWidth - width - MARGIN * 2) : 0.5;
        double relativeY = initialized ? fraction(top - MARGIN, this.viewportHeight - height - MARGIN * 2) : 0.5;
        this.viewportWidth = Math.max(1, viewportWidth);
        this.viewportHeight = Math.max(1, viewportHeight);
        if (!initialized) {
            loadLayouts();
            Saved stored = SAVED.get(key);
            width = stored == null ? defaultWidth : stored.width;
            height = stored == null ? defaultHeight : stored.height;
            if (stored != null) { relativeX = stored.x; relativeY = stored.y; }
        }
        width = Mth.clamp(width, effectiveMinWidth(), maxWidth());
        height = Mth.clamp(height, effectiveMinHeight(), maxHeight());
        left = MARGIN + (int) Math.round(relativeX * Math.max(0, this.viewportWidth - width - MARGIN * 2));
        top = MARGIN + (int) Math.round(relativeY * Math.max(0, this.viewportHeight - height - MARGIN * 2));
        initialized = true;
        operation = 0;
        changed = true;
    }

    public int left() { return left; }
    public int top() { return top; }
    public int width() { return width; }
    public int height() { return height; }
    public int bodyLeft() { return left + 6; }
    public int bodyTop() { return top + TITLE_HEIGHT; }
    public int bodyWidth() { return Math.max(1, width - 12); }
    public int bodyHeight() { return Math.max(1, height - TITLE_HEIGHT - 6); }
    public boolean containsBody(double x, double y) {
        return x >= bodyLeft() && x < bodyLeft() + bodyWidth() && y >= bodyTop() && y < bodyTop() + bodyHeight();
    }
    public boolean capturing() { return operation != 0; }
    public boolean consumeLayoutChanged() { boolean result = changed; changed = false; return result; }
    public boolean consumeCloseRequested() { boolean result = closeRequested; closeRequested = false; return result; }

    public boolean mouseClicked(double x, double y, int button) {
        if (button != 0) return false;
        if (inside(x, y, left + width - 101, top + 5, 58, 14)) {
            com.sorrowmist.useless.client.gui.StaffGlass.toggleStyle(); return true;
        }
        if (inside(x, y, left + width - 21, top + 5, 14, 14)) { closeRequested = true; return true; }
        if (inside(x, y, left + width - 38, top + 5, 14, 14)) { reset(); save(); return true; }
        int edge = edgeAt(x, y);
        if (edge != 0) operation = edge;
        else if (inside(x, y, left + EDGE, top + EDGE, width - EDGE * 2, TITLE_HEIGHT - EDGE)) operation = MOVE;
        else return false;
        startX = x; startY = y;
        startLeft = left; startTop = top; startWidth = width; startHeight = height;
        return true;
    }

    public boolean mouseDragged(double x, double y, int button) {
        if (button != 0 || operation == 0) return false;
        int dx = (int) Math.round(x - startX), dy = (int) Math.round(y - startY);
        int oldLeft = left, oldTop = top, oldWidth = width, oldHeight = height;
        if (operation == MOVE) {
            left = Mth.clamp(startLeft + dx, MARGIN, Math.max(MARGIN, viewportWidth - width - MARGIN));
            top = Mth.clamp(startTop + dy, MARGIN, Math.max(MARGIN, viewportHeight - height - MARGIN));
        } else {
            if ((operation & LEFT) != 0) {
                left = Mth.clamp(startLeft + dx, MARGIN, startLeft + startWidth - effectiveMinWidth());
                width = startLeft + startWidth - left;
            }
            if ((operation & RIGHT) != 0) width = Mth.clamp(startWidth + dx, effectiveMinWidth(), viewportWidth - left - MARGIN);
            if ((operation & TOP) != 0) {
                top = Mth.clamp(startTop + dy, MARGIN, startTop + startHeight - effectiveMinHeight());
                height = startTop + startHeight - top;
            }
            if ((operation & BOTTOM) != 0) height = Mth.clamp(startHeight + dy, effectiveMinHeight(), viewportHeight - top - MARGIN);
        }
        if (left != oldLeft || top != oldTop || width != oldWidth || height != oldHeight) { changed = true; dirty = true; }
        return true;
    }

    public boolean mouseReleased(int button) {
        if (button != 0 || operation == 0) return false;
        operation = 0;
        save();
        return true;
    }

    public void reset() {
        width = Mth.clamp(defaultWidth, effectiveMinWidth(), maxWidth());
        height = Mth.clamp(defaultHeight, effectiveMinHeight(), maxHeight());
        left = Math.max(MARGIN, (viewportWidth - width) / 2);
        top = Math.max(MARGIN, (viewportHeight - height) / 2);
        operation = 0;
        dirty = changed = true;
    }

    public void renderFrame(GuiGraphics graphics, Font font, Component title, int mouseX, int mouseY) {
        if (font == null) return;
        StretcherScreenStyle.drawPanel(graphics, left, top, width, height);
        // Preserve the 1.21 AE header strip when that appearance is selected.
        if (com.sorrowmist.useless.client.gui.StaffGlass.aeStyle())
            graphics.fill(left + 5, top + 4, left + width - 5, top + TITLE_HEIGHT - 2, StretcherScreenStyle.slotColor());
        graphics.drawString(font, font.plainSubstrByWidth(title.getString(), Math.max(1, width - 117)),
                left + 9, top + 9, StretcherScreenStyle.textColor(), false);
        com.sorrowmist.useless.client.gui.StaffGlass.button(graphics, left + width - 101, top + 5, 58, 14,
                true, inside(mouseX, mouseY, left + width - 101, top + 5, 58, 14), false);
        var appearance = com.sorrowmist.useless.client.gui.StaffGlass.styleLabel();
        graphics.drawString(font, appearance, left + width - 72 - font.width(appearance) / 2,
                top + 8, com.sorrowmist.useless.client.gui.StaffGlass.textColor(), false);
        drawControl(graphics, left + width - 38, top + 5, true, mouseX, mouseY);
        drawControl(graphics, left + width - 21, top + 5, false, mouseX, mouseY);
        int edge = edgeAt(mouseX, mouseY);
        int color = edge == 0 ? StretcherScreenStyle.subtleColor() : StretcherScreenStyle.activeColor();
        for (int i = 0; i < 3; i++) {
            int endX = left + width - 4, endY = top + height - 4, step = 3 + i * 3;
            com.sorrowmist.uselesscore.client.ui.UiDraw.line(graphics, endX - step, endY, endX, endY - step, .8f, color);
        }
        if ((edge & LEFT) != 0) graphics.fill(left, top + 4, left + 1, top + height - 4, color);
        if ((edge & RIGHT) != 0) graphics.fill(left + width - 1, top + 4, left + width, top + height - 4, color);
        if ((edge & TOP) != 0) graphics.fill(left + 4, top, left + width - 4, top + 1, color);
        if ((edge & BOTTOM) != 0) graphics.fill(left + 4, top + height - 1, left + width - 4, top + height, color);
    }

    public void renderTooltip(GuiGraphics graphics, Font font, int x, int y) {
        if (font == null || capturing()) return;
        String help = inside(x, y, left + width - 21, top + 5, 14, 14) ? "close"
                : inside(x, y, left + width - 38, top + 5, 14, 14) ? "reset"
                : edgeAt(x, y) != 0 ? "resize"
                : inside(x, y, left + 4, top + 4, width - 108, TITLE_HEIGHT - 4) ? "move" : null;
        if (help != null) graphics.renderTooltip(font, font.split(Component.translatable("gui.useless_stretcher.window." + help), 230), x, y);
    }

    private static void drawControl(GuiGraphics graphics, int x, int y, boolean restore, int mx, int my) {
        boolean hovered = inside(mx, my, x, y, 14, 14);
        if (com.sorrowmist.useless.client.gui.StaffGlass.aeStyle()) {
            if (hovered) graphics.fill(x, y, x + 14, y + 14, 0xFFF2F2F2);
        } else com.sorrowmist.useless.client.gui.StaffGlass.rounded(graphics, x, y, 14, 14, 7,
                restore ? (hovered ? 0xC87D9BB2 : 0x706B8399) : (hovered ? 0xF0F07583 : 0xD7D76274));
        int color = StretcherScreenStyle.textColor();
        if (restore) {
            graphics.renderOutline(x + 5, y + 3, 6, 6, color);
            graphics.fill(x + 3, y + 5, x + 9, y + 11, StretcherScreenStyle.slotColor());
            graphics.renderOutline(x + 3, y + 5, 6, 6, color);
        } else {
            com.sorrowmist.uselesscore.client.ui.UiDraw.line(graphics, x + 4, y + 4, x + 10, y + 10, .9f, color);
            com.sorrowmist.uselesscore.client.ui.UiDraw.line(graphics, x + 10, y + 4, x + 4, y + 10, .9f, color);
        }
    }
    private int edgeAt(double x, double y) {
        if (!inside(x, y, left - 1, top - 1, width + 2, height + 2)) return 0;
        int result = 0;
        if (x < left + EDGE) result |= LEFT;
        else if (x >= left + width - EDGE) result |= RIGHT;
        if (y < top + EDGE) result |= TOP;
        else if (y >= top + height - EDGE) result |= BOTTOM;
        return result;
    }
    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
    private int maxWidth() { return Math.max(1, viewportWidth - MARGIN * 2); }
    private int maxHeight() { return Math.max(1, viewportHeight - MARGIN * 2); }
    private int effectiveMinWidth() { return Math.min(minWidth, maxWidth()); }
    private int effectiveMinHeight() { return Math.min(minHeight, maxHeight()); }
    private static double fraction(int value, int maximum) { return maximum <= 0 ? 0.5 : Mth.clamp((double) value / maximum, 0, 1); }

    private static void loadLayouts() {
        Path path = Minecraft.getInstance().gameDirectory.toPath().resolve("config/useless_stretcher-windows.json");
        if (path.equals(loadedPath)) return;
        loadedPath = path;
        SAVED.clear();
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > 65536) return;
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            for (var entry : root.entrySet()) {
                try {
                    JsonObject value = entry.getValue().getAsJsonObject();
                    double x = value.get("x").getAsDouble(), y = value.get("y").getAsDouble();
                    int width = value.get("width").getAsInt(), height = value.get("height").getAsInt();
                    if (Double.isFinite(x) && Double.isFinite(y) && width > 0 && height > 0) {
                        Saved saved = new Saved(Mth.clamp(x, 0, 1), Mth.clamp(y, 0, 1), width, height);
                        if (value.has("prefs")) {
                            for (var pref : value.getAsJsonObject("prefs").entrySet()) {
                                try { saved.prefs.put(pref.getKey(), pref.getValue().getAsString()); }
                                catch (RuntimeException ignored) { /* Skip only malformed preference values. */ }
                            }
                        }
                        SAVED.put(entry.getKey(), saved);
                    }
                } catch (RuntimeException ignored) { /* Skip only the malformed entry. */ }
            }
        } catch (Exception exception) { LogUtils.getLogger().warn("Could not read Stretcher window positions", exception); }
    }

    public void save() {
        if (!initialized || !dirty) return;
        loadLayouts();
        Saved saved = new Saved(fraction(left - MARGIN, viewportWidth - width - MARGIN * 2),
                fraction(top - MARGIN, viewportHeight - height - MARGIN * 2), width, height);
        Saved previous = SAVED.get(key);
        if (previous != null) saved.prefs.putAll(previous.prefs);
        SAVED.put(key, saved);
        Path temporary = null;
        try {
            Files.createDirectories(loadedPath.getParent());
            temporary = Files.createTempFile(loadedPath.getParent(), "useless_stretcher-windows-", ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(SAVED));
            try { Files.move(temporary, loadedPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temporary, loadedPath, StandardCopyOption.REPLACE_EXISTING); }
            dirty = false;
        } catch (Exception exception) { LogUtils.getLogger().warn("Could not save Stretcher window positions", exception); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (java.io.IOException ignored) {} }
    }
    private static final class Saved {
        double x, y;
        int width, height;
        Map<String, String> prefs = new java.util.LinkedHashMap<>();

        Saved() { }

        Saved(double x, double y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    /** Remembers the active tab, folded sidebar and expanded sections next to the layout. */
    public String preference(String key, String fallback) {
        loadLayouts();
        Saved saved = SAVED.get(this.key);
        String value = saved == null ? null : saved.prefs.get(key);
        return value == null ? fallback : value;
    }

    public void setPreference(String key, String value) {
        loadLayouts();
        Saved saved = SAVED.get(this.key);
        if (saved == null) {
            saved = new Saved(fraction(left - MARGIN, viewportWidth - width - MARGIN * 2),
                    fraction(top - MARGIN, viewportHeight - height - MARGIN * 2), width, height);
            SAVED.put(this.key, saved);
        }
        saved.prefs.put(key, value);
        dirty = true;
    }
}
