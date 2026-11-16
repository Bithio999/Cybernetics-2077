package dev.cybernetics2077.client.ui;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Filtering is part of the material, so GUI-scale changes cannot turn type into enlarged pixels.
 */
public final class CyberMaterial extends RenderStateShard {
  private static ShaderInstance shader;

  public static void register(net.neoforged.neoforge.client.event.RegisterShadersEvent event)
      throws java.io.IOException {
    event.registerShader(
        new ShaderInstance(
            event.getResourceProvider(),
            dev.cybernetics2077.Cybernetics2077.id("ui_texture"),
            DefaultVertexFormat.POSITION_TEX_COLOR),
        s -> shader = s);
  }

  private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

  static RenderType texture(ResourceLocation path) {
    return TYPES.computeIfAbsent(
        path,
        p ->
            RenderType.create(
                "cyberware_smooth_type",
                DefaultVertexFormat.POSITION_TEX_COLOR,
                VertexFormat.Mode.QUADS,
                8192,
                false,
                false,
                RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> shader))
                    .setTextureState(new TextureStateShard(p, true, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(NO_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)));
  }

  private CyberMaterial() {
    super("cyberware", () -> {}, () -> {});
  }
}
