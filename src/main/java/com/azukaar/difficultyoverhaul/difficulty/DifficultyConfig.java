package com.azukaar.difficultyoverhaul.difficulty;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class DifficultyConfig {
    public static class Server {
        public final ForgeConfigSpec.ConfigValue<Boolean> perPlayerDifficulty;
        public final ForgeConfigSpec.ConfigValue<String> minPlayerDifficulty;
        public final ForgeConfigSpec.ConfigValue<String> maxPlayerDifficulty;
        public final ForgeConfigSpec.ConfigValue<String> enableDifficultyLockFrom;
        
        // Mechanics introduction
        public final ForgeConfigSpec.ConfigValue<String> enableHungerNerf;
        public final ForgeConfigSpec.ConfigValue<String> enableNoSleep;
        public final ForgeConfigSpec.ConfigValue<Boolean> softHardcore;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionToNightPurge;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> dimensionToFixSleep;
        public final ForgeConfigSpec.ConfigValue<Boolean> peacefulRegen;
        public final ForgeConfigSpec.ConfigValue<Boolean> peacefulNoFood;
        public final ForgeConfigSpec.ConfigValue<Boolean> disableCustomTexture;

        // bonuses
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> luckPerDiff;
        public final ForgeConfigSpec.ConfigValue<List<? extends Double>> damagePerDiff;
        public final ForgeConfigSpec.ConfigValue<List<? extends Double>> xpDropPerDiff;

        // list of mobs that only spawn on X difficulty
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> normalMobs;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> hardMobs;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> expertMobs;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> nightmareMobs;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> apocalypticMobs;

        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> respawnDistancePerDiff;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> healthDeathPenaltyPerDiff;
        public final ForgeConfigSpec.ConfigValue<Integer> healthDeathPenaltyMinimum;

        Server(ForgeConfigSpec.Builder builder) {
            builder
            .comment("Manage the difficulty settings of the server")
            .push("difficulty");

            perPlayerDifficulty = builder
                .comment("Enable per-player difficulty settings. Players will be able to set their own personnal difficulty with /my-difficulty. This will force the vanilla difficulty to hard (which should not impact the actual difficulty of the game), but the serverDifficulty will be used as default player difficulty.")
                .define("perPlayer", false);

            minPlayerDifficulty = builder
                .comment("The minimum difficulty a player can set for themselves.")
                .define("minPlayer", "easy", Server::isValidDifficulty);

            maxPlayerDifficulty = builder
                .comment("The maximum difficulty a player can set for themselves.")
                .define("maxPlayer", "apocalyptic", Server::isValidDifficulty);
                
            enableDifficultyLockFrom = builder
                .comment("Enable/Disable the difficulty lock. This will prevent players from downgrading their difficulty. By default the value is expert, meaning that players can only downgrade between peaceful to hard, but higher difficulties can only be upgraded. Leave empty to disable the lock.")
                .define("enableDifficultyLockFrom", "expert", Server::isValidDifficultyOrEmpty);

            softHardcore = builder
                .comment("Enable/Disable the soft-hardcore mechanics. This is disabled by default. It will make it so that if you die repeatedly, and the health penaly is applied to the player, once they reach the minimum health, they will be forced to forever spectate.")
                .define("softHardcore", false);

            peacefulRegen = builder
                .comment("Simulate vanilla peaceful health regeneration. This means that players on peaceful will regenerate health over time, even if they are not at full hunger.")
                .define("peacefulRegen", true);

            peacefulNoFood = builder
                .comment("Players on peaceful do not need to eat. This means that their hunger will not decrease over time.")
                .define("peacefulNoFood", true);

            disableCustomTexture = builder
                .comment("Disable the custom mob textures. This is useful if you use resource packs.")
                .define("disableCustomTexture", false);

            builder
                .pop()
                .comment("Enable/Disable, or change specific mechanics kick off")
                .push("mechanics");

            enableHungerNerf = builder
                .comment("This mechanics makes you respawn with less hunger to prevent suicide-feeding.")
                .define("hungerNerf", "expert", Server::isValidDifficulty);

            enableNoSleep =  builder
                .comment("Prevent player of that difficulty from sleeping. Forces players to have to survive the night and various events from other mods.")
                .define("noSleep", "expert", Server::isValidDifficulty); 

            dimensionToNightPurge = builder
                .comment("This is a purge that happens at the beginning of the night, to prevent the MC 1.18+ large cave population from hoarding the mob cap and preventing surface mobs.")
                .defineList("dimensionToNightPurge", new ArrayList<>(List.of("minecraft:overworld")), obj -> obj instanceof String);

            dimensionToFixSleep = builder
                .comment("Which dimension should have custom sleep mechanics? This mechanic checks if every players that are allowed to sleep are sleeping (otherwise you would be waiting for players who cannot sleep) and force the sleep when the conditions are met. It is compatible with the gamerule PlayerSleepingPercentage.")
                .defineList("dimensionToFixSleep", new ArrayList<>(List.of("minecraft:overworld")), obj -> obj instanceof String);

            respawnDistancePerDiff = builder
                .comment("The respawn distance for each difficulty. The default is 0, which means you will respawn at the spawnpoint. The order is: peaceful to apocalypse. The respawn distance is the maximum distance from the spawnpoint. This setting forces you to build an infrastructure to get back to your base.")
                .defineList("respawnDistancePerDiff", new ArrayList<>(List.of(0, 0, 0, 0, 0, 100, 500)), obj -> obj instanceof Integer);

            healthDeathPenaltyPerDiff = builder
                .comment("The health penalty for each difficulty. The default is 0, which means you will respawn with full health. The order is: peaceful to apocalypse. This is a penalty to your max health when you die. This setting makes you more self-conscious about the risk you will take as they have permanent consequences. Think of it as as a soft-hardcore system.")
                .defineList("healthDeathPenaltyPerDiff", new ArrayList<>(List.of(0, 0, 0, 0, 0, 0, -1)), obj -> obj instanceof Integer);

            healthDeathPenaltyMinimum = builder
                .comment("How low can your health go? This is the minimum health you can have when you die")
                .define("healthDeathPenaltyMinimum", 8, obj -> obj instanceof Integer);

            builder
            .pop()
            .comment("Enable/Disable, or change specific bonuses for difficulties")
            .push("bonuses");

            luckPerDiff = builder
                .comment("The luck bonus for each difficulty. The default is 0, which means you will have no luck bonus. The order is: peaceful to apocalypse. This setting makes it more worth it to take risk when using high difficulty (Use the Useful Luck mod !)")
                .defineList("luckPerDiff", new ArrayList<>(List.of(0, 0, 0, 0, 1, 3, 6)), obj -> obj instanceof Integer);

            damagePerDiff = builder
                .comment("The damage multiplicator bonus for each difficulty. The default is 1.0, which means you will have no damage bonus. The order is: peaceful to apocalypse. This setting encourages a high risk high reward fighting style, where high difficulty makes you more fragile but also more powerful. This is a good way to make the game more challenging without making it impossible.")
                .defineList("damagePerDiff", new ArrayList<>(List.of(1.0, 1.0, 1.0, 1.0, 1.25, 1.5, 2.0)), obj -> obj instanceof Double);

            xpDropPerDiff = builder
                .comment("The xp drop multiplicator bonus for each difficulty. The default is 1.0, which means you will have no xp drop bonus. The order is: peaceful to apocalypse.")
                .defineList("xpDropPerDiff", new ArrayList<>(List.of(1.0, 1.0, 1.0, 1.0, 1.25, 2.0, 3.0)), obj -> obj instanceof Double);

            builder
                .pop()
                .comment("Customize additional spawning rules for mobs.")
                .push("spawn");

            normalMobs = builder
                .comment("List of mobs that only spawn on normal difficulty and above.")
                .defineList("normalMobs", new ArrayList<>(List.of("example:some_normal_mobs")), obj -> obj instanceof String);

            hardMobs = builder
                .comment("List of mobs that only spawn on hard difficulty and above.")
                .defineList("hardMobs", new ArrayList<>(List.of("example:some_hard_mobs")), obj -> obj instanceof String);

            expertMobs = builder
                .comment("List of mobs that only spawn on expert difficulty and above.")
                .defineList("expertMobs", new ArrayList<>(List.of("example:some_expert_mobs")), obj -> obj instanceof String);

            nightmareMobs = builder
                .comment("List of mobs that only spawn on nightmare difficulty and above.")
                .defineList("nightmareMobs", new ArrayList<>(List.of("example:some_nightmare_mobs")), obj -> obj instanceof String);

            apocalypticMobs = builder
                .comment("List of mobs that only spawn on apocalyptic difficulty.")
                .defineList("apocalypticMobs", new ArrayList<>(List.of("example:some_apocalyptic_mobs")), obj -> obj instanceof String);
           
                builder.pop();
        }
        
        private static boolean isValidDifficulty(Object obj) {
            if (obj instanceof String) {
                String difficulty = (String) obj;
                return DifficultyCommand.DIFFICULTY_STRINGS.contains(difficulty.toLowerCase());
            }
            return false;
        }

        private static boolean isValidDifficultyOrEmpty(Object obj) {
            if (obj instanceof String) {
                String difficulty = (String) obj;
                return DifficultyCommand.DIFFICULTY_STRINGS.contains(difficulty.toLowerCase()) || difficulty.isEmpty();
            }
            return false;
        }

        public String getMobDifficulty(String entityName) {
            if (normalMobs.get().contains(entityName)) {
                return "normal";
            } else if (hardMobs.get().contains(entityName)) {
                return "hard";
            } else if (expertMobs.get().contains(entityName)) {
                return "expert";
            } else if (nightmareMobs.get().contains(entityName)) {
                return "nightmare";
            } else if (apocalypticMobs.get().contains(entityName)) {
                return "apocalyptic";
            }
            return "peaceful";
        }

        public Boolean getMechanicEnabled(String mechanic, String value) {
            switch (mechanic) {
                case "hungerNerf":
                    return DifficultyCommand.DIFFICULTY_STRINGS.indexOf(enableHungerNerf.get()) <= DifficultyCommand.DIFFICULTY_STRINGS.indexOf(value);
                case "dimensionToNightPurge":
                    return dimensionToNightPurge.get().contains(value);
                case "dimensionToFixSleep":
                    return dimensionToFixSleep.get().contains(value);
                case "noSleep": 
                    return DifficultyCommand.DIFFICULTY_STRINGS.indexOf(enableNoSleep.get()) <= DifficultyCommand.DIFFICULTY_STRINGS.indexOf(value);
                case "peacefulRegen":
                    return peacefulRegen.get();
                case "peacefulNoFood":
                    return peacefulNoFood.get();
                default:
                    return false;
            }
        }

        public int getRespawnDistance(String difficulty) {
            return respawnDistancePerDiff.get().get(DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty));
        }

        public int getHealthDeathPenalty(String difficulty) {
            return healthDeathPenaltyPerDiff.get().get(DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty));
        }

        public int getHealthDeathPenaltyMinimum() {
            return healthDeathPenaltyMinimum.get();
        }

        public Boolean isDifficultyChangeAllowed(String from, String to) {
            // check if the difficulty lock is enabled
            if (enableDifficultyLockFrom.get().isEmpty()) {
                return true;
            }
            
            // check if the from difficulty is lower than the to lock difficulty
            if (DifficultyCommand.DIFFICULTY_STRINGS.indexOf(from) < DifficultyCommand.DIFFICULTY_STRINGS.indexOf(enableDifficultyLockFrom.get())) {
                return true;
            }

            // check if upgrading 
            if (DifficultyCommand.DIFFICULTY_STRINGS.indexOf(from) < DifficultyCommand.DIFFICULTY_STRINGS.indexOf(to)) {
                return true;
            }

            return false;
        }

        public Boolean isSoftHardcoreEnabled() {
            return softHardcore.get();
        }

        public int getLuck(String difficulty) {
            return luckPerDiff.get().get(DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty));
        }

        public double getDamageMult(String difficulty) {
            return damagePerDiff.get().get(DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty));
        }

        public double getXpDropMult(String difficulty) {
            return xpDropPerDiff.get().get(DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty));
        }
    }

    public static final ForgeConfigSpec serverSpec;
    public static final Server SERVER;

    static {
        final Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }
}