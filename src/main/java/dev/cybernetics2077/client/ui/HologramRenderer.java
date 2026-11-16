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

  private record Uploaded(DiagnosticModel.Group group, VertexBuffer buffer, VertexBuffer back) {}

  private static java.util.List<Uploaded> meshes;
  private static RenderTarget target;
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
                      Cybernetics2077.id("textures/entity/dermal_detail.png"), false, false))
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
                      Cybernetics2077.id("textures/entity/dermal_detail.png"), false, false))
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
    Runnable clear =
        () -> {
          if (meshes != null) {
            for (var mesh : meshes) {
              mesh.buffer().close();
              if (mesh.back() != mesh.buffer()) mesh.back().close();
            }
            meshes = null;
          }
        };
    if (RenderSystem.isOnRenderThread()) clear.run();
    else RenderSystem.recordRenderCall(clear::run);
  }

  private static void load() {
    var mc = Minecraft.getInstance();
    try (var input = mc.getResourceManager().open(Cybernetics2077.id("models/hologram/body.cyh"))) {
      var model = DiagnosticModel.read(input);
      var uploaded = new java.util.ArrayList<Uploaded>();
      try {
        for (var group : model.groups) {
          int role = group.role();
          boolean film =
              role == 0 || role == 1 || role == 7 || role == 8 || role == 10 || role == 11;
          var front = upload(group, film, false);
          var back = film ? upload(group, true, true) : front;
          uploaded.add(new Uploaded(group, front, back));
        }
        VertexBuffer.unbind();
        uploaded.sort(java.util.Comparator.comparingInt(p -> priority(p.group().role())));
        meshes = java.util.List.copyOf(uploaded);
      } catch (RuntimeException e) {
        for (var part : uploaded) {
          part.buffer().close();
          if (part.back() != part.buffer()) part.back().close();
        }
        throw e;
      }
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot load Blender cyberware anatomy", e);
    }
  }

  private static int priority(int role) {
    return switch (role) {
      case 1, 10, 11 -> 0;
      case 7 -> 1;
      case 0 -> 2;
      case 8 -> 3;
      default -> -1;
    };
  }

  private static VertexBuffer upload(DiagnosticModel.Group group, boolean sorted, boolean back) {
    var d = group.data();
    int triangles = group.vertices() / 3;
    long[] order = new long[triangles];
    for (int i = 0; i < triangles; i++) {
      float z = (d[i * 24 + 2] + d[i * 24 + 10] + d[i * 24 + 18]) / 3;
      long key = Float.floatToRawIntBits(z + 2f) & 0xffffffffL;
      order[i] = sorted ? (key << 32) | (i & 0xffffffffL) : i;
    }
    if (sorted) java.util.Arrays.sort(order);
    var builder =
        Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.NEW_ENTITY);
    int tint =
        0xff000000
            | ((int) (group.material()[0] * 255) << 16)
            | ((int) (group.material()[1] * 255) << 8)
            | (int) (group.material()[2] * 255);
    for (int t = 0; t < triangles; t++) {
      int index = (int) order[sorted && !back ? triangles - 1 - t : t];
      for (int corner = 0; corner < 3; corner++) {
        int o = index * 24 + corner * 8;
        builder
            .addVertex(d[o], d[o + 1], d[o + 2])
            .setColor(tint)
            .setUv(d[o + 6], d[o + 7])
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(0xf000f0)
            .setNormal(d[o + 3], d[o + 4], d[o + 5]);
      }
    }
    var buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
    buffer.bind();
    buffer.upload(builder.buildOrThrow());
    return buffer;
  }

  public static void draw(
      GuiGraphics g, CyberwareCategory selected, BodyView view, float alpha, float detail) {
    if (shader == null) return;
    var mc = Minecraft.getInstance();
    if (meshes == null) load();
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
      var transform = new Matrix4f().rotateY((float) Math.toRadians(view.yaw()));
      shader
          .safeGetUniform("SelectedRegion")
          .set(selected == null ? -1f : (float) selected.ordinal());
      shader.safeGetUniform("Detail").set(detail);
      renderParts(
          transform, MATERIAL, 0, Math.cos(Math.toRadians(view.yaw())) < 0, selected, detail);
      renderParts(transform, SHELL, 1, Math.cos(Math.toRadians(view.yaw())) < 0, selected, detail);
      VertexBuffer.unbind();
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
      Matrix4f transform,
      RenderType type,
      int pass,
      boolean back,
      CyberwareCategory selected,
      float detail) {
    type.setupRenderState();
    try {
      for (var part : meshes) {
        int role = part.group().role();
        boolean film = role == 0 || role == 1 || role == 7 || role == 8 || role == 10 || role == 11;
        if (role == 2
            && detail > .001
            && (selected == CyberwareCategory.NERVOUS_SYSTEM
                || selected == CyberwareCategory.OPERATING_SYSTEM)) film = true;
        if (film != (pass == 1)) continue;
        shader.safeGetUniform("Role").set((float) role);
        shader.safeGetUniform("Region").set((float) part.group().region());
        var buffer = back ? part.back() : part.buffer();
        buffer.bind();
        buffer.drawWithShader(transform, RenderSystem.getProjectionMatrix(), shader);
      }
    } finally {
      type.clearRenderState();
    }
  }

  private HologramRenderer() {
    super("cyberware", () -> {}, () -> {});
  }
}
