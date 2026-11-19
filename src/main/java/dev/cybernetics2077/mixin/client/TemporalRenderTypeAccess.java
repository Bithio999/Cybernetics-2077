package dev.cybernetics2077.mixin.client;

import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public interface TemporalRenderTypeAccess {
  @Accessor("state")
  RenderType.CompositeState cybernetics$state();
}
