package dev.cybernetics2077.client.temporal;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.lwjgl.opengl.*;

/** One scene copy and one restrained chromatic lens pass; HUD and GUI are rendered afterwards. */
public final class SandevistanLens {
  private static ShaderInstance shader;
  private static RenderTarget scene;
  public static int passes;

  public static void register(RegisterShadersEvent e) throws java.io.IOException {
    e.registerShader(
        new ShaderInstance(
            e.getResourceProvider(),
            Cybernetics2077.id("temporal_lens"),
            DefaultVertexFormat.POSITION_TEX),
        s -> shader = s);
  }

  public static void apply() {
    SandevistanClient.frame();
    var m = Minecraft.getInstance();
    float intensity = SandevistanClient.intensity;
    if (shader == null
        || intensity <= 0
        || m.player == null
        || m.screen != null
        || m.player.isSpectator()) return;
    var main = m.getMainRenderTarget();
    if (scene == null) scene = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
    else if (scene.width != main.width || scene.height != main.height)
      scene.resize(main.width, main.height, Minecraft.ON_OSX);
    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
    GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, scene.frameBufferId);
    GL30.glBlitFramebuffer(
        0,
        0,
        main.width,
        main.height,
        0,
        0,
        scene.width,
        scene.height,
        GL11.GL_COLOR_BUFFER_BIT,
        GL11.GL_NEAREST);
    main.bindWrite(false);
    int previousTexture = RenderSystem.getShaderTexture(0);
    RenderSystem.setShaderTexture(0, scene.getColorTextureId());
    RenderSystem.disableDepthTest();
    RenderSystem.depthMask(false);
    RenderSystem.disableBlend();
    try {
      RenderSystem.setShader(() -> shader);
      shader.setSampler("Sampler0", scene.getColorTextureId());
      shader.safeGetUniform("Intensity").set(intensity);
      shader.safeGetUniform("Pulse").set(SandevistanClient.pulse());
      shader.safeGetUniform("Aspect").set(main.width / (float) Math.max(1, main.height));
      var b =
          Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
      b.addVertex(-1, -1, 0).setUv(0, 0);
      b.addVertex(1, -1, 0).setUv(1, 0);
      b.addVertex(1, 1, 0).setUv(1, 1);
      b.addVertex(-1, 1, 0).setUv(0, 1);
      BufferUploader.drawWithShader(b.buildOrThrow());
      passes++;
    } finally {
      RenderSystem.setShaderTexture(0, previousTexture);
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.setShaderColor(1, 1, 1, 1);
    }
  }

  public static void release() {
    if (scene != null) {
      scene.destroyBuffers();
      scene = null;
    }
  }

  private SandevistanLens() {}
}
