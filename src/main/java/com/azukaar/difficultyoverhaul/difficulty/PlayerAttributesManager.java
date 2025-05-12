package com.azukaar.difficultyoverhaul.difficulty;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.saveddata.SavedData;

public class PlayerAttributesManager extends SavedData {
  private static final String DATA_NAME = "afdo_player_attributes";
  private final CompoundTag maxHealth;
  public static final ResourceLocation maxHealthAttr = ResourceLocation
      .fromNamespaceAndPath("azukaarsfairdifficultyoverhaul", "max_health_modifier");

  public PlayerAttributesManager() {
    this.maxHealth = new CompoundTag();
  }

  public PlayerAttributesManager(CompoundTag nbt) {
    this.maxHealth = nbt.getCompound("max_health_modifier");
  }

  @Override
  public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
    nbt.put("max_health_modifier", maxHealth);
    return nbt;
  }

  public void setPlayerRawMaxHealth(String playerUUID, double penalty) {
    maxHealth.putDouble(playerUUID, penalty);
    setDirty();
  }

  public void setPlayerRawMaxHealth(UUID playerUUID, double penalty) {
    setPlayerRawMaxHealth(playerUUID.toString(), penalty);
  }

  public void setPlayerMaxHealth(Player player, double newMaxHealth) {
    // if the resulting max health is less than min, set it to min
    double newHealth = player.getMaxHealth() + newMaxHealth;
    int min = DifficultyConfig.SERVER.getHealthDeathPenaltyMinimum();
    if (newHealth < min) {
      newMaxHealth = -(player.getMaxHealth() - min);

      if (DifficultyConfig.SERVER.isSoftHardcoreEnabled()) {
        // set player to spectate mode
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.setGameMode(GameType.SPECTATOR);

            // send message to player
            Component message = Component.translatable("messages.azukaarsfairdifficultyoverhaul.softhardcore")
                    .withStyle(ChatFormatting.DARK_PURPLE);

            player.sendSystemMessage(message);
        }
      }
    }

    AttributeInstance instance = player.getAttribute(Attributes.MAX_HEALTH);
    if (instance != null) {
      // if the player has a max health modifier, remove it
      AttributeModifier existingModifier = instance.getModifier(maxHealthAttr);
      if (existingModifier != null) {
        instance.removeModifier(existingModifier);
      }

      instance.addPermanentModifier(new AttributeModifier(
          maxHealthAttr,
          newMaxHealth,
          AttributeModifier.Operation.ADD_VALUE));
    }

    setPlayerRawMaxHealth(player.getUUID(), newMaxHealth);
  }

  public void addPlayerMaxHealth(Player player, double difference) {
    double current = getPlayerMaxHealth(player.getUUID());
    double newMaxHealth = current + difference;
    setPlayerMaxHealth(player, newMaxHealth);
  }

  public double getPlayerMaxHealth(UUID playerUUID) {
    if (maxHealth.contains(playerUUID.toString())) {
      return maxHealth.getDouble(playerUUID.toString());
    }
    return 0.0;
  }

  public static PlayerAttributesManager get(MinecraftServer server) {
    ServerLevel level = server.overworld();

    return level.getDataStorage().computeIfAbsent(
        new SavedData.Factory<PlayerAttributesManager>(
            PlayerAttributesManager::new,
            (tag, provider) -> new PlayerAttributesManager(tag),
            null // or some appropriate DataFixTypes if needed
        ),
        DATA_NAME);
  }
}
