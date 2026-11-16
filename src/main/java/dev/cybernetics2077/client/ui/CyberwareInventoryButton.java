package dev.cybernetics2077.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;

/** Compact icon button in the inventory header, clear of slots, potion effects and recipe book. */
public final class CyberwareInventoryButton extends AbstractButton {
  private final Runnable press;

  public CyberwareInventoryButton(Runnable press) {
    super(0, 0, 20, 18, Component.translatable("cybernetics2077.button.open"));
    this.press = press;
  }

  @Override
  protected void updateWidgetNarration(
      net.minecraft.client.gui.narration.NarrationElementOutput out) {
    defaultButtonNarrationText(out);
  }

  @Override
  public void onPress() {
    press.run();
  }

  @Override
  protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {
    CyberBatch.begin();
    try {
      var c = new CyberCanvas(g, active ? 1 : .4f);
      var p = Geometry.panel(getX(), getY(), 20, 18, 3);
      c.shape(
          p,
          isHoveredOrFocused() ? 0xf0431720 : 0xed180f19,
          isHoveredOrFocused() ? 0xff6ee9f5 : 0xffd24651);
      if (isHoveredOrFocused()) c.halo(p, 0xffef5361);
      float x = getX() + 10, y = getY() + 9;
      c.rect(x - 4, y - 4, 8, 8, 0x204ac8db);
      c.stroke(
          java.util.List.of(
              new Geometry.Point(x - 4, y - 4),
              new Geometry.Point(x + 4, y - 4),
              new Geometry.Point(x + 4, y + 4),
              new Geometry.Point(x - 4, y + 4)),
          .7f,
          0xff70e2ed);
      for (int k = -2; k <= 2; k += 2) {
        c.line(x + k, y - 6, x + k, y - 4, .6f, 0xffef6970);
        c.line(x + k, y + 4, x + k, y + 6, .6f, 0xffef6970);
      }
      c.line(x - 2, y, x, y - 2, .8f, 0xff7ce7f4);
      c.line(x, y - 2, x + 2, y, .8f, 0xff7ce7f4);
      c.line(x + 2, y, x, y + 2, .8f, 0xff7ce7f4);
      c.line(x, y + 2, x - 2, y, .8f, 0xff7ce7f4);
    } finally {
      CyberBatch.flush(g);
    }
  }
}
