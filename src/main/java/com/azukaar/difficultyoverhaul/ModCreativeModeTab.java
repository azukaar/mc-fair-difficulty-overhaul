package com.azukaar.difficultyoverhaul;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;

public class ModCreativeModeTab extends CreativeModeTab {
  protected ModCreativeModeTab(Builder builder) {
    super(builder);
    
  }

  @Override
  public ResourceLocation getBackgroundTexture() {
    return ResourceLocation.fromNamespaceAndPath(DifficultyOverhaul.MODID, "textures/item/raised_zombie_spawn_egg.png");
  }
}