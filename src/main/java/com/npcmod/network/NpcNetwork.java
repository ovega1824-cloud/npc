package com.npcmod.network;

import com.npcmod.NpcMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public final class NpcNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NpcMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(id++, Packets.OpenDialog.class, Packets.OpenDialog::encode, Packets.OpenDialog::decode,
                Packets.OpenDialog::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Packets.OpenEditor.class, Packets.OpenEditor::encode, Packets.OpenEditor::decode,
                Packets.OpenEditor::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(id++, Packets.SaveNpc.class, Packets.SaveNpc::encode, Packets.SaveNpc::decode,
                Packets.SaveNpc::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.SetEquip.class, Packets.SetEquip::encode, Packets.SetEquip::decode,
                Packets.SetEquip::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(id++, Packets.DialogAction.class, Packets.DialogAction::encode, Packets.DialogAction::decode,
                Packets.DialogAction::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    public static void sendTo(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    private NpcNetwork() {}
}
