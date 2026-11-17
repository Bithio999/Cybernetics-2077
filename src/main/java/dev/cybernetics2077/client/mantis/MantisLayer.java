package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
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
      // Keep the actual shoulder socket without adding vanilla walk/swing angles twice.
      p.translate(arm.x / 16, arm.y / 16, arm.z / 16);
      p.mulPose(
          MantisRig.shoulder(
              left,
              player.getSkin().model() == net.minecraft.client.resources.PlayerSkin.Model.SLIM,
              motion));
      MantisMesh.render(p, buffers, light, player, left, motion);
      p.popPose();
    }
  }
}
