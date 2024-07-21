package com.azukaar.difficultyoverhaul;

import com.azukaar.difficultyoverhaul.entity.mobs.AncientCreeperRenderer;
import com.azukaar.difficultyoverhaul.entity.mobs.RaisedZombieRenderer;

import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD, modid = DifficultyOverhaul.MODID)
public class ModClientSetup {
  
	@SubscribeEvent
	public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerEntityRenderer(ModEntityRegistry.RAISED_ZOMBIE.get(), m -> new RaisedZombieRenderer(m));
		event.registerEntityRenderer(ModEntityRegistry.ANCIENT_CREEPER.get(), m -> new AncientCreeperRenderer(m));
  }
}
