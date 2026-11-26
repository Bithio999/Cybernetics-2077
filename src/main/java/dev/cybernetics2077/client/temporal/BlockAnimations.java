package dev.cybernetics2077.client.temporal;

import dev.cybernetics2077.temporal.*;
import java.nio.FloatBuffer;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

/** Bounded sparse visual clocks and immutable frame storage; no chunk rebuilds or block scans. */
public final class BlockAnimations {
  static AnimatedAtlas atlas;
  static int cellsTexture;
  static long serial, ticks;
  static float partial;
  static Vec3 camera = Vec3.ZERO;
  static final float[] rectangles = new float[256], animations = new float[256];
  public static final AnimationLattice LATTICE = new AnimationLattice();
  public static long shaderUses, uploads;
  private static FloatBuffer cells;
  private static ClientLevel level;
  private static long updated = Long.MIN_VALUE;
  private static int depth;
  private static boolean dirty = true;

  public static void uploaded(TextureAtlas value) {
    if (value.location().equals(TextureAtlas.LOCATION_BLOCKS)) {
      dirty = true;
      ticks = 0;
    }
  }

  public static void tick(TextureAtlas value) {
    if (value.location().equals(TextureAtlas.LOCATION_BLOCKS)) ticks++;
  }

  public static void begin(Vec3 pos, float delta) {
    depth++;
    camera = pos;
    partial = delta;
    serial++;
    var mc = Minecraft.getInstance();
    if (level != mc.level || dirty) {
      release();
      level = mc.level;
      dirty = false;
    }
    if (level == null) return;
    var origins = Sandevistan.fieldOrigins(level);
    if (origins.isEmpty() && LATTICE.clocks.isEmpty()) return;
    if (atlas == null)
      atlas = new AnimatedAtlas(mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS));
    if (atlas.sprites.isEmpty()) return;
    if (updated != ticks) {
      // Updating only nodes near at most six visible fields bounds the work even on large servers.
      var visible =
          origins.stream()
              .filter(p -> p.distanceToSqr(camera) < 192 * 192)
              .sorted(Comparator.comparingDouble(p -> p.distanceToSqr(camera)))
              .limit(6)
              .toList();
      var visited = new HashSet<AnimationLattice.Node>();
      for (var origin : visible) {
        int cx = (int) Math.floor(origin.x / 4),
            cy = (int) Math.floor(origin.y / 4),
            cz = (int) Math.floor(origin.z / 4);
        for (int x = cx - 9; x <= cx + 9; x++)
          for (int y = cy - 9; y <= cy + 9; y++)
            for (int z = cz - 9; z <= cz + 9; z++) {
              var node = new AnimationLattice.Node(x, y, z);
              if (!visited.add(node)) continue;
              double pace = Sandevistan.environmentPace(level, x * 4., y * 4., z * 4., true);
              LATTICE.touch(x, y, z, ticks, pace);
            }
      }
      LATTICE.finish(ticks);
      uploadCells();
      updated = ticks;
    }
    for (int i = 0; i < atlas.sprites.size(); i++) {
      var s = atlas.sprites.get(i);
      var sprite = s.sprite();
      rectangles[i * 4] = sprite.getU0();
      rectangles[i * 4 + 1] = sprite.getV0();
      rectangles[i * 4 + 2] = sprite.getU1();
      rectangles[i * 4 + 3] = sprite.getV1();
      animations[i * 4] = s.first();
      animations[i * 4 + 1] = s.cycle();
      animations[i * 4 + 2] = (float) (ticks % s.cycle() + partial);
      animations[i * 4 + 3] = s.blend() ? 1 : 0;
    }
  }

  private static void uploadCells() {
    if (cells == null) cells = MemoryUtil.memCallocFloat(AnimationLattice.SLOTS * 8);
    MemoryUtil.memSet(MemoryUtil.memAddress(cells), 0, (long) cells.capacity() * 4);
    for (var entry : LATTICE.clocks.entrySet()) {
      var n = entry.getKey();
      var c = entry.getValue();
      int slot = AnimationLattice.hash(n.x(), n.y(), n.z());
      for (int probe = 0;
          probe < AnimationLattice.PROBES;
          probe++, slot = (slot + 1) & (AnimationLattice.SLOTS - 1)) {
        int at = slot * 8;
        if (cells.get(at + 3) != 0) continue;
        cells.put(at, n.x());
        cells.put(at + 1, n.y());
        cells.put(at + 2, n.z());
        cells.put(at + 3, 1);
        cells.put(at + 4, (float) c.lag);
        cells.put(at + 5, (float) c.pace);
        cells.put(at + 6, c.weight(ticks));
        break;
      }
    }
    var unpack = new TextureUnpack();
    int old = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
    if (cellsTexture == 0) {
      cellsTexture = GL11.glGenTextures();
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, cellsTexture);
      GL11.glTexImage2D(
          GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, 1024, 256, 0, GL11.GL_RGBA, GL11.GL_FLOAT, cells);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    } else {
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, cellsTexture);
      GL11.glTexSubImage2D(
          GL11.GL_TEXTURE_2D, 0, 0, 0, 1024, 256, GL11.GL_RGBA, GL11.GL_FLOAT, cells);
    }
    GL11.glBindTexture(GL11.GL_TEXTURE_2D, old);
    unpack.close();
    uploads++;
  }

  public static void end() {
    depth = Math.max(0, depth - 1);
  }

  public static boolean rendering() {
    return depth > 0
        && atlas != null
        && !atlas.sprites.isEmpty()
        && !LATTICE.clocks.isEmpty()
        && cellsTexture != 0;
  }

  public static java.util.List<String> spriteNames() {
    return atlas == null
        ? java.util.List.of()
        : atlas.sprites.stream().map(s -> s.sprite().contents().name().toString()).toList();
  }

  public static int spriteCount() {
    return atlas == null ? 0 : atlas.sprites.size();
  }

  public static long textureBytes() {
    return atlas == null
        ? 0
        : (long) atlas.width * atlas.height * atlas.layers * 4 * 4 / 3
            + 16384 * 16
            + (cells == null ? 0 : (long) cells.capacity() * 4);
  }

  public static void release() {
    level = null;
    if (atlas != null) atlas.close();
    atlas = null;
    if (cellsTexture != 0) GL11.glDeleteTextures(cellsTexture);
    cellsTexture = 0;
    if (cells != null) MemoryUtil.memFree(cells);
    cells = null;
    LATTICE.clocks.clear();
    updated = Long.MIN_VALUE;
  }

  private BlockAnimations() {}
}
