package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.cyberware.CyberwareCategory;
import java.util.*;
import net.minecraft.world.phys.Vec3;

/** Original procedural anatomy: beveled shells, separated bones, optics and neural conduits. */
final class DiagnosticModel {
  record Face(
      Vec3 a,
      Vec3 b,
      Vec3 c,
      Vec3 d,
      Vec3 normal,
      int color,
      CyberwareCategory region,
      boolean shell) {}

  private final List<Face> faces = new ArrayList<>();
  final List<Face> core, shell;

  DiagnosticModel() {
    var skull = CyberwareCategory.FRONTAL_CORTEX;
    var bone = CyberwareCategory.SKELETON;
    var skin = CyberwareCategory.INTEGUMENTARY_SYSTEM;
    int ivory = 0xffd1b3bc, wire = 0xff7ed2dd, glass = 0x3ac6859b;
    box(0, -4, 0, 7.8, 7.8, 7.4, .85, glass, skin, true);
    box(0, -.55, -.1, 5.6, .85, 5.3, .3, 0xbba77d91, bone, false);
    for (int side : new int[] {-1, 1}) {
      sphere(side * 1.43, -5.7, .1, 1.55, 1.6, 2.3, 0xffc5a8be, skull, true);
      rod(
          v(side * .32, -7.1, 0),
          v(side * .32, -3.5, 0),
          .13,
          wire,
          CyberwareCategory.NERVOUS_SYSTEM);
      // An actual optic assembly: concentric lens rings, glass lens and rear connector.
      ring(side * 1.8, -3.65, -3.66, 1.0, .23, 0xffe3bfa0, CyberwareCategory.FACE);
      ring(side * 1.8, -3.65, -3.86, .66, .10, 0xffa2f0ee, CyberwareCategory.FACE);
      sphere(side * 1.8, -3.65, -3.78, .48, .48, .17, 0xff61dce5, CyberwareCategory.FACE, false);
      rod(v(side * 1.8, -3.65, -2.8), v(side * .55, -4.15, 1), .12, wire, CyberwareCategory.FACE);
      rod(v(side * 2.9, -2.5, -2.2), v(side * 1.8, -.8, -2.1), .2, ivory, bone);
      box(side * 3.65, -3.1, 0, .6, 2.1, 1.2, .2, 0xffac9aad, CyberwareCategory.FACE, false);
    }
    box(0, -1.8, -3.45, 2.4, .3, .25, .1, 0xffedb2ba, CyberwareCategory.FACE, false);
    rod(v(0, -.2, 0), v(0, 1, 0), .55, ivory, bone);
    box(0, 5.5, 0, 8, 10.8, 4.1, .65, glass, skin, true);
    // Separated vertebrae and neural cord remain visible through the outer shell.
    rod(v(0, .3, 1.35), v(0, 11.3, 1.35), .14, 0xfff4c973, CyberwareCategory.NERVOUS_SYSTEM);
    for (int i = 0; i < 10; i++) {
      box(0, .65 + i * 1.02, 1.35, .9, .68, .85, .16, ivory, bone, false);
      for (int side : new int[] {-1, 1})
        rod(
            v(0, .7 + i * 1.02, 1.3),
            v(side * 1.75, 1 + i * 1.02, 1),
            .07,
            wire,
            CyberwareCategory.NERVOUS_SYSTEM);
    }
    for (int side : new int[] {-1, 1}) {
      rod(v(0, .65, -1), v(side * 3.75, .6, -.35), .20, ivory, bone);
      for (int rib = 0; rib < 6; rib++) {
        double y = 1.75 + rib * 1.02;
        rod(v(side * .18, y + .3, -1.94), v(side * 2.6, y, -1.8), .13, ivory, bone);
        rod(v(side * 2.6, y, -1.8), v(side * 3.48, y - .45, -.2), .13, ivory, bone);
        rod(v(side * 3.48, y - .45, -.2), v(side * 1.0, y - .7, 1.4), .11, ivory, bone);
      }
      rod(v(side * .5, 9.1, -1.1), v(side * 3.45, 10.4, -.2), .35, ivory, bone);
      rod(v(side * 3.45, 10.4, -.2), v(side * 1.5, 11.3, -.8), .35, ivory, bone);
    }
    box(0, 4, -1.97, .35, 5.7, .3, .09, ivory, bone, false);
    sphere(1.18, 4.4, -.35, .85, 1.2, .75, 0xffed737e, CyberwareCategory.CIRCULATORY_SYSTEM, false);
    for (int side : new int[] {-1, 1}) {
      rod(
          v(side * .5, 1.7, -.2),
          v(side * 1.4, 8.5, -.4),
          .11,
          0xffdf717f,
          CyberwareCategory.CIRCULATORY_SYSTEM);
      box(
          side * 2,
          3.4,
          -.3,
          1.7,
          3.6,
          1.3,
          .3,
          0x4882b3c0,
          CyberwareCategory.CIRCULATORY_SYSTEM,
          true);
    }
    box(-1.75, 2.5, -1.35, 1.4, 1.9, .7, .2, 0xffd5b77a, CyberwareCategory.OPERATING_SYSTEM, false);
    for (int i = 0; i < 4; i++)
      box(
          -1.75,
          1.95 + i * .36,
          -1.75,
          1,
          .1,
          .1,
          .025,
          wire,
          CyberwareCategory.OPERATING_SYSTEM,
          false);
    for (int side : new int[] {-1, 1}) {
      var arm = CyberwareCategory.ARMS;
      var leg = CyberwareCategory.LEGS;
      var hand = CyberwareCategory.HANDS;
      double x = side * 5.75;
      sphere(x, 1.15, 0, 1.3, 1.3, 1.3, ivory, arm, false);
      box(x, 3.5, 0, 3.35, 5.0, 3.4, .55, glass, skin, true);
      rod(v(x, 1.5, 0), v(x, 5.5, 0), .32, ivory, arm);
      sphere(x, 6.2, 0, .8, .8, .8, 0xffa7b9c9, arm, false);
      box(x, 8.25, 0, 3.15, 4.0, 3.15, .45, glass, skin, true);
      for (int s : new int[] {-1, 1})
        rod(v(x + s * .43, 6.4, 0), v(x + s * .55, 10.15, 0), .21, ivory, arm);
      rod(
          v(x - side * .8, 1.2, .5),
          v(x - side * .8, 10.4, .5),
          .10,
          wire,
          CyberwareCategory.NERVOUS_SYSTEM);
      box(x, 10.9, 0, 2.65, 1.75, 2.25, .3, 0xc9c18b9e, hand, false);
      for (int f = 0; f < 4; f++) {
        double fx = x - 1.0 + f * .66;
        rod(
            v(fx, 11.5, -.4),
            v(fx, 12.35 + (.25 - Math.abs(f - 1.5) * .15), -.45),
            .18,
            ivory,
            hand);
        rod(v(fx, 12.35, -.45), v(fx, 13.12 - Math.abs(f - 1.5) * .18, -.70), .16, ivory, hand);
      }
      rod(v(x - side * 1.3, 10.7, -.3), v(x - side * 1.9, 11.7, -.5), .20, ivory, hand);
      double lx = side * 2;
      box(lx, 14.75, 0, 3.7, 5.8, 3.7, .55, 0x3a699cb5, skin, true);
      rod(v(lx, 12, 0), v(lx, 17.25, 0), .43, ivory, leg);
      sphere(lx, 17.6, -.4, .95, .8, .9, 0xffb5cbd7, leg, false);
      box(lx, 20.4, 0, 3.6, 5.0, 3.5, .4, 0x3a6899b9, skin, true);
      for (int s : new int[] {-1, 1})
        rod(v(lx + s * .4, 18, 0), v(lx + s * .5, 22.6, 0), .22, ivory, leg);
      rod(
          v(lx + side * .8, 12, .6),
          v(lx + side * .8, 22.6, .6),
          .10,
          wire,
          CyberwareCategory.NERVOUS_SYSTEM);
      box(lx, 23.3, -.55, 3.5, 1.2, 4.6, .35, 0xa579b3cb, leg, false);
      for (int f = 0; f < 4; f++)
        box(lx - 1 + f * .66, 23.3, -2.5, .42, .4, 1.0, .12, ivory, leg, false);
    }
    core = faces.stream().filter(f -> !f.shell).toList();
    shell = faces.stream().filter(f -> f.shell).toList();
  }

  static Vec3 v(double x, double y, double z) {
    return new Vec3(x, y, z);
  }

  private void face(List<Vec3> p, Vec3 center, int color, CyberwareCategory region, boolean shell) {
    Vec3 normal =
        p.get(1).subtract(p.getFirst()).cross(p.get(2).subtract(p.getFirst())).normalize();
    if (normal.dot(p.getFirst().subtract(center)) < 0) {
      p = new ArrayList<>(p);
      Collections.reverse(p);
      normal = normal.scale(-1);
    }
    for (int i = 1; i < p.size() - 1; i++)
      faces.add(
          new Face(
              p.getFirst().scale(1 / 16d),
              p.get(i).scale(1 / 16d),
              p.get(i + 1).scale(1 / 16d),
              p.get(i + 1).scale(1 / 16d),
              normal,
              color,
              region,
              shell));
  }

  private void box(
      double x,
      double y,
      double z,
      double w,
      double h,
      double d,
      double b,
      int color,
      CyberwareCategory region,
      boolean shell) {
    double[] c = {x, y, z}, half = {w / 2, h / 2, d / 2};
    b = Math.min(b, Math.min(w, Math.min(h, d)) * .45);
    var center = v(x, y, z);
    // Six octagonal faces, twelve chamfer strips and eight triangular corners.
    for (int axis = 0; axis < 3; axis++)
      for (int sign : new int[] {-1, 1}) {
        int u = (axis + 1) % 3, t = (axis + 2) % 3;
        List<Vec3> p = new ArrayList<>();
        double[][] loop = {
          {-1, -1, 1, 0},
          {1, -1, -1, 0},
          {1, -1, 0, 1},
          {1, 1, 0, -1},
          {1, 1, -1, 0},
          {-1, 1, 1, 0},
          {-1, 1, 0, -1},
          {-1, -1, 0, 1}
        };
        for (var q : loop) {
          double[] a = c.clone();
          a[axis] += sign * half[axis];
          a[u] += q[0] * half[u] + q[2] * b;
          a[t] += q[1] * half[t] + q[3] * b;
          p.add(v(a[0], a[1], a[2]));
        }
        face(p, center, color, region, shell);
        for (int s : new int[] {-1, 1}) {
          List<Vec3> edge = new ArrayList<>();
          for (int end : new int[] {-1, 1})
            for (int k : new int[] {0, 1}) {
              double[] a = c.clone();
              a[axis] += sign * (half[axis] - (k == 0 ? 0 : b));
              a[u] += s * (half[u] - (k == 0 ? b : 0));
              a[t] += end * (half[t] - b);
              edge.add(v(a[0], a[1], a[2]));
            }
          face(
              List.of(edge.get(0), edge.get(1), edge.get(3), edge.get(2)),
              center,
              color,
              region,
              shell);
        }
      }
    for (int sx : new int[] {-1, 1})
      for (int sy : new int[] {-1, 1})
        for (int sz : new int[] {-1, 1}) {
          int[] s = {sx, sy, sz};
          List<Vec3> p = new ArrayList<>();
          for (int axis = 0; axis < 3; axis++) {
            double[] a = c.clone();
            for (int i = 0; i < 3; i++) a[i] += s[i] * (half[i] - (axis == i ? 0 : b));
            p.add(v(a[0], a[1], a[2]));
          }
          face(p, center, color, region, shell);
        }
  }

  private void rod(Vec3 a, Vec3 b, double radius, int color, CyberwareCategory region) {
    var dir = b.subtract(a).normalize();
    var u = dir.cross(Math.abs(dir.y) > .9 ? v(1, 0, 0) : v(0, 1, 0)).normalize();
    var w = dir.cross(u);
    var center = a.add(b).scale(.5);
    for (int k = 0; k < 12; k++) {
      double t = k * Math.PI / 6, n = (k + 1) * Math.PI / 6;
      var p = u.scale(Math.cos(t) * radius).add(w.scale(Math.sin(t) * radius));
      var q = u.scale(Math.cos(n) * radius).add(w.scale(Math.sin(n) * radius));
      face(List.of(a.add(p), b.add(p), b.add(q), a.add(q)), center, color, region, false);
      face(List.of(a, a.add(p), a.add(q)), center, color, region, false);
      face(List.of(b, b.add(q), b.add(p)), center, color, region, false);
    }
  }

  private void sphere(
      double x,
      double y,
      double z,
      double rx,
      double ry,
      double rz,
      int color,
      CyberwareCategory region,
      boolean brain) {
    var center = v(x, y, z);
    int latitude = brain ? 16 : 8, longitude = brain ? 32 : 16;
    for (int lat = 0; lat < latitude; lat++)
      for (int lon = 0; lon < longitude; lon++) {
        List<Vec3> p = new ArrayList<>();
        for (int[] q : new int[][] {{0, 0}, {1, 0}, {1, 1}, {0, 1}}) {
          double a = (lat + q[0]) * Math.PI / latitude, t = (lon + q[1]) * Math.PI * 2 / longitude;
          double ripple = brain ? 1 + .065 * Math.sin(t * 5 + a * 9) : 1;
          p.add(
              center.add(
                  Math.sin(a) * Math.cos(t) * rx * ripple,
                  Math.cos(a) * ry,
                  Math.sin(a) * Math.sin(t) * rz * ripple));
        }
        double groove = Math.sin(lon * Math.PI * 2 / longitude * 7 + Math.cos(lat * .7));
        int shade = brain && groove < -.65 ? 0xff9f869e : color;
        face(p, center, shade, region, false);
      }
  }

  private void ring(
      double x,
      double y,
      double z,
      double radius,
      double thickness,
      int color,
      CyberwareCategory region) {
    for (int k = 0; k < 24; k++) {
      double a = k * Math.PI / 12, b = (k + 1) * Math.PI / 12;
      rod(
          v(x + Math.cos(a) * radius, y + Math.sin(a) * radius, z),
          v(x + Math.cos(b) * radius, y + Math.sin(b) * radius, z),
          thickness,
          color,
          region);
    }
  }
}
