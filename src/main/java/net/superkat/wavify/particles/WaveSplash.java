package net.superkat.wavify.particles;

import net.minecraft.client.particle.EntityFX;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.superkat.wavify.compat.WorldAccess;
import net.minecraft.world.World;

/** Uses the original animated splash sprites in the vanilla particle atlas. */
public final class WaveSplash extends EntityFX {
    public static final net.minecraft.util.IIcon[][] SPRITES = {new net.minecraft.util.IIcon[4],
            new net.minecraft.util.IIcon[11]};
    private final int type;
    private final int splashFrame;
    private final WorldAccess level;
    private boolean stopped;
    public WaveSplash(World w, double x, double y, double z, double vx, double vy, double vz, boolean big) {
        super(w, x, y, z, 0, 0, 0);
        level = new WorldAccess((net.minecraft.client.multiplayer.WorldClient) w);
        type = big ? 1 : 0;
        motionX = vx;
        motionY = vy;
        motionZ = vz;
        if (big) {
            motionX += (rand.nextDouble() * 2 - 1) * .4;
            motionY += (rand.nextDouble() * 2 - 1) * .4;
            motionZ += (rand.nextDouble() * 2 - 1) * .4;
            double speed = (rand.nextDouble() + rand.nextDouble() + 1) * .15 * .4;
            double length = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
            if (length > 0) {
                motionX = motionX / length * speed;
                motionY = motionY / length * speed + .1;
                motionZ = motionZ / length * speed;
            }
        }
        particleMaxAge = big ? 7 + rand.nextInt(24) : (int) (8 / (rand.nextDouble() * .8 + .2));
        splashFrame = rand.nextInt(4);
        particleScale = big ? 5.0f : 1.0f;
        particleGravity = 1f;
        int color = new net.superkat.wavify.compat.WorldAccess(
                (net.minecraft.client.multiplayer.WorldClient) w)
                        .waterColor(net.superkat.wavify.compat.BlockPos.containing(x, y, z));
        particleRed = (color >> 16 & 255) / 255f;
        particleGreen = (color >> 8 & 255) / 255f;
        particleBlue = (color & 255) / 255f;
        noClip = false;
        setSize(big ? .2f : .01f, big ? .2f : .01f);
        updateSprite();
    }

    private void updateSprite() {
        int f = type == 0
                ? splashFrame
                : Math.min(SPRITES[type].length - 1,
                        particleAge * (SPRITES[type].length - 1) / particleMaxAge);
        if (SPRITES[type][f] != null)
            setParticleIcon(SPRITES[type][f]);
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void onUpdate() {
        if (!net.superkat.wavify.config.WavifyConfig.isActive()
                || !net.superkat.wavify.config.WavifyConfig.enableOceanWaves) {
            setDead();
            return;
        }
        if (!worldObj.getChunkProvider().chunkExists(((int) Math.floor(posX)) >> 4,
                ((int) Math.floor(posZ)) >> 4)) {
            setDead();
            return;
        }
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (particleAge++ >= particleMaxAge) {
            setDead();
            return;
        }
        motionY -= type == 0 ? .04 : .04 * .04;
        moveParticle();
        motionX *= .98;
        motionY *= .98;
        motionZ *= .98;
        if (onGround) {
            if (type == 0 && rand.nextDouble() < .5)
                setDead();
            motionX *= .7;
            motionZ *= .7;
        }
        if (type == 0) {
            int x = (int) Math.floor(posX), y = (int) Math.floor(posY), z = (int) Math.floor(posZ);
            Block block = worldObj.getBlock(x, y, z);
            AxisAlignedBB shape = block.getCollisionBoundingBoxFromPool(worldObj, x, y, z);
            double top = shape != null && posX >= shape.minX && posX <= shape.maxX && posZ >= shape.minZ
                    && posZ <= shape.maxZ ? shape.maxY : y;
            if (block.getMaterial().isLiquid())
                top = Math.max(top,
                        y + 1 - BlockLiquid.getLiquidHeightPercent(worldObj.getBlockMetadata(x, y, z)));
            if (posY < top)
                setDead();
        } else {
            // BigSplashParticle applies a second gravity step after the base particle tick.
            motionY -= .04;
        }
        updateSprite();
    }

    private void moveParticle() {
        if (stopped)
            return;
        Vec3 move = level.collide(boundingBox, motionX, motionY, motionZ);
        boundingBox.offset(move.xCoord, move.yCoord, move.zCoord);
        posX = (boundingBox.minX + boundingBox.maxX) * .5;
        posY = boundingBox.minY;
        posZ = (boundingBox.minZ + boundingBox.maxZ) * .5;
        stopped = Math.abs(motionY) >= 1.0e-5 && Math.abs(move.yCoord) < 1.0e-5;
        onGround = motionY != move.yCoord && motionY < 0;
        if (motionX != move.xCoord)
            motionX = 0;
        if (motionZ != move.zCoord)
            motionZ = 0;
    }

}
