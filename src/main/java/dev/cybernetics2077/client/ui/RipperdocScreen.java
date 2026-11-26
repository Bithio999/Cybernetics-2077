package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mob.DamagedCyberneticPart;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** A six-row catalogue with one selected purchase, rather than two action buttons on every row. */
public final class RipperdocScreen extends AbstractContainerScreen<RipperdocMenu> {
  private int page, category = -1, tier, selected = -1;
  private EditBox search;
  private CyberActionButton purchase, install, filter, tierButton, prev, next, buy, trade;
  private final List<Row> rows = new ArrayList<>();

  public RipperdocScreen(RipperdocMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 314;
    imageHeight = 222;
    inventoryLabelY = titleLabelY = -1000;
  }

  private void send(int id) {
    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
  }

  private CyberActionButton button(int x, int y, int w, String text, Runnable press) {
    return addRenderableWidget(
        new CyberActionButton(leftPos + x, topPos + y, w, 15, Component.literal(text), press));
  }

  @Override
  protected void init() {
    super.init();
    rows.clear();
    purchase = button(10, 25, 88, "PURCHASE", () -> send(10));
    install = button(104, 25, 74, "INSTALL", () -> send(11));
    search =
        new EditBox(
            font, leftPos + 12, topPos + 46, 140, 14, Component.literal("Search cyberware"));
    search.setHint(Component.literal("Search"));
    search.setMaxLength(48);
    search.setResponder(
        s -> {
          page = 0;
          selected = -1;
        });
    addRenderableWidget(search);
    filter =
        button(
            160,
            45,
            95,
            "ALL SYSTEMS",
            () -> {
              category++;
              if (category >= CyberwareCategory.values().length) category = -1;
              page = 0;
              selected = -1;
            });
    tierButton =
        button(
            260,
            45,
            42,
            "ALL",
            () -> {
              tier = (tier + 1) % 6;
              page = 0;
              selected = -1;
            });
    for (int i = 0; i < 6; i++) rows.add(addRenderableWidget(new Row(i)));
    prev =
        button(
            12,
            184,
            18,
            "<",
            () -> {
              page = Math.max(0, page - 1);
              selected = -1;
            });
    next =
        button(
            87,
            184,
            18,
            ">",
            () -> {
              if ((page + 1) * 6 < filtered().size()) {
                page++;
                selected = -1;
              }
            });
    buy =
        button(
            155,
            184,
            98,
            "BUY",
            () -> {
              if (selected >= 0) send(1000 + selected);
            });
    trade =
        button(
            257,
            184,
            45,
            "-30%",
            () -> {
              if (selected >= 0) send(2000 + selected);
            });
  }

  private List<Integer> filtered() {
    var list = new ArrayList<Integer>();
    var all = CyberwareCatalogue.offers();
    String query = search == null ? "" : search.getValue().strip().toLowerCase(Locale.ROOT);
    for (int i = 0; i < all.size(); i++) {
      var o = all.get(i);
      if (menu.stocked(i)
          && menu.streetCred() >= o.requiredStreetCred()
          && (category < 0 || o.category() == CyberwareCategory.values()[category])
          && (tier == 0 || o.tier() == tier)
          && o.item().getDescription().getString().toLowerCase(Locale.ROOT).contains(query))
        list.add(i);
    }
    return list;
  }

  @Override
  public void render(GuiGraphics g, int mx, int my, float partial) {
    boolean shop = menu.tab() == 0;
    var offers = filtered();
    page = Math.min(page, Math.max(0, (offers.size() - 1) / 6));
    int selectedRow = offers.indexOf(selected);
    if (selectedRow < page * 6 || selectedRow >= (page + 1) * 6)
      selected = offers.isEmpty() ? -1 : offers.get(page * 6);
    purchase.active = !shop && menu.getCarried().isEmpty();
    install.active = shop && menu.getCarried().isEmpty();
    search.visible =
        search.active =
            filter.visible = tierButton.visible = prev.visible = next.visible = buy.visible = shop;
    filter.setMessage(
        Component.literal(
            category < 0
                ? "ALL SYSTEMS"
                : I18n.get("cybernetics2077.bench." + CyberwareCategory.values()[category].id)));
    tierButton.setMessage(Component.literal(tier == 0 ? "ALL" : "T" + tier));
    prev.active = page > 0;
    next.active = (page + 1) * 6 < offers.size();
    for (int i = 0; i < rows.size(); i++) {
      var row = rows.get(i);
      row.visible = shop && page * 6 + i < offers.size();
      row.offer = row.visible ? offers.get(page * 6 + i) : -1;
    }
    buy.active = false;
    trade.visible = false;
    if (selected >= 0) {
      var o = CyberwareCatalogue.offers().get(selected);
      boolean iconic = o.iconic();
      buy.setMessage(Component.literal(iconic ? "ACQUIRE" : "BUY"));
      buy.setWidth(iconic ? 147 : 98);
      buy.active =
          menu.getCarried().isEmpty()
              && menu.eddies() >= o.price()
              && (!iconic || menu.requisitions(o.tier()) > 0)
              && (o.prerequisite() == null || menu.prerequisites(o) > 0);
      buy.setTooltip(
          Tooltip.create(
              Component.literal(
                  "€$"
                      + o.price()
                      + (iconic ? " + 1 Tier " + o.tier() + " Iconic chip" : "")
                      + (o.prerequisite() != null ? " + unequipped Apogee" : ""))));
      trade.visible = shop && o.item() instanceof CyberwareItem && !iconic;
      trade.active =
          trade.visible
              && menu.getCarried().isEmpty()
              && menu.parts(o.tier()) > 0
              && menu.eddies() >= DamagedCyberneticPart.price(o);
      trade.setTooltip(
          Tooltip.create(
              Component.literal(
                  "€$"
                      + DamagedCyberneticPart.price(o)
                      + " + 1 Tier "
                      + o.tier()
                      + " damaged part")));
    }
    super.render(g, mx, my, partial);
    renderTooltip(g, mx, my);
    if (shop)
      for (var row : rows)
        if (row.visible && row.isHovered())
          g.renderTooltip(
              font, CyberwareCatalogue.offers().get(row.offer).item().getDefaultInstance(), mx, my);
  }

  @Override
  public boolean mouseScrolled(double x, double y, double dx, double dy) {
    if (menu.tab() == 0) {
      int previous = page;
      page = Math.clamp(page - (int) Math.signum(dy), 0, Math.max(0, (filtered().size() - 1) / 6));
      if (page != previous) selected = -1;
      return true;
    }
    return super.mouseScrolled(x, y, dx, dy);
  }

  @Override
  protected void renderLabels(GuiGraphics g, int x, int y) {}

  @Override
  protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      ServiceStyle.frame(c, imageWidth, imageHeight, ServiceStyle.CYAN);
      c.text("RIPPERDOC", 17, 7, 12, ServiceStyle.INK);
      c.right(
          "SC " + menu.streetCred() + " / €$" + String.format(Locale.ROOT, "%,d", menu.eddies()),
          304,
          9,
          9,
          ServiceStyle.GOLD);
      c.rect(menu.tab() == 0 ? 10 : 104, 40, menu.tab() == 0 ? 88 : 74, 1, ServiceStyle.CYAN);
      if (menu.tab() == 0) {
        var offers = filtered();
        c.body(
            (page + 1) + " / " + Math.max(1, (offers.size() + 5) / 6),
            38,
            188,
            8,
            ServiceStyle.MUTED);
        c.body(
            "DAY "
                + (menu.stockDay() + 1)
                + " / RESTOCK "
                + String.format(
                    Locale.ROOT,
                    "%02d:%02d",
                    (menu.restockTicks() + 19) / 1200,
                    (menu.restockTicks() + 19) / 20 % 60),
            12,
            203,
            7,
            ServiceStyle.MUTED);
        c.right(
            c.fit(result(), 288, 7, false),
            302,
            213,
            7,
            menu.result() == 3 ? ServiceStyle.CYAN : ServiceStyle.MUTED);
        if (offers.isEmpty()) c.center("NO MATCHING CYBERWARE", 157, 113, 10, ServiceStyle.MUTED);
      } else {
        for (var category : CyberwareCategory.values()) {
          float x = CyberneticsBenchMenu.x(category), y = 43 + (category.ordinal() / 2) * 18;
          c.body(
              c.fit(I18n.get("cybernetics2077.bench." + category.id), 70, 9, false),
              x,
              y + 4,
              9,
              ServiceStyle.MUTED);
          for (int n = 0; n < category.sockets; n++) {
            float sx = x + 75 + n * 18;
            ServiceStyle.slot(c, sx, y - 1, false);
            var item = menu.getSlot(category.firstSlot() + n).getItem();
            if (item.isEmpty()) CyberGlyphs.category(c, category, sx + 4, y + 3, 9, 0x50669aa8);
            else if (item.getItem() instanceof CyberwareItem a)
              c.rect(sx + 1, y + 15, 16, 1, a.tier().color);
          }
        }
        for (int row = 0; row < 3; row++)
          for (int col = 0; col < 9; col++)
            ServiceStyle.slot(c, 83 + col * 18, 136 + row * 18, false);
        for (int col = 0; col < 9; col++) ServiceStyle.slot(c, 83 + col * 18, 194, true);
        c.text("CAPACITY", 10, 147, 8, ServiceStyle.MUTED);
        c.text(menu.capacityUsed() + " / " + menu.capacityLimit(), 10, 160, 12, ServiceStyle.GOLD);
        c.body("Shift-click", 10, 186, 8, ServiceStyle.MUTED);
        c.body("equip/remove", 10, 198, 8, ServiceStyle.MUTED);
      }
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }

  private String result() {
    return switch (menu.result()) {
      case 1 -> "Not enough eddies";
      case 2 -> "Free an inventory slot";
      case 3 -> "Added to inventory";
      case 4 -> "More Street Cred required";
      case 5 -> "Stock changed; select an offer";
      case 6 -> "Matching damaged part required";
      case 7 -> "Matching Iconic chip required";
      case 8 -> "Unequip an Apogee into your inventory";
      default -> "Select an implant to buy. Hover for stats.";
    };
  }

  private final class Row extends AbstractButton {
    int offer = -1;

    Row(int row) {
      super(leftPos + 12, topPos + 64 + row * 19, 290, 18, Component.literal("Cyberware offer"));
    }

    @Override
    public void onPress() {
      selected = offer;
    }

    @Override
    protected void updateWidgetNarration(
        net.minecraft.client.gui.narration.NarrationElementOutput out) {
      defaultButtonNarrationText(out);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mx, int my, float partial) {
      if (offer < 0) return;
      var o = CyberwareCatalogue.offers().get(offer);
      setMessage(o.item().getDescription());
      CyberBatch.begin();
      try {
        var c = new CyberCanvas(g, 1);
        boolean chosen = selected == offer;
        if (chosen || isHoveredOrFocused())
          c.rect(getX(), getY(), width, height, chosen ? 0xff1c3037 : 0xff13232b);
        c.rect(
            getX(),
            getY() + 2,
            1,
            height - 4,
            chosen ? ServiceStyle.CYAN : CyberwareTier.of(o.tier()).color);
        c.line(
            getX() + 4, getY() + height, getX() + width, getY() + height, .45f, ServiceStyle.LINE);
        c.body(
            c.fit(o.item().getDescription().getString(), 192, 9, false),
            getX() + 23,
            getY() + 2,
            9,
            ServiceStyle.INK);
        c.body(
            (o.iconic() ? "ICONIC / " : "") + "T" + o.tier() + " / SC " + o.requiredStreetCred(),
            getX() + 23,
            getY() + 11,
            7,
            o.iconic() ? ServiceStyle.GOLD : ServiceStyle.MUTED);
        c.right(
            "€$" + String.format(Locale.ROOT, "%,d", o.price()),
            getX() + width - 3,
            getY() + 3,
            8,
            ServiceStyle.GOLD);
      } finally {
        CyberBatch.flush(g);
      }
      g.renderItem(o.item().getDefaultInstance(), getX() + 4, getY() + 1);
    }
  }
}
