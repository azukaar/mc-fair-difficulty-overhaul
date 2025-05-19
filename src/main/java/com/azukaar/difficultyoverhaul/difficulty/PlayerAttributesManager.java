package com.azukaar.difficultyoverhaul.difficulty;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.ChatFormatting;
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
  private final CompoundTag luck; // New CompoundTag for luck
  public static final ResourceLocation maxHealthAttr = new ResourceLocation("azukaarsfairdifficultyoverhaul", "max_health_modifier");
  public static final ResourceLocation luckAttr = new ResourceLocation("azukaarsfairdifficultyoverhaul", "luck_modifier");

  public UUID getUUID(ResourceLocation resourceLocation) {
      String resLocString = resourceLocation.toString();
      return UUID.nameUUIDFromBytes(resLocString.getBytes(StandardCharsets.UTF_8));
  }

  public PlayerAttributesManager() {
    this.maxHealth = new CompoundTag();
    this.luck = new CompoundTag(); // Initialize luck CompoundTag
  }

  public PlayerAttributesManager(CompoundTag nbt) {
    this.maxHealth = nbt.getCompound("max_health_modifier");
    this.luck = nbt.contains("luck_modifier") ? nbt.getCompound("luck_modifier") : new CompoundTag(); 
  }

  @Override
  public CompoundTag save(CompoundTag nbt) {
    nbt.put("max_health_modifier", maxHealth);
    nbt.put("luck_modifier", luck); // Save luck data
      return nbt;
  }
   

  // Existing max health methods
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
      AttributeModifier existingModifier = instance.getModifier(getUUID(maxHealthAttr));
      if (existingModifier != null) {
        instance.removeModifier(existingModifier);
      }

      instance.addPermanentModifier(new AttributeModifier(getUUID(maxHealthAttr), "Health Bonus", newMaxHealth, AttributeModifier.Operation.ADDITION));
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

  // New luck methods
  public void setPlayerRawLuck(String playerUUID, double luckBonus) {
    luck.putDouble(playerUUID, luckBonus);
    setDirty();
  }

  public void setPlayerRawLuck(UUID playerUUID, double luckBonus) {
    setPlayerRawLuck(playerUUID.toString(), luckBonus);
  }

  public void setPlayerLuck(Player player, double newLuckBonus) {
    AttributeInstance instance = player.getAttribute(Attributes.LUCK);
    if (instance != null) {
      // if the player has a luck modifier, remove it
      AttributeModifier existingModifier = instance.getModifier(getUUID(luckAttr));
      if (existingModifier != null) {
        instance.removeModifier(existingModifier);
      }

      instance.addPermanentModifier(new AttributeModifier(getUUID(luckAttr), "Luck Bonus", newLuckBonus, AttributeModifier.Operation.ADDITION));
    }

    setPlayerRawLuck(player.getUUID(), newLuckBonus);
  }

  public void addPlayerLuck(Player player, double difference) {
    double current = getPlayerLuck(player.getUUID());
    double newLuckBonus = current + difference;
    setPlayerLuck(player, newLuckBonus);
  }

  public double getPlayerLuck(UUID playerUUID) {
    if (luck.contains(playerUUID.toString())) {
      return luck.getDouble(playerUUID.toString());
    }
    return 0.0;
  }

  public static PlayerAttributesManager get(MinecraftServer server) {
    ServerLevel level = server.overworld();
    return level.getDataStorage().computeIfAbsent(
        (CompoundTag tag) -> new PlayerAttributesManager(tag),
        PlayerAttributesManager::new,
        DATA_NAME
    );
  }

  // set luck for a player
  public void applyLuck(ServerPlayer player) {
    String playerDifficulty = PlayerDifficultyManager.getDifficulty(player.level().getServer(), player.getUUID());
    float luck = DifficultyConfig.SERVER.getLuck(playerDifficulty);
    this.setPlayerLuck(player, luck);
    System.out.println("Luck set to " + luck + " for player " + player.getName().getString());
  }
}
