package dev.cybernetics2077.cyberware;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.ui.BodyView;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

final class BodyViewTest {
  @Test
  void cameraInterpolatesAndEveryRegionStaysInTheDetailViewport() {
    for (var region : CyberwareCategory.values()) {
      var target = BodyView.category(region);
      var middle = BodyView.OVERVIEW.towards(target, .5f);
      assertTrue(
          target.x() >= 950
              && target.x() + target.w() < 1700
              && target.y() > 110
              && target.y() + target.h() < 920);
      assertTrue(target.span() > 0 && target.span() < BodyView.OVERVIEW.span());
      assertEquals((BodyView.OVERVIEW.centerY() + target.centerY()) / 2, middle.centerY(), .00001);
      assertEquals(target.span(), BodyView.OVERVIEW.towards(target, 1).span(), .00001);
    }
  }

  @Test
  void equalWorldLengthsStayEqualInScreenPixelsThroughoutEveryZoom() {
    for (boolean slim : new boolean[] {false, true})
      for (var category : CyberwareCategory.values())
        for (int frame = 0; frame <= 20; frame++) {
          var view = BodyView.OVERVIEW.towards(BodyView.category(category, slim), frame / 20f);
          var projection = view.projection(0);
          var origin = projection.transform(new Vector4f(0, 0, 0, 1));
          var horizontal = projection.transform(new Vector4f(.5f, 0, 0, 1));
          var vertical = projection.transform(new Vector4f(0, .5f, 0, 1));
          float width = Math.abs(horizontal.x - origin.x) * view.w() / 2;
          float height = Math.abs(vertical.y - origin.y) * view.h() / 2;
          assertEquals(width, height, .001, category + " frame " + frame);
        }
  }

  @Test
  void focusProjectsToViewportCenterIncludingSlimArmsAndPointerRotation() {
    for (boolean slim : new boolean[] {false, true})
      for (var category : CyberwareCategory.values())
        for (float pointer : new float[] {-3, 0, 3}) {
          var view = BodyView.category(category, slim);
          var focus =
              new Vector4f(view.centerX(), view.centerY(), 0, 1)
                  .rotateY((float) Math.toRadians(view.yaw() + pointer));
          view.projection(pointer).transform(focus);
          assertEquals(0, focus.x, .00001, category.toString());
          assertEquals(0, focus.y, .00001, category.toString());
        }
    assertEquals(
        BodyView.OVERVIEW.y() + BodyView.OVERVIEW.h() / 2, BodyView.OVERVIEW.canvasY(.5f), .00001);
  }
}
