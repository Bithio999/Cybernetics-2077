package dev.cybernetics2077.temporal;

import net.minecraft.server.level.ServerLevel;

public interface TemporalTickAccess {
  void cybernetics$bind(ServerLevel level, boolean fluid);

  ServerLevel cybernetics$level();

  boolean cybernetics$fluid();
}
