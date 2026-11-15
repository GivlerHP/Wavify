package net.superkat.wavify.client;
import java.io.File;
import java.util.List;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.*;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.command.*;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.superkat.wavify.CommonProxy;
import net.superkat.wavify.compat.WorldAccess;
import net.superkat.wavify.config.WavifyConfig;
import net.superkat.wavify.particles.WaveSplash;
import net.superkat.wavify.renderer.*;
import net.superkat.wavify.sound.WaveAmbience;
/** One scene per current client world. No mixins/coremod hooks or mandatory client dependencies. */
public final class ClientProxy extends CommonProxy implements IResourceManagerReloadListener {
 private final WaveAtlas atlas=new WaveAtlas();private final WaveRenderer renderer=new WaveRenderer(atlas);
 private final WaveAmbience sounds=new WaveAmbience();private WaveScene scene;private long generation;private File config;
 @Override public void preInit(FMLPreInitializationEvent e){
 config=new File(e.getModConfigurationDirectory(),"wavify.cfg");WavifyConfig.load(config);
 MinecraftForge.EVENT_BUS.register(this);FMLCommonHandler.instance().bus().register(this);
 ClientCommandHandler.instance.registerCommand(new Command());
 e.getModLog().info("Wavify 1.7.10 backport by Givler; original Wavify by Superkat32 (MIT). Forge source port by Icywhenhot.");
 }
 // In 1.7.10 preInit occurs BEFORE Minecraft creates TextureManager.
 @Override public void init(cpw.mods.fml.common.event.FMLInitializationEvent e){
 IResourceManager manager=Minecraft.getMinecraft().getResourceManager();if(manager instanceof IReloadableResourceManager)((IReloadableResourceManager)manager).registerReloadListener(this);
 }
 @Override public void onResourceManagerReload(IResourceManager manager){reset();WaterPalette.reload(manager);atlas.onResourceManagerReload(manager);}
 @SubscribeEvent public void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;Minecraft mc=Minecraft.getMinecraft();
 if(!WavifyConfig.enabled||mc.theWorld==null||mc.thePlayer==null){reset();return;}
 if(scene==null||scene.level.world!=mc.theWorld){reset();scene=new WaveScene(new WorldAccess(mc.theWorld),++generation);}
 if(mc.isGamePaused())return;
 scene.tick();sounds.tick(scene);
 }
 @SubscribeEvent public void render(RenderWorldLastEvent e){if(WavifyConfig.enabled&&scene!=null&&scene.level.world==Minecraft.getMinecraft().theWorld)renderer.render(scene,e.partialTicks);}
 @SubscribeEvent public void unload(WorldEvent.Unload e){if(scene!=null&&e.world==scene.level.world)reset();}
 @SubscribeEvent public void stitch(TextureStitchEvent.Pre e){if(e.map.getTextureType()!=0)return;
 for(int type=0;type<2;type++)for(int i=0;i<WaveSplash.SPRITES[type].length;i++){
 // Atlas type 0 is the block atlas used by vanilla FX layer 1.
 WaveSplash.SPRITES[type][i]=e.map.registerIcon("wavify:splash/"+(type==0?"splash":"bigsplash")+(i+1));}
 for(int type=0;type<2;type++)for(int i=0;i<20;i++)net.superkat.wavify.particles.WaveSpray.SPRITES[type][i]=e.map.registerIcon("wavify:spray/"+(type==0?"wave":"wavewhite")+(i+1));}
 private void reset(){sounds.stop();if(scene!=null){scene.close();scene=null;}generation++;}
 private final class Command extends CommandBase {
 @Override public String getCommandName(){return "wavify";}
 @Override public String getCommandUsage(ICommandSender s){return "/wavify <status|reload|on|off>";}
 @Override public int getRequiredPermissionLevel(){return 0;}
 @Override public boolean canCommandSenderUseCommand(ICommandSender s){return true;}
 @Override public void processCommand(ICommandSender sender,String[] args){String action=args.length==0?"status":args[0];
 if("reload".equalsIgnoreCase(action)){reset();WavifyConfig.load(config);}
 else if("on".equalsIgnoreCase(action))WavifyConfig.enabled=true;
 else if("off".equalsIgnoreCase(action)){WavifyConfig.enabled=false;reset();}
 else if(!"status".equalsIgnoreCase(action))throw new WrongUsageException(getCommandUsage(sender));
 sender.addChatMessage(new ChatComponentText("Wavify: "+(WavifyConfig.enabled?"on":"off")+", waves="+(scene==null?0:scene.waves.size())+", scanned chunks="+(scene==null?0:scene.surface.chunks.size())+". Config: wavify.cfg"));
 }
 @Override public List addTabCompletionOptions(ICommandSender s,String[] args){return args.length==1?getListOfStringsMatchingLastWord(args,"status","reload","on","off"):null;}
 }
}
