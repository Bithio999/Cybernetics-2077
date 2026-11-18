package dev.cybernetics2077.city;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Server-owned wallet. Copies on death; no starter money or unfinished contract rewards. */
public final class StreetAccount implements INBTSerializable<CompoundTag> {
  private long eddies;
  private int streetCred;

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
    return t;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider p, CompoundTag t) {
    eddies = Math.clamp(t.getLong("eddies"), 0, 1_000_000_000L);
    streetCred = Math.clamp(t.getInt("streetCred"), 0, 50);
  }
}
