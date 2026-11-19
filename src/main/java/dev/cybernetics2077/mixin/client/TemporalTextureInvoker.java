package dev.cybernetics2077.mixin.client;

import java.util.Optional;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RenderStateShard.EmptyTextureStateShard.class)
public interface TemporalTextureInvoker {
  @Invoker("cutoutTexture")
  Optional<ResourceLocation> cybernetics$location();
}
