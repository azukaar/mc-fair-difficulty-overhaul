package com.azukaar.difficultyoverhaul.entity.ai;

import com.azukaar.difficultyoverhaul.difficulty.PlayerDifficultyManager;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class ComGoal extends Goal {
  private Monster monster;
  private Level level;
  private Float radius;
  
  public ComGoal(Monster monster) {
    this.monster = monster;
    this.level = monster.level();
    this.radius = 12.0F;
  }
  
  @Override
  public boolean canUse() {
    if(this.monster.getTarget() != null) {
      return true;
    }

    return false;
  }

  protected AABB getTargetSearchArea(double pTargetDistance) {
    return this.monster.getBoundingBox().inflate(pTargetDistance, pTargetDistance / 2, pTargetDistance);
  }

  @Override
  public void tick() {
    if(this.monster.getTarget() == null) {
      return;
    }
    
    AABB searchArea = getTargetSearchArea(this.radius);
    for(Monster entity : this.level.getEntitiesOfClass(Monster.class, searchArea)) {
      if(entity != null && entity.getTarget() == null) {
        // System.out.println("Monster found a target: " + entity + " at " + entity.position());
        entity.setTarget(this.monster.getTarget());
        return;
      }
    }
  }
}
