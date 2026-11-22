package com.npcmod.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.npcmod.network.NpcNetwork;
import com.npcmod.network.Packets;

public class NpcEntity extends PathfinderMob {
    private static final EntityDataAccessor<CompoundTag> DATA =
            SynchedEntityData.defineId(NpcEntity.class, EntityDataSerializers.COMPOUND_TAG);

    public NpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA, NpcData.defaults());
    }

    public CompoundTag data() {
        return this.entityData.get(DATA);
    }

    public void setData(CompoundTag tag) {
        this.entityData.set(DATA, NpcData.merged(tag.copy()));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new GatedWander(this));
        this.goalSelector.addGoal(2, new GatedLook(this));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    private static final class GatedWander extends WaterAvoidingRandomStrollGoal {
        private final NpcEntity npc;
        GatedWander(NpcEntity npc) { super(npc, 0.8D); this.npc = npc; }
        @Override public boolean canUse() { return npc.data().getBoolean("wander") && super.canUse(); }
        @Override public boolean canContinueToUse() { return npc.data().getBoolean("wander") && super.canContinueToUse(); }
    }

    private static final class GatedLook extends LookAtPlayerGoal {
        private final NpcEntity npc;
        GatedLook(NpcEntity npc) { super(npc, Player.class, 8.0F); this.npc = npc; }
        @Override public boolean canUse() { return npc.data().getBoolean("look") && super.canUse(); }
        @Override public boolean canContinueToUse() { return npc.data().getBoolean("look") && super.canContinueToUse(); }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        // con la varita de edición en cualquier mano, el clic lo atiende el ítem (abre el editor)
        if (player.getMainHandItem().is(com.npcmod.ModRegistry.EDITOR.get())
                || player.getOffhandItem().is(com.npcmod.ModRegistry.EDITOR.get())) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) {
            runCommandList("onClick", sp);
            String name = this.hasCustomName() ? this.getCustomName().getString() : "";
            NpcNetwork.sendTo(sp, new Packets.OpenDialog(this.getId(), name, this.data()));
        }
        return InteractionResult.CONSUME;
    }

    // ---------------------------------------------------------------- comandos

    public void runCommandList(String key, ServerPlayer player) {
        ListTag list = data().getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) runCommand(list.getString(i), player);
    }

    public void runNodeCommand(int node, ServerPlayer player) {
        ListTag nodes = data().getList("nodes", Tag.TAG_COMPOUND);
        if (node < 0 || node >= nodes.size()) return;
        runCommand(nodes.getCompound(node).getString("command"), player);
    }

    public void runChoiceCommand(int node, int choice, ServerPlayer player) {
        ListTag nodes = data().getList("nodes", Tag.TAG_COMPOUND);
        if (node < 0 || node >= nodes.size()) return;
        ListTag choices = nodes.getCompound(node).getList("choices", Tag.TAG_COMPOUND);
        if (choice < 0 || choice >= choices.size()) return;
        runCommand(choices.getCompound(choice).getString("command"), player);
    }

    /** Ejecuta un comando desde la posición del NPC con permiso 2. %player% = jugador que habla. */
    private void runCommand(String raw, ServerPlayer player) {
        if (raw == null) return;
        String cmd = raw.trim();
        if (cmd.isEmpty() || this.level().getServer() == null) return;
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        cmd = cmd.replace("%player%", player.getGameProfile().getName());
        try {
            this.level().getServer().getCommands().performPrefixedCommand(
                    this.createCommandSourceStack().withPermission(2).withSuppressedOutput(), cmd);
        } catch (Exception ignored) {
        }
    }

    // ---------------------------------------------------------------- comportamiento

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (data().getBoolean("invuln") && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        float s = data().getFloat("scale");
        if (s <= 0.0F) s = 1.0F;
        return super.getDimensions(pose).scale(Mth.clamp(s, 0.1F, 10.0F));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA.equals(key)) this.refreshDimensions();
        super.onSyncedDataUpdated(key);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put("NpcData", data().copy());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("NpcData")) setData(tag.getCompound("NpcData"));
    }
}
