package com.npcmod.item;

import com.npcmod.entity.NpcEntity;
import com.npcmod.network.NpcNetwork;
import com.npcmod.network.Packets;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class NpcEditorItem extends Item {
    public NpcEditorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof NpcEntity npc)) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            if (!sp.hasPermissions(2)) {
                sp.displayClientMessage(Component.literal("§cNecesitas permisos de operador (nivel 2) o trucos activados."), true);
                return InteractionResult.FAIL;
            }
            String name = npc.hasCustomName() ? npc.getCustomName().getString() : "";
            NpcNetwork.sendTo(sp, new Packets.OpenEditor(npc.getId(), name, npc.isCustomNameVisible(), npc.data()));
        }
        return InteractionResult.CONSUME;
    }
}
