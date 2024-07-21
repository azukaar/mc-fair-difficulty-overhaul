package com.azukaar.difficultyoverhaul;

// source
// https://github.com/TeamTwilight/twilightforest/blob/ca471cdefc8c73defcd840c93822fb6def49b274/src/main/java/twilightforest/init/TFEntities.java#L119

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Locale;

import com.azukaar.difficultyoverhaul.entity.mobs.AncientCreeper;
import com.azukaar.difficultyoverhaul.entity.mobs.RaisedZombie;

@EventBusSubscriber(modid = DifficultyOverhaul.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEntityRegistry {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, DifficultyOverhaul.MODID);
	public static final DeferredRegister<Item> SPAWN_EGGS = DeferredRegister.create(Registries.ITEM, DifficultyOverhaul.MODID);

	public static final RegistryObject<EntityType<RaisedZombie>> RAISED_ZOMBIE = make(prefix("raised_zombie"), RaisedZombie::new, MobCategory.MONSTER, 0.6F, 1.95F, false, 0x8F5F5F, 0xAF6C65);
	public static final RegistryObject<EntityType<AncientCreeper>> ANCIENT_CREEPER = make(prefix("ancient_creeper"), AncientCreeper::new, MobCategory.MONSTER, 0.6F, 1.7F, 0.6F, false, 0x769300, 0x2c2a28);

	public static ResourceLocation prefix(String name) {
		return new ResourceLocation(DifficultyOverhaul.MODID, name.toLowerCase(Locale.ROOT));
	}

 	//Same as below, but with riding offset set to 0.0F;
   private static <E extends Entity> RegistryObject<EntityType<E>> make(ResourceLocation id, EntityType.EntityFactory<E> factory, MobCategory classification, float width, float height, int primary, int secondary) {
		return make(id, factory, classification, width, height, primary, secondary, 0.0F);
	}

	private static <E extends Entity> RegistryObject<EntityType<E>> make(ResourceLocation id, EntityType.EntityFactory<E> factory, MobCategory classification, float width, float height, int primary, int secondary, float ridingOffset) {
		return make(id, factory, classification, width, height, false, primary, secondary, ridingOffset);
	}

	//Same as below, but with riding offset set to 0.0F;
	private static <E extends Entity> RegistryObject<EntityType<E>> make(ResourceLocation id, EntityType.EntityFactory<E> factory, MobCategory classification, float width, float height, boolean fireproof, int primary, int secondary) {
		return make(id, factory, classification, width, height, fireproof, primary, secondary, 0.0F);
	}

	private static <E extends Entity> RegistryObject<EntityType<E>> make(ResourceLocation id, EntityType.EntityFactory<E> factory, MobCategory classification, float width, float height, boolean fireproof, int primary, int secondary, float ridingOffset) {
		return build(id, makeBuilder(factory, classification, width, height, 80, 3, ridingOffset), fireproof, primary, secondary);
	}

	//Same as below, but with riding offset set to 0.0F;
	private static <E extends Entity> RegistryObject<EntityType<E>> make(ResourceLocation id, EntityType.EntityFactory<E> factory, MobCategory classification, float width, float height, float eyeHeight, boolean fireproof, int primary, int secondary) {
		return make(id, factory, classification, width, height, eyeHeight, fireproof, primary, secondary, 0.0F);
	}

	private static <E extends Entity> RegistryObject<EntityType<E>> make(ResourceLocation id, EntityType.EntityFactory<E> factory, MobCategory classification, float width, float height, float eyeHeight, boolean fireproof, int primary, int secondary, float ridingOffset) {
		return build(id, makeBuilder(factory, classification, width, height, 80, 3, ridingOffset), fireproof, primary, secondary);
	} 
  
	@SuppressWarnings("unchecked")
	private static <E extends Entity> RegistryObject<EntityType<E>> build(ResourceLocation id, EntityType.Builder<E> builder, boolean fireproof, int primary, int secondary) {
		if (fireproof) builder.fireImmune();
		RegistryObject<EntityType<E>> ret = ENTITIES.register(id.getPath(), () -> builder.build(id.toString()));
		if (primary != 0 && secondary != 0) {
			SPAWN_EGGS.register(id.getPath() + "_spawn_egg", () -> new ForgeSpawnEggItem(() -> (EntityType<? extends Mob>) ret.get(), primary, secondary, new Item.Properties()));		}
		return ret;
	}

	private static <E extends Entity> EntityType.Builder<E> makeBuilder(EntityType.EntityFactory<E> factory, MobCategory classification, float width, float height, int range, int interval, float ridingOffset) {
		return EntityType.Builder.of(factory, classification)
			.sized(width, height)
			.setTrackingRange(range)
			.setUpdateInterval(interval)
			.setShouldReceiveVelocityUpdates(true);
	}
  
	@SubscribeEvent
	public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
		event.register(RAISED_ZOMBIE.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
		event.register(ANCIENT_CREEPER.get(), SpawnPlacements.Type.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules, SpawnPlacementRegisterEvent.Operation.REPLACE);
	}
  
	@SubscribeEvent
	public static void addEntityAttributes(EntityAttributeCreationEvent event) {
		event.put(RAISED_ZOMBIE.get(), RaisedZombie.registerAttributes().build());
		event.put(ANCIENT_CREEPER.get(), AncientCreeper.registerAttributes().build());
  }
}