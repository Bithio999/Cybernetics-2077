package dev.cybernetics2077.client.temporal;

import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import java.util.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

/** Textured, emissive silhouettes with a view-dependent rim; ghosts never write depth. */
public final class TemporalMaterial extends RenderStateShard {
  private static ShaderInstance shader;

  private record Key(ResourceLocation texture, VertexFormat.Mode mode) {}

  private static final Map<Key, RenderType> TYPES = new HashMap<>();

  public static void register(RegisterShadersEvent e) throws java.io.IOException {
    e.registerShader(
        new ShaderInstance(
            e.getResourceProvider(),
            Cybernetics2077.id("temporal_ghost"),
            DefaultVertexFormat.NEW_ENTITY),
        s -> shader = s);
  }

  public static RenderType type(ResourceLocation texture, VertexFormat.Mode mode) {
    return TYPES.computeIfAbsent(
        new Key(texture, mode),
        k ->
            RenderType.create(
                "sandy_pose_echo",
                DefaultVertexFormat.NEW_ENTITY,
                mode,
                262144,
                false,
                true,
                RenderType.CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> shader))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false)));
  }

  private TemporalMaterial() {
    super("sandy_ghost", () -> {}, () -> {});
  }
}
