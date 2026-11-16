package net.superkat.wavify.compat.angelica;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.config.WavifyConfig.WaveMode;

/** Optional native Angelica Apply/Undo controls; no Angelica classes are linked by the mod. */
public final class AngelicaWaveOptions {
    private static final String ROOT = "me.jellysquid.mods.sodium.client.gui.options.";
    private AngelicaWaveOptions() {
    }

    public static void attach(GuiScreen screen, Runnable apply) {
        if (!screen.getClass().getName()
                .equals("me.flashyreese.mods.reeses_sodium_options.client.gui.ReeseSodiumVideoOptionsScreen"))
            return;
        try {
            Field field = screen.getClass().getDeclaredField("pages");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Object> pages = (List<Object>) field.get(screen);
            if (attachPages(pages, apply))
                screen.getClass().getMethod("rebuildGUI").invoke(screen);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Cannot add Wavify appearance options to Angelica", error);
        }
    }

    public static boolean attachPages(List<Object> pages, Runnable apply)
            throws ReflectiveOperationException {
        String appearance = I18n.format("sodium.options.pages.appearance");
        for (int i = 0; i < pages.size(); i++) {
            Object page = pages.get(i);
            if (!appearance.equals(page.getClass().getMethod("getName").invoke(page)))
                continue;
            for (Object option : (List<?>) page.getClass().getMethod("getOptions").invoke(page))
                if (I18n.format("wavify.options.enabled")
                        .equals(option.getClass().getMethod("getName").invoke(option)))
                    return false;
            List<Object> groups = new ArrayList<Object>(
                    (List<?>) page.getClass().getMethod("getGroups").invoke(page));
            groups.add(createGroup(apply));
            pages.set(i, page.getClass().getConstructor(String.class, ImmutableList.class)
                    .newInstance(appearance, ImmutableList.copyOf(groups)));
            return true;
        }
        return false;
    }

    public static Object createGroup(Runnable apply) throws ReflectiveOperationException {
        final Map<String, Object> values = new HashMap<String, Object>();
        values.put("enabled", WavifyConfig.enabled);
        values.put("mode", WavifyConfig.mode());
        Class<?> storageType = Class.forName(ROOT + "storage.OptionStorage");
        Object storage = Proxy.newProxyInstance(storageType.getClassLoader(), new Class<?>[]{storageType},
                (p, m, a) -> {
                    if (m.getName().equals("getData"))
                        return values;
                    if (m.getName().equals("save")) {
                        WavifyConfig.apply((Boolean) values.get("enabled"), (WaveMode) values.get("mode"));
                        apply.run();
                        return null;
                    }
                    if (m.getName().equals("hashCode"))
                        return System.identityHashCode(p);
                    if (m.getName().equals("equals"))
                        return p == a[0];
                    return "Wavify appearance settings";
                });
        Object group = Class.forName(ROOT + "OptionGroup").getMethod("createBuilder").invoke(null);
        add(group, storage, storageType, "enabled", Boolean.class);
        add(group, storage, storageType, "mode", WaveMode.class);
        return group.getClass().getMethod("build").invoke(group);
    }

    private static void add(Object group, Object storage, Class<?> storageType, String key, Class<?> type)
            throws ReflectiveOperationException {
        Object builder = Class.forName(ROOT + "OptionImpl")
                .getMethod("createBuilder", Class.class, storageType).invoke(null, type, storage);
        builder.getClass().getMethod("setName", String.class).invoke(builder,
                I18n.format("wavify.options." + key));
        builder.getClass().getMethod("setTooltip", String.class).invoke(builder,
                I18n.format("wavify.options." + key + ".tooltip"));
        BiConsumer<Map<String, Object>, Object> setter = (data, value) -> data.put(key, value);
        Function<Map<String, Object>, Object> getter = data -> data.get(key);
        builder.getClass().getMethod("setBinding", BiConsumer.class, Function.class).invoke(builder, setter,
                getter);
        Function<Object, Object> control = option -> {
            try {
                Class<?> optionType = Class.forName(ROOT + "Option");
                if (type == Boolean.class)
                    return Class.forName(ROOT + "control.TickBoxControl").getConstructor(optionType)
                            .newInstance(option);
                String[] labels = {I18n.format("wavify.options.mode.seas"),
                        I18n.format("wavify.options.mode.rivers"), I18n.format("wavify.options.mode.both")};
                return Class.forName(ROOT + "control.CyclingControl")
                        .getConstructor(optionType, Class.class, String[].class)
                        .newInstance(option, WaveMode.class, labels);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Cannot create native Wavify control", error);
            }
        };
        builder.getClass().getMethod("setControl", Function.class).invoke(builder, control);
        Object option = builder.getClass().getMethod("build").invoke(builder);
        group.getClass().getMethod("add", Class.forName(ROOT + "Option")).invoke(group, option);
    }
}
