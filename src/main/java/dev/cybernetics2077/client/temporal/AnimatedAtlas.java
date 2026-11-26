package dev.cybernetics2077.client.temporal;

import java.nio.*;
import java.util.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.lwjgl.opengl.*;
import org.lwjgl.system.MemoryUtil;

/**
 * Only unique animated block frames are copied, once per resource reload, with isolated mipmaps.
 */
final class AnimatedAtlas implements AutoCloseable {
  static final int MAX_SPRITES = 64;

  record Frame(int index, int duration) {}

  record Sprite(
      TextureAtlasSprite sprite, List<Frame> frames, int first, int cycle, boolean blend) {}

  final List<Sprite> sprites = new ArrayList<>();
  int frames, timeline, width, height, layers;

  AnimatedAtlas(TextureAtlas atlas) {
    int maxLayers = GL11.glGetInteger(GL30.GL_MAX_ARRAY_TEXTURE_LAYERS);
    var candidates =
        atlas.getTextures().values().stream()
            .filter(
                s ->
                    s.contents().name().getNamespace().equals("minecraft")
                        && s.contents().name().getPath().startsWith("block/")
                        && s.contents().getUniqueFrames().count() > 1)
            .sorted(
                Comparator.comparingInt(
                        (TextureAtlasSprite s) -> {
                          String name = s.contents().name().getPath();
                          return name.startsWith("block/water")
                              ? 0
                              : name.startsWith("block/lava")
                                  ? 1
                                  : name.startsWith("block/fire")
                                          || name.startsWith("block/soul_fire")
                                      ? 2
                                      : name.equals("block/nether_portal") ? 3 : 4;
                        })
                    .thenComparing(s -> s.contents().name().toString()))
            .toList();
    int ticks = 0;
    for (var sprite : candidates) {
      var c = sprite.contents();
      if (c.width() > 128 || c.height() > 128) continue;
      var meta =
          c.metadata()
              .getSection(AnimationMetadataSection.SERIALIZER)
              .orElse(AnimationMetadataSection.EMPTY);
      int count =
          c.getOriginalImage().getWidth()
              / c.width()
              * (c.getOriginalImage().getHeight() / c.height());
      var order = new ArrayList<Frame>();
      meta.forEachFrame(
          (i, duration) -> {
            if (i >= 0 && i < count && duration > 0) order.add(new Frame(i, duration));
          });
      if (order.isEmpty())
        for (int i = 0; i < count; i++) order.add(new Frame(i, meta.getDefaultFrameTime()));
      int cycle = order.stream().mapToInt(Frame::duration).sum();
      int unique = (int) order.stream().map(Frame::index).distinct().count();
      int w = Math.max(width, c.width()), h = Math.max(height, c.height());
      if (cycle <= 0
          || cycle > 16384
          || (long) ticks + cycle > 16384
          || layers + unique > maxLayers
          || (long) w * h * (layers + unique) * 4 > 16 * 1024 * 1024
          || sprites.size() == MAX_SPRITES) continue;
      sprites.add(new Sprite(sprite, order, ticks, cycle, meta.isInterpolatedFrames()));
      ticks += cycle;
      layers += unique;
      width = w;
      height = h;
    }
    if (sprites.isEmpty()) return;
    var unpack = new TextureUnpack();
    frames = GL11.glGenTextures();
    GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, frames);
    GL12.glTexImage3D(
        GL30.GL_TEXTURE_2D_ARRAY,
        0,
        GL11.GL_RGBA8,
        width,
        height,
        layers,
        0,
        GL11.GL_RGBA,
        GL11.GL_UNSIGNED_BYTE,
        (ByteBuffer) null);
    GL11.glTexParameteri(
        GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST_MIPMAP_LINEAR);
    GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
    GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
    GL11.glTexParameteri(GL30.GL_TEXTURE_2D_ARRAY, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
    var pixels = MemoryUtil.memAlloc(width * height * 4);
    var sequence = MemoryUtil.memCallocFloat(16384 * 4);
    int layer = 0;
    try {
      for (var sprite : sprites) {
        var image = sprite.sprite.contents().getOriginalImage();
        int sw = sprite.sprite.contents().width(), sh = sprite.sprite.contents().height();
        int columns = image.getWidth() / sw;
        var unique = new LinkedHashMap<Integer, Integer>();
        for (var frame : sprite.frames)
          if (!unique.containsKey(frame.index)) {
            pixels.clear();
            int ox = frame.index % columns * sw, oy = frame.index / columns * sh;
            for (int y = 0; y < height; y++)
              for (int x = 0; x < width; x++) {
                int rgba = image.getPixelRGBA(ox + x * sw / width, oy + y * sh / height);
                pixels
                    .put((byte) rgba)
                    .put((byte) (rgba >> 8))
                    .put((byte) (rgba >> 16))
                    .put((byte) (rgba >> 24));
              }
            pixels.flip();
            GL12.glTexSubImage3D(
                GL30.GL_TEXTURE_2D_ARRAY,
                0,
                0,
                0,
                layer,
                width,
                height,
                1,
                GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE,
                pixels);
            unique.put(frame.index, layer++);
          }
        int at = sprite.first;
        for (int i = 0; i < sprite.frames.size(); i++) {
          var frame = sprite.frames.get(i);
          int next = sprite.frames.get((i + 1) % sprite.frames.size()).index;
          for (int sub = 0; sub < frame.duration; sub++) {
            sequence.put(at * 4, unique.get(frame.index));
            sequence.put(at * 4 + 1, unique.get(next));
            sequence.put(at * 4 + 2, sprite.blend ? sub / (float) frame.duration : 0);
            sequence.put(at * 4 + 3, sprite.blend ? 1f / frame.duration : 0);
            at++;
          }
        }
      }
      GL30.glGenerateMipmap(GL30.GL_TEXTURE_2D_ARRAY);
      GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, 0);
      timeline = GL11.glGenTextures();
      int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, timeline);
      GL11.glTexImage2D(
          GL11.GL_TEXTURE_2D,
          0,
          GL30.GL_RGBA32F,
          1024,
          16,
          0,
          GL11.GL_RGBA,
          GL11.GL_FLOAT,
          sequence);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
      GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
      GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
    } finally {
      MemoryUtil.memFree(pixels);
      MemoryUtil.memFree(sequence);
      unpack.close();
    }
  }

  @Override
  public void close() {
    if (frames != 0) GL11.glDeleteTextures(frames);
    if (timeline != 0) GL11.glDeleteTextures(timeline);
  }
}
