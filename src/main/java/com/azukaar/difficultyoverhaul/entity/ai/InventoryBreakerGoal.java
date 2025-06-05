package com.azukaar.difficultyoverhaul.entity.ai;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class InventoryBreakerGoal extends Goal {
  private Monster monster;
  private Level level;
  private static final double CHECK_RADIUS = 16.0; // Radius in blocks to check for players/chests
  private BlockPos nextBlockOfInterest = null;
  private int breakingTime;
  private int lastBreakProgress = -1;
  private List<BlockPos> blackList = new java.util.ArrayList<BlockPos>();

  // Caching variables
  private boolean cachedScanResult = false;
  private long lastScanTime = 0;
  private BlockPos lastScanPosition = null;
  private static final long CACHE_DURATION_TICKS = 400; // 20 seconds at 20 TPS
  private static final double RESCAN_DISTANCE_THRESHOLD = 16.0; // 16 blocks

  // Multi-frame scanning variables
  private List<BlockPos> blocksToProcess = new java.util.ArrayList<BlockPos>();
  private boolean needsRescanAfterProcessing = false;

  public InventoryBreakerGoal(Monster monster) {
    this.monster = monster;
    this.level = monster.level();
  }

  @Override
  public boolean canUse() {
    // don't break blocks if there are players nearby (kill them first)
    if (!noPlayersNearby()) {
      return false;
    }

    // don't break blocks if the monster is in a minecart
    if (this.monster.isPassenger()) {
      return false;
    }

    if (nextBlockOfInterest != null) {
      // Only validate the current target if we need to rescan
      if (needsRescan()) {
        boolean hasPath = hasPathToInventory(nextBlockOfInterest);
        boolean isOfInterest = isBlockOfInterest(nextBlockOfInterest);

        if (!hasPath || !isOfInterest) {
          nextBlockOfInterest = null;
          invalidateCache(); // Clear cache when current target becomes invalid
          return false;
        }
        
        // Update cache info since we just validated
        lastScanTime = this.level.getGameTime();
        lastScanPosition = this.monster.blockPosition();
      }

      return true; // Target is valid (either recently validated or assumed valid)
    }

    return findNearbyInventoryWithCache();
  }

  /**
   * Invalidate the cache when conditions change
   */
  private void invalidateCache() {
    cachedScanResult = false;
    lastScanTime = 0;
    lastScanPosition = null;
    blocksToProcess.clear();
    needsRescanAfterProcessing = false;
  }

  /**
   * Check if we need to rescan based on time and distance conditions
   */
  private boolean needsRescan() {
    long currentTime = this.level.getGameTime();
    BlockPos currentPosition = this.monster.blockPosition();
    
    // Check if 10 seconds have passed
    if (currentTime - lastScanTime >= CACHE_DURATION_TICKS) {
      return true;
    }
    
    // Check if entity moved more than 16 blocks from last scan position
    if (lastScanPosition != null) {
      double distanceMoved = Math.sqrt(currentPosition.distSqr(lastScanPosition));
      if (distanceMoved > RESCAN_DISTANCE_THRESHOLD) {
        return true;
      }
    }
    
    return false;
  }
  
  static int SCANNED_THIS_TICK = 0; // Static counter to limit processing per tick
  static final int MAX_SCANS_PER_TICK = 2; // Limit to 1 scan per tick to avoid performance issues

  static public void onServerTick() {
    SCANNED_THIS_TICK = 0; // Reset the counter for the next tick
  }

  /**
   * Find nearby inventory with multi-frame processing and caching logic
   */
  private boolean findNearbyInventoryWithCache() {
    // If we're currently processing blocks, process one per tick
    if (!blocksToProcess.isEmpty()) {
      if (this.monster.getRandom().nextInt(2) != 0) {
        if (SCANNED_THIS_TICK >= MAX_SCANS_PER_TICK) {
          return false; // Skip this tick to allow other processing
        }
        SCANNED_THIS_TICK++;
        return  processNextBlock();
      }
      return false; // Skip this tick to allow other processing
    }
    
    // If we finished processing and need to rescan, start a new scan
    if (needsRescanAfterProcessing) {
      needsRescanAfterProcessing = false;
      return startNewScan();
    }
    
    // If we don't need to rescan, return cached result
    if (!needsRescan() && lastScanTime > 0) {
      return cachedScanResult;
    }
    
    // Start a new scan
    return startNewScan();
  }

  /**
   * Start a new scan by finding all blocks of interest (fast operation)
   */
  private boolean startNewScan() {
    blocksToProcess.clear();
    
    BlockPos pos = this.monster.blockPosition();
    int x0 = (int) (pos.getX() - CHECK_RADIUS);
    int y0 = (int) (pos.getY() - CHECK_RADIUS);
    int z0 = (int) (pos.getZ() - CHECK_RADIUS);
    int x1 = (int) (pos.getX() + CHECK_RADIUS);
    int y1 = (int) (pos.getY() + CHECK_RADIUS);
    int z1 = (int) (pos.getZ() + CHECK_RADIUS);

    // Find all blocks of interest (cheap operation)
    for (int x = x0; x <= x1; x++) {
      for (int y = y0; y <= y1; y++) {
        for (int z = z0; z <= z1; z++) {
          BlockPos blockPos = new BlockPos(x, y, z);
          if (isBlockOfInterest(blockPos)) {
            blocksToProcess.add(blockPos);
          }
        }
      }
    }
    
    // Update scan info
    lastScanTime = this.level.getGameTime();
    lastScanPosition = this.monster.blockPosition();
    
    // If no blocks found, cache the result and return
    if (blocksToProcess.isEmpty()) {
      cachedScanResult = false;
      return false;
    }
    
    return false;
  }

  /**
   * Process one block from the list (expensive operation spread over multiple ticks)
   */
  private boolean processNextBlock() {
    // Check if we need to rescan while processing
    if (needsRescan()) {
      needsRescanAfterProcessing = true;
    }
    
    if (blocksToProcess.isEmpty()) {
      // Finished processing all blocks, cache the result
      cachedScanResult = false;
      return false;
    }
    
    BlockPos blockPos = blocksToProcess.remove(0);
    
    // Do the expensive checks for this block
    boolean hasPath = hasPathToInventory(blockPos);
    boolean canSee = canSeeInventory(blockPos);
    
    if (hasPath && canSee) {
      // Found a valid target!
      setFakeTarget(blockPos);
      breakingTime = 0;
      lastBreakProgress = -1;
      cachedScanResult = true;
      blocksToProcess.clear(); // Clear remaining blocks since we found a target
      needsRescanAfterProcessing = false;
      return true;
    }
    
    // Continue processing next tick
    return !blocksToProcess.isEmpty();
  }

  private boolean isBlockOfInterest(BlockPos pos) {
    BlockState blockState = level.getBlockState(pos);
    BlockEntity blockEntity = level.getBlockEntity(pos);
    boolean isContainer = blockEntity instanceof Container;
    boolean isLightBlock = blockState.getLightEmission() > 0;
    boolean isBlackListed = blackList.contains(pos);

    if (isBlackListed) {
      return false;
    }

    // filter out transparent blocks
    if (isLightBlock && !(blockState.getBlock() instanceof LightBlock)
        && !(blockState.getBlock() instanceof TorchBlock)) {
      isLightBlock = false;
    }

    boolean blockImportant = isContainer || isLightBlock;

    if (isContainer && blockEntity instanceof RandomizableContainerBlockEntity) {
      RandomizableContainerBlockEntity chest = (RandomizableContainerBlockEntity) blockEntity;
      if (chest.getLootTable() != null) {
        return false;
      }
    }

    if (blockImportant) {
      if (monster instanceof Creeper) {
        // don't explode small blocks like torches
        if (getBlockHardness(pos) < 0.5) {
          return false;
        }

        // don't explode blocks that can resist the explosion
        if (canResistExplosion(pos, 3.0f)) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  private boolean hasPathToInventory(BlockPos targetPos) {
    if(this.monster instanceof Creeper) {
      Creeper creeper = (Creeper) this.monster;
      if(creeper.isPowered()) {
        return true;
      }
    }
    
    PathNavigation navigation = this.monster.getNavigation();

    if(this.monster instanceof Creeper) {
      navigation = ((Creeper) this.monster).getNavigation();
    } else if(this.monster instanceof Zombie) {
      navigation = ((Zombie) this.monster).getNavigation();
    }
    
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        for (int dy = -1; dy <= 1; dy++) {
          BlockPos checkPos = targetPos.offset(dx, dy, dz);
          Path path = navigation.createPath(checkPos, 0);
          if (path != null && path.canReach()) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private boolean _canSeeInventory(BlockPos targetPos) {
    Vec3 zombieEyePosition = new Vec3(this.monster.getX(), this.monster.getEyeY(), this.monster.getZ());
    Vec3 directionToTarget = new Vec3(targetPos.getX() + 0.5 - this.monster.getX(),
        targetPos.getY() + 0.5 - this.monster.getEyeY(), targetPos.getZ() + 0.5 - this.monster.getZ());
    Vec3 reducedTargetPosition = zombieEyePosition.add(directionToTarget.scale(0.9)); // Scale the vector to 90% of its length

    // if distance is < 2 blocks, we can see it
    if (directionToTarget.length() <= 2) {
      return true;
    }

    // if distance is > 30 blocks, we can't see it
    if (directionToTarget.length() > 30) {
      return false;
    }

    return this.level.clip(new ClipContext(zombieEyePosition, reducedTargetPosition, ClipContext.Block.COLLIDER,
        ClipContext.Fluid.NONE, this.monster)).getType() == HitResult.Type.MISS;
  }

  private boolean canSeeInventory(BlockPos targetPos) {
    if(this.monster instanceof Creeper) {
      Creeper creeper = (Creeper) this.monster;
      if(creeper.isPowered()) {
        return true;
      }
    }

    // for blocks around the target, check if the zombie can see them
    for (int dx = -1; dx <= 1; dx++) {
      for (int dz = -1; dz <= 1; dz++) {
        for (int dy = -1; dy <= 1; dy++) {
          BlockPos checkPos = targetPos.offset(dx, dy, dz);
          if (_canSeeInventory(checkPos)) {
            return true;
          }
        }
      }
    }
    return false;
  }



  public boolean canResistExplosion(BlockPos pos, float explosionStrength) {
    BlockState blockState = level.getBlockState(pos);
    float blastResistance = blockState.getBlock().getExplosionResistance();
    // Generally, if the blast resistance is greater than the explosion strength,
    // the block can resist the explosion.
    return blastResistance >= explosionStrength;
  }

  public float getBlockHardness(BlockPos pos) {
    BlockState blockState = level.getBlockState(pos);
    return blockState.getDestroySpeed(level, pos); // This method returns the hardness.
  }

  private boolean noPlayersNearby() {
    AABB areaToCheck = new AABB(
        monster.getX() - CHECK_RADIUS,
        monster.getY() - CHECK_RADIUS,
        monster.getZ() - CHECK_RADIUS,
        monster.getX() + CHECK_RADIUS,
        monster.getY() + CHECK_RADIUS,
        monster.getZ() + CHECK_RADIUS);

    List<Player> players = level.getEntitiesOfClass(Player.class, areaToCheck);

    // filter creative / spect players
    players.removeIf(player -> player.isCreative() || player.isSpectator());

    // filter players with invisibility effect
    players.removeIf(player -> player.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY));

    return players.isEmpty();
  }

  LivingEntity fakeLivingEntity;

  public void setFakeTarget(BlockPos pos) {
    if (pos == null) {
      nextBlockOfInterest = null;
      this.monster.setTarget(null);
      if (fakeLivingEntity != null) {
        fakeLivingEntity.remove(RemovalReason.DISCARDED);
        fakeLivingEntity = null;
      }
      return;
    }

    nextBlockOfInterest = pos;
    fakeLivingEntity = new Villager(EntityType.VILLAGER, level);
    fakeLivingEntity.setSilent(true);
    fakeLivingEntity.setPos(nextBlockOfInterest.getX() + 0.5, nextBlockOfInterest.getY(), nextBlockOfInterest.getZ() + 0.5);
    this.monster.setTarget(fakeLivingEntity);
  }

  @Override
  public void tick() {
    super.tick();
    if (nextBlockOfInterest == null) {
      return;
    }

    double distanceToInv = this.monster.distanceToSqr((double) nextBlockOfInterest.getX() + 0.5,
        (double) nextBlockOfInterest.getY() + 0.5, (double) nextBlockOfInterest.getZ() + 0.5);

    // Move towards the chest if not close enough
    if (distanceToInv > 3 * 3) { // Check if more than 2 blocks away
      // this.monster.getNavigation().moveTo(nextBlockOfInterest.getX() + 0.5, nextBlockOfInterest.getY(),
      //     nextBlockOfInterest.getZ() + 0.5, 1.0);
    } else {
      // Stop moving when close enough
      this.monster.getNavigation().stop();

      // if creeper, explode
      if (this.monster instanceof Creeper) {
        Creeper creeper = (Creeper) this.monster;
        if (this.monster.isInWater()) {
          blackList.add(nextBlockOfInterest);
          setFakeTarget(null);
          return;
        }
        creeper.ignite();
        return;
      }

      // Trigger the breaking process
      if (this.monster.getRandom().nextInt(12) == 0) {
        this.monster.level().levelEvent(1019, nextBlockOfInterest, 0); // Sound for block breaking start
        if (!this.monster.swinging) {
          this.monster.swing(this.monster.getUsedItemHand());
        }
      }

      breakingTime++;
      float blockHardness = getBlockHardness(nextBlockOfInterest) * 50.0f;
      int i = (int) ((float) this.breakingTime / blockHardness * 10.0F);
      if (i != this.lastBreakProgress) {
        this.monster.level().destroyBlockProgress(this.monster.getId(), nextBlockOfInterest, i);
        this.lastBreakProgress = i;
      }

      if (breakingTime < blockHardness) {
        return;
      }

      // Simulate the breaking of the chest
      this.monster.level().destroyBlock(nextBlockOfInterest, true, this.monster);
      this.monster.level().levelEvent(1021, nextBlockOfInterest, 0); // Sound for block breaking completion
      this.monster.level().levelEvent(2001, nextBlockOfInterest,
          Block.getId(this.monster.level().getBlockState(nextBlockOfInterest))); // Particle effects for block breaking

      // Reset closestInventory to prevent re-breaking
      blackList.add(nextBlockOfInterest);
      setFakeTarget(null);
      invalidateCache(); // Invalidate cache when a block is broken
    }
  }
}