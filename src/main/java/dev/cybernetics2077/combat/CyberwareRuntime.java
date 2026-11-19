package dev.cybernetics2077.combat;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.CyberdeckItem;
import dev.cybernetics2077.deck.QuickhackItem;
import dev.cybernetics2077.scanner.ScannerNetwork;
import java.util.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-authoritative equipment, RAM reservation, uploads and blade contact frames. */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class CyberwareRuntime {
  public static final class State {
    public float ram = CombatRules.RAM_MAX;
    public boolean deployed;
    public long changed = -1000, attack = -1000, charge = -1, lastAttack = -1000;
    public int combo, nextCombo, target = -1;
    public boolean pendingHit, lungeFinished;
    public Lunge lunge;
    public final ArrayDeque<Upload> uploads = new ArrayDeque<>();
    private CompoundTag sent;
  }

  public record Upload(
      UUID target,
      long start,
      long finish,
      int id,
      net.minecraft.world.item.ItemStack sourceDeck,
      QuickhackItem program,
      float cost,
      float damage) {}

  public record Lunge(UUID target, LungePath path, long started) {}

  private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();

  public static State state(ServerPlayer p) {
    return STATES.computeIfAbsent(p, k -> new State());
  }

  public static net.minecraft.world.item.ItemStack deck(Player p) {
    return p.getData(Cybernetics2077.CYBERWARE)
        .getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot());
  }

  public static boolean claws(Player p) {
    return bladeTier(p) != null;
  }

  public static CyberwareTier bladeTier(Player p) {
    var stack =
        p.getData(Cybernetics2077.CYBERWARE).getStackInSlot(CyberwareCategory.ARMS.firstSlot());
    return stack.getItem() instanceof MantisBladesItem item ? item.tier() : null;
  }

  public static CyberwareTier deckTier(Player p) {
    return deck(p).getItem() instanceof CyberdeckItem item ? item.tier() : CyberwareTier.ONE;
  }

  public static float quickhackDamage(Player p, QuickhackItem program) {
    float bonus = deck(p).getItem() instanceof CyberdeckItem item ? item.damageBonus() : 0;
    float damage = program.damage() * (1 + bonus);
    if (program.kind() == dev.cybernetics2077.deck.QuickhackKind.SYNAPSE_BURNOUT
        && p instanceof ServerPlayer server) {
      float spent = Math.max(0, AugmentRuntime.maxRam(p) - state(server).ram + program.cost());
      damage *= 1 + Math.min(1, spent * .1f);
    }
    return damage;
  }

  /** Equipment changes cancel only affected reservations; swapping decks cannot amplify uploads. */
  private static void reconcileUploads(ServerPlayer p) {
    var s = state(p);
    var deck = deck(p);
    var programs = CyberdeckItem.programs(deck);
    var it = s.uploads.iterator();
    while (it.hasNext()) {
      var upload = it.next();
      if (!p.isAlive() || upload.sourceDeck != deck || !programs.contains(upload.program)) {
        s.ram += upload.cost;
        it.remove();
      }
    }
    s.ram = Math.clamp(s.ram, 0, Math.max(CombatRules.RAM_MAX, AugmentRuntime.maxRam(p)));
  }

  public static boolean emptyHands(Player p) {
    return p.getInventory().getSelected().isEmpty() && p.getInventory().offhand.get(0).isEmpty();
  }

  public static boolean usable(Player p) {
    return p.isAlive() && !p.isSpectator() && p.containerMenu == p.inventoryMenu;
  }

  public static CompoundTag snapshot(ServerPlayer p) {
    var s = state(p);
    var t = new CompoundTag();
    t.putInt("entity", p.getId());
    t.putBoolean("deck", deck(p).getItem() instanceof CyberdeckItem);
    var program = CyberdeckItem.quickhack(deck(p));
    t.putBoolean(
        "shortCircuit",
        CyberdeckItem.programs(deck(p)).stream()
            .anyMatch(q -> q.kind() == dev.cybernetics2077.deck.QuickhackKind.SHORT_CIRCUIT));
    var programs = new ListTag();
    for (var q : CyberdeckItem.programs(deck(p))) programs.add(programData(p, q));
    t.put("programs", programs);
    t.putInt("deckTier", deckTier(p).level);
    t.putInt("ramMax", AugmentRuntime.maxRam(p));
    t.putInt("programSlots", deckTier(p).programSlots());
    t.putInt("queueLimit", deckTier(p).queueLimit());
    t.putInt("hackTier", program == null ? 0 : program.tier().level);
    t.putInt("hackCost", program == null ? 0 : program.cost());
    t.putFloat("hackDamage", program == null ? 0 : quickhackDamage(p, program));
    t.putInt("uploadTicks", program == null ? 0 : AugmentRuntime.uploadTicks(p, program));
    AugmentRuntime.snapshot(p, t);
    t.putBoolean("claws", claws(p));
    t.putBoolean("deployed", s.deployed);
    t.putFloat("ram", s.ram);
    t.putLong("time", p.serverLevel().getGameTime());
    t.putLong("changed", s.changed);
    t.putLong("attack", s.attack);
    t.putInt("combo", s.combo);
    t.putLong("charge", s.charge);
    if (s.lunge != null) {
      var l = s.lunge;
      var path = l.path();
      var q = new CompoundTag();
      q.putLong("started", l.started());
      q.putInt("ticks", path.ticks());
      q.putDouble("arc", path.arc());
      q.putDouble("sx", path.sx());
      q.putDouble("sy", path.sy());
      q.putDouble("sz", path.sz());
      q.putDouble("ex", path.ex());
      q.putDouble("ey", path.ey());
      q.putDouble("ez", path.ez());
      t.put("lunge", q);
    }
    var queue = new ListTag();
    for (var upload : s.uploads) {
      var q = new CompoundTag();
      q.putInt("target", upload.id);
      q.putLong("start", upload.start);
      q.putLong("finish", upload.finish);
      q.putString("icon", upload.program.kind().icon());
      q.putString("name", upload.program.getDescription().getString());
      queue.add(q);
    }
    t.put("uploads", queue);
    return t;
  }

  public static CompoundTag programData(ServerPlayer p, QuickhackItem program) {
    var t = new CompoundTag();
    t.putInt("kind", program.kind().ordinal());
    t.putString("name", program.getDescription().getString());
    t.putString("icon", program.kind().icon());
    t.putString("category", program.kind().category);
    t.putString("type", program.kind().damageType);
    t.putInt("tier", program.tier().level);
    t.putInt("cost", program.cost());
    t.putFloat("damage", quickhackDamage(p, program));
    t.putInt("duration", program.duration());
    t.putInt("uploadTicks", AugmentRuntime.uploadTicks(p, program));
    return t;
  }

  public static void syncIfChanged(ServerPlayer p) {
    var s = state(p);
    var t = snapshot(p);
    var compare = t.copy();
    compare.remove("time");
    compare.remove("augmentTime");
    compare.remove("hackDamage");
    for (var value : compare.getList("programs", 10)) ((CompoundTag) value).remove("damage");
    // RAM and countdown are quantized for UI; at most 5 updates/sec while regenerating.
    compare.putFloat("ram", Math.round(s.ram * 10) / 10f);
    if (!compare.equals(s.sent)) {
      s.sent = compare;
      PacketDistributor.sendToPlayer(p, new CyberwareNetwork.State(t));
    }
  }

  public static void broadcast(ServerPlayer p) {
    var packet = new CyberwareNetwork.State(snapshot(p));
    PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, packet);
    state(p).sent = null;
  }

  public static void action(ServerPlayer p, int action) {
    if (action >= 5) {
      AugmentRuntime.action(p, action);
      return;
    }
    var s = state(p);
    long now = p.serverLevel().getGameTime();
    if (action == 4) {
      if (s.lunge != null) s.lungeFinished = true;
      return;
    }
    if (!usable(p)) return;
    if (action == 0) {
      if (!claws(p)) return;
      if (!s.deployed && !emptyHands(p)) {
        p.displayClientMessage(Component.translatable("cybernetics2077.mantis.empty_hands"), true);
        return;
      }
      if (now - s.changed < 6) return;
      long elapsed = Math.clamp(now - s.changed, 0, CombatRules.DEPLOY);
      s.deployed = !s.deployed;
      s.changed = now - (CombatRules.DEPLOY - elapsed);
      s.charge = -1;
      s.pendingHit = false;
      s.lunge = null;
      p.level()
          .playSound(
              null,
              p.blockPosition(),
              s.deployed ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
              SoundSource.PLAYERS,
              .65f,
              1.5f);
      broadcast(p);
      return;
    }
    if (!claws(p) || !s.deployed) return;
    if (action == 2) {
      if (s.charge < 0) {
        s.charge = Math.max(now, s.changed + CombatRules.DEPLOY);
        broadcast(p);
      }
      return;
    }
    if (now - s.changed < CombatRules.DEPLOY) {
      if (action == 3) {
        s.charge = -1;
        broadcast(p);
      }
      return;
    }
    if (action != 1 && action != 3) return;
    if (action == 3 && s.charge < 0) return;
    boolean heavy = action == 3 && now - s.charge >= 8;
    s.charge = -1;
    if (!CombatRules.ready(
        now,
        s.lastAttack,
        AugmentRuntime.interval(
            p, s.combo == 3 ? CombatRules.HEAVY_INTERVAL : CombatRules.LIGHT_INTERVAL))) {
      broadcast(p);
      return;
    }
    s.combo = heavy ? 3 : (now - s.lastAttack > 30 ? 0 : s.nextCombo);
    s.nextCombo = (s.combo + 1) % 3;
    s.attack = now;
    s.lastAttack = now;
    s.pendingHit = true;
    if (heavy) beginLunge(p);
    p.level()
        .playSound(
            null,
            p.blockPosition(),
            SoundEvents.PLAYER_ATTACK_SWEEP,
            SoundSource.PLAYERS,
            .7f,
            heavy ? .65f : 1.15f + s.combo * .1f);
    broadcast(p);
  }

  public static boolean queue(ServerPlayer p, int id) {
    var program = CyberdeckItem.quickhack(deck(p));
    return program != null && queue(p, id, program.kind().ordinal());
  }

  public static boolean queue(ServerPlayer p, int id, int kind) {
    var s = state(p);
    reconcileUploads(p);
    var program =
        CyberdeckItem.programs(deck(p)).stream()
            .filter(q -> q.kind().ordinal() == kind)
            .findFirst()
            .orElse(null);
    if (!usable(p)
        || !ScannerNetwork.installed(p)
        || !(deck(p).getItem() instanceof CyberdeckItem)
        || program == null
        || s.uploads.size() >= deckTier(p).queueLimit()
        || s.ram + .0001f < program.cost()
        || !ScannerNetwork.inspect(p, id).contains("id")) return false;
    var target = p.serverLevel().getEntity(id);
    if (!(target instanceof LivingEntity living) || !canAttack(p, living)) return false;
    long now = p.serverLevel().getGameTime();
    long start = s.uploads.isEmpty() ? now : Math.max(now, s.uploads.getLast().finish);
    int cost = program.cost();
    float damage = quickhackDamage(p, program);
    s.ram -= cost;
    s.uploads.add(
        new Upload(
            target.getUUID(),
            start,
            start + AugmentRuntime.uploadTicks(p, program),
            id,
            deck(p),
            program,
            cost,
            damage));
    syncIfChanged(p);
    return true;
  }

  public static boolean canAttack(ServerPlayer p, LivingEntity target) {
    return target != p
        && target.isAlive()
        && !target.isSpectator()
        && !p.isAlliedTo(target)
        && (!(target instanceof Player other) || p.server.isPvpAllowed() && p.canHarmPlayer(other));
  }

  public static void tick(ServerPlayer p) {
    AugmentRuntime.tick(p);
    var s = state(p);
    long now = p.serverLevel().getGameTime();
    if (s.charge >= 0 && (!usable(p) || !s.deployed)) {
      s.charge = -1;
      broadcast(p);
    }
    if (s.deployed && (!claws(p) || !p.isAlive() || p.isSpectator())) {
      long elapsed = Math.clamp(now - s.changed, 0, CombatRules.DEPLOY);
      s.deployed = false;
      s.changed = now - (CombatRules.DEPLOY - elapsed);
      s.charge = -1;
      s.pendingHit = false;
      s.lunge = null;
      broadcast(p);
    }
    if (s.lunge != null) tickLunge(p);
    if (s.deployed
        && s.lunge == null
        && s.pendingHit
        && now - s.attack >= AugmentRuntime.interval(p, CombatRules.impactTick(s.combo))) {
      s.pendingHit = false;
      if (usable(p) && claws(p)) bladeHit(p, s.combo);
    }
    reconcileUploads(p);
    if (deck(p).getItem() instanceof CyberdeckItem item)
      s.ram =
          Math.min(
              AugmentRuntime.maxRam(p),
              s.ram
                  + (item.ramPerSecond()
                          + dev.cybernetics2077.cyberware.AugmentExpansion.ramRecovery(p))
                      / 20);
    if (!s.uploads.isEmpty() && s.uploads.getFirst().finish <= now) {
      var upload = s.uploads.removeFirst();
      var target = p.serverLevel().getEntity(upload.target);
      if (target instanceof LivingEntity living
          && canAttack(p, living)
          && p.distanceToSqr(living) <= Math.pow(ScannerNetwork.range(p) + 16, 2)) {
        QuickhackEffects.land(p, living, upload.program, upload.damage, upload.sourceDeck);
      }
    }
    if (now % 4 == 0) syncIfChanged(p);
  }

  public static LivingEntity lungeTarget(ServerPlayer p) {
    var eye = p.getEyePosition();
    var direction = p.getLookAngle();
    LivingEntity nearest = null;
    double best = 9 * 9;
    for (var target :
        p.serverLevel()
            .getEntitiesOfClass(
                LivingEntity.class, p.getBoundingBox().inflate(8), e -> canAttack(p, e))) {
      var center = target.getBoundingBox().getCenter();
      var delta = center.subtract(eye);
      double distance = delta.length();
      if (distance < .1
          || distance > 8
          || direction.dot(delta.scale(1 / distance)) < .95
          || !p.hasLineOfSight(target)) continue;
      if (distance * distance < best) {
        best = distance * distance;
        nearest = target;
      }
    }
    return nearest;
  }

  private static void beginLunge(ServerPlayer p) {
    var target = lungeTarget(p);
    if (p.isPassenger()) return;
    var start = p.position();
    var destination = start.add(p.getLookAngle().scale(6));
    if (target != null) {
      var toward = target.position().subtract(start).multiply(1, 0, 1);
      double separation = (p.getBbWidth() + target.getBbWidth()) * .5 + .75;
      if (toward.length() <= separation + .15) return;
      destination = target.position().subtract(toward.normalize().scale(separation));
    }
    int duration =
        Math.max(
            2,
            (int)
                Math.ceil(
                    LungePath.duration(start.distanceTo(destination))
                        / dev.cybernetics2077.temporal.Sandevistan.playerPace(p)));
    var path =
        new LungePath(
            start.x, start.y, start.z, destination.x, destination.y, destination.z, .65, duration);
    state(p).lungeFinished = false;
    state(p).lunge =
        new Lunge(target == null ? null : target.getUUID(), path, p.serverLevel().getGameTime());
  }

  private static void tickLunge(ServerPlayer p) {
    var s = state(p);
    var l = s.lunge;
    long age = p.serverLevel().getGameTime() - l.started();
    var target = l.target() == null ? null : p.serverLevel().getEntity(l.target());
    boolean valid =
        usable(p)
            && !p.isPassenger()
            && claws(p)
            && (l.target() == null
                || target instanceof LivingEntity living && canAttack(p, living));
    boolean arrived =
        valid
            && age >= 2
            && (l.target() == null
                ? p.position().distanceToSqr(l.path().ex(), l.path().ey(), l.path().ez()) < .25
                : target.getBoundingBox().inflate(1.15).intersects(p.getBoundingBox()));
    boolean contact = arrived || valid && target == null && s.lungeFinished;
    // Completion follows the client's normal position packet. Allow time for slow frames/latency;
    // a blocked path still cancels immediately on acknowledgement, with no terrain bypass.
    if (!valid || contact || s.lungeFinished || age > l.path().ticks() + 40) {
      s.lunge = null;
      if (contact) {
        s.pendingHit = false;
        s.attack =
            p.serverLevel().getGameTime() - AugmentRuntime.interval(p, CombatRules.impactTick(3));
        if (target == null) bladeHit(p, 3);
        else if (p.hasLineOfSight(target)) hit(p, (LivingEntity) target, 3);
      } else s.pendingHit = false;
      // Hand back to ordinary collision movement without erasing the leap velocity.
      broadcast(p);
    }
  }

  public static LivingEntity bladeTarget(ServerPlayer p, double reach) {
    var eye = p.getEyePosition();
    var end = eye.add(p.getLookAngle().scale(reach));
    LivingEntity best = null;
    double distance = reach * reach;
    for (var target :
        p.serverLevel()
            .getEntitiesOfClass(
                LivingEntity.class, p.getBoundingBox().inflate(reach), e -> canAttack(p, e))) {
      var hit = target.getBoundingBox().inflate(.25).clip(eye, end);
      if (hit.isPresent() && eye.distanceToSqr(hit.get()) < distance && p.hasLineOfSight(target)) {
        best = target;
        distance = eye.distanceToSqr(hit.get());
      }
    }
    return best;
  }

  private static void bladeHit(ServerPlayer p, int combo) {
    var target = bladeTarget(p, combo == 3 ? 4 : 3.3);
    if (target == null) return;
    hit(p, target, combo);
  }

  private static void hit(ServerPlayer p, LivingEntity target, int combo) {
    var tier = bladeTier(p);
    if (tier != null
        && target.hurt(
            p.damageSources().playerAttack(p),
            AugmentRuntime.bladeDamage(
                p, target, CombatRules.damage(combo) * tier.bladeMultiplier()))) {
      target.knockback(combo == 3 ? .55 : .2, p.getX() - target.getX(), p.getZ() - target.getZ());
      p.serverLevel()
          .sendParticles(
              ParticleTypes.SWEEP_ATTACK,
              target.getX(),
              target.getY() + target.getBbHeight() * .55,
              target.getZ(),
              1,
              0,
              0,
              0,
              0);
      p.level()
          .playSound(
              null,
              target.blockPosition(),
              SoundEvents.PLAYER_ATTACK_STRONG,
              SoundSource.PLAYERS,
              .8f,
              combo == 3 ? .7f : 1.2f);
    }
  }

  @SubscribeEvent
  public static void tick(PlayerTickEvent.Post e) {
    if (e.getEntity() instanceof ServerPlayer p) tick(p);
  }

  @SubscribeEvent
  public static void login(PlayerEvent.PlayerLoggedInEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      STATES.remove(p);
      broadcast(p);
    }
  }

  @SubscribeEvent
  public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      STATES.remove(p);
      broadcast(p);
    }
  }

  @SubscribeEvent
  public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e) {
    if (e.getEntity() instanceof ServerPlayer p) {
      var s = state(p);
      s.uploads.clear();
      var augments = AugmentRuntime.state(p);
      augments.highlights.clear();
      augments.scanUntil = 0;
      augments.ankleStart = -1;
      augments.airJumped = false;
      s.deployed = false;
      s.changed = p.serverLevel().getGameTime() - CombatRules.DEPLOY;
      s.charge = -1;
      s.lunge = null;
      broadcast(p);
    }
  }

  @SubscribeEvent
  public static void tracking(PlayerEvent.StartTracking e) {
    if (e.getEntity() instanceof ServerPlayer p && e.getTarget() instanceof ServerPlayer target)
      PacketDistributor.sendToPlayer(p, new CyberwareNetwork.State(snapshot(target)));
  }

  @SubscribeEvent
  public static void vanillaAttack(AttackEntityEvent e) {
    if (MantisHands.occupied(e.getEntity())) e.setCanceled(true);
  }

  private CyberwareRuntime() {}
}
