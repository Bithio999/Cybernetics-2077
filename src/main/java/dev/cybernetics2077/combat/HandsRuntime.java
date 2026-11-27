package dev.cybernetics2077.combat;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.scanner.ScannerNetwork;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Equipment-owned effects. No guided ghost projectiles, terrain removal or recursive discharges.
 */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class HandsRuntime {
  public static int smartTier(net.minecraft.world.entity.LivingEntity e) {
    return Math.max(
        AugmentRuntime.tier(e, SMART_LINK), AugmentRuntime.tier(e, TYGER_IMPRINT) > 0 ? 1 : 0);
  }

  public static final String DATA = "cybernetics2077_hands";
  public static final ResourceKey<DamageType> DISCHARGE =
      ResourceKey.create(Registries.DAMAGE_TYPE, Cybernetics2077.id("hand_discharge"));

  public static boolean ranged(ItemStack stack) {
    return dev.cybernetics2077.compat.TacZCompat.gun(stack)
        || stack.getItem() instanceof BowItem
        || stack.getItem() instanceof CrossbowItem
        || stack.getItem() instanceof TridentItem;
  }

  public static boolean melee(ItemStack stack) {
    return stack.getItem() instanceof SwordItem
        || stack.getItem() instanceof AxeItem
        || stack.getItem() instanceof TridentItem;
  }

  public static void lock(ServerPlayer p, LivingEntity target) {
    int tier = smartTier(p);
    if (tier == 0
        || !ScannerNetwork.installed(p)
        || !NeuralActors.canAttack(p, target)
        || !p.hasLineOfSight(target)
        || p.distanceToSqr(target) > Math.pow(24 + 4 * tier, 2)) return;
    var s = AugmentRuntime.state(p);
    s.smartTarget = target.getUUID();
    s.smartUntil = AugmentRuntime.now(p) + (2 + tier) * 20L;
  }

  public static LivingEntity locked(ServerPlayer p) {
    var s = AugmentRuntime.state(p);
    int tier = smartTier(p);
    if (tier == 0
        || !ScannerNetwork.installed(p)
        || s.smartTarget == null
        || AugmentRuntime.now(p) > s.smartUntil) return null;
    var target = p.serverLevel().getEntity(s.smartTarget);
    return target instanceof LivingEntity living
            && NeuralActors.canAttack(p, living)
            && !living.isInvisibleTo(p)
            && p.distanceToSqr(living) <= Math.pow(24 + 4 * tier, 2)
        ? living
        : null;
  }

  public static void primeWrap(ServerPlayer p) {
    if (AugmentRuntime.tier(p, HANDLE_WRAP) == 0 || !p.isAlive() || p.isSpectator()) return;
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    if (now < s.wrapReady) return;
    s.wrapEnd = now + 120;
    s.wrapReady = now + 200;
  }

  public static float wrapChance(ServerPlayer p) {
    return AugmentRuntime.now(p) < AugmentRuntime.state(p).wrapEnd
        ? AugmentRuntime.tier(p, HANDLE_WRAP) * .04f
        : 0;
  }

  public static void snapshot(ServerPlayer p, CompoundTag t) {
    var target = locked(p);
    var s = AugmentRuntime.state(p);
    t.putInt("smartTier", smartTier(p));
    t.putInt("ballisticTier", AugmentRuntime.tier(p, BALLISTIC_COPROCESSOR));
    t.putInt("shockTier", AugmentRuntime.tier(p, SHOCK_ABSORBER));
    t.putInt("immovableTier", AugmentRuntime.tier(p, IMMOVABLE_FORCE));
    t.putInt("microTier", AugmentRuntime.tier(p, MICROGENERATOR));
    t.putLong("microReady", s.microReady);
    t.putLong("wrapEnd", s.wrapEnd);
    t.putInt("smartTarget", target == null ? -1 : target.getId());
    t.putString("smartName", target == null ? "" : target.getName().getString());
    t.putLong("smartUntil", s.smartUntil);
  }

  public static boolean clear(ServerLevel level, Vec3 from, Vec3 to, Entity context) {
    return level
            .clip(
                new ClipContext(
                    from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context))
            .getType()
        == HitResult.Type.MISS;
  }

  public static void launch(AbstractArrow arrow, LivingEntity p) {
    var data = arrow.getPersistentData();
    if (data.contains(DATA)) return;
    var t = new CompoundTag();
    data.put(DATA, t); // Mark once, including shots with no implants.
    if (!p.isAlive() || p.isSpectator()) return;
    var target =
        p instanceof ServerPlayer player
            ? locked(player)
            : p instanceof Mob m ? dev.cybernetics2077.mob.MobImplants.smartTarget(m) : null;
    int smart = smartTier(p);
    if (target != null && p.hasLineOfSight(target)) {
      t.putUUID("target", target.getUUID());
      t.putInt("smart", smart);
    }
    t.putInt(
        "bounce",
        arrow instanceof ThrownTrident ? 0 : AugmentRuntime.tier(p, BALLISTIC_COPROCESSOR));
    t.putInt("flat", arrow instanceof ThrownTrident ? 0 : AugmentRuntime.tier(p, IMMOVABLE_FORCE));
    t.putInt("micro", AugmentRuntime.tier(p, MICROGENERATOR));
    t.putBoolean(
        "modified",
        smart > 0 || t.getInt("bounce") > 0 || t.getInt("flat") > 0 || t.getInt("micro") > 0);
    if (arrow instanceof ThrownTrident && p instanceof ServerPlayer player) primeWrap(player);
  }

  @SubscribeEvent
  public static void joined(EntityJoinLevelEvent e) {
    if (!e.getLevel().isClientSide
        && e.getEntity() instanceof AbstractArrow a
        && a.getOwner() instanceof LivingEntity p) launch(a, p);
  }

  @SubscribeEvent
  public static void tick(EntityTickEvent.Pre e) {
    if (!(e.getEntity() instanceof AbstractArrow arrow)
        || !(arrow.level() instanceof ServerLevel level)) return;
    var t = arrow.getPersistentData().getCompound(DATA);
    // Complete the bank even if its owner logs out between impact and this tick.
    if (t.getBoolean("pendingBounce")) {
      arrow.setDeltaMovement(t.getDouble("vx"), t.getDouble("vy"), t.getDouble("vz"));
      t.remove("pendingBounce");
      arrow.hasImpulse = true;
    }
    if (!(arrow.getOwner() instanceof LivingEntity p) || p.level() != level) return;
    if (!arrow.getPersistentData().contains(DATA)) {
      if (arrow.tickCount > 2) return;
      launch(arrow, p);
      t = arrow.getPersistentData().getCompound(DATA);
    }
    if (t.getBoolean("landed")
        || arrow.isNoPhysics()
        || arrow.tickCount > 80
        || arrow.getDeltaMovement().lengthSqr() < .01
        || !p.isAlive()
        || p.isSpectator()) return;
    if (t.getInt("lastTick") == arrow.tickCount + 1) return;
    t.putInt("lastTick", arrow.tickCount + 1);
    if (t.contains("target") && arrow.tickCount < 40) {
      var candidate = level.getEntity(t.getUUID("target"));
      if (candidate instanceof LivingEntity target
          && NeuralActors.canAttack(p, target)
          && !(p instanceof net.minecraft.world.entity.player.Player viewer
              && target.isInvisibleTo(viewer))
          && arrow.distanceToSqr(target) <= 4096
          && clear(level, arrow.position(), target.getBoundingBox().getCenter(), arrow)) {
        var v = arrow.getDeltaMovement();
        var d = target.getBoundingBox().getCenter().subtract(arrow.position());
        var movement = target.position().subtract(new Vec3(target.xo, target.yo, target.zo));
        var turn =
            HandsRules.guide(
                v.x,
                v.y,
                v.z,
                d.x,
                d.y,
                d.z,
                movement.x,
                movement.y,
                movement.z,
                t.getInt("smart"),
                1);
        arrow.setDeltaMovement(turn.x(), turn.y(), turn.z());
        arrow.hasImpulse = true;
        if (arrow.tickCount % 4 == 0)
          level.sendParticles(
              ParticleTypes.END_ROD, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0, 0, 0, 0);
      } else t.remove("target");
    }
    if (t.getInt("flat") > 0 && !arrow.isNoGravity())
      arrow.setDeltaMovement(
          arrow.getDeltaMovement().add(0, .05 * HandsRules.gravityReduction(t.getInt("flat")), 0));
  }

  public static Vec3 bounceDirection(AbstractArrow arrow, BlockHitResult hit, int tier) {
    Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal()),
        origin = hit.getLocation().add(normal.scale(.06));
    var v = arrow.getDeltaMovement();
    var reflected = HandsRules.reflect(v.x, v.y, v.z, normal.x, normal.y, normal.z);
    Vec3 direction = new Vec3(reflected.x(), reflected.y(), reflected.z()).normalize();
    return direction.scale(v.length() * HandsRules.bounceSpeed(tier));
  }

  @SubscribeEvent
  public static void impact(ProjectileImpactEvent e) {
    if (!(e.getProjectile() instanceof AbstractArrow arrow)
        || !(arrow.level() instanceof ServerLevel level)
        || !(arrow.getOwner() instanceof LivingEntity p)) return;
    var t = arrow.getPersistentData().getCompound(DATA);
    if (!t.getBoolean("modified")) return;
    if (e.getRayTraceResult() instanceof EntityHitResult hit) {
      t.putBoolean("landed", true);
      t.remove("target");
      if (hit.getEntity() instanceof LivingEntity target && !NeuralActors.canAttack(p, target)) {
        e.setCanceled(true);
        arrow.setDeltaMovement(arrow.getDeltaMovement().scale(-.1));
      }
      return;
    }
    if (!(e.getRayTraceResult() instanceof BlockHitResult hit)) return;
    int tier = t.getInt("bounce");
    if (tier == 0
        || t.getBoolean("bounced")
        || arrow instanceof ThrownTrident
        || arrow.getDeltaMovement().lengthSqr() < .04) {
      t.putBoolean("landed", true);
      return;
    }
    var normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
    var velocity = bounceDirection(arrow, hit, tier);
    e.setCanceled(true);
    t.putBoolean("bounced", true);
    t.remove("target");
    arrow.setPos(hit.getLocation().add(normal.scale(.06)));
    t.putBoolean("pendingBounce", true);
    t.putDouble("vx", velocity.x);
    t.putDouble("vy", velocity.y);
    t.putDouble("vz", velocity.z);
    arrow.setDeltaMovement(Vec3.ZERO);
    arrow.hasImpulse = true;
    arrow.setBaseDamage(arrow.getBaseDamage() * (1 + .05 * tier));
    level.sendParticles(
        ParticleTypes.ELECTRIC_SPARK,
        hit.getLocation().x,
        hit.getLocation().y,
        hit.getLocation().z,
        8,
        .05,
        .05,
        .05,
        .015);
  }

  @SubscribeEvent
  public static void equipped(LivingEquipmentChangeEvent e) {
    if (e.getEntity() instanceof ServerPlayer p
        && e.getSlot() == EquipmentSlot.MAINHAND
        && melee(e.getTo())
        && !ItemStack.isSameItem(e.getFrom(), e.getTo())) primeWrap(p);
  }

  @SubscribeEvent
  public static void charging(LivingEntityUseItemEvent.Tick e) {
    var entity = e.getEntity();
    if (!ranged(e.getItem())) return;
    int tier =
        entity instanceof ServerPlayer p
            ? AugmentRuntime.tier(p, IMMOVABLE_FORCE)
            : clientImmovable.applyAsInt(entity);
    if (tier > 0
        && entity.level().getGameTime() % HandsRules.chargePeriod(tier) == 0
        && e.getDuration() > 1) e.setDuration(e.getDuration() - 1);
  }

  public static java.util.function.ToIntFunction<LivingEntity> clientImmovable = p -> 0;

  @SubscribeEvent
  public static void knockback(LivingKnockBackEvent e) {
    if (e.getEntity() instanceof ServerPlayer p
        && (p.isUsingItem() && ranged(p.getUseItem())
            || dev.cybernetics2077.compat.TacZCompat.aiming(p))) {
      int t = AugmentRuntime.tier(p, SHOCK_ABSORBER);
      if (t > 0) e.setStrength(e.getStrength() * (1 - (.3f + .1f * t)));
    }
  }

  public static boolean weaponSource(DamageSource source) {
    return source.is(DamageTypes.PLAYER_ATTACK)
        || MantisVariant.elemental(source)
        || source.getDirectEntity() instanceof AbstractArrow
            && source.is(DamageTypeTags.IS_PROJECTILE);
  }

  @SubscribeEvent
  public static void hit(LivingDamageEvent.Post e) {
    if (e.getNewDamage() <= 0
        || !(e.getSource().getEntity() instanceof ServerPlayer p)
        || !p.isAlive()
        || !weaponSource(e.getSource())
        || p == e.getEntity()
        || p.isAlliedTo(e.getEntity())) return;
    int tier =
        e.getSource().getDirectEntity() instanceof AbstractArrow a
            ? a.getPersistentData().getCompound(DATA).getInt("micro")
            : AugmentRuntime.tier(p, MICROGENERATOR);
    discharge(p, e.getEntity(), tier);
  }

  public static void discharge(ServerPlayer p, LivingEntity main, int tier) {
    if (!p.isAlive() || p.isSpectator() || main == p || p.isAlliedTo(main)) return;
    if (main instanceof net.minecraft.world.entity.player.Player other
        && (!p.server.isPvpAllowed() || !p.canHarmPlayer(other))) return;
    var s = AugmentRuntime.state(p);
    long now = AugmentRuntime.now(p);
    if (tier == 0 || now < s.microReady) return;
    s.microReady = now + (12 - tier) * 20L;
    var point = main.getBoundingBox().getCenter();
    var targets =
        p.serverLevel()
            .getEntitiesOfClass(
                LivingEntity.class,
                main.getBoundingBox().inflate(3),
                target ->
                    NeuralActors.canAttack(p, target)
                        && target.distanceToSqr(point) <= 9
                        && (target == main || AugmentRuntime.hostile(p, target))
                        && clear(p.serverLevel(), point, target.getBoundingBox().getCenter(), p));
    targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(point)));
    var source =
        new DamageSource(
            p.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DISCHARGE),
            p,
            p);
    for (var target : targets.stream().limit(4).toList())
      SecondaryDamage.hurt(target, source, 2 + 2 * tier);
    p.serverLevel()
        .sendParticles(
            ParticleTypes.ELECTRIC_SPARK, point.x, point.y, point.z, 28, .7, .5, .7, .04);
  }

  @SubscribeEvent
  public static void secondaryInvulnerability(LivingIncomingDamageEvent e) {
    if (e.getSource().is(DISCHARGE) || e.getSource().is(MantisEffects.BLEED))
      e.setInvulnerabilityTicks(0);
  }

  private HandsRuntime() {}
}
