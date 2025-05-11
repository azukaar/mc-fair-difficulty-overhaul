package com.azukaar.difficultyoverhaul;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModBiomeModifier {
    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, DifficultyOverhaul.MODID);

    public static final DeferredRegister<BiomeModifier> BIOME_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIERS, DifficultyOverhaul.MODID);

    private static final Set<Holder<Biome>> zombieBiomes = new HashSet<>();

    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<CopySpawningBiomes>> COLLECT_ZOMBIE_BIOMES_CODEC =
        BIOME_MODIFIER_SERIALIZERS.register("copy_spawning_biomes", () -> MapCodec.unit(CopySpawningBiomes::new));

    static void registerBiomeModifiers(IEventBus eventBus) {        
        BIOME_MODIFIER_SERIALIZERS.register(eventBus);
        BIOME_MODIFIERS.register(eventBus);

        BIOME_MODIFIERS.register("copy_spawning_biomes", () -> new CopySpawningBiomes());
    }

    public static class CopySpawningBiomes implements BiomeModifier {
        @Override
        public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
            if (phase == Phase.ADD) {
                List<MobSpawnSettings.SpawnerData> monsterSpawns = builder.getMobSpawnSettings().getSpawner(MobCategory.MONSTER);
                boolean hasZombieSpawn = monsterSpawns.stream()
                    .anyMatch(spawnerData -> spawnerData.type == EntityType.ZOMBIE);
                if (hasZombieSpawn) {
                    builder.getMobSpawnSettings().addSpawn(
                        MobCategory.MONSTER,
                        new MobSpawnSettings.SpawnerData(ModEntityRegistry.RAISED_ZOMBIE.get(), 100, 1, 3)
                    );
                }
            }
        }

        @Override
        public MapCodec<? extends BiomeModifier> codec() {
            return COLLECT_ZOMBIE_BIOMES_CODEC.get();
        }
    }
}