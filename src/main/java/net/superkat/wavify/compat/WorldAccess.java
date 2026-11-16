package net.superkat.wavify.compat;

import java.util.List;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.particles.WaveSplash;

/** All access to the live world is confined to the client thread. Never passed to a worker. */
public final class WorldAccess {
    public final WorldClient world;
    public WorldAccess(WorldClient world) {
        this.world = world;
    }

    public boolean loaded(int x, int z) {
        return world.getChunkProvider().chunkExists(x >> 4, z >> 4);
    }

    public boolean loaded(BlockPos p) {
        return loaded(p.getX(), p.getZ());
    }

    public State getBlockState(BlockPos p) {
        if (!loaded(p))
            return State.UNLOADED;
        Block b = world.getBlock(p.getX(), p.getY(), p.getZ());
        return new State(b, b.isAir(world, p.getX(), p.getY(), p.getZ()));
    }

    public boolean isEmptyBlock(BlockPos p) {
        return loaded(p) && world.isAirBlock(p.getX(), p.getY(), p.getZ());
    }

    public boolean water(BlockPos p) {
        if (!loaded(p))
            return false;
        Block b = world.getBlock(p.getX(), p.getY(), p.getZ());
        return b.getMaterial() == Material.water && !b.getMaterial().blocksMovement();
    }

    public boolean surface(BlockPos p) {
        return water(p) && isEmptyBlock(p.above());
    }

    public boolean ocean(BlockPos p) {
        return loaded(p) && BiomeDictionary.isBiomeOfType(world.getBiomeGenForCoords(p.getX(), p.getZ()),
                BiomeDictionary.Type.OCEAN);
    }

    public boolean river(BlockPos p) {
        return loaded(p) && BiomeDictionary.isBiomeOfType(world.getBiomeGenForCoords(p.getX(), p.getZ()),
                BiomeDictionary.Type.RIVER);
    }

    public Random getRandom() {
        return world.rand;
    }

    public long getGameTime() {
        return world.getTotalWorldTime();
    }

    public long getDayTime() {
        return world.getWorldTime();
    }

    public int light(BlockPos p) {
        return loaded(p) ? world.getLightBrightnessForSkyBlocks(p.getX(), p.getY(), p.getZ(), 0) : 0;
    }

    public int waterColor(BlockPos p) {
        if (!loaded(p))
            return WavifyConfig.customColor;
        BiomeGenBase biome = world.getBiomeGenForCoords(p.getX(), p.getZ());
        Integer override = WavifyConfig.biomeColors.get(biome.biomeID);
        if (override != null)
            return override;
        if ("CUSTOM".equalsIgnoreCase(WavifyConfig.colorSource))
            return WavifyConfig.customColor;
        Block b = world.getBlock(p.getX(), p.getY() - 1, p.getZ());
        // Use native water colorMultiplier, so BOP / MCPatcher water colors participate.
        if (b.getMaterial() != Material.water)
            b = net.minecraft.init.Blocks.water;
        return net.superkat.wavify.renderer.WaterPalette
                .color(b.colorMultiplier(world, p.getX(), p.getY() - 1, p.getZ()));
    }

    public Vec3 collide(AxisAlignedBB original, double x, double y, double z) {
        AxisAlignedBB box = original.copy();
        int minX = Mth.floor(box.minX + Math.min(0, x)), maxX = Mth.floor(box.maxX + Math.max(0, x));
        int minZ = Mth.floor(box.minZ + Math.min(0, z)), maxZ = Mth.floor(box.maxZ + Math.max(0, z));
        for (int cx = minX >> 4; cx <= maxX >> 4; cx++)
            for (int cz = minZ >> 4; cz <= maxZ >> 4; cz++)
                if (!world.getChunkProvider().chunkExists(cx, cz))
                    return Vec3.createVectorHelper(0, 0, 0);
        List<AxisAlignedBB> collisions = new java.util.ArrayList<AxisAlignedBB>();
        AxisAlignedBB area = box.addCoord(x, y, z);
        for (int bx = Mth.floor(area.minX); bx <= Mth.floor(area.maxX); bx++)
            for (int bz = Mth.floor(area.minZ); bz <= Mth.floor(area.maxZ); bz++)
                for (int by = Mth.floor(area.minY) - 1; by <= Mth.floor(area.maxY); by++) {
                    Block block = world.getBlock(bx, by, bz);
                    block.addCollisionBoxesToList(world, bx, by, bz, area, collisions, null);
                }
        for (AxisAlignedBB b : collisions)
            y = b.calculateYOffset(box, y);
        box.offset(0, y, 0);
        for (AxisAlignedBB b : collisions)
            x = b.calculateXOffset(box, x);
        box.offset(x, 0, 0);
        for (AxisAlignedBB b : collisions)
            z = b.calculateZOffset(box, z);
        return Vec3.createVectorHelper(x, y, z);
    }

    public void splash(boolean big, double x, double y, double z, double vx, double vy, double vz) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!WavifyConfig.particles || mc.gameSettings.particleSetting == 2)
            return;
        if (mc.gameSettings.particleSetting == 1 && world.rand.nextBoolean())
            return;
        mc.effectRenderer.addEffect(new WaveSplash(world, x, y, z, vx, vy, vz, big));
    }

    public void spray(float yaw, float intensity, float scale, double x, double y, double z, double vx,
            double vz) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!WavifyConfig.particles || mc.gameSettings.particleSetting == 2)
            return;
        if (mc.gameSettings.particleSetting == 1 && world.rand.nextBoolean())
            return;
        mc.effectRenderer.addEffect(new net.superkat.wavify.particles.WaveSpray(this, x, y - .05, z, vx, vz,
                yaw - 180, intensity, scale, false));
        mc.effectRenderer.addEffect(new net.superkat.wavify.particles.WaveSpray(this, x, y - .04, z, vx, vz,
                yaw - 180, intensity, scale, true));
    }
    public static final class State {
        public static final State WATER = new State(net.minecraft.init.Blocks.water, false),
                UNLOADED = new State(net.minecraft.init.Blocks.bedrock, false);
        public final Block block;
        private final boolean air;
        State(Block block, boolean air) {
            this.block = block;
            this.air = air;
        }

        public boolean isAir() {
            return air;
        }
    }
}
