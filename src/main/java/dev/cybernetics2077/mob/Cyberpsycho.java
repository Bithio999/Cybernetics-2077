package dev.cybernetics2077.mob;

import static dev.cybernetics2077.cyberware.Augment.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.BountyCombat;
import dev.cybernetics2077.cyberware.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/**
 * Dedicated human enemy: real sockets, persistent medical charges and autonomous combat implants.
 */
public final class Cyberpsycho extends Monster {
  private static final EntityDataAccessor<Integer> RANK =
      SynchedEntityData.defineId(Cyberpsycho.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> BERSERK =
      SynchedEntityData.defineId(Cyberpsycho.class, EntityDataSerializers.BOOLEAN);
  private CompoundTag systems = new CompoundTag();

  public Cyberpsycho(EntityType<? extends Monster> type, Level level) {
    super(type, level);
    setPersistenceRequired();
    setCanPickUpLoot(false);
  }

  public static AttributeSupplier.Builder attributes() {
    return Monster.createMonsterAttributes()
        .add(Attributes.MAX_HEALTH, 40)
        .add(Attributes.ATTACK_DAMAGE, 4)
        .add(Attributes.MOVEMENT_SPEED, .27)
        .add(Attributes.FOLLOW_RANGE, 40)
        .add(Attributes.ARMOR)
        .add(Attributes.ARMOR_TOUGHNESS);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(RANK, 1);
    b.define(BERSERK, false);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(
        2,
        new MeleeAttackGoal(this, 1, false) {
          @Override
          protected int getAttackInterval() {
            return berserk()
                ? Math.max(8, Math.round(20 / (1 + (5 + tier(BIODYNE_BERSERK) * 5) * .01f)))
                : 20;
          }
        });
    goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .8));
    goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
    goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  public int rank() {
    return entityData.get(RANK);
  }

  public int capacity() {
    return getData(Cybernetics2077.CYBERWARE).capacityUsed();
  }

  public int tier(Augment a) {
    return EnhancedMobs.tier(this, a);
  }

  public CompoundTag systems() {
    return systems;
  }

  public boolean berserk() {
    return entityData.get(BERSERK);
  }

  public long now() {
    return level().getGameTime();
  }

  public boolean operational() {
    return isAlive() && !isNoAi() && !isInvulnerable();
  }

  public void configure(int rank) {
    var plan = CyberpsychoLoadouts.forRank(rank);
    setData(Cybernetics2077.CYBERWARE, plan.inventory());
    entityData.set(RANK, plan.rank());
    systems = new CompoundTag();
    systems.putInt("pumpCharges", 2);
    setItemSlot(
        EquipmentSlot.MAINHAND,
        new ItemStack(rank >= 5 ? Items.NETHERITE_SWORD : Items.IRON_SWORD));
    setDropChance(EquipmentSlot.MAINHAND, 0);
    refresh(true);
  }

  @Override
  public void onAddedToLevel() {
    super.onAddedToLevel();
    if (level().isClientSide) return;
    if (capacity() <= CyberpsychoRules.BASE_CAPACITY) configure(rank());
    else {
      entityData.set(RANK, CyberpsychoRules.rank(capacity()));
      refresh(false);
    }
  }

  private void modifier(
      net.minecraft.core.Holder<Attribute> attr,
      String name,
      double value,
      AttributeModifier.Operation op) {
    var instance = getAttribute(attr);
    if (instance == null) return;
    var id = Cybernetics2077.id("psycho_" + name);
    var old = instance.getModifier(id);
    if (old != null && old.amount() == value && old.operation() == op) return;
    instance.removeModifier(id);
    if (value != 0) instance.addPermanentModifier(new AttributeModifier(id, value, op));
  }

  public void refresh(boolean fill) {
    BountyCombat.apply(this, rank(), true, false);
    double armor = 0, toughness = 0;
    for (var a : Augment.values()) {
      armor += a.armor(tier(a));
      toughness += a.toughness(tier(a));
    }
    modifier(Attributes.ARMOR, "armor", armor, AttributeModifier.Operation.ADD_VALUE);
    modifier(
        Attributes.ARMOR_TOUGHNESS, "toughness", toughness, AttributeModifier.Operation.ADD_VALUE);
    modifier(
        Attributes.ARMOR,
        "para",
        tier(PARA_BELLUM) * .04,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    modifier(
        Attributes.MAX_HEALTH,
        "epi",
        Math.max(0, tier(EPIMORPHIC_SKELETON) - 2) * .05,
        AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    if (fill) setHealth(getMaxHealth());
    else if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
  }

  public boolean usePump() {
    int t = tier(BLOOD_PUMP);
    if (t == 0
        || systems.getInt("pumpCharges") <= 0
        || !operational()
        || getHealth() >= getMaxHealth()) return false;
    systems.putInt("pumpCharges", systems.getInt("pumpCharges") - 1);
    if (systems.getLong("pumpNext") == 0) systems.putLong("pumpNext", now() + (50 - t * 5) * 20L);
    systems.putLong("pumpEnd", now() + 120);
    systems.putLong("pumpPulse", now() + 20);
    heal(getMaxHealth() * (.10f + .05f * t));
    ((ServerLevel) level())
        .sendParticles(ParticleTypes.HEART, getX(), getEyeY(), getZ(), 5, .25, .2, .25, .02);
    return true;
  }

  public boolean blockHack() {
    int t = tier(SELF_ICE);
    if (!operational() || t == 0 || now() < systems.getLong("iceReady")) return false;
    systems.putLong("iceReady", now() + (50 - t * 6) * 20L);
    ((ServerLevel) level())
        .sendParticles(
            ParticleTypes.ELECTRIC_SPARK, getX(), getEyeY(), getZ(), 15, .3, .3, .3, .02);
    return true;
  }

  @Override
  public void tick() {
    super.tick();
    if (level().isClientSide) return;
    if (!operational()) {
      entityData.set(BERSERK, false);
      return;
    }
    long n = now();
    int b = tier(BIODYNE_BERSERK), a = tier(ADRENALINE_CONVERTER), pump = tier(BLOOD_PUMP);
    var target = getTarget();
    boolean combat = target != null && target.isAlive();
    if (systems.getLong("berserkEnd") > 0 && n >= systems.getLong("berserkEnd")) {
      if (b > 0) heal(getMaxHealth() * .25f * systems.getInt("berserkKills"));
      systems.putInt("berserkKills", 0);
      systems.putLong("berserkEnd", 0);
    }
    if (combat) {
      if (b > 0 && n >= systems.getLong("berserkReady")) {
        systems.putLong("berserkEnd", n + (6 + b) * 20L);
        systems.putLong("berserkReady", n + (60 - 5 * b) * 20L);
        level()
            .playSound(
                null,
                blockPosition(),
                SoundEvents.IRON_GOLEM_REPAIR,
                SoundSource.HOSTILE,
                .8f,
                .7f);
      }
      if (!systems.getBoolean("inCombat") && n >= systems.getLong("adrenalineReady")) {
        systems.putLong("adrenalineEnd", n + (4 + a) * 20L);
        systems.putLong("adrenalineReady", n + 400);
      }
      if (!systems.getBoolean("inCombat")
          && tier(HANDLE_WRAP) > 0
          && n >= systems.getLong("wrapReady")) {
        systems.putLong("wrapEnd", n + 120);
        systems.putLong("wrapReady", n + 200);
      }
      systems.putLong("lastCombat", n);
      systems.putBoolean("inCombat", true);
      int legs = tier(REINFORCED_TENDONS);
      double distance = distanceToSqr(target);
      if (legs > 0
          && onGround()
          && !isInWater()
          && distance > 16
          && distance < 81
          && hasLineOfSight(target)
          && n >= systems.getLong("jumpReady")) {
        var dir = target.position().subtract(position()).multiply(1, 0, 1).normalize();
        setDeltaMovement(dir.scale(.45).add(0, .42, 0));
        systems.putLong("doubleJumpAt", n + 5);
        systems.putLong("doubleJumpUntil", n + 20);
        hasImpulse = true;
        systems.putLong("jumpReady", n + 100);
      }
    } else if (n - systems.getLong("lastCombat") >= 100) systems.putBoolean("inCombat", false);
    if (combat
        && !onGround()
        && systems.getLong("doubleJumpAt") > 0
        && n >= systems.getLong("doubleJumpAt")
        && n <= systems.getLong("doubleJumpUntil")
        && tier(REINFORCED_TENDONS) > 0
        && hasLineOfSight(target)) {
      var direction = target.position().subtract(position()).multiply(1, 0, 1).normalize();
      setDeltaMovement(direction.scale(.45).add(0, .42 + tier(REINFORCED_TENDONS) * .035, 0));
      hasImpulse = true;
      systems.putLong("doubleJumpAt", 0);
      systems.putLong("lastDoubleJump", n);
    }
    entityData.set(BERSERK, b > 0 && n < systems.getLong("berserkEnd"));
    modifier(
        Attributes.MOVEMENT_SPEED,
        "adrenaline",
        n < systems.getLong("adrenalineEnd") ? a * .08 : 0,
        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    while (pump > 0
        && systems.getInt("pumpCharges") < 2
        && systems.getLong("pumpNext") > 0
        && n >= systems.getLong("pumpNext")) {
      int charges = systems.getInt("pumpCharges") + 1;
      systems.putInt("pumpCharges", charges);
      systems.putLong(
          "pumpNext", charges == 2 ? 0 : systems.getLong("pumpNext") + (50 - pump * 5) * 20L);
    }
    if (tier(BIOMONITOR) > 0
        && getHealth() < getMaxHealth() * .5f
        && n >= systems.getLong("monitorReady")) {
      systems.putLong("monitorReady", n + 40);
      usePump();
    }
    if (pump > 0 && n < systems.getLong("pumpEnd") && n >= systems.getLong("pumpPulse")) {
      systems.putLong("pumpPulse", n + 20);
      heal(getMaxHealth() * pump * .005f);
    }
  }

  @Override
  public boolean doHurtTarget(Entity target) {
    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    boolean hit = super.doHurtTarget(target);
    if (hit && target instanceof LivingEntity living)
      CyberpsychoCombat.microgenerator(this, living);
    return hit;
  }

  @Override
  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putInt("PsychoRank", rank());
    t.put("PsychoSystems", systems.copy());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    entityData.set(RANK, Math.clamp(t.getInt("PsychoRank"), 1, 6));
    systems = t.getCompound("PsychoSystems").copy();
  }
}
