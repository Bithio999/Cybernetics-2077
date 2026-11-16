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
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

/** Local player skin and model, composited once into a translucent hologram. */
public final class HologramRenderer extends RenderStateShard {
  private static ShaderInstance shader, composite;
  private static PlayerModel<AbstractClientPlayer> model;
  private static boolean modelSlim;
  private static ResourceLocation texture;
  private static RenderType material;
  private static RenderTarget target;
  private static final ByteBufferBuilder MEMORY = new ByteBufferBuilder(32768);
  private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(MEMORY);

  private static RenderType material(ResourceLocation skin) {
    if (skin.equals(texture) && material != null) return material;
    texture = skin;
    material =
        RenderType.create(
            "cyberware_hologram",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            32768,
            false,
            true,
            RenderType.CompositeState.builder()
                .setShaderState(new ShaderStateShard(() -> shader))
                .setTextureState(new TextureStateShard(skin, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(CULL)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false));

    return material;
  }

  public static void register(RegisterShadersEvent e) throws java.io.IOException {
    e.registerShader(
        new ShaderInstance(
            e.getResourceProvider(),
            Cybernetics2077.id("skin_composite"),
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
    texture = null;
    material = null;
  }

  public static void draw(
      GuiGraphics g, CyberwareCategory selected, BodyView view, float alpha, float yaw) {
    if (shader == null || composite == null) return;
    var mc = Minecraft.getInstance();
    var player = mc.player;
    if (player == null) return;
    var skin = player.getSkin();
    boolean slim = skin.model() == PlayerSkin.Model.SLIM;
    if (model == null || modelSlim != slim) {
      model =
          new PlayerModel<>(
              mc.getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER),
              slim);
      modelSlim = slim;
    }
    var skinMaterial = material(skin.texture());
    if (target == null) {
      target = new TextureTarget(448, 896, true, Minecraft.ON_OSX);
      target.setClearColor(0, 0, 0, 0);
      target.setFilterMode(GL11.GL_LINEAR);
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
      RenderSystem.setProjectionMatrix(view.projection(yaw), VertexSorting.ORTHOGRAPHIC_Z);
      RenderSystem.setShaderColor(1, 1, 1, 1);
      RenderSystem.enableDepthTest();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      var pose = new PoseStack();
      pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(view.yaw() + yaw));
      model.head.xRot = 0;
      model.head.yRot = 0;
      model.rightArm.zRot = 0;
      model.leftArm.zRot = 0;
      model.rightLeg.zRot = 0;
      model.leftLeg.zRot = 0;
      var out = BUFFER.getBuffer(skinMaterial);
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
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, head ? 0xff7ef4ff : 0xfff3a3b5);
      model.body.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, torso ? 0xff7ef4ff : 0xffe68194);
      model.rightArm.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, arms ? 0xff7ef4ff : 0xffba7d9a);
      model.leftArm.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, arms ? 0xff7ef4ff : 0xffba7d9a);
      model.rightLeg.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, legs ? 0xff7ef4ff : 0xff66a7c6);
      model.leftLeg.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, legs ? 0xff7ef4ff : 0xff66a7c6);
      // Use the same pose for each skin overlay, including slim-arm UVs and model geometry.
      model.hat.copyFrom(model.head);
      model.jacket.copyFrom(model.body);
      model.leftSleeve.copyFrom(model.leftArm);
      model.rightSleeve.copyFrom(model.rightArm);
      model.leftPants.copyFrom(model.leftLeg);
      model.rightPants.copyFrom(model.rightLeg);
      model.hat.visible = player.isModelPartShown(PlayerModelPart.HAT);
      model.jacket.visible = player.isModelPartShown(PlayerModelPart.JACKET);
      model.leftSleeve.visible = player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE);
      model.rightSleeve.visible = player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE);
      model.leftPants.visible = player.isModelPartShown(PlayerModelPart.LEFT_PANTS_LEG);
      model.rightPants.visible = player.isModelPartShown(PlayerModelPart.RIGHT_PANTS_LEG);
      model.hat.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, head ? 0xff7ef4ff : 0xfff3a3b5);
      model.jacket.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, torso ? 0xff7ef4ff : 0xffe68194);
      model.leftSleeve.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, arms ? 0xff7ef4ff : 0xffba7d9a);
      model.rightSleeve.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, arms ? 0xff7ef4ff : 0xffba7d9a);
      model.leftPants.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, legs ? 0xff7ef4ff : 0xff66a7c6);
      model.rightPants.render(
          pose, out, 0xf000f0, OverlayTexture.NO_OVERLAY, legs ? 0xff7ef4ff : 0xff66a7c6);
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
      int color = ((int) (255 * alpha * .58f) << 24) | 0xffffff;
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

  private HologramRenderer() {
    super("cyberware", () -> {}, () -> {});
  }
}
