package com.azukaar.difficultyoverhaul.entity.ai;

import com.azukaar.difficultyoverhaul.difficulty.PlayerDifficultyManager;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class SmellerGoal extends Goal {
  private Monster monster;
  private Level level;
  private Float radius;
  
  public SmellerGoal(Monster monster) {
    this.monster = monster;
    this.level = monster.level();
    this.radius = 12.0F;
  }

  public SmellerGoal(Monster monster, Float radius) {
    this.monster = monster;
    this.level = monster.level();
    this.radius = radius;
  }
  
  @Override
  public boolean canUse() {
    if(this.monster.getTarget() != null) {
      return false;
    }

    return true;
  }

  protected AABB getTargetSearchArea(double pTargetDistance) {
    return this.monster.getBoundingBox().inflate(pTargetDistance, pTargetDistance / 2, pTargetDistance);
  }


  @Override
  public void tick() {
    if(this.monster.getTarget() != null) {
      return;
    }
    
    AABB searchArea = getTargetSearchArea(this.radius);
    for(Player entity : this.level.getEntitiesOfClass(Player.class, searchArea)) {
      if(entity != null && !entity.isSpectator() && !entity.isCreative() && PlayerDifficultyManager.isDifficultyOver(entity.getServer(), entity, "expert")) {
        this.monster.setTarget(entity);
        return;
      }
    }
  }
}
