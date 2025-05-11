package com.azukaar.difficultyoverhaul.entity.mobs;

import com.azukaar.difficultyoverhaul.ModEntityRegistry;
import com.azukaar.difficultyoverhaul.entity.ai.BuilderGoal;
import com.azukaar.difficultyoverhaul.entity.ai.ComGoal;
import com.azukaar.difficultyoverhaul.entity.ai.InventoryBreakerGoal;
import com.azukaar.difficultyoverhaul.entity.ai.MinerGoal;
import com.azukaar.difficultyoverhaul.entity.ai.SmellerGoal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;

public class RaisedZombie extends Zombie {
  public Boolean hasRole = false;

  // Create a new ResourceLocation for the loot table
  public RaisedZombie(EntityType<? extends Zombie> type, Level world) {
    super(type, world);
  }

  // Override the isSunSensitive method to make the zombie not burn in the sun
  @Override
  protected boolean isSunSensitive() {
    return false;
  }
  
	public static AttributeSupplier.Builder registerAttributes() {
		return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.ARMOR, 3.0)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.1);
	}

  Goal currentRole = null;
  @Override
  public void aiStep() {
    super.aiStep();
      Item item =  this.getMainHandItem().getItem();
      boolean isBlock = item instanceof BlockItem;
      boolean isTool = item instanceof PickaxeItem || item instanceof ShovelItem || item instanceof AxeItem;
      boolean isSpyglass = item instanceof SpyglassItem;
      
      if(currentRole == null) {
        if(isBlock) {
          currentRole = new BuilderGoal(this);
          this.goalSelector.addGoal(2, currentRole);
          hasRole = true;
        } else if (isTool) {
          currentRole = new MinerGoal(this);
          this.goalSelector.addGoal(2, currentRole);
          hasRole = true;
        } else if(isSpyglass) {
          this.goalSelector.addGoal(2, new SmellerGoal(this, 64.0F));
          this.goalSelector.addGoal(2, new ComGoal(this));
          hasRole = true;
        }
      } else if(!isBlock && !isTool) {
        this.goalSelector.removeGoal(currentRole);
      }
  }

  @Override
  protected void registerGoals() {
      super.registerGoals();
      this.targetSelector.addGoal(3, new InventoryBreakerGoal(this));
      this.targetSelector.addGoal(4, new SmellerGoal(this));
  }

  public static RaisedZombie fromZombie(Zombie zombie) {
    RaisedZombie rz = zombie.convertTo(ModEntityRegistry.RAISED_ZOMBIE.get(), true);
    rz.handleAttributes(rz.level().getCurrentDifficultyAt(rz.blockPosition()).getSpecialMultiplier());
    rz.setCanBreakDoors(rz.supportsBreakDoorGoal() && zombie.canBreakDoors());
    EventHooks.onLivingConvert(zombie, rz);
    rz.setCanPickUpLoot(true);
    return rz;
  }
}
