package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.cybernetics2077.compat.TacZCompat;
import dev.cybernetics2077.temporal.*;
import net.minecraft.world.entity.*;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.tacz.guns.entity.EntityKineticBullet", remap = false)
public abstract class TacZBulletMixin implements BulletFlightAccess {
  @Shadow private float friction;
  @Shadow private float gravity;

  public float cybernetics$friction() {
    return friction;
  }

  public float cybernetics$gravity() {
    return gravity;
  }

  @WrapMethod(method = "tick")
  private void cybernetics$fractional(Operation<Void> original) {
    BulletTime.tick((Entity) (Object) this, () -> original.call());
  }

  @ModifyExpressionValue(
      method = {"tick", "onBulletTick"},
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lcom/tacz/guns/entity/EntityKineticBullet;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;",
              ordinal = 0))
  private net.minecraft.world.phys.Vec3 cybernetics$distance(net.minecraft.world.phys.Vec3 v) {
    return BulletTime.motion((Entity) (Object) this, v, friction, gravity);
  }

  @WrapOperation(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
  private net.minecraft.world.phys.Vec3 cybernetics$drag(
      net.minecraft.world.phys.Vec3 v,
      double scale,
      Operation<net.minecraft.world.phys.Vec3> original) {
    return original.call(v, BulletTime.drag((Entity) (Object) this, scale));
  }

  @ModifyArg(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lnet/minecraft/world/phys/Vec3;add(DDD)Lnet/minecraft/world/phys/Vec3;"),
      index = 1)
  private double cybernetics$gravity(double g) {
    return BulletTime.gravity((Entity) (Object) this, g);
  }

  @ModifyExpressionValue(
      method = "tick",
      at = @At(value = "FIELD", target = "Lcom/tacz/guns/entity/EntityKineticBullet;tickCount:I"))
  private int cybernetics$age(int age) {
    return BulletTime.nativeAge((Entity) (Object) this, age);
  }

  @Inject(method = "tick", at = @At("HEAD"))
  private void cybernetics$flightStart(CallbackInfo ci) {
    TacZCompat.beginFlight((Entity) (Object) this);
  }

  @Inject(method = "onHitEntity", at = @At("HEAD"))
  private void cybernetics$hit(
      @Coerce Object hit,
      net.minecraft.world.phys.Vec3 start,
      net.minecraft.world.phys.Vec3 end,
      CallbackInfo ci) {
    if (hit instanceof net.minecraft.world.phys.HitResult result)
      TacZCompat.flightHit((Entity) (Object) this, result.getLocation());
  }

  @Inject(method = "onHitBlock", at = @At("HEAD"))
  private void cybernetics$block(
      net.minecraft.world.phys.BlockHitResult hit,
      net.minecraft.world.phys.Vec3 start,
      net.minecraft.world.phys.Vec3 end,
      CallbackInfo ci) {
    if (hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS)
      TacZCompat.flightHit((Entity) (Object) this, hit.getLocation());
  }

  @Inject(
      method = "tick",
      at =
          @At(
              value = "INVOKE",
              target = "Lcom/tacz/guns/entity/EntityKineticBullet;onBulletTick()V",
              shift = At.Shift.AFTER),
      cancellable = true)
  private void cybernetics$bank(CallbackInfo ci) {
    TacZCompat.finishFlight((Entity) (Object) this);
    if (TacZCompat.completeBounce((Entity) (Object) this)) ci.cancel();
  }

  @ModifyVariable(method = "shootFromRotation", at = @At("HEAD"), argsOnly = true)
  private Vector2d cybernetics$spread(Vector2d spread, @Local(argsOnly = true) Entity shooter) {
    return shooter instanceof LivingEntity living
        ? new Vector2d(spread).mul(TacZCompat.spread(living))
        : spread;
  }
}
