package net.superkat.wavify.particles;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.world.World;
/** Uses the original animated splash sprites in the vanilla particle atlas. */
public final class WaveSplash extends EntityFX {
 public static final net.minecraft.util.IIcon[][] SPRITES={new net.minecraft.util.IIcon[4],new net.minecraft.util.IIcon[11]};
 private final int type;
 public WaveSplash(World w,double x,double y,double z,double vx,double vy,double vz,boolean big){super(w,x,y,z,0,0,0);type=big?1:0;motionX=vx;motionY=vy;motionZ=vz;particleMaxAge=big?22:12;particleScale=big?2.0f:.7f;particleGravity=1f;int color=new net.superkat.wavify.compat.WorldAccess((net.minecraft.client.multiplayer.WorldClient)w).waterColor(net.superkat.wavify.compat.BlockPos.containing(x,y,z));particleRed=(color>>16&255)/255f;particleGreen=(color>>8&255)/255f;particleBlue=(color&255)/255f;
 noClip=false;setSize(.08f,.08f);updateSprite();}
 private void updateSprite(){int f=Math.min(SPRITES[type].length-1,particleAge*SPRITES[type].length/particleMaxAge);if(SPRITES[type][f]!=null)setParticleIcon(SPRITES[type][f]);}
 @Override public int getFXLayer(){return 1;}
 @Override public void onUpdate(){if(!worldObj.getChunkProvider().chunkExists(((int)Math.floor(posX))>>4,((int)Math.floor(posZ))>>4)){setDead();return;}super.onUpdate();updateSprite();}
}
