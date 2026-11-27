package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.cyberware.CarryReserveMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Compact native slot grid in the service menu's charcoal/cyan palette. */
public final class CarryReserveScreen extends AbstractContainerScreen<CarryReserveMenu> {
  public CarryReserveScreen(CarryReserveMenu menu, Inventory inv, Component title) {
    super(menu, inv, title);
    imageWidth = 176;
    imageHeight = 166;
    inventoryLabelY = 73;
  }

  @Override
  protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
    g.fill(leftPos, topPos, leftPos + 176, topPos + 166, 0xff0c181d);
    g.fill(leftPos, topPos, leftPos + 176, topPos + 2, 0xff79dedc);
    for (var slot : menu.slots) {
      int x = leftPos + slot.x, y = topPos + slot.y;
      g.fill(x - 1, y - 1, x + 17, y + 17, 0xff31515a);
      g.fill(x, y, x + 16, y + 16, slot.isActive() ? 0xff13282e : 0xff080f13);
    }
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    super.render(g, mx, my, partial);
    renderTooltip(g, mx, my);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int mx, int my) {
    g.drawString(font, title, 8, 6, 0xffa8e4df, false);
    g.drawString(font, playerInventoryTitle, 8, 73, 0xff849da4, false);
  }
}
