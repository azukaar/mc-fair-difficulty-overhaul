package com.azukaar.difficultyoverhaul.difficulty;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DifficultyConfig {
    public static class Server {
        public final ModConfigSpec.ConfigValue<String> serverDifficulty;
        public final ModConfigSpec.ConfigValue<Boolean> perPlayerDifficulty;
        public final ModConfigSpec.ConfigValue<String> minPlayerDifficulty;
        public final ModConfigSpec.ConfigValue<String> maxPlayerDifficulty;
        
        // Mechanics introduction
        public final ModConfigSpec.ConfigValue<String> enableHungerNerf;

        // list of mobs that only spawn on X difficulty
        public final ModConfigSpec.ConfigValue<List<? extends String>> normalMobs;
        public final ModConfigSpec.ConfigValue<List<? extends String>> hardMobs;
        public final ModConfigSpec.ConfigValue<List<? extends String>> expertMobs;
        public final ModConfigSpec.ConfigValue<List<? extends String>> nightmareMobs;
        public final ModConfigSpec.ConfigValue<List<? extends String>> apocalypticMobs;

        Server(ModConfigSpec.Builder builder) {
            builder
            .comment("Manage the difficulty settings of the server")
            .push("difficulty");

            serverDifficulty = builder
                .comment("The default difficulty factor for players (" + DifficultyCommand.DIFFICULTY_STRINGS + "). If per-player difficulty is enabled, they will default to this difficulty as well. The server itself (ex. spawned mobs) will use this difficulty when no players are around.")
                .define("server", "normal", Server::isValidDifficulty);

            perPlayerDifficulty = builder
                .comment("Enable per-player difficulty settings. Players will be able to set their own personnal difficulty with /my-difficulty. This will force the vanilla difficulty to hard (which should not impact the actual difficulty of the game), but the serverDifficulty will be used as default player difficulty.")
                .define("perPlayer", false);

            minPlayerDifficulty = builder
                .comment("The minimum difficulty a player can set for themselves.")
                .define("minPlayer", "easy", Server::isValidDifficulty);

            maxPlayerDifficulty = builder
                .comment("The maximum difficulty a player can set for themselves.")
                .define("maxPlayer", "apocalyptic", Server::isValidDifficulty);
                
            builder
            .comment("Enable/Disable, or change specific mechanics kick off")
            .push("mechanics");

            enableHungerNerf = builder
                .comment("This mechanics makes you respawn with less hunger to prevent suicide-feeding.")
                .define("hungerNerf", "expert", Server::isValidDifficulty);

            builder
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

        public Boolean getMechanicEnabled(String mechanic, String diff) {
            switch (mechanic) {
                case "hungerNerf":
                    return DifficultyCommand.DIFFICULTY_STRINGS.indexOf(enableHungerNerf.get()) <= DifficultyCommand.DIFFICULTY_STRINGS.indexOf(diff);
                default:
                    return false;
            }
        }
    }

    public static final ModConfigSpec serverSpec;
    public static final Server SERVER;

    static {
        final Pair<Server, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Server::new);
        serverSpec = specPair.getRight();
        SERVER = specPair.getLeft();
    }
}