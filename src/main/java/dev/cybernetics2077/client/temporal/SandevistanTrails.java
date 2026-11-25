package dev.cybernetics2077.client.temporal;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.client.mantis.*;
import dev.cybernetics2077.temporal.Sandevistan;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Bounded pose history. No duplicate entities, recursive rendering or unbounded ghost buffers. */
public final class SandevistanTrails {
  public static boolean capturing;

  private record Echo(Vec3 position, TrailStyle.PoseTint tint, List<TemporalMesh> mesh) {}

  private static final Map<UUID, ArrayDeque<Echo>> HISTORY = new LinkedHashMap<>();
  private static final Map<UUID, Double> COLOR_ORIGINS = new HashMap<>();
  private static final Map<UUID, Float> LAST_SWING = new HashMap<>();
  private static final Map<UUID, Float> LAST_YAW = new HashMap<>();
  private static final ArrayDeque<Echo> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
  private static Object level;
  public static int lastActors, lastEchoes, lastVertices, lastBodyExclusions, lastSelfSuppressed;

  private static List<TemporalMesh> upload(List<PoseMesh.Batch> source) {
    return source.stream().map(TemporalMesh::new).toList();
  }

  private static void discard(Echo echo) {
    echo.mesh.forEach(TemporalMesh::close);
  }

  private static void trim(ArrayDeque<Echo> history, int count) {
    while (history.size() > count) discard(history.removeFirst());
  }

  private static TrailStyle.PoseTint captureTint(AbstractClientPlayer player, double now) {
    double origin = COLOR_ORIGINS.computeIfAbsent(player.getUUID(), id -> now);
    return TrailStyle.capture(now, now - origin);
  }

  private static void emit(PoseStack pose, List<TemporalMesh> mesh, int color, AABB exclude) {
    for (var batch : mesh) {
      batch.draw(pose, color, exclude);
      lastVertices += batch.count;
    }
  }

  public static void render(RenderLevelStageEvent e) {
    var m = Minecraft.getInstance();
    if (m.level == null || m.player == null) return;
    if (level != m.level) {
      clear();
      level = m.level;
    }
    double now = m.level.getGameTime() + e.getPartialTick().getGameTimeDeltaPartialTick(false);
    COLOR_ORIGINS
        .keySet()
        .removeIf(
            id -> {
              var player = m.level.getPlayerByUUID(id);
              return player == null || !Sandevistan.active(player);
            });
    var candidates =
        m.level.players().stream()
            .filter(
                p ->
                    Sandevistan.active(p)
                        && !p.isInvisible()
                        && p.distanceToSqr(m.player) < 64 * 64)
            .sorted(Comparator.comparingDouble(p -> p.distanceToSqr(m.player)))
            .limit(6)
            .toList();
    int captured = 0;
    for (var p : candidates) {
      COLOR_ORIGINS.putIfAbsent(p.getUUID(), now);
      var history = HISTORY.computeIfAbsent(p.getUUID(), id -> new ArrayDeque<>());
      var last = history.peekLast();
      float swing = p.getAttackAnim(0), yaw = p.yBodyRot;
      boolean motion =
          last == null
              || last.position.distanceToSqr(p.position()) > .0025
              || Math.abs(swing - LAST_SWING.getOrDefault(p.getUUID(), swing)) > .04
              || Math.abs(
                      net.minecraft.util.Mth.wrapDegrees(
                          yaw - LAST_YAW.getOrDefault(p.getUUID(), yaw)))
                  > 8;
      if (motion
          && (last == null || last.tint.age(now) >= TrailStyle.WORLD_INTERVAL)
          && captured < 3) {
        var mesh = new PoseMesh();
        capturing = true;
        try {
          var renderer = m.getEntityRenderDispatcher().getRenderer(p);
          renderer.render(
              p,
              p.getYRot(),
              e.getPartialTick().getGameTimeDeltaPartialTick(false),
              new PoseStack(),
              mesh,
              15728640);
        } finally {
          capturing = false;
        }
        var batches = upload(mesh.finish());
        if (!batches.isEmpty()) {
          history.addLast(
              new Echo(
                  p.getPosition(e.getPartialTick().getGameTimeDeltaPartialTick(false)),
                  captureTint(p, now),
                  batches));
          captured++;
        }
        LAST_SWING.put(p.getUUID(), swing);
        LAST_YAW.put(p.getUUID(), yaw);
        trim(history, TrailStyle.WORLD_HISTORY);
      }
    }
    var pose = e.getPoseStack();
    var source = m.renderBuffers().bufferSource();
    source.endBatch();
    var camera = e.getCamera().getPosition();
    lastActors = lastEchoes = lastVertices = lastBodyExclusions = lastSelfSuppressed = 0;
    var iterator = HISTORY.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      var history = entry.getValue();
      while (!history.isEmpty() && history.peekFirst().tint.age(now) >= TrailStyle.WORLD_LIFETIME)
        discard(history.removeFirst());
      if (history.isEmpty()) {
        iterator.remove();
        LAST_SWING.remove(entry.getKey());
        LAST_YAW.remove(entry.getKey());
        continue;
      }
      var owner = m.level.getPlayerByUUID(entry.getKey());
      // The body renderer is for observers/third person. First person has its own arm echoes.
      if (!TrailStyle.bodyVisible(
          entry.getKey().equals(m.player.getUUID()), m.options.getCameraType().isFirstPerson())) {
        lastSelfSuppressed++;
        continue;
      }
      if (lastActors >= 6 || lastEchoes >= TrailStyle.WORLD_DRAW_LIMIT) continue;
      lastActors++;
      int budget =
          Math.min(
              TrailStyle.WORLD_HISTORY,
              TrailStyle.WORLD_DRAW_LIMIT / Math.max(1, Math.min(6, HISTORY.size())));
      if (history.peekLast().position.distanceToSqr(camera) > 32 * 32) budget = Math.min(budget, 6);
      int index = 0;
      for (var echo : history) {
        int ordinal = index++;
        double age = echo.tint.age(now);
        if (age < 1.5 || !TrailStyle.selected(ordinal, history.size(), budget)) continue;
        if (lastEchoes >= TrailStyle.WORLD_DRAW_LIMIT) break;
        pose.pushPose();
        var delta = echo.position.subtract(camera);
        pose.translate(delta.x, delta.y, delta.z);
        // The live body's volume is expressed in the saved mesh's world-aligned local space.
        var exclude =
            owner == null
                ? null
                : owner
                    .getBoundingBox()
                    .move(
                        owner
                            .getPosition(e.getPartialTick().getGameTimeDeltaPartialTick(false))
                            .subtract(owner.position()))
                    .inflate(.09)
                    .move(echo.position.scale(-1));
        emit(pose, echo.mesh, echo.tint.color(now, false), exclude);
        if (exclude != null) lastBodyExclusions++;
        pose.popPose();
        lastEchoes++;
      }
    }
    source.endBatch();
  }

  /**
   * Capture a low-LOD copy of the exact articulated first-person pose, then show its recent echoes.
   */
  public static void hand(
      PoseStack pose,
      MultiBufferSource source,
      AbstractClientPlayer player,
      boolean left,
      MantisMotion.Pose motion,
      int light) {
    if (capturing) return;
    var history = left ? LEFT : RIGHT;
    double now =
        player.level().getGameTime()
            + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    boolean active = Sandevistan.active(player);
    var last = history.peekLast();
    boolean moving = player.getDeltaMovement().lengthSqr() > .0005 || motion.trail() > 0;
    if (active && moving && (last == null || last.tint.age(now) >= TrailStyle.HAND_INTERVAL)) {
      var recorder = new PoseMesh();
      capturing = true;
      try {
        MantisMesh.render(pose, recorder, light, player, left, motion, true);
      } finally {
        capturing = false;
      }
      history.addLast(new Echo(Vec3.ZERO, captureTint(player, now), upload(recorder.finish())));
      trim(history, TrailStyle.HAND_HISTORY);
    }
    while (!history.isEmpty() && history.peekFirst().tint.age(now) > TrailStyle.HAND_LIFETIME)
      discard(history.removeFirst());
    var identity = new PoseStack();
    var types = new HashSet<RenderType>();
    for (var echo : history)
      if (echo.tint.age(now) >= 1.5) {
        emit(identity, echo.mesh, echo.tint.color(now, true), null);
        for (var b : echo.mesh) types.add(b.type);
      }
    if (source instanceof MultiBufferSource.BufferSource buffers) types.forEach(buffers::endBatch);
  }

  public static void vanillaHand(
      AbstractClientPlayer player,
      boolean left,
      MultiBufferSource source,
      java.util.function.Consumer<PoseMesh> render) {
    if (capturing) return;
    var history = left ? LEFT : RIGHT;
    double now =
        player.level().getGameTime()
            + Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    boolean active = Sandevistan.active(player);
    var last = history.peekLast();
    boolean moving = player.getDeltaMovement().lengthSqr() > .0005 || player.getAttackAnim(0) > .01;
    if (active && moving && (last == null || last.tint.age(now) >= TrailStyle.HAND_INTERVAL)) {
      var recorder = new PoseMesh();
      capturing = true;
      try {
        render.accept(recorder);
      } finally {
        capturing = false;
      }
      history.addLast(new Echo(Vec3.ZERO, captureTint(player, now), upload(recorder.finish())));
      trim(history, TrailStyle.HAND_HISTORY);
    }
    while (!history.isEmpty() && history.peekFirst().tint.age(now) > TrailStyle.HAND_LIFETIME)
      discard(history.removeFirst());
    var types = new HashSet<RenderType>();
    for (var echo : history)
      if (echo.tint.age(now) >= 1.5) {
        emit(new PoseStack(), echo.mesh, echo.tint.color(now, true), null);
        for (var b : echo.mesh) types.add(b.type);
      }
    if (source instanceof MultiBufferSource.BufferSource buffers) types.forEach(buffers::endBatch);
  }

  public static void clear() {
    HISTORY.values().forEach(h -> h.forEach(SandevistanTrails::discard));
    LEFT.forEach(SandevistanTrails::discard);
    RIGHT.forEach(SandevistanTrails::discard);
    HISTORY.clear();
    COLOR_ORIGINS.clear();
    LAST_SWING.clear();
    LAST_YAW.clear();
    LEFT.clear();
    RIGHT.clear();
  }

  private SandevistanTrails() {}
}
