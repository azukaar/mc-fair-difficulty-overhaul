package com.azukaar.difficultyoverhaul.difficulty;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PlayerDifficultyManager extends SavedData {
  private static final String DATA_NAME = "player_difficulties";
  private final CompoundTag difficulties;

  public PlayerDifficultyManager() {
      this.difficulties = new CompoundTag();
  }

  public PlayerDifficultyManager(CompoundTag nbt) {
    this.difficulties = nbt.getCompound("Difficulties");
  }

  @Override
  public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
      nbt.put("Difficulties", difficulties);
      return nbt;
  }
   
  public void setPlayerDifficulty(UUID playerUUID, String difficulty) {
    setPlayerDifficulty(playerUUID.toString(), difficulty);
  }
   
  public void setPlayerDifficulty(String playerUUID, String difficulty) {
    if (DifficultyCommand.DIFFICULTY_STRINGS.contains(difficulty.toLowerCase())) {
        difficulties.putString(playerUUID, difficulty.toLowerCase());
        setDirty();
    }
  }

  public String getPlayerDifficulty(UUID playerUUID) {
    if (difficulties.contains(playerUUID.toString())) {
        return difficulties.getString(playerUUID.toString());
    }
    return DifficultyConfig.SERVER.serverDifficulty.get();
  }

  public static PlayerDifficultyManager get(MinecraftServer server) {
    ServerLevel level = server.overworld();

      return level.getDataStorage().computeIfAbsent(
          new SavedData.Factory<PlayerDifficultyManager>(
              PlayerDifficultyManager::new,
              (tag, provider) -> new PlayerDifficultyManager(tag), 
              null // or some appropriate DataFixTypes if needed
          ),
          DATA_NAME
      );
  }

  public static void checkAllPlayersDifficulty(MinecraftServer server) {
    String minDiff = DifficultyConfig.SERVER.minPlayerDifficulty.get();
    String maxDiff = DifficultyConfig.SERVER.maxPlayerDifficulty.get();
    PlayerDifficultyManager manager = get(server);
    
    for (String difficultyKey: get(server).difficulties.getAllKeys()) {
      String difficulty = get(server).difficulties.getString(difficultyKey);

      if (!DifficultyCommand.DIFFICULTY_STRINGS.contains(difficulty)) {
        get(server).difficulties.remove(difficulty);
        manager.setDirty();
      }

      try {  
        if(DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty) < DifficultyCommand.DIFFICULTY_STRINGS.indexOf(minDiff)) {
          manager.setPlayerDifficulty(difficultyKey, minDiff);
          manager.setDirty();
        } else if(DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty) > DifficultyCommand.DIFFICULTY_STRINGS.indexOf(maxDiff)) {
          manager.setPlayerDifficulty(difficultyKey, maxDiff);
          manager.setDirty();
        }
      } catch (IllegalArgumentException e) {
        get(server).difficulties.remove(difficultyKey);
      }
    }
  }

  public static void setDifficulty(MinecraftServer server, UUID playerUUID, String difficulty) {
    if (DifficultyConfig.SERVER.perPlayerDifficulty.get()) {
      PlayerDifficultyManager manager = get(server);
      manager.setPlayerDifficulty(playerUUID, difficulty);
      manager.setDirty();
    }
  }

  public static String getDifficulty(MinecraftServer server, UUID playerUUID) {
    if (DifficultyConfig.SERVER.perPlayerDifficulty.get()) {
      PlayerDifficultyManager manager = get(server);
      return manager.getPlayerDifficulty(playerUUID);
    } else {
      return DifficultyConfig.SERVER.serverDifficulty.get(); 
    }
  }

  public static String getDifficulty(MinecraftServer server, Player player) {
    return getDifficulty(server, player.getUUID());
  }

  public static String getDifficultyAtLocaltion(ServerLevel level, int x, int y, int z) {
    AABB areaToCheck = new AABB(
        x - 64,
        y - 64,
        z - 64,
        x + 64,
        y + 64,
        z + 64);

    if (DifficultyConfig.SERVER.perPlayerDifficulty.get()) {
      int maxDiff = -1;

      for (Entity entity : level.getEntities(null, areaToCheck)) {
        
        if (entity instanceof ServerPlayer) {
          String difficulty = getDifficulty(level.getServer(), entity.getUUID());
          int diff = DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty);
          
          if (diff > maxDiff) {
            maxDiff = diff;
          }
        }
      }

      if (maxDiff != -1) {
        return DifficultyCommand.DIFFICULTY_STRINGS.get(maxDiff);
      } else {
        return DifficultyConfig.SERVER.serverDifficulty.get(); 
      }
    }

    return DifficultyConfig.SERVER.serverDifficulty.get(); 
  }


  public static boolean isDifficultyOver(MinecraftServer server, Player player, String difficulty) {
    return DifficultyCommand.DIFFICULTY_STRINGS.indexOf(getDifficulty(server,  player.getUUID())) >= DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty);
  }

  public static String getDifficultyAtLocaltion(ServerLevel level, BlockPos pos) {
    return getDifficultyAtLocaltion(level, pos.getX(), pos.getY(), pos.getZ());
  }

  public static String getDifficultyAtLocaltion(ServerLevel level, Entity entity) {
    return getDifficultyAtLocaltion(level, entity.blockPosition());
  }

  public static boolean isDifficultyAtLocationOver(ServerLevel level, int x, int y, int z, String difficulty) {
    return DifficultyCommand.DIFFICULTY_STRINGS.indexOf(getDifficultyAtLocaltion(level, x, y, z)) >= DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty);
  }

  public static boolean isDifficultyAtLocationOver(ServerLevel level, BlockPos pos, String difficulty) {
    return isDifficultyAtLocationOver(level, pos.getX(), pos.getY(), pos.getZ(), difficulty);
  }

  public static boolean isDifficultyAtLocationOver(ServerLevel level, Entity entity, String difficulty) {
    return isDifficultyAtLocationOver(level, entity.blockPosition(), difficulty);
  }
}