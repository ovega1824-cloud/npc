package com.npcmod.item;

import com.npcmod.ModRegistry;
import com.npcmod.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class NpcSpawnerItem extends Item {
    public NpcSpawnerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.hasPermissions(2)) {
            player.displayClientMessage(Component.literal("§cNecesitas permisos de operador (nivel 2) o trucos activados."), true);
            return InteractionResult.FAIL;
        }
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        NpcEntity npc = ModRegistry.NPC.get().create(level);
        if (npc == null) return InteractionResult.FAIL;
        float yaw = player.getYRot() + 180.0F;
        npc.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, yaw, 0.0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setCustomName(Component.literal("NPC"));
        npc.setCustomNameVisible(true);
        level.addFreshEntity(npc);
        return InteractionResult.CONSUME;
    }
}
