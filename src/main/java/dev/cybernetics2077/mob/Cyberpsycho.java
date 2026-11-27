package dev.cybernetics2077.mob;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.BountyCombat;
import dev.cybernetics2077.cyberware.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Dedicated human combatant. Cyberware behavior is shared with every other chromed mob. */
public final class Cyberpsycho extends Monster implements RangedAttackMob {
  private static final EntityDataAccessor<Integer>
      RANK = SynchedEntityData.defineId(Cyberpsycho.class, EntityDataSerializers.INT),
      ROLE = SynchedEntityData.defineId(Cyberpsycho.class, EntityDataSerializers.INT);
  private static final EntityDataAccessor<Boolean> BERSERK =
      SynchedEntityData.defineId(Cyberpsycho.class, EntityDataSerializers.BOOLEAN);
  private long seed;

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
    b.define(ROLE, 0);
    b.define(BERSERK, false);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(
        1,
        new AvoidEntityGoal<>(
            this,
            Player.class,
            6,
            1,
            1.2,
            p -> role() == CyberpsychoLoadouts.Role.NETRUNNER && MobImplants.hasPrograms(this)));
    goalSelector.addGoal(2, new RangedBowAttackGoal<Cyberpsycho>(this, 1, 30, 22));
    goalSelector.addGoal(
        3,
        new MeleeAttackGoal(this, 1, false) {
          @Override
          public boolean canUse() {
            return !getMainHandItem().is(Items.BOW) && super.canUse();
          }

          @Override
          protected int getAttackInterval() {
            return Math.max(6, (int) Math.round(20 / MobImplants.meleeRate(Cyberpsycho.this)));
          }
        });
    goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .8));
    goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
    goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    targetSelector.addGoal(1, new HurtByTargetGoal(this));
    targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
  }

  @Override
  public void performRangedAttack(LivingEntity target, float power) {
    if (ExpansionRuntime.jammed(this) || !hasLineOfSight(target)) return;
    var arrow = new Arrow(level(), this, new ItemStack(Items.ARROW), getMainHandItem());
    double dx = target.getX() - getX(),
        dz = target.getZ() - getZ(),
        dy = target.getY(.5) - arrow.getY();
    arrow.setBaseDamage(2 + rank() * .5);
    arrow.shoot(dx, dy + Math.hypot(dx, dz) * .12, dz, 1.6f, 6);
    level().addFreshEntity(arrow);
    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
  }

  public int rank() {
    return entityData.get(RANK);
  }

  public CyberpsychoLoadouts.Role role() {
    return CyberpsychoLoadouts.Role.values()[Math.clamp(entityData.get(ROLE), 0, 2)];
  }

  public int capacity() {
    return getData(Cybernetics2077.CYBERWARE).capacityUsed();
  }

  public int tier(Augment a) {
    return AugmentRuntime.tier(this, a);
  }

  public CompoundTag systems() {
    return ExpansionRuntime.extra(this);
  }

  public boolean berserk() {
    return entityData.get(BERSERK);
  }

  public void combatVisual(boolean active) {
    entityData.set(BERSERK, active);
  }

  public long now() {
    return level().getGameTime();
  }

  public boolean operational() {
    return ExpansionRuntime.operational(this);
  }

  public void configure(int rank) {
    configure(rank, getUUID().getMostSignificantBits() ^ getUUID().getLeastSignificantBits());
  }

  public void configure(int rank, long seed) {
    this.seed = seed;
    var plan = CyberpsychoLoadouts.forSeed(rank, seed);
    setData(Cybernetics2077.CYBERWARE, plan.inventory());
    setData(Cybernetics2077.AUGMENT_STATE, new AugmentState());
    entityData.set(RANK, plan.rank());
    entityData.set(ROLE, plan.role().ordinal());
    setItemSlot(
        EquipmentSlot.MAINHAND,
        new ItemStack(
            plan.role() == CyberpsychoLoadouts.Role.NETRUNNER
                ? Items.BOW
                : rank >= 5 ? Items.NETHERITE_SWORD : Items.IRON_SWORD));
    setDropChance(EquipmentSlot.MAINHAND, 0);
    MobImplants.initialize(this);
    refresh(true);
  }

  @Override
  public void onAddedToLevel() {
    super.onAddedToLevel();
    if (level().isClientSide) return;
    if (capacity() <= 150) configure(rank(), seed);
    else {
      entityData.set(RANK, CyberpsychoRules.rank(capacity()));
      refresh(false);
      MobImplants.initialize(this);
    }
  }

  public void refresh(boolean fill) {
    BountyCombat.apply(this, rank(), true, false);
    MobImplants.attributes(this);
    if (fill) setHealth(getMaxHealth());
    else if (getHealth() > getMaxHealth()) setHealth(getMaxHealth());
  }

  public boolean usePump() {
    return MobImplants.usePump(this);
  }

  public boolean blockHack() {
    return AugmentExpansion.blockHack(null, this);
  }

  @Override
  public boolean doHurtTarget(Entity target) {
    swing(net.minecraft.world.InteractionHand.MAIN_HAND);
    boolean hit = super.doHurtTarget(target);
    if (hit && target instanceof LivingEntity living) MobImplants.discharge(this, living);
    return hit;
  }

  @Override
  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putInt("PsychoRank", rank());
    t.putInt("PsychoRole", role().ordinal());
    t.putLong("PsychoSeed", seed);
  }

  @Override
  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    entityData.set(RANK, Math.clamp(t.getInt("PsychoRank"), 1, 6));
    entityData.set(ROLE, Math.clamp(t.getInt("PsychoRole"), 0, 2));
    seed = t.getLong("PsychoSeed");
    if (t.contains("PsychoSystems")) systems().merge(t.getCompound("PsychoSystems"));
  }
}
