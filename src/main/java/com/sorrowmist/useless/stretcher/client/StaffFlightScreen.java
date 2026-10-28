package com.sorrowmist.useless.stretcher.client;

import com.sorrowmist.useless.stretcher.client.gui.*;
import com.sorrowmist.useless.stretcher.content.item.StaffUtilitySettings;
import com.sorrowmist.useless.stretcher.network.StaffUtilityNetwork;
import com.sorrowmist.useless.stretcher.UselessStretcherMod;
import com.sorrowmist.useless.stretcher.init.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

public final class StaffFlightScreen extends StaffScreen {
    private final Screen parent; private final InteractionHand hand;
    private double speed=1,inertia=1; private boolean loaded;
    public StaffFlightScreen(Screen parent,InteractionHand hand) {
        super(Component.translatable("gui.useless_stretcher.flight.title"),StaffScreen.Tab.FLIGHT,parent,hand);
        this.parent=parent; this.hand=hand;
    }
    @Override protected void initContent() {
        if (minecraft.player==null) return;
        if (!loaded) {
            var stack=minecraft.player.getItemInHand(hand);
            speed=StaffUtilitySettings.speed(stack); inertia=StaffUtilitySettings.inertia(stack); loaded=true;
        }
        addRenderableWidget(new FlightSlider(contentLeft(),contentTop()+3,contentWidth(),true));
        addRenderableWidget(new FlightSlider(contentLeft(),contentTop()+30,contentWidth(),false));
        addRenderableWidget(new SelectableAE2Button(contentLeft(),contentTop()+85,contentWidth()/2-2,20,
                Component.translatable("controls.reset"),b->{speed=1;inertia=1;save();rebuildWidgets();}));
        addRenderableWidget(new SelectableAE2Button(contentLeft()+contentWidth()/2+2,contentTop()+85,contentWidth()/2-2,20,
                Component.translatable("gui.done"),b->onClose()));
        setContentExtent(112);
    }
    private void save() {
        if (minecraft.player==null) return;
        var stack=minecraft.player.getItemInHand(hand);
        if (!stack.is(ModItems.WONDROUS_STAFF.get())) return;
        StaffUtilitySettings.flight(stack,speed,inertia);
        UselessStretcherMod.NETWORK.sendToServer(new StaffUtilityNetwork.Settings(hand==InteractionHand.OFF_HAND,
                StaffUtilitySettings.dismantle(stack),speed,inertia));
    }
    @Override public void onClose() { save(); minecraft.setScreen(parent); }
    @Override protected void renderContent(GuiGraphics graphics,int x,int y,float delta) {
        graphics.drawWordWrap(font,Component.translatable("gui.useless_stretcher.flight.hint"),contentLeft(),contentTop()+57,contentWidth(),0xD0D0D0);
    }
    private final class FlightSlider extends AbstractSliderButton {
        private final boolean speedSlider;
        FlightSlider(int x,int y,int width,boolean speedSlider) {
            super(x,y,width,20,Component.empty(),speedSlider?(Math.log(speed)/Math.log(10)+1)/2:inertia);
            this.speedSlider=speedSlider;updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(speedSlider?Component.translatable("gui.useless_stretcher.flight.speed",String.format(java.util.Locale.ROOT,"%.2f",speed))
                    :Component.translatable("gui.useless_stretcher.flight.inertia",(int)Math.round(inertia*100)));
        }
        @Override protected void applyValue() {
            if (speedSlider) speed=Math.round(Math.pow(10,value*2-1)*100)/100.0;
            else inertia=Math.round(value*100)/100.0;
            updateMessage();
        }
        @Override public void onRelease(double x,double y) { super.onRelease(x,y);save(); }
    }
}
