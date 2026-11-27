package dev.cybernetics2077.client.ui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.mob.Cyberpsycho;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Minecraft humanoid skins with the mod's existing armored panels and illuminated implants. */
public final class CyberpsychoRenderer
    extends HumanoidMobRenderer<Cyberpsycho, PlayerModel<Cyberpsycho>> {
  private static final ResourceLocation SKIN =
      Cybernetics2077.id("textures/entity/cyberpsycho.png");
  private final CyberpsychoModel[] bodies;

  public CyberpsychoRenderer(EntityRendererProvider.Context context) {
    super(context, new CyberpsychoModel(context.bakeLayer(CyberpsychoModel.LAYERS[0])), .5f);
    bodies =
        new CyberpsychoModel[] {
          (CyberpsychoModel) model,
          new CyberpsychoModel(context.bakeLayer(CyberpsychoModel.LAYERS[1])),
          new CyberpsychoModel(context.bakeLayer(CyberpsychoModel.LAYERS[2]))
        };
    addLayer(
        new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(
            this, context.getItemInHandRenderer()));
    addLayer(new dev.cybernetics2077.client.mantis.MobMantisLayer<>(this));
    addLayer(
        new RenderLayer<>(this) {
          @Override
          public void render(
              PoseStack pose,
              MultiBufferSource buffers,
              int light,
              Cyberpsycho mob,
              float swing,
              float amount,
              float partial,
              float age,
              float yaw,
              float pitch) {
            if (mob.isInvisible()) return;
            getParentModel()
                .renderToBuffer(
                    pose,
                    buffers.getBuffer(
                        RenderType.entityDecal(
                            Cybernetics2077.id("textures/entity/chromed_humanoid.png"))),
                    light,
                    LivingEntityRenderer.getOverlayCoords(mob, 0),
                    -1);
            getParentModel()
                .renderToBuffer(
                    pose,
                    buffers.getBuffer(
                        RenderType.eyes(
                            Cybernetics2077.id("textures/entity/chromed_humanoid_glow.png"))),
                    15728640,
                    OverlayTexture.NO_OVERLAY,
                    mob.berserk() ? 0xffff806b : 0xff9ceae5);
          }
        });
  }

  @Override
  public void render(
      Cyberpsycho mob,
      float yaw,
      float partial,
      PoseStack pose,
      MultiBufferSource buffers,
      int light) {
    model = bodies[mob.role().ordinal()];
    super.render(mob, yaw, partial, pose, buffers, light);
  }

  @Override
  public ResourceLocation getTextureLocation(Cyberpsycho mob) {
    return SKIN;
  }
}
