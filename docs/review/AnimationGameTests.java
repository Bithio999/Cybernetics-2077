package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.temporal.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class AnimationGameTests {
  static PistonMovingBlockEntity moving(GameTestHelper h, BlockPos pos, boolean extending) {
    var state =
        Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, Direction.EAST);
    h.getLevel().setBlock(pos, state, 2);
    var piston =
        new PistonMovingBlockEntity(
            pos, state, Blocks.STONE.defaultBlockState(), Direction.EAST, extending, false);
    h.getLevel().setBlockEntity(piston);
    return piston;
  }

  @GameTest(template = "empty", batch = "animationPiston", timeoutTicks = 80)
  public static void nativePistonProgressIsMonotonicEveryTickForBothModels(GameTestHelper h) {
    for (var model : new Augment[] {Augment.DYNALAR_SANDEVISTAN, Augment.MILITECH_APOGEE}) {
      var p = SandevistanGameTests.runner(h, 5);
      AugmentGameTests.equip(p, model, 5, 0);
      var pos = p.blockPosition().above(2);
      AugmentRuntime.action(p, 13);
      double pace = model == Augment.MILITECH_APOGEE ? .1875 : .625;
      try {
        for (boolean extending : new boolean[] {true, false}) {
          var piston = moving(h, pos, extending);
          float previous = 0;
          int steps = 0;
          while (previous < 1 && steps < 30) {
            PistonMovingBlockEntity.tick(h.getLevel(), pos, piston.getBlockState(), piston);
            steps++;
            float current = piston.getProgress(1);
            h.assertTrue(current > previous, "Piston froze between virtual ticks");
            h.assertTrue(
                Math.abs(current - Math.min(1, previous + .5 * pace)) < 1e-6,
                "Piston skipped/full-stepped its motion");
            float mid = piston.getProgress(.5f);
            h.assertTrue(
                mid >= previous && mid <= current, "Native interpolation reversed/overshot");
            previous = current;
          }
          h.assertTrue(steps == (int) Math.ceil(2 / pace), "Wrong fractional extension duration");
          PistonMovingBlockEntity.tick(h.getLevel(), pos, piston.getBlockState(), piston);
          h.assertTrue(
              h.getLevel().getBlockState(pos).is(Blocks.STONE), "Completed piston did not settle");
        }
      } finally {
        h.getLevel().removeBlock(pos, false);
        SandevistanGameTests.cleanup(p);
      }
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "animationAssembly", timeoutTicks = 80)
  public static void assemblySharesBaseClockAcrossBubbleBoundaryAndResumesNormally(
      GameTestHelper h) {
    var p = IconicGameTests.apogee(h);
    AugmentRuntime.action(p, 13);
    var base = p.blockPosition().east(30);
    var front = base.east(3);
    h.getLevel()
        .setBlock(
            base,
            Blocks.PISTON
                .defaultBlockState()
                .setValue(PistonBaseBlock.FACING, Direction.EAST)
                .setValue(PistonBaseBlock.EXTENDED, true),
            2);
    try {
      var piston = moving(h, front, true);
      h.assertTrue(
          TemporalEnvironment.pace(h.getLevel(), front) == 1,
          "Fixture front should be outside field");
      h.assertTrue(
          Math.abs(PistonTime.pace(h.getLevel(), front, piston) - .1875) < 1e-7,
          "Assembly sheared across field boundary");
      PistonMovingBlockEntity.tick(h.getLevel(), front, piston.getBlockState(), piston);
      float before = piston.getProgress(1);
      Sandevistan.stop(p, AugmentRuntime.now(p));
      PistonMovingBlockEntity.tick(h.getLevel(), front, piston.getBlockState(), piston);
      h.assertTrue(
          Math.abs(piston.getProgress(1) - before - .5) < 1e-6,
          "Leaving field kept slow motion or jumped to completion");
    } finally {
      h.getLevel().removeBlock(front, false);
      h.getLevel().removeBlock(base, false);
      SandevistanGameTests.cleanup(p);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "animationTicker", timeoutTicks = 160)
  public static void actualBoundTickerMovesSlowedPistonOnEveryNativeTick(GameTestHelper h) {
    var p = IconicGameTests.apogee(h);
    var pos = p.blockPosition().above(2);
    var chunk = new net.minecraft.world.level.ChunkPos(pos);
    boolean forced = h.getLevel().getForcedChunks().contains(chunk.toLong());
    h.getLevel().setChunkForced(chunk.x, chunk.z, true);
    h.startSequence()
        .thenWaitUntil(
            () -> h.assertTrue(h.getLevel().isPositionEntityTicking(pos), "Wait for piston chunk"))
        .thenExecute(
            () -> {
              AugmentRuntime.action(p, 13);
              var piston = moving(h, pos, true);
              final float[] previous = {piston.getProgress(1)};
              for (int step = 1; step <= 7; step++) {
                int tick = step;
                h.runAfterDelay(
                    step,
                    () -> {
                      float current = piston.getProgress(1);
                      h.assertTrue(
                          current > previous[0] && current - previous[0] < .11f,
                          "Bound ticker still freezes/jumps at native tick "
                              + tick
                              + ": "
                              + previous[0]
                              + " -> "
                              + current);
                      previous[0] = current;
                      if (tick == 7) {
                        h.getLevel().removeBlock(pos, false);
                        SandevistanGameTests.cleanup(p);
                        if (!forced) h.getLevel().setChunkForced(chunk.x, chunk.z, false);
                        h.succeed();
                      }
                    });
              }
            });
  }

  @GameTest(template = "empty", batch = "animationSlime", timeoutTicks = 80)
  public static void slimePistonKeepsNativePushWithoutFullSpeedLaunch(GameTestHelper h) {
    var p = IconicGameTests.apogee(h);
    AugmentRuntime.action(p, 13);
    var pos = p.blockPosition().above(2);
    var state =
        Blocks.MOVING_PISTON.defaultBlockState().setValue(MovingPistonBlock.FACING, Direction.EAST);
    var piston =
        new PistonMovingBlockEntity(
            pos, state, Blocks.SLIME_BLOCK.defaultBlockState(), Direction.EAST, true, false);
    h.getLevel().setBlock(pos, state, 2);
    h.getLevel().setBlockEntity(piston);
    var pig = net.minecraft.world.entity.EntityType.PIG.create(h.getLevel());
    pig.setPos(pos.getX() + .1, pos.getY(), pos.getZ() + .5);
    pig.setNoAi(true);
    pig.setNoGravity(true);
    h.getLevel().addFreshEntity(pig);
    double before = pig.getX();
    try {
      PistonMovingBlockEntity.tick(h.getLevel(), pos, state, piston);
      h.assertTrue(
          pig.getX() > before && pig.getX() - before < .2,
          "Native contact push missing or excessive");
      h.assertTrue(
          Math.abs(pig.getDeltaMovement().x - .1875) < 1e-7, "Slime launched at full normal speed");
    } finally {
      pig.discard();
      h.getLevel().removeBlock(pos, false);
      SandevistanGameTests.cleanup(p);
    }
    h.succeed();
  }
}
