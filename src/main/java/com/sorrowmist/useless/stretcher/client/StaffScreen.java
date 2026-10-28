package com.sorrowmist.useless.stretcher.client;

import com.sorrowmist.useless.stretcher.client.gui.FloatingScreen;
import com.sorrowmist.useless.stretcher.client.gui.SelectableAE2Button;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/** Staff pages behave as browser tabs inside one shared, persistent floating window. */
public abstract class StaffScreen extends FloatingScreen {
    public enum Tab { MAIN, ENTITY, RANGE, HISTORY, SUMMON, APOTHEOSIS, MACHINES, PHASE, FLIGHT }

    private final Tab tab;
    private final Session session;
    private final List<AbstractWidget> sidebar = new ArrayList<>();

    protected StaffScreen(Component title, Tab tab, Screen parent, InteractionHand hand) {
        this(title, tab, parent, hand, "staff", 440, 280, 340, 170);
    }

    /** Shared history and reclaimer components can also serve as standalone windows. */
    protected StaffScreen(Component title, Tab tab, Screen parent, InteractionHand hand,
                          String standaloneKey, int width, int height, int minWidth, int minHeight) {
        super(title, tab == null ? standaloneKey : "staff",
                tab == null ? width : 440, tab == null ? height : 280,
                tab == null ? minWidth : 340, tab == null ? minHeight : 170);
        this.tab = tab;
        this.session = parent instanceof StaffScreen staff ? staff.session
                : new Session(hand == null ? InteractionHand.MAIN_HAND : hand);
        if (tab != null) session.pages.put(tab, this);
    }

    public static InteractionHand staffHand(Screen parent) {
        return parent instanceof StaffScreen staff ? staff.session.hand : InteractionHand.MAIN_HAND;
    }

    public static void open(InteractionHand hand) {
        WondrousStaffConfigScreen root = new WondrousStaffConfigScreen(null, hand);
        Tab selected;
        try { selected = Tab.valueOf(root.floatingWindow().preference("activeTab", "MAIN")); }
        catch (IllegalArgumentException ignored) { selected = Tab.MAIN; }
        if (selected == Tab.APOTHEOSIS && !com.sorrowmist.useless.stretcher.config.StretcherConfig.hasApotheosisConfig()) selected = Tab.MAIN;
        Minecraft.getInstance().setScreen(selected == Tab.MAIN ? root : root.page(selected));
    }

    public final Tab staffTab() { return tab; }

    public final boolean sidebarCollapsed() {
        return Boolean.parseBoolean(floatingWindow().preference("sidebarCollapsed", "false"));
    }

    @Override protected final int sidebarWidth() { return tab == null ? 0 : sidebarCollapsed() ? 26 : 92; }

    /** Optional one-line toggle pinned above the tab strip's bottom edge. */
    protected Component sidebarFooterText() { return null; }
    protected void sidebarFooterClicked() { }

    @Override protected final void initChrome() {
        sidebar.clear();
        if (tab == null) return;
        floatingWindow().setPreference("activeTab", tab.name());
        int left = floatingWindow().bodyLeft() + 2;
        int top = floatingWindow().bodyTop() + 2;
        int width = sidebarWidth() - 6;
        var fold = new SelectableAE2Button(left, top, width, 16,
                Component.literal(sidebarCollapsed() ? "»" : "«"), ignored -> {
                    floatingWindow().setPreference("sidebarCollapsed", Boolean.toString(!sidebarCollapsed()));
                    floatingWindow().save();
                    rebuildWidgets();
                });
        fold.setTooltip(Tooltip.create(Component.translatable("gui.useless_stretcher.staff_tabs.fold")));
        sidebar.add(addWidget(fold));
        List<Tab> tabs = new ArrayList<>(List.of(Tab.values()));
        if (!com.sorrowmist.useless.stretcher.config.StretcherConfig.hasApotheosisConfig()) tabs.remove(Tab.APOTHEOSIS);
        Component footer = sidebarFooterText();
        int footerSpace = footer == null ? 0 : 21;
        int rowHeight = Math.max(11, Math.min(23, (floatingWindow().bodyHeight() - 21 - footerSpace) / Math.max(1, tabs.size())));
        for (int index = 0; index < tabs.size(); index++) {
            Tab choice = tabs.get(index);
            Component label = Component.translatable("gui.useless_stretcher.staff_tabs." + choice.name().toLowerCase(Locale.ROOT));
            var button = new SelectableAE2Button(left, top + 19 + rowHeight * index, width, rowHeight - 1,
                    sidebarCollapsed() ? Component.literal(Integer.toString(index + 1)) : label,
                    ignored -> navigate(choice));
            button.setSelected(choice == tab);
            button.setTooltip(Tooltip.create(label));
            sidebar.add(addWidget(button));
        }
        if (footer != null) {
            var toggle = new SelectableAE2Button(left, floatingWindow().bodyTop() + floatingWindow().bodyHeight() - 21,
                    width, 17, footer, ignored -> { sidebarFooterClicked(); rebuildWidgets(); });
            toggle.setTooltip(Tooltip.create(Component.translatable("gui.useless_stretcher.dismantle.hint")));
            sidebar.add(addWidget(toggle));
        }
    }

    @Override protected final void renderChrome(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (tab == null) return;
        int right = contentLeft() - 3;
        graphics.fill(right, floatingWindow().bodyTop() + 2, right + 1,
                floatingWindow().bodyTop() + floatingWindow().bodyHeight() - 2, 0x66203044);
        graphics.enableScissor(floatingWindow().bodyLeft(), floatingWindow().bodyTop(), contentLeft(),
                floatingWindow().bodyTop() + floatingWindow().bodyHeight());
        try {
            for (AbstractWidget widget : sidebar) widget.render(graphics,
                    floatingWindow().capturing() ? -10000 : mouseX, mouseY, partialTick);
        } finally { graphics.disableScissor(); }
    }

    public final void navigate(Tab selected) {
        if (selected == tab) return;
        floatingWindow().save();
        minecraft.setScreen(page(selected));
    }

    final StaffScreen page(Tab selected) {
        StaffScreen existing = session.pages.get(selected);
        if (existing != null) return existing;
        StaffScreen root = session.pages.getOrDefault(Tab.MAIN, this);
        return switch (selected) {
            case MAIN -> new WondrousStaffConfigScreen(root, session.hand);
            case ENTITY -> new EntityAccelerationConfigScreen(root, session.hand);
            case RANGE -> new RangeAccelerationConfigScreen(root, session.hand);
            case HISTORY -> new RangeAccelerationHistoryScreen(root);
            case SUMMON -> new WondrousStaffSummonScreen(root, session.hand);
            case APOTHEOSIS -> new WondrousStaffApotheosisScreen(root, session.hand);
            case MACHINES -> new ReclaimerScreen(root, List.of(), true);
            case PHASE -> new StaffPhaseScreen(root, session.hand);
            case FLIGHT -> new StaffFlightScreen(root, session.hand);
        };
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (tab != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { minecraft.setScreen(null); return true; }
            if (keyCode == GLFW.GLFW_KEY_TAB && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                List<Tab> tabs = new ArrayList<>(List.of(Tab.values()));
                if (!com.sorrowmist.useless.stretcher.config.StretcherConfig.hasApotheosisConfig()) tabs.remove(Tab.APOTHEOSIS);
                int direction = (modifiers & GLFW.GLFW_MOD_SHIFT) == 0 ? 1 : -1;
                navigate(tabs.get(Math.floorMod(tabs.indexOf(tab) + direction, tabs.size())));
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static final class Session {
        final InteractionHand hand;
        final EnumMap<Tab, StaffScreen> pages = new EnumMap<>(Tab.class);

        Session(InteractionHand hand) { this.hand = hand; }
    }
}