package dev.cybernetics2077.client.ui;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.CyberwareCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Detailed original anatomical geometry in a reusable, depth-isolated diagnostic render target. */
public final class HologramRenderer extends RenderStateShard {
  private static ShaderInstance shader, composite;
  private static DiagnosticModel model;
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
                      Cybernetics2077.id("textures/entity/white.png"), false, false))
              .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
              .setCullState(NO_CULL)
              .setDepthTestState(LEQUAL_DEPTH_TEST)
              .setWriteMaskState(COLOR_DEPTH_WRITE)
              .createCompositeState(false));

  private static final RenderType SHELL =
      RenderType.create(
          "cyberware_glass",
          DefaultVertexFormat.NEW_ENTITY,
          VertexFormat.Mode.QUADS,
          32768,
          false,
          true,
          RenderType.CompositeState.builder()
              .setShaderState(new ShaderStateShard(() -> shader))
              .setTextureState(
                  new TextureStateShard(
                      Cybernetics2077.id("textures/entity/white.png"), false, false))
              .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
              .setCullState(NO_CULL)
              .setDepthTestState(LEQUAL_DEPTH_TEST)
              .setWriteMaskState(COLOR_WRITE)
              .createCompositeState(false));

  public static void register(RegisterShadersEvent e) throws java.io.IOException {
    e.registerShader(
        new ShaderInstance(
            e.getResourceProvider(),
            Cybernetics2077.id("body_composite"),
            DefaultVertexFormat.POSITION_TEX_COLOR),
        s -> composite = s);
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

  public static void draw(GuiGraphics g, CyberwareCategory selected, BodyView view, float alpha) {
    if (shader == null) return;
    var mc = Minecraft.getInstance();
    if (model == null) model = new DiagnosticModel();
    if (target == null) {
      target = new TextureTarget(768, 1024, true, Minecraft.ON_OSX);
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
                  view.centerX() - view.span() * view.w() / view.h() / 2,
                  view.centerX() + view.span() * view.w() / view.h() / 2,
                  view.centerY() + view.span() / 2,
                  view.centerY() - view.span() / 2,
                  10,
                  -10),
          VertexSorting.ORTHOGRAPHIC_Z);
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      var pose = new PoseStack();
      pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(view.yaw()));
      renderParts(pose, model.core, MATERIAL, selected);
      BUFFER.endBatch();
      renderParts(pose, model.shell, SHELL, selected);
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
      RenderSystem.setShader(() -> composite);
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

  private static void renderParts(
      PoseStack pose,
      java.util.List<DiagnosticModel.Face> parts,
      RenderType type,
      CyberwareCategory selected) {
    var out = BUFFER.getBuffer(type);
    var p = pose.last();
    for (var f : parts) {
      int color = f.color();
      boolean hot = f.region() == selected;
      if (hot) color = (color & 0xff000000) | 0x72e8f1;
      if (f.shell() && selected != null && selected != CyberwareCategory.INTEGUMENTARY_SYSTEM)
        color = (color & 0xffffff) | 0x1c000000;
      float shade =
          (float)
              (.66
                  + .34
                      * Math.max(
                          0,
                          f.normal()
                              .dot(new net.minecraft.world.phys.Vec3(-.3, -.5, -.8).normalize())));
      int a = color >>> 24,
          r = (int) ((color >> 16 & 255) * shade),
          g = (int) ((color >> 8 & 255) * shade),
          b = (int) ((color & 255) * shade);
      int tint = a << 24 | r << 16 | g << 8 | b;
      for (var v : new net.minecraft.world.phys.Vec3[] {f.a(), f.b(), f.c(), f.d()})
        out.addVertex(p, (float) v.x, (float) v.y, (float) v.z)
            .setColor(tint)
            .setUv(.5f, .5f)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(0xf000f0)
            .setNormal(p, (float) f.normal().x, (float) f.normal().y, (float) f.normal().z);
    }
  }

  private HologramRenderer() {
    super("cyberware", () -> {}, () -> {});
  }
}
