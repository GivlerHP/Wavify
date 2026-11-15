package net.superkat.wavify.renderer;
import java.nio.*;
import org.lwjgl.*;
import org.lwjgl.opengl.*;
import net.minecraft.client.renderer.OpenGlHelper;
/** Real offscreen GL context: verify restoration without opening a game window. */
public final class RenderStateChecks {
 public static void main(String[] args)throws Exception {
 Pbuffer context=new Pbuffer(16,16,new PixelFormat(),null,null);context.makeCurrent();
 try{
 OpenGlHelper.initializeTextures();
 for(int pass=0;pass<12;pass++){
 GL11.glMatrixMode(GL11.GL_MODELVIEW);GL11.glLoadIdentity();GL11.glTranslatef(2,3,4);
 OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glLoadIdentity();GL11.glTranslatef(.2f,.3f,.4f);
 OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);GL11.glMatrixMode(GL11.GL_PROJECTION);
 GL11.glTexCoord4f(.2f,.4f,.6f,.8f);OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,64,128);
 GL11.glColor4f(.2f,.3f,.4f,.5f);GL11.glNormal3f(.1f,.2f,.3f);
 GL11.glDisable(GL11.GL_BLEND);GL11.glEnable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(true);GL11.glEnable(GL11.GL_CULL_FACE);GL11.glEnable(GL11.GL_LIGHTING);GL11.glEnable(GL11.GL_ALPHA_TEST);
 OpenGlHelper.glBlendFunc(GL11.GL_ONE,GL11.GL_ZERO,GL11.GL_ZERO,GL11.GL_ONE);
 String before=snapshot();
 try(RenderState saved=new RenderState().capture()){
 GL11.glEnable(GL11.GL_BLEND);GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDepthMask(false);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_ALPHA_TEST);
 GL11.glTexCoord4f(1,2,3,4);OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);GL11.glColor4f(1,0,0,1);GL11.glNormal3f(0,1,0);OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ZERO);
 OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);GL11.glMatrixMode(GL11.GL_TEXTURE);GL11.glLoadIdentity();GL11.glScalef(4,4,4);GL11.glDisable(GL11.GL_TEXTURE_2D);
 OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glMatrixMode(GL11.GL_MODELVIEW);
 }
 if(!before.equals(snapshot()))throw new AssertionError("GL state changed: "+before+" -> "+snapshot());
 if(GL11.glGetError()!=GL11.GL_NO_ERROR)throw new AssertionError("GL error");
 }
 System.out.println("Wavify offscreen GL restoration passed (12 rounds). Renderer: "+GL11.glGetString(GL11.GL_RENDERER));
 }finally{context.destroy();}
 }
 private static String snapshot(){StringBuilder b=new StringBuilder();
 for(int i:new int[]{GL13.GL_ACTIVE_TEXTURE,GL11.GL_MATRIX_MODE,GL14.GL_BLEND_SRC_RGB,GL14.GL_BLEND_DST_RGB,GL14.GL_BLEND_SRC_ALPHA,GL14.GL_BLEND_DST_ALPHA,GL11.GL_DEPTH_FUNC})b.append(GL11.glGetInteger(i)).append(',');
 for(int i:new int[]{GL11.GL_BLEND,GL11.GL_DEPTH_TEST,GL11.GL_CULL_FACE,GL11.GL_LIGHTING,GL11.GL_ALPHA_TEST})b.append(GL11.glIsEnabled(i)).append(',');b.append(GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK));
 FloatBuffer f=BufferUtils.createFloatBuffer(16);GL11.glGetFloat(GL11.GL_CURRENT_COLOR,f);for(int i=0;i<4;i++)b.append('/').append(f.get(i));
 int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),mode=GL11.glGetInteger(GL11.GL_MATRIX_MODE);
 for(int unit:new int[]{OpenGlHelper.defaultTexUnit,OpenGlHelper.lightmapTexUnit}){OpenGlHelper.setActiveTexture(unit);b.append('/').append(GL11.glIsEnabled(GL11.GL_TEXTURE_2D)).append('/').append(GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D));GL11.glGetFloat(GL11.GL_TEXTURE_MATRIX,f);for(int i=0;i<16;i++)b.append('/').append(f.get(i));GL11.glGetFloat(GL11.GL_CURRENT_TEXTURE_COORDS,f);for(int i=0;i<4;i++)b.append('/').append(f.get(i));}
 OpenGlHelper.setActiveTexture(active);GL11.glMatrixMode(mode);b.append('/').append(OpenGlHelper.lastBrightnessX).append('/').append(OpenGlHelper.lastBrightnessY);return b.toString();
 }
}