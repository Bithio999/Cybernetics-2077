package dev.cybernetics2077.cyberware;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.ui.BodyView;
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
}
