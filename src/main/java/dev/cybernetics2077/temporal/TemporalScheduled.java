package dev.cybernetics2077.temporal;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.ticks.*;

/** Vanilla owns every queued tick. Only its remaining local delay is persisted here. */
public final class TemporalScheduled extends SavedData {
  public record Key(boolean fluid, long pos, String type) {}

  private record ChunkKey(boolean fluid, long chunk) {}

  private final Map<Key, LocalTime.Timer> timers = new HashMap<>();
  private final Map<ChunkKey, Set<Key>> chunks = new HashMap<>();

  private static ChunkKey chunk(Key key) {
    return new ChunkKey(key.fluid, ChunkPos.asLong(BlockPos.of(key.pos)));
  }

  private void put(Key key, LocalTime.Timer timer) {
    timers.put(key, timer);
    chunks.computeIfAbsent(chunk(key), k -> new HashSet<>()).add(key);
  }

  private void remove(Key key) {
    timers.remove(key);
    var keys = chunks.get(chunk(key));
    if (keys != null) {
      keys.remove(key);
      if (keys.isEmpty()) chunks.remove(chunk(key));
    }
  }

  public static void loaded(ServerLevel level, boolean fluid, ChunkPos pos) {
    var data = get(level);
    var keys = data.chunks.get(new ChunkKey(fluid, pos.toLong()));
    if (keys != null) {
      for (var key : keys) data.timers.get(key).last = level.getGameTime();
      data.setDirty();
    }
  }

  public static void unloaded(ServerLevel level, boolean fluid, ChunkPos pos) {
    var data = get(level);
    var keys = data.chunks.get(new ChunkKey(fluid, pos.toLong()));
    if (keys != null) {
      for (var key : keys)
        data.timers
            .get(key)
            .advance(level.getGameTime(), TemporalEnvironment.pace(level, BlockPos.of(key.pos)));
      data.setDirty();
    }
  }

  public static TemporalScheduled get(ServerLevel level) {
    return level
        .getDataStorage()
        .computeIfAbsent(
            new SavedData.Factory<>(TemporalScheduled::new, TemporalScheduled::load),
            "cybernetics2077_local_ticks");
  }

  private static TemporalScheduled load(CompoundTag tag, HolderLookup.Provider provider) {
    var data = new TemporalScheduled();
    for (var raw : tag.getList("timers", Tag.TAG_COMPOUND)) {
      var t = (CompoundTag) raw;
      double left = t.getDouble("left");
      if (Double.isFinite(left))
        data.put(
            new Key(t.getBoolean("fluid"), t.getLong("pos"), t.getString("type")),
            new LocalTime.Timer(left, t.getLong("at")));
    }
    return data;
  }

  @Override
  public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
    var list = new ListTag();
    timers.forEach(
        (key, value) -> {
          var t = new CompoundTag();
          t.putBoolean("fluid", key.fluid);
          t.putLong("pos", key.pos);
          t.putString("type", key.type);
          t.putDouble("left", value.remaining);
          t.putLong("at", value.last);
          list.add(t);
        });
    tag.put("timers", list);
    return tag;
  }

  private static Key key(boolean fluid, BlockPos pos, Object type) {
    String id =
        fluid
            ? BuiltInRegistries.FLUID
                .getKey((net.minecraft.world.level.material.Fluid) type)
                .toString()
            : BuiltInRegistries.BLOCK
                .getKey((net.minecraft.world.level.block.Block) type)
                .toString();
    return new Key(fluid, pos.asLong(), id);
  }

  public static void queued(
      ServerLevel level, boolean fluid, ScheduledTick<?> tick, boolean scanning) {
    double pace = TemporalEnvironment.pace(level, tick.pos());
    if (pace >= 1) return;
    var data = get(level);
    var key = key(fluid, tick.pos(), tick.type());
    if (data.timers.containsKey(key)) return;
    long now = level.getGameTime();
    double left = Math.max(0, tick.triggerTick() - now);
    if (scanning && tick.triggerTick() >= now) left += 1 - pace;
    data.put(key, new LocalTime.Timer(left, now));
    data.setDirty();
  }

  public static <T> void prepare(
      ServerLevel level, boolean fluid, Long2ObjectMap<LevelChunkTicks<T>> containers) {
    var data = get(level);
    long now = level.getGameTime();
    boolean changed = false;
    // Loaded native containers are the bound, not all saved timers across explored terrain.
    // Container lifecycle hooks freeze/resume metadata along with vanilla's saved delays.
    if (!data.timers.isEmpty())
      for (var container : containers.long2ObjectEntrySet()) {
        var keys = data.chunks.get(new ChunkKey(fluid, container.getLongKey()));
        if (keys == null) continue;
        for (var key : keys) {
          var timer = data.timers.get(key);
          if (timer.last != now) {
            timer.advance(now, TemporalEnvironment.pace(level, BlockPos.of(key.pos)));
            changed = true;
          }
        }
      }

    if (changed) data.setDirty();
    var visited = new HashSet<Long>();
    for (var origin : Sandevistan.fieldOrigins(level)) {
      int x = (int) Math.floor(origin.x) >> 4, z = (int) Math.floor(origin.z) >> 4;
      for (int dx = -2; dx <= 2; dx++)
        for (int dz = -2; dz <= 2; dz++) {
          long chunk = ChunkPos.asLong(x + dx, z + dz);
          if (!visited.add(chunk)) continue;
          var container = containers.get(chunk);
          if (container != null)
            container.getAll().forEach(tick -> queued(level, fluid, tick, true));
        }
    }
  }

  public static boolean ready(ServerLevel level, boolean fluid, ScheduledTick<?> tick) {
    var data = get(level);
    var key = key(fluid, tick.pos(), tick.type());
    var timer = data.timers.get(key);
    if (timer == null) return true;
    timer.advance(level.getGameTime(), TemporalEnvironment.pace(level, tick.pos()));
    data.setDirty();
    if (timer.remaining > 1e-7) return false;
    data.remove(key);
    return true;
  }

  public static long retryDelay(ServerLevel level, boolean fluid, ScheduledTick<?> tick) {
    var timer = get(level).timers.get(key(fluid, tick.pos(), tick.type()));
    return timer == null ? 1 : Math.max(1, (long) Math.ceil(timer.remaining));
  }

  public static void clear(ServerLevel level, boolean fluid, BoundingBox area) {
    var data = get(level);
    var removed =
        data.timers.keySet().stream()
            .filter(k -> k.fluid == fluid && area.isInside(BlockPos.of(k.pos)))
            .toList();
    removed.forEach(data::remove);
    if (!removed.isEmpty()) data.setDirty();
  }

  public Map<Key, Double> copies(
      ServerLevel source,
      boolean fluid,
      BoundingBox area,
      Vec3i offset,
      java.util.function.Predicate<Key> alreadyQueued) {
    var copies = new HashMap<Key, Double>();
    timers.forEach(
        (k, t) -> {
          if (k.fluid != fluid
              || !area.isInside(BlockPos.of(k.pos))
              || !source.hasChunkAt(BlockPos.of(k.pos))) return;
          t.advance(source.getGameTime(), TemporalEnvironment.pace(source, BlockPos.of(k.pos)));
          var dest = new Key(fluid, BlockPos.of(k.pos).offset(offset).asLong(), k.type);
          if (!alreadyQueued.test(dest)) copies.put(dest, Math.max(0, t.remaining));
        });
    if (!copies.isEmpty()) setDirty();
    return copies;
  }

  public void applyCopies(
      ServerLevel level, Map<Key, Double> copies, java.util.function.Predicate<Key> queued) {
    copies.forEach(
        (k, t) -> {
          if (queued.test(k)) put(k, new LocalTime.Timer(t, level.getGameTime()));
        });
    if (!copies.isEmpty()) setDirty();
  }

  public static Object type(Key key) {
    var id = ResourceLocation.parse(key.type);
    return key.fluid ? BuiltInRegistries.FLUID.get(id) : BuiltInRegistries.BLOCK.get(id);
  }
}
