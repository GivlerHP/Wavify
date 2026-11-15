// Directional crest spray adapted from Wavify by Superkat32 (MIT), backport by Givler.
package net.superkat.wavify.particles;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.superkat.wavify.compat.*;
public final class WaveSpray extends EntityFX {
 public static final IIcon[][] SPRITES={new IIcon[20],new IIcon[20]};
 private final WorldAccess level;private final float yaw,intensity,size;private final int type;
 private float roll=75,previousRoll=75;
 public WaveSpray(WorldAccess level,double x,double y,double z,double vx,double vz,float yaw,float intensity,float scale,boolean white){
 super(level.world,x,y,z,0,0,0);this.level=level;this.yaw=yaw;this.intensity=intensity;type=white?1:0;
 double speed=(Math.abs(vx)+Math.abs(vz))/1.25;motionX=Math.cos(Math.toRadians(yaw))*speed;motionZ=Math.sin(Math.toRadians(yaw))*speed;motionY=.15*intensity;
 particleMaxAge=(int)(50+intensity*5);size=Mth.clamp(intensity*4,1,scale*2);noClip=false;setSize(.2f,.2f);
 if(!white){int color=level.waterColor(BlockPos.containing(x,y,z));particleRed=(color>>16&255)/255f;particleGreen=(color>>8&255)/255f;particleBlue=(color&255)/255f;}
 }
 @Override public int getFXLayer(){return 1;}
 @Override public void onUpdate(){
 if(!level.loaded(BlockPos.containing(posX,posY,posZ))||particleAge++>=particleMaxAge){setDead();return;}
 prevPosX=posX;prevPosY=posY;prevPosZ=posZ;previousRoll=roll;
 motionY-=.02;double oldY=motionY;Vec3 move=level.collide(boundingBox.expand(0,.15,0),motionX,motionY,motionZ);
 boundingBox.offset(move.xCoord,move.yCoord,move.zCoord);posX=(boundingBox.minX+boundingBox.maxX)*.5;posY=boundingBox.minY;posZ=(boundingBox.minZ+boundingBox.maxZ)*.5;
 onGround=oldY!=move.yCoord&&oldY<0;if(onGround){motionY=0;motionX*=.7;motionZ*=.7;}else{motionX*=.98;motionZ*=.98;}
 roll=onGround?roll*.7f:roll+(float)motionY*35;
 if(level.water(BlockPos.containing(posX,posY+1,posZ))){if(type==1)for(int i=0;i<5;i++)level.splash(false,posX+rand.nextGaussian(),posY+1,posZ+rand.nextGaussian(),rand.nextGaussian()*.05,.1+intensity,rand.nextGaussian()*.05);setDead();}
 }
 @Override public void renderParticle(Tessellator t,float partial,float rx,float rz,float ryz,float rxy,float rxz){
 IIcon sprite=SPRITES[type][Math.min(19,particleAge*20/particleMaxAge)];if(sprite==null)return;
 double x=prevPosX+(posX-prevPosX)*partial-interpPosX,y=prevPosY+(posY-prevPosY)*partial-interpPosY,z=prevPosZ+(posZ-prevPosZ)*partial-interpPosZ;
 double a=Math.toRadians(-90-yaw),c=Math.cos(a),s=Math.sin(a),r=Math.toRadians(previousRoll+(roll-previousRoll)*partial),cr=Math.cos(r),sr=Math.sin(r);
 t.setBrightness(getBrightnessForRender(partial));t.setColorRGBA_F(particleRed,particleGreen,particleBlue,particleAlpha);t.setNormal(0,1,0);
 // R_x(-90) R_z(-90-yaw) R_x(roll), matching the original oriented spray plane.
 vertex(t,x,y,z,-size,-size,c,s,cr,sr,sprite.getMaxU(),sprite.getMaxV());vertex(t,x,y,z,-size,size,c,s,cr,sr,sprite.getMaxU(),sprite.getMinV());vertex(t,x,y,z,size,size,c,s,cr,sr,sprite.getMinU(),sprite.getMinV());vertex(t,x,y,z,size,-size,c,s,cr,sr,sprite.getMinU(),sprite.getMaxV());
 }
 private void vertex(Tessellator t,double x,double y,double z,double px,double py,double c,double s,double cr,double sr,double u,double v){double rotatedY=py*cr,rotatedZ=py*sr;t.addVertexWithUV(x+px*c-rotatedY*s,y+rotatedZ,z-px*s-rotatedY*c,u,v);}
}