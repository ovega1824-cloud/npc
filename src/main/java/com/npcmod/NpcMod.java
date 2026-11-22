package com.npcmod;

import com.npcmod.network.NpcNetwork;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(NpcMod.MODID)
public class NpcMod {
    public static final String MODID = "npcmod";

    public NpcMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModRegistry.register(bus);
        bus.addListener((FMLCommonSetupEvent e) -> e.enqueueWork(NpcNetwork::register));
    }
}
