package dev.cybernetics2077.combat;

import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Inventory stacks remain stored normally; deployed cyberarms expose empty gameplay hand slots. */
public final class MantisHands {
  public static Predicate<Player> clientOccupied = p -> false;

  public static boolean occupied(Player p) {
    if (p == null) return false;
    if (p instanceof ServerPlayer server) {
      var state = CyberwareRuntime.state(server);
      long age = server.serverLevel().getGameTime() - state.changed;
      return (state.deployed || age >= 0 && age < CombatRules.DEPLOY)
          && CyberwareRuntime.claws(server);
    }
    return clientOccupied.test(p);
  }

  private MantisHands() {}
}
