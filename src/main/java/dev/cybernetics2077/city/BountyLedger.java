package dev.cybernetics2077.city;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** One authoritative job per player, including offline players. No chunk tickets. */
public final class BountyLedger extends SavedData {
  private final Map<UUID, CompoundTag> jobs = new HashMap<>();

  public static BountyLedger get(MinecraftServer server) {
    return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(BountyLedger::new, BountyLedger::load),
            "cybernetics2077_bounties");
  }

  public CompoundTag job(UUID player) {
    return jobs.getOrDefault(player, new CompoundTag()).copy();
  }

  public void put(UUID owner, CompoundTag job) {
    jobs.put(owner, job.copy());
    setDirty();
  }

  public void remove(UUID owner) {
    if (jobs.remove(owner) != null) setDirty();
  }

  public static BountyLedger load(CompoundTag tag, HolderLookup.Provider registries) {
    var ledger = new BountyLedger();
    for (var value : tag.getList("jobs", Tag.TAG_COMPOUND)) {
      var job = (CompoundTag) value;
      if (job.hasUUID("owner") && job.hasUUID("token"))
        ledger.jobs.put(job.getUUID("owner"), job.copy());
    }
    return ledger;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
    var list = new ListTag();
    jobs.values().forEach(j -> list.add(j.copy()));
    tag.put("jobs", list);
    return tag;
  }
}
