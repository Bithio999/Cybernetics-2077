package dev.cybernetics2077.client.ui;

/** Lightweight clinic/fixer surfaces; the full cyberware viewer keeps its own rendering. */
final class ServiceStyle {
  static final int BACK = 0xfd090f14,
      INK = 0xffd8e5e6,
      MUTED = 0xff81969e,
      CYAN = 0xff70dce4,
      LINE = 0xff2b4048,
      GOLD = 0xffe1b56b;

  static void frame(CyberCanvas c, int w, int h, int accent) {
    c.shape(Geometry.panel(0, 0, w, h, 4), BACK, LINE);
    c.line(10, 23, w - 10, 23, .5f, LINE);
    c.rect(10, 7, 2, 10, accent);
  }

  static void slot(CyberCanvas c, float x, float y, boolean hotbar) {
    c.rect(x, y, 18, 18, hotbar ? 0xff132129 : 0xff0b161c);
    c.line(x + 1, y + 17, x + 17, y + 17, .5f, hotbar ? 0xff597783 : LINE);
  }

  static void surface(CyberCanvas c, float x, float y, float w, float h) {
    c.rect(x, y, w, h, 0x70121e24);
    c.line(x, y + h, w + x, y + h, .5f, LINE);
  }

  private ServiceStyle() {}
}
