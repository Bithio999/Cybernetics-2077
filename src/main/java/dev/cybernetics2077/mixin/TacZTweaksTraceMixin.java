package dev.cybernetics2077.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.Coerce;

/** Tweaks stores active trace context in static fields shared by both integrated-server threads. */
@Pseudo
@Mixin(targets = "me.muksc.tacztweaks.feature.raytracer.BulletRayTracer", remap = false)
public abstract class TacZTweaksTraceMixin {
  @Unique private static final Object cybernetics$traceLock = new Object();

  @WrapMethod(method = "rayTraceBlocks")
  private static BlockHitResult cybernetics$isolatedTrace(
      @Coerce Object args, @Coerce Object callback, Operation<BlockHitResult> original) {
    synchronized (cybernetics$traceLock) {
      return original.call(args, callback);
    }
  }
}
