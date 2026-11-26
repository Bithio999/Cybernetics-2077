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
      boolean hover = isHoveredOrFocused();
      c.rect(getX(), getY(), width, height, hover ? 0xff20313a : 0x88111c24);
      c.line(
          getX(),
          getY() + height - 1,
          getX() + width,
          getY() + height - 1,
          .65f,
          hover ? ServiceStyle.CYAN : ServiceStyle.LINE);
      if (hover) c.rect(getX(), getY() + 3, 1, height - 6, ServiceStyle.CYAN);
      String text =
          c.fit(getMessage().getString().toUpperCase(java.util.Locale.ROOT), width - 8, 9, true);
      c.center(text, getX() + width / 2f, getY() + (height - 9) / 2f, 9, ServiceStyle.CYAN);
    } finally {
      CyberBatch.flush(g);
    }
  }
}
