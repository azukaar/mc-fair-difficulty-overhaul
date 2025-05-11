package com.azukaar.difficultyoverhaul.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class MinerGoal extends Goal {
  private Zombie monster;
  private Level level;
  private int timer = 0;

  private BlockPos breakingBlock = null;
  private int breakingBlockTicks = 0;
  private int lastBreakProgress = -1;

  public MinerGoal(Zombie monster) {
    this.monster = monster;
    this.level = monster.level();
  }

  @Override
  public boolean canUse() {
    if (this.monster.getTarget() == null) {
      timer = 0;
      breakingBlock = null;
      breakingBlockTicks = 0;
      lastBreakProgress = -1;
      return false;
    }

    return true;
  }

  @Override
  public void start() {
    this.timer = 0;
    breakingBlock = null;
    breakingBlockTicks = 0;
    lastBreakProgress = -1;
  }

  public float getBlockHardness(BlockPos pos) {
    BlockState blockState = level.getBlockState(pos);
    return blockState.getDestroySpeed(level, pos); // This method returns the hardness.
  }

  public boolean isValidTarget(BlockPos pos) {
    BlockState blockState = this.level.getBlockState(pos);

    if (blockState.isAir()) {
      return false;
    }

    if (blockState.getDestroySpeed(level, pos) == -1.0F) {
      return false;
    }

    if (!blockState.requiresCorrectToolForDrops()) {
      return true;
    }

    if (blockState.is(BlockTags.MINEABLE_WITH_PICKAXE)
        && Items.DIAMOND_PICKAXE.isCorrectToolForDrops(new ItemStack(Items.DIAMOND_PICKAXE), blockState)) {
      return true;
    }

    if (blockState.is(BlockTags.MINEABLE_WITH_AXE)
        && Items.DIAMOND_AXE.isCorrectToolForDrops(new ItemStack(Items.DIAMOND_AXE), blockState)) {
      return true;
    }

    if (blockState.is(BlockTags.MINEABLE_WITH_SHOVEL)
        && Items.DIAMOND_SHOVEL.isCorrectToolForDrops(new ItemStack(Items.DIAMOND_SHOVEL), blockState)) {
      return true;
    }

    return false;
  }

  @Override
  public void tick() {
    if (this.monster.getTarget() == null) {
      return;
    }

    if (breakingBlock != null) {
      breakingBlockTicks++;

      if (!isValidTarget(breakingBlock)) {
        breakingBlock = null;
        breakingBlockTicks = 0;
        lastBreakProgress = -1;
        return;
      }

      double distanceToBlock = this.monster.distanceToSqr((double) breakingBlock.getX() + 0.5,
          (double) breakingBlock.getY() + 0.5, (double) breakingBlock.getZ() + 0.5);

      if (distanceToBlock > 3 * 3) {
        breakingBlock = null;
        breakingBlockTicks = 0;
        lastBreakProgress = -1;
        return;
      }

      // Trigger the breaking process sound
      if (this.monster.getRandom().nextInt(12) == 0) {
        BlockState blockState = this.monster.level().getBlockState(breakingBlock);
        int stateId = net.minecraft.world.level.block.Block.getId(blockState);
        this.monster.level().levelEvent(2001, breakingBlock, stateId);
        if (!this.monster.swinging) {
          this.monster.swing(this.monster.getUsedItemHand());
        }
      }

      float blockHardness = getBlockHardness(breakingBlock) * 25.0f;
      int i = (int) ((float) this.breakingBlockTicks / blockHardness * 10.0F);

      if (i != this.lastBreakProgress) {
        this.monster.level().destroyBlockProgress(this.monster.getId(), breakingBlock, i);
        this.lastBreakProgress = i;
      }

      if (breakingBlockTicks < blockHardness) {
        return;
      }

      this.monster.level().destroyBlock(breakingBlock, true, this.monster);
      this.monster.level().levelEvent(1021, breakingBlock, 0); // Sound for block breaking completion
      this.monster.level().levelEvent(2001, breakingBlock,
          Block.getId(this.monster.level().getBlockState(breakingBlock))); // Particle effects for block breaking

      breakingBlock = null;
      breakingBlockTicks = 0;
      lastBreakProgress = -1;

      return;
    } else {
      BlockPos nextBlockOfInterest = this.monster.getTarget().blockPosition();

      double distanceToInv = this.monster.distanceToSqr((double) nextBlockOfInterest.getX() + 0.5,
          (double) nextBlockOfInterest.getY() + 0.5, (double) nextBlockOfInterest.getZ() + 0.5);

      if (distanceToInv > 2 * 2) {
        if (this.timer >= 10) {
          Boolean isTargetAbove = nextBlockOfInterest.getY() > this.monster.blockPosition().getY() + 1;
          Boolean isTargetBelow = nextBlockOfInterest.getY() < this.monster.blockPosition().getY() - 1;

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

          Boolean done = false;
          
          if (!isTargetBelow) {
            for (int dy = 1; dy >= 0; dy--) {
              BlockPos pos = nextPos.offset(0, dy, 0);
              if (done)
                break;
              if (isTargetAbove)
                pos = pos.above();
              if (isValidTarget(pos)) {
                breakingBlock = pos;
                done = true;
              }
            }
          } else {
            for (int dy = 0; dy <= 1; dy++) {
              BlockPos pos = nextPos.offset(0, dy, 0);
              if (done)
                break;
              pos = pos.below();
              if (isValidTarget(pos)) {
                breakingBlock = pos;
                done = true;
              }
            }
          }

          if (!done) {
            BlockPos below = this.monster.blockPosition().below();
            if (isValidTarget(below)) {
              breakingBlock = below;
            }
          }

          this.timer = 0;
        }

        this.timer++;
      }
    }
  }
}
