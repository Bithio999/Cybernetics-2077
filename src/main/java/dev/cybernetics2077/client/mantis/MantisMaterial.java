package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;

/** Native entity lighting with real triangles; no duplicate fourth vertex per face. */
final class MantisMaterial extends RenderStateShard {
  static final RenderType TYPE = create(dev.cybernetics2077.cyberware.MantisVariant.PHYSICAL);
  private static final java.util.Map<dev.cybernetics2077.cyberware.MantisVariant, RenderType>
      TYPES = new java.util.EnumMap<>(dev.cybernetics2077.cyberware.MantisVariant.class);

  static RenderType type(dev.cybernetics2077.cyberware.MantisVariant variant) {
    return variant == dev.cybernetics2077.cyberware.MantisVariant.PHYSICAL
        ? TYPE
        : TYPES.computeIfAbsent(variant, MantisMaterial::create);
  }

  private static RenderType create(dev.cybernetics2077.cyberware.MantisVariant variant) {
    return RenderType.create(
        "mantis_blender_triangles_" + variant.id,
        DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.TRIANGLES,
        524288,
        false,
        false,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
            .setTextureState(new TextureStateShard(MantisBlenderMesh.atlas(variant), false, false))
            .setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP)
            .setOverlayState(OVERLAY)
            .createCompositeState(true));
  }

  private MantisMaterial() {
    super("mantis", () -> {}, () -> {});
  }
}
