package com.npcmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

/** Utilidades para la configuración (NBT) de un NPC. Se comparte entre cliente y servidor. */
public final class NpcData {

    public static CompoundTag defaultBox() {
        CompoundTag b = new CompoundTag();
        b.putInt("x", 8);          // % de pantalla (esquina superior izquierda)
        b.putInt("y", 64);
        b.putInt("w", 84);
        b.putInt("h", 30);
        b.putInt("bg", 0xE0140A2A);
        b.putInt("border", 0xFFD4AF37);
        b.putInt("nameColor", 0xFFFFE27A);
        b.putInt("textColor", 0xFFFFFFFF);
        b.putFloat("speed", 1.0F);        // letras por tick
        b.putFloat("fontScale", 1.0F);
        b.putString("bgImage", "");
        b.putFloat("bgAlpha", 0.85F);
        b.putString("portrait", "");
        b.putInt("portX", 2);              // % de pantalla
        b.putInt("portY", 18);
        b.putInt("portSize", 46);          // % de la altura de pantalla
        b.putBoolean("dim", true);
        return b;
    }

    public static CompoundTag defaultNode(String text) {
        CompoundTag n = new CompoundTag();
        n.putString("speaker", "");
        n.putString("text", text);
        n.putString("portrait", "");
        n.putString("sound", "");
        n.putString("command", "");
        n.putInt("next", -2);   // -2 = siguiente nodo, -1 = cerrar
        n.put("choices", new ListTag());
        return n;
    }

    public static CompoundTag defaultChoice() {
        CompoundTag c = new CompoundTag();
        c.putString("text", "Opción");
        c.putInt("goto", -1);
        c.putString("command", "");
        return c;
    }

    public static CompoundTag defaults() {
        CompoundTag t = new CompoundTag();
        t.putString("model", "default");
        t.putString("texture", "");
        t.putFloat("scale", 1.0F);
        t.putString("voice", "minecraft:entity.villager.ambient");
        t.putFloat("voicePitch", 1.0F);
        t.putFloat("voiceVolume", 1.0F);
        t.putBoolean("voiceBlip", true);
        t.putBoolean("look", true);
        t.putBoolean("wander", false);
        t.putBoolean("invuln", true);
        t.put("box", defaultBox());
        ListTag nodes = new ListTag();
        nodes.add(defaultNode("¡Hola, viajero! Haz clic derecho para hablar conmigo."));
        t.put("nodes", nodes);
        t.put("onClick", new ListTag());
        t.put("onEnd", new ListTag());
        return t;
    }

    /** Rellena con valores por defecto lo que falte. */
    public static CompoundTag merged(CompoundTag in) {
        CompoundTag def = defaults();
        for (String k : def.getAllKeys()) {
            if (!in.contains(k)) in.put(k, def.get(k).copy());
        }
        CompoundTag box = in.getCompound("box");
        CompoundTag defBox = defaultBox();
        for (String k : defBox.getAllKeys()) {
            if (!box.contains(k)) box.put(k, defBox.get(k).copy());
        }
        in.put("box", box);
        if (in.getList("nodes", Tag.TAG_COMPOUND).isEmpty()) {
            ListTag nodes = new ListTag();
            nodes.add(defaultNode("..."));
            in.put("nodes", nodes);
        }
        return in;
    }

    public static ListTag stringList(java.util.List<String> values) {
        ListTag l = new ListTag();
        for (String s : values) l.add(StringTag.valueOf(s));
        return l;
    }

    private NpcData() {}
}
