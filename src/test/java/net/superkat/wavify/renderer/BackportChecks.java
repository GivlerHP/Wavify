package net.superkat.wavify.renderer;

import java.util.*;
import net.superkat.wavify.compat.*;
import net.superkat.wavify.scan.*;
import net.superkat.wavify.river.*;
import net.superkat.wavify.wave.ShoreWaveSchedule;
import net.superkat.wavify.wave.ShoreWave;

public final class BackportChecks {
    private static int checks;
    private static void check(boolean ok, String label) {
        checks++;
        if (!ok)
            throw new AssertionError(label);
    }

    public static void main(String[] args) {
        check(WaterKey.chunk(-1, 0) != WaterKey.chunk(0, -1), "negative chunk keys");
        check(new BlockPos(-2, -64, 8).equals(new BlockPos(-2, -64, 8)), "negative-height positions");
        check(BlockPos.containing(-.1, -63.1, 15.99).equals(new BlockPos(-1, -64, 15)),
                "floor vs truncation");
        Map<Long, SurfaceSnapshot> chunks = new HashMap<Long, SurfaceSnapshot>();
        for (int cz = -1; cz <= 1; cz++)
            for (int cx = -1; cx <= 1; cx++) {
                int[] heights = new int[324];
                byte[] flags = new byte[324];
                for (int z = 1; z <= 16; z++)
                    for (int x = 1; x <= 16; x++) {
                        int wx = cx * 16 + x - 1, i = z * 18 + x;
                        heights[i] = -32;
                        flags[i] = (byte) (wx < 0 ? 8 : 11);
                    }
                chunks.put(WaterKey.chunk(cx, cz), new SurfaceSnapshot(cx, cz, 1, 1, 1, heights, flags));
                // Snapshot must own its data.
                Arrays.fill(heights, 123);
                Arrays.fill(flags, (byte) 0);
            }
        java.awt.image.BufferedImage waterTexture = new java.awt.image.BufferedImage(2, 1,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        waterTexture.setRGB(0, 0, 0xFF4C585E);
        waterTexture.setRGB(1, 0, 0x00FFFFFF);
        check(WaterPalette.average(waterTexture) == 0x4C585E,
                "transparent texture pixels cannot whiten wave body");
        check(WaterPalette.multiply(0x4C585E, 0xFFFFFF) == 0x4C585E,
                "white legacy biome tint preserves water texture color");
        check(WaterPalette.multiply(0x4080C0, 0x80FF80) == 0x208060, "texture and biome color multiply");
        SurfaceSnapshot center = chunks.get(WaterKey.chunk(0, 0));
        ShoreScanner.Result result = ShoreScanner.scan(center, chunks, 8);
        ShoreScanner.Result seas = ShoreScanner.scan(center, chunks, 8, true, false),
                riversOnly = ShoreScanner.scan(center, chunks, 8, false, true);
        check(!seas.sites.isEmpty() && seas.waters.isEmpty() && seas.rivers.isEmpty(),
                "seas-only mode skips river candidate collection");
        check(riversOnly.sites.isEmpty() && riversOnly.waters.size() == 256,
                "rivers-only mode skips coastal analysis");
        check(result.waters.size() == 256, "all water columns, including negative Y");
        check(!result.sites.isEmpty(), "shore across chunk boundary");
        for (ShoreScanner.Site s : result.sites) {
            check(s.pos.getY() == -32, "surface Y preserved");
            check(s.pos.getX() >= 6 && s.pos.getX() <= 8, "spawn distance");
            check(Math.abs(Math.abs(s.yaw) - 180) < 1, "waves face shore");
        }
        List<ShoreScanner.Site> contour = new ArrayList<ShoreScanner.Site>();
        for (SurfaceSnapshot snap : chunks.values())
            contour.addAll(ShoreScanner.scan(snap, chunks, 8).sites);
        List<ShoreScanner.Front> fronts = ShoreScanner.fronts(contour);
        check(fronts.size() < contour.size() && fronts.size() > 0,
                "adjacent coast cells form grouped fronts");
        check(fronts.size() >= (contour.size() + 2) / 3, "front group has at most three cells");
        for (ShoreScanner.Front front : fronts) {
            check(front.width >= 1 && front.width <= 3, "original front width bounds");
            check(Math.abs(Math.abs(front.yaw) - 180) < 1, "coherent front direction");
        }
        shoreSets(fronts);
        Map<Long, SurfaceSnapshot> beachChunks = new HashMap<Long, SurfaceSnapshot>();
        for (SurfaceSnapshot snap : chunks.values()) {
            byte[] flags = snap.flags.clone();
            for (int z = 1; z <= 16; z++)
                for (int x = 1; x <= 16; x++) {
                    int wx = snap.chunkX * 16 + x - 1;
                    if (wx >= 0 && wx < 12)
                        flags[z * 18 + x] = (byte) (flags[z * 18 + x] & ~2);
                }
            beachChunks.put(WaterKey.chunk(snap.chunkX, snap.chunkZ),
                    new SurfaceSnapshot(snap.chunkX, snap.chunkZ, 1, 1, 1, snap.heights, flags));
        }
        check(!ShoreScanner.scan(beachChunks.get(WaterKey.chunk(0, 0)), beachChunks, 8).sites.isEmpty(),
                "ocean-connected beach water retains surf");
        Map<Long, SurfaceSnapshot> isolated = new HashMap<Long, SurfaceSnapshot>();
        isolated.put(WaterKey.chunk(0, 0), center);
        check(ShoreScanner.scan(center, isolated, 8).sites.isEmpty(), "unloaded neighbour is not land");
        int[] h = new int[324];
        byte[] f = new byte[324];
        Arrays.fill(h, 64);
        Arrays.fill(f, (byte) 15);
        SurfaceSnapshot river = new SurfaceSnapshot(0, 0, 1, 1, 1, h, f);
        isolated.put(WaterKey.chunk(0, 0), river);
        result = ShoreScanner.scan(river, isolated, 8);
        check(result.sites.isEmpty() && result.rivers.size() == 256,
                "river water excluded from ocean spawns");
        RiverFlowField field = new RiverFlowField();
        Map<Long, Set<BlockPos>> waters = new HashMap<Long, Set<BlockPos>>();
        for (int z = -48; z < 48; z++)
            for (int x = -4; x <= 4; x++) {
                long key = WaterKey.chunk(x >> 4, z >> 4);
                if (!waters.containsKey(key))
                    waters.put(key, new HashSet<BlockPos>());
                waters.get(key).add(new BlockPos(x, 63, z));
            }
        field.ensureBuilt(waters, 0, 0, 1);
        RiverFlow.Flow flow = field.flowAt(0, 0);
        check(flow != null, "river shape produces a direction");
        check(Math.abs(flow.dirZ()) > .7, "river follows long axis");
        check(Math.abs(flow.dirX() * flow.dirX() + flow.dirZ() * flow.dirZ() - 1) < .001,
                "normalized river vector");
        field.ensureBuilt(Collections.<Long, Set<BlockPos>>emptyMap(), 200, 200, 500);
        check(field.flowAt(200, 200) == null, "empty river rebuild");
        WaveAtlas.Sheet animated = new WaveAtlas.Sheet(0, 1, 0, 1, 16, 5);
        check(animated.frameV(5, 100) == 1f / 16, "frame time");
        check(animated.frameV(80, 100) == 0, "animation wrap");
        WaveAtlas.Sheet washing = new WaveAtlas.Sheet(0, 1, 0, 1, 18, 0);
        check(washing.frameV(60, 60) == 17f / 18, "washing last frame clamped");
        check(washing.frameV(0, 0) == 0, "zero lifetime safe");
        int[] islandHeights = new int[324];
        byte[] islandFlags = new byte[324];
        Arrays.fill(islandHeights, 63);
        Arrays.fill(islandFlags, (byte) 11);
        islandFlags[9 * 18 + 9] = 8;
        SurfaceSnapshot island = new SurfaceSnapshot(0, 0, 1, 1, 1, islandHeights, islandFlags);
        Map<Long, SurfaceSnapshot> islandMap = new HashMap<Long, SurfaceSnapshot>();
        islandMap.put(WaterKey.chunk(0, 0), island);
        check(ShoreScanner.scan(island, islandMap, 4).sites.isEmpty(),
                "small isolated object does not create surf");
        try {
            for (String name : WaveAtlas.NAMES) {
                java.io.InputStream stream = BackportChecks.class
                        .getResourceAsStream("/assets/wavify/textures/wave/" + name + ".png");
                check(stream != null, "wave sheet " + name);
                try {
                    check(javax.imageio.ImageIO.read(stream) != null, "wave PNG " + name);
                } finally {
                    stream.close();
                }
            }
            for (String kind : new String[]{"splash", "bigsplash", "wave", "wavewhite"}) {
                int count = kind.equals("splash") ? 4 : kind.equals("bigsplash") ? 11 : 20;
                String dir = kind.startsWith("wave") ? "spray" : "splash";
                for (int i = 1; i <= count; i++)
                    check(BackportChecks.class.getResource(
                            "/assets/wavify/textures/blocks/" + dir + "/" + kind + i + ".png") != null,
                            "particle texture " + kind + i);
            }
            for (String name : new String[]{"ocean_wave_1", "ocean_wave_2", "river_wave"})
                check(BackportChecks.class.getResource("/assets/wavify/sounds/" + name + ".ogg") != null,
                        "ambient audio " + name);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        try (java.io.Reader reader = new java.io.InputStreamReader(
                BackportChecks.class.getResourceAsStream("/assets/wavify/sounds.json"), "UTF-8")) {
            com.google.gson.JsonObject sounds = new com.google.gson.JsonParser().parse(reader)
                    .getAsJsonObject();
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder()
                    .registerTypeAdapter(net.minecraft.client.audio.SoundList.class,
                            new net.minecraft.client.audio.SoundListSerializer())
                    .create();
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : sounds.entrySet())
                check(gson.fromJson(entry.getValue(), net.minecraft.client.audio.SoundList.class) != null,
                        "vanilla sound decoder: " + entry.getKey());
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        try (java.io.InputStream stream = RenderState.class.getResourceAsStream("RenderState.class")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] block = new byte[2048];
            int read;
            while ((read = stream.read(block)) != -1)
                bytes.write(block, 0, read);
            check(!new String(bytes.toByteArray(), "ISO-8859-1").contains("glMultiTexCoord4"),
                    "no unsupported core-context MultiTexCoord4 calls");
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        check(!ShaderState.active(), "optional shader classes absent");
        WaveLifecycleChecks.run();
        System.out.println("Wavify backport checks passed: " + checks);
    }

    private static void shoreSets(List<ShoreScanner.Front> fronts) {
        Random random = new Random(90210);
        int[] counts = new int[4];
        for (int i = 0; i < 10000; i++)
            counts[ShoreWaveSchedule.chooseCount(random, 3)]++;
        check(counts[1] > 6500 && counts[1] < 7500 && counts[2] > 2000 && counts[3] > 250,
                "single waves dominate; pairs and triples remain occasional");
        for (int i = 0; i < 100; i++)
            check(ShoreWaveSchedule.chooseCount(random, 1) == 1
                    && ShoreWaveSchedule.chooseCount(random, 2) <= 2,
                    "configured maximum bounds random sets");
        check(ShoreWave.rainMultiplier(0, 1.35f) == 1f, "dry weather preserves calm surf");
        check(Math.abs(ShoreWave.rainMultiplier(1, 1.35f) - 1.35f) < .0001f, "full rain grows coastal waves");
        check(ShoreWave.rainMultiplier(.5f, 1.35f) > 1f && ShoreWave.rainMultiplier(.5f, 1.35f) < 1.35f,
                "rain transition changes size gradually");
        check(ShoreWave.rainMultiplier(-1, 1.35f) == 1f && ShoreWave.rainMultiplier(2, 1.35f) == 1.35f,
                "weather strength is clamped");
        ShoreWaveSchedule schedule = new ShoreWaveSchedule();
        List<ShoreScanner.Front> coast = fronts.subList(0, 2);
        schedule.addSets(coast, 100, 3, 28, 7);
        check(schedule.size() == 6, "reserve complete three-crest sets within total wave cap");
        schedule.addSets(coast, 100, 3, 28, 7);
        check(schedule.size() == 6, "existing reservations prevent river/shore oversubscription");
        check(schedule.poll(99) == null, "no early surf crest");
        Map<ShoreScanner.Front, List<Long>> births = new HashMap<ShoreScanner.Front, List<Long>>();
        for (long tick : new long[]{100, 128, 156}) {
            for (int i = 0; i < 2; i++) {
                ShoreWaveSchedule.Crest crest = schedule.poll(tick);
                check(crest != null && crest.birthTick == tick, "coherent fronts in each successive crest");
                List<Long> times = births.get(crest.front);
                if (times == null) {
                    times = new ArrayList<Long>();
                    births.put(crest.front, times);
                }
                times.add(crest.birthTick);
            }
            check(schedule.poll(tick + 27) == null, "surf gap separates consecutive crests");
        }
        check(schedule.size() == 0 && births.size() == 2,
                "all selected coastline fronts receive their full set");
        for (List<Long> times : births.values())
            check(times.equals(Arrays.asList(100L, 128L, 156L)),
                    "same coastline receives three spaced waves");
        schedule.addSets(coast, 200, 3, 28, 2);
        check(schedule.size() == 0, "insufficient capacity cannot create an isolated first wave");
        schedule.addSets(coast, 200, 3, 28, 6);
        schedule.poll(200);
        schedule.clear();
        check(schedule.size() == 0 && schedule.poll(Long.MAX_VALUE) == null,
                "world reset/disable cancels delayed crests");
        schedule.addSets(coast, 300, 1, 28, 2);
        check(schedule.size() == 2, "single-crest configuration remains supported");
    }
}
