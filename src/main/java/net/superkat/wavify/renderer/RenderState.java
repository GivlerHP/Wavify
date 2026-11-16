package net.superkat.wavify.renderer;

import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import net.minecraft.client.renderer.OpenGlHelper;

/**
 * Explicit restoration keeps Angelica's GL state tracker in sync (no glPopAttrib). Scratch buffers are
 * reused, rather than allocating native buffers every frame.
 */
final class RenderState implements AutoCloseable {
    private boolean blend, cull, lighting, alpha, depth, mask, texture, lightTexture;
    private int srcRGB, dstRGB, srcAlpha, dstAlpha, active, clientActive, matrix, depthFunc, binding,
            lightBinding;
    private float brightnessX, brightnessY;
    private final FloatBuffer color = BufferUtils.createFloatBuffer(16),
            normal = BufferUtils.createFloatBuffer(16), texCoord = BufferUtils.createFloatBuffer(16),
            lightCoord = BufferUtils.createFloatBuffer(16);
    RenderState capture() {
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        alpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        srcRGB = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        dstRGB = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        clientActive = GL11.glGetInteger(GL13.GL_CLIENT_ACTIVE_TEXTURE);
        matrix = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        brightnessX = OpenGlHelper.lastBrightnessX;
        brightnessY = OpenGlHelper.lastBrightnessY;
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, color);
        GL11.glGetFloat(GL11.GL_CURRENT_NORMAL, normal);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glGetFloat(GL11.GL_CURRENT_TEXTURE_COORDS, texCoord);
        binding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        texture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glGetFloat(GL11.GL_CURRENT_TEXTURE_COORDS, lightCoord);
        lightBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        lightTexture = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPushMatrix();
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glMatrixMode(matrix);
        return this;
    }

    static void state(int cap, boolean enabled) {
        if (enabled)
            GL11.glEnable(cap);
        else
            GL11.glDisable(cap);
    }

    @Override
    public void close() {
        OpenGlHelper.glBlendFunc(srcRGB, dstRGB, srcAlpha, dstAlpha);
        state(GL11.GL_BLEND, blend);
        state(GL11.GL_CULL_FACE, cull);
        state(GL11.GL_LIGHTING, lighting);
        state(GL11.GL_ALPHA_TEST, alpha);
        state(GL11.GL_DEPTH_TEST, depth);
        GL11.glDepthMask(mask);
        GL11.glDepthFunc(depthFunc);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glPopMatrix();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, lightBinding);
        state(GL11.GL_TEXTURE_2D, lightTexture);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, binding);
        state(GL11.GL_TEXTURE_2D, texture);
        // Unit 0 uses the legacy TexCoord path which Angelica emulates in its core context.
        GL11.glTexCoord4f(texCoord.get(0), texCoord.get(1), texCoord.get(2), texCoord.get(3));
        // Use Minecraft's supported two-component lightmap path. MultiTexCoord4f is NOT
        // emulated by Angelica and can abort the JVM with lwjgl3ify's core context.
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightCoord.get(0),
                lightCoord.get(1));
        OpenGlHelper.lastBrightnessX = brightnessX;
        OpenGlHelper.lastBrightnessY = brightnessY;
        OpenGlHelper.setClientActiveTexture(clientActive);
        GL11.glNormal3f(normal.get(0), normal.get(1), normal.get(2));
        GL11.glColor4f(color.get(0), color.get(1), color.get(2), color.get(3));
        OpenGlHelper.setActiveTexture(active);
        GL11.glMatrixMode(matrix);
    }
}
