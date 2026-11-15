package net.superkat.wavify.scan;
/** Immutable 18x18 chunk snapshot including a one-column border. Contains no World reference. */
public final class SurfaceSnapshot {
 public final int chunkX,chunkZ;public final long generation,version,tick;
 public final int[] heights;public final byte[] flags;
 public SurfaceSnapshot(int x,int z,long generation,long version,long tick,int[] heights,byte[] flags){this.chunkX=x;this.chunkZ=z;this.generation=generation;this.version=version;this.tick=tick;this.heights=heights.clone();this.flags=flags.clone();}
}
