package com.sorrowmist.useless.stretcher.client;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.useless.stretcher.client.gui.AE2RangeSlider;
import com.sorrowmist.useless.stretcher.client.gui.ButtonHelp;
import com.sorrowmist.useless.stretcher.client.gui.FloatingScreen;
import com.sorrowmist.useless.stretcher.client.gui.SelectableAE2Button;
import com.sorrowmist.useless.stretcher.client.gui.StretcherScreenStyle;
import com.sorrowmist.useless.stretcher.content.apotheosis.ApotheosisStaffSettings;
import com.sorrowmist.useless.stretcher.content.apotheosis.ApotheosisTableData;
import com.sorrowmist.useless.stretcher.init.ModItems;
import com.sorrowmist.useless.stretcher.network.Network;
import com.sorrowmist.useless.stretcher.network.ApotheosisNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Virtual-bookshelf editor for Apotheosis. */
public final class WondrousStaffApotheosisScreen extends StaffScreen {

    private final Screen parent;
    private final InteractionHand hand;
    private int panelLeft;
    private int panelTop;
    private int eterna;
    private int quanta;
    private int arcana;
    private int clues;
    private boolean initialized;
    private boolean selectionMode;
    private boolean dirty;
    private int saveDelay;
    private ApotheosisTableData.Boost lastSent;
    private boolean edited;
    private SelectableAE2Button selectionButton;

    public WondrousStaffApotheosisScreen(Screen parent, InteractionHand hand) {
        super(Component.translatable("gui.useless_stretcher.staff_apotheosis.title"),
                StaffScreen.Tab.APOTHEOSIS, parent, hand);
        this.parent = parent;
        this.hand = hand;
    }

    @Override
    protected void initContent() {
        if (!initialized) {
            initialized = true;
            ItemStack staff = currentStaff();
            loadBoost(ApotheosisStaffSettings.boost(staff));
            lastSent = currentBoost();
            selectionMode = ApotheosisStaffSettings.selectionMode(staff);
            if (minecraft.getConnection() != null) ApotheosisNetwork.requestState(hand);
        }
        panelLeft = contentLeft();
        panelTop = contentTop();
        int panelWidth = contentWidth();

        int rowsTop = panelTop + 19;
        int rowWidth = panelWidth - 8;
        addValueSlider(rowsTop, rowWidth, "eterna", 0, 100, eterna, value -> eterna = value);
        addValueSlider(rowsTop + 22, rowWidth, "quanta", 0, 100, quanta, value -> quanta = value);
        addValueSlider(rowsTop + 44, rowWidth, "arcana", 0, 100, arcana, value -> arcana = value);
        addValueSlider(rowsTop + 66, rowWidth, "clues", 0, 15, clues, value -> clues = value);

        int footerY = rowsTop + 92;
        int gap = 3;
        int buttonWidth = (panelWidth - gap * 2) / 3;
        SelectableAE2Button select = addRenderableWidget(new SelectableAE2Button(
                panelLeft, footerY, buttonWidth, 18,
                Component.translatable("gui.useless_stretcher.staff_apotheosis.select_table"), ignored -> {
                    if (!com.sorrowmist.useless.stretcher.config.StretcherConfig.enableApotheosisCompat()) return;
                    selectionMode = !selectionMode;
                    edited = true;
                    save(true);
                    if (selectionMode) minecraft.setScreen(null);
                    else rebuildWidgets();
                }));
        select.setSelected(selectionMode);
        selectionButton = select;
        updateSelectionButton();
        SelectableAE2Button save = addRenderableWidget(new SelectableAE2Button(
                panelLeft + buttonWidth + gap, footerY, buttonWidth, 18,
                Component.translatable("gui.useless_stretcher.staff_apotheosis.save"), ignored ->
                save(true)));
        ButtonHelp.set(save, "apotheosis_save");
        addRenderableWidget(new SelectableAE2Button(
                panelLeft + (buttonWidth + gap) * 2, footerY, panelWidth - (buttonWidth + gap) * 2, 18,
                Component.translatable("gui.useless_stretcher.back"), ignored -> onClose()));
        setContentExtent(footerY - panelTop + 21);
    }

    private void updateSelectionButton() {
        if (selectionButton == null) return;
        selectionButton.active = com.sorrowmist.useless.stretcher.config.StretcherConfig.enableApotheosisCompat();
        selectionButton.setSelected(selectionButton.active && selectionMode);
        if (selectionButton.active) ButtonHelp.set(selectionButton, "apotheosis_select_table");
        else selectionButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(
                "gui.useless_stretcher.staff_apotheosis.enable_config_hint")));
    }

    private void addValueSlider(int y, int rowWidth, String key, int min, int max, int initial,
                                java.util.function.IntConsumer changed) {
        int x = panelLeft + 4;
        int sliderWidth = rowWidth - 48;
        AE2RangeSlider slider = addRenderableWidget(new AE2RangeSlider(x + 20, y, sliderWidth, 18,
                Component.translatable("gui.useless_stretcher.staff_apotheosis.stat." + key).getString(),
                min, max, initial, value -> {
                    changed.accept(value);
                    edited = true;
                    if (!dirty) saveDelay = 4;
                    dirty = true;
                }));
        SelectableAE2Button decrease = addRenderableWidget(new SelectableAE2Button(x, y, 18, 18,
                Component.literal("-"), ignored -> slider.step(-1)));
        SelectableAE2Button increase = addRenderableWidget(new SelectableAE2Button(
                x + rowWidth - 22, y, 18, 18, Component.literal("+"), ignored -> slider.step(1)));
        ButtonHelp.set(decrease, "decrease");
        ButtonHelp.set(increase, "increase");
        ButtonHelp.set(slider, "apotheosis_stat", Component.translatable(
                "gui.useless_stretcher.staff_apotheosis.stat." + key));
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(font, Component.translatable("gui.useless_stretcher.staff_apotheosis.stats"),
                panelLeft + 4, panelTop + 4, StretcherScreenStyle.subtleColor(), false);
    }

    private ApotheosisTableData.Boost currentBoost() {
        return new ApotheosisTableData.Boost(eterna, quanta, arcana, clues).normalized();
    }

    private void loadBoost(ApotheosisTableData.Boost boost) {
        eterna = boost.eterna();
        quanta = boost.quanta();
        arcana = boost.arcana();
        clues = boost.clues();
    }

    public void acceptState(ApotheosisNetwork.SettingsPayload state) {
        if (state.offhand() != (hand == InteractionHand.OFF_HAND)) return;
        // Do not let a delayed server acknowledgment overwrite a newer slider edit.
        if (!edited) {
            var boost = state.boost();
            boolean rebuild = !boost.equals(currentBoost()) || selectionMode != state.selecting();
            loadBoost(boost);
            lastSent = boost;
            selectionMode = state.selecting();
            if (rebuild) rebuildWidgets();
        }
    }

    private void save(boolean force) {
        var boost = currentBoost();
        if (!force && (!dirty || boost.equals(lastSent))) { dirty = false; return; }
        ItemStack staff = currentStaff();
        if (staff.isEmpty()) return;
        ApotheosisStaffSettings.setBoost(staff, boost);
        ApotheosisStaffSettings.setSelectionMode(staff, selectionMode);
        if (selectionMode) {
            CoreItemComponents.set(staff, com.sorrowmist.uselesscore.api.component.UselessStretcherComponents.RANGE_FILTER_MARKING_MODE.get(), false);
            CoreItemComponents.set(staff, com.sorrowmist.uselesscore.api.component.UselessStretcherComponents.RANGE_PLACEMENT_MODE.get(), false);
        }
        Network.sendApotheosisOptions(selectionMode, boost, hand);
        lastSent = boost;
        dirty = false;
    }

    @Override public void tick() {
        super.tick();
        if (selectionButton != null && selectionButton.active != com.sorrowmist.useless.stretcher.config.StretcherConfig.enableApotheosisCompat()) updateSelectionButton();
        if (dirty && --saveDelay <= 0) save(false);
    }

    @Override public void removed() {
        save(false);
        super.removed();
    }

    private ItemStack currentStaff() {
        if (minecraft == null || minecraft.player == null) return ItemStack.EMPTY;
        ItemStack held = minecraft.player.getItemInHand(hand);
        return held.is(ModItems.WONDROUS_STAFF.get()) ? held : ItemStack.EMPTY;
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
