package com.azukaar.difficultyoverhaul.enigmatic;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;

public class EnigmaticLegacyIntegration {
    /**
     * Checks if a player has the Cursed Ring equipped
     * @param player The player to check
     * @return true if the player has the Cursed Ring, false otherwise
     */
    public static boolean hasCursedRing(Player player) {
        try {
            // Only import and use these classes inside this method
            return com.aizistral.enigmaticlegacy.handlers.SuperpositionHandler.hasCurio(
                player, 
                com.aizistral.enigmaticlegacy.registries.EnigmaticItems.CURSED_RING
            );
        } catch (Exception e) {
            // Log the error if needed
            return false;
        }
    }
    
    // Add other integration methods as needed
}
