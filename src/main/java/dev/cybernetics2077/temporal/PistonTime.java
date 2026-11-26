package dev.cybernetics2077.temporal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.*;

/** A pushed assembly shares its base's clock rather than shearing at a field boundary. */
public final class PistonTime {
  public static double pace(Level level, BlockPos pos, PistonMovingBlockEntity piston) {
    if (piston.isSourcePiston() && !piston.isExtending())
      return TemporalEnvironment.pace(level, pos);
    var back = piston.getDirection().getOpposite();
    for (int distance = 1; distance <= 13; distance++) {
      var at = pos.relative(back, distance);
      if (!level.hasChunkAt(at)) break;
      var state = level.getBlockState(at);
      if (state.getBlock() instanceof PistonBaseBlock
          && state.getValue(PistonBaseBlock.FACING) == piston.getDirection())
        return TemporalEnvironment.pace(level, at);
      if (level.getBlockEntity(at) instanceof PistonMovingBlockEntity source
          && source.isSourcePiston()
          && !source.isExtending()
          && source.getDirection() == piston.getDirection())
        return TemporalEnvironment.pace(level, at);
    }
    return TemporalEnvironment.pace(level, pos);
  }

  private PistonTime() {}
}
