package net.superkat.wavify.compat;

import java.lang.reflect.Method;

/** Optional detection, resolved once. A selected-but-disabled Iris pack is not active. */
public final class ShaderState {
    private static final Object API;
    private static final Method ACTIVE, PACK;
    static {
        Object api = null;
        Method active = null, pack = null;
        try {
            Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api = c.getMethod("getInstance").invoke(null);
            active = c.getMethod("isShaderPackInUse");
        } catch (ReflectiveOperationException | LinkageError absent) {
            try {
                pack = Class.forName("shadersmod.client.Shaders").getMethod("getShaderPackName");
            } catch (ReflectiveOperationException | LinkageError ignored) {
            }
        }
        API = api;
        ACTIVE = active;
        PACK = pack;
    }
    private ShaderState() {
    }

    public static boolean active() {
        try {
            if (ACTIVE != null)
                return Boolean.TRUE.equals(ACTIVE.invoke(API));
            if (PACK != null) {
                Object p = PACK.invoke(null);
                return p != null && !p.toString().isEmpty() && !"OFF".equalsIgnoreCase(p.toString())
                        && !"(internal)".equalsIgnoreCase(p.toString());
            }
        } catch (ReflectiveOperationException | LinkageError ignored) {
        }
        return false;
    }
}
