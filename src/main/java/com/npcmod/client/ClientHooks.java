package com.npcmod.client;

import com.npcmod.client.screen.DialogScreen;
import com.npcmod.client.screen.EditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Punto de entrada desde los paquetes (solo se carga en el cliente). */
public final class ClientHooks {

    public static void openDialog(int entityId, String name, CompoundTag data) {
        if (data == null) return;
        Minecraft.getInstance().setScreen(new DialogScreen(entityId, name, data));
    }

    public static void openEditor(int entityId, String name, boolean nameVisible, CompoundTag data) {
        if (data == null) return;
        Minecraft.getInstance().setScreen(new EditorScreen(entityId, name, nameVisible, data));
    }

    private ClientHooks() {}
}
