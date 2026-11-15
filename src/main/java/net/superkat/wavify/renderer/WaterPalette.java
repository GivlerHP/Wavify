package net.superkat.wavify.renderer;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
/** In 1.7 water RGB lives in both the texture and the biome multiplier.
 * Unlike modern water, the default biome multiplier alone is usually white. */
public final class WaterPalette {
 private static int textureColor=0x3F76E4;
 private WaterPalette(){}
 public static void reload(IResourceManager resources){
 try(InputStream input=resources.getResource(new ResourceLocation("minecraft","textures/blocks/water_still.png")).getInputStream()){
 BufferedImage image=ImageIO.read(input);if(image!=null)textureColor=average(image);
 }catch(java.io.IOException missing){textureColor=0x3F76E4;}
 }
 public static int average(BufferedImage image){long red=0,green=0,blue=0,weight=0;
 for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
 int rgba=image.getRGB(x,y),a=rgba>>>24;red+=(long)(rgba>>16&255)*a;green+=(long)(rgba>>8&255)*a;blue+=(long)(rgba&255)*a;weight+=a;}
 return weight==0?0x3F76E4:(int)(red/weight)<<16|(int)(green/weight)<<8|(int)(blue/weight);
 }
 public static int color(int biomeTint){return multiply(textureColor,biomeTint);}
 public static int multiply(int texture,int tint){return ((texture>>16&255)*(tint>>16&255)/255)<<16|((texture>>8&255)*(tint>>8&255)/255)<<8|(texture&255)*(tint&255)/255;}
}