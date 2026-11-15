package net.superkat.wavify.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.superkat.wavify.compat.*;
import net.superkat.wavify.scan.*;
import net.superkat.wavify.wave.*;
import net.superkat.wavify.river.*;
import net.superkat.wavify.config.WavifyConfig;
public final class WaveScene implements AutoCloseable {
 public final WorldAccess level;public final SurfaceCache surface;public final List<Wave> waves=new ArrayList<Wave>();
 public final Map<BlockPos,Long> wet=new HashMap<BlockPos,Long>();
 private final RiverFlowField field=new RiverFlowField();
 private long tick,frontBirth;private final ArrayDeque<ShoreScanner.Front> spawnQueue=new ArrayDeque<ShoreScanner.Front>();
 public WaveScene(WorldAccess world,long generation){level=world;surface=new SurfaceCache(world,generation);}
 public void tick(){tick++;Minecraft mc=Minecraft.getMinecraft();surface.tick(Mth.floor(mc.thePlayer.posX),Mth.floor(mc.thePlayer.posZ));
 if(tick%80==1&&WavifyConfig.enableOceanWaves){
 spawnQueue.clear();List<ShoreScanner.Site> sites=new ArrayList<ShoreScanner.Site>();for(ShoreScanner.Result r:surface.chunks.values())sites.addAll(r.sites);
 List<ShoreScanner.Front> fronts=ShoreScanner.fronts(sites);Collections.shuffle(fronts,level.getRandom());spawnQueue.addAll(fronts);frontBirth=tick;}
 // Spread creation/collision work rather than spawning an entire coastline in one frame.
 for(int n=0;n<16&&!spawnQueue.isEmpty()&&waves.size()<WavifyConfig.maxWaves;n++){
 ShoreScanner.Front s=spawnQueue.remove();if(!level.surface(s.pos)||!WavifyConfig.enableOceanWaves)continue;
 double dx=mc.thePlayer.posX-s.pos.getX(),dz=mc.thePlayer.posZ-s.pos.getZ();
 if(dx*dx+dz*dz>WavifyConfig.chunkRadius*WavifyConfig.chunkRadius*256)continue;
 Wave w=new Wave(level,s.pos.above(),s.yaw,.65f,s.large);w.setWidth(s.width);
 // Stagger CPU work across ticks without staggering the visible crest animation.
 for(int elapsed=0;elapsed<Math.min(12,tick-frontBirth)&&!w.isDead();elapsed++)w.tick();
 if(!w.isDead())waves.add(w);
 }
 if(WavifyConfig.enableRiverWaves&&WavifyConfig.riverWaveDensity>0&&tick%40==0)spawnRivers();
 Iterator<Wave> iterator=waves.iterator();while(iterator.hasNext()){
 Wave w=iterator.next();boolean river=w instanceof RiverWave;
 if(!level.loaded(w.getBlockPos())||(river?!WavifyConfig.enableRiverWaves:!WavifyConfig.enableOceanWaves)){iterator.remove();continue;}
 w.tick();if(w.isDead()){iterator.remove();continue;}
 if(WavifyConfig.enableWetOverlay&&tick%5==0)for(BlockPos p:w.getCoveredBlocks())if(wet.size()<2048)wet.put(p,tick+60);
 }
 if(!WavifyConfig.enableWetOverlay)wet.clear();
 Iterator<Map.Entry<BlockPos,Long>> wi=wet.entrySet().iterator();while(wi.hasNext()){Map.Entry<BlockPos,Long> e=wi.next();if(e.getValue()<=tick||!level.loaded(e.getKey()))wi.remove();}
 }
 private void spawnRivers(){
 Map<Long,Set<BlockPos>> riverWaters=new HashMap<Long,Set<BlockPos>>();List<BlockPos> candidates=new ArrayList<BlockPos>();
 Minecraft mc=Minecraft.getMinecraft();double radiusSq=WavifyConfig.riverWaveSpawnRadius*WavifyConfig.riverWaveSpawnRadius;
 for(Map.Entry<Long,ShoreScanner.Result> e:surface.chunks.entrySet()){
 riverWaters.put(e.getKey(),e.getValue().rivers);for(BlockPos p:e.getValue().waters){double dx=p.getX()+.5-mc.thePlayer.posX,dz=p.getZ()+.5-mc.thePlayer.posZ;
 if(dx*dx+dz*dz<=radiusSq&&(e.getValue().rivers.contains(p)||RiverFlow.dynamicFlowAt(level,p.getX()+.5,p.getY(),p.getZ()+.5)!=null))candidates.add(p);}}
 field.ensureBuilt(riverWaters,mc.thePlayer.posX,mc.thePlayer.posZ,tick);
 int count=0;for(Wave w:waves)if(w instanceof RiverWave)count++;
 int target=Math.min(48,(int)Math.round(candidates.size()/100.0*WavifyConfig.riverWaveDensity));
 Collections.shuffle(candidates,level.getRandom());int spawned=0;
 for(BlockPos p:candidates){if(count>=target||waves.size()>=WavifyConfig.maxWaves||spawned>=8)break;
 if(nearWave(p,5)||!level.surface(p))continue;
 RiverFlow.Flow flow=RiverFlow.dynamicFlowAt(level,p.getX()+.5,p.getY(),p.getZ()+.5);
 if(flow==null){flow=field.flowAt(p.getX()+.5,p.getZ()+.5);if(flow==null||field.isOceanSurroundedRiver(level,p)||RiverFlow.banksAt(level,p.getX()+.5,p.getZ()+.5,p.getY(),flow.dirX(),flow.dirZ(),4).clearance()<1)continue;}
 waves.add(new RiverWave(level,p,field,flow.dirX(),flow.dirZ(),WavifyConfig.riverWaveTravelBlocks));count++;spawned++;
 }
 }
 private boolean nearWave(BlockPos p,double distance){for(Wave w:waves){double dx=w.x-p.getX(),dz=w.z-p.getZ();if(dx*dx+dz*dz<distance*distance&&Math.abs(w.y-p.getY())<3)return true;}return false;}
 @Override public void close(){surface.close();waves.clear();wet.clear();spawnQueue.clear();}
}
