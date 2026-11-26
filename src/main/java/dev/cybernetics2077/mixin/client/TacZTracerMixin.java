package dev.cybernetics2077.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.cybernetics2077.client.GuidedBulletClient;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A curving round's tracer stays on its actual path instead of a separate muzzle-offset path. */
@Pseudo
@Mixin(targets = "com.tacz.guns.client.renderer.entity.EntityBulletRenderer", remap = false)
public abstract class TacZTracerMixin {
  @Unique private boolean cybernetics$managed;

  @Inject(method = "renderTracerAmmo", at = @At("HEAD"))
  private void cybernetics$begin(
      @Coerce Object bullet,
      float[] color,
      float partial,
      com.mojang.blaze3d.vertex.PoseStack pose,
      int light,
      CallbackInfo ci) {
    cybernetics$managed = bullet instanceof Entity e && GuidedBulletClient.managed(e);
  }

  @ModifyExpressionValue(
      method = "lambda$renderTracerAmmo$1",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/tacz/guns/client/util/FirstPersonRenderMath;worldOffset(Lorg/joml/Quaternionfc;Lorg/joml/Vector3fc;)Lorg/joml/Vector3f;"))
  private Vector3f cybernetics$singlePath(Vector3f value) {
    return cybernetics$managed ? new Vector3f() : value;
  }

  @Inject(method = "renderTracerAmmo", at = @At("RETURN"))
  private void cybernetics$end(
      @Coerce Object bullet,
      float[] color,
      float partial,
      com.mojang.blaze3d.vertex.PoseStack pose,
      int light,
      CallbackInfo ci) {
    cybernetics$managed = false;
  }
}
