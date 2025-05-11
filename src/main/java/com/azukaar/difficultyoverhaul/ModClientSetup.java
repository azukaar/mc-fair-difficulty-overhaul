package com.azukaar.difficultyoverhaul;

import com.azukaar.difficultyoverhaul.entity.mobs.AncientCreeperRenderer;
import com.azukaar.difficultyoverhaul.entity.mobs.RaisedZombieRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = DifficultyOverhaul.MODID)
public class ModClientSetup {
  
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntityRegistry.RAISED_ZOMBIE.get(), m -> new RaisedZombieRenderer(m));
		event.registerEntityRenderer(ModEntityRegistry.ANCIENT_CREEPER.get(), m -> new AncientCreeperRenderer(m));
  }
}
