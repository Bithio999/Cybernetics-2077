package dev.cybernetics2077.city;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Server-owned wallet and one-time contract ledger. Both copy on death. */
public final class StreetAccount implements INBTSerializable<CompoundTag> {
  private long eddies;
  private int streetCred;
  private CompoundTag delivery = new CompoundTag();

  public void reputation(int amount) {
    if (amount < 0) throw new IllegalArgumentException("Negative reputation");
    streetCred = (int) Math.min(50L, (long) streetCred + amount);
  }

  /** A snapshot: callers cannot edit the authoritative ledger through a tooltip or payload. */
  public CompoundTag delivery() {
    return delivery.copy();
  }

  public int deliveryStage() {
    return delivery.getInt("stage");
  }

  public void delivery(CompoundTag value) {
    delivery = value.copy();
  }

  public long eddies() {
    return eddies;
  }

  public int streetCred() {
    return streetCred;
  }

  public void credit(long amount) {
    if (amount < 0) throw new IllegalArgumentException("Negative credit");
    eddies = Math.min(1_000_000_000L, eddies + Math.min(amount, 1_000_000_000L));
  }

  public boolean spend(int price) {
    if (price < 0 || eddies < price) return false;
    eddies -= price;
    return true;
  }

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider p) {
    var t = new CompoundTag();
    t.putLong("eddies", eddies);
    t.putInt("streetCred", streetCred);
    t.put("delivery", delivery.copy());
    return t;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider p, CompoundTag t) {
    eddies = Math.clamp(t.getLong("eddies"), 0, 1_000_000_000L);
    streetCred = Math.clamp(t.getInt("streetCred"), 0, 50);
    delivery = t.getCompound("delivery").copy();
  }
}
