package com.npcmod.client.screen;

import com.npcmod.client.NpcAssets;
import com.npcmod.client.ui.FButton;
import com.npcmod.client.ui.Fantasy;
import com.npcmod.entity.NpcData;
import com.npcmod.entity.NpcEntity;
import com.npcmod.network.NpcNetwork;
import com.npcmod.network.Packets;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Editor visual del NPC con estética de fantasía. */
public class EditorScreen extends Screen {
    private static final String[] TABS = {"General", "Modelo", "Voz", "Diálogo", "Caja", "Imágenes", "Acciones", "Equipo"};

    private record Label(String text, int x, int y) {}

    private final int entityId;
    private final CompoundTag d;
    private String name;
    private boolean nameVisible;

    private int tab = 0;
    private int nodeIdx = 0;
    private int choiceIdx = 0;
    private int cmdIdx = 0;
    private String cmdKey = "onClick";
    private int hotbarFrom = 0;
    private String status = "";

    private final List<Label> labels = new ArrayList<>();
    private int px, py, pw, ph, cx, cy, cw;

    public EditorScreen(int entityId, String name, boolean nameVisible, CompoundTag data) {
        super(Component.translatable("npcmod.editor.title"));
        this.entityId = entityId;
        this.name = name;
        this.nameVisible = nameVisible;
        this.d = NpcData.merged(data.copy());
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private CompoundTag box() { return d.getCompound("box"); }
    private ListTag nodes() { return d.getList("nodes", Tag.TAG_COMPOUND); }
    private CompoundTag node() {
        if (nodeIdx >= nodes().size()) nodeIdx = Math.max(0, nodes().size() - 1);
        return nodes().getCompound(nodeIdx);
    }
    private ListTag choices() { return node().getList("choices", Tag.TAG_COMPOUND); }

    // ================================================================== layout

    @Override
    protected void init() {
        labels.clear();
        pw = Math.min(width - 8, 470);
        ph = Math.min(height - 8, 262);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        cx = px + 98;
        cw = pw - 98 - 10;
        cy = py + 26;

        for (int i = 0; i < TABS.length; i++) {
            final int t = i;
            addRenderableWidget(new FButton(px + 8, py + 26 + i * 22, 80, 20, Component.literal(TABS[i]), b -> {
                tab = t;
                rebuildWidgets();
            }).selected(i == tab));
        }

        switch (tab) {
            case 0 -> buildGeneral();
            case 1 -> buildModel();
            case 2 -> buildVoice();
            case 3 -> buildDialog();
            case 4 -> buildBox();
            case 5 -> buildImages();
            case 6 -> buildActions();
            case 7 -> buildEquip();
            default -> { }
        }

        int by = py + ph - 24;
        int third = (cw - 8) / 3;
        addRenderableWidget(new FButton(cx, by, third, 18, Component.literal("Guardar"), b -> save(false)));
        addRenderableWidget(new FButton(cx + third + 4, by, third, 18, Component.literal("Guardar y cerrar"), b -> save(true)));
        addRenderableWidget(new FButton(cx + 2 * (third + 4), by, third, 18, Component.literal("Cancelar"), b -> onClose()));
    }

    private int colX(int col) { return col == 0 ? cx : cx + cw / 2 + 3; }
    private int colW() { return cw / 2 - 3; }
    private int rowY(int row) { return cy + row * 30; }

    private void save(boolean close) {
        NpcNetwork.sendToServer(new Packets.SaveNpc(entityId, name, nameVisible, d));
        status = "✔ Guardado";
        if (close) onClose();
    }

    // ================================================================== helpers de widgets

    private EditBox field(int x, int y, int w, String label, String value, int max, Consumer<String> onChange) {
        labels.add(new Label(label, x, y));
        EditBox e = new EditBox(font, x, y + 10, w, 18, Component.literal(label));
        e.setMaxLength(max);
        e.setValue(value == null ? "" : value);
        e.setResponder(onChange);
        addRenderableWidget(e);
        return e;
    }

    private void intField(int x, int y, int w, String label, int value, int min, int max, IntConsumer set) {
        field(x, y, w, label, Integer.toString(value), 9, s -> {
            try { set.accept(Math.max(min, Math.min(max, Integer.parseInt(s.trim())))); } catch (NumberFormatException ignored) { }
        });
    }

    private void floatField(int x, int y, int w, String label, float value, float min, float max, Consumer<Float> set) {
        field(x, y, w, label, Float.toString(value), 8, s -> {
            try { set.accept(Math.max(min, Math.min(max, Float.parseFloat(s.trim().replace(',', '.'))))); } catch (NumberFormatException ignored) { }
        });
    }

    private void colorField(int x, int y, int w, String label, CompoundTag t, String key) {
        field(x, y, w, label + " (AARRGGBB)", String.format("%08X", t.getInt(key)), 8, s -> {
            if (s.length() == 8) {
                try { t.putInt(key, (int) Long.parseLong(s, 16)); } catch (NumberFormatException ignored) { }
            }
        });
    }

    private void toggle(int x, int y, int w, String label, boolean cur, Consumer<Boolean> set) {
        final boolean[] v = {cur};
        FButton b = new FButton(x, y + 10, w, 18, Component.literal(label + ": " + (cur ? "Sí" : "No")), null) {
            @Override public void onPress() {
                v[0] = !v[0];
                set.accept(v[0]);
                setMessage(Component.literal(label + ": " + (v[0] ? "Sí" : "No")));
            }
        };
        addRenderableWidget(b);
    }

    private void cycler(int x, int y, int w, String label, List<String> opts, String cur, Consumer<String> set) {
        labels.add(new Label(label, x, y));
        final int[] idx = {Math.max(0, opts.indexOf(cur))};
        final FButton[] mid = new FButton[1];
        Runnable refresh = () -> {
            String o = opts.get(idx[0]);
            mid[0].setMessage(Component.literal(o.isEmpty() ? "(ninguna)" : o));
        };
        FButton left = new FButton(x, y + 10, 18, 18, Component.literal("<"), b -> {
            idx[0] = (idx[0] - 1 + opts.size()) % opts.size(); set.accept(opts.get(idx[0])); refresh.run();
        });
        mid[0] = new FButton(x + 20, y + 10, w - 40, 18, Component.empty(), b -> {
            idx[0] = (idx[0] + 1) % opts.size(); set.accept(opts.get(idx[0])); refresh.run();
        });
        FButton right = new FButton(x + w - 18, y + 10, 18, 18, Component.literal(">"), b -> {
            idx[0] = (idx[0] + 1) % opts.size(); set.accept(opts.get(idx[0])); refresh.run();
        });
        refresh.run();
        addRenderableWidget(left);
        addRenderableWidget(mid[0]);
        addRenderableWidget(right);
    }

    private void navRow(int x, int y, int w, String name, int idx, int count, Runnable prev, Runnable next, Runnable add, Runnable del) {
        int bw = 20;
        int midW = w - 4 * bw - 8;
        addRenderableWidget(new FButton(x, y + 10, bw, 18, Component.literal("<"), b -> prev.run()));
        addRenderableWidget(new FButton(x + bw + 2, y + 10, midW, 18,
                Component.literal(name + " " + (count == 0 ? 0 : idx + 1) + "/" + count), b -> { }).selected(true));
        addRenderableWidget(new FButton(x + bw + midW + 4, y + 10, bw, 18, Component.literal(">"), b -> next.run()));
        addRenderableWidget(new FButton(x + 2 * bw + midW + 6, y + 10, bw, 18, Component.literal("+"), b -> add.run()));
        addRenderableWidget(new FButton(x + 3 * bw + midW + 8, y + 10, bw, 18, Component.literal("−"), b -> del.run()));
    }

    private void playTest(String id, float vol, float pitch) {
        ResourceLocation rl = ResourceLocation.tryParse(id == null ? "" : id);
        if (rl == null) { status = "ID de sonido inválido"; return; }
        SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(rl);
        if (ev == null) ev = SoundEvent.createVariableRangeEvent(rl);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(ev, pitch, vol));
    }

    private static List<String> withFirst(String first, List<String> rest) {
        List<String> l = new ArrayList<>();
        l.add(first);
        l.addAll(rest);
        return l;
    }

    // ================================================================== pestañas

    private void buildGeneral() {
        field(colX(0), rowY(0), cw, "Nombre del NPC", name, 64, s -> name = s);
        toggle(colX(0), rowY(1), colW(), "Nombre visible", nameVisible, v -> nameVisible = v);
        toggle(colX(1), rowY(1), colW(), "Mira al jugador", d.getBoolean("look"), v -> d.putBoolean("look", v));
        toggle(colX(0), rowY(2), colW(), "Camina libre", d.getBoolean("wander"), v -> d.putBoolean("wander", v));
        toggle(colX(1), rowY(2), colW(), "Invulnerable", d.getBoolean("invuln"), v -> d.putBoolean("invuln", v));
        floatField(colX(0), rowY(3), colW(), "Tamaño (0.1 - 10)", d.getFloat("scale"), 0.1F, 10F, v -> d.putFloat("scale", v));
    }

    private void buildModel() {
        cycler(colX(0), rowY(0), cw, "Modelo (.geo.json de BBS / Blockbench)", withFirst("default", NpcAssets.modelNames()),
                d.getString("model"), v -> d.putString("model", v));
        cycler(colX(0), rowY(1), cw, "Textura (.png)", withFirst("", NpcAssets.textureNames()),
                d.getString("texture"), v -> d.putString("texture", v));
        addRenderableWidget(new FButton(colX(0), rowY(2) + 10, colW(), 18, Component.literal("Recargar archivos"), b -> {
            NpcAssets.reload();
            rebuildWidgets();
            status = "Archivos recargados";
        }));
        addRenderableWidget(new FButton(colX(1), rowY(2) + 10, colW(), 18, Component.literal("Abrir carpeta de modelos"), b ->
                Util.getPlatform().openFile(NpcAssets.dir("models").toFile())));
        addRenderableWidget(new FButton(colX(0), rowY(3) + 10, colW(), 18, Component.literal("Abrir carpeta de texturas"), b ->
                Util.getPlatform().openFile(NpcAssets.dir("textures").toFile())));
    }

    private void buildVoice() {
        field(colX(0), rowY(0), cw, "Sonido de voz (ID, ej. minecraft:entity.villager.ambient)", d.getString("voice"), 128, s -> d.putString("voice", s));
        floatField(colX(0), rowY(1), colW(), "Tono (0.5 - 2)", d.getFloat("voicePitch"), 0.5F, 2F, v -> d.putFloat("voicePitch", v));
        floatField(colX(1), rowY(1), colW(), "Volumen (0 - 2)", d.getFloat("voiceVolume"), 0F, 2F, v -> d.putFloat("voiceVolume", v));
        toggle(colX(0), rowY(2), colW(), "Voz al escribir", d.getBoolean("voiceBlip"), v -> d.putBoolean("voiceBlip", v));
        addRenderableWidget(new FButton(colX(1), rowY(2) + 10, colW(), 18, Component.literal("▶ Probar voz"), b ->
                playTest(d.getString("voice"), d.getFloat("voiceVolume"), d.getFloat("voicePitch"))));
    }

    private void buildDialog() {
        int fullW = cw;
        navRow(colX(0), rowY(0) - 6, colW(), "Nodo", nodeIdx, nodes().size(),
                () -> { nodeIdx = Math.max(0, nodeIdx - 1); choiceIdx = 0; rebuildWidgets(); },
                () -> { nodeIdx = Math.min(nodes().size() - 1, nodeIdx + 1); choiceIdx = 0; rebuildWidgets(); },
                () -> { nodes().add(NpcData.defaultNode("...")); nodeIdx = nodes().size() - 1; choiceIdx = 0; rebuildWidgets(); },
                () -> { if (nodes().size() > 1) { nodes().remove(nodeIdx); nodeIdx = Math.max(0, nodeIdx - 1); choiceIdx = 0; rebuildWidgets(); } });
        int nx = node().getInt("next");
        intField(colX(1), rowY(0) - 6, colW(), "Siguiente nodo (-2 auto, -1 cerrar)", nx >= 0 ? nx + 1 : nx, -2, 999,
                v -> node().putInt("next", v > 0 ? v - 1 : (v == 0 ? 0 : v)));

        field(colX(0), rowY(1) - 4, fullW, "Texto (acepta colores con §c, §l...)", node().getString("text"), 1000, s -> node().putString("text", s));
        field(colX(0), rowY(2) - 4, colW(), "Quién habla (vacío = nombre del NPC)", node().getString("speaker"), 64, s -> node().putString("speaker", s));
        cycler(colX(1), rowY(2) - 4, colW(), "Retrato de este nodo", withFirst("", NpcAssets.imageNames()),
                node().getString("portrait"), v -> node().putString("portrait", v));
        field(colX(0), rowY(3) - 4, colW(), "Sonido al aparecer (ID)", node().getString("sound"), 128, s -> node().putString("sound", s));
        field(colX(1), rowY(3) - 4, colW(), "Comando al aparecer", node().getString("command"), 256, s -> node().putString("command", s));

        navRow(colX(0), rowY(4) - 4, fullW, "Pregunta/opción", choiceIdx, choices().size(),
                () -> { choiceIdx = Math.max(0, choiceIdx - 1); rebuildWidgets(); },
                () -> { choiceIdx = Math.min(choices().size() - 1, choiceIdx + 1); rebuildWidgets(); },
                () -> { choices().add(NpcData.defaultChoice()); choiceIdx = choices().size() - 1; rebuildWidgets(); },
                () -> { if (!choices().isEmpty()) { choices().remove(choiceIdx); choiceIdx = Math.max(0, choiceIdx - 1); rebuildWidgets(); } });

        if (!choices().isEmpty()) {
            if (choiceIdx >= choices().size()) choiceIdx = choices().size() - 1;
            CompoundTag ch = choices().getCompound(choiceIdx);
            int y = rowY(5) - 4;
            field(colX(0), y, 130, "Texto de la opción", ch.getString("text"), 128, s -> ch.putString("text", s));
            intField(colX(0) + 136, y, 56, "Ir a nodo (-1=fin)", ch.getInt("goto") + 1 > 0 ? ch.getInt("goto") + 1 : ch.getInt("goto"), -1, 999,
                    v -> ch.putInt("goto", v > 0 ? v - 1 : v));
            field(colX(0) + 198, y, fullW - 198, "Comando al elegir", ch.getString("command"), 256, s -> ch.putString("command", s));
        }
    }

    private void buildBox() {
        CompoundTag b = box();
        intField(colX(0), rowY(0), colW(), "Posición X (% pantalla)", b.getInt("x"), 0, 100, v -> b.putInt("x", v));
        intField(colX(1), rowY(0), colW(), "Posición Y (% pantalla)", b.getInt("y"), 0, 100, v -> b.putInt("y", v));
        intField(colX(0), rowY(1), colW(), "Ancho (%)", b.getInt("w"), 10, 100, v -> b.putInt("w", v));
        intField(colX(1), rowY(1), colW(), "Alto (%)", b.getInt("h"), 8, 100, v -> b.putInt("h", v));
        floatField(colX(0), rowY(2), colW(), "Velocidad de texto (letras/tick)", b.getFloat("speed"), 0.1F, 20F, v -> b.putFloat("speed", v));
        floatField(colX(1), rowY(2), colW(), "Tamaño de letra", b.getFloat("fontScale"), 0.5F, 3F, v -> b.putFloat("fontScale", v));
        colorField(colX(0), rowY(3), colW(), "Fondo", b, "bg");
        colorField(colX(1), rowY(3), colW(), "Borde", b, "border");
        colorField(colX(0), rowY(4), colW(), "Color nombre", b, "nameColor");
        colorField(colX(1), rowY(4), colW(), "Color texto", b, "textColor");
        toggle(colX(0), rowY(5), colW(), "Oscurecer pantalla", b.getBoolean("dim"), v -> b.putBoolean("dim", v));
    }

    private void buildImages() {
        CompoundTag b = box();
        int w = colW() + 20;
        cycler(colX(0), rowY(0), w, "Imagen de fondo de la caja", withFirst("", NpcAssets.imageNames()), b.getString("bgImage"), v -> b.putString("bgImage", v));
        floatField(colX(0), rowY(1), w, "Opacidad imagen (0 - 1)", b.getFloat("bgAlpha"), 0F, 1F, v -> b.putFloat("bgAlpha", v));
        cycler(colX(0), rowY(2), w, "Retrato general", withFirst("", NpcAssets.imageNames()), b.getString("portrait"), v -> b.putString("portrait", v));
        intField(colX(0), rowY(3), w / 2 - 2, "Retrato X (%)", b.getInt("portX"), -50, 150, v -> b.putInt("portX", v));
        intField(colX(0) + w / 2 + 2, rowY(3), w / 2 - 2, "Retrato Y (%)", b.getInt("portY"), -50, 150, v -> b.putInt("portY", v));
        intField(colX(0), rowY(4), w / 2 - 2, "Retrato tamaño (%)", b.getInt("portSize"), 5, 200, v -> b.putInt("portSize", v));
        addRenderableWidget(new FButton(colX(0), rowY(5) + 2, w / 2 - 2, 18, Component.literal("Carpeta de imágenes"), x ->
                Util.getPlatform().openFile(NpcAssets.dir("images").toFile())));
        addRenderableWidget(new FButton(colX(0) + w / 2 + 2, rowY(5) + 2, w / 2 - 2, 18, Component.literal("Recargar"), x -> {
            NpcAssets.reload(); rebuildWidgets();
        }));
    }

    private void buildActions() {
        addRenderableWidget(new FButton(colX(0), rowY(0) + 6, colW(), 18, Component.literal("Al hacer clic derecho"), b -> {
            cmdKey = "onClick"; cmdIdx = 0; rebuildWidgets();
        }).selected(cmdKey.equals("onClick")));
        addRenderableWidget(new FButton(colX(1), rowY(0) + 6, colW(), 18, Component.literal("Al terminar el diálogo"), b -> {
            cmdKey = "onEnd"; cmdIdx = 0; rebuildWidgets();
        }).selected(cmdKey.equals("onEnd")));

        ListTag list = d.getList(cmdKey, Tag.TAG_STRING);
        navRow(colX(0), rowY(1), cw, "Comando", cmdIdx, list.size(),
                () -> { cmdIdx = Math.max(0, cmdIdx - 1); rebuildWidgets(); },
                () -> { cmdIdx = Math.min(list.size() - 1, cmdIdx + 1); rebuildWidgets(); },
                () -> { list.add(StringTag.valueOf("say Hola %player%")); d.put(cmdKey, list); cmdIdx = list.size() - 1; rebuildWidgets(); },
                () -> { if (!list.isEmpty()) { list.remove(cmdIdx); cmdIdx = Math.max(0, cmdIdx - 1); rebuildWidgets(); } });
        if (!list.isEmpty()) {
            if (cmdIdx >= list.size()) cmdIdx = list.size() - 1;
            final int at = cmdIdx;
            field(colX(0), rowY(2), cw, "Comando (sin /). %player% = jugador que habla", list.getString(at), 256, s -> list.set(at, StringTag.valueOf(s)));
        }
    }

    private static final String[] SLOT_NAMES = {"Mano principal", "Mano secundaria", "Casco", "Pechera", "Pantalones", "Botas"};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private void buildEquip() {
        List<String> slots = new ArrayList<>();
        for (int i = 1; i <= 9; i++) slots.add("Casilla " + i);
        cycler(colX(0), rowY(0) - 8, cw, "Tomar ítem de la barra rápida", slots, slots.get(hotbarFrom), v -> {
            hotbarFrom = Math.max(0, slots.indexOf(v));
        });
        for (int i = 0; i < SLOT_NAMES.length; i++) {
            final int s = i;
            int y = cy + 28 + i * 24;
            addRenderableWidget(new FButton(cx + cw - 150, y, 90, 18, Component.literal("Poner ítem"), b ->
                    NpcNetwork.sendToServer(new Packets.SetEquip(entityId, s, false, hotbarFrom))));
            addRenderableWidget(new FButton(cx + cw - 56, y, 56, 18, Component.literal("Vaciar"), b ->
                    NpcNetwork.sendToServer(new Packets.SetEquip(entityId, s, true, 0))));
        }
    }

    // ================================================================== render

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        Fantasy.panel(g, px, py, pw, ph);
        Fantasy.sparkles(g, px, py, pw, ph, System.currentTimeMillis());
        Fantasy.title(g, font, "✦ Editor de NPC ✦", px + pw / 2, py + 8);
        g.fill(px + 94, py + 22, px + 95, py + ph - 8, Fantasy.GOLD_DARK);

        for (Label l : labels) g.drawString(font, l.text(), l.x(), l.y(), Fantasy.TEXT_DIM, false);

        switch (tab) {
            case 1 -> hint(g, cy + 4 * 30 + 4,
                    "Pon tus modelos .geo.json (exportados de BBS mod o Blockbench)",
                    "en  .minecraft/npcmod/models  y las texturas .png en  npcmod/textures.",
                    "Si un hueso se llama righthand / rightitem, ahí se sostiene el ítem.");
            case 2 -> hint(g, cy + 3 * 30 + 4,
                    "Puedes usar sonidos de otros mods o de un resource pack propio",
                    "(ej. mipack:npc.hola). También hay un sonido por cada nodo del diálogo.");
            case 4 -> hint(g, cy + 5 * 30 + 36, "Los % son relativos al tamaño de la pantalla.");
            case 6 -> hint(g, cy + 3 * 30 + 12,
                    "Se ejecutan como operador desde la posición del NPC.",
                    "Ej: give %player% diamond 1  |  effect give %player% speed 10  |  tp %player% 0 80 0");
            case 7 -> renderEquip(g);
            default -> { }
        }
        if (tab == 5) renderPreview(g);
        if (tab == 3 && nodes().size() > 0) hint(g, cy + 6 * 30 - 4 + 20, "Opciones: si el nodo tiene opciones, se muestran como botones.");

        if (!status.isEmpty()) g.drawString(font, status, px + 10, py + ph - 16, Fantasy.GOLD_LIGHT, true);
        super.render(g, mx, my, partial);
    }

    private void hint(GuiGraphics g, int y, String... lines) {
        for (int i = 0; i < lines.length; i++) g.drawString(font, lines[i], cx, y + i * 11, Fantasy.TEXT_DIM, false);
    }

    private void renderEquip(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level != null ? mc.level.getEntity(entityId) : null;
        for (int i = 0; i < SLOT_NAMES.length; i++) {
            int y = cy + 28 + i * 24;
            g.drawString(font, SLOT_NAMES[i], cx, y + 5, Fantasy.TEXT, false);
            if (e instanceof NpcEntity npc) {
                ItemStack st = npc.getItemBySlot(SLOTS[i]);
                g.fill(cx + 88, y, cx + 108, y + 20, 0x80000000);
                Fantasy.frame(g, cx + 88, y, 20, 20, Fantasy.GOLD_DARK);
                if (!st.isEmpty()) {
                    g.renderItem(st, cx + 90, y + 2);
                    g.drawString(font, font.plainSubstrByWidth(st.getHoverName().getString(), cw - 270), cx + 112, y + 5, Fantasy.GOLD_LIGHT, false);
                }
            }
        }
        if (mc.player != null) {
            ItemStack from = mc.player.getInventory().getItem(hotbarFrom);
            String txt = from.isEmpty() ? "(casilla vacía)" : from.getHoverName().getString();
            g.drawString(font, "Ítem elegido: " + txt, cx, cy + 28 + 6 * 24 + 2, Fantasy.TEXT_DIM, false);
        }
    }

    /** Miniatura de la pantalla para colocar caja, imagen y retrato. */
    private void renderPreview(GuiGraphics g) {
        int w = cw / 2 - 12, h = w * 9 / 16;
        int x = colX(1) + 14, y = cy + 6;
        g.fill(x, y, x + w, y + h, 0xFF0A0614);
        Fantasy.frame(g, x - 1, y - 1, w + 2, h + 2, Fantasy.GOLD_DARK);
        g.enableScissor(x, y, x + w, y + h);

        CompoundTag b = box();
        NpcAssets.Img p = NpcAssets.image(b.getString("portrait"));
        if (p != null) {
            int pH = Math.max(4, b.getInt("portSize") * h / 100);
            int pW = Math.max(1, pH * p.w() / Math.max(1, p.h()));
            g.blit(p.loc(), x + b.getInt("portX") * w / 100, y + b.getInt("portY") * h / 100, pW, pH, 0, 0, p.w(), p.h(), p.w(), p.h());
        }
        int bx = x + b.getInt("x") * w / 100, by = y + b.getInt("y") * h / 100;
        int bw = Math.max(6, b.getInt("w") * w / 100), bh = Math.max(4, b.getInt("h") * h / 100);
        g.fill(bx, by, bx + bw, by + bh, b.getInt("bg"));
        NpcAssets.Img bg = NpcAssets.image(b.getString("bgImage"));
        if (bg != null) {
            g.setColor(1F, 1F, 1F, b.getFloat("bgAlpha"));
            g.blit(bg.loc(), bx, by, bw, bh, 0, 0, bg.w(), bg.h(), bg.w(), bg.h());
            g.setColor(1F, 1F, 1F, 1F);
        }
        Fantasy.frame(g, bx, by, bw, bh, b.getInt("border"));
        g.disableScissor();
        g.drawString(font, "Vista previa", x, y + h + 4, Fantasy.TEXT_DIM, false);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(null);
    }
}
