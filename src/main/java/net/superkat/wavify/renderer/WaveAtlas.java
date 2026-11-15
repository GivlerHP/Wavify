package net.superkat.wavify.renderer;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.*;
import net.minecraft.util.ResourceLocation;
/** Own small atlas: no changes to terrain atlases, CTM, display lists or shader programs. */
public final class WaveAtlas implements IResourceManagerReloadListener {
 public static final String[] NAMES={"moving","moving_white","washing_top_colorable","washing_top_white","washing_bottom_colorable","washing_bottom_white","wet_overlay"};
 public static final ResourceLocation LOCATION=new ResourceLocation("wavify","runtime/waves");
 private final Sheet[] sheets=new Sheet[NAMES.length];public boolean ready;
 @Override public void onResourceManagerReload(IResourceManager manager){ready=false;
 try{
 BufferedImage[] images=new BufferedImage[NAMES.length];int[] times=new int[NAMES.length],frames=new int[NAMES.length];int width=0,height=0;
 for(int i=0;i<NAMES.length;i++){
 ResourceLocation path=new ResourceLocation("wavify","textures/wave/"+NAMES[i]+".png");
 try(InputStream stream=manager.getResource(path).getInputStream()){images[i]=ImageIO.read(stream);}
 if(images[i]==null||images[i].getWidth()>1024||images[i].getHeight()>8192)throw new IOException("Invalid wave sheet "+path);
 times[i]=5;int frameHeight=i==6?images[i].getHeight():16;
 try(InputStream meta=manager.getResource(new ResourceLocation(path.getResourceDomain(),path.getResourcePath()+".mcmeta")).getInputStream();Reader reader=new InputStreamReader(meta,"UTF-8")){
 JsonObject root=new JsonParser().parse(reader).getAsJsonObject();if(root.has("wave_animation")){JsonObject a=root.getAsJsonObject("wave_animation");if(a.has("frametime"))times[i]=Math.max(0,a.get("frametime").getAsInt());if(a.has("frame_height"))frameHeight=a.get("frame_height").getAsInt();}
 }catch(FileNotFoundException absent){}catch(IOException absent){}
 if(frameHeight<=0||images[i].getHeight()%frameHeight!=0)throw new IOException("Invalid frame height: "+path);
 frames[i]=images[i].getHeight()/frameHeight;width+=images[i].getWidth()+2;height=Math.max(height,images[i].getHeight()+2);
 }
 int w=powerOfTwo(width),h=powerOfTwo(height);BufferedImage atlas=new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);int x=1;
 for(int i=0;i<NAMES.length;i++){
 BufferedImage image=images[i];int iw=image.getWidth(),ih=image.getHeight();atlas.setRGB(x,1,iw,ih,image.getRGB(0,0,iw,ih,null,0,iw),0,iw);
 sheets[i]=new Sheet((x+.01f)/w,(x+iw-.01f)/w,1f/h,(float)ih/h,frames[i],times[i]);x+=iw+2;
 }
 Minecraft.getMinecraft().getTextureManager().deleteTexture(LOCATION);
 Minecraft.getMinecraft().getTextureManager().loadTexture(LOCATION,new DynamicTexture(atlas));ready=true;
 }catch(Exception e){org.apache.logging.log4j.LogManager.getLogger("Wavify").error("Cannot reload wave atlas; disabling wave rendering until next reload",e);}
 }
 private static int powerOfTwo(int n){int p=1;while(p<n)p<<=1;return p;}
 public Sheet sheet(int index){return sheets[index];}
 public static final class Sheet {
 public final float u0,u1,v0,span;public final int frames,time;
 Sheet(float a,float b,float c,float d,int f,int t){u0=a;u1=b;v0=c;span=d;frames=f;time=t;}
 public float frameV(int age,int maxAge){int f=time>0?Math.floorMod(age/time,frames):Math.max(0,Math.min(frames-1,(int)((double)age/Math.max(1,maxAge)*frames)));return v0+span*f/frames;}
 }
}
