package dev.cybernetics2077.cyberware;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;

/** Passive cyberware hooks; medical payments and defensive cooldowns are owned by the server. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class AugmentExpansion {
  public static final ResourceKey<DamageType> DISCHARGE =
      ResourceKey.create(Registries.DAMAGE_TYPE, Cybernetics2077.id("cybernetic_discharge"));
  public static java.util.function.ToIntFunction<Player> clientLynx = p -> 0;

  public static float stepVolume(Entity entity, float original) {
    if (!(entity instanceof Player player)) return original;
    int t =
        player.level().isClientSide
            ? clientLynx.applyAsInt(player)
            : AugmentRuntime.tier(player, LYNX_PAWS);
    return t > 0 ? original * .5f : original;
  }

  public static float ramRecovery(Player player) {
    return AugmentRuntime.tier(player, RAM_UPGRADE) * .06f;
  }

  public static boolean blockHack(LivingEntity attacker, LivingEntity defender) {
    if (attacker == defender) return false;
    int t = AugmentRuntime.tier(defender, SELF_ICE);
    var state = AugmentRuntime.state(defender);
    long now = defender.level().getGameTime();
    if (t == 0 || now < state.iceReady) return false;
    state.iceReady =
        ExpansionRuntime.finished(
            defender, ExpansionRuntime.cooldown(defender, now + (50 - 6 * t) * 20L));
    if (defender instanceof ServerPlayer p)
      p.displayClientMessage(Component.translatable("cybernetics2077.self_ice.blocked"), true);
    if (attacker instanceof ServerPlayer p)
      p.displayClientMessage(Component.translatable("cybernetics2077.self_ice.rejected"), true);
    ((net.minecraft.server.level.ServerLevel) defender.level())
        .sendParticles(
            ParticleTypes.ELECTRIC_SPARK,
            defender.getX(),
            defender.getEyeY(),
            defender.getZ(),
            18,
            .3,
            .3,
            .3,
            .02);
    return true;
  }

  public static void tick(ServerPlayer p) {
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    if (s.jenkinsClock > 0 && now > s.jenkinsClock + 1)
      s.jenkinsFatigue = (int) Math.max(0, s.jenkinsFatigue - (now - s.jenkinsClock - 1));
    boolean newTick = s.jenkinsClock != now;
    s.jenkinsClock = now;
    int j = AugmentRuntime.tier(p, JENKINS_TENDONS);
    if (p.isAlive() && !p.isSpectator()) {
      if (newTick && j > 0 && p.isSprinting())
        s.jenkinsFatigue = Math.min(100, s.jenkinsFatigue + 1);
      else if (newTick) s.jenkinsFatigue = Math.max(0, s.jenkinsFatigue - 1);
      if (now % 10 == 0
          && AugmentRuntime.tier(p, BIOMONITOR) > 0
          && p.getHealth() < p.getMaxHealth() * .5f
          && now >= s.biomonitorReady) {
        // Reserve the debounce before triggering a pump; never recursively spend two charges.
        s.biomonitorReady = now + 40;
        if (!AugmentRuntime.usePump(p) && !AugmentRuntime.berserk(p)) autoPotion(p);
      }
    }
  }

  private static boolean autoPotion(ServerPlayer p) {
    for (int slot = 0; slot < 36; slot++) {
      var stack = p.getInventory().getItem(slot);
      if (!stack.is(Items.POTION)) continue;
      boolean healing = false, harmful = false;
      for (var effect :
          stack
              .getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
              .getAllEffects()) {
        harmful |= effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL;
        healing |=
            effect.is(MobEffects.HEAL)
                || effect.is(MobEffects.REGENERATION) && !p.hasEffect(MobEffects.REGENERATION);
      }
      if (!healing || harmful) continue;
      var result = stack.getItem().finishUsingItem(stack, p.serverLevel(), p);
      p.getInventory().setItem(slot, result);
      p.getInventory().setChanged();
      p.level()
          .playSound(
              null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, .5f, 1.2f);
      return true;
    }
    return false;
  }

  public static double sprintBonus(ServerPlayer p) {
    int t = AugmentRuntime.tier(p, JENKINS_TENDONS);
    if (t == 0 || !p.isSprinting() || !p.isAlive() || p.isSpectator()) return 0;
    double charge = 1 - AugmentRuntime.state(p).jenkinsFatigue / 100.;
    return t * (.02 + .10 * charge);
  }

  /** Probabilities combine independently, with a hard 50% cyberware chance cap. */
  public static float weaponCritChance(ServerPlayer p, Entity target, boolean blade) {
    float external = ExpansionRuntime.critChance(p, target);
    float stab = blade ? AugmentRuntime.tier(p, STABBER) * .04f : 0;
    float distance = (float) Math.clamp(p.distanceTo(target) / 32., 0, 1);
    float visual = AugmentRuntime.tier(p, VISUAL_CORTEX_SUPPORT) * .06f * distance;
    float berserk = BerserkSpecs.critChance(p);
    float sandy =
        dev.cybernetics2077.temporal.Sandevistan.active(p)
            ? dev.cybernetics2077.temporal.Sandevistan.model(p)
                .critChance(dev.cybernetics2077.temporal.Sandevistan.tier(p))
            : 0;
    return 1
        - (1 - external)
            * (1
                - Math.min(
                    .5f,
                    1
                        - (1 - stab)
                            * (1 - visual)
                            * (1 - berserk)
                            * (1 - sandy)
                            * (1 - dev.cybernetics2077.combat.HandsRuntime.wrapChance(p))));
  }

  public static float critMultiplier(ServerPlayer p) {
    return 1.5f
        * (dev.cybernetics2077.temporal.Sandevistan.active(p)
            ? 1
                + dev.cybernetics2077.temporal.Sandevistan.model(p)
                    .critDamage(dev.cybernetics2077.temporal.Sandevistan.tier(p))
            : 1)
        * BerserkSpecs.critDamage(p);
  }

  @SubscribeEvent
  public static void incoming(LivingIncomingDamageEvent e) {
    if (e.getSource().getEntity() instanceof ServerPlayer attacker) {
      if ((e.getSource().is(DamageTypes.PLAYER_ATTACK) || MantisVariant.elemental(e.getSource()))
          && e.getAmount() > 0) {
        int dense = AugmentRuntime.tier(attacker, DENSE_MARROW);
        if (dense > 0) {
          e.setAmount(e.getAmount() * (1 + dense * .06f));
          attacker.causeFoodExhaustion(dense * .004f);
        }
      } else if (e.getSource().getDirectEntity() instanceof Projectile projectile
          && e.getSource().is(DamageTypeTags.IS_PROJECTILE)
          && !dev.cybernetics2077.compat.TacZCompat.bullet(projectile)
          && attacker.getRandom().nextFloat()
              < weaponCritChance(attacker, e.getEntity(), projectile instanceof ThrownTrident)) {
        e.setAmount(e.getAmount() * critMultiplier(attacker));
        attacker
            .serverLevel()
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
    }
  }

  @SubscribeEvent
  public static void healing(LivingHealEvent e) {
    if (e.getEntity() instanceof ServerPlayer p && e.getAmount() > 0)
      e.setAmount(e.getAmount() * (1 + AugmentRuntime.tier(p, BIOMONITOR) * .04f));
  }

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void revive(LivingDeathEvent e) {
    if (!(e.getEntity() instanceof ServerPlayer p)
        || p.isSpectator()
        || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    int t = AugmentRuntime.tier(p, SECOND_HEART);
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    if (t == 0 || now < s.heartReady) return;
    s.heartReady = now + (400 - 40 * t) * 20L;
    e.setCanceled(true);
    p.setHealth(p.getMaxHealth());
    p.clearFire();
    p.invulnerableTime = Math.max(20, p.invulnerableTime);
    p.serverLevel()
        .sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1, p.getZ(), 20, .5, .7, .5, .02);
    p.level()
        .playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, .7f, 1.2f);
    p.displayClientMessage(Component.translatable("cybernetics2077.second_heart.revived"), true);
    CyberwareRuntime.syncIfChanged(p);
  }

  @SubscribeEvent
  public static void discharge(LivingDamageEvent.Post e) {
    if (!(e.getEntity() instanceof ServerPlayer p)
        || !p.isAlive()
        || e.getNewDamage() <= 0
        || !(e.getSource().getEntity() instanceof LivingEntity attacker)
        || attacker == p
        || e.getSource().is(DISCHARGE)
        || !CyberwareRuntime.canAttack(p, attacker)) return;
    int t = AugmentRuntime.tier(p, SHOCK_N_AWE);
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    if (t == 0 || now < s.shockReady || p.getRandom().nextFloat() >= t * .02f) return;
    s.shockReady = now + 40;
    var targets =
        p.serverLevel()
            .getEntitiesOfClass(
                LivingEntity.class,
                p.getBoundingBox().inflate(4),
                other ->
                    other != p
                        && p.distanceToSqr(other) <= 16
                        && CyberwareRuntime.canAttack(p, other)
                        && (other == attacker || AugmentRuntime.hostile(p, other))
                        && p.hasLineOfSight(other));
    targets.sort(Comparator.comparingDouble(p::distanceToSqr));
    var source =
        new DamageSource(
            p.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DISCHARGE),
            p,
            p);
    for (var target : targets.stream().limit(8).toList()) target.hurt(source, 2 + 2 * t);
    p.serverLevel()
        .sendParticles(
            ParticleTypes.ELECTRIC_SPARK, p.getX(), p.getY() + 1, p.getZ(), 48, 1.5, .7, 1.5, .06);
    p.level()
        .playSound(
            null,
            p.blockPosition(),
            SoundEvents.TRIDENT_THUNDER.value(),
            SoundSource.PLAYERS,
            .4f,
            1.8f);
  }

  private AugmentExpansion() {}
}
