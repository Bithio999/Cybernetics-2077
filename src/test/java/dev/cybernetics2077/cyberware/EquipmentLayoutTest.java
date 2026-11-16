package dev.cybernetics2077.cyberware;

import static org.junit.jupiter.api.Assertions.*;

import dev.cybernetics2077.client.ui.CyberLayout;
import java.util.*;
import org.junit.jupiter.api.Test;

final class EquipmentLayoutTest {
  @Test
  void everySocketHasOneCategoryAndVisibleHitbox() {
    assertEquals(21, CyberwareCategory.totalSlots());
    assertEquals(21, CyberLayout.SOCKETS.size());
    var ids = new HashSet<Integer>();
    for (var socket : CyberLayout.SOCKETS) {
      assertTrue(ids.add(socket.index()));
      assertEquals(socket.category(), CyberwareCategory.forSlot(socket.index()));
      assertEquals(socket.ordinal(), socket.index() - socket.category().firstSlot());
      assertTrue(socket.x() > 250 && socket.x() + 96 < 1650);
      assertTrue(socket.y() > 100 && socket.y() + 96 < 920);
    }
    assertThrows(IndexOutOfBoundsException.class, () -> CyberwareCategory.forSlot(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> CyberwareCategory.forSlot(21));
  }

  @Test
  void layoutAndHitboxesStayAlignedAtDifferentAspectRatios() {
    for (int[] size :
        new int[][] {{320, 240}, {427, 240}, {640, 360}, {960, 720}, {1920, 720}, {2560, 1440}}) {
      var layout = CyberLayout.fit(size[0], size[1]);
      for (var socket : CyberLayout.SOCKETS) {
        int x = layout.screenX(socket.x() + 48), y = layout.screenY(socket.y() + 48);
        assertTrue(x >= 0 && x < size[0] && y >= 0 && y < size[1]);
        assertEquals(socket.x() + 48, layout.virtualX(x), .51 / layout.scale());
        assertEquals(socket.y() + 48, layout.virtualY(y), .51 / layout.scale());
      }
    }
  }

  @Test
  void socketsNeverOverlapEachOther() {
    for (var a : CyberLayout.SOCKETS)
      for (var b : CyberLayout.SOCKETS)
        if (a.index() != b.index())
          assertFalse(
              a.x() < b.x() + 96 && a.x() + 96 > b.x() && a.y() < b.y() + 96 && a.y() + 96 > b.y());
  }

  @Test
  void equipmentStatsSurviveTheSignedShortMenuProtocol() {
    for (int value :
        new int[] {0, 100, 32767, 32768, 65535, 65536, 70000, 2_100_000, Integer.MAX_VALUE})
      assertEquals(
          value,
          StatWords.combine((short) StatWords.word(value, 0), (short) StatWords.word(value, 1)));
  }
}
