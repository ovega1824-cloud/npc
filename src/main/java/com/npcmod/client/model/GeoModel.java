package com.npcmod.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modelo estilo Bedrock (.geo.json) como los que exporta Blockbench / BBS mod.
 * Soporta formato 1.12.0 ("minecraft:geometry") y el antiguo ("geometry.nombre"),
 * UV por caja y UV por cara, inflate, pivotes y rotaciones de huesos y cubos.
 */
public class GeoModel {

    public static final class Cube {
        float x, y, z, w, h, d, inflate;
        float[] pivot; float[] rot;
        float u, v; boolean boxUv = true;
        JsonObject faceUv;
    }

    public static final class Bone {
        public String name, parent;
        float[] pivot = {0, 0, 0};
        float[] rot = {0, 0, 0};
        final List<Cube> cubes = new ArrayList<>();
        final List<Bone> children = new ArrayList<>();
    }

    public final List<Bone> roots = new ArrayList<>();
    public final Map<String, Bone> bones = new HashMap<>();
    public int texW = 64, texH = 64;

    // ------------------------------------------------------------------ carga

    public static GeoModel parse(JsonObject root) {
        JsonObject geo = null;
        int tw = 64, th = 64;
        if (root.has("minecraft:geometry")) {
            JsonArray arr = root.getAsJsonArray("minecraft:geometry");
            if (arr.size() == 0) return null;
            geo = arr.get(0).getAsJsonObject();
            if (geo.has("description")) {
                JsonObject desc = geo.getAsJsonObject("description");
                if (desc.has("texture_width")) tw = desc.get("texture_width").getAsInt();
                if (desc.has("texture_height")) th = desc.get("texture_height").getAsInt();
            }
        } else {
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                if (e.getKey().startsWith("geometry.") && e.getValue().isJsonObject()) {
                    geo = e.getValue().getAsJsonObject();
                    if (geo.has("texturewidth")) tw = geo.get("texturewidth").getAsInt();
                    if (geo.has("textureheight")) th = geo.get("textureheight").getAsInt();
                    break;
                }
            }
        }
        if (geo == null || !geo.has("bones")) return null;

        GeoModel m = new GeoModel();
        m.texW = Math.max(1, tw);
        m.texH = Math.max(1, th);

        for (JsonElement be : geo.getAsJsonArray("bones")) {
            JsonObject bo = be.getAsJsonObject();
            Bone b = new Bone();
            b.name = bo.has("name") ? bo.get("name").getAsString() : "bone" + m.bones.size();
            b.parent = bo.has("parent") ? bo.get("parent").getAsString() : null;
            if (bo.has("pivot")) b.pivot = floats(bo.getAsJsonArray("pivot"), 3);
            if (bo.has("rotation")) b.rot = floats(bo.getAsJsonArray("rotation"), 3);
            if (bo.has("cubes")) {
                for (JsonElement ce : bo.getAsJsonArray("cubes")) {
                    JsonObject co = ce.getAsJsonObject();
                    if (!co.has("origin") || !co.has("size")) continue;
                    Cube c = new Cube();
                    float[] o = floats(co.getAsJsonArray("origin"), 3);
                    float[] s = floats(co.getAsJsonArray("size"), 3);
                    c.x = o[0]; c.y = o[1]; c.z = o[2];
                    c.w = s[0]; c.h = s[1]; c.d = s[2];
                    if (co.has("inflate")) c.inflate = co.get("inflate").getAsFloat();
                    if (co.has("pivot")) c.pivot = floats(co.getAsJsonArray("pivot"), 3);
                    if (co.has("rotation")) c.rot = floats(co.getAsJsonArray("rotation"), 3);
                    if (co.has("uv")) {
                        JsonElement uv = co.get("uv");
                        if (uv.isJsonArray()) {
                            float[] f = floats(uv.getAsJsonArray(), 2);
                            c.u = f[0]; c.v = f[1];
                        } else if (uv.isJsonObject()) {
                            c.boxUv = false;
                            c.faceUv = uv.getAsJsonObject();
                        }
                    }
                    b.cubes.add(c);
                }
            }
            m.bones.put(b.name, b);
        }
        for (Bone b : m.bones.values()) {
            Bone p = b.parent != null ? m.bones.get(b.parent) : null;
            if (p != null && p != b) p.children.add(b); else m.roots.add(b);
        }
        return m;
    }

    private static float[] floats(JsonArray a, int n) {
        float[] r = new float[n];
        for (int i = 0; i < n && i < a.size(); i++) r[i] = a.get(i).getAsFloat();
        return r;
    }

    // ------------------------------------------------------------------ render

    /** Dibuja el modelo. El PoseStack debe estar ya orientado (rotación del cuerpo + escala). */
    public void render(PoseStack ps, VertexConsumer vc, int light) {
        ps.pushPose();
        ps.scale(-1.0F, 1.0F, 1.0F);   // Bedrock -> mundo (espejo en X)
        for (Bone b : roots) renderBone(b, ps, vc, light);
        ps.popPose();
    }

    private void renderBone(Bone b, PoseStack ps, VertexConsumer vc, int light) {
        ps.pushPose();
        applyPivotRot(ps, b.pivot, b.rot);
        for (Cube c : b.cubes) renderCube(c, ps, vc, light);
        for (Bone ch : b.children) renderBone(ch, ps, vc, light);
        ps.popPose();
    }

    private static void applyPivotRot(PoseStack ps, float[] pivot, float[] rot) {
        ps.translate(pivot[0] / 16F, pivot[1] / 16F, pivot[2] / 16F);
        if (rot[2] != 0) ps.mulPose(Axis.ZP.rotationDegrees(rot[2]));
        if (rot[1] != 0) ps.mulPose(Axis.YP.rotationDegrees(rot[1]));
        if (rot[0] != 0) ps.mulPose(Axis.XP.rotationDegrees(rot[0]));
        ps.translate(-pivot[0] / 16F, -pivot[1] / 16F, -pivot[2] / 16F);
    }

    /** Posición mundial (en bloques, relativa al modelo) del pivote de un hueso: útil para objetos en mano. */
    public Bone find(String... names) {
        for (Bone b : bones.values()) {
            String n = b.name.toLowerCase().replace("_", "");
            for (String q : names) if (n.equals(q)) return b;
        }
        return null;
    }

    /** Aplica las transformaciones de la cadena de huesos hasta el hueso dado (para anclar ítems). */
    public boolean applyChain(Bone target, PoseStack ps) {
        List<Bone> chain = new ArrayList<>();
        for (Bone b = target; b != null; b = b.parent != null ? bones.get(b.parent) : null) {
            chain.add(0, b);
            if (chain.size() > 64) return false;
        }
        ps.scale(-1.0F, 1.0F, 1.0F);
        for (Bone b : chain) applyPivotRot(ps, b.pivot, b.rot);
        return true;
    }

    private void renderCube(Cube c, PoseStack ps, VertexConsumer vc, int light) {
        ps.pushPose();
        if (c.rot != null) applyPivotRot(ps, c.pivot != null ? c.pivot : new float[]{0, 0, 0}, c.rot);

        float x0 = (c.x - c.inflate) / 16F, x1 = (c.x + c.w + c.inflate) / 16F;
        float y0 = (c.y - c.inflate) / 16F, y1 = (c.y + c.h + c.inflate) / 16F;
        float z0 = (c.z - c.inflate) / 16F, z1 = (c.z + c.d + c.inflate) / 16F;

        float u = c.u, v = c.v, w = c.w, h = c.h, d = c.d;
        PoseStack.Pose pose = ps.last();
        Matrix4f mat = pose.pose();
        Matrix3f nrm = pose.normal();

        // north (-Z, frente)
        face(vc, mat, nrm, light, 0, 0, -1,
                x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0,
                uv(c, "north", u + d, v + d, w, h));
        // south (+Z)
        face(vc, mat, nrm, light, 0, 0, 1,
                x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1,
                uv(c, "south", u + 2 * d + w, v + d, w, h));
        // east (+X)
        face(vc, mat, nrm, light, 1, 0, 0,
                x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1,
                uv(c, "east", u, v + d, d, h));
        // west (-X)
        face(vc, mat, nrm, light, -1, 0, 0,
                x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0,
                uv(c, "west", u + d + w, v + d, d, h));
        // up (+Y)
        face(vc, mat, nrm, light, 0, 1, 0,
                x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                uv(c, "up", u + d, v, w, d));
        // down (-Y)
        face(vc, mat, nrm, light, 0, -1, 0,
                x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0,
                uv(c, "down", u + d + w, v, w, d));
        ps.popPose();
    }

    /** Devuelve {u,v,w,h} en píxeles de textura, o null si la cara no existe (UV por cara). */
    private static float[] uv(Cube c, String face, float u, float v, float w, float h) {
        if (c.boxUv) return new float[]{u, v, w, h};
        if (c.faceUv == null || !c.faceUv.has(face)) return null;
        JsonObject f = c.faceUv.getAsJsonObject(face);
        if (!f.has("uv")) return null;
        float[] p = floats(f.getAsJsonArray("uv"), 2);
        float[] s = f.has("uv_size") ? floats(f.getAsJsonArray("uv_size"), 2) : new float[]{w, h};
        return new float[]{p[0], p[1], s[0], s[1]};
    }

    private void face(VertexConsumer vc, Matrix4f mat, Matrix3f nrm, int light,
                      float nx, float ny, float nz,
                      float ax, float ay, float az, float bx, float by, float bz,
                      float cx, float cy, float cz, float dx, float dy, float dz,
                      float[] t) {
        if (t == null) return;
        float u0 = t[0] / texW, v0 = t[1] / texH;
        float u1 = (t[0] + t[2]) / texW, v1 = (t[1] + t[3]) / texH;
        vert(vc, mat, nrm, light, ax, ay, az, u0, v0, nx, ny, nz); // TL
        vert(vc, mat, nrm, light, bx, by, bz, u1, v0, nx, ny, nz); // TR
        vert(vc, mat, nrm, light, cx, cy, cz, u1, v1, nx, ny, nz); // BR
        vert(vc, mat, nrm, light, dx, dy, dz, u0, v1, nx, ny, nz); // BL
    }

    private static void vert(VertexConsumer vc, Matrix4f mat, Matrix3f nrm, int light,
                             float x, float y, float z, float u, float v,
                             float nx, float ny, float nz) {
        org.joml.Vector3f n = nrm.transform(new org.joml.Vector3f(nx, ny, nz));
        vc.vertex(mat, x, y, z)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(n.x, n.y, n.z)
                .endVertex();
    }
}
