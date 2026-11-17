package dev.cybernetics2077.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cybernetics2077.client.mantis.CyberwareClient;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
public abstract class MantisArmorMixin {
  @Unique private boolean cybernetics$openArms;

  @Inject(
      method =
          "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
      at = @At("HEAD"))
  private void cybernetics$entity(
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      LivingEntity entity,
      float limb,
      float amount,
      float partial,
      float age,
      float yaw,
      float pitch,
      CallbackInfo ci) {
    cybernetics$openArms = entity instanceof AbstractClientPlayer p && CyberwareClient.visible(p);
  }

  @Inject(method = "setPartVisibility", at = @At("TAIL"))
  private void cybernetics$allowMechanism(
      HumanoidModel<?> model, EquipmentSlot slot, CallbackInfo ci) {
    if (cybernetics$openArms && slot == EquipmentSlot.CHEST) {
      model.rightArm.visible = false;
      model.leftArm.visible = false;
    }
  }
}
