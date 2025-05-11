package com.azukaar.difficultyoverhaul.entity.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

public class KamikazeGoal extends Goal {
  private Creeper monster;
  private Level level;
  private int timer = 0;
  
  public KamikazeGoal(Creeper monster) {
    this.monster = monster;
    this.level = monster.level();
  }
  
  @Override
  public boolean canUse() {
    if(this.monster.getTarget() == null) {
      timer = 0;
      return false;
    }

    return true;
  }

  @Override
  public void start() {
    this.timer = 0;
  }

  @Override
  public void tick() {
    if(this.monster.getTarget() == null) {
      return;
    }

    if(this.timer >= 100) {
      this.monster.ignite();
    }

    this.timer++;
  }
}
