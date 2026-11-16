package net.superkat.wavify.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.ResourceLocation;
import net.superkat.wavify.client.WaveScene;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.wave.*;

/** At most two locally controlled streams. Never replaces another mod's sound handler. */
public final class WaveAmbience {
    private Loop ocean, river;
    public void tick(WaveScene scene) {
        if (scene == null || !WavifyConfig.enableWaveSounds) {
            stop();
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        double od = 48 * 48, rd = 48 * 48;
        for (Wave w : scene.waves) {
            double d = mc.thePlayer.getDistanceSq(w.x, w.y, w.z);
            if (w instanceof RiverWave)
                rd = Math.min(rd, d);
            else
                od = Math.min(od, d);
        }
        ocean = update(ocean, od, .6f,
                scene.level.getRandom().nextBoolean() ? "ocean_wave_1" : "ocean_wave_2");
        river = update(river, rd, .5f, "river_wave");
    }

    private Loop update(Loop loop, double distance, float max, String name) {
        float target = (float) (Math.max(0, 1 - Math.sqrt(distance) / 48) * max
                * WavifyConfig.waveSoundVolume);
        if (loop == null && target > .005f) {
            loop = new Loop(name);
            Minecraft.getMinecraft().getSoundHandler().playSound(loop);
        }
        if (loop != null) {
            loop.target = target;
            if (loop.silentTicks > 60) {
                Minecraft.getMinecraft().getSoundHandler().stopSound(loop);
                return null;
            }
        }
        return loop;
    }

    public void stop() {
        if (ocean == null && river == null)
            return;
        Minecraft mc = Minecraft.getMinecraft();
        if (ocean != null)
            mc.getSoundHandler().stopSound(ocean);
        if (river != null)
            mc.getSoundHandler().stopSound(river);
        ocean = null;
        river = null;
    }
    private static final class Loop extends MovingSound {
        float target;
        int silentTicks;
        Loop(String name) {
            super(new ResourceLocation("wavify", name));
            repeat = true;
            field_147665_h = 0;
            volume = 0;
            field_147666_i = ISound.AttenuationType.NONE;
        }

        @Override
        public void update() {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.thePlayer == null) {
                donePlaying = true;
                return;
            }
            xPosF = (float) mc.thePlayer.posX;
            yPosF = (float) mc.thePlayer.posY;
            zPosF = (float) mc.thePlayer.posZ;
            volume += (target - volume) * .08f;
            if (target <= .001f)
                silentTicks++;
            else
                silentTicks = 0;
        }
    }
}
