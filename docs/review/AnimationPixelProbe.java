package dev.cybernetics2077.review;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.client.temporal.*;
import dev.cybernetics2077.temporal.AnimationLattice;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/** Compare real shader pixels against an independently selected source animation frame. */
final class AnimationPixelProbe {
  static List<Map<String, Object>> run() throws Exception {
    var mc = Minecraft.getInstance();
    var camera = mc.gameRenderer.getMainCamera().getPosition();
    var atlas = mc.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS);
    var original = GameRenderer.getRendertypeSolidShader();
    var field = BlockAnimations.class.getDeclaredField("ticks");
    field.setAccessible(true);
    var results = new ArrayList<Map<String, Object>>();
    try {
      BlockAnimations.begin(camera, .5f);
      var local = AnimationShaders.select("solid", original);
      ContentClientReview.check(local != original, "Local shader not selected");
      for (String name : List.of("water_still", "lava_still", "fire_0", "nether_portal", "magma")) {
        var sprite = atlas.getSprite(ResourceLocation.withDefaultNamespace("block/" + name));
        var node = new AnimationLattice.Node(0, 16, 2);
        var c = BlockAnimations.LATTICE.clocks.get(node);
        ContentClientReview.check(c != null, "No source-point visual clock");
        double time = field.getLong(null) - c.lag + c.pace * .5;
        try (var expected = new DynamicTexture(expected(sprite, time));
            var white = new DynamicTexture(white())) {
          int[] actual =
              draw(
                  local,
                  atlas.getId(),
                  white.getId(),
                  sprite.getU0(),
                  sprite.getV0(),
                  sprite.getU1(),
                  sprite.getV1(),
                  new Vec3(0, 64, 8).subtract(camera));
          int[] oracle = draw(original, expected.getId(), white.getId(), 0, 0, 1, 1, Vec3.ZERO);
          int[] outside =
              draw(
                  local,
                  atlas.getId(),
                  white.getId(),
                  sprite.getU0(),
                  sprite.getV0(),
                  sprite.getU1(),
                  sprite.getV1(),
                  new Vec3(80, 64, 8).subtract(camera));
          int[] nativePixels =
              draw(
                  original,
                  atlas.getId(),
                  white.getId(),
                  sprite.getU0(),
                  sprite.getV0(),
                  sprite.getU1(),
                  sprite.getV1(),
                  Vec3.ZERO);
          int maxError = 0, outsideError = 0, changed = 0;
          for (int i = 0; i < actual.length; i++)
            for (int shift : new int[] {0, 8, 16}) {
              maxError =
                  Math.max(
                      maxError, Math.abs((actual[i] >> shift & 255) - (oracle[i] >> shift & 255)));
              outsideError =
                  Math.max(
                      outsideError,
                      Math.abs((outside[i] >> shift & 255) - (nativePixels[i] >> shift & 255)));
              if ((actual[i] >> shift & 255) != (nativePixels[i] >> shift & 255)) changed++;
            }
          results.add(
              Map.of(
                  "sprite",
                  name,
                  "maxChannelError",
                  maxError,
                  "outsideMaxError",
                  outsideError,
                  "changedChannels",
                  changed,
                  "lagTicks",
                  c.lag));
          if (maxError > 2) {
            for (var entry :
                Map.of("actual", actual, "expected", oracle, "native", nativePixels).entrySet()) {
              try (var im = new NativeImage(32, 32, false)) {
                for (int y = 0; y < 32; y++)
                  for (int x = 0; x < 32; x++) im.setPixelRGBA(x, y, entry.getValue()[y * 32 + x]);
                im.writeToFile(
                    CyberwareClientReview.OUT.resolve(
                        "animation-probe-" + entry.getKey() + ".png"));
              }
            }
            for (String uniform :
                List.of(
                    "CyberCameraCell",
                    "CyberCameraRem",
                    "CyberRects[0]",
                    "CyberAnimations[0]",
                    "CyberPartial",
                    "CyberCount",
                    "CyberCells",
                    "CyberFrames")) {
              int location = org.lwjgl.opengl.GL20.glGetUniformLocation(local.getId(), uniform);
              float[] value = new float[4];
              if (location >= 0)
                org.lwjgl.opengl.GL20.glGetUniformfv(local.getId(), location, value);
              System.out.println(
                  "Animation probe uniform "
                      + uniform
                      + " "
                      + location
                      + " "
                      + Arrays.toString(value));
            }
            System.out.println(
                "Animation probe location "
                    + camera
                    + "; clock="
                    + c.lag
                    + "/"
                    + c.pace
                    + "; GPU bytes="
                    + BlockAnimations.textureBytes());
          }
          ContentClientReview.check(
              maxError <= 2,
              "Local texture pixels disagree with source frame: " + results.getLast());
          ContentClientReview.check(outsideError == 0, "Texture changed outside field: " + name);
        }
      }
    } finally {
      BlockAnimations.end();
      mc.getMainRenderTarget().bindWrite(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.enableBlend();
    }
    ContentClientReview.check(
        results.stream().anyMatch(r -> ((Number) r.get("changedChannels")).intValue() > 100),
        "Local frame did not differ from native atlas");
    return results;
  }

  static NativeImage white() {
    var im = new NativeImage(1, 1, false);
    im.setPixelRGBA(0, 0, -1);
    return im;
  }

  record Frame(int index, int time) {}

  static NativeImage expected(TextureAtlasSprite sprite, double time) {
    var content = sprite.contents();
    var source = content.getOriginalImage();
    var meta = content.metadata().getSection(AnimationMetadataSection.SERIALIZER).orElseThrow();
    var frames = new ArrayList<Frame>();
    meta.forEachFrame((i, t) -> frames.add(new Frame(i, t)));
    if (frames.isEmpty())
      for (int i = 0;
          i < source.getWidth() / content.width() * (source.getHeight() / content.height());
          i++) frames.add(new Frame(i, meta.getDefaultFrameTime()));
    int cycle = frames.stream().mapToInt(Frame::time).sum();
    time = (time % cycle + cycle) % cycle;
    int current = 0;
    while (time >= frames.get(current).time) {
      time -= frames.get(current).time;
      current++;
    }
    var a = frames.get(current);
    var b = frames.get((current + 1) % frames.size());
    float blend = meta.isInterpolatedFrames() ? (float) (time / a.time) : 0;
    var image = new NativeImage(content.width(), content.height(), false);
    for (int y = 0; y < content.height(); y++)
      for (int x = 0; x < content.width(); x++) {
        int first = sprite.getPixelRGBA(a.index, x, y),
            next = sprite.getPixelRGBA(b.index, x, y),
            color = first & 0xff000000;
        for (int shift : new int[] {0, 8, 16})
          color |=
              Math.round((first >> shift & 255) * (1 - blend) + (next >> shift & 255) * blend)
                  << shift;
        image.setPixelRGBA(x, y, color);
      }
    return image;
  }

  static int[] draw(
      ShaderInstance shader,
      int texture,
      int light,
      float u0,
      float v0,
      float u1,
      float v1,
      Vec3 offset) {
    var target = new TextureTarget(32, 32, false, Minecraft.ON_OSX);
    try {
      target.setClearColor(0, 0, 0, 0);
      target.clear(Minecraft.ON_OSX);
      target.bindWrite(true);
      RenderSystem.disableDepthTest();
      RenderSystem.disableBlend();
      RenderSystem.disableCull();
      shader.setSampler("Sampler0", texture);
      shader.setSampler("Sampler2", light);
      shader
          .safeGetUniform("ModelViewMat")
          .set(new Matrix4f().translation((float) -offset.x, (float) -offset.y, (float) -offset.z));
      shader.safeGetUniform("ProjMat").set(new Matrix4f().ortho(-1, 1, -1, 1, -1, 1));
      shader
          .safeGetUniform("ChunkOffset")
          .set((float) offset.x, (float) offset.y, (float) offset.z);
      shader.safeGetUniform("ColorModulator").set(1f, 1f, 1f, 1f);
      shader.safeGetUniform("FogStart").set(10000f);
      shader.safeGetUniform("FogEnd").set(20000f);
      shader.apply();
      var b = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
      b.addVertex(-1, -1, 0).setColor(-1).setUv(u0, v0).setUv2(240, 240).setNormal(0, 0, 1);
      b.addVertex(1, -1, 0).setColor(-1).setUv(u1, v0).setUv2(240, 240).setNormal(0, 0, 1);
      b.addVertex(1, 1, 0).setColor(-1).setUv(u1, v1).setUv2(240, 240).setNormal(0, 0, 1);
      b.addVertex(-1, 1, 0).setColor(-1).setUv(u0, v1).setUv2(240, 240).setNormal(0, 0, 1);
      BufferUploader.draw(b.buildOrThrow());
      shader.clear();
      try (var image = Screenshot.takeScreenshot(target)) {
        int[] pixels = new int[1024];
        for (int y = 0; y < 32; y++)
          for (int x = 0; x < 32; x++) pixels[y * 32 + x] = image.getPixelRGBA(x, y);
        return pixels;
      }
    } finally {
      target.destroyBuffers();
    }
  }
}
