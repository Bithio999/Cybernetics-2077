package dev.cybernetics2077.review;

import java.nio.file.Files;
import java.util.Arrays;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Test-only frame pacing evidence, with no readback or disk writes during combat. */
@EventBusSubscriber(modid = "cybernetics2077", value = Dist.CLIENT)
public final class RecordingFrames {
  public static boolean quarter;

  @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
  public static void hideHands(net.neoforged.neoforge.client.event.RenderHandEvent event) {
    if (quarter) event.setCanceled(true);
  }

  private static final double[] times = new double[16384];
  private static int count;
  private static long previous;

  @SubscribeEvent
  public static void frame(RenderFrameEvent.Post event) {
    if (!Boolean.getBoolean("cybernetics2077.recording")) return;
    int tick = CyberwareClientReview.contentTicks;
    if (tick < 220 || tick > 502) {
      previous = 0;
      return;
    }
    long now = System.nanoTime();
    if (previous != 0 && count < times.length) times[count++] = (now - previous) / 1000000.0;
    previous = now;
  }

  static void finish() throws Exception {
    var sorted = Arrays.copyOf(times, count);
    Arrays.sort(sorted);
    if (count == 0) throw new IllegalStateException("No rendered combat frames recorded");
    double mean = Arrays.stream(sorted).average().orElseThrow();
    String result =
        String.format(
            java.util.Locale.ROOT,
            "Combat render frames: %d%nMean frame time: %.3f ms (%.1f FPS)%nMedian: %.3f ms%nP95:"
                + " %.3f ms%nP99: %.3f ms%n",
            count,
            mean,
            1000 / mean,
            sorted[count / 2],
            sorted[(int) (count * .95)],
            sorted[(int) (count * .99)]);
    Files.writeString(CyberwareClientReview.OUT.resolve("recording-frame-times.txt"), result);
    System.out.print(result);
  }
}
