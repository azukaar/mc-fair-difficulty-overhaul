package com.azukaar.difficultyoverhaul.difficulty;

import com.azukaar.difficultyoverhaul.DifficultyOverhaul;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class WorldDifficultyManager extends SavedData {
    private static final String DATA_NAME = "afdo_world_difficulty";
    private String worldDifficulty;

    public WorldDifficultyManager() {
    }

    public WorldDifficultyManager(CompoundTag nbt) {
        this.worldDifficulty = nbt.getString("WorldDifficulty");
        // Fallback to default if empty or invalid
        if (!DifficultyCommand.DIFFICULTY_STRINGS.contains(this.worldDifficulty)) {
        }
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag worldDifficultyTag = new CompoundTag();
        worldDifficultyTag.putString("WorldDifficulty", worldDifficulty);
        nbt.put("WorldDifficulty", worldDifficultyTag);
        return nbt;
    }
    
    public void setWorldDifficulty(MinecraftServer server, String difficulty) {
        if (DifficultyCommand.DIFFICULTY_STRINGS.contains(difficulty.toLowerCase())) {
            worldDifficulty = difficulty.toLowerCase();
            setDirty();
        }
    }

    public String getWorldDifficulty() {
        if (worldDifficulty == null || worldDifficulty.isEmpty()) {
            return DifficultyOverhaul.CURRENT_SERVER_DIFFICULTY.getKey().toString();
        }
        return worldDifficulty;
    }

    public static WorldDifficultyManager get(MinecraftServer server) {
        ServerLevel level = server.overworld();

        return level.getDataStorage().computeIfAbsent(
            (CompoundTag tag) -> new WorldDifficultyManager(tag),
            WorldDifficultyManager::new,
            DATA_NAME
        );
    }
}

