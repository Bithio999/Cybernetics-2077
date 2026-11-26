package dev.cybernetics2077.client.temporal;

import com.google.gson.*;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.cybernetics2077.Cybernetics2077;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.*;

/** Augments installed block shaders at reload; preserves native fog, lighting and cutout rules. */
public final class AnimationShaders {
  static final Map<String, LocalShader> SHADERS = new HashMap<>();

  public static void register(RegisterShadersEvent event) throws IOException {
    SHADERS.clear();
    BlockAnimations.release();
    for (String type :
        List.of("solid", "cutout", "cutout_mipped", "translucent", "translucent_moving_block")) {
      var provider = event.getResourceProvider();
      var resource =
          provider.getResourceOrThrow(
              ResourceLocation.withDefaultNamespace("shaders/core/rendertype_" + type + ".json"));
      JsonObject json;
      try (var reader = resource.openAsReader()) {
        json = JsonParser.parseReader(reader).getAsJsonObject();
      }
      var virtual = new HashMap<ResourceLocation, byte[]>();
      String name = "local_blocks_" + type;
      boolean supported = true;
      for (String stage : List.of("vertex", "fragment")) {
        String ext = stage.equals("vertex") ? ".vsh" : ".fsh";
        var source = ResourceLocation.parse(json.get(stage).getAsString());
        String text;
        try (var reader =
            provider.openAsReader(
                ResourceLocation.fromNamespaceAndPath(
                    source.getNamespace(), "shaders/core/" + source.getPath() + ext))) {
          text = new String(reader.lines().collect(java.util.stream.Collectors.joining("\n")));
        }
        if (stage.equals("vertex")) {
          if (!text.contains("void main()") || !text.contains("in vec3 Position;")) {
            supported = false;
            break;
          }
          text = text.replace("void main()", "out vec3 cyberWorld;\nvoid main()");
          int end = text.lastIndexOf('}');
          text =
              text.substring(0, end)
                  + "\ncyberWorld = Position"
                  + (text.contains("uniform vec3 ChunkOffset;") ? " + ChunkOffset" : "")
                  + ";\n"
                  + text.substring(end);
        } else {
          if (!text.contains("texture(Sampler0, texCoord0)")) {
            supported = false;
            break;
          }
          text =
              text.replace(
                      "void main()",
                      "#moj_import <cybernetics2077:local_animation.glsl>\nvoid main()")
                  .replace("texture(Sampler0, texCoord0)", "cyberSample(texCoord0)");
        }
        virtual.put(
            Cybernetics2077.id("shaders/core/" + name + ext),
            text.getBytes(StandardCharsets.UTF_8));
        json.addProperty(stage, Cybernetics2077.id(name).toString());
      }
      if (!supported) continue; // Unrecognized resource-pack shader: retain its native rendering.
      for (String sampler : List.of("CyberTimeline", "CyberCells")) {
        var entry = new JsonObject();
        entry.addProperty("name", sampler);
        json.getAsJsonArray("samplers").add(entry);
      }
      virtual.put(
          Cybernetics2077.id("shaders/core/" + name + ".json"),
          json.toString().getBytes(StandardCharsets.UTF_8));
      ResourceProvider wrapped =
          path -> {
            var bytes = virtual.get(path);
            return bytes == null
                ? provider.getResource(path)
                : Optional.of(
                    new Resource(resource.source(), () -> new ByteArrayInputStream(bytes)));
          };
      var shader = new LocalShader(wrapped, Cybernetics2077.id(name));
      event.registerShader(shader, s -> SHADERS.put(type, shader));
    }
  }

  public static ShaderInstance select(String type, ShaderInstance original) {
    if (!BlockAnimations.rendering() || !SHADERS.containsKey(type)) return original;
    var shader = SHADERS.get(type);
    shader.setSampler("CyberTimeline", BlockAnimations.atlas.timeline);
    shader.setSampler("CyberCells", BlockAnimations.cellsTexture);
    return shader;
  }

  static final class LocalShader extends ShaderInstance {
    final int frames, rects, animations, count, cameraCell, cameraRem, partial;
    long frame = Long.MIN_VALUE;

    LocalShader(ResourceProvider provider, ResourceLocation name) throws IOException {
      super(provider, name, DefaultVertexFormat.BLOCK);
      frames = GL20.glGetUniformLocation(getId(), "CyberFrames");
      rects = GL20.glGetUniformLocation(getId(), "CyberRects[0]");
      animations = GL20.glGetUniformLocation(getId(), "CyberAnimations[0]");
      count = GL20.glGetUniformLocation(getId(), "CyberCount");
      cameraCell = GL20.glGetUniformLocation(getId(), "CyberCameraCell");
      cameraRem = GL20.glGetUniformLocation(getId(), "CyberCameraRem");
      partial = GL20.glGetUniformLocation(getId(), "CyberPartial");
    }

    @Override
    public void apply() {
      super.apply();
      if (BlockAnimations.atlas == null) return;
      int active = GlStateManager._getActiveTexture();
      RenderSystem.activeTexture(GL13.GL_TEXTURE0 + 8);
      GL11.glBindTexture(GL30.GL_TEXTURE_2D_ARRAY, BlockAnimations.atlas.frames);
      RenderSystem.activeTexture(active);
      GL20.glUniform1i(frames, 8);
      if (frame == BlockAnimations.serial) return;
      frame = BlockAnimations.serial;
      GL20.glUniform4fv(rects, BlockAnimations.rectangles);
      GL20.glUniform4fv(animations, BlockAnimations.animations);
      GL20.glUniform1i(count, BlockAnimations.atlas.sprites.size());
      var camera = BlockAnimations.camera;
      int x = (int) Math.floor(camera.x / 4),
          y = (int) Math.floor(camera.y / 4),
          z = (int) Math.floor(camera.z / 4);
      GL20.glUniform3f(cameraCell, x, y, z);
      GL20.glUniform3f(
          cameraRem,
          (float) (camera.x - x * 4.),
          (float) (camera.y - y * 4.),
          (float) (camera.z - z * 4.));
      GL20.glUniform1f(partial, BlockAnimations.partial);
      BlockAnimations.shaderUses++;
    }
  }

  private AnimationShaders() {}
}
