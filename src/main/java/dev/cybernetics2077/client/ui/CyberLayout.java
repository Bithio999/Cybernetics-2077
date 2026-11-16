package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.cyberware.CyberwareCategory;
import java.util.*;

/** One reference canvas; all drawing and hitboxes use this same transform. */
public record CyberLayout(double scale, double left, double top) {
  public static final int WIDTH = 1920, HEIGHT = 1080, SOCKET = 96;

  public record Row(CyberwareCategory category, int x, int y, boolean left) {}

  public record Socket(int index, CyberwareCategory category, int ordinal, int x, int y) {}

  public static final List<Row> ROWS =
      List.of(
          new Row(CyberwareCategory.FRONTAL_CORTEX, 530, 164, true),
          new Row(CyberwareCategory.OPERATING_SYSTEM, 1164, 164, false),
          new Row(CyberwareCategory.ARMS, 706, 322, true),
          new Row(CyberwareCategory.FACE, 1228, 322, false),
          new Row(CyberwareCategory.SKELETON, 386, 480, true),
          new Row(CyberwareCategory.HANDS, 1308, 480, false),
          new Row(CyberwareCategory.NERVOUS_SYSTEM, 530, 638, true),
          new Row(CyberwareCategory.CIRCULATORY_SYSTEM, 1164, 638, false),
          new Row(CyberwareCategory.INTEGUMENTARY_SYSTEM, 530, 796, true),
          new Row(CyberwareCategory.LEGS, 1164, 796, false));
  public static final List<Socket> SOCKETS =
      ROWS.stream()
          .flatMap(
              row ->
                  java.util.stream.IntStream.range(0, row.category.sockets)
                      .mapToObj(
                          n ->
                              new Socket(
                                  row.category.firstSlot() + n,
                                  row.category,
                                  n,
                                  row.x + n * 112,
                                  row.y)))
          .toList();

  public static CyberLayout fit(int width, int height) {
    double scale = Math.min(width / (double) WIDTH, height / (double) HEIGHT);
    return new CyberLayout(scale, (width - WIDTH * scale) / 2, (height - HEIGHT * scale) / 2);
  }

  public double virtualX(double x) {
    return (x - left) / scale;
  }

  public double virtualY(double y) {
    return (y - top) / scale;
  }

  public int screenX(double x) {
    return (int) Math.round(left + x * scale);
  }

  public int screenY(double y) {
    return (int) Math.round(top + y * scale);
  }

  public int size(double value) {
    return Math.max(1, (int) Math.round(value * scale));
  }
}
