package dev.cybernetics2077.mob;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import dev.cybernetics2077.temporal.*;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Equipment-driven NPC activations, RAM, uploads and defenses. No fake players or instant casts.
 */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class MobImplants {
  public static boolean supported(LivingEntity e) {
    return e instanceof Mob && (e instanceof Cyberpsycho || EnhancedMobs.enhanced(e));
  }

  public static CompoundTag state(Mob m) {
    return ExpansionRuntime.extra(m);
  }

  public static int tier(Mob m, Augment a) {
    return AugmentRuntime.tier(m, a);
  }

  public static boolean berserk(Mob m) {
    return AugmentRuntime.berserkTier(m) > 0
        && state(m).getLong("berserkEnd") > m.level().getGameTime()
        && ExpansionRuntime.operational(m);
  }

  private static final java.util.Set<Mob> MANTIS_GOALS =
      java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());

  public static void initialize(Mob m) {
    if (!supported(m) || m.level().isClientSide) return;
    var x = state(m);
    // 0.27 stored Self-ICE in PsychoSystems; preserve its recharge during migration.
    if (x.contains("iceReady")) {
      AugmentRuntime.state(m).iceReady =
          Math.max(AugmentRuntime.state(m).iceReady, x.getLong("iceReady"));
      x.remove("iceReady");
    }
    if (m instanceof Cyberpsycho)
      for (var attr :
          new net.minecraft.core.Holder[] {
            Attributes.ARMOR,
            Attributes.ARMOR_TOUGHNESS,
            Attributes.MAX_HEALTH,
            Attributes.MOVEMENT_SPEED
          }) {
        var instance = m.getAttribute(attr);
        if (instance != null)
          for (String old : new String[] {"armor", "toughness", "para", "epi", "adrenaline"})
            instance.removeModifier(Cybernetics2077.id("psycho_" + old));
      }
    if (!x.getBoolean("initialized")) {
      x.putBoolean("initialized", true);
      if (!x.contains("pumpCharges")) x.putInt("pumpCharges", 2);
      ExpansionRuntime.ram(m, ExpansionRuntime.maxRam(m));
    }
    var stack = ExpansionRuntime.deck(m);
    if (stack.getItem() instanceof CyberdeckItem deck && CyberdeckItem.programs(stack).isEmpty()) {
      var programmed = CyberpsychoLoadouts.stack(deck, m.getUUID().getMostSignificantBits());
      m.getData(Cybernetics2077.CYBERWARE)
          .setStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot(), programmed);
    }
    var arms =
        m.getData(Cybernetics2077.CYBERWARE).getStackInSlot(CyberwareCategory.ARMS.firstSlot());
    boolean mantis = arms.getItem() instanceof MantisBladesItem;
    if (mantis && !m.getMainHandItem().isEmpty())
      m.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    if ((mantis || ExpansionImplants.berserk(ExpansionImplants.os(m)))
        && m instanceof net.minecraft.world.entity.PathfinderMob path
        && MANTIS_GOALS.add(m))
      ((dev.cybernetics2077.mixin.MobGoalAccess) m)
          .cybernetics$goals()
          .addGoal(
              1,
              new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(path, 1.2, false) {
                private boolean enabled() {
                  return berserk(m)
                      || !ExpansionRuntime.disabled(m)
                          && m.getData(Cybernetics2077.CYBERWARE)
                                  .getStackInSlot(CyberwareCategory.ARMS.firstSlot())
                                  .getItem()
                              instanceof MantisBladesItem;
                }

                @Override
                public boolean canUse() {
                  return enabled() && super.canUse();
                }

                @Override
                public boolean canContinueToUse() {
                  return enabled() && super.canContinueToUse();
                }

                @Override
                protected int getAttackInterval() {
                  return Math.max(6, (int) Math.ceil(20 / meleeRate(m)));
                }
              });
    Sandevistan.trackMob(m);
    attributes(m);
  }

  public static boolean hasPrograms(Mob m) {
    return !ExpansionRuntime.disabled(m)
        && !CyberdeckItem.programs(ExpansionRuntime.deck(m)).isEmpty();
  }

  public static void attributes(Mob m) {
    double armor = 0, toughness = 0;
    for (var a : Augment.values()) {
      armor += a.armor(tier(m, a));
      toughness += a.toughness(tier(m, a));
    }
    // EnhancedMobs owns its ordinary plating. A psycho has no vanilla enhancement layer.
    if (m instanceof Cyberpsycho) {
      ExpansionRuntime.modifier(
          m, Attributes.ARMOR, "npc_armor", armor, AttributeModifier.Operation.ADD_VALUE);
      ExpansionRuntime.modifier(
          m,
          Attributes.ARMOR_TOUGHNESS,
          "npc_toughness",
          toughness,
          AttributeModifier.Operation.ADD_VALUE);
    }
    ExpansionRuntime.modifier(
        m,
        Attributes.ARMOR,
        "npc_para",
        tier(m, PARA_BELLUM) * .04,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    ExpansionRuntime.modifier(
        m,
        Attributes.MAX_HEALTH,
        "npc_epi",
        m instanceof Cyberpsycho ? Math.max(0, tier(m, EPIMORPHIC_SKELETON) - 2) * .05 : 0,
        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    int optic = 0;
    for (var a : Augment.values()) if (a.optics()) optic = Math.max(optic, tier(m, a));
    ExpansionRuntime.modifier(
        m, Attributes.FOLLOW_RANGE, "npc_optics", optic * 4, AttributeModifier.Operation.ADD_VALUE);
    var arms =
        m.getData(Cybernetics2077.CYBERWARE).getStackInSlot(CyberwareCategory.ARMS.firstSlot());
    var attack = m.getAttribute(Attributes.ATTACK_DAMAGE);
    ExpansionRuntime.modifier(
        m,
        Attributes.ATTACK_DAMAGE,
        "npc_mantis",
        arms.getItem() instanceof MantisBladesItem blades
                && attack != null
                && !ExpansionRuntime.disabled(m)
            ? Math.max(0, 8 * blades.tier().bladeMultiplier() - attack.getBaseValue())
            : 0,
        AttributeModifier.Operation.ADD_VALUE);
    if (m.getHealth() > m.getMaxHealth()) m.setHealth(m.getMaxHealth());
  }

  public static double meleeRate(Mob m) {
    return ExpansionRuntime.meleeRate(m) * BerserkSpecs.attackRate(m);
  }

  public static boolean usePump(Mob m) {
    var x = state(m);
    int t = tier(m, BLOOD_PUMP);
    long n = m.level().getGameTime();
    if (!ExpansionRuntime.operational(m)
        || t == 0
        || x.getInt("pumpCharges") <= 0
        || m.getHealth() >= m.getMaxHealth()) return false;
    x.putInt("pumpCharges", x.getInt("pumpCharges") - 1);
    if (x.getLong("pumpNext") == 0)
      x.putLong(
          "pumpNext",
          ExpansionRuntime.cooldown(
              m, n + (long) ((50 - t * 5) * 20L / (1 + tier(m, CELLULAR_ADAPTER) * .02))));
    if (x.getInt("pumpCharges") == 0)
      x.putLong("pumpNext", ExpansionRuntime.finished(m, x.getLong("pumpNext")));
    x.putLong("pumpEnd", n + 120);
    x.putLong("pumpPulse", n + 20);
    ExpansionRuntime.medicalUsed(m);
    m.heal(m.getMaxHealth() * (.1f + .05f * t));
    ((ServerLevel) m.level())
        .sendParticles(ParticleTypes.HEART, m.getX(), m.getEyeY(), m.getZ(), 5, .2, .2, .2, 0);
    return true;
  }

  public static void tick(Mob m) {
    if (!supported(m) || !(m.level() instanceof ServerLevel level)) return;
    var x = state(m);
    long n = level.getGameTime();
    if (!x.getBoolean("initialized")) initialize(m);
    if (!ExpansionRuntime.operational(m)) {
      if (m instanceof Cyberpsycho c) c.combatVisual(false);
      return;
    }
    var target = m.getTarget();
    boolean combat = target != null && NeuralActors.canAttack(m, target);
    int b = AugmentRuntime.berserkTier(m), pump = tier(m, BLOOD_PUMP);
    if (x.getLong("berserkEnd") > 0 && n >= x.getLong("berserkEnd")) {
      if (b > 0) m.heal(m.getMaxHealth() * .25f * x.getInt("berserkKills"));
      x.putLong("berserkReady", ExpansionRuntime.finished(m, x.getLong("berserkReady")));
      x.putInt("berserkKills", 0);
      x.putLong("berserkEnd", 0);
    }
    if (combat && b > 0 && n >= x.getLong("berserkReady")) {
      x.putLong(
          "berserkEnd", n + (ExpansionImplants.os(m) == MILITECH_BERSERK ? 240 : (6 + b) * 20));
      x.putLong(
          "berserkReady",
          n
              + (ExpansionImplants.os(m) == MILITECH_BERSERK
                  ? 500
                  : ExpansionImplants.os(m) == ZETATECH_BERSERK
                      ? (50 - 5 * b) * 20
                      : (60 - 5 * b) * 20));
    }
    int a = tier(m, ADRENALINE_CONVERTER);
    if (combat && !x.getBoolean("inCombat") && n >= x.getLong("adrenalineReady")) {
      x.putLong("adrenalineEnd", n + (4 + a) * 20L);
      x.putLong("adrenalineReady", n + 400);
      if (tier(m, HANDLE_WRAP) > 0 && n >= x.getLong("wrapReady")) {
        x.putLong("wrapEnd", n + 120);
        x.putLong("wrapReady", n + 200);
      }
    }
    if (combat) {
      x.putBoolean("inCombat", true);
      x.putLong("combatClock", n);
    } else if (n - x.getLong("combatClock") >= 100) x.putBoolean("inCombat", false);
    ExpansionRuntime.modifier(
        m,
        Attributes.MOVEMENT_SPEED,
        "npc_adrenaline",
        n < x.getLong("adrenalineEnd") ? a * .08 : 0,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    if (combat) m.setSprinting(!(m.getMainHandItem().getItem() instanceof BowItem));
    if (m instanceof Cyberpsycho c) c.combatVisual(berserk(m));
    var s = AugmentRuntime.state(m);
    int sandy = Sandevistan.tier(m);
    if (sandy > 0
        && combat
        && !ExpansionRuntime.disabled(m)
        && s.sandyEnd <= n
        && n >= s.sandyRearm) {
      double charge =
          s.sandyEnd == 0
              ? Math.clamp(
                  s.sandyCharge + (n - s.sandyClock) / Sandevistan.rechargeTicks(m, s), 0, 1)
              : 0;
      if (charge >= .9) {
        var model = Sandevistan.model(m);
        s.sandyProfile = sandy;
        s.extra.putInt("sandyModel", model.ordinal());
        s.sandyEnd = n + model.duration(sandy) * 20L;
        s.sandyCharge = 0;
        x.putLong("lastSync", n - 10);
        Sandevistan.invalidate(level);
      }
    }
    if (s.sandyEnd > 0 && (n >= s.sandyEnd || sandy == 0)) {
      s.sandyEnd = 0;
      s.sandyCharge = 0;
      s.sandyClock = n;
      s.sandyRearm = n + 20;
      Sandevistan.finishRecharge(m, s, n);
      x.putLong("lastSync", n - 10);
      Sandevistan.invalidate(level);
    }
    while (pump > 0
        && x.getInt("pumpCharges") < 2
        && x.getLong("pumpNext") > 0
        && n >= x.getLong("pumpNext")) {
      int charges = x.getInt("pumpCharges") + 1;
      x.putInt("pumpCharges", charges);
      x.putLong(
          "pumpNext",
          charges >= 2
              ? 0
              : x.getLong("pumpNext")
                  + (long) ((50 - pump * 5) * 20L / (1 + tier(m, CELLULAR_ADAPTER) * .02)));
    }
    if (pump > 0
        && m.getHealth() < m.getMaxHealth() * (tier(m, BIOMONITOR) > 0 ? .5 : .35)
        && n >= x.getLong("monitorReady")) {
      x.putLong("monitorReady", n + 40);
      usePump(m);
    }
    if (pump > 0 && n < x.getLong("pumpEnd") && n >= x.getLong("pumpPulse")) {
      x.putLong("pumpPulse", n + 20);
      m.heal(m.getMaxHealth() * pump * .005f);
    }
    if (combat) {
      int legs = Math.max(tier(m, REINFORCED_TENDONS), tier(m, FORTIFIED_ANKLES));
      boolean mantis =
          m.getData(Cybernetics2077.CYBERWARE)
                  .getStackInSlot(CyberwareCategory.ARMS.firstSlot())
                  .getItem()
              instanceof MantisBladesItem;
      if ((legs > 0 || mantis)
          && m.onGround()
          && !m.isInWater()
          && m.distanceToSqr(target) > 16
          && m.distanceToSqr(target) < 100
          && m.hasLineOfSight(target)
          && n >= x.getLong("jumpReady")
          && !QuickhackEffects.affected(m, QuickhackKind.CRIPPLE_MOVEMENT)) {
        var d = target.position().subtract(m.position()).multiply(1, 0, 1).normalize();
        m.setDeltaMovement(
            d.scale(mantis ? .7 : .45)
                .add(0, tier(m, FORTIFIED_ANKLES) > 0 ? .6 + legs * .08 : .42, 0));
        m.hasImpulse = true;
        x.putLong("jumpReady", n + 100);
        if (tier(m, REINFORCED_TENDONS) > 0) {
          x.putLong("doubleJumpAt", n + 5);
          x.putLong("doubleJumpUntil", n + 20);
        }
      }
      if (tier(m, REINFORCED_TENDONS) > 0
          && !m.onGround()
          && n >= x.getLong("doubleJumpAt")
          && x.getLong("doubleJumpAt") > 0
          && n <= x.getLong("doubleJumpUntil")
          && !QuickhackEffects.affected(m, QuickhackKind.CRIPPLE_MOVEMENT)) {
        var d = target.position().subtract(m.position()).multiply(1, 0, 1).normalize();
        m.setDeltaMovement(d.scale(.45).add(0, .42 + tier(m, REINFORCED_TENDONS) * .035, 0));
        m.hasImpulse = true;
        x.putLong("doubleJumpAt", 0);
        x.putLong("lastDoubleJump", n);
      }
      if (tier(m, OPTICAL_CAMO) > 0 && n >= x.getLong("camoReady")) {
        int t = tier(m, OPTICAL_CAMO);
        x.putLong("camoReady", n + (70 - 5 * t) * 20L);
        x.putLong("camoEnd", n + (3 + t) * 20L);
        m.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (3 + t) * 20, 0, false, false));
      }
    }
    if (x.getLong("camoEnd") > 0 && n >= x.getLong("camoEnd")) {
      x.putLong("camoReady", ExpansionRuntime.finished(m, x.getLong("camoReady")));
      x.putLong("camoEnd", 0);
    }
    if (ExpansionRuntime.disabled(m)) x.remove("uploads");
    else if (hasPrograms(m)) uploads(m, target);
    if (x.getLong("berserkReady") > n && !x.getBoolean("cooldownAdjusted")) {
      x.putLong("berserkReady", ExpansionRuntime.cooldown(m, x.getLong("berserkReady")));
      x.putBoolean("cooldownAdjusted", true);
    }
    if (n >= x.getLong("berserkReady")) x.putBoolean("cooldownAdjusted", false);
    ExpansionRuntime.tick(m);
    if (n % 20 == 0) {
      attributes(m);
      if (EnhancedMobs.enhanced(m)) EnhancedMobs.refresh(m);
    }
    if (n - x.getLong("lastSync") >= 10) {
      x.putLong("lastSync", n);
      PacketDistributor.sendToPlayersTrackingEntity(m, new CyberwareNetwork.State(snapshot(m)));
    }
  }

  private static void uploads(Mob m, LivingEntity target) {
    var x = state(m);
    long n = m.level().getGameTime();
    var deckStack = ExpansionRuntime.deck(m);
    var deck = (CyberdeckItem) deckStack.getItem();
    ExpansionRuntime.ram(m, ExpansionRuntime.ram(m) + ExpansionRuntime.ramRate(m) / 20);
    var jobs = x.getList("uploads", Tag.TAG_COMPOUND);
    if (!jobs.isEmpty() && jobs.getCompound(0).getLong("finish") <= n) {
      var job = jobs.getCompound(0);
      jobs.remove(0);
      var entity = ((ServerLevel) m.level()).getEntity(job.getUUID("target"));
      if (entity instanceof LivingEntity victim
          && NeuralActors.canAttack(m, victim)
          && m.distanceToSqr(victim) <= 1024
          && m.hasLineOfSight(victim)) {
        var kind =
            QuickhackKind.values()[
                Math.clamp(job.getInt("kind"), 0, QuickhackKind.values().length - 1)];
        var program = Cybernetics2077.quickhack(kind, job.getInt("tier"));
        QuickhackItem next = null;
        if (!jobs.isEmpty() && jobs.getCompound(0).getUUID("target").equals(victim.getUUID())) {
          var q = jobs.getCompound(0);
          next =
              Cybernetics2077.quickhack(QuickhackKind.values()[q.getInt("kind")], q.getInt("tier"));
        }
        if (CyberdeckItem.has(deckStack, program))
          QuickhackEffects.land(
              m,
              victim,
              program,
              job.getFloat("damage") * deck.queuedMultiplier(program, next),
              deckStack);
      }
    }
    if (target != null
        && NeuralActors.canAttack(m, target)
        && m.distanceToSqr(target) <= 1024
        && m.hasLineOfSight(target)
        && n >= x.getLong("hackReady")
        && jobs.size() < deck.tier().queueLimit()) {
      var programs = CyberdeckItem.programs(deckStack);
      int index = x.getInt("programIndex");
      for (int k = 0; k < programs.size(); k++) {
        var p = programs.get(Math.floorMod(index + k, programs.size()));
        if (QuickhackEffects.affected(target, p.kind())) continue;
        int cost = deck.cost(p);
        if (ExpansionRuntime.ram(m) < cost) continue;
        ExpansionRuntime.ram(m, ExpansionRuntime.ram(m) - cost);
        x.putInt("programIndex", index + k + 1);
        x.putLong("hackReady", n + 20);
        var job = new CompoundTag();
        job.putUUID("target", target.getUUID());
        job.putInt("kind", p.kind().ordinal());
        job.putInt("tier", p.tier().level);
        long start =
            jobs.isEmpty()
                ? n
                : Math.max(
                    n, jobs.getLast() instanceof CompoundTag last ? last.getLong("finish") : n);
        job.putLong("start", start);
        job.putLong(
            "finish",
            start
                + Math.max(
                    20, (int) ((60 - 6 * deck.tier().level) / (1 + tier(m, EX_DISK) * .05))));
        float damage = p.damage() * deck.damageMultiplier(p);
        if (m.getRandom().nextFloat() < (tier(m, COX_2) > 0 ? 1 : tier(m, BIOCONDUCTOR) * .08f))
          damage *= 1.5f;
        job.putFloat("damage", damage);
        jobs.add(job);
        if (target instanceof ServerPlayer player)
          player.displayClientMessage(
              Component.literal(
                  "Enemy upload: "
                      + p.getName(new ItemStack(p)).getString()
                      + " // break line of sight"),
              true);
        break;
      }
    }
    x.put("uploads", jobs);
  }

  public static CompoundTag snapshot(Mob m) {
    var t = new CompoundTag();
    t.putInt("entity", m.getId());
    t.putUUID("uuid", m.getUUID());
    t.putBoolean("mob", true);
    t.putInt("sandyTier", Sandevistan.tier(m));
    t.putInt("sandyModel", Sandevistan.model(m).ordinal());
    t.putLong("sandyEnd", Sandevistan.active(m) ? AugmentRuntime.state(m).sandyEnd : 0);
    t.putDouble("temporalPace", Sandevistan.pace(m));
    t.putBoolean("sandyPvp", true);
    var arms =
        m.getData(Cybernetics2077.CYBERWARE).getStackInSlot(CyberwareCategory.ARMS.firstSlot());
    t.putBoolean(
        "claws", arms.getItem() instanceof MantisBladesItem && !ExpansionRuntime.disabled(m));
    if (arms.getItem() instanceof MantisBladesItem blades)
      t.putInt("mantisVariant", blades.variant().ordinal());
    t.putBoolean("berserk", berserk(m));
    var neural = state(m).copy();
    neural.putDouble("gunRate", ExpansionRuntime.gunRate(m));
    t.put("expansion", neural);
    t.putFloat("ram", ExpansionRuntime.ram(m));
    t.putInt("ramMax", ExpansionRuntime.maxRam(m));
    return t;
  }

  public static LivingEntity smartTarget(Mob m) {
    var target = m.getTarget();
    return (tier(m, SMART_LINK) > 0 || tier(m, TYGER_IMPRINT) > 0)
            && target != null
            && NeuralActors.canAttack(m, target)
            && m.hasLineOfSight(target)
        ? target
        : null;
  }

  public static void discharge(Mob m, LivingEntity origin) {
    int t = tier(m, MICROGENERATOR);
    long n = m.level().getGameTime();
    var x = state(m);
    if (t == 0 || n < x.getLong("microReady")) return;
    x.putLong("microReady", n + (12 - t) * 20L);
    shock(m, origin, t, 3, 4);
  }

  private static void shock(Mob m, LivingEntity origin, int t, int radius, int limit) {
    var source =
        new DamageSource(
            m.registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE)
                .getHolderOrThrow(AugmentExpansion.DISCHARGE),
            m,
            m);
    var targets =
        m.level()
            .getEntitiesOfClass(
                LivingEntity.class,
                origin.getBoundingBox().inflate(radius),
                e ->
                    NeuralActors.hostile(m, e)
                        && origin.distanceToSqr(e) <= radius * radius
                        && m.hasLineOfSight(e));
    targets.sort(Comparator.comparingDouble(origin::distanceToSqr));
    for (var target : targets.stream().limit(limit).toList())
      SecondaryDamage.hurt(target, source, 2 + 2 * t);
    ((ServerLevel) m.level())
        .sendParticles(
            ParticleTypes.ELECTRIC_SPARK,
            origin.getX(),
            origin.getEyeY(),
            origin.getZ(),
            24,
            .7,
            .3,
            .7,
            .04);
  }

  @SubscribeEvent
  public static void tick(EntityTickEvent.Post event) {
    if (event.getEntity() instanceof Mob m) tick(m);
  }

  @SubscribeEvent
  public static void tracking(PlayerEvent.StartTracking event) {
    if (event.getEntity() instanceof ServerPlayer p
        && event.getTarget() instanceof Mob m
        && supported(m)) PacketDistributor.sendToPlayer(p, new CyberwareNetwork.State(snapshot(m)));
  }

  @SubscribeEvent
  public static void join(EntityJoinLevelEvent event) {
    if (!event.getLevel().isClientSide && event.getEntity() instanceof Mob m && supported(m))
      initialize(m);
  }

  @SubscribeEvent
  public static void incoming(LivingIncomingDamageEvent event) {
    if (!(event.getSource().getEntity() instanceof Mob m) || !supported(m) || !m.isAlive()) return;
    float damage = event.getAmount();
    boolean melee = event.getSource().is(DamageTypes.MOB_ATTACK);
    if (!melee && event.getSource().is(DamageTypeTags.IS_PROJECTILE) && EnhancedMobs.enhanced(m)) {
      var loadout = EnhancedMobs.loadout(m);
      damage *= 1 + loadout.count() * loadout.averageTier() * .08f;
    }
    if (melee) damage *= 1 + tier(m, DENSE_MARROW) * .06f;
    if (melee
        || event.getSource().is(DamageTypeTags.IS_PROJECTILE)
            && !dev.cybernetics2077.compat.TacZCompat.bullet(event.getSource().getDirectEntity())) {
      float stab =
          melee && m.getMainHandItem().getItem() instanceof SwordItem ? tier(m, STABBER) * .04f : 0;
      float visual =
          tier(m, VISUAL_CORTEX_SUPPORT)
              * .06f
              * (float) Math.clamp(m.distanceTo(event.getEntity()) / 32., 0, 1);
      float wrap =
          m.level().getGameTime() < state(m).getLong("wrapEnd") ? tier(m, HANDLE_WRAP) * .04f : 0;
      float rage = BerserkSpecs.critChance(m);
      if (Sandevistan.active(m))
        rage = 1 - (1 - rage) * (1 - Sandevistan.model(m).critChance(Sandevistan.tier(m)));
      double crit =
          Math.min(
              1,
              1
                  - (1 - stab)
                      * (1 - visual)
                      * (1 - wrap)
                      * (1 - rage)
                      * (1 - ExpansionRuntime.critChance(m, event.getEntity())));
      if (m.getRandom().nextDouble() < crit)
        damage *=
            1.5f
                * BerserkSpecs.critDamage(m)
                * (Sandevistan.active(m)
                    ? 1 + Sandevistan.model(m).critDamage(Sandevistan.tier(m))
                    : 1);
    }
    event.setAmount(damage);
  }

  @SubscribeEvent
  public static void defense(LivingDamageEvent.Pre event) {
    if (!(event.getEntity() instanceof Mob m)
        || !supported(m)
        || !ExpansionRuntime.operational(m)
        || state(m).getBoolean("payingDebt")
        || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    float damage = event.getNewDamage() * (1 - tier(m, PAIN_EDITOR) * .02f);
    if (berserk(m)) {
      damage =
          ExpansionImplants.os(m) == MILITECH_BERSERK
              ? 0
              : damage * (1 - AugmentRuntime.berserkTier(m) * .1f);
      damage = Math.min(damage, Math.max(0, m.getHealth() - m.getMaxHealth() * .25f));
    }
    event.setNewDamage(damage);
  }

  @SubscribeEvent
  public static void heal(LivingHealEvent event) {
    if (event.getEntity() instanceof Mob m && supported(m))
      event.setAmount(event.getAmount() * (1 + tier(m, BIOMONITOR) * .04f));
  }

  @SubscribeEvent(priority = EventPriority.HIGHEST)
  public static void revive(LivingDeathEvent event) {
    if (!(event.getEntity() instanceof Mob m)
        || !supported(m)
        || m.isNoAi()
        || m.isInvulnerable()
        || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    int t = tier(m, SECOND_HEART);
    var x = state(m);
    long n = m.level().getGameTime();
    if (t > 0 && n >= x.getLong("heartReady")) {
      x.putLong("heartReady", n + (400 - 40 * t) * 20L);
      event.setCanceled(true);
      m.setHealth(m.getMaxHealth());
      m.clearFire();
      m.invulnerableTime = 20;
    }
  }

  @SubscribeEvent
  public static void hit(LivingDamageEvent.Post event) {
    if (event.getNewDamage() <= 0) return;
    if (event.getSource().getEntity() instanceof Mob attacker
        && supported(attacker)
        && !event.getSource().is(AugmentExpansion.DISCHARGE)) {
      discharge(attacker, event.getEntity());
      var arms =
          attacker
              .getData(Cybernetics2077.CYBERWARE)
              .getStackInSlot(CyberwareCategory.ARMS.firstSlot());
      if (event.getSource().is(DamageTypes.MOB_ATTACK)
          && arms.getItem() instanceof MantisBladesItem blades)
        MantisEffects.hit(
            attacker, event.getEntity(), blades.variant(), blades.tier().level, false);
    }
    if (event.getEntity() instanceof Mob m
        && supported(m)
        && ExpansionRuntime.operational(m)
        && event.getSource().getEntity() instanceof LivingEntity attacker
        && !event.getSource().is(AugmentExpansion.DISCHARGE)) {
      int t = tier(m, SHOCK_N_AWE);
      var x = state(m);
      long n = m.level().getGameTime();
      if (t > 0 && n >= x.getLong("shockReady") && m.getRandom().nextFloat() < t * .02f) {
        x.putLong("shockReady", n + 40);
        shock(m, m, t, 4, 8);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void kill(LivingDeathEvent event) {
    if (event.isCanceled()
        || !(event.getSource().getEntity() instanceof Mob m)
        || !supported(m)
        || !ExpansionRuntime.operational(m)) return;
    if (Sandevistan.active(m)) {
      var model = Sandevistan.model(m);
      var sandy = AugmentRuntime.state(m);
      sandy.sandyEnd =
          Math.min(
              m.level().getGameTime() + model.duration(Sandevistan.tier(m)) * 20L,
              sandy.sandyEnd + model.killExtensionTicks());
      if (model == SandevistanModel.FALCON) m.heal(m.getMaxHealth() * .12f);
    }
    int h = tier(m, HEAL_ON_KILL);
    if (h > 0) m.heal(m.getMaxHealth() * (h + 1) * .01f);
    if (berserk(m)) state(m).putInt("berserkKills", state(m).getInt("berserkKills") + 1);
    if (tier(m, MEMORY_BOOST) > 0)
      ExpansionRuntime.ram(m, ExpansionRuntime.ram(m) + .35f + tier(m, MEMORY_BOOST) * .2f);
  }

  private MobImplants() {}
}
