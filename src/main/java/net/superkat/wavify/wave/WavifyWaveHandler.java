package net.superkat.wavify.wave;
import net.superkat.wavify.compat.WorldAccess;
import net.superkat.wavify.compat.BlockPos;
import net.minecraft.block.material.Material;
/** Shared native-fluid predicates used by the ported simulation. */
public final class WavifyWaveHandler {
 private WavifyWaveHandler(){}
 public static boolean posIsWater(WorldAccess world,BlockPos pos){return world.water(pos);}
 public static boolean stateIsWater(WorldAccess.State state){return state.block.getMaterial()==Material.water&&!state.block.getMaterial().blocksMovement();}
}
