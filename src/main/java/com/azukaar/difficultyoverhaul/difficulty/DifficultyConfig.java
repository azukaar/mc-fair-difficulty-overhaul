package com.azukaar.difficultyoverhaul.difficulty;

import org.apache.commons.lang3.tuple.Pair;

import net.neoforged.neoforge.common.ModConfigSpec;

public class DifficultyConfig {
    public static class Server {
        public final ModConfigSpec.ConfigValue<String> serverDifficulty;
        public final ModConfigSpec.ConfigValue<Boolean> perPlayerDifficulty;
        public final ModConfigSpec.ConfigValue<String> minPlayerDifficulty;
        public final ModConfigSpec.ConfigValue<String> maxPlayerDifficulty;

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

            builder.pop();
        }
        
        private static boolean isValidDifficulty(Object obj) {
            if (obj instanceof String) {
                String difficulty = (String) obj;
                return DifficultyCommand.DIFFICULTY_STRINGS.contains(difficulty.toLowerCase());
            }
            return false;
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