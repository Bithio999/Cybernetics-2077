package dev.cybernetics2077.city;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Persistent service NPC. Stays at their workstation and only looks at nearby customers. */
public final class CityResident extends PathfinderMob {
  private static final EntityDataAccessor<Integer> STYLE =
      SynchedEntityData.defineId(CityResident.class, EntityDataSerializers.INT);

  public CityResident(EntityType<? extends PathfinderMob> type, Level level) {
    super(type, level);
    setPersistenceRequired();
  }

  public static AttributeSupplier.Builder attributes() {
    return Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 40)
        .add(Attributes.MOVEMENT_SPEED, .25)
        .add(Attributes.FOLLOW_RANGE, 8);
  }

  @Override
  protected void registerGoals() {
    goalSelector.addGoal(0, new FloatGoal(this));
    goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 6));
    goalSelector.addGoal(3, new RandomLookAroundGoal(this));
  }

  public boolean fixer() {
    return getType() == CityContent.FIXER.get();
  }

  public boolean slim() {
    return fixer() ? style() == 0 : style() == 1;
  }

  public int style() {
    return entityData.get(STYLE);
  }

  @Override
  protected void defineSynchedData(SynchedEntityData.Builder b) {
    super.defineSynchedData(b);
    b.define(STYLE, 0);
  }

  @Override
  public void addAdditionalSaveData(CompoundTag t) {
    super.addAdditionalSaveData(t);
    t.putInt("CityStyle", style());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag t) {
    super.readAdditionalSaveData(t);
    entityData.set(STYLE, Math.clamp(t.getInt("CityStyle"), 0, 1));
  }

  @Override
  public Component getName() {
    return hasCustomName()
        ? getCustomName()
        : Component.translatable(
            "cybernetics2077.city."
                + (fixer()
                    ? (style() == 0 ? "fixer_mara" : "fixer_ren")
                    : (style() == 1 ? "doctor_iris" : "doctor")));
  }

  @Override
  protected InteractionResult mobInteract(Player p, InteractionHand hand) {
    if (hand != InteractionHand.MAIN_HAND || p.isSpectator() || !isAlive())
      return InteractionResult.PASS;
    if (p instanceof ServerPlayer server && p.containerMenu.getCarried().isEmpty()) {
      server.openMenu(
          new SimpleMenuProvider(
              (id, inv, owner) ->
                  fixer() ? new FixerMenu(id, inv, getId()) : new RipperdocMenu(id, inv, getId()),
              getName()),
          b -> b.writeVarInt(getId()));
    }
    return InteractionResult.sidedSuccess(level().isClientSide);
  }

  @Override
  public boolean isPushable() {
    return false;
  }

  @Override
  protected void doPush(Entity e) {}

  @Override
  public boolean removeWhenFarAway(double d) {
    return false;
  }
}
