package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Stable two-tab service: purchase never exposes or mutates installed sockets. */
public final class RipperdocScreen extends AbstractContainerScreen<RipperdocMenu> {
  private int page, category = -1, tier;
  private EditBox search;
  private CyberActionButton purchase, install, filter, tierButton, prev, next;
  private final java.util.List<CyberActionButton> buys = new ArrayList<>();

  public RipperdocScreen(RipperdocMenu m, Inventory i, Component title) {
    super(m, i, title);
    imageWidth = 318;
    imageHeight = 234;
    inventoryLabelY = titleLabelY = -1000;
  }

  private void send(int id) {
    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
  }

  @Override
  protected void init() {
    super.init();
    buys.clear();
    purchase = button(10, 26, 92, "PURCHASE", () -> send(10));
    install = button(108, 26, 92, "INSTALL", () -> send(11));
    search =
        new EditBox(
            font, leftPos + 12, topPos + 51, 146, 16, Component.literal("Search cyberware"));
    search.setMaxLength(48);
    search.setHint(Component.literal("Search cyberware"));
    search.setResponder(s -> page = 0);
    addRenderableWidget(search);
    filter =
        button(
            164,
            50,
            92,
            "ALL SYSTEMS",
            () -> {
              category++;
              if (category >= CyberwareCategory.values().length) category = -1;
              page = 0;
            });
    tierButton =
        button(
            260,
            50,
            48,
            "ALL",
            () -> {
              tier = (tier + 1) % 6;
              page = 0;
            });
    for (int n = 0; n < 5; n++) {
      final int row = n;
      buys.add(
          button(
              249,
              77 + n * 25,
              58,
              "BUY",
              () -> {
                var offers = filtered();
                int idx = page * 5 + row;
                if (idx < offers.size()) send(1000 + offers.get(idx));
              }));
    }
    prev = button(12, 204, 30, "<", () -> page = Math.max(0, page - 1));
    next =
        button(
            278,
            204,
            30,
            ">",
            () -> {
              if ((page + 1) * 5 < filtered().size()) page++;
            });
  }

  private CyberActionButton button(int x, int y, int w, String label, Runnable press) {
    return addRenderableWidget(
        new CyberActionButton(leftPos + x, topPos + y, w, 18, Component.literal(label), press));
  }

  private java.util.List<Integer> filtered() {
    var all = CyberwareCatalogue.offers();
    var out = new ArrayList<Integer>();
    String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
    for (int n = 0; n < all.size(); n++) {
      var o = all.get(n);
      if ((category < 0 || o.category() == CyberwareCategory.values()[category])
          && (tier == 0 || o.tier() == tier)
          && o.item().getDescription().getString().toLowerCase(Locale.ROOT).contains(query))
        out.add(n);
    }
    return out;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    boolean shop = menu.tab() == 0;
    var offers = filtered();
    page = Math.min(page, Math.max(0, (offers.size() - 1) / 5));
    purchase.active = !shop && menu.getCarried().isEmpty();
    install.active = shop && menu.getCarried().isEmpty();
    search.visible =
        search.active = filter.visible = tierButton.visible = prev.visible = next.visible = shop;
    filter.setMessage(
        Component.literal(
            category < 0
                ? "ALL SYSTEMS"
                : I18n.get("cybernetics2077.bench." + CyberwareCategory.values()[category].id)));
    tierButton.setMessage(Component.literal(tier == 0 ? "ALL" : "T" + tier));
    prev.active = page > 0;
    next.active = (page + 1) * 5 < offers.size();
    for (int n = 0; n < 5; n++) {
      var b = buys.get(n);
      int i = page * 5 + n;
      b.visible = shop && i < offers.size();
      b.active =
          b.visible
              && menu.getCarried().isEmpty()
              && menu.eddies() >= CyberwareCatalogue.offers().get(offers.get(i)).price();
    }
    super.render(g, mx, my, partial);
    renderTooltip(g, mx, my);
    if (shop)
      for (int n = 0; n < 5 && page * 5 + n < offers.size(); n++)
        if (mx >= leftPos + 12
            && mx < leftPos + 244
            && my >= topPos + 74 + n * 25
            && my < topPos + 98 + n * 25)
          g.renderTooltip(
              font,
              CyberwareCatalogue.offers().get(offers.get(page * 5 + n)).item().getDefaultInstance(),
              mx,
              my);
  }

  @Override
  protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      c.panel(0, 0, imageWidth, imageHeight, 7, 0xfc0a131b, 0xff4a878b);
      c.glowText("RIPPERDOC // CLINICAL SERVICES", 10, 7, 12, 0xff80e7e0);
      c.right("€$ " + String.format(Locale.ROOT, "%,d", menu.eddies()), 306, 31, 10, 0xffe6bd6c);
      c.rect(menu.tab() == 0 ? 10 : 108, 45, 92, 1, 0xff79ece0);
      if (menu.tab() == 0) {
        var rows = filtered();
        int first = page * 5;
        for (int n = 0; n < 5 && first + n < rows.size(); n++) {
          var o = CyberwareCatalogue.offers().get(rows.get(first + n));
          float y = 74 + n * 25;
          c.panel(11, y, 297, 23, 3, 0xff11232b, 0xff31515d);
          c.rect(12, y + 1, 2, 21, CyberwareTier.of(o.tier()).color);
          c.text(o.item().getDescription().getString(), 38, y + 4, 10, 0xffd6e2df);
          c.text(
              "TIER " + o.tier() + "   /   €$ " + String.format(Locale.ROOT, "%,d", o.price()),
              38,
              y + 14,
              8,
              0xff9db8b6);
        }
        c.center(
            rows.isEmpty()
                ? "NO MATCHING CYBERWARE"
                : (page + 1) + " / " + Math.max(1, (rows.size() + 4) / 5),
            159,
            209,
            10,
            0xff8acac9);
        String result =
            switch (menu.result()) {
              case 1 -> "INSUFFICIENT EDDIES";
              case 2 -> "FREE AN INVENTORY SLOT";
              case 3 -> "PURCHASE ADDED TO INVENTORY";
              default -> "Hover an implant for its effects. Installation is free.";
            };
        c.center(result, 159, 225, 8, menu.result() == 3 ? 0xff80e8bd : 0xffb6b2a7);
        CyberBatch.flush(g);
        for (int n = 0; n < 5 && first + n < rows.size(); n++)
          g.renderItem(
              CyberwareCatalogue.offers().get(rows.get(first + n)).item().getDefaultInstance(),
              17,
              78 + n * 25);
      } else {
        for (var cat : CyberwareCategory.values()) {
          float x = CyberneticsBenchMenu.x(cat), y = 49 + (cat.ordinal() / 2) * 18;
          c.text(I18n.get("cybernetics2077.bench." + cat.id), x, y + 4, 10, 0xffc3d1ce);
          for (int n = 0; n < cat.sockets; n++) {
            float sx = x + 75 + n * 18;
            c.panel(sx, y - 1, 18, 18, 2, 0xff071018, 0xff52757d);
            if (menu.getSlot(cat.firstSlot() + n).getItem().isEmpty())
              CyberGlyphs.category(c, cat, sx + 4, y + 3, 10, 0x6866a5b5);
          }
        }
        c.body("SHIFT-CLICK", 10, 189, 8, 0xffa6b6b5);
        c.body("INSTALL /", 10, 201, 8, 0xffa6b6b5);
        c.body("REMOVE", 10, 211, 8, 0xffa6b6b5);
        for (int row = 0; row < 3; row++)
          for (int col = 0; col < 9; col++)
            c.panel(83 + col * 18, 142 + row * 18, 18, 18, 2, 0xff070e16, 0xff394f58);
        for (int col = 0; col < 9; col++)
          c.panel(83 + col * 18, 204, 18, 18, 2, 0xff111e25, 0xff567b7f);
        c.body("CAPACITY", 10, 153, 9, 0xff7fd1d0);
        c.text(
            menu.capacityUsed() + " / " + Cybernetics2077.CAPACITY.get(), 10, 169, 12, 0xffe7bf6f);
      }
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }
}
