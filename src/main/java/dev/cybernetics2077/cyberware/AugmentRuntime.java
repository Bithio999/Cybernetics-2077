package dev.cybernetics2077.cyberware;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.combat.*;
import java.util.function.Predicate;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;

/** Server-owned implant effects; active timers and charges are saved with the player. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class AugmentRuntime {
  public static Predicate<Player> clientCamo = p -> false;
  public static Predicate<Player> clientBerserk = p -> false;

  public static int tier(Player p, Augment kind) {
    return tier((LivingEntity) p, kind);
  }

  public static int tier(LivingEntity p, Augment kind) {
    if (p != null && ExpansionRuntime.disabled(p)) return 0;
    if (p == null) return 0;
    var inventory = p.getData(Cybernetics2077.CYBERWARE);
    int best = 0;
    for (int i = kind.category.firstSlot();
        i < kind.category.firstSlot() + kind.category.sockets;
        i++)
      if (inventory.getStackInSlot(i).getItem() instanceof AugmentItem item
          && item.augment() == kind) best = Math.max(best, item.tier().level);
    return best;
  }

  public static AugmentState state(LivingEntity p) {
    return p.getData(Cybernetics2077.AUGMENT_STATE);
  }

  public static long now(ServerPlayer p) {
    return p.server.overworld().getGameTime();
  }

  public static int berserkTier(LivingEntity e) {
    var a = ExpansionImplants.os(e);
    return a != null && ExpansionImplants.berserk(a) ? tier(e, a) : 0;
  }

  public static boolean berserk(Player p) {
    return p instanceof ServerPlayer server
        ? p.isAlive() && berserkTier(p) > 0 && state(p).berserkEnd > now(server)
        : clientBerserk.test(p);
  }

  public static boolean camouflaged(Player p) {
    return p instanceof ServerPlayer server
        ? p.isAlive() && tier(p, OPTICAL_CAMO) > 0 && state(p).camoEnd > now(server)
        : clientCamo.test(p);
  }

  public static int maxRam(Player p) {
    return ExpansionRuntime.maxRam(p);
  }

  public static int uploadTicks(Player p) {
    return uploadTicks(p, null);
  }

  public static int uploadTicks(Player p, dev.cybernetics2077.deck.QuickhackItem program) {
    int base = CyberwareRuntime.deckTier(p).uploadTicks();
    if (program != null) {
      if (program.kind().control()) base = Math.max(6, base / 2);
      else if (program.kind() == dev.cybernetics2077.deck.QuickhackKind.SYNAPSE_BURNOUT) base += 12;
      else if (program.kind() != dev.cybernetics2077.deck.QuickhackKind.SHORT_CIRCUIT) base += 6;
    }
    return Math.max(6, Math.round(base / (1 + tier(p, EX_DISK) * .05f)));
  }

  public static float meleeRate(Player p) {
    return (float) ExpansionRuntime.meleeRate(p)
        * BerserkSpecs.attackRate(p)
        * (float) dev.cybernetics2077.temporal.Sandevistan.playerPace(p);
  }

  public static int interval(Player p, int ticks) {
    return Math.max(1, (int) Math.ceil(ticks / meleeRate(p)));
  }

  public static boolean grounded(ServerPlayer p) {
    return p.onGround() && !p.serverLevel().noCollision(p, p.getBoundingBox().move(0, -.08, 0));
  }

  private static void attribute(
      ServerPlayer p,
      net.minecraft.core.Holder<Attribute> attr,
      String name,
      double amount,
      AttributeModifier.Operation operation) {
    var instance = p.getAttribute(attr);
    if (instance == null) return;
    var id = Cybernetics2077.id(name);
    var old = instance.getModifier(id);
    if (old != null && old.amount() == amount && old.operation() == operation) return;
    instance.removeModifier(id);
    if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, amount, operation));
  }

  public static void tick(ServerPlayer p) {
    var s = state(p);
    long time = now(p);
    if (!p.isAlive() || p.isSpectator()) {
      s.berserkEnd = s.camoEnd = s.pumpEnd = s.adrenalineEnd = s.scanUntil = 0;
      s.berserkKills = 0;
      s.highlights.clear();
      s.wrapEnd = s.smartUntil = 0;
      s.smartTarget = null;
    }
    int b = berserkTier(p), pump = tier(p, BLOOD_PUMP), camo = tier(p, OPTICAL_CAMO);
    if (s.berserkEnd != 0 && (time >= s.berserkEnd || b == 0)) {
      if (b != 0 && p.isAlive()) p.heal(p.getMaxHealth() * .25f * s.berserkKills);
      s.berserkReady = ExpansionRuntime.finished(p, s.berserkReady);
      s.berserkEnd = 0;
      s.berserkKills = 0;
    }
    if (s.camoEnd > 0 && time >= s.camoEnd) s.camoReady = ExpansionRuntime.finished(p, s.camoReady);
    if (camo == 0 || time >= s.camoEnd) s.camoEnd = 0;
    if (pump == 0) s.pumpEnd = 0;
    while (s.pumpCharges < 2 && s.pumpNext != 0 && time >= s.pumpNext) {
      s.pumpCharges++;
      s.pumpNext = s.pumpCharges < 2 ? s.pumpNext + s.pumpPeriod : 0;
    }
    if (p.isAlive() && pump > 0 && time <= s.pumpEnd && s.pumpEnd > 0) {
      if (s.pumpPulse == 0) s.pumpPulse = time + 20;
      if (time >= s.pumpPulse) {
        s.pumpPulse = time + 20;
        p.heal(p.getMaxHealth() * pump * .005f);
      }
    }
    dev.cybernetics2077.temporal.Sandevistan.tick(p);
    AugmentExpansion.tick(p);
    int epi = tier(p, EPIMORPHIC_SKELETON);
    double armor = 0, toughness = 0;
    for (var kind : Augment.values()) {
      int t = tier(p, kind);
      armor += kind.armor(t);
      toughness += kind.toughness(t);
    }
    attribute(p, Attributes.ARMOR, "implant_armor", armor, AttributeModifier.Operation.ADD_VALUE);
    attribute(
        p,
        Attributes.ARMOR_TOUGHNESS,
        "implant_toughness",
        toughness,
        AttributeModifier.Operation.ADD_VALUE);
    attribute(
        p,
        Attributes.ARMOR,
        "para_bellum_armor",
        tier(p, PARA_BELLUM) * .04,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    attribute(
        p,
        Attributes.SNEAKING_SPEED,
        "lynx_speed",
        tier(p, LYNX_PAWS) * .04,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    attribute(
        p,
        Attributes.MOVEMENT_SPEED,
        "jenkins_speed",
        AugmentExpansion.sprintBonus(p),
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    attribute(
        p,
        Attributes.MAX_HEALTH,
        "epimorphic_health",
        epi == 0 ? 0 : (epi - 2) * .05,
        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
    attribute(
        p,
        Attributes.ATTACK_SPEED,
        "berserk_speed",
        BerserkSpecs.attackRate(p) - 1,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    int adrenaline = tier(p, ADRENALINE_CONVERTER);
    attribute(
        p,
        Attributes.MOVEMENT_SPEED,
        "adrenaline_speed",
        adrenaline > 0 && time < s.adrenalineEnd ? adrenaline * .08 : 0,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    if (grounded(p) || p.isInWater() || p.onClimbable()) s.airJumped = false;
    if (s.ankleStart >= 0
        && (!grounded(p) || tier(p, FORTIFIED_ANKLES) == 0 || !CyberwareRuntime.usable(p)))
      s.ankleStart = -1;
    s.highlights
        .entrySet()
        .removeIf(
            entry -> entry.getValue() <= time || p.serverLevel().getEntity(entry.getKey()) == null);
    scanHighlights(p, time);
    boolean camoNow = camouflaged(p), berserkNow = berserk(p);
    if (camoNow != s.wasCamo || berserkNow != s.wasBerserk) {
      s.wasCamo = camoNow;
      s.wasBerserk = berserkNow;
      CyberwareRuntime.broadcast(p);
    }
  }

  public static boolean hostile(ServerPlayer p, LivingEntity target) {
    return CyberwareRuntime.canAttack(p, target) && enemyKind(p, target);
  }

  public static boolean enemyKind(ServerPlayer p, LivingEntity target) {
    return target instanceof Enemy
        || target instanceof Mob mob && mob.getTarget() == p
        || target == p.getLastHurtByMob()
        || target instanceof Player && target == p.getLastHurtMob();
  }

  public static boolean action(ServerPlayer p, int action) {
    var s = state(p);
    long time = now(p);
    if (action == 20 && tier(p, TITANIUM_BONES) > 0 && CyberwareRuntime.usable(p)) {
      p.openMenu(
          new net.minecraft.world.SimpleMenuProvider(
              (id, inv, owner) -> new CarryReserveMenu(id, inv),
              net.minecraft.network.chat.Component.translatable("cybernetics2077.reserve")));
      return true;
    }
    if (action == 21) return ExpansionRuntime.faceplate(p);
    if (action == 11) {
      s.scanUntil = 0;
      return true;
    }
    if (!CyberwareRuntime.usable(p) || p.isPassenger()) return false;
    switch (action) {
      case 13 -> {
        return dev.cybernetics2077.temporal.Sandevistan.toggle(p);
      }
      case 5 -> {
        int t = berserkTier(p);
        if (t == 0 || time < s.berserkReady || berserk(p)) return false;
        s.berserkEnd = time + (ExpansionImplants.os(p) == MILITECH_BERSERK ? 240 : (6 + t) * 20L);
        s.berserkReady =
            ExpansionRuntime.cooldown(
                p,
                time
                    + (ExpansionImplants.os(p) == MILITECH_BERSERK
                        ? 500
                        : ExpansionImplants.os(p) == ZETATECH_BERSERK
                            ? (50 - t * 5) * 20
                            : (60 - t * 5) * 20L));
        s.berserkKills = 0;
        p.stopUsingItem();
        p.setHealth(Math.max(p.getHealth(), p.getMaxHealth() * .25f));
        p.level()
            .playSound(
                null,
                p.blockPosition(),
                SoundEvents.IRON_GOLEM_REPAIR,
                SoundSource.PLAYERS,
                .7f,
                .65f);
      }
      case 6 -> {
        if (!usePump(p)) return false;
      }
      case 7 -> {
        int t = tier(p, OPTICAL_CAMO);
        if (t == 0 || time < s.camoReady || camouflaged(p)) return false;
        s.camoEnd = time + (3 + t) * 20L;
        s.camoReady = ExpansionRuntime.cooldown(p, time + (70 - t * 5) * 20L);
        for (var mob :
            p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32)))
          if (mob.getTarget() == p && mob.distanceToSqr(p) > 6.25) mob.setTarget(null);
        p.serverLevel()
            .sendParticles(
                ParticleTypes.END_ROD, p.getX(), p.getY() + 1, p.getZ(), 20, .3, .7, .3, .01);
        p.level()
            .playSound(
                null,
                p.blockPosition(),
                SoundEvents.BEACON_DEACTIVATE,
                SoundSource.PLAYERS,
                .5f,
                1.8f);
      }
      case 8 -> {
        int t = tier(p, REINFORCED_TENDONS);
        if (t == 0
            || p.onGround()
            || s.airJumped
            || p.getAbilities().flying
            || p.isFallFlying()
            || p.isInWater()
            || p.onClimbable()) return false;
        s.airJumped = true;
        jump(p, .42 + t * .035);
      }
      case 9 -> {
        if (tier(p, FORTIFIED_ANKLES) == 0 || !grounded(p) || p.getAbilities().flying) return false;
        if (s.ankleStart < 0) s.ankleStart = time;
      }
      case 12 -> {
        int t = tier(p, FORTIFIED_ANKLES);
        if (t == 0 || s.ankleStart < 0 || !grounded(p)) {
          s.ankleStart = -1;
          return false;
        }
        double fraction = Math.clamp((time - s.ankleStart) / 20.0, 0, 1);
        s.ankleStart = -1;
        jump(p, .42 + (.6 + t * .08 - .42) * fraction);
      }
      case 10 -> {
        if (!dev.cybernetics2077.scanner.ScannerNetwork.installed(p)) return false;
        s.scanUntil = time + 12;
      }
      default -> {
        return false;
      }
    }
    tick(p);
    CyberwareRuntime.broadcast(p);
    return true;
  }

  public static boolean usePump(ServerPlayer p) {
    var s = state(p);
    int t = tier(p, BLOOD_PUMP);
    if (!p.isAlive()
        || p.isSpectator()
        || t == 0
        || s.pumpCharges <= 0
        || p.getHealth() >= p.getMaxHealth()) return false;
    s.pumpCharges--;
    if (s.pumpCharges == 0) s.pumpNext = ExpansionRuntime.finished(p, s.pumpNext);
    ExpansionRuntime.medicalUsed(p);
    if (s.pumpNext == 0) {
      s.pumpPeriod = (int) ((50 - t * 5) * 20 / (1 + tier(p, CELLULAR_ADAPTER) * .02));
      s.pumpNext = now(p) + s.pumpPeriod;
    }
    s.pumpEnd = now(p) + 120;
    s.pumpPulse = now(p) + 20;
    p.heal(p.getMaxHealth() * (.1f + t * .05f));
    p.serverLevel()
        .sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1, p.getZ(), 5, .3, .4, .3, 0);
    p.level()
        .playSound(
            null,
            p.blockPosition(),
            SoundEvents.BEACON_POWER_SELECT,
            SoundSource.PLAYERS,
            .5f,
            1.3f);
    return true;
  }

  private static void scanHighlights(ServerPlayer p, long time) {
    var s = state(p);
    Augment optic = null;
    for (var kind : Augment.values())
      if (kind.optics() && tier(p, kind) > 0) {
        optic = kind;
        break;
      }
    if (s.scanProfile != optic) {
      s.highlights.clear();
      s.smartUntil = 0;
      s.smartTarget = null;
      s.scanProfile = optic;
    }
    if (optic == null) {
      s.highlights.clear();
      // Basic Kiroshi Optics provide scanning without an advanced highlight profile.
      if (!dev.cybernetics2077.scanner.ScannerNetwork.installed(p)) {
        s.smartUntil = 0;
        s.smartTarget = null;
      }
      return;
    }
    if (s.scanUntil <= time || !CyberwareRuntime.usable(p) || time % 4 != 0) return;
    int t = tier(p, optic);
    int radius =
        optic == ORACLE || optic == SENTRY
            ? 24 + 4 * t
            : optic == CLAIRVOYANT ? t * 4 : (optic == DOOMSAYER ? 8 : 12) + t * 4;
    for (var target : p.serverLevel().getEntities(p, p.getBoundingBox().inflate(radius))) {
      if (!target.isAlive() || target.isInvisibleTo(p) || p.distanceToSqr(target) > radius * radius)
        continue;
      boolean eligible;
      if (optic == DOOMSAYER)
        eligible =
            target instanceof net.minecraft.world.entity.monster.Creeper
                || target instanceof net.minecraft.world.entity.item.PrimedTnt
                || target instanceof net.minecraft.world.entity.projectile.Fireball;
      else if (optic == SENTRY)
        eligible =
            target instanceof net.minecraft.world.entity.animal.IronGolem
                || target instanceof Mob m
                    && (dev.cybernetics2077.mob.MobImplants.supported(m)
                        || m.getMainHandItem().getItem()
                            instanceof net.minecraft.world.item.ProjectileWeaponItem);
      else if (optic == ORACLE)
        eligible =
            target instanceof LivingEntity living && hostile(p, living)
                || target instanceof net.minecraft.world.entity.item.PrimedTnt
                || target instanceof net.minecraft.world.entity.projectile.Fireball;
      else {
        eligible = target instanceof LivingEntity living && hostile(p, living);
        if (optic == STALKER && target instanceof ServerPlayer other)
          eligible = CyberwareRuntime.canAttack(p, other);
        if (optic == CLAIRVOYANT) eligible &= p.hasLineOfSight(target);
        else
          eligible &=
              p.getLookAngle()
                      .dot(
                          target
                              .getBoundingBox()
                              .getCenter()
                              .subtract(p.getEyePosition())
                              .normalize())
                  >= .866;
      }
      if (eligible && (s.highlights.containsKey(target.getId()) || s.highlights.size() < 64))
        s.highlights.put(target.getId(), time + (optic == STALKER ? 60 : 1200));
    }
  }

  private static void jump(ServerPlayer p, double power) {
    var velocity = p.getDeltaMovement();
    p.setDeltaMovement(velocity.x, power, velocity.z);
    p.setOnGround(false);
    p.hasImpulse = true;
    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
    p.level()
        .playSound(
            null, p.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, .35f, 1.6f);
  }

  public static void snapshot(ServerPlayer p, CompoundTag t) {
    var s = state(p);
    long time = now(p);
    dev.cybernetics2077.temporal.Sandevistan.snapshot(p, t);
    HandsRuntime.snapshot(p, t);
    t.putLong("augmentTime", time);
    t.putBoolean("berserk", berserk(p));
    t.putLong("berserkEnd", s.berserkEnd);
    t.putLong("berserkReady", s.berserkReady);
    t.putFloat("meleeRate", meleeRate(p));
    t.putBoolean("camo", camouflaged(p));
    t.putLong("camoEnd", s.camoEnd);
    t.putLong("camoReady", s.camoReady);
    t.putInt("pumpCharges", s.pumpCharges);
    t.putLong("pumpNext", s.pumpNext);
    t.putInt("pumpTier", tier(p, BLOOD_PUMP));
    t.putInt("berserkTier", berserkTier(p));
    t.putBoolean("weaponJammed", ExpansionRuntime.jammed(p));
    var neural = ExpansionRuntime.extra(p).copy();
    neural.putDouble("gunRate", ExpansionRuntime.gunRate(p));
    t.put("expansion", neural);
    t.putFloat("gunRate", (float) ExpansionRuntime.gunRate(p));
    t.putInt("camoTier", tier(p, OPTICAL_CAMO));
    t.putInt("iceTier", tier(p, SELF_ICE));
    t.putLong("iceReady", s.iceReady);
    t.putInt("heartTier", tier(p, SECOND_HEART));
    t.putLong("heartReady", s.heartReady);
    t.putInt("lynxTier", tier(p, LYNX_PAWS));
    t.putInt("legTier", Math.max(tier(p, REINFORCED_TENDONS), tier(p, FORTIFIED_ANKLES)));
    t.putBoolean("ankles", tier(p, FORTIFIED_ANKLES) > 0);
    var list = new ListTag();
    s.highlights.entrySet().stream()
        .sorted(java.util.Map.Entry.comparingByKey())
        .forEach(
            entry -> {
              var mark = new CompoundTag();
              mark.putInt("id", entry.getKey());
              mark.putLong("until", entry.getValue());
              list.add(mark);
            });
    t.put("highlights", list);
  }

  private static void combat(ServerPlayer p) {
    var s = state(p);
    int t = tier(p, ADRENALINE_CONVERTER);
    long time = now(p);
    if (t > 0 && time - s.lastCombat >= 100 && time >= s.adrenalineReady) {
      s.adrenalineEnd = time + (4 + t) * 20L;
      s.adrenalineReady = time + 400;
    }
    s.lastCombat = time;
  }

  @SubscribeEvent
  public static void damage(LivingDamageEvent.Pre e) {
    if (e.getSource().getEntity() instanceof ServerPlayer attacker && attacker != e.getEntity())
      combat(attacker);
    if (!(e.getEntity() instanceof ServerPlayer p)
        || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    if (e.getSource().getEntity() instanceof LivingEntity attacker && attacker != p) combat(p);
    float amount = e.getNewDamage();
    if (ExpansionRuntime.extra(p).getBoolean("payingDebt")) return;
    amount *= 1 - tier(p, PAIN_EDITOR) * .02f;
    if (e.getSource().is(DamageTypeTags.IS_FALL)) amount *= 1 - tier(p, LYNX_PAWS) * .04f;
    if (berserk(p)) {
      if (ExpansionImplants.os(p) == MILITECH_BERSERK) amount = 0;
      else amount *= 1 - berserkTier(p) * .1f;
      amount = Math.min(amount, Math.max(0, p.getHealth() - p.getMaxHealth() * .25f));
    }
    e.setNewDamage(amount);
  }

  @SubscribeEvent
  public static void ranged(LivingIncomingDamageEvent e) {
    if (e.getSource().getEntity() instanceof ServerPlayer p
        && berserk(p)
        && e.getSource().getDirectEntity() != p) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void critical(CriticalHitEvent e) {
    if (!(e.getEntity() instanceof ServerPlayer p)) return;
    boolean blade = p.getMainHandItem().getItem() instanceof net.minecraft.world.item.SwordItem;
    if (e.isCriticalHit()
        || p.getRandom().nextFloat() < AugmentExpansion.weaponCritChance(p, e.getTarget(), blade)) {
      e.setCriticalHit(true);
      float multiplier = Math.max(1.5f, e.getDamageMultiplier());
      if (dev.cybernetics2077.temporal.Sandevistan.active(p))
        multiplier *=
            1
                + dev.cybernetics2077.temporal.Sandevistan.model(p)
                    .critDamage(dev.cybernetics2077.temporal.Sandevistan.tier(p));
      multiplier *= BerserkSpecs.critDamage(p);
      e.setDamageMultiplier(multiplier);
    }
  }

  public static float bladeDamage(ServerPlayer p, LivingEntity target, float damage) {
    return p.getRandom().nextFloat() < AugmentExpansion.weaponCritChance(p, target, true)
        ? damage * AugmentExpansion.critMultiplier(p)
        : damage;
  }

  @SubscribeEvent
  public static void death(LivingDeathEvent e) {
    if (e.getEntity() instanceof ServerPlayer dead) {
      var s = state(dead);
      s.berserkEnd = s.camoEnd = s.pumpEnd = s.adrenalineEnd = s.scanUntil = 0;
      s.berserkKills = 0;
      s.highlights.clear();
      s.wrapEnd = s.smartUntil = 0;
      s.smartTarget = null;
    }
    if (!(e.getSource().getEntity() instanceof ServerPlayer p)
        || p.isAlliedTo(e.getEntity())
        || !enemyKind(p, e.getEntity())) return;
    int heal = tier(p, HEAL_ON_KILL);
    if (heal > 0) p.heal(p.getMaxHealth() * (heal + 1) * .01f);
    int memory = tier(p, MEMORY_BOOST);
    if (memory > 0 && maxRam(p) > 0)
      CyberwareRuntime.state(p).ram =
          Math.min(maxRam(p), CyberwareRuntime.state(p).ram + .35f + .2f * memory);
    if (berserk(p)) state(p).berserkKills++;
  }

  @SubscribeEvent
  public static void use(LivingEntityUseItemEvent.Start e) {
    if (e.getEntity() instanceof Player p && berserk(p)) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void interact(PlayerInteractEvent.RightClickBlock e) {
    if (berserk(e.getEntity())) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void interactItem(PlayerInteractEvent.RightClickItem e) {
    if (berserk(e.getEntity())) e.setCanceled(true);
  }

  @SubscribeEvent
  public static void target(LivingChangeTargetEvent e) {
    if (e.getNewAboutToBeSetTarget() instanceof ServerPlayer p
        && camouflaged(p)
        && e.getEntity().distanceToSqr(p) > 6.25) e.setCanceled(true);
  }

  private AugmentRuntime() {}
}
