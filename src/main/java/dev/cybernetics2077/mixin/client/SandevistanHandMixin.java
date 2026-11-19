package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.cybernetics2077.client.temporal.SandevistanTrails;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemInHandRenderer.class)
public abstract class SandevistanHandMixin {
  @WrapMethod(method = "renderArmWithItem")
  private void cybernetics$handEcho(
      AbstractClientPlayer p,
      float partial,
      float pitch,
      InteractionHand hand,
      float swing,
      ItemStack stack,
      float equip,
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      Operation<Void> original) {
    original.call(p, partial, pitch, hand, swing, stack, equip, pose, buffers, light);
    if (stack.getItem() instanceof MapItem) return;
    boolean left =
        (hand == InteractionHand.MAIN_HAND ? p.getMainArm() : p.getMainArm().getOpposite())
            == HumanoidArm.LEFT;
    SandevistanTrails.vanillaHand(
        p,
        left,
        buffers,
        recorder ->
            original.call(p, partial, pitch, hand, swing, stack, equip, pose, recorder, light));
  }
}
