// Adapted from Wavify by Superkat32 (MIT); 1.7.10 backport by Givler.
package net.superkat.wavify.river;
import net.superkat.wavify.compat.*;
import net.minecraft.block.BlockLiquid;
import net.minecraft.util.Vec3;
public final class RiverFlow {
 private RiverFlow(){}
 public static final class Flow {private final double x,z;public Flow(double x,double z){this.x=x;this.z=z;}public double dirX(){return x;}public double dirZ(){return z;}}
 public static final class Banks {private final int left,right;public Banks(int l,int r){left=l;right=r;}public int width(){return left+right+1;}public int clearance(){return Math.min(left,right);}}
 public static Flow dynamicFlowAt(WorldAccess level,double x,int y,double z){
 BlockPos p=BlockPos.containing(x,y,z);if(!level.water(p))return null;
 // Native fluid metadata flow works without a dependency on modern flow mods.
 if(!(level.world.getBlock(p.getX(),y,p.getZ()) instanceof BlockLiquid))return null;
 double angle=BlockLiquid.getFlowDirection(level.world,p.getX(),y,p.getZ(),net.minecraft.block.material.Material.water);
 if(angle< -999)return null;return new Flow(-Math.sin(angle),Math.cos(angle));
 }
 public static boolean isRiverWater(WorldAccess level,BlockPos p){for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(level.river(p.offset(x,0,z)))return true;return false;}
 public static Banks banksAt(WorldAccess level,double x,double z,int y,double dx,double dz,int max){int left=0,right=0;
 for(int s=1;s<=max;s++){if(surfaceWaterY(level,x-dz*s,z+dx*s,y)==Integer.MIN_VALUE)break;left++;}
 for(int s=1;s<=max;s++){if(surfaceWaterY(level,x+dz*s,z-dx*s,y)==Integer.MIN_VALUE)break;right++;}return new Banks(left,right);}
 public static int depthAt(WorldAccess level,BlockPos p,int max){int d=0;while(d<max&&level.water(p.offset(0,-d,0)))d++;return d;}
 public static boolean isSurfaceWater(WorldAccess level,BlockPos p){return level.surface(p);}
 public static int surfaceWaterY(WorldAccess level,double x,double z,int preferredY){for(int y=preferredY+1;y>=preferredY-2;y--)if(level.surface(BlockPos.containing(x,y,z)))return y;return Integer.MIN_VALUE;}
 public static boolean isOceanSurrounded(WorldAccess level,BlockPos pos,int maxRay){boolean ocean=false;
 for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){if(dx==0&&dz==0)continue;int py=pos.getY();
 for(int s=1;s<=maxRay;s++){int x=pos.getX()+dx*s,z=pos.getZ()+dz*s;int y=surfaceWaterY(level,x+.5,z+.5,py);if(y==Integer.MIN_VALUE)return false;py=y;BlockPos p=new BlockPos(x,y,z);if(level.ocean(p)){ocean=true;break;}if(!level.river(p))return false;}}
 return ocean;}
}
