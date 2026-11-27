package dev.cybernetics2077.client.mantis;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cybernetics2077.cyberware.MantisVariant;
import net.minecraft.client.model.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.*;

/** NPC arms use the same optimized Blender blade meshes, attached to the native animated limbs. */
public final class MobMantisLayer<T extends LivingEntity, M extends EntityModel<T>>
    extends RenderLayer<T, M> {
  public MobMantisLayer(RenderLayerParent<T, M> parent) {
    super(parent);
  }

  @Override
  public void render(
      PoseStack p,
      MultiBufferSource buffers,
      int light,
      T mob,
      float swing,
      float amount,
      float partial,
      float age,
      float yaw,
      float pitch) {
    var state = CyberwareClient.state(mob.getId());
    if (mob.isInvisible()
        || !state.getBoolean("claws")
        || !(getParentModel() instanceof ArmedModel armed)) return;
    var variant = MantisVariant.of(state.getInt("mantisVariant"));
    var motion = new MantisMotion.Pose(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1);
    for (var arm : HumanoidArm.values()) {
      p.pushPose();
      armed.translateToHand(arm, p);
      MantisBlenderMesh.render(
          p, buffers.getBuffer(MantisMaterial.type(variant)), light, motion, true, .25f, variant);
      p.popPose();
    }
  }
}
