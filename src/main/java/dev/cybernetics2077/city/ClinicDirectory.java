package dev.cybernetics2077.city;

import dev.cybernetics2077.Cybernetics2077;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Actual doctor addresses, persisted without forcing chunks or searching terrain on a menu click.
 */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class ClinicDirectory extends SavedData {
  private final Map<UUID, CompoundTag> doctors = new HashMap<>();

  public static ClinicDirectory get(MinecraftServer server) {
    return server
        .overworld()
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(ClinicDirectory::new, ClinicDirectory::load),
            "cybernetics2077_clinics");
  }

  private static ClinicDirectory load(CompoundTag tag, HolderLookup.Provider provider) {
    var data = new ClinicDirectory();
    for (var raw : tag.getList("doctors", Tag.TAG_COMPOUND)) {
      var t = (CompoundTag) raw;
      if (t.hasUUID("doctor")) data.doctors.put(t.getUUID("doctor"), t.copy());
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag t, HolderLookup.Provider provider) {
    var entries = new ListTag();
    doctors.values().forEach(v -> entries.add(v.copy()));
    t.put("doctors", entries);
    return t;
  }

  public void record(CityResident doctor) {
    if (doctor.fixer() || !doctor.isAlive()) return;
    var t = new CompoundTag();
    t.putUUID("doctor", doctor.getUUID());
    t.putString("dimension", doctor.level().dimension().location().toString());
    t.putInt("x", doctor.getBlockX());
    t.putInt("y", doctor.getBlockY());
    t.putInt("z", doctor.getBlockZ());
    var previous = doctors.put(doctor.getUUID(), t);
    if (!t.equals(previous)) setDirty();
  }

  public Optional<CompoundTag> nearest(ServerPlayer p) {
    String dimension = p.level().dimension().location().toString();
    return doctors.values().stream()
        .filter(t -> t.getString("dimension").equals(dimension))
        .min(
            Comparator.comparingDouble(
                t -> p.distanceToSqr(t.getInt("x") + .5, t.getInt("y"), t.getInt("z") + .5)))
        .map(CompoundTag::copy);
  }

  @SubscribeEvent
  public static void joined(EntityJoinLevelEvent e) {
    if (!e.getLevel().isClientSide && e.getEntity() instanceof CityResident n && !n.fixer()) {
      // Entity insertion can happen during world generation. SavedData stays on the server thread.
      var server = e.getLevel().getServer();
      server.execute(
          () -> {
            if (!n.isRemoved()) get(server).record(n);
          });
    }
  }

  @SubscribeEvent
  public static void died(LivingDeathEvent e) {
    if (!e.getEntity().level().isClientSide
        && e.getEntity() instanceof CityResident n
        && !n.fixer()) {
      var directory = get(n.getServer());
      directory.doctors.remove(n.getUUID());
      directory.setDirty();
    }
  }
}
