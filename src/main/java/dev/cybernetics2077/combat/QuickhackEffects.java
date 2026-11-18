package dev.cybernetics2077.combat;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.AugmentRuntime;
import dev.cybernetics2077.deck.*;
import java.util.*;
import net.minecraft.core.particles.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/**
 * Bounded, server-owned hacks. DOTs refresh per kind, never recursively spread or modify terrain.
 */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class QuickhackEffects {
  private static final Map<LivingEntity, EnumMap<QuickhackKind, Effect>> ACTIVE =
      new WeakHashMap<>();

  private static final class Effect {
    final java.lang.ref.WeakReference<ServerPlayer> owner;
    final QuickhackKind kind;
    final int tier, interval;
    final float damage;
    final long end;
    long next;
    int pulses = 4;

    Effect(ServerPlayer p, QuickhackItem program, float damage, int duration, long now) {
      owner = new java.lang.ref.WeakReference<>(p);
      kind = program.kind();
      tier = program.tier().level;
      this.damage = damage;
      interval = Math.max(1, duration / 4);
      next = now + interval;
      end = now + duration;
    }
  }

  public static boolean affected(LivingEntity target, QuickhackKind kind) {
    var effects = ACTIVE.get(target);
    var effect = effects == null ? null : effects.get(kind);
    return effect != null && effect.end >= target.level().getGameTime();
  }

  private static DamageSource source(ServerPlayer p, QuickhackKind kind) {
    return new DamageSource(
        p.registryAccess()
            .registryOrThrow(Registries.DAMAGE_TYPE)
            .getHolderOrThrow(
                ResourceKey.create(Registries.DAMAGE_TYPE, Cybernetics2077.id(kind.id))),
        p,
        p);
  }

  public static void land(
      ServerPlayer p,
      LivingEntity target,
      QuickhackItem program,
      float damage,
      ItemStack sourceDeck) {
    if (!CyberwareRuntime.canAttack(p, target)) return;
    Vec3 origin = target.position().add(0, target.getBbHeight() * .6, 0);
    if (!apply(p, target, program, damage, 1)) return;
    var deck = sourceDeck.getItem() instanceof CyberdeckItem item ? item : null;
    boolean infection = program.kind() == QuickhackKind.CONTAGION;
    int count =
        (infection ? (program.tier().level == 2 ? 2 : 4) : 0)
            + (deck == null ? 0 : deck.relayTargets());
    double radius =
        infection
            ? (deck != null && deck.relayTargets() > 0 ? 16 : 8)
            : deck == null ? 0 : deck.relayRange();
    if (count == 0) return;
    var candidates =
        p.serverLevel()
            .getEntitiesOfClass(
                LivingEntity.class,
                target.getBoundingBox().inflate(radius),
                e ->
                    e != target
                        && AugmentRuntime.hostile(p, e)
                        && target.distanceToSqr(e) <= radius * radius
                        && p.distanceToSqr(e)
                            <= Math.pow(dev.cybernetics2077.scanner.ScannerNetwork.range(p) + 16, 2)
                        && target.hasLineOfSight(e));
    candidates.sort(
        Comparator.<LivingEntity>comparingDouble(target::distanceToSqr)
            .thenComparingInt(Entity::getId));
    int applied = 0;
    for (var other : candidates) {
      float fraction = infection ? 1 : deck.relayDamage();
      if (apply(p, other, program, damage * fraction, program.kind().control() ? fraction : 1)) {
        arc(p, origin, other.position().add(0, other.getBbHeight() * .6, 0), program.kind());
        if (++applied >= Math.min(7, count)) break;
      }
    }
  }

  private static boolean apply(
      ServerPlayer p, LivingEntity t, QuickhackItem q, float damage, float durationScale) {
    if (!CyberwareRuntime.canAttack(p, t)) return false;
    if (dev.cybernetics2077.cyberware.AugmentExpansion.blockHack(p, t)) return false;
    long now = t.level().getGameTime();
    switch (q.kind()) {
      case SHORT_CIRCUIT, SYNAPSE_BURNOUT -> {
        if (!t.hurt(source(p, q.kind()), damage)) return false;
      }
      case OVERHEAT, CONTAGION -> {
        if (q.kind() == QuickhackKind.OVERHEAT && t.fireImmune()) return false;
        var effects = ACTIVE.computeIfAbsent(t, k -> new EnumMap<>(QuickhackKind.class));
        var infection = effects.get(QuickhackKind.CONTAGION);
        if (q.kind() == QuickhackKind.OVERHEAT
            && infection != null
            && infection.tier >= 4
            && infection.end >= now) {
          effects.remove(QuickhackKind.CONTAGION);
          t.hurt(source(p, QuickhackKind.OVERHEAT), 8 + 2 * infection.tier);
          p.serverLevel()
              .sendParticles(
                  ParticleTypes.EXPLOSION, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
          for (var other :
              p
                  .serverLevel()
                  .getEntitiesOfClass(
                      LivingEntity.class,
                      t.getBoundingBox().inflate(3),
                      e ->
                          e != t
                              && AugmentRuntime.hostile(p, e)
                              && t.distanceToSqr(e) <= 9
                              && t.hasLineOfSight(e))
                  .stream()
                  .limit(3)
                  .toList())
            other.hurt(source(p, QuickhackKind.OVERHEAT), (8 + 2 * infection.tier) * .5f);
        }
        effects.put(q.kind(), new Effect(p, q, damage, q.duration(), now));
      }
      case REBOOT_OPTICS, CRIPPLE_MOVEMENT -> {
        int duration = Math.max(1, Math.round(q.duration() * durationScale));
        ACTIVE
            .computeIfAbsent(t, k -> new EnumMap<>(QuickhackKind.class))
            .put(q.kind(), new Effect(p, q, 0, duration, now));
        t.addEffect(
            new MobEffectInstance(
                q.kind() == QuickhackKind.REBOOT_OPTICS
                    ? MobEffects.BLINDNESS
                    : MobEffects.MOVEMENT_SLOWDOWN,
                duration,
                q.kind() == QuickhackKind.CRIPPLE_MOVEMENT ? 9 : 0,
                false,
                true));
        if (t instanceof Mob mob) {
          if (q.kind() == QuickhackKind.REBOOT_OPTICS) clearTarget(mob);
          mob.getNavigation().stop();
        }
      }
    }
    impact(p, t, q.kind());
    return true;
  }

  public static void tick(LivingEntity target) {
    var effects = ACTIVE.get(target);
    if (effects == null || target.level().isClientSide) return;
    if (!target.isAlive()) {
      ACTIVE.remove(target);
      return;
    }
    long now = target.level().getGameTime();
    var it = effects.values().iterator();
    while (it.hasNext()) {
      var effect = it.next();
      var player = effect.owner.get();
      if (player == null
          || !player.isAlive()
          || player.isRemoved()
          || player.level() != target.level()
          || !CyberwareRuntime.canAttack(player, target)) {
        it.remove();
        continue;
      }
      if (effect.kind.control()) {
        if (now >= effect.end) {
          it.remove();
          continue;
        }
        if (target instanceof Mob mob) {
          if (effect.kind == QuickhackKind.REBOOT_OPTICS) clearTarget(mob);
          else mob.getNavigation().stop();
        }
        if (effect.kind == QuickhackKind.CRIPPLE_MOVEMENT) {
          target.setDeltaMovement(0, Math.min(0, target.getDeltaMovement().y), 0);
          if (target instanceof Player p) {
            p.stopFallFlying();
            if (p.getAbilities().flying) {
              p.getAbilities().flying = false;
              p.onUpdateAbilities();
            }
          }
        }
      } else {
        if (now >= effect.next && effect.pulses > 0) {
          target.hurt(source(player, effect.kind), effect.damage / 4);
          impact(player, target, effect.kind);
          effect.next += effect.interval;
          effect.pulses--;
        }
        if (effect.pulses == 0 || now > effect.end) it.remove();
      }
    }
    if (effects.isEmpty()) ACTIVE.remove(target);
  }

  @SubscribeEvent
  public static void entityTick(EntityTickEvent.Pre e) {
    if (e.getEntity() instanceof LivingEntity living) tick(living);
  }

  @SubscribeEvent
  public static void changeTarget(LivingChangeTargetEvent e) {
    if (affected(e.getEntity(), QuickhackKind.REBOOT_OPTICS)) e.setNewAboutToBeSetTarget(null);
  }

  @SubscribeEvent
  public static void jump(LivingEvent.LivingJumpEvent e) {
    if (affected(e.getEntity(), QuickhackKind.CRIPPLE_MOVEMENT)) {
      var target = e.getEntity();
      target.setDeltaMovement(0, 0, 0);
      target.hurtMarked = true;
    }
  }

  private static void clearTarget(Mob mob) {
    mob.setTarget(null);
    mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
  }

  private static Vector3f color(QuickhackKind kind) {
    return switch (kind) {
      case OVERHEAT -> new Vector3f(1, .27f, .07f);
      case CONTAGION -> new Vector3f(.25f, 1, .35f);
      case SYNAPSE_BURNOUT -> new Vector3f(.8f, .35f, 1);
      default -> new Vector3f(.2f, .9f, 1);
    };
  }

  private static void arc(ServerPlayer p, Vec3 a, Vec3 b, QuickhackKind kind) {
    var dust = new DustParticleOptions(color(kind), .85f);
    for (int i = 0; i <= 16; i++) {
      var at = a.lerp(b, i / 16.).add(0, Math.sin(i * 2.3) * .12, Math.cos(i * 1.9) * .08);
      p.serverLevel().sendParticles(dust, at.x, at.y, at.z, 1, .01, .01, .01, 0);
    }
  }

  private static void impact(ServerPlayer p, LivingEntity target, QuickhackKind kind) {
    p.serverLevel()
        .sendParticles(
            new DustParticleOptions(color(kind), 1),
            target.getX(),
            target.getY() + target.getBbHeight() * .6,
            target.getZ(),
            12,
            .3,
            .45,
            .3,
            .05);
    if (kind == QuickhackKind.SHORT_CIRCUIT) {
      p.serverLevel()
          .sendParticles(
              ParticleTypes.ELECTRIC_SPARK,
              target.getX(),
              target.getY() + 1,
              target.getZ(),
              24,
              .35,
              .4,
              .35,
              .1);
      p.level()
          .playSound(
              null,
              target.blockPosition(),
              SoundEvents.TRIDENT_THUNDER.value(),
              SoundSource.PLAYERS,
              .3f,
              1.8f);
      PacketDistributor.sendToPlayersTrackingEntityAndSelf(
          p, new CyberwareNetwork.Impact(target.getId()));
    } else if (kind == QuickhackKind.OVERHEAT)
      p.serverLevel()
          .sendParticles(
              ParticleTypes.FLAME,
              target.getX(),
              target.getY() + .7,
              target.getZ(),
              12,
              .25,
              .4,
              .25,
              .02);
  }

  private QuickhackEffects() {}
}
