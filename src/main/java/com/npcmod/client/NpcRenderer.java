package com.npcmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.npcmod.client.model.GeoModel;
import com.npcmod.entity.NpcEntity;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class NpcRenderer extends HumanoidMobRenderer<NpcEntity, HumanoidModel<NpcEntity>> {
    private static final ResourceLocation STEVE = new ResourceLocation("textures/entity/player/wide/steve.png");

    public NpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity npc) {
        NpcAssets.Img t = NpcAssets.texture(npc.data().getString("texture"));
        return t != null ? t.loc() : STEVE;
    }

    @Override
    protected void scale(NpcEntity npc, PoseStack ps, float partial) {
        float s = npc.data().getFloat("scale");
        if (s <= 0.0F) s = 1.0F;
        ps.scale(s, s, s);
    }

    @Override
    public void render(NpcEntity npc, float yaw, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        GeoModel geo = NpcAssets.model(npc.data().getString("model"));
        if (geo == null) {
            super.render(npc, yaw, partial, ps, buf, light);
            return;
        }
        renderGeo(npc, geo, partial, ps, buf, light);
        // nombre flotante (el renderer vanilla lo gestiona en super.render, aquí lo hacemos a mano)
        if (this.shouldShowName(npc)) {
            this.renderNameTag(npc, npc.getDisplayName(), ps, buf, light);
        }
    }

    private void renderGeo(NpcEntity npc, GeoModel geo, float partial, PoseStack ps, MultiBufferSource buf, int light) {
        NpcAssets.Img tex = NpcAssets.texture(npc.data().getString("texture"));
        ResourceLocation loc = tex != null ? tex.loc() : STEVE;
        float s = npc.data().getFloat("scale");
        if (s <= 0.0F) s = 1.0F;
        float body = Mth.rotLerp(partial, npc.yBodyRotO, npc.yBodyRot);

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - body));
        ps.scale(s, s, s);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(loc));
        geo.render(ps, vc, light);

        // objeto en mano principal anclado a un hueso llamado "righthand"/"rightitem"/"rightarm"
        ItemStack main = npc.getMainHandItem();
        if (!main.isEmpty()) {
            GeoModel.Bone b = geo.find("righthand", "rightitem", "rightarm");
            if (b != null) {
                ps.pushPose();
                if (geo.applyChain(b, ps)) {
                    ps.translate(0.0F, -0.5F, 0.0F);
                    ps.scale(1.0F, -1.0F, 1.0F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(main, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                            light, OverlayTexture.NO_OVERLAY, ps, buf, npc.level(), npc.getId());
                }
                ps.popPose();
            }
        }
        ps.popPose();
    }
}
