package net.superkat.wavify.renderer;

import java.io.File;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraftforge.common.config.Configuration;
import net.superkat.wavify.client.*;
import net.superkat.wavify.compat.*;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.config.WavifyConfig.WaveMode;
import sun.misc.Unsafe;

/** Exercise actual handler shutdown without a Minecraft window or graphics context. */
public final class WaveLifecycleChecks {
    static void check(boolean condition, String message) {
        if (!condition)
            throw new AssertionError(message);
    }

    static Object get(Object owner, String name) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(owner);
    }

    static void set(Object owner, String name, Object value) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(owner, value);
    }

    public static void run() {
        try {
            runChecked();
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }

    private static void runChecked() throws Exception {
        Field home = cpw.mods.fml.relauncher.FMLInjectionData.class.getDeclaredField("minecraftHome");
        home.setAccessible(true);
        home.set(null, new File("."));
        File file = new File("build/wave-options-check/wavify.cfg");
        file.getParentFile().mkdirs();
        Configuration config = new Configuration(file);
        config.load();
        config.get("waves", "rainWaveScale", 1.35).set(1.7);
        config.save();
        WavifyConfig.load(file);
        for (WaveMode mode : WaveMode.values()) {
            WavifyConfig.apply(true, mode);
            check(WavifyConfig.isActive() && WavifyConfig.mode() == mode, "Mode not applied");
            WavifyConfig.load(file);
            check(WavifyConfig.mode() == mode, "Mode did not persist");
        }
        WavifyConfig.apply(false, WaveMode.RIVERS);
        WavifyConfig.load(file);
        check(!WavifyConfig.isActive() && WavifyConfig.mode() == WaveMode.RIVERS,
                "Disabled mode did not persist");
        check(Math.abs(WavifyConfig.rainWaveScale - 1.7) < .0001, "UI save overwrote wave tuning");

        WavifyConfig.apply(true, WaveMode.BOTH);
        ClientProxy client = new ClientProxy();
        WaveScene scene = new WaveScene(new WorldAccess(null), 1);
        set(client, "scene", scene);
        scene.wet.put(new BlockPos(0, 64, 0), 100L);
        SurfaceCache cache = scene.surface;
        ExecutorService worker = (ExecutorService) get(cache, "worker");
        CountDownLatch started = new CountDownLatch(1);
        Future<?> pending = worker.submit(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException expected) {
                Thread.currentThread().interrupt();
            }
        });
        set(cache, "pending", pending);
        check(started.await(2, TimeUnit.SECONDS), "Scan worker did not start");
        WaveRenderer renderer = (WaveRenderer) get(client, "renderer");
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        ((List<Object>) get(renderer, "visible"))
                .add(unsafe.allocateInstance(net.superkat.wavify.wave.Wave.class));
        WavifyConfig.apply(false, WaveMode.BOTH);
        client.applyOptions();
        check(get(client, "scene") == null && scene.wet.isEmpty(), "Disabled handler still owns scene data");
        check(((List<?>) get(renderer, "visible")).isEmpty(), "Renderer retained old wave/world references");
        check(pending.isCancelled() && worker.isShutdown() && worker.awaitTermination(2, TimeUnit.SECONDS),
                "Disable did not stop the shoreline worker");
        cache.tick(0, 0);
        scene.tick(); // Must return before any access to the null Minecraft/world fixture.
        client.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        client.tick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
        check(get(client, "scene") == null, "Disabled ticks restarted processing");
        net.superkat.wavify.particles.WaveSpray spray = (net.superkat.wavify.particles.WaveSpray) unsafe
                .allocateInstance(net.superkat.wavify.particles.WaveSpray.class);
        net.superkat.wavify.particles.WaveSplash splash = (net.superkat.wavify.particles.WaveSplash) unsafe
                .allocateInstance(net.superkat.wavify.particles.WaveSplash.class);
        spray.onUpdate();
        splash.onUpdate();
        check(spray.isDead && splash.isDead, "Disabled particles continued processing");
        WavifyConfig.apply(true, WaveMode.RIVERS);
        WaveScene replacement = new WaveScene(new WorldAccess(null), 2);
        check(!((ExecutorService) get(replacement.surface, "worker")).isShutdown(),
                "New mode reused a stopped worker");
        replacement.close();
        System.out.println(
                "Wavify lifecycle: saved modes, interrupted worker, closed-scene guards, cleared render references, stopped particles and fresh restart passed");
    }
}
