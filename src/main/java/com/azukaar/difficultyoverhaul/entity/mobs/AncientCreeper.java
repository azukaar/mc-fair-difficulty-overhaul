package com.azukaar.difficultyoverhaul.entity.mobs;

import com.azukaar.difficultyoverhaul.ModEntityRegistry;
import com.azukaar.difficultyoverhaul.entity.ai.InventoryBreakerGoal;
import com.azukaar.difficultyoverhaul.entity.ai.KamikazeGoal;
import com.azukaar.difficultyoverhaul.entity.ai.SmellerGoal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AncientCreeper extends Creeper {
  Boolean isKaimaze = false;

  // Create a new ResourceLocation for the loot table
  public AncientCreeper(EntityType<? extends Creeper> type, Level world) {
    super(type, world);
  }

	public static AttributeSupplier.Builder registerAttributes() {
		return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0)
                .add(Attributes.FOLLOW_RANGE, 50.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35);
	}

  // on tick
  @Override
  public void aiStep() {
    super.aiStep();
    if(isPowered() && !isKaimaze) {
      this.goalSelector.addGoal(2, new KamikazeGoal(this));
      this.targetSelector.addGoal(4, new SmellerGoal(this));
      
      isKaimaze = true;
    }
  }

  @Override
  protected void registerGoals() {
      super.registerGoals();
      this.targetSelector.addGoal(3, new InventoryBreakerGoal(this));
  }

  public static AncientCreeper fromCreeper(Creeper creeper) {
    CompoundTag originalNBT = creeper.saveWithoutId(new CompoundTag());
    originalNBT.remove("UUID");
    
    AncientCreeper ac = creeper.convertTo(ModEntityRegistry.ANCIENT_CREEPER.get(), true);
    
    ac.load(originalNBT);
    
    EventHooks.onLivingConvert(creeper, ac);
    return ac;
  }
}
