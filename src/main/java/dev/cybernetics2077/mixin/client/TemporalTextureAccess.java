package dev.cybernetics2077.mixin.client;

import net.minecraft.client.renderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderType.CompositeState.class)
public interface TemporalTextureAccess {
  @Accessor("textureState")
  RenderStateShard.EmptyTextureStateShard cybernetics$texture();
}
