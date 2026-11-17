package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

public final class MantisLayer
    extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
  public MantisLayer(
      RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
    super(parent);
  }

  @Override
  public void render(
      PoseStack p,
      MultiBufferSource buffers,
      int light,
      AbstractClientPlayer player,
      float limbSwing,
      float limbAmount,
      float partial,
      float age,
      float headYaw,
      float headPitch) {
    if (!CyberwareClient.visible(player) || player.isInvisible()) return;
    for (boolean left : new boolean[] {false, true}) {
      var arm = left ? getParentModel().leftArm : getParentModel().rightArm;
      var motion = CyberwareClient.pose(player, left, partial);
      p.pushPose();
      arm.translateAndRotate(p);
      p.translate(
          (left ? 1 : -1)
              * (player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM
                  ? .5f
                  : 1f)
              / 16f,
          0,
          0);
      p.mulPose(Axis.ZP.rotationDegrees(motion.roll() * .9f));
      p.mulPose(Axis.YP.rotationDegrees(motion.yaw() * .8f));
      p.mulPose(Axis.XP.rotationDegrees(motion.pitch() - motion.blade() * 55));
      MantisMesh.render(p, buffers, light, player, left, motion);
      p.popPose();
    }
  }
}
