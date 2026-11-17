package dev.cybernetics2077.client.mantis;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;

/**
 * Native geometry must stay in cube-local coordinates; a visited ModelPart pose must not bake in.
 */
public final class ArmSkinBoundsReview {
  public static void verify(AbstractClientPlayer player) {
    boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
    for (boolean left : new boolean[] {false, true})
      for (boolean sleeve : new boolean[] {false, true}) {
        float[] min = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY};
        float[] max = {Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
        var faces = NativeArmSkin.mesh(player, left, sleeve, slim);
        if (faces.size() != 6)
          throw new IllegalStateException("Native arm cube did not have six faces");
        for (var face : faces)
          for (var v : face) {
            float[] xyz = {v.x(), v.y(), v.z()};
            for (int i = 0; i < 3; i++) {
              min[i] = Math.min(min[i], xyz[i]);
              max[i] = Math.max(max[i], xyz[i]);
            }
            if (v.u() < 0 || v.u() > 1 || v.v() < 0 || v.v() > 1)
              throw new IllegalStateException("Native skin UV out of bounds");
          }
        float inf = sleeve ? .015625f : 0, w = (slim ? 3 : 4) / 32f;
        float[] expectedMin = {-w - inf, -.125f - inf, -.125f - inf},
            expectedMax = {w + inf, .625f + inf, .125f + inf};
        for (int i = 0; i < 3; i++)
          if (Math.abs(min[i] - expectedMin[i]) > .00001
              || Math.abs(max[i] - expectedMax[i]) > .00001)
            throw new IllegalStateException(
                "Arm skin contains an extra model transform: axis "
                    + i
                    + " bounds "
                    + min[i]
                    + ".."
                    + max[i]);
      }
    System.out.println("Native arm cube-local skin bounds and UVs match both arms/sleeves");
  }
}
