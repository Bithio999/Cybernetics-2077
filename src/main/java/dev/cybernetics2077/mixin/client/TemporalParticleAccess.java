package dev.cybernetics2077.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface TemporalParticleAccess {
  @Accessor("x")
  double cybernetics$x();

  @Accessor("y")
  double cybernetics$y();

  @Accessor("z")
  double cybernetics$z();
}
