package com.npcmod.client.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Botón con estética de fantasía. */
public class FButton extends Button {
    private boolean selected;

    public FButton(int x, int y, int w, int h, Component label, OnPress onPress) {
        super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
    }

    public FButton selected(boolean s) { this.selected = s; return this; }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
        boolean hov = this.isHoveredOrFocused() && this.active;
        int top = !this.active ? 0xFF1A1230 : selected ? 0xFF5A38A8 : hov ? 0xFF4A2D8F : 0xFF2A1858;
        int bot = !this.active ? 0xFF120C22 : selected ? 0xFF3A2278 : hov ? 0xFF2E1A66 : 0xFF190E38;
        g.fillGradient(getX(), getY(), getX() + width, getY() + height, top, bot);
        int border = !this.active ? Fantasy.GOLD_DARK : (selected || hov) ? Fantasy.GOLD_LIGHT : Fantasy.GOLD;
        Fantasy.frame(g, getX(), getY(), width, height, border);
        var font = Minecraft.getInstance().font;
        int col = !this.active ? Fantasy.TEXT_DIM : (selected || hov) ? Fantasy.GOLD_LIGHT : Fantasy.TEXT;
        String s = font.plainSubstrByWidth(getMessage().getString(), width - 8);
        g.drawString(font, s, getX() + (width - font.width(s)) / 2, getY() + (height - 8) / 2, col, true);
    }
}
