package com.sow.wegui.client;

import com.sow.wegui.WeGuiMod;
import com.sow.wegui.WeStatusSnapshot;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;

/**
 * WorldEdit 反射桥接。
 *
 * 通过反射读取 WE 的 LocalSession / Region / Clipboard，构造 WeStatusSnapshot。
 * 首次成功后缓存反射方法；失败时返回无 WE 或无选区状态，不会抛异常。
 */
public final class WorldEditBridge {
    private static final String MOD_ID = "worldedit";
    private static volatile Boolean loaded;

    private static boolean reflectionReady = false;
    private static Method adaptPlayerMethod;
    private static Method getInstanceMethod;
    private static Method getSessionManagerMethod;
    private static Method sessionGetMethod;
    private static Method getSelectionWorldMethod;
    private static Method getSelectionMethod;
    private static Method getClipboardMethod;
    private static Method getWidthMethod;
    private static Method getHeightMethod;
    private static Method getLengthMethod;
    private static Method getVolumeMethod;
    private static Method getMinimumPointMethod;
    private static Method getMaximumPointMethod;

    private WorldEditBridge() {}

    public static boolean isLoaded() {
        if (loaded == null) loaded = FabricLoader.getInstance().isModLoaded(MOD_ID);
        return loaded;
    }

    public static WeStatusSnapshot capture(Minecraft mc) {
        if (!isLoaded() || mc.player == null) return WeStatusSnapshot.noWorldEdit();
        try {
            return captureViaReflection(mc.player);
        } catch (Throwable e) {
            WeGuiMod.LOGGER.debug("[WE GUI] 读取 WE 状态失败: {}", e.toString());
            return WeStatusSnapshot.noWorldEdit();
        }
    }

    private static void ensureReflection() throws ReflectiveOperationException {
        if (reflectionReady) return;

        Class<?> fa = Class.forName("com.sk89q.worldedit.fabric.FabricAdapter");
        adaptPlayerMethod = fa.getMethod("adaptPlayer", Player.class);

        Class<?> we = Class.forName("com.sk89q.worldedit.WorldEdit");
        getInstanceMethod = we.getMethod("getInstance");
        getSessionManagerMethod = we.getMethod("getSessionManager");

        Class<?> actor = Class.forName("com.sk89q.worldedit.extension.platform.Actor");
        sessionGetMethod = getSessionManagerMethod.getReturnType().getMethod("get", actor);

        Class<?> session = sessionGetMethod.getReturnType();
        getSelectionWorldMethod = session.getMethod("getSelectionWorld");
        getSelectionMethod = session.getMethod("getSelection", Class.forName("com.sk89q.worldedit.world.World"));
        getClipboardMethod = session.getMethod("getClipboard");

        Class<?> region = Class.forName("com.sk89q.worldedit.regions.Region");
        getWidthMethod = region.getMethod("getWidth");
        getHeightMethod = region.getMethod("getHeight");
        getLengthMethod = region.getMethod("getLength");
        getVolumeMethod = region.getMethod("getVolume");
        getMinimumPointMethod = region.getMethod("getMinimumPoint");
        getMaximumPointMethod = region.getMethod("getMaximumPoint");

        reflectionReady = true;
    }

    private static WeStatusSnapshot captureViaReflection(Player player) throws ReflectiveOperationException {
        ensureReflection();

        Object wePlayer = adaptPlayerMethod.invoke(null, player);
        Object we = getInstanceMethod.invoke(null);
        Object sm = getSessionManagerMethod.invoke(we);
        Object session = sessionGetMethod.invoke(sm, wePlayer);

        String version = readVersion(we);
        boolean clipboard = hasClipboard(session);

        Object selWorld = getSelectionWorldMethod.invoke(session);
        if (selWorld == null) return WeStatusSnapshot.noSelection(clipboard, version);

        Object region = getSelectionMethod.invoke(session, selWorld);
        if (region == null) return WeStatusSnapshot.noSelection(clipboard, version);

        int w = ((Number) getWidthMethod.invoke(region)).intValue();
        int h = ((Number) getHeightMethod.invoke(region)).intValue();
        int l = ((Number) getLengthMethod.invoke(region)).intValue();
        long vol = ((Number) getVolumeMethod.invoke(region)).longValue();

        String min = formatVector(getMinimumPointMethod.invoke(region));
        String max = formatVector(getMaximumPointMethod.invoke(region));

        return WeStatusSnapshot.ready(w, h, l, vol, clipboard, version, min, max);
    }

    private static String readVersion(Object we) {
        try {
            Object pm = we.getClass().getMethod("getPlatformManager").invoke(we);
            Object p = pm.getClass().getMethod("getPlatform").invoke(pm);
            Object v = p.getClass().getMethod("getVersion").invoke(p);
            return v != null ? v.toString() : "";
        } catch (Throwable e) {
            return "";
        }
    }

    private static boolean hasClipboard(Object session) {
        try {
            Object cb = getClipboardMethod.invoke(session);
            return cb != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private static String formatVector(Object weVec) {
        try {
            Method getX = weVec.getClass().getMethod("getX");
            Method getY = weVec.getClass().getMethod("getY");
            Method getZ = weVec.getClass().getMethod("getZ");
            int x = ((Number) getX.invoke(weVec)).intValue();
            int y = ((Number) getY.invoke(weVec)).intValue();
            int z = ((Number) getZ.invoke(weVec)).intValue();
            return x + "," + y + "," + z;
        } catch (Throwable e) {
            return "-";
        }
    }
}
