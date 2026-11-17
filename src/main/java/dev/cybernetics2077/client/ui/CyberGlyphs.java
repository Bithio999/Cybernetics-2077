package dev.cybernetics2077.client.ui;

import static dev.cybernetics2077.client.ui.Geometry.Point;

import dev.cybernetics2077.cyberware.CyberwareCategory;
import java.util.List;

final class CyberGlyphs {
  static void category(
      CyberCanvas c, CyberwareCategory category, float x, float y, float size, int color) {
    c.g.pose().pushPose();
    c.g.pose().translate(x, y, 0);
    c.g.pose().scale(size / 48, size / 48, 1);
    switch (category) {
      case FRONTAL_CORTEX, NERVOUS_SYSTEM -> {
        c.stroke(
            List.of(
                new Point(11, 34),
                new Point(7, 23),
                new Point(11, 12),
                new Point(20, 7),
                new Point(29, 8),
                new Point(36, 17),
                new Point(35, 28),
                new Point(28, 35),
                new Point(28, 42),
                new Point(16, 42),
                new Point(16, 34)),
            1,
            color);
        c.line(22, 11, 22, 29, 1, color);
        c.line(13, 20, 31, 20, 1, color);
        c.line(13, 27, 29, 27, 1, color);
        if (category == CyberwareCategory.NERVOUS_SYSTEM) {
          c.line(22, 29, 22, 39, 1, color);
          c.line(22, 33, 11, 40, 1, color);
          c.line(22, 33, 34, 40, 1, color);
        }
      }
      case OPERATING_SYSTEM -> {
        c.stroke(
            List.of(new Point(11, 10), new Point(36, 10), new Point(36, 37), new Point(11, 37)),
            1,
            color);
        for (int k = 15; k <= 33; k += 6) {
          c.line(k, 5, k, 10, 1, color);
          c.line(k, 37, k, 43, 1, color);
          c.line(5, k, 11, k, 1, color);
          c.line(36, k, 43, k, 1, color);
        }
        c.stroke(
            List.of(new Point(18, 17), new Point(29, 17), new Point(29, 30), new Point(18, 30)),
            1,
            color);
      }
      case FACE -> {
        c.stroke(
            List.of(
                new Point(5, 24),
                new Point(13, 15),
                new Point(23, 11),
                new Point(35, 16),
                new Point(43, 24),
                new Point(35, 32),
                new Point(24, 36),
                new Point(12, 32)),
            1,
            color);
        c.arc(24, 24, 8, 0, (float) (Math.PI * 2), 1, color);
        c.rect(22, 22, 4, 4, color);
      }
      case SKELETON -> {
        c.arc(24, 10, 5, 0, (float) (Math.PI * 2), 1, color);
        c.line(24, 15, 24, 35, 1.5f, color);
        for (int k = 20; k <= 29; k += 4) {
          c.line(13, k - 2, 24, k, 1, color);
          c.line(24, k, 35, k - 2, 1, color);
        }
        c.line(24, 35, 16, 44, 1, color);
        c.line(24, 35, 32, 44, 1, color);
      }
      case CIRCULATORY_SYSTEM -> {
        c.stroke(
            List.of(
                new Point(24, 42),
                new Point(8, 26),
                new Point(7, 16),
                new Point(14, 10),
                new Point(22, 12),
                new Point(25, 17),
                new Point(31, 10),
                new Point(39, 12),
                new Point(43, 20),
                new Point(39, 30)),
            1.3f,
            color);
        c.line(7, 25, 17, 25, 1, color);
        c.line(17, 25, 21, 18, 1, color);
        c.line(21, 18, 25, 31, 1, color);
        c.line(25, 31, 30, 24, 1, color);
        c.line(30, 24, 40, 24, 1, color);
      }
      case HANDS -> {
        c.stroke(
            List.of(
                new Point(13, 28),
                new Point(10, 20),
                new Point(13, 17),
                new Point(19, 24),
                new Point(19, 8),
                new Point(23, 8),
                new Point(24, 23),
                new Point(27, 5),
                new Point(31, 6),
                new Point(30, 24),
                new Point(35, 10),
                new Point(39, 12),
                new Point(35, 28),
                new Point(33, 39),
                new Point(25, 44),
                new Point(17, 39)),
            1,
            color);
      }
      case ARMS -> {
        c.stroke(
            List.of(
                new Point(15, 6),
                new Point(27, 8),
                new Point(30, 19),
                new Point(24, 28),
                new Point(36, 34),
                new Point(32, 43),
                new Point(14, 34),
                new Point(12, 25),
                new Point(18, 16)),
            1.3f,
            color);
        c.line(17, 27, 27, 21, 1, color);
        c.line(20, 31, 31, 36, 1, color);
      }
      case LEGS -> {
        c.stroke(
            List.of(
                new Point(20, 5),
                new Point(33, 6),
                new Point(30, 24),
                new Point(25, 28),
                new Point(24, 38),
                new Point(36, 40),
                new Point(36, 45),
                new Point(13, 45),
                new Point(16, 27),
                new Point(17, 22)),
            1.2f,
            color);
        c.line(18, 25, 29, 25, 1, color);
        c.line(18, 30, 24, 31, 1, color);
      }
      case INTEGUMENTARY_SYSTEM -> {
        c.stroke(
            List.of(
                new Point(24, 5),
                new Point(41, 13),
                new Point(38, 32),
                new Point(24, 44),
                new Point(10, 32),
                new Point(7, 13)),
            1.2f,
            color);
        c.line(11, 17, 36, 30, 1, color);
        c.line(12, 30, 36, 17, 1, color);
        c.line(24, 7, 24, 40, 1, color);
      }
    }
    c.g.pose().popPose();
  }

  private CyberGlyphs() {}

  public static void quickhack(CyberCanvas c, float x, float y, float size, int color) {
    float z = size / 32;
    c.g.pose().pushPose();
    c.g.pose().translate(x, y, 0);
    c.g.pose().scale(z, z, 1);
    c.stroke(Geometry.panel(3, 3, 26, 26, 5), 1, color);
    for (int k = 0; k < 4; k++) {
      c.line(8 + k * 5, 0, 8 + k * 5, 3, 1, color);
      c.line(8 + k * 5, 29, 8 + k * 5, 32, 1, color);
      c.line(0, 8 + k * 5, 3, 8 + k * 5, 1, color);
      c.line(29, 8 + k * 5, 32, 8 + k * 5, 1, color);
    }
    c.polygon(
        java.util.List.of(
            new Geometry.Point(18, 6),
            new Geometry.Point(10, 17),
            new Geometry.Point(16, 17),
            new Geometry.Point(13, 26),
            new Geometry.Point(23, 13),
            new Geometry.Point(17, 13)),
        color);
    c.g.pose().popPose();
  }
}
