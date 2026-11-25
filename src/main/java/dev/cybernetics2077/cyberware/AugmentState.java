package dev.cybernetics2077.cyberware;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Absolute server-clock timers persist across logout/death, so reinstallation cannot reset them.
 */
public final class AugmentState implements INBTSerializable<CompoundTag> {
  public long berserkEnd,
      berserkReady,
      camoEnd,
      camoReady,
      pumpNext,
      pumpEnd,
      adrenalineEnd,
      adrenalineReady;
  public long sandyEnd, sandyClock, sandyRearm;
  public double sandyCharge = 1, lastSandyPace = 1;
  public int sandyProfile = 2;
  public boolean wasSandy;
  public long microReady, wrapReady, wrapEnd, smartUntil;
  public UUID smartTarget;
  public long pumpPulse;
  public long iceReady, heartReady, biomonitorReady, shockReady, jenkinsClock;
  public int jenkinsFatigue;
  public int pumpCharges = 2, berserkKills;
  public int pumpPeriod = 800;
  public long lastCombat = -10000;
  public long scanUntil, ankleStart = -1;
  public Augment scanProfile;
  public boolean airJumped;
  public boolean wasCamo, wasBerserk;
  public final Map<Integer, Long> highlights = new HashMap<>();

  @Override
  public CompoundTag serializeNBT(HolderLookup.Provider registries) {
    var t = new CompoundTag();
    t.putLong("microReady", microReady);
    t.putLong("wrapReady", wrapReady);
    t.putLong("wrapEnd", wrapEnd);
    t.putLong("sandyEnd", sandyEnd);
    t.putLong("sandyClock", sandyClock);
    t.putLong("sandyRearm", sandyRearm);
    t.putDouble("sandyCharge", sandyCharge);
    t.putInt("sandyProfile", sandyProfile);
    t.putLong("berserkEnd", berserkEnd);
    t.putLong("berserkReady", berserkReady);
    t.putLong("camoEnd", camoEnd);
    t.putLong("camoReady", camoReady);
    t.putLong("pumpNext", pumpNext);
    t.putLong("pumpPulse", pumpPulse);
    t.putLong("pumpEnd", pumpEnd);
    t.putInt("pumpCharges", pumpCharges);
    t.putInt("pumpPeriod", pumpPeriod);
    t.putLong("lastCombat", lastCombat);
    t.putInt("berserkKills", berserkKills);
    t.putLong("adrenalineEnd", adrenalineEnd);
    t.putLong("adrenalineReady", adrenalineReady);
    t.putLong("iceReady", iceReady);
    t.putLong("heartReady", heartReady);
    t.putLong("biomonitorReady", biomonitorReady);
    t.putLong("shockReady", shockReady);
    t.putLong("jenkinsClock", jenkinsClock);
    t.putInt("jenkinsFatigue", jenkinsFatigue);
    return t;
  }

  @Override
  public void deserializeNBT(HolderLookup.Provider registries, CompoundTag t) {
    microReady = t.getLong("microReady");
    wrapReady = t.getLong("wrapReady");
    wrapEnd = t.getLong("wrapEnd");
    sandyEnd = t.getLong("sandyEnd");
    sandyClock = t.getLong("sandyClock");
    sandyRearm = t.getLong("sandyRearm");
    sandyCharge = t.contains("sandyCharge") ? Math.clamp(t.getDouble("sandyCharge"), 0, 1) : 1;
    sandyProfile = t.contains("sandyProfile") ? Math.clamp(t.getInt("sandyProfile"), 2, 5) : 2;
    iceReady = t.getLong("iceReady");
    heartReady = t.getLong("heartReady");
    biomonitorReady = t.getLong("biomonitorReady");
    shockReady = t.getLong("shockReady");
    jenkinsClock = t.getLong("jenkinsClock");
    jenkinsFatigue = Math.clamp(t.getInt("jenkinsFatigue"), 0, 100);
    berserkEnd = t.getLong("berserkEnd");
    berserkReady = t.getLong("berserkReady");
    camoEnd = t.getLong("camoEnd");
    camoReady = t.getLong("camoReady");
    pumpNext = t.getLong("pumpNext");
    pumpPulse = t.getLong("pumpPulse");
    pumpEnd = t.getLong("pumpEnd");
    pumpCharges = t.contains("pumpCharges") ? Math.clamp(t.getInt("pumpCharges"), 0, 2) : 2;
    pumpPeriod = t.contains("pumpPeriod") ? Math.clamp(t.getInt("pumpPeriod"), 500, 800) : 800;
    lastCombat = t.contains("lastCombat") ? t.getLong("lastCombat") : -10000;
    berserkKills = Math.max(0, t.getInt("berserkKills"));
    adrenalineEnd = t.getLong("adrenalineEnd");
    adrenalineReady = t.getLong("adrenalineReady");
  }
}
