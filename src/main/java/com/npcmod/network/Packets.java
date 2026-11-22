package com.npcmod.network;

import com.npcmod.entity.NpcEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Todos los paquetes del mod. Cada uno tiene encode/decode/handle. */
public final class Packets {

    /** Máximo de tamaño NBT aceptado desde el cliente (evita abusos). */
    private static final long MAX_NBT = 2_000_000L;

    private static NpcEntity npcOf(ServerPlayer sp, int id) {
        Entity e = sp.level().getEntity(id);
        if (e instanceof NpcEntity npc && npc.distanceToSqr(sp) < 64 * 64) return npc;
        return null;
    }

    // ===================================================== servidor -> cliente

    public static final class OpenDialog {
        public final int entityId;
        public final String name;
        public final CompoundTag data;

        public OpenDialog(int entityId, String name, CompoundTag data) {
            this.entityId = entityId; this.name = name; this.data = data;
        }
        public static void encode(OpenDialog m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId); b.writeUtf(m.name, 256); b.writeNbt(m.data);
        }
        public static OpenDialog decode(FriendlyByteBuf b) {
            return new OpenDialog(b.readVarInt(), b.readUtf(256), b.readNbt());
        }
        public static void handle(OpenDialog m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.npcmod.client.ClientHooks.openDialog(m.entityId, m.name, m.data)));
            ctx.get().setPacketHandled(true);
        }
    }

    public static final class OpenEditor {
        public final int entityId;
        public final String name;
        public final boolean nameVisible;
        public final CompoundTag data;

        public OpenEditor(int entityId, String name, boolean nameVisible, CompoundTag data) {
            this.entityId = entityId; this.name = name; this.nameVisible = nameVisible; this.data = data;
        }
        public static void encode(OpenEditor m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId); b.writeUtf(m.name, 256); b.writeBoolean(m.nameVisible); b.writeNbt(m.data);
        }
        public static OpenEditor decode(FriendlyByteBuf b) {
            return new OpenEditor(b.readVarInt(), b.readUtf(256), b.readBoolean(), b.readNbt());
        }
        public static void handle(OpenEditor m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> com.npcmod.client.ClientHooks.openEditor(m.entityId, m.name, m.nameVisible, m.data)));
            ctx.get().setPacketHandled(true);
        }
    }

    // ===================================================== cliente -> servidor

    /** Guarda todo lo editado (nombre + datos). Solo operadores. */
    public static final class SaveNpc {
        public final int entityId;
        public final String name;
        public final boolean nameVisible;
        public final CompoundTag data;

        public SaveNpc(int entityId, String name, boolean nameVisible, CompoundTag data) {
            this.entityId = entityId; this.name = name; this.nameVisible = nameVisible; this.data = data;
        }
        public static void encode(SaveNpc m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId); b.writeUtf(m.name, 256); b.writeBoolean(m.nameVisible); b.writeNbt(m.data);
        }
        public static SaveNpc decode(FriendlyByteBuf b) {
            return new SaveNpc(b.readVarInt(), b.readUtf(256), b.readBoolean(), b.readNbt());
        }
        public static void handle(SaveNpc m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null || !sp.hasPermissions(2) || m.data == null) return;
                NpcEntity npc = npcOf(sp, m.entityId);
                if (npc == null) return;
                npc.setData(m.data);
                npc.setCustomName(m.name.isEmpty() ? null : Component.literal(m.name));
                npc.setCustomNameVisible(m.nameVisible && !m.name.isEmpty());
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /**
     * Equipamiento: copia un ítem del inventario del jugador al slot del NPC (o lo vacía).
     * slot: 0=mano principal, 1=mano secundaria, 2=casco, 3=pechera, 4=pantalones, 5=botas.
     */
    public static final class SetEquip {
        public final int entityId;
        public final int slot;
        public final boolean clear;
        public final int from; // casilla del inventario del jugador (0-8 = barra rápida)

        public SetEquip(int entityId, int slot, boolean clear, int from) {
            this.entityId = entityId; this.slot = slot; this.clear = clear; this.from = from;
        }
        public static void encode(SetEquip m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId); b.writeVarInt(m.slot); b.writeBoolean(m.clear); b.writeVarInt(m.from);
        }
        public static SetEquip decode(FriendlyByteBuf b) {
            return new SetEquip(b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readVarInt());
        }
        private static final EquipmentSlot[] SLOTS = {
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD,
                EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };
        public static void handle(SetEquip m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null || !sp.hasPermissions(2)) return;
                NpcEntity npc = npcOf(sp, m.entityId);
                if (npc == null || m.slot < 0 || m.slot >= SLOTS.length) return;
                int from = Math.max(0, Math.min(35, m.from));
                ItemStack stack = m.clear ? ItemStack.EMPTY : sp.getInventory().getItem(from).copyWithCount(1);
                npc.setItemSlot(SLOTS[m.slot], stack);
                npc.setDropChance(SLOTS[m.slot], 0.0F);
            });
            ctx.get().setPacketHandled(true);
        }
    }

    /** Acciones del diálogo: kind 0 = nodo mostrado (sonido/comando), 1 = opción elegida, 2 = fin del diálogo. */
    public static final class DialogAction {
        public final int entityId;
        public final int kind;
        public final int node;
        public final int choice;

        public DialogAction(int entityId, int kind, int node, int choice) {
            this.entityId = entityId; this.kind = kind; this.node = node; this.choice = choice;
        }
        public static void encode(DialogAction m, FriendlyByteBuf b) {
            b.writeVarInt(m.entityId); b.writeVarInt(m.kind); b.writeVarInt(m.node); b.writeVarInt(m.choice);
        }
        public static DialogAction decode(FriendlyByteBuf b) {
            return new DialogAction(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
        }
        public static void handle(DialogAction m, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer sp = ctx.get().getSender();
                if (sp == null) return;
                NpcEntity npc = npcOf(sp, m.entityId);
                if (npc == null || npc.distanceToSqr(sp) > 16 * 16) return;
                switch (m.kind) {
                    case 0 -> npc.runNodeCommand(m.node, sp);
                    case 1 -> npc.runChoiceCommand(m.node, m.choice, sp);
                    case 2 -> npc.runCommandList("onEnd", sp);
                    default -> { }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    private Packets() {}
}
