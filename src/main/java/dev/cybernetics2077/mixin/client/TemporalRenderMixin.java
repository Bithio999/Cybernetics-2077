package dev.cybernetics2077.mixin.client;

import dev.cybernetics2077.temporal.Sandevistan;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(LevelRenderer.class)
public abstract class TemporalRenderMixin {
  @ModifyVariable(method = "renderEntity", at = @At("HEAD"), argsOnly = true)
  private float cybernetics$subtick(float partial, Entity entity) {
    return Sandevistan.renderPartial(entity, partial);
  }
}
