package net.superkat.wavify.client;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.client.Minecraft;
import net.superkat.wavify.compat.*;
import net.superkat.wavify.scan.*;
import net.superkat.wavify.config.WavifyConfig;

/** Bounded snapshot pipeline. No worker receives a world, chunk, entity or renderer. */
public final class SurfaceCache implements AutoCloseable {
    private final WorldAccess level;
    private final long generation;
    private final Map<Long, SurfaceSnapshot> snapshots = new HashMap<Long, SurfaceSnapshot>();
    public final Map<Long, ShoreScanner.Result> chunks = new HashMap<Long, ShoreScanner.Result>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Wavify shoreline scan");
            t.setDaemon(true);
            return t;
        }
    });
    private Future<List<ShoreScanner.Result>> pending;
    private boolean closed;
    private int scanX, scanZ, column;
    private int[] heights;
    private byte[] flags;
    private long version, tick;
    public SurfaceCache(WorldAccess world, long generation) {
        level = world;
        this.generation = generation;
    }

    public void tick(int px, int pz) {
        if (closed || !WavifyConfig.isActive())
            return;
        tick++;
        int radius = Math.min(WavifyConfig.chunkRadius,
                Math.max(2, Minecraft.getMinecraft().gameSettings.renderDistanceChunks));
        int cx = px >> 4, cz = pz >> 4;
        Iterator<Map.Entry<Long, SurfaceSnapshot>> it = snapshots.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, SurfaceSnapshot> e = it.next();
            SurfaceSnapshot s = e.getValue();
            if (Math.abs(s.chunkX - cx) > radius || Math.abs(s.chunkZ - cz) > radius
                    || !level.loaded(s.chunkX * 16, s.chunkZ * 16)) {
                chunks.remove(e.getKey());
                it.remove();
            }
        }
        if (pending != null && pending.isDone()) {
            try {
                for (ShoreScanner.Result r : pending.get()) {
                    SurfaceSnapshot current = snapshots.get(WaterKey.chunk(r.source.chunkX, r.source.chunkZ));
                    if (current == r.source && r.source.generation == generation
                            && level.loaded(r.source.chunkX * 16, r.source.chunkZ * 16))
                        chunks.put(WaterKey.chunk(r.source.chunkX, r.source.chunkZ), r);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new IllegalStateException("Wavify shore scan failed", e.getCause());
            } finally {
                pending = null;
            }
        }
        if (heights != null && (Math.abs(scanX - cx) > radius || Math.abs(scanZ - cz) > radius
                || !level.loaded(scanX * 16, scanZ * 16))) {
            heights = null;
            flags = null;
        }
        if (heights == null) {
            long best = Long.MAX_VALUE;
            boolean found = false;
            for (int z = cz - radius; z <= cz + radius; z++)
                for (int x = cx - radius; x <= cx + radius; x++) {
                    if (!level.loaded(x * 16, z * 16))
                        continue;
                    SurfaceSnapshot old = snapshots.get(WaterKey.chunk(x, z));
                    if (old != null && tick - old.tick < WavifyConfig.chunkUpdatesRescanAmount)
                        continue;
                    long score = (old == null ? 0 : 100000 + old.tick)
                            + (long) ((x - cx) * (x - cx) + (z - cz) * (z - cz)) * 20;
                    if (score < best) {
                        best = score;
                        scanX = x;
                        scanZ = z;
                        found = true;
                    }
                }
            if (!found)
                return;
            column = 0;
            heights = new int[324];
            flags = new byte[324];
        }
        for (int n = 0; n < WavifyConfig.scanColumnsPerTick && column < 256; n++, column++) {
            int lx = column & 15, lz = column >> 4, x = scanX * 16 + lx, z = scanZ * 16 + lz,
                    i = (lz + 1) * 18 + lx + 1;
            if (!level.loaded(x, z))
                continue;
            int y = level.world.getHeightValue(x, z) - 1;
            BlockPos p = new BlockPos(x, y, z);
            heights[i] = y;
            flags[i] = 8;
            if (level.surface(p))
                flags[i] |= 1;
            if (level.ocean(p))
                flags[i] |= 2;
            if (level.river(p))
                flags[i] |= 4;
        }
        if (column < 256 || pending != null)
            return;
        SurfaceSnapshot s = new SurfaceSnapshot(scanX, scanZ, generation, ++version, tick, heights, flags);
        final long key = WaterKey.chunk(scanX, scanZ);
        snapshots.put(key, s);
        heights = null;
        flags = null;
        final Map<Long, SurfaceSnapshot> copy = new HashMap<Long, SurfaceSnapshot>(snapshots);
        final int distance = WavifyConfig.spawnDistance;
        final boolean oceans = WavifyConfig.enableOceanWaves, rivers = WavifyConfig.enableRiverWaves;
        // Neighbouring chunks gain complete border information as each snapshot arrives.
        pending = worker.submit(new Callable<List<ShoreScanner.Result>>() {
            public List<ShoreScanner.Result> call() {
                List<ShoreScanner.Result> results = new ArrayList<ShoreScanner.Result>();
                int x = (int) key, z = (int) (key >> 32);
                for (int dz = -2; dz <= 2; dz++)
                    for (int dx = -2; dx <= 2; dx++) {
                        if (Thread.currentThread().isInterrupted())
                            return results;
                        SurfaceSnapshot neighbor = copy.get(WaterKey.chunk(x + dx, z + dz));
                        if (neighbor != null)
                            results.add(ShoreScanner.scan(neighbor, copy, distance, oceans, rivers));
                    }
                return results;
            }
        });
    }

    @Override
    public void close() {
        closed = true;
        if (pending != null)
            pending.cancel(true);
        pending = null;
        worker.shutdownNow();
        snapshots.clear();
        chunks.clear();
        heights = null;
        flags = null;
    }
}
