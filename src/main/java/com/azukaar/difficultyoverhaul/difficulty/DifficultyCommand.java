package com.azukaar.difficultyoverhaul.difficulty;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
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
  }

  private static Component getTranslatedDifficulty(String difficulty) {
    return Component.translatable("difficulty." + difficulty.toLowerCase());
  }

  private static int getSetup(CommandContext<CommandSourceStack> context) {
    final String setup = Component.translatable("difficulty.setup",
        DifficultyConfig.SERVER.serverDifficulty.get(),
        DifficultyConfig.SERVER.perPlayerDifficulty.get(),
        DifficultyConfig.SERVER.minPlayerDifficulty.get(),
        DifficultyConfig.SERVER.maxPlayerDifficulty.get()).getString();

    context.getSource().sendSuccess(() -> Component.literal(setup), false);
    return 1;
  }

  private static CompletableFuture<Suggestions> suggestConfigName(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    builder.suggest("serverDifficulty");
    builder.suggest("perPlayerDifficulty");
    builder.suggest("minPlayerDifficulty");
    builder.suggest("maxPlayerDifficulty");
    return builder.buildFuture();
  }

  private static int setSetup(CommandContext<CommandSourceStack> context, String key, String value) {
    if (key.equals("serverDifficulty") || key.equals("minPlayerDifficulty") || key.equals("maxPlayerDifficulty")) {
      if (!DifficultyCommand.DIFFICULTY_STRINGS.contains(value.toLowerCase())) {
        context.getSource().sendFailure(Component.translatable("difficulty.invalid.option"));
        return 0;
      }

      Component translatedDifficulty = getTranslatedDifficulty(value);

      switch (key) {
        case "serverDifficulty":
          DifficultyConfig.SERVER.serverDifficulty.set(value);
          DifficultyConfig.SERVER.serverDifficulty.save();
          context.getSource().sendSuccess(() -> Component.translatable("difficulty.server.set", translatedDifficulty),
              true);
          break;
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

    PlayerDifficultyManager.setDifficulty(level.getServer(), player.getUUID(), difficulty);
    
    Component translatedDifficulty = getTranslatedDifficulty(difficulty);
    context.getSource().sendSuccess(() -> Component.translatable("difficulty.personal.set", translatedDifficulty), false);
    
    return 1;
  }

  private static int getDifficulty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
    String currentDifficulty = DifficultyConfig.SERVER.serverDifficulty.get();
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

    if (VANILLA_DIFFICULTY_STRINGS.contains(factor)) {
      context.getSource().getServer().setDifficulty(Difficulty.byName(factor), true);
    } else {
      context.getSource().getServer().setDifficulty(Difficulty.HARD, true);
    }

    DifficultyConfig.SERVER.serverDifficulty.set(factor);
    DifficultyConfig.SERVER.serverDifficulty.save();

    Component translatedDifficulty = getTranslatedDifficulty(factor);
    context.getSource().sendSuccess(() -> Component.translatable("difficulty.set", translatedDifficulty), true);
    return 1;
  }

  private static CompletableFuture<Suggestions> suggestDifficulty(CommandContext<CommandSourceStack> context,
      SuggestionsBuilder builder) {
    DIFFICULTY_STRINGS.forEach(builder::suggest);
    builder.suggest("help");
    return builder.buildFuture();
  }
}
