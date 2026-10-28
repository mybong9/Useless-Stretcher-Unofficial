package com.sorrowmist.useless.stretcher.client;


import com.sorrowmist.uselesscore.api.component.CoreItemComponents;
import com.sorrowmist.uselesscore.api.component.UselessComponents;
import com.sorrowmist.useless.stretcher.client.gui.SelectableAE2Button;
import com.sorrowmist.useless.stretcher.client.gui.StretcherScreenStyle;
import com.sorrowmist.useless.stretcher.client.gui.FloatingScreen;
import com.sorrowmist.useless.stretcher.content.entity.WondrousStaffAcceleration;
import com.sorrowmist.useless.stretcher.content.apotheosis.ApotheosisStaffSettings;
import com.sorrowmist.useless.stretcher.config.StretcherConfig;
import com.sorrowmist.useless.stretcher.init.ModItems;
import com.sorrowmist.uselesscore.api.component.UselessStretcherComponents;
import com.sorrowmist.useless.stretcher.network.Network;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Button-based acceleration selector styled after Useless Mod's current G configuration screen. */
public final class WondrousStaffConfigScreen extends StaffScreen {
    private static final int[] GEARS = {0, 2, 4, 16, 32, 64, 128, 256, 512, 1024};

    private final InteractionHand hand;
    private final List<ChoiceButton> speedButtons = new ArrayList<>();
    private final List<ChoiceButton> modeButtons = new ArrayList<>();
    private SelectableAE2Button accelerationButton;
    private SelectableAE2Button lootRefreshButton;
    private SelectableAE2Button summonOpenButton;
    private SelectableAE2Button apotheosisOpenButton;
    private SelectableAE2Button dismantleButton;
    private int panelLeft;
    private int panelTop;
    private int selectedSpeed;
    private int selectedMode;
    private boolean accelerationEnabled;
    private boolean summonEnabled;
    private boolean lootRefreshEnabled;
    private boolean settingsLoaded;
    private int speedTop, speedHeight, modeTop, modeHeight, featuresTop, featuresHeight;

    public WondrousStaffConfigScreen(Screen parent, InteractionHand hand) {
        super(Component.translatable("gui.useless_stretcher.staff_config.title"), StaffScreen.Tab.MAIN, parent, hand);
        this.hand = hand;
    }

    @Override
    protected void initContent() {
        panelLeft = contentLeft();
        panelTop = contentTop();
        int panelWidth = contentWidth();
        if (!settingsLoaded) {
            ItemStack staff = currentStaff();
            selectedSpeed = WondrousStaffAcceleration.getSpeed(staff);
            selectedMode = WondrousStaffAcceleration.getMode(staff);
            accelerationEnabled = WondrousStaffAcceleration.isEnabled(staff);
            summonEnabled = WondrousStaffAcceleration.isSummonEnabled(staff);
            lootRefreshEnabled = WondrousStaffAcceleration.isLootRefreshEnabled(staff);
            settingsLoaded = true;
        }

        speedButtons.clear();
        modeButtons.clear();
        accelerationButton = addRenderableWidget(new SelectableAE2Button(
                panelLeft + panelWidth - 104, panelTop, 104, 18,
                accelerationMessage(), ignored -> toggleAcceleration()));

        int cardLeft = panelLeft;
        int cardWidth = panelWidth;
        int gap = 6;
        int columns = Math.max(2, Math.min(5, (cardWidth - 16 + gap) / 54));
        int buttonWidth = (cardWidth - 16 - gap * (columns - 1)) / columns;
        speedTop = panelTop + 28;
        speedHeight = 25 + ((GEARS.length + columns - 1) / columns) * 28;
        for (int i = 0; i < GEARS.length; i++) {
            int speed = GEARS[i];
            int x = cardLeft + 8 + (i % columns) * (buttonWidth + gap);
            int y = speedTop + 22 + (i / columns) * 28;
            Component label = speed == 0
                    ? Component.translatable("gui.useless_stretcher.speed_off")
                    : Component.literal("x" + speed);
            SelectableAE2Button button = addRenderableWidget(new SelectableAE2Button(
                    x, y, buttonWidth, 22, label, ignored -> selectSpeed(speed)));
            speedButtons.add(new ChoiceButton(speed, button));
        }

        String[] modeKeys = {
                "gui.useless_stretcher.staff_config.normal_short",
                "gui.useless_stretcher.staff_config.permanent_short",
                "gui.useless_stretcher.mode_no_throttle_short"
        };
        modeTop = speedTop + speedHeight + 8;
        int modeColumns = cardWidth >= 240 ? 3 : 1;
        modeHeight = 25 + ((modeKeys.length + modeColumns - 1) / modeColumns) * 28;
        int modeButtonWidth = (cardWidth - 16 - (modeColumns - 1) * gap) / modeColumns;
        for (int mode = 0; mode < modeKeys.length; mode++) {
            int value = mode;
            SelectableAE2Button button = addRenderableWidget(new SelectableAE2Button(
                    cardLeft + 8 + (mode % modeColumns) * (modeButtonWidth + gap),
                    modeTop + 22 + (mode / modeColumns) * 28, modeButtonWidth, 22,
                    Component.translatable(modeKeys[mode]), ignored -> selectMode(value)));
            modeButtons.add(new ChoiceButton(mode, button));
            com.sorrowmist.useless.stretcher.client.gui.ButtonHelp.set(button,
                    mode == 0 ? "normal" : mode == 1 ? "permanent" : "no_throttle");
        }
        featuresTop = modeTop + modeHeight + 8;
        int featureColumns = cardWidth >= 270 ? 2 : 1;
        int featureWidth = (cardWidth - 16 - gap * (featureColumns - 1)) / featureColumns;
        featuresHeight = 25 + ((2 + featureColumns - 1) / featureColumns) * 28;
        lootRefreshButton = addRenderableWidget(new SelectableAE2Button(
                cardLeft + 8, featuresTop + 22, featureWidth, 22,
                lootRefreshMessage(), ignored -> toggleLootRefresh()));
        lootRefreshButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.useless_stretcher.staff_config.loot_refresh_help")));
        dismantleButton = addRenderableWidget(new SelectableAE2Button(
                cardLeft + 8 + (featureColumns == 2 ? featureWidth + gap : 0),
                featuresTop + 22 + (featureColumns == 2 ? 0 : 28), featureWidth, 22,
                dismantleLabel(com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings.dismantle(currentStaff())),
                ignored -> toggleDismantle()));
        dismantleButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                Component.translatable("gui.useless_stretcher.dismantle.hint")));

        updateSelection();
        setContentExtent(featuresTop - panelTop + featuresHeight + 6);
    }

    private static Component dismantleLabel(boolean enabled) {
        return Component.translatable("gui.useless_stretcher.dismantle.toggle").append(": ")
                .append(Component.translatable(enabled?"options.on":"options.off"));
    }

    @Override
    protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cardLeft = panelLeft;
        int cardRight = panelLeft + contentWidth();
        StretcherScreenStyle.drawInset(graphics, cardLeft, speedTop, cardRight, speedTop + speedHeight);
        graphics.drawString(font, Component.translatable("gui.useless_stretcher.staff_config.speed"),
                cardLeft + 5, speedTop + 5, StretcherScreenStyle.textColor(), false);
        StretcherScreenStyle.drawInset(graphics, cardLeft, modeTop, cardRight, modeTop + modeHeight);
        graphics.drawString(font, Component.translatable("gui.useless_stretcher.staff_config.duration"),
                cardLeft + 5, modeTop + 5, StretcherScreenStyle.textColor(), false);
        StretcherScreenStyle.drawInset(graphics, cardLeft, featuresTop, cardRight, featuresTop + featuresHeight);
        graphics.drawString(font, Component.translatable("gui.useless_stretcher.staff_config.features"),
                cardLeft + 5, featuresTop + 5, StretcherScreenStyle.textColor(), false);
    }

    private void selectSpeed(int speed) {
        selectedSpeed = speed;
        ItemStack staff = currentStaff();
        if (!staff.is(ModItems.WONDROUS_STAFF.get())) return;
        CoreItemComponents.set(staff, UselessStretcherComponents.WONDROUS_STAFF_SPEED.get(), speed);
        sendSelection();
    }

    private void selectMode(int mode) {
        selectedMode = mode;
        ItemStack staff = currentStaff();
        if (!staff.is(ModItems.WONDROUS_STAFF.get())) return;
        WondrousStaffAcceleration.setMode(staff, mode);
        sendSelection();
    }

    private void toggleAcceleration() {
        ItemStack staff = currentStaff();
        if (!staff.is(ModItems.WONDROUS_STAFF.get())) return;
        accelerationEnabled = !accelerationEnabled;
        CoreItemComponents.set(staff, UselessComponents.BeefTimeAccelerationEnabledComponent.get(), accelerationEnabled);
        sendSelection();
    }

    private void toggleLootRefresh() {
        ItemStack staff = currentStaff();
        if (!staff.is(ModItems.WONDROUS_STAFF.get()) || !StretcherConfig.enableStaffLootRefresh()) return;
        summonEnabled = WondrousStaffAcceleration.isSummonEnabled(staff);
        lootRefreshEnabled = !WondrousStaffAcceleration.isLootRefreshEnabled(staff);
        CoreItemComponents.set(staff, UselessStretcherComponents.WONDROUS_STAFF_LOOT_REFRESH.get(), lootRefreshEnabled);
        Network.sendWondrousStaffFeatures(summonEnabled, lootRefreshEnabled, hand);
        updateSelection();
    }

    private void sendSelection() {
        updateSelection();
        Network.sendWondrousStaffSpeed(selectedSpeed, selectedMode, accelerationEnabled, hand);
    }

    private void updateSelection() {
        if (dismantleButton != null) {
            boolean enabled = com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings.dismantle(currentStaff());
            dismantleButton.setMessage(dismantleLabel(enabled));
            dismantleButton.setSelected(enabled);
        }
        boolean allowAcceleration = StretcherConfig.serverStaffAcceleration();
        speedButtons.forEach(entry -> entry.button().active = allowAcceleration);
        modeButtons.forEach(entry -> entry.button().active = allowAcceleration);
        speedButtons.forEach(entry -> entry.button().setSelected(entry.value() == selectedSpeed));
        modeButtons.forEach(entry -> entry.button().setSelected(entry.value() == selectedMode));
        if (accelerationButton != null) {
            accelerationButton.active = allowAcceleration;
            accelerationButton.setSelected(accelerationEnabled);
            accelerationButton.setMessage(accelerationMessage());
        }
        if (lootRefreshButton != null) {
            boolean configEnabled = StretcherConfig.enableStaffLootRefresh();
            lootRefreshButton.active = configEnabled;
            lootRefreshButton.setSelected(configEnabled && lootRefreshEnabled);
            lootRefreshButton.setMessage(configEnabled
                    ? lootRefreshMessage()
                    : Component.translatable("gui.useless_stretcher.staff_config.loot_refresh_disabled"));
        }
        if (summonOpenButton != null) {
            boolean configEnabled = StretcherConfig.enableStaffSummon();
            summonOpenButton.visible = true;
            summonOpenButton.active = configEnabled;
            summonOpenButton.setMessage(configEnabled
                    ? summonOpenMessage()
                    : Component.translatable("gui.useless_stretcher.staff_summon.disabled"));
        }
        if (apotheosisOpenButton != null) {
            boolean configEnabled = StretcherConfig.enableApotheosisCompat();
            apotheosisOpenButton.active = configEnabled && ApotheosisStaffSettings.isApotheosisLoaded();
            apotheosisOpenButton.setMessage(configEnabled
                    ? apotheosisOpenMessage()
                    : Component.translatable("gui.useless_stretcher.staff_apotheosis.disabled"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateSelection(); // Reflect live server config updates while this screen stays open.
    }

    private Component accelerationMessage() {
        return Component.translatable("gui.useless_stretcher.staff_config.master",
                Component.translatable(accelerationEnabled
                        ? "gui.useless_stretcher.staff_config.on"
                        : "gui.useless_stretcher.staff_config.off"));
    }

    private Component summonOpenMessage() {
        return Component.translatable("gui.useless_stretcher.staff_summon.open");
    }

    private Component apotheosisOpenMessage() {
        return Component.translatable("gui.useless_stretcher.staff_apotheosis.open");
    }

    private Component lootRefreshMessage() {
        return Component.translatable("gui.useless_stretcher.staff_config.loot_refresh_short",
                Component.translatable(lootRefreshEnabled ? "gui.useless_stretcher.staff_config.on"
                        : "gui.useless_stretcher.staff_config.off"));
    }

    private void toggleDismantle() {
        var selected = currentStaff();
        if (!selected.is(ModItems.WONDROUS_STAFF.get())) return;
        boolean enabled = !com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings.dismantle(selected);
        com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings.setDismantle(selected, enabled);
        com.sorrowmist.useless.stretcher.UselessStretcherMod.NETWORK.sendToServer(
                new com.sorrowmist.useless.stretcher.network.StaffUtilityNetwork.Settings(
                        hand == InteractionHand.OFF_HAND, enabled,
                        com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings.speed(selected),
                        com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings.inertia(selected)));
    }

    private ItemStack currentStaff() {
        if (minecraft == null || minecraft.player == null) return ItemStack.EMPTY;
        return minecraft.player.getItemInHand(hand);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record ChoiceButton(int value, SelectableAE2Button button) {
    }
}
