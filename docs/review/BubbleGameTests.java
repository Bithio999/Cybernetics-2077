package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.AugmentRuntime;
import dev.cybernetics2077.temporal.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.*;
import net.minecraft.world.ticks.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class BubbleGameTests {
  static ServerPlayer runner(GameTestHelper h) {
    var p = SandevistanGameTests.runner(h, 5);
    p.setPos(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(2, 20, 2))));
    AugmentRuntime.action(p, 13);
    return p;
  }

  static <T> LevelTicks<T> scheduler(GameTestHelper h, BlockPos pos, boolean fluid) {
    var ticks = new LevelTicks<T>(chunk -> true, h.getLevel()::getProfiler);
    ((TemporalTickAccess) ticks).cybernetics$bind(h.getLevel(), fluid);
    ticks.addContainer(new net.minecraft.world.level.ChunkPos(pos), new LevelChunkTicks<>());
    return ticks;
  }

  @GameTest(template = "empty", batch = "bubble_delay", timeoutTicks = 80)
  public static void scheduledBlockAndFluidDelaysUseTheLocalClock(GameTestHelper h) {
    var p = runner(h);
    var pos = p.blockPosition();
    long start = h.getLevel().getGameTime();
    LevelTicks<Block> blocks = scheduler(h, pos, false);
    LevelTicks<Fluid> fluids = scheduler(h, pos, true);
    var blockAt = new AtomicInteger(-1);
    var fluidAt = new AtomicInteger(-1);
    blocks.schedule(new ScheduledTick<>(Blocks.OBSERVER, pos, start + 10, TickPriority.HIGH, 0));
    fluids.schedule(new ScheduledTick<>(Fluids.WATER, pos, start + 10, TickPriority.NORMAL, 1));
    for (int i = 1; i <= 22; i++) {
      int step = i;
      h.runAfterDelay(
          i,
          () -> {
            blocks.tick(h.getLevel().getGameTime(), 100, (at, type) -> blockAt.set(step));
            fluids.tick(h.getLevel().getGameTime(), 100, (at, type) -> fluidAt.set(step));
            if (step == 22) {
              try {
                h.assertTrue(
                    blockAt.get() >= 15 && blockAt.get() <= 17,
                    "Block timer not slowed: " + blockAt);
                h.assertTrue(
                    fluidAt.get() >= 15 && fluidAt.get() <= 17,
                    "Fluid timer not slowed: " + fluidAt);
                h.assertTrue(
                    blocks.count() == 0 && fluids.count() == 0,
                    "Completed tick duplicated/lost cleanup");
              } finally {
                SandevistanGameTests.cleanup(p);
              }
              h.succeed();
            }
          });
    }
  }

  @GameTest(template = "empty", batch = "bubble_entry", timeoutTicks = 80)
  public static void enteringBubbleSlowsAnAlreadyQueuedTick(GameTestHelper h) {
    var p = runner(h);
    Sandevistan.stop(p, AugmentRuntime.now(p));
    var pos = p.blockPosition();
    long start = h.getLevel().getGameTime();
    LevelTicks<Block> ticks = scheduler(h, pos, false);
    var fired = new AtomicInteger(-1);
    ticks.schedule(new ScheduledTick<>(Blocks.OBSERVER, pos, start + 12, TickPriority.NORMAL, 0));
    for (int i = 1; i <= 25; i++) {
      int step = i;
      h.runAfterDelay(
          i,
          () -> {
            if (step == 4) {
              var s = AugmentRuntime.state(p);
              s.sandyCharge = 1;
              s.sandyRearm = 0;
              AugmentRuntime.action(p, 13);
            }
            ticks.tick(h.getLevel().getGameTime(), 100, (at, type) -> fired.set(step));
            if (step == 25) {
              try {
                h.assertTrue(
                    fired.get() >= 16 && fired.get() <= 19,
                    "Pre-existing timer ignored entry: " + fired);
              } finally {
                SandevistanGameTests.cleanup(p);
              }
              h.succeed();
            }
          });
    }
  }

  @GameTest(template = "empty", batch = "bubble_exit", timeoutTicks = 80)
  public static void leavingBubbleResumesRemainingTimeWithoutAnUpdateBurst(GameTestHelper h) {
    var p = runner(h);
    var pos = p.blockPosition();
    long start = h.getLevel().getGameTime();
    LevelTicks<Block> ticks = scheduler(h, pos, false);
    var fired = new AtomicInteger(-1);
    var calls = new AtomicInteger();
    ticks.schedule(new ScheduledTick<>(Blocks.OBSERVER, pos, start + 20, TickPriority.NORMAL, 0));
    for (int i = 1; i <= 32; i++) {
      int step = i;
      h.runAfterDelay(
          i,
          () -> {
            if (step == 9) Sandevistan.stop(p, AugmentRuntime.now(p));
            ticks.tick(
                h.getLevel().getGameTime(),
                100,
                (at, type) -> {
                  fired.set(step);
                  calls.incrementAndGet();
                });
            if (step == 32) {
              try {
                h.assertTrue(
                    fired.get() >= 22 && fired.get() <= 25,
                    "Exit discarded remaining delay: " + fired);
                h.assertTrue(calls.get() == 1, "Deferred tick replayed more than once");
              } finally {
                SandevistanGameTests.cleanup(p);
              }
              h.succeed();
            }
          });
    }
  }

  @GameTest(template = "empty", batch = "bubble_persist", timeoutTicks = 80)
  public static void timerMetadataSurvivesNbtAndClearAreaCancelsIt(GameTestHelper h)
      throws Exception {
    var p = runner(h);
    var pos = p.blockPosition();
    long start = h.getLevel().getGameTime();
    LevelTicks<Block> ticks = scheduler(h, pos, false);
    var fired = new AtomicInteger(-1);
    ticks.schedule(new ScheduledTick<>(Blocks.OBSERVER, pos, start + 10, TickPriority.NORMAL, 0));
    for (int i = 1; i <= 23; i++) {
      int step = i;
      h.runAfterDelay(
          i,
          () -> {
            if (step == 5)
              try {
                var saved =
                    TemporalScheduled.get(h.getLevel())
                        .save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
                var load =
                    TemporalScheduled.class.getDeclaredMethod(
                        "load", net.minecraft.nbt.CompoundTag.class, HolderLookup.Provider.class);
                load.setAccessible(true);
                h.getLevel()
                    .getDataStorage()
                    .set(
                        "cybernetics2077_local_ticks",
                        (TemporalScheduled)
                            load.invoke(null, saved, h.getLevel().registryAccess()));
              } catch (Exception ex) {
                throw new IllegalStateException(ex);
              }
            ticks.tick(h.getLevel().getGameTime(), 100, (at, type) -> fired.set(step));
            if (step == 23) {
              try {
                h.assertTrue(
                    fired.get() >= 15 && fired.get() <= 17,
                    "Saved timer lost local delay: " + fired);
                ticks.schedule(
                    new ScheduledTick<>(
                        Blocks.OBSERVER,
                        pos,
                        h.getLevel().getGameTime() + 20,
                        TickPriority.NORMAL,
                        0));
                ticks.clearArea(new BoundingBox(pos));
                var saved =
                    TemporalScheduled.get(h.getLevel())
                        .save(new net.minecraft.nbt.CompoundTag(), h.getLevel().registryAccess());
                h.assertTrue(
                    saved.getList("timers", 10).stream()
                        .noneMatch(
                            raw ->
                                ((net.minecraft.nbt.CompoundTag) raw).getLong("pos")
                                    == pos.asLong()),
                    "Clear left stale time metadata at the cleared position");
              } finally {
                SandevistanGameTests.cleanup(p);
              }
              h.succeed();
            }
          });
    }
  }

  @GameTest(template = "empty", batch = "bubble_boundary", timeoutTicks = 80)
  public static void rangeOverlapAndRandomUpdatesDoNotMultiplySlowing(GameTestHelper h) {
    var a = runner(h);
    var b = runner(h);
    try {
      b.setPos(a.position().add(0, 0, 2));
      Sandevistan.invalidate(a.level());
      h.assertTrue(
          TemporalEnvironment.pace(a.level(), a.blockPosition()) == .625,
          "Fields multiplied or absent");
      h.assertTrue(
          TemporalEnvironment.pace(a.level(), a.blockPosition().above(80)) == 1,
          "Field leaked beyond radius");
      int n = 0;
      var random = net.minecraft.util.RandomSource.create(81);
      for (int i = 0; i < 20000; i++)
        if (TemporalEnvironment.randomStep(a.level(), a.blockPosition(), random)) n++;
      h.assertTrue(n > 12100 && n < 12900, "Random tick frequency not paced: " + n);
      double inner =
          Sandevistan.environmentPace(a.level(), a.getX(), a.getY(), a.getZ() + 27, true);
      double edge = Sandevistan.environmentPace(a.level(), a.getX(), a.getY(), a.getZ() - 31, true);
      h.assertTrue(inner < edge && edge < 1, "Visual field edge is not blended");
    } finally {
      SandevistanGameTests.cleanup(a, b);
    }
    h.succeed();
  }

  @GameTest(template = "empty", batch = "bubble_machines", timeoutTicks = 160)
  public static void actualFurnaceTickersAndWorldClocksRemainConsistent(GameTestHelper h) {
    var p = runner(h);
    var inside = p.blockPosition();
    var outside = inside.above(80);
    var chunk = new net.minecraft.world.level.ChunkPos(inside);
    boolean forced = h.getLevel().getForcedChunks().contains(chunk.toLong());
    h.getLevel().setChunkForced(chunk.x, chunk.z, true);
    h.startSequence()
        .thenWaitUntil(
            () ->
                h.assertTrue(
                    h.getLevel().isPositionEntityTicking(inside), "Wait for machine chunk"))
        .thenExecute(
            () -> {
              for (var pos : java.util.List.of(inside, outside)) {
                h.getLevel().setBlockAndUpdate(pos, Blocks.FURNACE.defaultBlockState());
                var furnace =
                    (net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)
                        h.getLevel().getBlockEntity(pos);
                furnace.setItem(
                    0,
                    new net.minecraft.world.item.ItemStack(
                        net.minecraft.world.item.Items.IRON_ORE, 8));
                furnace.setItem(
                    1,
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 8));
              }
              long time = h.getLevel().getGameTime(), sun = h.getLevel().getDayTime();
              boolean daylight =
                  h.getLevel()
                      .getGameRules()
                      .getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
              h.runAfterDelay(
                  48,
                  () -> {
                    try {
                      var slow =
                          h.getLevel()
                              .getBlockEntity(inside)
                              .saveWithoutMetadata(h.getLevel().registryAccess());
                      var normal =
                          h.getLevel()
                              .getBlockEntity(outside)
                              .saveWithoutMetadata(h.getLevel().registryAccess());
                      int s = slow.getShort("CookTime"), n = normal.getShort("CookTime");
                      h.assertTrue(
                          n > 35 && s > 20 && s < n * .8 && s > n * .5,
                          "Native furnace timers not paced: " + s + "/" + n);
                      long elapsed = h.getLevel().getGameTime() - time;
                      h.assertTrue(elapsed >= 47 && elapsed <= 50, "World game clock was slowed");
                      if (daylight)
                        h.assertTrue(
                            h.getLevel().getDayTime() - sun == elapsed, "Sun clock changed");
                    } finally {
                      SandevistanGameTests.cleanup(p);
                      h.getLevel().removeBlock(inside, false);
                      h.getLevel().removeBlock(outside, false);
                      if (!forced) h.getLevel().setChunkForced(chunk.x, chunk.z, false);
                    }
                    h.succeed();
                  });
            });
  }

  @GameTest(template = "empty", batch = "bubble_copy", timeoutTicks = 100)
  public static void clonedScheduledTicksRetainTheirUnfinishedLocalDelay(GameTestHelper h) {
    var p = runner(h);
    var pos = p.blockPosition();
    var dest = pos.offset(1, 0, 0);
    LevelTicks<Block> ticks = scheduler(h, pos, false);
    var sourceAt = new AtomicInteger(-1);
    var destAt = new AtomicInteger(-1);
    ticks.schedule(
        new ScheduledTick<>(
            Blocks.OBSERVER, pos, h.getLevel().getGameTime() + 20, TickPriority.HIGH, 7));
    for (int i = 1; i <= 38; i++) {
      int step = i;
      h.runAfterDelay(
          i,
          () -> {
            if (step == 8) ticks.copyArea(new BoundingBox(pos), new Vec3i(1, 0, 0));
            ticks.tick(
                h.getLevel().getGameTime(),
                100,
                (at, type) -> {
                  if (at.equals(pos)) sourceAt.set(step);
                  else if (at.equals(dest)) destAt.set(step);
                });
            if (step == 38) {
              try {
                h.assertTrue(
                    sourceAt.get() >= 31 && sourceAt.get() <= 34,
                    "Source delay changed: " + sourceAt);
                h.assertTrue(
                    Math.abs(sourceAt.get() - destAt.get()) <= 1,
                    "Clone lost virtual remaining time: " + destAt);
              } finally {
                SandevistanGameTests.cleanup(p);
              }
              h.succeed();
            }
          });
    }
  }

  @GameTest(template = "empty", batch = "bubble_water", timeoutTicks = 100)
  public static void nativeWaterFlowSlowsInsideAndResumes(GameTestHelper h) {
    var p = runner(h);
    var inside = p.blockPosition();
    var outside = inside.above(80);
    for (var pos : java.util.List.of(inside, outside)) {
      for (int x = -2; x <= 2; x++)
        for (int z = -2; z <= 2; z++)
          h.getLevel().setBlockAndUpdate(pos.offset(x, -1, z), Blocks.STONE.defaultBlockState());
      h.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
      h.getLevel().scheduleTick(pos, Fluids.WATER, 5);
    }
    h.runAfterDelay(
        7,
        () -> {
          h.assertTrue(
              h.getLevel().getFluidState(outside.east()).isSource()
                  || !h.getLevel().getFluidState(outside.east()).isEmpty(),
              "Control water never flowed");
          h.assertTrue(
              h.getLevel().getFluidState(inside.east()).isEmpty(),
              "Water ignored local scheduled time");
        });
    h.runAfterDelay(
        20,
        () -> {
          try {
            h.assertTrue(
                !h.getLevel().getFluidState(inside.east()).isEmpty(),
                "Delayed water never resumed");
          } finally {
            SandevistanGameTests.cleanup(p);
          }
          h.succeed();
        });
  }

  @GameTest(template = "empty", batch = "bubble_redstone", timeoutTicks = 100)
  public static void nativeRepeaterDelayUsesTheSameLocalClock(GameTestHelper h) {
    var p = runner(h);
    var inside = p.blockPosition();
    var outside = inside.above(80);
    for (var pos : java.util.List.of(inside, outside)) {
      h.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
      h.getLevel()
          .setBlockAndUpdate(
              pos,
              Blocks.REPEATER
                  .defaultBlockState()
                  .setValue(RepeaterBlock.FACING, Direction.NORTH)
                  .setValue(RepeaterBlock.DELAY, 4));
      h.getLevel().setBlockAndUpdate(pos.north(), Blocks.REDSTONE_BLOCK.defaultBlockState());
    }
    h.runAfterDelay(
        10,
        () -> {
          h.assertTrue(
              h.getLevel().getBlockState(outside).getValue(RepeaterBlock.POWERED),
              "Control repeater never powered");
          h.assertTrue(
              !h.getLevel().getBlockState(inside).getValue(RepeaterBlock.POWERED),
              "Repeater timer ignored bubble");
        });
    h.runAfterDelay(
        18,
        () -> {
          try {
            h.assertTrue(
                h.getLevel().getBlockState(inside).getValue(RepeaterBlock.POWERED),
                "Delayed repeater did not power");
          } finally {
            SandevistanGameTests.cleanup(p);
          }
          h.succeed();
        });
  }
}
