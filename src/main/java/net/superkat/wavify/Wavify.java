package net.superkat.wavify;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
@Mod(modid="wavify", name="Wavify", version="@VERSION@", acceptableRemoteVersions="*")
public final class Wavify {
 @SidedProxy(clientSide="net.superkat.wavify.client.ClientProxy", serverSide="net.superkat.wavify.CommonProxy")
 public static CommonProxy proxy;
 @Mod.EventHandler public void init(cpw.mods.fml.common.event.FMLInitializationEvent e) { proxy.init(e); }
 @Mod.EventHandler public void preInit(FMLPreInitializationEvent e) { proxy.preInit(e); }
}
