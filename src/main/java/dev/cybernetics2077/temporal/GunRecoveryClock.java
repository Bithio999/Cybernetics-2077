package dev.cybernetics2077.temporal;

/** Completed mechanical work survives changes of pace; timestamps remain on TaCZ's clock. */
public final class GunRecoveryClock {
  private long shot = Long.MIN_VALUE, at;
  private double work, pace = 1;

  public void reset() {
    shot = Long.MIN_VALUE;
  }

  public long remaining(long timestamp, long now, long interval, double nextPace, int tolerance) {
    nextPace = Math.max(TemporalRules.MIN_PACE, nextPace);
    if (shot != timestamp) {
      shot = timestamp;
      at = timestamp;
      work = 0;
      pace = nextPace;
    }
    // An older network query may inspect the clock but must not rewind/recount its progress.
    double progress = work;
    if (now >= at) {
      work += (now - at) * pace;
      at = now;
      pace = nextPace;
      progress = work;
    } else progress = work - (at - now) * pace;
    return Math.max(0, Math.round((interval - progress) / nextPace) - tolerance);
  }
}
