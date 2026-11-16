package net.superkat.wavify.wave;

import net.superkat.wavify.compat.BlockPos;
import net.superkat.wavify.compat.WorldAccess;
import net.superkat.wavify.config.WavifyConfig;

/** Keep upstream surf motion and washing animation, with a separate coastal size control. */
public final class ShoreWave extends Wave {
    private final float calmScale;

    public ShoreWave(WorldAccess world, BlockPos water, float yaw, boolean large, int width) {
        super(world, water.above(), yaw, .65f, large);
        this.calmScale = this.scale * (float) WavifyConfig.shoreWaveScale;
        this.scale = weatherScale();
        this.setWidth(width);
        syncBoxToCurrentPosition();
    }

    public static float rainMultiplier(float strength, float stormScale) {
        return 1f + Math.max(0f, Math.min(1f, strength)) * (stormScale - 1f);
    }

    private float weatherScale() {
        return calmScale
                * rainMultiplier(level.world.getRainStrength(1f), (float) WavifyConfig.rainWaveScale);
    }

    @Override
    public void tick() {
        float target = weatherScale();
        if (Math.abs(target - scale) > .001f) {
            scale += (target - scale) * .05f;
            syncBoxToCurrentPosition();
        }
        super.tick();
    }
}
