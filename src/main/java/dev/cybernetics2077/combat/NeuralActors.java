package dev.cybernetics2077.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;

/** Shared permission boundary for player and NPC neural actions. */
public final class NeuralActors {
  public static boolean canAttack(LivingEntity actor, LivingEntity target) {
    if (actor instanceof ServerPlayer p) return CyberwareRuntime.canAttack(p, target);
    return actor != null
        && target != null
        && actor != target
        && actor.isAlive()
        && target.isAlive()
        && actor.level() == target.level()
        && !target.isInvulnerable()
        && !actor.isAlliedTo(target)
        && !(target instanceof Player p && (p.isCreative() || p.isSpectator()));
  }

  public static boolean hostile(LivingEntity actor, LivingEntity target) {
    if (actor instanceof ServerPlayer p)
      return dev.cybernetics2077.cyberware.AugmentRuntime.hostile(p, target);
    return canAttack(actor, target)
        && (actor instanceof Mob m && m.getTarget() == target
            || target == actor.getLastHurtByMob()
            || target instanceof Mob m && m.getTarget() == actor);
  }

  public static int range(LivingEntity actor) {
    return actor instanceof ServerPlayer p
        ? dev.cybernetics2077.scanner.ScannerNetwork.range(p)
        : 32;
  }

  private NeuralActors() {}
}
