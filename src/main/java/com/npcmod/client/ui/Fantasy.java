package com.npcmod.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/** Dibujo de paneles y adornos con estética de fantasía (oro, púrpura profundo, destellos). */
public final class Fantasy {
    public static final int GOLD = 0xFFD4AF37;
    public static final int GOLD_LIGHT = 0xFFFFE27A;
    public static final int GOLD_DARK = 0xFF8A6A12;
    public static final int PURPLE_DARK = 0xF00E0620;
    public static final int PURPLE = 0xF01C0F3A;
    public static final int PURPLE_LIGHT = 0xFF3B2470;
    public static final int TEXT = 0xFFF4EBFF;
    public static final int TEXT_DIM = 0xFFA89BC9;

    /** Panel con degradado y marco dorado doble con esquinas decoradas. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, PURPLE, PURPLE_DARK);
        frame(g, x, y, w, h, GOLD);
    }

    public static void frame(GuiGraphics g, int x, int y, int w, int h, int color) {
        // marco exterior
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
        // marco interior tenue
        int c2 = (color & 0x00FFFFFF) | 0x80000000;
        g.fill(x + 3, y + 3, x + w - 3, y + 4, c2);
        g.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, c2);
        g.fill(x + 3, y + 3, x + 4, y + h - 3, c2);
        g.fill(x + w - 4, y + 3, x + w - 3, y + h - 3, c2);
        // esquinas
        corner(g, x, y, 1, 1, color);
        corner(g, x + w, y, -1, 1, color);
        corner(g, x, y + h, 1, -1, color);
        corner(g, x + w, y + h, -1, -1, color);
    }

    private static void corner(GuiGraphics g, int cx, int cy, int sx, int sy, int color) {
        // pequeño rombo dorado sobresaliendo
        for (int i = 0; i < 4; i++) {
            int xa = cx + sx * (1 + i), xb = cx + sx * (2 + i);
            g.fill(Math.min(xa, xb), cy + sy * (6 - i) - (sy > 0 ? 0 : 1), Math.max(xa, xb), cy + sy * (6 - i) + (sy > 0 ? 1 : 0), color);
        }
        int px = cx + sx * 6, py = cy + sy * 6;
        g.fill(px - 1, py - 1, px + 2, py + 2, GOLD_LIGHT);
    }

    /** Barra de título con destellos. */
    public static void title(GuiGraphics g, net.minecraft.client.gui.Font font, String text, int cx, int y) {
        int w = font.width(text);
        g.drawString(font, text, cx - w / 2, y, GOLD_LIGHT, true);
        int lineW = w / 2 + 24;
        g.fill(cx - w / 2 - lineW, y + 4, cx - w / 2 - 8, y + 5, GOLD_DARK);
        g.fill(cx + w / 2 + 8, y + 4, cx + w / 2 + lineW, y + 5, GOLD_DARK);
        g.drawString(font, "✦", cx - w / 2 - 6, y, GOLD, false);
        g.drawString(font, "✦", cx + w / 2 + 2, y, GOLD, false);
    }

    /** Estrellitas fijas de fondo (decorativas). */
    public static void sparkles(GuiGraphics g, int x, int y, int w, int h, long time) {
        for (int i = 0; i < 18; i++) {
            int sx = x + 6 + (int) ((i * 97L + 31) % Math.max(1, w - 12));
            int sy = y + 6 + (int) ((i * 53L + 17) % Math.max(1, h - 12));
            double ph = Math.sin((time / 400.0) + i * 1.7);
            int a = (int) (40 + 60 * (ph * 0.5 + 0.5));
            g.fill(sx, sy, sx + 1, sy + 1, (a << 24) | 0xFFE9A8);
        }
    }

    private Fantasy() {}
}
