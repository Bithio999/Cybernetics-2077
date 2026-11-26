package dev.cybernetics2077.port;

import net.minecraft.resources.ResourceLocation;

public interface PortPayload {
  record Type<T extends PortPayload>(ResourceLocation id, Class<T> javaType) {}

  Type<? extends PortPayload> type();
}
