package net.superkat.wavify.config;

import java.io.File;
import net.minecraftforge.common.config.Configuration;

public final class WavifyConfig {
    public static boolean enabled = true, enableOceanWaves = true, enableRiverWaves = true,
            enableWetOverlay = true, enableWaveSounds = true, applyTransparencyToFoam = false,
            particles = true;
    public static int chunkRadius = 5, spawnDistance = 8, maxWaves = 96, scanColumnsPerTick = 64,
            chunkUpdatesRescanAmount = 50, riverWaveSpawnRadius = 56, riverWaveTravelBlocks = 11,
            customColor = 0x3F76E4;
    public static double transparency = 1, shaderWaveYSink = -.30, waveYOffset = 0, riverWaveDensity = 1.5,
            waveSoundVolume = 1, riverWaveScale = .55, shoreWaveScale = 1.2, rainWaveScale = 1.35;
    public static int riverWaveSpawnInterval = 80, shoreWaveCount = 3, shoreWaveGap = 52;
    public static String colorSource = "BIOME";
    public static final java.util.Map<Integer, Integer> biomeColors = new java.util.HashMap<Integer, Integer>();
    private static File configFile;
    public enum WaveMode {
        SEAS, RIVERS, BOTH
    }
    public static boolean isActive() {
        return enabled && (enableOceanWaves || enableRiverWaves);
    }

    public static WaveMode mode() {
        return enableOceanWaves && !enableRiverWaves
                ? WaveMode.SEAS
                : !enableOceanWaves && enableRiverWaves ? WaveMode.RIVERS : WaveMode.BOTH;
    }

    public static void apply(boolean active, WaveMode mode) {
        enabled = active;
        enableOceanWaves = mode != WaveMode.RIVERS;
        enableRiverWaves = mode != WaveMode.SEAS;
        saveSwitches();
    }

    private static void saveSwitches() {
        if (configFile == null)
            return;
        Configuration c = new Configuration(configFile);
        c.load();
        c.get("general", "enabled", true).set(enabled);
        c.get("waves", "oceanWaves", true).set(enableOceanWaves);
        c.get("waves", "riverWaves", true).set(enableRiverWaves);
        c.save();
    }

    private WavifyConfig() {
    }

    public static void load(File file) {
        configFile = file;
        Configuration c = new Configuration(file);
        try {
            c.load();
            enabled = c.getBoolean("enabled", "general", true,
                    "Client visual effects; does not change world blocks.");
            enableOceanWaves = c.getBoolean("oceanWaves", "waves", true,
                    "Shore waves in ocean biomes, including modded oceans.");
            enableRiverWaves = c.getBoolean("riverWaves", "waves", true,
                    "Curving river waves following flowing water and river shape.");
            enableWetOverlay = c.getBoolean("wetOverlay", "appearance", true,
                    "Temporary dark overlay where a wave washes onto a solid shore.");
            particles = c.getBoolean("particles", "appearance", true,
                    "Spray at cliffs and waterfalls. Respects vanilla particle setting.");
            enableWaveSounds = c.getBoolean("waveSounds", "sounds", true,
                    "Additional shore and river ambience; can coexist with Dynamic Surroundings.");
            applyTransparencyToFoam = c.getBoolean("applyTransparencyToFoam", "appearance", false,
                    "Apply transparency to white foam too.");
            chunkRadius = c.getInt("chunkRadius", "performance", 5, 3, 12,
                    "Loaded chunk scan radius, also limited by render distance.");
            scanColumnsPerTick = c.getInt("scanColumnsPerTick", "performance", 64, 8, 256,
                    "Main-thread snapshot budget. Worker only processes snapshots.");
            maxWaves = c.getInt("maxWaves", "performance", 96, 8, 256,
                    "Total active ocean and river wave limit.");
            chunkUpdatesRescanAmount = c.getInt("rescanTicks", "performance", 50, 20, 1200,
                    "Minimum chunk rescan age; scanning is spread across ticks.");
            spawnDistance = c.getInt("spawnDistance", "waves", 8, 4, 24,
                    "Distance in blocks from shore where ocean waves appear.");
            riverWaveSpawnRadius = c.getInt("riverWaveSpawnRadius", "waves", 56, 16, 128,
                    "River wave radius.");
            riverWaveTravelBlocks = c.getInt("riverWaveTravelBlocks", "waves", 11, 4, 32,
                    "River wave travel distance.");
            transparency = c.getFloat("transparency", "appearance", 1, 0, 1, "Wave body opacity.");
            riverWaveDensity = c.getFloat("riverWaveDensity", "waves", 1.5f, 0, 20,
                    "River waves per 100 surface water blocks; replenished gradually.");
            riverWaveScale = c.getFloat("riverWaveScale", "waves", .55f, .25f, 1.5f,
                    "River ripple size multiplier, independent of coastal surf.");
            riverWaveSpawnInterval = c.getInt("riverWaveSpawnInterval", "waves", 80, 20, 400,
                    "Ticks between river spawn attempts; at most two new ripples per attempt.");
            shoreWaveScale = c.getFloat("shoreWaveScale", "waves", 1.2f, .5f, 2f,
                    "Coastal wave size multiplier.");
            shoreWaveCount = c.getInt("shoreWaveCount", "waves", 3, 1, 5,
                    "Maximum crests per coastal set: usually one, sometimes two, rarely more. All count towards maxWaves.");
            shoreWaveGap = c.getInt("shoreWaveGap", "waves", 52, 12, 120,
                    "Minimum ticks between coastal crests; each set adds 0-20 ticks of variation.");
            rainWaveScale = c.getFloat("rainWaveScale", "waves", 1.35f, 1f, 2f,
                    "Coastal size multiplier at full rain strength; fades smoothly with the weather.");
            waveSoundVolume = c.getFloat("volume", "sounds", 1, 0, 1, "Ambient wave volume.");
            waveYOffset = c.getFloat("waveYOffset", "appearance", 0, -.5f, .5f, "Wave height adjustment.");
            shaderWaveYSink = c.getFloat("shaderWaveYSink", "appearance", -.30f, -.5f, .5f,
                    "Additional body offset only while a shader pack is actively enabled.");
            colorSource = c.getString("colorSource", "appearance", "BIOME",
                    "BIOME combines water texture RGB with biome tint (1.7.10); CUSTOM uses customColor.");
            customColor = parseColor(c.getString("customColor", "appearance", "#3F76E4", "Custom water RGB."),
                    0x3F76E4);
            biomeColors.clear();
            for (String entry : c.getStringList("biomeColorOverrides", "appearance", new String[0],
                    "Numeric biome id=#RRGGBB, one entry per line.")) {
                String[] pair = entry.split("=", 2);
                try {
                    if (pair.length == 2)
                        biomeColors.put(Integer.parseInt(pair[0].trim()), parseColor(pair[1], customColor));
                } catch (NumberFormatException ignored) {
                }
            }
        } finally {
            if (c.hasChanged())
                c.save();
        }
    }

    private static int parseColor(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim().replace("#", "").replace("0x", ""), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
