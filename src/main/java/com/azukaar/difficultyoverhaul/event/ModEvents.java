package com.azukaar.difficultyoverhaul.event;


import com.azukaar.difficultyoverhaul.difficulty.DifficultyParameters;
import com.azukaar.difficultyoverhaul.difficulty.PlayerDifficultyManager;
import com.azukaar.difficultyoverhaul.entity.mobs.AncientCreeper;
import com.azukaar.difficultyoverhaul.entity.mobs.RaisedZombie;

import java.util.HashMap;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.bus.api.SubscribeEvent;

public class ModEvents {
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity =  event.getEntity();
        Level level = event.getLevel();

        if (level instanceof ServerLevel serverLevel) {
            HashMap<Class, Class> evolvedEntities = new HashMap<>();
            evolvedEntities.put(Zombie.class, RaisedZombie.class);
            evolvedEntities.put(Creeper.class, AncientCreeper.class);
        
            if (entity.tickCount == 0) {
                if (entity.getPersistentData().contains("Processed")) {
                    return;
                }

                // Chance to power creeper
                if(entity instanceof Creeper && !((Creeper)entity).isPowered()) {
                    Creeper creeper = (Creeper) entity;
                    int chanceOfPowered = creeper.getRandom().nextInt(100);
                    float poweredChance =  DifficultyParameters.getAddPoweredChances(PlayerDifficultyManager.getDifficultyAtLocaltion(serverLevel, creeper)) * 100;
                    if(chanceOfPowered < poweredChance) {
                        CompoundTag poweredCreeper = creeper.saveWithoutId(new CompoundTag());
                        poweredCreeper.putBoolean("powered", true);
                        creeper.readAdditionalSaveData(poweredCreeper);
                    }
                }

                // Chance to wither skeleton
                if(entity instanceof Skeleton && !(entity instanceof WitherSkeleton)) {
                    Skeleton skeleton = (Skeleton) entity;
                    int chanceOfWither = skeleton.getRandom().nextInt(100);
                    float witherChance =  DifficultyParameters.getAddPoweredChances(PlayerDifficultyManager.getDifficultyAtLocaltion(serverLevel, skeleton)) * 100;
                    if(chanceOfWither < witherChance) {
                        CompoundTag originalNBT = skeleton.saveWithoutId(new CompoundTag());
                        originalNBT.remove("UUID");
                        WitherSkeleton ws = skeleton.convertTo(EntityType.WITHER_SKELETON, true);
                        ws.load(originalNBT);
                        EventHooks.onLivingConvert(skeleton, ws);
                        event.setCanceled(true);
                    }
                }   

                for (Class entityClass : evolvedEntities.keySet()) {
                    if (entityClass.isInstance(entity) && !evolvedEntities.get(entityClass).isInstance(entity)) {
                        String currentDifficulty = PlayerDifficultyManager.getDifficultyAtLocaltion(serverLevel, entity);
                        int chanceOfSpawn = entity.getRandom().nextInt(100);
                        float evolutionChances = DifficultyParameters.getEvolutionChances(currentDifficulty) * 100;
                        
                        if(chanceOfSpawn < evolutionChances) {
                            Entity evolvedEntity = null;
                            if (entityClass == Zombie.class) {
                                evolvedEntity = RaisedZombie.fromZombie((Zombie) entity);
                            } else if (entityClass == Creeper.class) {
                                evolvedEntity = AncientCreeper.fromCreeper((Creeper) entity);
                            }

                            if (evolvedEntity != null) {
                                event.getLevel().addFreshEntity(evolvedEntity);
                                event.setCanceled(true);
                            }
                        }

                        entity.getPersistentData().putBoolean("Processed", true);
                    }
                }

                // Change to give role to zombies
                if(entity instanceof RaisedZombie) {
                    RaisedZombie rz = (RaisedZombie) entity;
                    int chanceOfRole = rz.getRandom().nextInt(100);
                    float roleChance =  DifficultyParameters.getAddPoweredChances(PlayerDifficultyManager.getDifficultyAtLocaltion(serverLevel, rz)) * 100;
                    if(chanceOfRole < roleChance) {
                        HashMap<Integer, ItemStack> possibleRoles = new HashMap<>();                        
                        possibleRoles.put(0, new ItemStack(Items.DIAMOND_PICKAXE));
                        possibleRoles.put(1, new ItemStack(Items.DIAMOND_SHOVEL));
                        possibleRoles.put(2, new ItemStack(Items.DIAMOND_AXE));
                        possibleRoles.put(3, new ItemStack(Items.DIAMOND_HOE));
                        possibleRoles.put(4, new ItemStack(Blocks.DIRT, 32));
                        possibleRoles.put(5, new ItemStack(Blocks.COBBLESTONE, 32));
                        possibleRoles.put(6, new ItemStack(Blocks.STONE, 32));
                        possibleRoles.put(7, new ItemStack(Blocks.GRASS_BLOCK, 32));

                        int role = rz.getRandom().nextInt(possibleRoles.size());

                        rz.setItemSlot(EquipmentSlot.MAINHAND, possibleRoles.get(role));
                    }
                    
                    entity.getPersistentData().putBoolean("Processed", true);
                }
                
                // Change to give armour/swprds to monsters
                if(entity instanceof Zombie || entity instanceof Skeleton) {
                    int chanceOfGear = entity.getRandom().nextInt(100);
                    float gearChance =  DifficultyParameters.getAddPoweredChances(PlayerDifficultyManager.getDifficultyAtLocaltion(serverLevel, entity)) * 100;
                    if(chanceOfGear < gearChance) {
                        int nbOfGear = entity.getRandom().nextInt(5);

                        HashMap<Integer, ItemStack> possibleGear = new HashMap<>();                        
                        possibleGear.put(0, new ItemStack(Items.DIAMOND_SWORD));
                        possibleGear.put(1, new ItemStack(Items.GOLDEN_SWORD));
                        possibleGear.put(2, new ItemStack(Items.IRON_SWORD));

                        possibleGear.put(3, new ItemStack(Items.DIAMOND_HELMET));
                        possibleGear.put(4, new ItemStack(Items.GOLDEN_HELMET));
                        possibleGear.put(5, new ItemStack(Items.IRON_HELMET));

                        possibleGear.put(6, new ItemStack(Items.DIAMOND_CHESTPLATE));
                        possibleGear.put(7, new ItemStack(Items.GOLDEN_CHESTPLATE));
                        possibleGear.put(8, new ItemStack(Items.IRON_CHESTPLATE));

                        possibleGear.put(9, new ItemStack(Items.DIAMOND_LEGGINGS));
                        possibleGear.put(10, new ItemStack(Items.GOLDEN_LEGGINGS));
                        possibleGear.put(11, new ItemStack(Items.IRON_LEGGINGS));

                        possibleGear.put(12, new ItemStack(Items.DIAMOND_BOOTS));
                        possibleGear.put(13, new ItemStack(Items.GOLDEN_BOOTS));
                        possibleGear.put(14, new ItemStack(Items.IRON_BOOTS));

                        for(int i = 0; i < nbOfGear; i++) {
                            int role = entity.getRandom().nextInt(possibleGear.size());
                            EquipmentSlot slot = EquipmentSlot.MAINHAND;
                            
                            if(role >= 12) {
                                slot = EquipmentSlot.FEET;
                            } else if (role >= 9) {
                                slot = EquipmentSlot.LEGS;
                            } else if (role >= 6) {
                                slot = EquipmentSlot.CHEST;
                            } else if (role >= 3) {
                                slot = EquipmentSlot.HEAD;
                            }

                            if(((LivingEntity)entity).getItemBySlot(slot).isEmpty()) {
                                if(entity instanceof LivingEntity) {
                                    ((LivingEntity)entity).setItemSlot(slot, possibleGear.get(role));
                                }
                            }
                        }
                    }
                    
                    entity.getPersistentData().putBoolean("Processed", true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        Level level = event.getEntity().level();

        if (level instanceof ServerLevel serverLevel) {
            if (event.getEntity() instanceof Player) {
                Player player = (Player) event.getEntity();
                String difficulty = PlayerDifficultyManager.getDifficulty(serverLevel.getServer(), player.getUUID());
                float difficultyFactor = DifficultyParameters.getDamageMultiplier(difficulty);
                DamageSource source = event.getSource();


                if (source.getEntity() instanceof Monster) {
                    // Change damage for  difficulty
                    float newDamage = event.getAmount() * difficultyFactor;
                    if(newDamage <= 0) {
                        event.setCanceled(true);
                    }
                    event.setAmount(newDamage);
                } else if (source.getMsgId().equals("starve")) {
                    float currentHealth = player.getHealth();

                    if (difficulty.equals("peaceful")) {
                        event.setCanceled(true);
                    } else if(difficulty.equals("easy") && currentHealth <= 10) {
                        event.setCanceled(true);
                    } else if(difficulty.equals("normal") && currentHealth < 2) {
                        event.setCanceled(true);
                    }
                }
            } 

            // else if (event.getSource().getEntity() instanceof Player) {
            //     Player player = (Player) event.getSource().getEntity();
            //     float difficultyFactor = DifficultyParameters.getDamageMultiplier(PlayerDifficultyManager.getDifficulty(serverLevel, player.getUUID()));
                
            //     // Decrease damage for higher difficulties
            //     float newDamage = event.getAmount() / difficultyFactor;

            //     event.setNewDamage(newDamage);            
            // }
        }
    }


    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String command = event.getParseResults().getReader().getString().toLowerCase();
        
        if (command.startsWith("difficulty easy") || command.startsWith("difficulty normal") || command.startsWith("difficulty hard")) {
            event.setCanceled(true);
            // create command /difficulty server instead
            
            String[] parts = command.split(" ", 2);

            String difficultyArg = parts[1];
            
            // Execute your custom difficulty command
            CommandSourceStack source = event.getParseResults().getContext().getSource();
            String customCommand = "difficulty-server " + difficultyArg;
            
            source.getServer().getCommands().performPrefixedCommand(source, customCommand);
        }
    }
}
