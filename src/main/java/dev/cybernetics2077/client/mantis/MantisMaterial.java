package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;

/** Native entity lighting with real triangles; no duplicate fourth vertex per face. */
final class MantisMaterial extends RenderStateShard {
  static final RenderType TYPE =
      RenderType.create(
          "mantis_blender_triangles",
          DefaultVertexFormat.NEW_ENTITY,
          VertexFormat.Mode.TRIANGLES,
          524288,
          false,
          false,
          RenderType.CompositeState.builder()
              .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
              .setTextureState(new TextureStateShard(MantisBlenderMesh.ATLAS, false, false))
              .setCullState(NO_CULL)
              .setLightmapState(LIGHTMAP)
              .setOverlayState(OVERLAY)
              .createCompositeState(true));

  private MantisMaterial() {
    super("mantis", () -> {}, () -> {});
  }
}
