package com.azukaar.difficultyoverhaul.difficulty;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class DifficultyCommand {
  public static final List<String> VANILLA_DIFFICULTY_STRINGS = List.of(
      "peaceful", "easy", "normal", "hard");
  public static final List<String> DIFFICULTY_STRINGS = List.of(
      "peaceful", "easy", "normal", "hard",
      "expert", "nightmare", "apocalyptic");

  public enum DIFFICULTY {
    PEACEFUL,
    EASY,
    NORMAL,
    HARD,
    EXPERT,
    NIGHTMARE,
    APOCALYPTIC;
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    dispatcher.register(
        Commands.literal("my-difficulty")
            .executes(DifficultyCommand::getMyDifficulty)
            .then(Commands.argument("difficulty", StringArgumentType.string())
                .suggests(DifficultyCommand::suggestDifficulty)
                .executes(context -> setMyDifficulty(context, context.getArgument("difficulty", String.class)))));

    dispatcher.register(
        Commands.literal("difficulty")
            .requires(source -> source.hasPermission(2))
            .executes(DifficultyCommand::getDifficulty)
            .then(Commands.argument("difficulty", StringArgumentType.string())
                .suggests(DifficultyCommand::suggestDifficulty)
                .executes(context -> setDifficulty(context, context.getArgument("difficulty", String.class)))));

    dispatcher.register(
        Commands.literal("difficulty-server")
            .requires(source -> source.hasPermission(2))
            .executes(DifficultyCommand::getDifficulty)
            .then(Commands.argument("difficulty", StringArgumentType.string())
                .suggests(DifficultyCommand::suggestDifficulty)
                .executes(context -> setDifficulty(context, context.getArgument("difficulty", String.class)))));

    dispatcher.register(
        Commands.literal("difficulty-setup")
            .requires(source -> source.hasPermission(2))
            .executes(DifficultyCommand::getSetup)
            .then(Commands.literal("set")
                .then(Commands.argument("key", StringArgumentType.string())
                    .suggests(DifficultyCommand::suggestConfigName)
                    .then(Commands.argument("value", StringArgumentType.string())
                        .executes(context -> setSetup(context, context.getArgument("key", String.class),
                            context.getArgument("value", String.class)))))));
    
    // set max health penalty
    dispatcher.register(
        Commands.literal("difficulty-health-modifier")
            .requires(source -> source.hasPermission(2))
            .executes(DifficultyCommand::getSetup)
            .then(Commands.literal("set")
                .then(Commands.argument("value", IntegerArgumentType.integer())
                    .executes(context -> setOwnHealthPenalty(context, context.getArgument("value", Integer.class))))));

    dispatcher.register(
        Commands.literal("difficulty-health-modifier")
            .requires(source -> source.hasPermission(2))
            .executes(DifficultyCommand::getSetup)
            .then(Commands.literal("set")
                .then(Commands.argument("player", StringArgumentType.string())
                    .then(Commands.argument("value", IntegerArgumentType.integer())
                        .executes(context -> setHealthPenalty(context, GetUUIDFromUsername(context, context.getArgument("player", String.class)),
                            context.getArgument("value", Integer.class)))))));

    dispatcher.register(
        Commands.literal("difficulty-health-modifier")
            .requires(source -> source.hasPermission(2))
            .executes(DifficultyCommand::getSetup)
            .then(Commands.literal("get")
                    .executes(context -> getOwnHealthPenalty(context))));

    dispatcher.register(
        Commands.literal("difficulty-health-modifier")
            .requires(source -> source.hasPermission(2))
            .executes(DifficultyCommand::getSetup)
            .then(Commands.literal("get")
                .then(Commands.argument("player", StringArgumentType.string())
                    .executes(context -> getHealthPenalty(context, GetUUIDFromUsername(context, context.getArgument("player", String.class)))))));
  }

  private static Component getTranslatedDifficulty(String difficulty) {
    return Component.translatable("difficulty." + difficulty.toLowerCase());
  }

  private static int getSetup(CommandContext<CommandSourceStack> context) {
    final String setup = Component.translatable("difficulty.setup",
        DifficultyConfig.SERVER.perPlayerDifficulty.get(),
        DifficultyConfig.SERVER.minPlayerDifficulty.get(),
        DifficultyConfig.SERVER.maxPlayerDifficulty.get(),
        DifficultyConfig.SERVER.enableDifficultyLockFrom.get()
        ).getString();

    context.getSource().sendSuccess(() -> Component.literal(setup), false);
    return 1;
  }

  private static UUID GetUUIDFromUsername(CommandContext<CommandSourceStack> context, String username) {
    ServerPlayer player = context.getSource().getServer().getPlayerList().getPlayerByName(username);
    if (player != null) {
      return player.getUUID();
    }
    context.getSource().sendFailure(Component.translatable("difficulty.player.not.found", username));
    return null;
  }

  private static CompletableFuture<Suggestions> suggestConfigName(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    builder.suggest("perPlayerDifficulty");
    builder.suggest("minPlayerDifficulty");
    builder.suggest("maxPlayerDifficulty");
    builder.suggest("enableDifficultyLockFrom");
    return builder.buildFuture();
  }

  private static int setSetup(CommandContext<CommandSourceStack> context, String key, String value) {
    if (key.equals("serverDifficulty") || key.equals("minPlayerDifficulty") || key.equals("maxPlayerDifficulty") || key.equals("enableDifficultyLockFrom")) {
      System.out.println("Setting " + key + " to '" + value + "'");

      if(key.equals("enableDifficultyLockFrom") && (value.equals("")|| value.equals("null"))) {
        System.out.println("Setting " + key + " to null");
          DifficultyConfig.SERVER.enableDifficultyLockFrom.set(value);
          DifficultyConfig.SERVER.enableDifficultyLockFrom.save();
          context.getSource()
              .sendSuccess(() -> Component.translatable("difficulty.lock.set.null", value), true);
              return 1;
      }

      if (!DifficultyCommand.DIFFICULTY_STRINGS.contains(value.toLowerCase())) {
        context.getSource().sendFailure(Component.translatable("difficulty.invalid.option"));
        return 0;
      }

      Component translatedDifficulty = getTranslatedDifficulty(value);

      switch (key) {
        case "minPlayerDifficulty":
          DifficultyConfig.SERVER.minPlayerDifficulty.set(value);
          DifficultyConfig.SERVER.minPlayerDifficulty.save();
          context.getSource()
              .sendSuccess(() -> Component.translatable("difficulty.min.player.set", translatedDifficulty), true);
          break;
        case "maxPlayerDifficulty":
          DifficultyConfig.SERVER.maxPlayerDifficulty.set(value);
          DifficultyConfig.SERVER.maxPlayerDifficulty.save();
          context.getSource()
              .sendSuccess(() -> Component.translatable("difficulty.max.player.set", translatedDifficulty), true);
          break;
        case "enableDifficultyLockFrom":
          DifficultyConfig.SERVER.enableDifficultyLockFrom.set(value);
          DifficultyConfig.SERVER.enableDifficultyLockFrom.save();
          context.getSource()
              .sendSuccess(() -> Component.translatable("difficulty.lock.set", value), true);
          break;
      }

      PlayerDifficultyManager.checkAllPlayersDifficulty(context.getSource().getServer());
    } else if (key.equals("perPlayerDifficulty")) {
      DifficultyConfig.SERVER.perPlayerDifficulty.set(Boolean.parseBoolean(value));
      DifficultyConfig.SERVER.perPlayerDifficulty.save();
      context.getSource().sendSuccess(() -> Component.translatable("difficulty.per.player.set", value), true);
    } else {
      context.getSource().sendFailure(Component.translatable("difficulty.invalid.setting"));
      return 0;
    }

    return 1;
  }

  private static int getMyDifficulty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    if (!DifficultyConfig.SERVER.perPlayerDifficulty.get()) {
      context.getSource().sendFailure(Component.translatable("difficulty.per.player.disabled"));
      return 0;
    }

    ServerPlayer player;
    try {
      player = context.getSource().getPlayerOrException();
    } catch (CommandSyntaxException e) {
      context.getSource().sendFailure(Component.translatable("difficulty.player.only"));
      return 0;
    }

    ServerLevel level = player.serverLevel();
    String difficulty = PlayerDifficultyManager.getDifficulty(level.getServer(), player.getUUID());
    Component translatedDifficulty = getTranslatedDifficulty(difficulty);
    
    context.getSource().sendSuccess(() -> Component.translatable("difficulty.personal", translatedDifficulty), false);
    return 1;
  }

  private static int setMyDifficulty(CommandContext<CommandSourceStack> context, String factor) {
    if (!DifficultyConfig.SERVER.perPlayerDifficulty.get()) {
      context.getSource().sendFailure(Component.translatable("difficulty.per.player.disabled"));
      return 0;
    }

    ServerPlayer player;
    try {
      player = context.getSource().getPlayerOrException();
    } catch (CommandSyntaxException e) {
      context.getSource().sendFailure(Component.translatable("difficulty.player.only"));
      return 0;
    }

    ServerLevel level = player.serverLevel();
    String difficulty = factor;

    if (!DifficultyCommand.DIFFICULTY_STRINGS.contains(difficulty.toLowerCase())) {
      context.getSource().sendFailure(Component.translatable("difficulty.invalid.option"));
      return 0;
    }

    if (difficulty.equals("help")) {
      context.getSource().sendSuccess(() -> Component.translatable("difficulty.options", DIFFICULTY_STRINGS), true);
      return 1;
    }

    String minDiff = DifficultyConfig.SERVER.minPlayerDifficulty.get();
    String maxDiff = DifficultyConfig.SERVER.maxPlayerDifficulty.get();

    if (DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty) > DifficultyCommand.DIFFICULTY_STRINGS
        .indexOf(maxDiff)) {
      context.getSource().sendFailure(Component.translatable("difficulty.too.high", maxDiff));
      return 0;
    } else if (DifficultyCommand.DIFFICULTY_STRINGS.indexOf(difficulty) < DifficultyCommand.DIFFICULTY_STRINGS
        .indexOf(minDiff)) {
      context.getSource().sendFailure(Component.translatable("difficulty.too.low", minDiff));
      return 0;
    }

    String currentDifficulty = PlayerDifficultyManager.getDifficulty(level.getServer(), player.getUUID());
    if (!DifficultyConfig.SERVER.isDifficultyChangeAllowed(currentDifficulty, difficulty)) {
      context.getSource().sendFailure(Component.translatable("difficulty.change.not.allowed", currentDifficulty, difficulty));
      return 0;
    }

    PlayerDifficultyManager.setDifficulty(level.getServer(), player.getUUID(), difficulty);
    
    Component translatedDifficulty = getTranslatedDifficulty(difficulty);
    context.getSource().sendSuccess(() -> Component.translatable("difficulty.personal.set", translatedDifficulty), false);
    
    return 1;
  }

  private static int getDifficulty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    WorldDifficultyManager worldDifficultyManager = WorldDifficultyManager.get(context.getSource().getServer());
    String currentDifficulty = worldDifficultyManager.getWorldDifficulty();
    Component translatedDifficulty = getTranslatedDifficulty(currentDifficulty);
    context.getSource().sendSuccess(() -> Component.translatable("difficulty.current", translatedDifficulty), false);
    return 1;
  }

  private static int setDifficulty(CommandContext<CommandSourceStack> context, String factor) {
    if (!DifficultyCommand.DIFFICULTY_STRINGS.contains(factor.toLowerCase())) {
      context.getSource().sendFailure(Component.translatable("difficulty.invalid.option"));
      return 0;
    }

    if (factor.equals("help")) {
      context.getSource().sendSuccess(() -> Component.translatable("difficulty.options", DIFFICULTY_STRINGS), true);
      return 1;
    }

    WorldDifficultyManager worldDifficultyManager = WorldDifficultyManager.get(context.getSource().getServer());
    String currentDifficulty = worldDifficultyManager.getWorldDifficulty();

    if(!DifficultyConfig.SERVER.isDifficultyChangeAllowed(currentDifficulty, factor)) {
      context.getSource().sendFailure(Component.translatable("difficulty.change.not.allowed", currentDifficulty, factor));
      return 0;
    }

    if (VANILLA_DIFFICULTY_STRINGS.contains(factor)) {
      context.getSource().getServer().setDifficulty(Difficulty.byName(factor), true);
    } else {
      context.getSource().getServer().setDifficulty(Difficulty.HARD, true);
    }

    worldDifficultyManager.setWorldDifficulty(context.getSource().getServer(), factor);

    Component translatedDifficulty = getTranslatedDifficulty(factor);
    context.getSource().sendSuccess(() -> Component.translatable("difficulty.set", translatedDifficulty), true);

    for(ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
      PlayerAttributesManager playerAttributesManager = PlayerAttributesManager.get(context.getSource().getServer());
      playerAttributesManager.applyLuck(player);
    }

    return 1;
  }

  private static CompletableFuture<Suggestions> suggestDifficulty(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    DIFFICULTY_STRINGS.forEach(builder::suggest);
    builder.suggest("help");
    return builder.buildFuture();
  }

  private static int setHealthPenalty(CommandContext<CommandSourceStack> context, UUID playerUuid, int value) {
    ServerLevel serverLevel = context.getSource().getLevel();
    PlayerAttributesManager playerAttributesManager = PlayerAttributesManager.get(serverLevel.getServer());

    playerAttributesManager.setPlayerMaxHealth(context.getSource().getServer().getPlayerList().getPlayer(playerUuid), value);

    context.getSource().sendSuccess(() -> Component.translatable("difficulty.health.modifier.set", value), true);
    return 1;
  }

  private static int setOwnHealthPenalty(CommandContext<CommandSourceStack> context, int value) {
    UUID playerUuid;
    try {
      playerUuid = context.getSource().getPlayerOrException().getUUID();
    } catch (CommandSyntaxException e) {
      e.printStackTrace();
      context.getSource().sendFailure(Component.translatable("difficulty.player.only"));
      return 0;
    }

    return setHealthPenalty(context, playerUuid, value);
  }

  private static int getHealthPenalty(CommandContext<CommandSourceStack> context, UUID playerUuid) {
    ServerLevel serverLevel = context.getSource().getLevel();
    PlayerAttributesManager playerAttributesManager = PlayerAttributesManager.get(serverLevel.getServer());
    double penalty = playerAttributesManager.getPlayerMaxHealth(playerUuid);

    context.getSource().sendSuccess(() -> Component.translatable("difficulty.health.modifier.get", penalty), true);
    return 1;
  }

  private static int getOwnHealthPenalty(CommandContext<CommandSourceStack> context) {
    UUID playerUuid;
    try {
      playerUuid = context.getSource().getPlayerOrException().getUUID();
    } catch (CommandSyntaxException e) {
      e.printStackTrace();
      context.getSource().sendFailure(Component.translatable("difficulty.player.only"));
      return 0;
    }

    return getHealthPenalty(context, playerUuid);
  }
}
