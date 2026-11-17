package dev.cybernetics2077.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;

/** Small workstation action, using the same type and cut-corner panels as the cyberware screen. */
public final class CyberActionButton extends AbstractButton {
  private final Runnable press;

  public CyberActionButton(int x, int y, int width, int height, Component text, Runnable press) {
    super(x, y, width, height, text);
    this.press = press;
  }

  @Override
  public void onPress() {
    press.run();
  }

  @Override
  protected void updateWidgetNarration(
      net.minecraft.client.gui.narration.NarrationElementOutput out) {
    defaultButtonNarrationText(out);
  }

  @Override
  protected void renderWidget(GuiGraphics g, int x, int y, float partial) {
    CyberBatch.begin();
    try {
      var c = new CyberCanvas(g, active ? 1 : .4f);
      int border = isHoveredOrFocused() ? 0xff7dedf4 : 0xffa94756;
      c.panel(
          getX(), getY(), width, height, 3, isHoveredOrFocused() ? 0xff233543 : 0xff111a26, border);
      c.center(
          getMessage().getString().toUpperCase(java.util.Locale.ROOT),
          getX() + width / 2f,
          getY() + 5,
          11,
          0xff82dce7);
    } finally {
      CyberBatch.flush(g);
    }
  }
}
