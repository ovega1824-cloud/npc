package com.npcmod.client.screen;

import com.npcmod.client.NpcAssets;
import com.npcmod.client.ui.FButton;
import com.npcmod.client.ui.Fantasy;
import com.npcmod.entity.NpcData;
import com.npcmod.network.NpcNetwork;
import com.npcmod.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/** Caja de diálogo que aparece al hacer clic derecho en un NPC. Totalmente configurable desde el editor. */
public class DialogScreen extends Screen {
    private final int entityId;
    private final String npcName;
    private final CompoundTag data;
    private final CompoundTag box;
    private final ListTag nodes;

    private int node = 0;
    private float shown = 0F;
    private int lastBlip = 0;
    private boolean finished = false;
    private boolean closedSent = false;

    // geometría calculada
    private int bx, by, bw, bh;

    public DialogScreen(int entityId, String npcName, CompoundTag data) {
        super(Component.literal(npcName));
        this.entityId = entityId;
        this.npcName = npcName;
        this.data = NpcData.merged(data);
        this.box = this.data.getCompound("box");
        this.nodes = this.data.getList("nodes", Tag.TAG_COMPOUND);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        bx = box.getInt("x") * width / 100;
        by = box.getInt("y") * height / 100;
        bw = Math.max(80, box.getInt("w") * width / 100);
        bh = Math.max(40, box.getInt("h") * height / 100);
        enterNode(node);
    }

    private CompoundTag cur() {
        return nodes.getCompound(Math.max(0, Math.min(node, nodes.size() - 1)));
    }

    private void enterNode(int n) {
        if (n < 0 || n >= nodes.size()) { close(); return; }
        node = n;
        shown = 0F;
        lastBlip = 0;
        finished = false;
        clearWidgets();
        CompoundTag c = cur();
        NpcNetwork.sendToServer(new Packets.DialogAction(entityId, 0, node, 0));
        playSound(c.getString("sound"), 1.0F, 1.0F);
    }

    private void buildChoices() {
        clearWidgets();
        ListTag choices = cur().getList("choices", Tag.TAG_COMPOUND);
        int n = choices.size();
        int cw = Math.min(bw, Math.max(120, bw / 2));
        int ch = 20;
        int startY = by - 6 - n * (ch + 3);
        if (startY < 4) startY = by + bh + 6; // si no cabe arriba, debajo
        for (int i = 0; i < n; i++) {
            final int idx = i;
            CompoundTag choice = choices.getCompound(i);
            addRenderableWidget(new FButton(bx + bw - cw, startY + i * (ch + 3), cw, ch,
                    Component.literal(choice.getString("text")), b -> choose(idx)));
        }
    }

    private void choose(int idx) {
        ListTag choices = cur().getList("choices", Tag.TAG_COMPOUND);
        if (idx < 0 || idx >= choices.size()) return;
        NpcNetwork.sendToServer(new Packets.DialogAction(entityId, 1, node, idx));
        int go = choices.getCompound(idx).getInt("goto");
        if (go < 0) close(); else enterNode(go);
    }

    private void advance() {
        CompoundTag c = cur();
        if (!c.getList("choices", Tag.TAG_COMPOUND).isEmpty()) return; // se avanza con botones
        int next = c.getInt("next");
        if (next == -1) { close(); return; }
        enterNode(next == -2 ? node + 1 : next);
    }

    private void close() {
        if (!closedSent) {
            closedSent = true;
            NpcNetwork.sendToServer(new Packets.DialogAction(entityId, 2, 0, 0));
        }
        onClose();
    }

    private void playSound(String id, float vol, float pitch) {
        if (id == null || id.isEmpty()) return;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        if (rl == null) return;
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(rl);
        if (ev == null) ev = SoundEvent.createVariableRangeEvent(rl);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol));
    }

    @Override
    public void tick() {
        CompoundTag c = cur();
        String text = c.getString("text");
        float speed = Math.max(0.1F, box.getFloat("speed"));
        if (!finished) {
            shown += speed;
            int visible = (int) shown;
            if (visible >= text.length()) {
                visible = text.length();
                finished = true;
                buildChoices();
            }
            if (data.getBoolean("voiceBlip") && visible - lastBlip >= 2) {
                lastBlip = visible;
                playSound(data.getString("voice"), data.getFloat("voiceVolume"),
                        Math.max(0.5F, data.getFloat("voicePitch")) + (visible % 3) * 0.04F);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (!finished) {
            shown = cur().getString("text").length();
            return true;
        }
        advance();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 32 || key == 257) { // espacio / enter
            if (!finished) shown = cur().getString("text").length(); else advance();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        if (box.getBoolean("dim")) g.fill(0, 0, width, height, 0x66000000);

        CompoundTag c = cur();

        // retrato (del nodo o el general)
        String portrait = c.getString("portrait").isEmpty() ? box.getString("portrait") : c.getString("portrait");
        NpcAssets.Img p = NpcAssets.image(portrait);
        if (p != null) {
            int ph = Math.max(16, box.getInt("portSize") * height / 100);
            int pw = Math.max(1, ph * p.w() / Math.max(1, p.h()));
            int px = box.getInt("portX") * width / 100;
            int py = box.getInt("portY") * height / 100;
            g.blit(p.loc(), px, py, pw, ph, 0, 0, p.w(), p.h(), p.w(), p.h());
        }

        // caja
        int bg = box.getInt("bg");
        g.fill(bx, by, bx + bw, by + bh, bg);
        NpcAssets.Img bgi = NpcAssets.image(box.getString("bgImage"));
        if (bgi != null) {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.setColor(1F, 1F, 1F, box.getFloat("bgAlpha"));
            g.blit(bgi.loc(), bx, by, bw, bh, 0, 0, bgi.w(), bgi.h(), bgi.w(), bgi.h());
            g.setColor(1F, 1F, 1F, 1F);
        }
        Fantasy.frame(g, bx, by, bw, bh, box.getInt("border"));

        // nombre
        String speaker = c.getString("speaker").isEmpty() ? npcName : c.getString("speaker");
        if (!speaker.isEmpty()) {
            int nw = font.width(speaker) + 16;
            int ny = by - 14;
            g.fill(bx + 10, ny, bx + 10 + nw, by + 1, bg);
            Fantasy.frame(g, bx + 10, ny, nw, 15, box.getInt("border"));
            g.drawString(font, speaker, bx + 18, ny + 4, box.getInt("nameColor"), true);
        }

        // texto
        String full = c.getString("text");
        int visible = Math.min(full.length(), (int) shown);
        if (visible > 0 && visible < full.length() && full.charAt(visible - 1) == '§') visible++;
        String part = full.substring(0, Math.min(full.length(), visible));
        float fs = Math.max(0.5F, box.getFloat("fontScale"));
        int tw = (int) ((bw - 20) / fs);
        List<FormattedCharSequence> lines = font.split(FormattedText.of(part), Math.max(20, tw));
        g.pose().pushPose();
        g.pose().translate(bx + 10, by + 10, 0);
        g.pose().scale(fs, fs, 1F);
        int y = 0;
        for (FormattedCharSequence l : lines) {
            g.drawString(font, l, 0, y, box.getInt("textColor"), true);
            y += 10;
        }
        g.pose().popPose();

        if (finished && cur().getList("choices", Tag.TAG_COMPOUND).isEmpty()) {
            boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
            if (blink) g.drawString(font, "▼", bx + bw - 14, by + bh - 12, Fantasy.GOLD_LIGHT, true);
        }
        super.render(g, mx, my, partial);
    }
}
