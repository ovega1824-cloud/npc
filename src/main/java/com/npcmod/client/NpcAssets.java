package com.npcmod.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.npcmod.NpcMod;
import com.npcmod.client.model.GeoModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Carpeta del usuario: .minecraft/npcmod/
 *   models/    -> *.geo.json (modelos de BBS mod / Blockbench)
 *   textures/  -> *.png (texturas de los modelos y skins)
 *   images/    -> *.png (imágenes para la caja de diálogo y retratos)
 */
public final class NpcAssets {

    public record Img(ResourceLocation loc, int w, int h) {}

    private static final Map<String, GeoModel> MODELS = new HashMap<>();
    private static final Map<String, Img> IMAGES = new HashMap<>();
    private static final Map<String, Img> TEXTURES = new HashMap<>();
    private static final Img MISSING = new Img(null, 0, 0);

    public static Path dir(String sub) {
        Path p = FMLPaths.GAMEDIR.get().resolve("npcmod").resolve(sub);
        try { Files.createDirectories(p); } catch (IOException ignored) { }
        return p;
    }

    public static void init() {
        dir("models"); dir("textures"); dir("images");
    }

    public static List<String> list(String sub, String suffix) {
        List<String> out = new ArrayList<>();
        try (Stream<Path> s = Files.list(dir(sub))) {
            s.filter(Files::isRegularFile).forEach(f -> {
                String n = f.getFileName().toString();
                if (n.toLowerCase().endsWith(suffix)) out.add(n.substring(0, n.length() - suffix.length()));
            });
        } catch (IOException ignored) { }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static List<String> modelNames() { return list("models", ".geo.json"); }
    public static List<String> imageNames() { return list("images", ".png"); }
    public static List<String> textureNames() { return list("textures", ".png"); }

    /** Recarga todo desde disco (botón "Recargar" del editor). */
    public static void reload() {
        MODELS.clear();
        Minecraft mc = Minecraft.getInstance();
        for (Map<String, Img> map : List.of(IMAGES, TEXTURES)) {
            for (Img i : map.values()) if (i.loc() != null) mc.getTextureManager().release(i.loc());
            map.clear();
        }
    }

    public static GeoModel model(String name) {
        if (name == null || name.isEmpty() || name.equals("default")) return null;
        if (MODELS.containsKey(name)) return MODELS.get(name);
        GeoModel m = null;
        Path f = dir("models").resolve(safe(name) + ".geo.json");
        if (Files.isRegularFile(f)) {
            try (Reader r = Files.newBufferedReader(f)) {
                JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                m = GeoModel.parse(o);
            } catch (Exception e) {
                System.err.println("[npcmod] No se pudo leer el modelo " + name + ": " + e);
            }
        }
        MODELS.put(name, m);
        return m;
    }

    public static Img image(String name) { return load(IMAGES, "images", name); }
    public static Img texture(String name) { return load(TEXTURES, "textures", name); }

    private static Img load(Map<String, Img> cache, String sub, String name) {
        if (name == null || name.isEmpty()) return null;
        Img c = cache.get(name);
        if (c != null) return c == MISSING ? null : c;
        Path f = dir(sub).resolve(safe(name) + ".png");
        if (Files.isRegularFile(f)) {
            try (InputStream in = Files.newInputStream(f)) {
                NativeImage ni = NativeImage.read(in);
                ResourceLocation loc = new ResourceLocation(NpcMod.MODID,
                        "dyn/" + sub + "/" + Integer.toHexString(name.hashCode()) + "_" + cache.size());
                Img img = new Img(loc, ni.getWidth(), ni.getHeight());
                Minecraft.getInstance().getTextureManager().register(loc, new DynamicTexture(ni));
                cache.put(name, img);
                return img;
            } catch (Exception e) {
                System.err.println("[npcmod] No se pudo leer la imagen " + name + ": " + e);
            }
        }
        cache.put(name, MISSING);
        return null;
    }

    /** Evita rutas con .. o separadores. */
    private static String safe(String n) {
        return n.replace("\\", "").replace("/", "").replace("..", "");
    }

    private NpcAssets() {}
}
