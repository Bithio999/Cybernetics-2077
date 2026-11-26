package dev.cybernetics2077.city;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Server-owned wallet and one-time contract ledger. Both copy on death. */
public final class StreetAccount implements INBTSerializable<CompoundTag> {
  private long eddies;
  private int streetCred;
  private CompoundTag delivery = new CompoundTag();
  private CompoundTag bountyProgress = new CompoundTag();

  public boolean bountyUsed(long day, java.util.UUID offer) {
    if (bountyProgress.getLong("day") != day) return false;
    return bountyProgress.getList("used", 8).stream()
        .anyMatch(t -> t.getAsString().equals(offer.toString()));
  }

  public void reserveBounty(long day, java.util.UUID offer) {
    var used =
        bountyProgress.getLong("day") == day
            ? bountyProgress.getList("used", 8)
            : new net.minecraft.nbt.ListTag();
    used.add(net.minecraft.nbt.StringTag.valueOf(offer.toString()));
    bountyProgress.putLong("day", day);
    bountyProgress.put("used", used);
  }

  public int favor(int profile) {
    return bountyProgress.getInt("favor" + profile);
  }

  public int legends() {
    return bountyProgress.getInt("legends");
  }

  public boolean iconicRewardClaimed() {
    return bountyProgress.getBoolean("iconicReward");
  }

  public void completedBounty(int profile, int rank) {
    bountyProgress.putInt("favor" + profile, Math.min(1000000, favor(profile) + 1));
    if (rank == 6) bountyProgress.putInt("legends", Math.min(1000000, legends() + 1));
  }

  public void iconicRewardClaimed(boolean value) {
    bountyProgress.putBoolean("iconicReward", value);
  }

  public CompoundTag bountyReport() {
    return bountyProgress.getCompound("report").copy();
  }

  public void bountyReport(CompoundTag report) {
    bountyProgress.put("report", report.copy());
  }

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
    t.put("bountyProgress", bountyProgress.copy());
    return t;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider p, CompoundTag t) {
    eddies = Math.clamp(t.getLong("eddies"), 0, 1_000_000_000L);
    streetCred = Math.clamp(t.getInt("streetCred"), 0, 50);
    delivery = t.getCompound("delivery").copy();
    bountyProgress = t.getCompound("bountyProgress").copy();
  }
}
