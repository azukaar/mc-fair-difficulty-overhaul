package com.azukaar.difficultyoverhaul.difficulty;

public class DifficultyParameters {
  static public float normalizeDiff(String diff1, String diff2) {
    if(diff1.equals("easy") || diff1.equals("normal")) {
      return getRawDamageMultiplier(diff2) / getRawDamageMultiplier(diff1);
    }
    return 1.0f;
  }
  
  static public float getRawDamageMultiplier(String difficulty) {
    switch (difficulty) {
      case "peaceful":
        return 0.0f;
        // return 1.0f;
      case "easy":
        return 0.34f;
        // return 1.0f;
      case "normal":
        return 0.67f;
        // return 1.0f;
      case "hard":
        return 1.0f;
      case "expert":
        return 1.25f;
      case "nightmare":
        return 2.0f;
      case "apocalyptic":
        return 4.0f;
      default:
        return 1.0f;
    }
  }

  static public float getDamageMultiplier(String difficulty) {
    // String serverDiff = DifficultyConfig.SERVER.serverDifficulty.get();
    // Float normalizer = normalizeDiff(serverDiff, "hard");
    return getRawDamageMultiplier(difficulty); // * normalizer;
  }
  
  static public float getEvolutionChances(String difficulty) {
    switch (difficulty) {
      case "peaceful":
        return 0.0f;
      case "easy":
        return 0.0f;
      case "normal":
        return 0.0f;
      case "hard":
        return 0.0f;
      case "expert":
        return 0.2f;
      case "nightmare":
        return 0.35f;
      case "apocalyptic":
        return 0.6f;
      default:
        return 0.0f;
    }
  }
  
  static public float getAddPoweredChances(String difficulty) {
    switch (difficulty) {
      case "peaceful":
        return 0.0f;
      case "easy":
        return 0.0f;
      case "normal":
        return 0.0f;
      case "hard":
        return 0.0f;
      case "expert":
        return 0.1f;
      case "nightmare":
        return 0.15f;
      case "apocalyptic":
        return 0.25f;
      default:
        return 0.0f;
    }
  }
  
  static public int getRespawnHunger(String difficulty) {
    switch (difficulty) {
      case "peaceful":
        return 20;
      case "easy":
        return 20;
      case "normal":
        return 20;
      case "hard":
        return 20;
      case "expert":
        return 10;
      case "nightmare":
        return 5;
      case "apocalyptic":
        return 1;
      default:
        return 20;
    }
  }
}
