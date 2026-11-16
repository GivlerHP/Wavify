// Wave mesh/animation adapted from Wavify by Superkat32 (MIT); backport by Givler.
package net.superkat.wavify.renderer;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;
import net.superkat.wavify.client.WaveScene;
import net.superkat.wavify.compat.*;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.wave.Wave;

public final class WaveRenderer {
    private final RenderState saved = new RenderState();
    private double cosYaw, sinYaw, cosPitch, sinPitch, baseX, baseY, baseZ;
    private final WaveAtlas atlas;
    private final List<Wave> visible = new ArrayList<Wave>();
    public WaveRenderer(WaveAtlas atlas) {
        this.atlas = atlas;
    }

    public void clear() {
        visible.clear();
    }

    public void render(WaveScene scene, float delta) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity camera = mc.renderViewEntity;
        if (!atlas.ready || camera == null)
            return;
        double cx = camera.lastTickPosX + (camera.posX - camera.lastTickPosX) * delta,
                cy = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * delta,
                cz = camera.lastTickPosZ + (camera.posZ - camera.lastTickPosZ) * delta;
        Frustrum frustum = new Frustrum();
        frustum.setPosition(cx, cy, cz);
        visible.clear();
        for (Wave w : scene.waves) {
            double reach = w.scale * (w.width + w.length + 2);
            if (frustum.isBoundingBoxInFrustum(w.getBoundingBox().expand(reach, 2, reach)))
                visible.add(w);
        }
        if (visible.isEmpty() && scene.wet.isEmpty())
            return;
        // Back-to-front transparent wave order; depth testing still hides terrain-covered fragments.
        Collections.sort(visible, new Comparator<Wave>() {
            public int compare(Wave a, Wave b) {
                double ad = (a.x - cx) * (a.x - cx) + (a.z - cz) * (a.z - cz),
                        bd = (b.x - cx) * (b.x - cx) + (b.z - cz) * (b.z - cz);
                return Double.compare(bd, ad);
            }
        });
        try (RenderState state = saved.capture()) {
            mc.entityRenderer.enableLightmap(delta);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            mc.getTextureManager().bindTexture(WaveAtlas.LOCATION);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_BLEND);
            OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE,
                    GL11.GL_ZERO);
            Tessellator t = Tessellator.instance;
            t.startDrawingQuads();
            try {
                float body = (float) (WavifyConfig.waveYOffset
                        + (ShaderState.active() ? WavifyConfig.shaderWaveYSink : 0));
                for (Wave w : visible) {
                    double pitch = Math.toRadians(w.pitch), yaw = Math.toRadians(-w.getYaw(delta) + 90);
                    cosYaw = Math.cos(yaw);
                    sinYaw = Math.sin(yaw);
                    cosPitch = Math.cos(pitch);
                    sinPitch = Math.sin(pitch);
                    baseX = w.getX(delta) - cx;
                    baseY = w.getY(delta) - cy;
                    baseZ = w.getZ(delta) - cz;
                    int light = w.getLight();
                    int age = w.getAge(), max = w.getMaxAge(), index = w.isWashingUp() ? 2 : 0;
                    float sink = w.getRenderYSink();
                    for (int i = 0, n = w.getRenderColumnCount(); i < n; i++) {
                        float x = w.getRenderColumnLateralOffset(i, n),
                                y = w.getRenderColumnVerticalOffset(i, n),
                                z = w.getRenderColumnForwardOffset(i, n),
                                width = w.getRenderColumnWidth(i, n), length = w.getRenderColumnLength(i, n);
                        quad(t, w, delta, cx, cy, cz, atlas.sheet(index), age, max, x, y + body + sink, z,
                                width, length, w.red, w.green, w.blue,
                                w.alpha * (float) WavifyConfig.transparency, light);
                        quad(t, w, delta, cx, cy, cz, atlas.sheet(index + 1), age, max, x,
                                y + .08f + (float) WavifyConfig.waveYOffset + sink, z, width, length, 1, 1, 1,
                                w.alpha * (float) (WavifyConfig.applyTransparencyToFoam
                                        ? WavifyConfig.transparency
                                        : 1),
                                light);
                    }
                    if (w.isWashingUp() && w.bigWave) {
                        float progress = (float) age / Math.max(1, max),
                                length = progress > .5 ? Mth.lerp((progress - .5f) * 2, 2, 3) : 2,
                                z = progress > .5 ? Mth.lerp((progress - .5f) * 2, 1.35f, 0) : 1.35f;
                        for (int i = 0; i < w.width; i++) {
                            quad(t, w, delta, cx, cy, cz, atlas.sheet(4), age, max, (i - .15f) * 1.25f,
                                    -.05f + body + sink, z, 1.25f, length, w.red, w.green, w.blue,
                                    w.alpha * (float) WavifyConfig.transparency, light);
                            quad(t, w, delta, cx, cy, cz, atlas.sheet(5), age, max, (i - .15f) * 1.25f,
                                    -.01f + (float) WavifyConfig.waveYOffset + sink, z, 1.25f, length, 1, 1,
                                    1,
                                    w.alpha * (float) (WavifyConfig.applyTransparencyToFoam
                                            ? WavifyConfig.transparency
                                            : 1),
                                    light);
                        }
                    }
                }
                for (BlockPos p : scene.wet.keySet()) {
                    if (!scene.level.loaded(p)
                            || !scene.level.world.getBlock(p.getX(), p.getY(), p.getZ()).isNormalCube())
                        continue;
                    WaveAtlas.Sheet s = atlas.sheet(6);
                    t.setBrightness(scene.level.light(p.above()));
                    t.setNormal(0, 1, 0);
                    t.setColorRGBA_F(.1f, .1f, .25f, .25f);
                    double x = p.getX() - cx, y = p.getY() + 1.01 - cy, z = p.getZ() - cz;
                    t.addVertexWithUV(x, y, z, s.u0, s.v0);
                    t.addVertexWithUV(x, y, z + 1, s.u0, s.v0 + s.span);
                    t.addVertexWithUV(x + 1, y, z + 1, s.u1, s.v0 + s.span);
                    t.addVertexWithUV(x + 1, y, z, s.u1, s.v0);
                }
            } finally {
                t.draw();
            }
        }
    }

    private void quad(Tessellator t, Wave w, float delta, double cx, double cy, double cz, WaveAtlas.Sheet s,
            int age, int max, float x, float y, float z, float width, float length, float r, float g, float b,
            float a, int light) {
        if (a <= 0)
            return;
        t.setBrightness(light);
        t.setColorRGBA_F(r, g, b, Math.min(1, a));
        t.setNormal(0, 1, 0);
        float v = s.frameV(age, max), span = s.span / s.frames;
        vertex(t, w, delta, cx, cy, cz, x - width / 2, y, z - length / 2, s.u0, v + span);
        vertex(t, w, delta, cx, cy, cz, x - width / 2, y, z + length / 2, s.u0, v);
        vertex(t, w, delta, cx, cy, cz, x + width / 2, y, z + length / 2, s.u1, v);
        vertex(t, w, delta, cx, cy, cz, x + width / 2, y, z - length / 2, s.u1, v + span);
    }

    private void vertex(Tessellator t, Wave w, float delta, double cx, double cy, double cz, float x, float y,
            float z, float u, float v) {
        double py = cosPitch * y - sinPitch * z * w.scale, pz = sinPitch * y + cosPitch * z * w.scale,
                px = x * w.scale;
        t.addVertexWithUV(baseX + cosYaw * px + sinYaw * pz, baseY + py, baseZ - sinYaw * px + cosYaw * pz, u,
                v);
    }
}
