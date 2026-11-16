package dev.cybernetics2077.client.ui;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.CyberwareCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** A real Minecraft player mesh in a reusable, depth-isolated diagnostic render target. */
public final class HologramRenderer extends RenderStateShard {
  private static ShaderInstance shader;
  private static PlayerModel<AbstractClientPlayer> model;
  private static RenderTarget target;
  private static final ByteBufferBuilder MEMORY = new ByteBufferBuilder(32768);
  private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(MEMORY);
  private static final RenderType MATERIAL =
      RenderType.create(
          "cyberware_hologram",
          DefaultVertexFormat.NEW_ENTITY,
          VertexFormat.Mode.QUADS,
          32768,
          false,
          true,
          RenderType.CompositeState.builder()
              .setShaderState(new ShaderStateShard(() -> shader))
              .setTextureState(
                  new TextureStateShard(
                      Cybernetics2077.id("textures/entity/diagnostic.png"), false, false))
              .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
              .setCullState(CULL)
              .setDepthTestState(LEQUAL_DEPTH_TEST)
              .setWriteMaskState(COLOR_DEPTH_WRITE)
              .createCompositeState(false));

  public static void register(RegisterShadersEvent e) throws java.io.IOException {
    e.registerShader(
        new ShaderInstance(
            e.getResourceProvider(),
            Cybernetics2077.id("hologram"),
            DefaultVertexFormat.NEW_ENTITY),
        s -> shader = s);
  }

  public static void reload() {
    model = null;
  }

  public static void draw(
      GuiGraphics g, CyberwareCategory selected, BodyView view, float alpha, float yaw) {
    if (shader == null) return;
    var mc = Minecraft.getInstance();
    if (model == null)
      model = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
    if (target == null) {
      target = new TextureTarget(448, 896, true, Minecraft.ON_OSX);
      target.setClearColor(0, 0, 0, 0);
    }
    g.flush();
    var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
    var sorting = RenderSystem.getVertexSorting();
    var mv = RenderSystem.getModelViewStack();
    boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST), blend = GL11.glIsEnabled(GL11.GL_BLEND);
    float[] tint = RenderSystem.getShaderColor().clone();
    mv.pushMatrix();
    mv.identity();
    RenderSystem.applyModelViewMatrix();
    try {
      target.clear(Minecraft.ON_OSX);
      target.bindWrite(true);
      RenderSystem.setProjectionMatrix(
          new Matrix4f()
              .setOrtho(
                  view.centerX() - halfWidth(view),
                  view.centerX() + halfWidth(view),
                  view.centerY() + view.span() / 2,
                  view.centerY() - view.span() / 2,
                  -10,
                  10),
          VertexSorting.ORTHOGRAPHIC_Z);
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      var pose = new PoseStack();
      pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(view.yaw() + yaw));
      pose.scale(.88f, 1, 1);
      model.head.xRot = -.025f;
      model.head.yRot = yaw * .0015f;
      model.rightArm.zRot = .055f;
      model.leftArm.zRot = -.055f;
      model.rightLeg.zRot = .012f;
      model.leftLeg.zRot = -.012f;
      var out = BUFFER.getBuffer(MATERIAL);
      boolean all =
          selected == CyberwareCategory.SKELETON
              || selected == CyberwareCategory.INTEGUMENTARY_SYSTEM;
      boolean head =
          all
              || selected == CyberwareCategory.FRONTAL_CORTEX
              || selected == CyberwareCategory.FACE
              || selected == CyberwareCategory.NERVOUS_SYSTEM;
      boolean torso =
          all
              || selected == CyberwareCategory.OPERATING_SYSTEM
              || selected == CyberwareCategory.CIRCULATORY_SYSTEM
              || selected == CyberwareCategory.NERVOUS_SYSTEM;
      boolean arms =
          all || selected == CyberwareCategory.ARMS || selected == CyberwareCategory.HANDS;
      boolean legs = all || selected == CyberwareCategory.LEGS;
      model.head.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, head ? 0xf07ef4ff : 0xbff3a3b5);
      model.body.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, torso ? 0xf07ef4ff : 0xbfe68194);
      model.rightArm.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, arms ? 0xf07ef4ff : 0xbfba7d9a);
      model.leftArm.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, arms ? 0xf07ef4ff : 0xbfba7d9a);
      model.rightLeg.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, legs ? 0xf07ef4ff : 0xbf66a7c6);
      model.leftLeg.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, legs ? 0xf07ef4ff : 0xbf66a7c6);
      BUFFER.endBatch();
    } finally {
      mc.getMainRenderTarget().bindWrite(true);
      RenderSystem.setProjectionMatrix(projection, sorting);
      mv.popMatrix();
      RenderSystem.applyModelViewMatrix();
    }
    try {
      RenderSystem.disableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      // FBO colors already include surface alpha. Composite as premultiplied color.
      RenderSystem.blendFuncSeparate(
          com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
          com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
          com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
          com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, target.getColorTextureId());
      var b =
          Tesselator.getInstance()
              .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
      var p = g.pose().last().pose();
      int color = ((int) (255 * alpha) << 24) | 0xffffff;
      float l = view.x(), t = view.y(), r = l + view.w(), bottom = t + view.h();
      b.addVertex(p, l, t, 0).setUv(0, 1).setColor(color);
      b.addVertex(p, l, bottom, 0).setUv(0, 0).setColor(color);
      b.addVertex(p, r, bottom, 0).setUv(1, 0).setColor(color);
      b.addVertex(p, r, t, 0).setUv(1, 1).setColor(color);
      BufferUploader.drawWithShader(b.buildOrThrow());
    } finally {
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);
      if (depth) RenderSystem.enableDepthTest();
      else RenderSystem.disableDepthTest();
      if (blend) RenderSystem.enableBlend();
      else RenderSystem.disableBlend();
    }
  }

  private static float halfWidth(BodyView view) {
    // Match the first release's .73 horizontal extent in its original overview viewport.
    return view.span() * view.w() / view.h() * (1.46f * 830f / (2.28f * 448f)) / 2;
  }

  private HologramRenderer() {
    super("cyberware", () -> {}, () -> {});
  }
}
