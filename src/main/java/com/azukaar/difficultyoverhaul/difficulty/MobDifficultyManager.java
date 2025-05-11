package com.azukaar.difficultyoverhaul.difficulty;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public class MobDifficultyManager {
  public static boolean canMobSpawn(ServerLevel serverLevel, Entity entity) {
    String difficulty = PlayerDifficultyManager.getDifficultyAtLocaltion(serverLevel, entity);

    
    String entityName = entity.getType().toString().replace("entity.", "");
    
    String mobDifficulty = DifficultyConfig.SERVER.getMobDifficulty(entityName);

    if(DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty) >= DifficultyCommand.DIFFICULTY_STRINGS.indexOf(mobDifficulty)) {
      return true;
    }

    return false;
  }
}
