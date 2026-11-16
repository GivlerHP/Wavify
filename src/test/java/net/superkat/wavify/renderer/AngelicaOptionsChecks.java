package net.superkat.wavify.renderer;

import com.google.common.collect.ImmutableList;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.superkat.wavify.compat.angelica.AngelicaWaveOptions;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.config.WavifyConfig.WaveMode;

/** Real optional Angelica option API, including native Apply and Undo semantics. */
public final class AngelicaOptionsChecks {
    static final String ROOT = "me.jellysquid.mods.sodium.client.gui.options.";
    static void check(boolean value, String message) {
        if (!value)
            throw new AssertionError(message);
    }

    static Object call(Object target, String name) throws Exception {
        return target.getClass().getMethod(name).invoke(target);
    }

    public static void main(String[] args) throws Exception {
        Field locale = net.minecraft.client.resources.I18n.class.getDeclaredField("i18nLocale");
        locale.setAccessible(true);
        locale.set(null, new net.minecraft.client.resources.Locale());
        WavifyConfig.enabled = true;
        WavifyConfig.enableOceanWaves = true;
        WavifyConfig.enableRiverWaves = true;
        AtomicInteger applied = new AtomicInteger();
        Object template = AngelicaWaveOptions.createGroup(applied::incrementAndGet);
        Object existing = ((List<?>) call(template, "getOptions")).get(0);
        Field name = existing.getClass().getDeclaredField("name");
        name.setAccessible(true);
        name.set(existing, "Existing Appearance Option");
        Object groupBuilder = Class.forName(ROOT + "OptionGroup").getMethod("createBuilder").invoke(null);
        groupBuilder.getClass().getMethod("add", Class.forName(ROOT + "Option")).invoke(groupBuilder,
                existing);
        Object existingGroup = call(groupBuilder, "build");
        Constructor<?> page = Class.forName(ROOT + "OptionPage").getConstructor(String.class,
                ImmutableList.class);
        List<Object> pages = new ArrayList<Object>();
        Object other = page.newInstance("Other", ImmutableList.of(existingGroup));
        pages.add(other);
        pages.add(page.newInstance("sodium.options.pages.appearance", ImmutableList.of(existingGroup)));
        check(AngelicaWaveOptions.attachPages(pages, applied::incrementAndGet),
                "Appearance group was not attached");
        check(pages.get(0) == other && ((List<?>) call(pages.get(1), "getGroups")).size() == 2,
                "Other settings/groups changed");
        check(!AngelicaWaveOptions.attachPages(pages, applied::incrementAndGet), "Duplicate Wavify group");
        List<?> options = (List<?>) call(pages.get(1), "getOptions");
        check(options.size() == 3 && options.get(0) == existing, "Existing appearance option lost");
        Object enabled = options.get(1), mode = options.get(2);
        check(call(enabled, "getControl").getClass().getSimpleName().equals("TickBoxControl"),
                "Native toggle missing");
        check(call(mode, "getControl").getClass().getSimpleName().equals("CyclingControl"),
                "Native three-mode selector missing");
        enabled.getClass().getMethod("setValue", Object.class).invoke(enabled, false);
        mode.getClass().getMethod("setValue", Object.class).invoke(mode, WaveMode.RIVERS);
        check(WavifyConfig.enabled && WavifyConfig.mode() == WaveMode.BOTH && applied.get() == 0,
                "Pending edit applied too soon");
        call(enabled, "reset");
        call(mode, "reset");
        check(!(Boolean) call(enabled, "hasChanged") && !(Boolean) call(mode, "hasChanged"),
                "Undo did not restore options");
        enabled.getClass().getMethod("setValue", Object.class).invoke(enabled, false);
        mode.getClass().getMethod("setValue", Object.class).invoke(mode, WaveMode.RIVERS);
        call(enabled, "applyChanges");
        call(mode, "applyChanges");
        Object storage = call(enabled, "getStorage");
        Class.forName(ROOT + "storage.OptionStorage").getMethod("save").invoke(storage);
        check(!WavifyConfig.isActive() && WavifyConfig.mode() == WaveMode.RIVERS && applied.get() == 1,
                "Apply did not persist mode and invoke shutdown");
        System.out.println(
                "Real Angelica API: separate Appearance group, native toggle/three-mode selector, idempotent attach, Apply/Undo and shutdown callback passed");
    }
}
