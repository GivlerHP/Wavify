package net.superkat.wavify.scan;

import java.util.*;
import net.superkat.wavify.compat.BlockPos;
import net.superkat.wavify.compat.WaterKey;

/** Pure worker-side coastline analysis. Adjacent unloaded columns never count as shore. */
public final class ShoreScanner {
    private ShoreScanner() {
    }
    public static final class Site {
        public final BlockPos pos;
        public final float yaw;
        public final boolean large;
        Site(BlockPos p, float yaw, boolean large) {
            pos = p;
            this.yaw = yaw;
            this.large = large;
        }
    }

    public static final class Result {
        public final SurfaceSnapshot source;
        public final List<Site> sites;
        public final Set<BlockPos> rivers, waters;
        Result(SurfaceSnapshot s, List<Site> sites, Set<BlockPos> rivers, Set<BlockPos> waters) {
            source = s;
            this.sites = sites;
            this.rivers = rivers;
            this.waters = waters;
        }
    }
    public static Result scan(SurfaceSnapshot s, Map<Long, SurfaceSnapshot> chunks, int distance) {
        return scan(s, chunks, distance, true, true);
    }

    public static Result scan(SurfaceSnapshot s, Map<Long, SurfaceSnapshot> chunks, int distance,
            boolean oceans, boolean scanRivers) {
        Map<BlockPos, Boolean> islands = new HashMap<BlockPos, Boolean>();
        List<Site> sites = new ArrayList<Site>();
        Set<BlockPos> rivers = new HashSet<BlockPos>(), waters = new HashSet<BlockPos>();
        for (int z = 1; z <= 16; z++)
            for (int x = 1; x <= 16; x++) {
                if (Thread.currentThread().isInterrupted())
                    return new Result(s, sites, rivers, waters);
                int i = z * 18 + x;
                byte flags = s.flags[i];
                if ((flags & 1) == 0)
                    continue;
                int wx = s.chunkX * 16 + x - 1, wz = s.chunkZ * 16 + z - 1, y = s.heights[i];
                BlockPos p = new BlockPos(wx, y, wz);
                if (scanRivers) {
                    waters.add(p);
                    if ((flags & 4) != 0)
                        rivers.add(p);
                }
                if (!oceans || (flags & 4) != 0)
                    continue;
                // March towards nearby land. Stable per-column sampling keeps adjacent scans seamless.
                double best = Double.MAX_VALUE;
                int bestX = 0, bestZ = 0, land = 0;
                for (int dz = -distance; dz <= distance; dz++)
                    for (int dx = -distance; dx <= distance; dx++) {
                        int sx = wx + dx, sz = wz + dz;
                        SurfaceSnapshot neighbor = chunks.get(WaterKey.chunk(sx >> 4, sz >> 4));
                        if (neighbor == null)
                            continue;
                        int j = ((sz & 15) + 1) * 18 + (sx & 15) + 1;
                        if (neighbor.flags[j] == 0 || (neighbor.flags[j] & 1) != 0 || neighbor.heights[j] < y)
                            continue;
                        double d = dx * dx + dz * dz;
                        land++;
                        if (d < best) {
                            best = d;
                            bestX = dx;
                            bestZ = dz;
                        }
                    }
                if (best >= distance * distance && best < (distance + 1) * (distance + 1)
                        && oceanConnected(chunks, p, bestX, bestZ)
                        && !isolated(chunks, new BlockPos(wx + bestX, y, wz + bestZ), islands)) {
                    float yaw = Math.round((float) Math.toDegrees(Math.atan2(bestZ, bestX)) / 15f) * 15f;
                    sites.add(new Site(p, yaw, land > 20));
                }
            }
        return new Result(s, sites, rivers, waters);
    }
    /**
     * Continuous fronts use the upstream grouping rule: up to three adjacent cells, compatible shore
     * direction, and a shared animation birth time.
     */
    public static final class Front {
        public final BlockPos pos;
        public final float yaw;
        public final boolean large;
        public final int width;
        Front(BlockPos p, float yaw, boolean large, int width) {
            pos = p;
            this.yaw = yaw;
            this.large = large;
            this.width = width;
        }
    }
    public static List<Front> fronts(Collection<Site> sites) {
        Map<BlockPos, Site> remaining = new HashMap<BlockPos, Site>();
        for (Site site : sites)
            remaining.put(site.pos, site);
        List<Site> ordered = new ArrayList<Site>(remaining.values());
        Collections.sort(ordered, new Comparator<Site>() {
            public int compare(Site a, Site b) {
                int x = Integer.compare(a.pos.getX(), b.pos.getX());
                return x != 0 ? x : Integer.compare(a.pos.getZ(), b.pos.getZ());
            }
        });
        List<Front> fronts = new ArrayList<Front>();
        for (Site start : ordered) {
            if (!remaining.containsKey(start.pos))
                continue;
            List<Site> connected = new ArrayList<Site>();
            ArrayDeque<Site> queue = new ArrayDeque<Site>();
            Set<BlockPos> queued = new HashSet<BlockPos>();
            queue.add(start);
            queued.add(start.pos);
            while (!queue.isEmpty() && connected.size() < 3) {
                Site site = queue.remove();
                if (remaining.remove(site.pos) == null)
                    continue;
                connected.add(site);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        Site next = remaining.get(site.pos.offset(dx, 0, dz));
                        if (next != null
                                && Math.abs(net.superkat.wavify.compat.Mth.rotLerp(1, start.yaw, next.yaw)
                                        - start.yaw) <= 15
                                && queued.add(next.pos))
                            queue.add(next);
                    }
            }
            Collections.sort(connected, new Comparator<Site>() {
                public int compare(Site a, Site b) {
                    return Integer.compare(a.pos.getZ(), b.pos.getZ());
                }
            });
            Site center = connected.get(connected.size() / 2);
            fronts.add(new Front(center.pos, start.yaw, start.large,
                    Math.min(3, (int) (connected.size() * 1.5))));
        }
        return fronts;
    }

    private static boolean oceanConnected(Map<Long, SurfaceSnapshot> chunks, BlockPos p, int shoreX,
            int shoreZ) {
        double length = Math.hypot(shoreX, shoreZ);
        if (length == 0)
            return false;
        for (int step = 0; step <= 16; step++) {
            int x = (int) Math.floor(p.getX() + .5 - shoreX / length * step),
                    z = (int) Math.floor(p.getZ() + .5 - shoreZ / length * step);
            SurfaceSnapshot s = chunks.get(WaterKey.chunk(x >> 4, z >> 4));
            if (s == null)
                return false;
            int i = ((z & 15) + 1) * 18 + (x & 15) + 1;
            if ((s.flags[i] & 1) == 0 || (s.flags[i] & 4) != 0 || Math.abs(s.heights[i] - p.getY()) > 1)
                return false;
            if ((s.flags[i] & 2) != 0)
                return true;
        }
        return false;
    }

    // Upstream's isolated-object guard: no surf around a single block or a small island.
    private static boolean isolated(Map<Long, SurfaceSnapshot> chunks, BlockPos seed,
            Map<BlockPos, Boolean> cache) {
        Boolean known = cache.get(seed);
        if (known != null)
            return known;
        Set<BlockPos> visited = new HashSet<BlockPos>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<BlockPos>();
        queue.add(seed);
        visited.add(seed);
        boolean small = true;
        while (!queue.isEmpty()) {
            BlockPos p = queue.remove();
            if (visited.size() > 768) {
                small = false;
                break;
            }
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0)
                        continue;
                    BlockPos next = p.offset(dx, 0, dz);
                    SurfaceSnapshot n = chunks.get(WaterKey.chunk(next.getX() >> 4, next.getZ() >> 4));
                    if (n == null) {
                        small = false;
                        continue;
                    }
                    int i = ((next.getZ() & 15) + 1) * 18 + (next.getX() & 15) + 1;
                    if (n.flags[i] != 0 && (n.flags[i] & 1) == 0 && n.heights[i] >= seed.getY()
                            && visited.add(next))
                        queue.add(next);
                }
        }
        for (BlockPos p : visited)
            cache.put(p, small);
        return small;
    }

}
