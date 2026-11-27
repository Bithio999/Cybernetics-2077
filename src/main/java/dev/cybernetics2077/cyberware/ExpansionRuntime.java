package dev.cybernetics2077.cyberware;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.deck.*;
import dev.cybernetics2077.temporal.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Shared conditional implants. Cooldowns, delayed wounds and RAM remain server-authoritative. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class ExpansionRuntime {
  public static java.util.function.Function<LivingEntity, CompoundTag> client =
      e -> new CompoundTag();

  public static int tier(LivingEntity e, Augment a) {
    return AugmentRuntime.tier(e, a);
  }

  public static CompoundTag extra(LivingEntity e) {
    return AugmentRuntime.state(e).extra;
  }

  public static ItemStack deck(LivingEntity e) {
    return e.getData(Cybernetics2077.CYBERWARE)
        .getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot());
  }

  public static boolean operational(LivingEntity e) {
    return e.isAlive()
        && !e.isSpectator()
        && !(e instanceof Mob m && (m.isNoAi() || m.isInvulnerable()));
  }

  public static int maxRam(LivingEntity e) {
    if (!(deck(e).getItem() instanceof CyberdeckItem d) || disabled(e)) return 0;
    return Math.max(
        1,
        d.ram()
            + tier(e, EX_DISK)
            + (tier(e, MEMORY_BOOST) > 0 ? 1 : 0)
            + (tier(e, RAM_UPGRADE) > 0 ? 1 + tier(e, RAM_UPGRADE) / 3 : 0)
            + (tier(e, SELF_ICE) > 0 ? 2 : 0)
            + (tier(e, MECHATRONIC_CORE) > 0 ? 2 : 0)
            + (tier(e, CAMILLO_RAM_MANAGER) > 0 ? 2 : 0)
            + (tier(e, RAM_REALLOCATOR) > 0 ? 2 : 0)
            + (tier(e, RAM_RECOUP) > 0 ? 2 : 0)
            - (tier(e, BIOCONDUCTOR) > 0 || tier(e, COX_2) > 0 ? 4 : 0));
  }

  public static float ram(LivingEntity e) {
    return e instanceof ServerPlayer p
        ? CyberwareRuntime.state(p).ram
        : extra(e).contains("ram") ? extra(e).getFloat("ram") : maxRam(e);
  }

  public static void ram(LivingEntity e, float n) {
    n = Math.clamp(n, 0, maxRam(e));
    if (e instanceof ServerPlayer p) CyberwareRuntime.state(p).ram = n;
    else extra(e).putFloat("ram", n);
  }

  public static float ramRate(LivingEntity e) {
    if (!(deck(e).getItem() instanceof CyberdeckItem d) || disabled(e)) return 0;
    return (d.ramPerSecond() + tier(e, RAM_UPGRADE) * .06f)
        * (ram(e) < 7 ? 1 + tier(e, FEEN_X) * .5f : 1);
  }

  public static boolean disabled(LivingEntity e) {
    return (e.level().isClientSide ? client.apply(e) : extra(e)).getLong("disabledUntil")
            > e.level().getGameTime()
        || QuickhackEffects.affected(e, QuickhackKind.CYBERWARE_MALFUNCTION);
  }

  public static boolean jammed(LivingEntity e) {
    return (e.level().isClientSide ? client.apply(e) : extra(e)).getLong("jammedUntil")
            > e.level().getGameTime()
        || QuickhackEffects.affected(e, QuickhackKind.WEAPON_GLITCH);
  }

  public static double temporalStrength(LivingEntity e) {
    if (!operational(e)) return 1;
    var x = e.level().isClientSide ? client.apply(e) : extra(e);
    return x.getLong("chronoEnd") > e.level().getGameTime()
        ? Math.clamp(x.getDouble("chronoStrength"), 1, 20)
        : 1;
  }

  public static double personalStrength(LivingEntity e) {
    var x = e.level().isClientSide ? client.apply(e) : extra(e);
    return x.getBoolean("chronoSelf") ? 1 : temporalStrength(e);
  }

  public static void chrono(
      LivingEntity e, double slow, int ticks, boolean self, String reason, int cooldown) {
    var x = extra(e);
    long now = e.level().getGameTime();
    if (now < x.getLong(reason + "Ready") || disabled(e)) return;
    x.putLong(reason + "Ready", now + cooldown);
    if (now + ticks > x.getLong("chronoEnd")) {
      x.putLong("lastSync", now - 10);
      x.putLong("chronoEnd", now + ticks);
      x.putDouble("chronoStrength", TemporalRules.acceleration(slow));
      x.putBoolean("chronoSelf", self);
      x.putString("chronoKind", reason);
      if (e instanceof Mob m) Sandevistan.trackMob(m);
      Sandevistan.invalidate(e.level());
    }
  }

  public static void modifier(
      LivingEntity e,
      net.minecraft.core.Holder<Attribute> a,
      String id,
      double value,
      AttributeModifier.Operation op) {
    var attr = e.getAttribute(a);
    if (attr == null) return;
    var key = Cybernetics2077.id("expansion_" + id);
    var old = attr.getModifier(key);
    if (old != null && old.amount() == value && old.operation() == op) return;
    attr.removeModifier(key);
    if (value != 0) attr.addTransientModifier(new AttributeModifier(key, value, op));
  }

  private static LivingEntity enemy(LivingEntity e, double radius) {
    return e
        .level()
        .getEntitiesOfClass(
            LivingEntity.class,
            e.getBoundingBox().inflate(radius),
            o -> NeuralActors.hostile(e, o) && e.distanceToSqr(o) <= radius * radius)
        .stream()
        .min(Comparator.comparingDouble(e::distanceToSqr))
        .orElse(null);
  }

  public static double gunRate(LivingEntity e) {
    if (e.level().isClientSide)
      return client.apply(e).contains("gunRate") ? client.apply(e).getDouble("gunRate") : 1;
    return 1
        + tier(e, CLUTCH_PADDING) * .04
        + (tier(e, ISOMETRIC_STABILIZER) > 0 ? .2 : 0)
        + (e.level().getGameTime() < extra(e).getLong("boosterEnd") ? .1 : 0);
  }

  public static double meleeRate(LivingEntity e) {
    return 1
        + tier(e, MICROROTORS) * .05
        + (tier(e, ISOMETRIC_STABILIZER) > 0 ? .2 : 0)
        + (e.level().getGameTime() < extra(e).getLong("boosterEnd") ? .1 : 0);
  }

  public static float critChance(LivingEntity e, Entity target) {
    float c = tier(e, COCKATRICE) > 0 ? .35f : 0,
        d = tier(e, DEEP_FIELD) > 0 ? (float) Math.clamp(e.distanceTo(target) / 100., 0, 1) : 0;
    return 1 - (1 - c) * (1 - d);
  }

  public static float attackBonus(
      LivingEntity e, LivingEntity target, boolean ranged, boolean headshot, boolean precision) {
    float n = 1;
    if (tier(e, MECHATRONIC_CORE) > 0
        && (target instanceof net.minecraft.world.entity.animal.IronGolem
            || dev.cybernetics2077.mob.EnhancedMobs.enhanced(target)
            || target instanceof dev.cybernetics2077.mob.Cyberpsycho))
      n *= 1 + tier(e, MECHATRONIC_CORE) * .08f;
    if (tier(e, BLACK_MAMBA) > 0 && target.hasEffect(MobEffects.POISON))
      n *= 1 + tier(e, BLACK_MAMBA) * .044f;
    if (ranged && headshot && e.level().getGameTime() < extra(e).getLong("tyrosineEnd"))
      n *= 1 + tier(e, TYROSINE_INJECTOR) * .042f;
    if (ranged && precision) n *= 1 + tier(e, CELLULAR_ADAPTER) * .02f;
    if (ranged
        && headshot
        && Sandevistan.active(e)
        && Sandevistan.model(e) == SandevistanModel.ZETATECH
        && !e.onGround()) n *= 1 + tier(e, ZETATECH_SANDEVISTAN) * .08f;
    if (Sandevistan.active(e) && Sandevistan.model(e) == SandevistanModel.FALCON) n *= 1.1f;
    if (!ranged && BerserkSpecs.active(e) && ExpansionImplants.os(e) == MILITECH_BERSERK)
      n *= 1 + .5f * (1 - Math.clamp(e.getHealth() / e.getMaxHealth(), 0, .8f) / .8f);
    if (deck(e).getItem() instanceof CyberdeckItem d
        && d.model() == CyberdeckItem.Model.TETRATRONIC
        && Arrays.stream(QuickhackKind.values())
            .anyMatch(k -> k.control() && QuickhackEffects.affected(target, k))) n *= 1.15f;
    return n;
  }

  public static void precisionHit(LivingEntity e, LivingEntity target) {
    long now = e.level().getGameTime();
    var x = extra(e);
    if (now < x.getLong("feedbackReady")) return;
    int f = tier(e, FEEDBACK_CIRCUIT), r = tier(e, ELECTROMAG_RECYCLER);
    if (f == 0 && r == 0) return;
    x.putLong("feedbackReady", now + 20);
    e.heal(e.getMaxHealth() * (r > 0 ? .05f : f * .007f));
    if (r > 0 && e instanceof Player p) p.getFoodData().eat(1, .2f);
  }

  public static void reduceCooldowns(LivingEntity e, double fraction) {
    var s = AugmentRuntime.state(e);
    long n = e.level().getGameTime();
    var x = extra(e);
    for (String key :
        new String[] {
          "berserkReady",
          "camoReady",
          "heartReady",
          "monitorReady",
          "iceReady",
          "adrenalineReady",
          "microReady",
          "reflexReady",
          "synapticReady",
          "kerenzikovReady"
        })
      if (x.getLong(key) > n) x.putLong(key, n + (long) ((x.getLong(key) - n) * (1 - fraction)));
    s.heartReady = n + (long) (Math.max(0, s.heartReady - n) * (1 - fraction));
    s.berserkReady = n + (long) (Math.max(0, s.berserkReady - n) * (1 - fraction));
    s.camoReady = n + (long) (Math.max(0, s.camoReady - n) * (1 - fraction));
    s.iceReady = n + (long) (Math.max(0, s.iceReady - n) * (1 - fraction));
    if (s.pumpNext > n) s.pumpNext = n + (long) ((s.pumpNext - n) * (1 - fraction));
    if (s.sandyEnd <= n)
      s.sandyClock -= (long) (20 * Sandevistan.profile(s).recharge(s.sandyProfile) * fraction);
  }

  public static long cooldown(LivingEntity e, long ready) {
    long n = e.level().getGameTime();
    return n + (long) ((ready - n) * (tier(e, QUANTUM_TUNER) > 0 ? .85 : 1));
  }

  public static long finished(LivingEntity e, long ready) {
    var x = extra(e);
    if (tier(e, QUANTUM_TUNER) > 0
        && x.getBoolean("quantumAvailable")
        && ready > e.level().getGameTime()) {
      x.putBoolean("quantumAvailable", false);
      x.putLong("lastCombat", e.level().getGameTime());
      return Math.max(e.level().getGameTime(), ready - 1000);
    }
    return ready;
  }

  public static boolean faceplate(ServerPlayer p) {
    var x = extra(p);
    long n = p.level().getGameTime();
    if (tier(p, BEHAVIORAL_FACEPLATE) == 0
        || n < x.getLong("faceplateReady")
        || n - Math.max(x.getLong("lastAttack"), x.getLong("lastDamage")) < 100) return false;
    x.putLong("disguiseEnd", n + 600);
    x.putLong("faceplateReady", cooldown(p, n + 18000));
    for (var m : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32)))
      if (m.getTarget() == p) m.setTarget(null);
    p.displayClientMessage(
        net.minecraft.network.chat.Component.literal(
            "IDENTITY MASKED // Attacking breaks the disguise"),
        true);
    return true;
  }

  public static void medicalUsed(LivingEntity e) {
    if (tier(e, UNIVERSAL_BOOSTER) > 0)
      extra(e).putLong("boosterEnd", e.level().getGameTime() + 100);
  }

  public static void tick(LivingEntity e) {
    if (!(e.level() instanceof ServerLevel) || !operational(e)) return;
    var x = extra(e);
    long n = e.level().getGameTime();
    boolean combat =
        n - x.getLong("lastDamage") < 100
            || n - x.getLong("lastAttack") < 100
            || e instanceof Mob m && m.getTarget() != null;
    if (!combat)
      combat =
          e.level()
                  .getEntitiesOfClass(
                      Mob.class, e.getBoundingBox().inflate(24), m -> m.getTarget() == e)
                  .size()
              > 0;
    if (combat) x.putLong("lastCombat", n);
    if (tier(e, QUANTUM_TUNER) > 0 && !x.getBoolean("quantumInitialized")) {
      x.putBoolean("quantumInitialized", true);
      x.putBoolean("quantumAvailable", true);
    }
    if (!combat && tier(e, QUANTUM_TUNER) > 0 && n - x.getLong("lastCombat") >= 1200)
      x.putBoolean("quantumAvailable", true);
    int reflex = Math.max(tier(e, REFLEX_TUNER), tier(e, REVULSOR));
    if (reflex > 0 && e.getHealth() <= e.getMaxHealth() * .25)
      chrono(e, .6, 45 + reflex * 9, tier(e, REVULSOR) == 0, "reflex", 700);
    if (combat && !x.getBoolean("wasCombat")) {
      if (tier(e, SYNAPTIC_ACCELERATOR) > 0)
        chrono(e, .5, 40 + tier(e, SYNAPTIC_ACCELERATOR) * 10, true, "synaptic", 1200);
      if (tier(e, ADRENO_TRIGGER) > 0 && n >= x.getLong("adrenoReady")) {
        x.putLong("adrenoEnd", n + (15 + 5 * tier(e, ADRENO_TRIGGER)) * 20);
        x.putLong("adrenoReady", n + 800);
      }
    }
    x.putBoolean("wasCombat", combat);
    boolean aim =
        e instanceof ServerPlayer p
            ? (dev.cybernetics2077.compat.TacZCompat.aiming(p) || p.isUsingItem())
            : e instanceof Mob m && m.getTarget() != null && e.isUsingItem();
    if (aim) {
      if (!x.getBoolean("aiming")) x.putLong("aimStart", n);
    } else x.putLong("aimStart", n);
    x.putBoolean("aiming", aim);
    if (tier(e, KERENZIKOV) > 0 && aim && (e.isSprinting() || !e.onGround()))
      chrono(
          e,
          tier(e, KERENZIKOV_BOOST) > 0 ? .7 : .6,
          40 + 7 * tier(e, KERENZIKOV) + (tier(e, KERENZIKOV_BOOST) > 0 ? 20 : 0),
          false,
          "kerenzikov",
          120);
    if (x.getLong("chronoEnd") > 0 && n >= x.getLong("chronoEnd") && !x.getBoolean("chronoEnded")) {
      if (x.getString("chronoKind").equals("kerenzikov") && tier(e, DEFENZIKOV) > 0)
        x.putLong("defenzikovEnd", n + 80);
      x.putBoolean("chronoEnded", true);
      Sandevistan.invalidate(e.level());
    } else if (n < x.getLong("chronoEnd")) x.putBoolean("chronoEnded", false);
    if (n % 10 == 0) {
      double move =
          (BerserkSpecs.active(e) && ExpansionImplants.os(e) == MILITECH_BERSERK ? .2 : 0)
              + (tier(e, LEEROY_LIGAMENT) > 0 ? .2 : 0)
              + (n < x.getLong("adrenoEnd") ? .3 : 0)
              + (n < x.getLong("tyrosineEnd") ? tier(e, TYROSINE_INJECTOR) * .022 : 0);
      if (e.getHealth() / e.getMaxHealth() < .25)
        move += tier(e, THREATEVAC) * .078 * (1 - e.getHealth() / e.getMaxHealth());
      if (!combat && tier(e, ATOMIC_SENSORS) > 0) {
        var near = enemy(e, 16);
        if (near != null)
          move += tier(e, ATOMIC_SENSORS) * .128 * (1 - Math.min(1, e.distanceTo(near) / 16));
      }
      if (e.isShiftKeyDown() && tier(e, SCARAB) > 0) move -= .2;
      modifier(
          e,
          Attributes.MOVEMENT_SPEED,
          "movement",
          move,
          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      modifier(
          e,
          Attributes.ATTACK_SPEED,
          "rotors",
          meleeRate(e) - 1,
          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
      double armor =
          (e.isShiftKeyDown() ? tier(e, SCARAB) * 1.2 : 0)
              + (deck(e).getItem() instanceof CyberdeckItem && ram(e) < 7
                  ? tier(e, COGITO_LATTICE) * 1.5
                  : 0);
      if (tier(e, RANGEGUARD) > 0 && enemy(e, 6) == null) armor += tier(e, RANGEGUARD) * 1.2;
      modifier(e, Attributes.ARMOR, "conditional", armor, AttributeModifier.Operation.ADD_VALUE);
      modifier(
          e,
          Attributes.ARMOR,
          "total",
          (tier(e, RARA_AVIS) > 0 ? .4 : 0)
              + (e.getHealth() < e.getMaxHealth() * .5 ? tier(e, SCAR_COALESCER) * .044 : 0)
              + (n < x.getLong("boosterEnd") ? tier(e, UNIVERSAL_BOOSTER) * .02 : 0),
          AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
    if (n % 20 == 0 && e instanceof Mob m && tier(e, TITANIUM_BONES) > 0) {
      var reserve = e.getData(Cybernetics2077.CARRY_RESERVE);
      int limit = 9 * ((tier(e, TITANIUM_BONES) + 1) / 2);
      for (var dropped :
          e
              .level()
              .getEntitiesOfClass(
                  net.minecraft.world.entity.item.ItemEntity.class,
                  e.getBoundingBox().inflate(1.5),
                  item -> !item.hasPickUpDelay())
              .stream()
              .limit(3)
              .toList()) {
        ItemStack rest = dropped.getItem().copy();
        for (int i = 0; i < limit && !rest.isEmpty(); i++)
          rest = reserve.insertItem(i, rest, false);
        if (rest.isEmpty()) dropped.discard();
        else dropped.setItem(rest);
      }
    }
    if (n % 20 == 0 && tier(e, CHITIN) > 0 && n - x.getLong("lastDamage") >= 100)
      e.heal(e.getMaxHealth() * .01f);
    if (maxRam(e) > 0 && ram(e) <= maxRam(e) * .2f) {
      Augment manager = tier(e, RAM_REALLOCATOR) > 0 ? RAM_REALLOCATOR : CAMILLO_RAM_MANAGER;
      int t = tier(e, manager);
      if (t > 0 && n >= x.getLong("ramManagerReady")) {
        ram(e, ram(e) + maxRam(e) * (manager == RAM_REALLOCATOR ? .2f + .05f * t : .1f + .04f * t));
        x.putLong("ramManagerReady", n + (manager == RAM_REALLOCATOR ? 1500 : 1600));
      }
    }
    float debt = x.getFloat("woundDebt");
    if (debt > 0 && n >= x.getLong("woundPulse")) {
      x.putLong("woundPulse", n + 20);
      float part = Math.min(debt, x.getFloat("woundRate"));
      x.putFloat("woundDebt", Math.max(0, debt - part));
      x.putBoolean("payingDebt", true);
      try {
        e.hurt(
            new DamageSource(
                e.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(
                        net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                            Cybernetics2077.id("delayed_wound")))),
            part);
      } finally {
        x.putBoolean("payingDebt", false);
      }
    }
    if (e instanceof ServerPlayer p && n % 10 == 0) CyberwareRuntime.syncIfChanged(p);
  }

  @SubscribeEvent
  public static void playerTick(PlayerTickEvent.Post event) {
    if (event.getEntity() instanceof ServerPlayer p) tick(p);
  }

  @SubscribeEvent
  public static void outgoing(LivingIncomingDamageEvent event) {
    if (!(event.getSource().getEntity() instanceof LivingEntity e) || e.level().isClientSide)
      return;
    if (event.getSource().is(DamageTypes.PLAYER_ATTACK)
        || event.getSource().is(DamageTypes.MOB_ATTACK)) {
      extra(e).putLong("lastAttack", e.level().getGameTime());
      extra(e).putLong("disguiseEnd", 0);
    }
    if (event.getSource().is(DamageTypes.PLAYER_ATTACK)
        || event.getSource().is(DamageTypes.MOB_ATTACK))
      event.setAmount(event.getAmount() * attackBonus(e, event.getEntity(), false, false, false));
    if (event.getSource().getDirectEntity() instanceof AbstractArrow)
      event.setAmount(
          event.getAmount()
              * attackBonus(
                  e,
                  event.getEntity(),
                  true,
                  false,
                  event.getSource().getDirectEntity() instanceof AbstractArrow arrow
                      && arrow.isCritArrow()));
    if (event.getSource().is(DamageTypes.MAGIC)
        && tier(e, BLACK_MAMBA) > 0
        && event.getEntity().hasEffect(MobEffects.POISON)) event.setAmount(event.getAmount() * .1f);
  }

  @SubscribeEvent(priority = EventPriority.HIGH)
  public static void defense(LivingDamageEvent.Pre event) {
    var e = event.getEntity();
    if (e.level().isClientSide
        || !operational(e)
        || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    var x = extra(e);
    if (x.getBoolean("payingDebt")) {
      event.setNewDamage(event.getOriginalDamage());
      return;
    }
    float amount = event.getNewDamage();
    long n = e.level().getGameTime();
    if (BerserkSpecs.active(e)
        && ExpansionImplants.os(e) == ZETATECH_BERSERK
        && event.getSource().is(DamageTypeTags.IS_FALL)) amount *= .7f;
    int nano = tier(e, NANO_PLATING);
    if (nano > 0 && event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
      if (n >= x.getLong("nanoWindow")) {
        x.putLong("nanoWindow", n + 100);
        x.putInt("nanoCount", 0);
      }
      if (x.getInt("nanoCount") < 3
          && e.getRandom().nextDouble()
              < nano * .014 * (e.isSprinting() || !e.onGround() ? 2 : 1)) {
        x.putInt("nanoCount", x.getInt("nanoCount") + 1);
        event.setNewDamage(0);
        return;
      }
    }
    double chance = tier(e, NEOFIBER) * .02;
    if ((e instanceof Player p
        ? p.getFoodData().getFoodLevel() > 17
        : e.getHealth() > e.getMaxHealth() * .85)) chance += tier(e, KINETIC_FRAME) * .032;
    if (n < x.getLong("countershellEnd")) chance += .5;
    if (n < x.getLong("defenzikovEnd")) chance += .9;
    if (Sandevistan.active(e) && Sandevistan.model(e) == SandevistanModel.QIANT) chance += .24;
    if (e.getRandom().nextDouble() < Math.min(.95, chance))
      amount *= 1 - Math.min(.9, .5 + tier(e, NEOFIBER) * .02 + tier(e, SPRING_JOINTS) * .032);
    if (event.getSource().getEntity() instanceof LivingEntity attacker) {
      double distance = e.distanceTo(attacker);
      double proximity = Math.clamp((6 - distance) / 3, 0, 1);
      amount *=
          1 - proximity * (tier(e, PERIPHERAL_INVERSE) > 0 ? .45 : tier(e, PROXISHIELD) * .04);
      var direction = attacker.position().subtract(e.position()).normalize();
      if (e.getLookAngle().dot(direction) < .5) amount *= 1 - tier(e, CARAPACE) * .064;
    }
    if (event.getSource().is(DamageTypeTags.IS_EXPLOSION))
      amount *= 1 - tier(e, CELLULAR_ADAPTER) * .02;
    if (Sandevistan.active(e)
        && Sandevistan.model(e) == SandevistanModel.QIANT
        && (event.getSource().is(DamageTypeTags.IS_FIRE)
            || event.getSource().is(AugmentExpansion.DISCHARGE)
            || event.getSource().is(DamageTypes.MAGIC)
            || dev.cybernetics2077.cyberware.MantisVariant.elemental(event.getSource())
            || java.util.stream.Stream.of(
                    "short_circuit", "overheat", "contagion", "cyberware_malfunction")
                .anyMatch(
                    id ->
                        event
                            .getSource()
                            .typeHolder()
                            .is(dev.cybernetics2077.Cybernetics2077.id(id))))) amount *= .4f;
    event.setNewDamage(amount);
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  public static void defer(LivingDamageEvent.Pre event) {
    var e = event.getEntity();
    var x = extra(e);
    if (e.level().isClientSide
        || x.getBoolean("payingDebt")
        || tier(e, PAINDUCER) == 0
        || event.getNewDamage() <= 0
        || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    float delayed = event.getNewDamage() * .3f;
    event.setNewDamage(event.getNewDamage() - delayed);
    x.putFloat("woundDebt", x.getFloat("woundDebt") + delayed);
    x.putFloat("woundRate", Math.max(x.getFloat("woundRate"), x.getFloat("woundDebt") / 5));
  }

  @SubscribeEvent
  public static void targeting(LivingChangeTargetEvent event) {
    if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer p
        && p.level().getGameTime() < extra(p).getLong("disguiseEnd"))
      event.setNewAboutToBeSetTarget(null);
  }

  @SubscribeEvent
  public static void carryDrops(LivingDropsEvent event) {
    var e = event.getEntity();
    if (e.level().isClientSide
        || e instanceof Player
            && e.level()
                .getGameRules()
                .getBoolean(net.minecraft.world.level.GameRules.RULE_KEEPINVENTORY)) return;
    if (!e.hasData(Cybernetics2077.CARRY_RESERVE)) return;
    var reserve = e.getData(Cybernetics2077.CARRY_RESERVE);
    for (int i = 0; i < reserve.getSlots(); i++)
      if (!reserve.getStackInSlot(i).isEmpty()) {
        event
            .getDrops()
            .add(
                new net.minecraft.world.entity.item.ItemEntity(
                    e.level(), e.getX(), e.getY(), e.getZ(), reserve.getStackInSlot(i).copy()));
        reserve.setStackInSlot(i, ItemStack.EMPTY);
      }
  }

  @SubscribeEvent
  public static void slam(LivingDamageEvent.Post event) {
    if (event.getNewDamage() <= 0
        || !(event.getSource().getEntity() instanceof LivingEntity e)
        || e.level().isClientSide
        || e.onGround()
        || !BerserkSpecs.active(e)
        || ExpansionImplants.os(e) != ZETATECH_BERSERK
        || !(event.getSource().is(DamageTypes.PLAYER_ATTACK)
            || event.getSource().is(DamageTypes.MOB_ATTACK))) return;
    var x = extra(e);
    long n = e.level().getGameTime();
    if (x.getBoolean("slamming") || n < x.getLong("slamReady")) return;
    x.putLong("slamReady", n + 40);
    x.putBoolean("slamming", true);
    var hit = event.getEntity();
    try {
      for (var other :
          e
              .level()
              .getEntitiesOfClass(
                  LivingEntity.class,
                  hit.getBoundingBox().inflate(4),
                  o ->
                      o != hit
                          && NeuralActors.hostile(e, o)
                          && o.distanceToSqr(hit) <= 16
                          && hit.hasLineOfSight(o))
              .stream()
              .limit(8)
              .toList())
        SecondaryDamage.hurt(
            other, e.level().damageSources().mobAttack(e), event.getNewDamage() * .5f);
    } finally {
      x.putBoolean("slamming", false);
    }
    ((ServerLevel) e.level())
        .sendParticles(
            net.minecraft.core.particles.ParticleTypes.SWEEP_ATTACK,
            hit.getX(),
            hit.getY() + .3,
            hit.getZ(),
            12,
            1.5,
            .2,
            1.5,
            0);
  }

  @SubscribeEvent
  public static void damaged(LivingDamageEvent.Post event) {
    var e = event.getEntity();
    if (e.level().isClientSide || event.getNewDamage() <= 0) return;
    var x = extra(e);
    long n = e.level().getGameTime();
    x.putLong("lastDamage", n);
    if (tier(e, RAM_RECOUP) > 0)
      ram(e, ram(e) + event.getNewDamage() * tier(e, RAM_RECOUP) * .0056f);
    if (n >= x.getLong("damageWindow")) {
      x.putLong("damageWindow", n + 60);
      x.putFloat("burstDamage", 0);
    }
    x.putFloat("burstDamage", x.getFloat("burstDamage") + event.getNewDamage());
    if (tier(e, COUNTERSHELL) > 0
        && x.getFloat("burstDamage") >= e.getMaxHealth() * .35
        && n >= x.getLong("countershellReady")) {
      x.putLong("countershellEnd", n + 80);
      x.putLong("countershellReady", n + 120);
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void kill(LivingDeathEvent event) {
    if (event.isCanceled()
        || !(event.getSource().getEntity() instanceof LivingEntity e)
        || e.level().isClientSide) return;
    reduceCooldowns(e, Math.max(tier(e, AXOLOTL) * .015, tier(e, NEWTON_MODULE) * .003));
    if (event.getSource().is(DamageTypes.PLAYER_ATTACK)
        || event.getSource().is(DamageTypes.MOB_ATTACK)) {
      if (tier(e, TYROSINE_INJECTOR) > 0)
        extra(e).putLong("tyrosineEnd", e.level().getGameTime() + 300);
      if (tier(e, ADRENALINE_BOOSTER) > 0) {
        if (e instanceof Player p) p.getFoodData().eat(tier(e, ADRENALINE_BOOSTER), .1f);
        extra(e).putLong("jumpReady", 0);
      }
    }
  }

  private ExpansionRuntime() {}
}
