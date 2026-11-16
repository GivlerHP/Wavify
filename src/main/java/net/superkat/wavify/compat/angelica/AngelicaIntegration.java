package net.superkat.wavify.compat.angelica;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * Optional Angelica entry point. UI edits are pending until Apply; the callback then releases the client's
 * current wave handler. Simulation ownership stays in ClientProxy.
 */
public final class AngelicaIntegration {
    private final Runnable apply;

    private AngelicaIntegration(Runnable apply) {
        this.apply = apply;
    }

    public static void register(Runnable apply) {
        if (Loader.isModLoaded("angelica"))
            MinecraftForge.EVENT_BUS.register(new AngelicaIntegration(apply));
    }

    // Attach after other mods have added/rebuilt their Angelica pages (including MBO).
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void init(GuiScreenEvent.InitGuiEvent.Post event) {
        AngelicaWaveOptions.attach(event.gui, apply);
    }
}
