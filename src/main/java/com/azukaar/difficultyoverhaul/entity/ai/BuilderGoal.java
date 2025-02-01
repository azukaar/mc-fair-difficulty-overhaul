package com.azukaar.difficultyoverhaul.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public class BuilderGoal extends Goal {
  private Zombie monster;
  private Level level;
  private int timer = 0;
  private int patternTick = 0;
  
  public BuilderGoal(Zombie monster) {
    this.monster = monster;
    this.level = monster.level();
  }
  
  @Override
  public boolean canUse() {
    if(this.monster.getTarget() == null) {
      timer = 0;
      patternTick = 0;
      return false;
    }

    if(this.monster.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
      timer = 0;
      patternTick = 0;
      return false;
    }

    return true;
  }

  @Override
  public void start() {
    this.timer = 0;
    this.patternTick = 0;
  }

  public Block exhaustBlock() {
    ItemStack stack = this.monster.getItemBySlot(EquipmentSlot.MAINHAND);
    int nbItem = stack.getCount();
    Item item = stack.getItem();
        
    if(item instanceof BlockItem) { 
      if(nbItem == 1) {
        this.monster.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
      } else {
        stack.shrink(1);
      }

      return ((BlockItem) item).getBlock();
    } else {
      return null;
    }
  }

  BlockPos tempTarget;
  public void moveToTemp(BlockPos pos) {
    this.monster.getNavigation().moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0);
    this.tempTarget = pos;
  }

  @Override
  public void tick() {
    if(this.monster.getTarget() == null) {
      return;
    }

    if(this.monster.getItemBySlot(EquipmentSlot.MAINHAND).isEmpty()) {
      return;
    }
    
    BlockPos nextBlockOfInterest = this.monster.getTarget().blockPosition();

    double distanceToInv = this.monster.distanceToSqr((double) nextBlockOfInterest.getX() + 0.5,
        (double) nextBlockOfInterest.getY() + 0.5, (double) nextBlockOfInterest.getZ() + 0.5);
    
    if (distanceToInv > 2 * 2) {

      if(this.timer >= 20 || (tempTarget != null && this.monster.blockPosition().distSqr(this.tempTarget) < 2)) {
          tempTarget = null;
          Vec3 point1 = new Vec3(this.monster.blockPosition().getX(), this.monster.blockPosition().getY(),
              this.monster.blockPosition().getZ());

          Vec3 point2 = new Vec3(nextBlockOfInterest.getX(), nextBlockOfInterest.getY(), nextBlockOfInterest.getZ());

          Vec3 vector = point2.subtract(point1);
          
          Direction directionToTarget = Direction.getNearest(vector.x, vector.y, vector.z);

          if (directionToTarget.getStepX() == 0 && directionToTarget.getStepZ() == 0) {
            directionToTarget = this.monster.getDirection();
          }

          BlockPos nextPos = this.monster.blockPosition().offset(directionToTarget.getStepX(), 0,
              directionToTarget.getStepZ());

        BlockPos nextPosBelow = nextPos.below();

        Boolean isTargetAbove = nextBlockOfInterest.getY() > this.monster.blockPosition().getY() + 1;

        // if below is air, place cobblestone
        if(isTargetAbove) {
          if(this.level.getBlockState(nextPos).isAir()) {
            Block toUse = exhaustBlock();
            if(toUse != null) {
              this.level.setBlock(nextPos, toUse.defaultBlockState(), 3);
              moveToTemp(nextPos);
            }
          } else {
            Boolean isDone = false;
            for(int dx = -1; dx <= 1; dx++) {
              for(int dz = -1; dz <= 1; dz++) {
                if (isDone) {
                  break;
                }
                if(dx == 0 && dz == 0 ||
                  dx == 1 && dz == 1 ||
                  dx == -1 && dz == -1 ||
                  dx == 1 && dz == -1 ||
                  dx == -1 && dz == 1) {
                  continue;
                }
                BlockPos pos = this.monster.blockPosition().offset(dx, 0, dz);
                BlockPos pos2 = this.monster.blockPosition().offset(dx*2, 1, dz*2);

                if(this.level.getBlockState(pos).isAir() && this.level.getBlockState(pos2).isAir()) {
                  Block toUse = exhaustBlock();
                  if(toUse != null) {
                    this.level.setBlock(pos, toUse.defaultBlockState(), 3);
                    moveToTemp(pos);
                  }
                  toUse = exhaustBlock();
                  if(toUse != null) {
                    this.level.setBlock(pos2, toUse.defaultBlockState(), 3);
                    moveToTemp(pos2);
                  }
                  isDone = true;
                }
              }
            }
          }
        } else {
          if(this.level.getBlockState(nextPosBelow).isAir()) {
            Block toUse = exhaustBlock();
            if(toUse != null) {
              this.level.setBlock(nextPosBelow, toUse.defaultBlockState(), 3);
              moveToTemp(nextPosBelow);
            }
          }
        }

        this.timer = 0;
      }
  
      this.timer++;
    }

  }
}
