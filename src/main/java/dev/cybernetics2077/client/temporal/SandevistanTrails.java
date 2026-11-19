package dev.cybernetics2077.client.temporal;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.client.mantis.*;
import dev.cybernetics2077.temporal.Sandevistan;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Bounded pose history. No duplicate entities, recursive rendering or unbounded ghost buffers. */
public final class SandevistanTrails {
  public static boolean capturing;

  private record Echo(Vec3 position, double time, List<TemporalMesh> mesh) {}

  private static final Map<UUID, ArrayDeque<Echo>> HISTORY = new LinkedHashMap<>();
  private static final Map<UUID, Float> LAST_SWING = new HashMap<>();
  private static final Map<UUID, Float> LAST_YAW = new HashMap<>();
  private static final ArrayDeque<Echo> LEFT = new ArrayDeque<>(), RIGHT = new ArrayDeque<>();
  private static Object level;
  public static int lastActors, lastEchoes, lastVertices;

  private static int color(double fraction, boolean hands) {
    int[] colors = {0xf5f258, 0x7dff70, 0x29edb4, 0x26cfee, 0x5a83ff, 0xb26aff};
    double step = Math.clamp(fraction, 0, .9999) * (colors.length - 1);
    int i = (int) step;
    float t = (float) (step - i);
    int a = colors[i], b = colors[Math.min(i + 1, colors.length - 1)];
    int rgb = 0;
    for (int shift : new int[] {0, 8, 16})
      rgb |= Math.round(((a >> shift) & 255) * (1 - t) + ((b >> shift) & 255) * t) << shift;
    int alpha = (int) ((hands ? 85 : 150) * Math.pow(1 - fraction, 1.15));
    return (alpha << 24) | rgb;
  }

  private static List<TemporalMesh> upload(List<PoseMesh.Batch> source) {
    return source.stream().map(TemporalMesh::new).toList();
  }

  private static void discard(Echo echo) {
    echo.mesh.forEach(TemporalMesh::close);
  }

  private static void trim(ArrayDeque<Echo> history, int count) {
    while (history.size() > count) discard(history.removeFirst());
  }

  private static void emit(
      PoseStack pose, MultiBufferSource source, List<TemporalMesh> mesh, int color) {
    for (var batch : mesh) {
      batch.draw(pose, color);
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
      if (motion && (last == null || now - last.time >= 2) && captured < 3) {
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
                  now,
                  batches));
          captured++;
        }
        LAST_SWING.put(p.getUUID(), swing);
        LAST_YAW.put(p.getUUID(), yaw);
        trim(history, 8);
      }
    }
    var pose = e.getPoseStack();
    var source = m.renderBuffers().bufferSource();
    source.endBatch();
    var camera = e.getCamera().getPosition();
    lastActors = lastEchoes = lastVertices = 0;
    var iterator = HISTORY.entrySet().iterator();
    while (iterator.hasNext()) {
      var entry = iterator.next();
      var history = entry.getValue();
      while (!history.isEmpty() && now - history.peekFirst().time >= 16)
        discard(history.removeFirst());
      if (history.isEmpty()) {
        iterator.remove();
        LAST_SWING.remove(entry.getKey());
        LAST_YAW.remove(entry.getKey());
        continue;
      }
      var owner = m.level.getPlayerByUUID(entry.getKey());
      if (owner != null
          && Sandevistan.active(owner)
          && (!entry.getKey().equals(m.player.getUUID())
              || !m.options.getCameraType().isFirstPerson())) {
        double activationAge = now - CyberwareClient.state(owner.getId()).getLong("sandyClock");
        if (activationAge >= 0 && activationAge < 8) {
          var current = history.peekLast();
          pose.pushPose();
          var d =
              owner
                  .getPosition(e.getPartialTick().getGameTimeDeltaPartialTick(false))
                  .subtract(camera);
          pose.translate(d.x, d.y, d.z);
          int tint = ((int) (180 * (1 - activationAge / 8)) << 24) | 0xaaff70;
          emit(pose, source, current.mesh, tint);
          pose.popPose();
        }
      }
      lastActors++;
      int ordinal = 0;
      for (var echo : history) {
        double age = now - echo.time;
        if (age < 1.5) continue;
        if (echo.position.distanceToSqr(camera) > 32 * 32 && (++ordinal % 2 == 0)) continue;
        if (entry.getKey().equals(m.player.getUUID())
            && m.options.getCameraType().isFirstPerson()
            && echo.position.distanceToSqr(camera) < .9) continue;
        pose.pushPose();
        var delta = echo.position.subtract(camera);
        pose.translate(delta.x, delta.y, delta.z);
        emit(pose, source, echo.mesh, color(age / 16, false));
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
    if (active && moving && (last == null || now - last.time >= 2)) {
      var recorder = new PoseMesh();
      capturing = true;
      try {
        MantisMesh.render(pose, recorder, light, player, left, motion, true);
      } finally {
        capturing = false;
      }
      history.addLast(new Echo(Vec3.ZERO, now, upload(recorder.finish())));
      trim(history, 3);
    }
    while (!history.isEmpty() && now - history.peekFirst().time > 7) discard(history.removeFirst());
    var identity = new PoseStack();
    var types = new HashSet<RenderType>();
    for (var echo : history)
      if (now - echo.time >= 1.5) {
        emit(identity, source, echo.mesh, color((now - echo.time) / 7, true));
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
    if (active && moving && (last == null || now - last.time >= 2)) {
      var recorder = new PoseMesh();
      capturing = true;
      try {
        render.accept(recorder);
      } finally {
        capturing = false;
      }
      history.addLast(new Echo(Vec3.ZERO, now, upload(recorder.finish())));
      trim(history, 3);
    }
    while (!history.isEmpty() && now - history.peekFirst().time > 7) discard(history.removeFirst());
    var types = new HashSet<RenderType>();
    for (var echo : history)
      if (now - echo.time >= 1.5) {
        emit(new PoseStack(), source, echo.mesh, color((now - echo.time) / 7, true));
        for (var b : echo.mesh) types.add(b.type);
      }
    if (source instanceof MultiBufferSource.BufferSource buffers) types.forEach(buffers::endBatch);
  }

  public static void clear() {
    HISTORY.values().forEach(h -> h.forEach(SandevistanTrails::discard));
    LEFT.forEach(SandevistanTrails::discard);
    RIGHT.forEach(SandevistanTrails::discard);
    HISTORY.clear();
    LAST_SWING.clear();
    LAST_YAW.clear();
    LEFT.clear();
    RIGHT.clear();
  }

  private SandevistanTrails() {}
}
