package dev.cybernetics2077.combat;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.CyberdeckItem;
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
    public boolean pendingHit;
    public final ArrayDeque<Upload> uploads = new ArrayDeque<>();
    private CompoundTag sent;
  }

  public record Upload(UUID target, long start, long finish, int id) {}

  private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();

  public static State state(ServerPlayer p) {
    return STATES.computeIfAbsent(p, k -> new State());
  }

  public static net.minecraft.world.item.ItemStack deck(Player p) {
    return p.getData(Cybernetics2077.CYBERWARE)
        .getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot());
  }

  public static boolean claws(Player p) {
    return p.getData(Cybernetics2077.CYBERWARE)
        .getStackInSlot(CyberwareCategory.ARMS.firstSlot())
        .is(Cybernetics2077.MANTIS.get());
  }

  public static boolean emptyHands(Player p) {
    return p.getMainHandItem().isEmpty() && p.getOffhandItem().isEmpty();
  }

  public static boolean usable(Player p) {
    return p.isAlive() && !p.isSpectator() && p.containerMenu == p.inventoryMenu;
  }

  public static CompoundTag snapshot(ServerPlayer p) {
    var s = state(p);
    var t = new CompoundTag();
    t.putInt("entity", p.getId());
    t.putBoolean("deck", deck(p).getItem() instanceof CyberdeckItem);
    t.putBoolean("shortCircuit", CyberdeckItem.has(deck(p), Cybernetics2077.SHORT_CIRCUIT.get()));
    t.putBoolean("claws", claws(p));
    t.putBoolean("deployed", s.deployed);
    t.putFloat("ram", s.ram);
    t.putLong("time", p.serverLevel().getGameTime());
    t.putLong("changed", s.changed);
    t.putLong("attack", s.attack);
    t.putInt("combo", s.combo);
    t.putLong("charge", s.charge);
    var queue = new ListTag();
    for (var upload : s.uploads) {
      var q = new CompoundTag();
      q.putInt("target", upload.id);
      q.putLong("start", upload.start);
      q.putLong("finish", upload.finish);
      queue.add(q);
    }
    t.put("uploads", queue);
    return t;
  }

  public static void syncIfChanged(ServerPlayer p) {
    var s = state(p);
    var t = snapshot(p);
    var compare = t.copy();
    compare.remove("time");
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
    var s = state(p);
    long now = p.serverLevel().getGameTime();
    if (!usable(p)) return;
    if (action == 0) {
      if (!claws(p)) return;
      if (!s.deployed && !emptyHands(p)) {
        p.displayClientMessage(Component.translatable("cybernetics2077.mantis.empty_hands"), true);
        return;
      }
      if (now - s.changed < 6) return;
      s.deployed = !s.deployed;
      s.changed = now;
      s.charge = -1;
      s.pendingHit = false;
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
    if (!claws(p) || !s.deployed || !emptyHands(p) || now - s.changed < CombatRules.DEPLOY) return;
    if (action == 2) {
      if (s.charge < 0 && CombatRules.ready(now, s.lastAttack, CombatRules.LIGHT_INTERVAL)) {
        s.charge = now;
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
        s.combo == 3 ? CombatRules.HEAVY_INTERVAL : CombatRules.LIGHT_INTERVAL)) {
      broadcast(p);
      return;
    }
    s.combo = heavy ? 3 : (now - s.lastAttack > 30 ? 0 : s.nextCombo);
    s.nextCombo = (s.combo + 1) % 3;
    s.attack = now;
    s.lastAttack = now;
    s.pendingHit = true;
    if (heavy) {
      // A short collision-tested lunge, never a terrain-ignoring teleport.
      var direction = p.getLookAngle().multiply(1, 0, 1).normalize();
      if (!p.horizontalCollision) {
        p.setDeltaMovement(p.getDeltaMovement().add(direction.scale(.55)));
        p.hurtMarked = true;
      }
    }
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
    var s = state(p);
    if (!usable(p)
        || !ScannerNetwork.installed(p)
        || !(deck(p).getItem() instanceof CyberdeckItem)
        || !CyberdeckItem.has(deck(p), Cybernetics2077.SHORT_CIRCUIT.get())
        || s.uploads.size() >= 2
        || s.ram + .0001f < CombatRules.RAM_COST
        || !ScannerNetwork.inspect(p, id).contains("id")) return false;
    var target = p.serverLevel().getEntity(id);
    if (!(target instanceof LivingEntity living) || !canAttack(p, living)) return false;
    long now = p.serverLevel().getGameTime();
    long start = s.uploads.isEmpty() ? now : Math.max(now, s.uploads.getLast().finish);
    s.ram -= CombatRules.RAM_COST;
    s.uploads.add(new Upload(target.getUUID(), start, start + CombatRules.UPLOAD, id));
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
    var s = state(p);
    long now = p.serverLevel().getGameTime();
    if (s.charge >= 0 && (!usable(p) || !s.deployed)) {
      s.charge = -1;
      broadcast(p);
    }
    if (s.deployed && (!claws(p) || !p.isAlive() || p.isSpectator() || !emptyHands(p))) {
      s.deployed = false;
      s.changed = now;
      s.charge = -1;
      s.pendingHit = false;
      broadcast(p);
    }
    if (s.deployed && s.pendingHit && now - s.attack >= CombatRules.impactTick(s.combo)) {
      s.pendingHit = false;
      if (usable(p) && claws(p) && emptyHands(p)) bladeHit(p, s.combo);
    }
    if (!(deck(p).getItem() instanceof CyberdeckItem)
        || !CyberdeckItem.has(deck(p), Cybernetics2077.SHORT_CIRCUIT.get())
        || !p.isAlive()) {
      if (!s.uploads.isEmpty()) {
        s.ram = Math.min(CombatRules.RAM_MAX, s.ram + s.uploads.size() * CombatRules.RAM_COST);
        s.uploads.clear();
      }
    }
    if (deck(p).getItem() instanceof CyberdeckItem)
      s.ram = Math.min(CombatRules.RAM_MAX, s.ram + CombatRules.RAM_PER_TICK);
    if (!s.uploads.isEmpty() && s.uploads.getFirst().finish <= now) {
      var upload = s.uploads.removeFirst();
      var target = p.serverLevel().getEntity(upload.target);
      if (target instanceof LivingEntity living
          && canAttack(p, living)
          && p.distanceToSqr(living) <= 48 * 48) {
        if (living.hurt(
            new net.minecraft.world.damagesource.DamageSource(
                p.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                    .getHolderOrThrow(
                        net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                            Cybernetics2077.id("short_circuit"))),
                p,
                p),
            CombatRules.SHORT_CIRCUIT_DAMAGE)) {
          p.serverLevel()
              .sendParticles(
                  ParticleTypes.ELECTRIC_SPARK,
                  living.getX(),
                  living.getY() + living.getBbHeight() * .55,
                  living.getZ(),
                  35,
                  living.getBbWidth() * .5,
                  living.getBbHeight() * .3,
                  living.getBbWidth() * .5,
                  .15);
          p.level()
              .playSound(
                  null,
                  living.blockPosition(),
                  SoundEvents.TRIDENT_THUNDER.value(),
                  SoundSource.PLAYERS,
                  .3f,
                  1.8f);
          PacketDistributor.sendToPlayersTrackingEntityAndSelf(
              p, new CyberwareNetwork.Impact(living.getId()));
        }
      }
    }
    if (now % 4 == 0) syncIfChanged(p);
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
    if (target.hurt(p.damageSources().playerAttack(p), CombatRules.damage(combo))) {
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
      state(p).uploads.clear();
      state(p).deployed = false;
      state(p).charge = -1;
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
    if (e.getEntity() instanceof ServerPlayer p && state(p).deployed) e.setCanceled(true);
  }

  private CyberwareRuntime() {}
}
