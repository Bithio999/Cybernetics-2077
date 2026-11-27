package dev.cybernetics2077.mob;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.SecondaryDamage;
import dev.cybernetics2077.cyberware.AugmentExpansion;
import java.util.Comparator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;

/** NPC counterparts to the equipped player implants, using the same tier formulas and timers. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class CyberpsychoCombat {
  @SubscribeEvent
  public static void incoming(LivingIncomingDamageEvent e) {
    if (!(e.getSource().getEntity() instanceof Cyberpsycho c)
        || !c.operational()
        || !e.getSource().is(DamageTypes.MOB_ATTACK)
        || e.getSource().getDirectEntity() != c) return;
    float damage = e.getAmount() * (1 + c.tier(DENSE_MARROW) * .06f);
    float stab = c.tier(STABBER) * .04f;
    float rage = c.berserk() ? (c.tier(BIODYNE_BERSERK) - 1) * .05f : 0;
    float wrap = c.now() < c.systems().getLong("wrapEnd") ? c.tier(HANDLE_WRAP) * .04f : 0;
    float chance = Math.min(.5f, 1 - (1 - stab) * (1 - rage) * (1 - wrap));
    if (c.getRandom().nextFloat() < chance) {
      damage *= 1.5f * (c.berserk() ? 1 + (c.tier(BIODYNE_BERSERK) - 1) * .25f : 1);
      ((ServerLevel) c.level())
          .sendParticles(
              ParticleTypes.CRIT,
              e.getEntity().getX(),
              e.getEntity().getEyeY(),
              e.getEntity().getZ(),
              8,
              .2,
              .3,
              .2,
              .03);
    }
    e.setAmount(damage);
  }

  @SubscribeEvent
  public static void defense(LivingDamageEvent.Pre e) {
    if (!(e.getEntity() instanceof Cyberpsycho c)
        || !c.operational()
        || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    float damage = e.getNewDamage();
    int neo = c.tier(NEOFIBER);
    if (c.getRandom().nextFloat() < neo * .02f) damage *= 1 - (.5f + neo * .02f);
    damage *= 1 - c.tier(PAIN_EDITOR) * .02f;
    if (c.berserk()) {
      damage *= 1 - c.tier(BIODYNE_BERSERK) * .1f;
      damage = Math.min(damage, Math.max(0, c.getHealth() - c.getMaxHealth() * .25f));
    }
    e.setNewDamage(damage);
  }

  @SubscribeEvent
  public static void healing(LivingHealEvent e) {
    if (e.getEntity() instanceof Cyberpsycho c && e.getAmount() > 0)
      e.setAmount(e.getAmount() * (1 + c.tier(BIOMONITOR) * .04f));
  }

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void revive(LivingDeathEvent e) {
    if (!(e.getEntity() instanceof Cyberpsycho c)
        || c.isNoAi()
        || c.isInvulnerable()
        || c.isRemoved()
        || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    int t = c.tier(SECOND_HEART);
    if (t == 0 || c.now() < c.systems().getLong("heartReady")) return;
    c.systems().putLong("heartReady", c.now() + (400 - t * 40) * 20L);
    e.setCanceled(true);
    c.setHealth(c.getMaxHealth());
    c.clearFire();
    c.invulnerableTime = 20;
    ((ServerLevel) c.level())
        .sendParticles(ParticleTypes.HEART, c.getX(), c.getEyeY(), c.getZ(), 20, .5, .5, .5, .02);
  }

  private static boolean enemy(Cyberpsycho c, LivingEntity other) {
    return other != c
        && other.isAlive()
        && !other.isInvulnerable()
        && !(other instanceof Cyberpsycho)
        && !c.isAlliedTo(other)
        && (other == c.getTarget()
            || other == c.getLastHurtByMob()
            || other instanceof net.minecraft.world.entity.player.Player p
                && !p.isCreative()
                && !p.isSpectator());
  }

  private static void discharge(Cyberpsycho c, LivingEntity origin, int t, int radius, int limit) {
    if (!(c.level() instanceof ServerLevel level) || t == 0) return;
    var source =
        new DamageSource(
            c.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(AugmentExpansion.DISCHARGE),
            c,
            c);
    var targets =
        level.getEntitiesOfClass(
            LivingEntity.class,
            origin.getBoundingBox().inflate(radius),
            other ->
                enemy(c, other)
                    && origin.distanceToSqr(other) <= radius * radius
                    && c.hasLineOfSight(other));
    targets.sort(Comparator.comparingDouble(c::distanceToSqr));
    for (var target : targets.stream().limit(limit).toList())
      SecondaryDamage.hurt(target, source, 2 + 2 * t);
    level.sendParticles(
        ParticleTypes.ELECTRIC_SPARK, c.getX(), c.getY() + 1, c.getZ(), 32, 1, .5, 1, .05);
  }

  public static void microgenerator(Cyberpsycho c, LivingEntity target) {
    int t = c.tier(MICROGENERATOR);
    if (t == 0 || c.now() < c.systems().getLong("microReady")) return;
    c.systems().putLong("microReady", c.now() + (12 - t) * 20L);
    discharge(c, target, t, 3, 4);
  }

  @SubscribeEvent
  public static void retaliation(LivingDamageEvent.Post e) {
    if (!(e.getEntity() instanceof Cyberpsycho c)
        || !c.operational()
        || e.getNewDamage() <= 0
        || e.getSource().is(AugmentExpansion.DISCHARGE)
        || !(e.getSource().getEntity() instanceof LivingEntity attacker)
        || !enemy(c, attacker)) return;
    int t = c.tier(SHOCK_N_AWE);
    if (t == 0
        || c.now() < c.systems().getLong("shockReady")
        || c.getRandom().nextFloat() >= t * .02f) return;
    c.systems().putLong("shockReady", c.now() + 40);
    discharge(c, c, t, 4, 8);
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void kill(LivingDeathEvent e) {
    if (e.isCanceled() || !(e.getSource().getEntity() instanceof Cyberpsycho c) || !c.operational())
      return;
    int t = c.tier(HEAL_ON_KILL);
    if (t > 0) c.heal(c.getMaxHealth() * (t + 1) * .01f);
    if (c.berserk()) c.systems().putInt("berserkKills", c.systems().getInt("berserkKills") + 1);
  }

  @SubscribeEvent
  public static void drops(LivingDropsEvent e) {
    if (!(e.getEntity() instanceof Cyberpsycho c)
        || !(c.level() instanceof ServerLevel level)
        || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) return;
    var inv = c.getData(Cybernetics2077.CYBERWARE);
    int count = 0, sum = 0;
    for (int s = 0; s < inv.getSlots(); s++)
      if (inv.getStackInSlot(s).getItem()
          instanceof dev.cybernetics2077.cyberware.CyberwareItem i) {
        count++;
        sum += i.tier().level;
      }
    if (count == 0) return;
    double avg = (double) sum / count;
    count = Math.min(6, count); // Existing salvage odds saturate at six implants.
    int tier = SalvageOdds.roll(avg, count, c.getRandom().nextDouble());
    e.getDrops()
        .add(
            new ItemEntity(
                level,
                c.getX(),
                c.getY(),
                c.getZ(),
                new ItemStack(Cybernetics2077.DAMAGED_PARTS.get(tier - 1).get())));
    if (c.getRandom().nextDouble() < IconicOdds.chance(avg, count))
      e.getDrops()
          .add(
              new ItemEntity(
                  level,
                  c.getX(),
                  c.getY(),
                  c.getZ(),
                  new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(tier - 1).get())));
  }

  private CyberpsychoCombat() {}
}
