package dev.cybernetics2077.temporal;

/** Fractional time without accumulated catch-up work when a field moves or disappears. */
public final class LocalTime {
  public static final class Timer {
    public double remaining;
    public long last;

    public Timer(double remaining, long now) {
      this.remaining = remaining;
      last = now;
    }

    public void advance(long now, double pace) {
      if (now > last) remaining -= (now - last) * Math.clamp(pace, .05, 1);
      last = now;
    }
  }

  public static final class Visual {
    public double time, last;

    public Visual(double now) {
      time = last = now;
    }

    public double advance(double now, double pace) {
      if (now >= last) time += (now - last) * Math.clamp(pace, .05, 1);
      else time = now;
      last = now;
      return time;
    }
  }

  private LocalTime() {}
}
