package dev.cybernetics2077.client.ui;

import static dev.cybernetics2077.client.ui.Geometry.Point;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.cyberware.*;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class CyberwareScreen extends AbstractContainerScreen<CyberwareMenu> {
  private static final int RED = 0xffed5964,
      CYAN = 0xff72e8f1,
      WHITE = 0xffe6ced2,
      MUTED = 0xff9a7d86;
  private final long opened = System.nanoTime();
  private final List<SocketButton> sockets = new ArrayList<>();
  private CyberLayout layout;
  private int selected = -1;
  private long lastFrame = System.nanoTime();
  private BodyView bodyView = BodyView.OVERVIEW;
  private float zoom;
  private CategoryButton previous, next;
  private BackButton back;

  public CyberwareScreen(CyberwareMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
  }

  public static void reloadResources() {
    CyberFont.warmup();
    HologramRenderer.reload();
  }

  @Override
  protected void init() {
    super.init();
    layout = CyberLayout.fit(width, height);
    sockets.clear();
    for (var slot : CyberLayout.SOCKETS) {
      var b = new SocketButton(slot);
      sockets.add(b);
      addRenderableWidget(b);
    }
    previous = new CategoryButton(-1);
    next = new CategoryButton(1);
    addRenderableWidget(previous);
    addRenderableWidget(next);
    back = new BackButton();
    addRenderableWidget(back);
  }

  @Override
  protected void containerTick() {
    for (var b : sockets) b.setMessage(slotName(b.socket));
    back.setMessage(
        Component.translatable(
            selected >= 0 ? "cybernetics2077.button.all" : "cybernetics2077.button.back"));
  }

  private Component slotName(CyberLayout.Socket socket) {
    var stack = menu.getSlot(socket.index()).getItem();
    return Component.translatable(
        "cybernetics2077.slot.narration",
        Component.translatable(socket.category().translation()),
        socket.ordinal() + 1,
        stack.isEmpty()
            ? Component.translatable("cybernetics2077.slot.empty")
            : stack.getHoverName());
  }

  @Override
  public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
    long now = System.nanoTime();
    float dt = (float) Math.min(.1, (now - lastFrame) / 1e9);
    lastFrame = now;
    float follow = (float) (1 - Math.exp(-dt / 0.105));
    zoom += ((selected >= 0 ? 1 : 0) - zoom) * follow;
    bodyView =
        bodyView.towards(
            selected >= 0
                ? BodyView.category(
                    CyberwareCategory.forSlot(selected),
                    minecraft.player.getSkin().model()
                        == net.minecraft.client.resources.PlayerSkin.Model.SLIM)
                : BodyView.OVERVIEW,
            follow);
    for (var b : sockets) {
      b.visible = selected < 0 || b.socket.category() == CyberwareCategory.forSlot(selected);
      b.setPosition(layout.screenX(slotX(b.socket)), layout.screenY(slotY(b.socket)));
    }
    previous.visible = next.visible = selected >= 0;
    for (var r : renderables) r.render(g, mouseX, mouseY, partial);
    int hovered = -1;
    for (var b : sockets)
      if (b.visible && b.isMouseOver(mouseX, mouseY)) {
        hovered = b.socket.index();
        break;
      }
    if (hovered < 0 && minecraft.getLastInputType().isKeyboard())
      for (var b : sockets)
        if (b.visible && b.isFocused()) {
          hovered = b.socket.index();
          break;
        }
    int focus = hovered >= 0 ? hovered : selected;
    var category = focus < 0 ? null : CyberwareCategory.forSlot(focus);
    float fade = (float) Geometry.ease((System.nanoTime() - opened) / 240e6);
    CyberBatch.begin();
    try {
      var background = new CyberCanvas(g, 1);
      background.rect(0, 0, width, height, 0xff060b11);
      background.gradient(0, 0, width, height, 0xff241017, 0xff090c13, 0xff040c12, 0xff090e16);
      g.pose().pushPose();
      g.pose().translate(layout.left(), layout.top(), 0);
      g.pose().scale((float) layout.scale(), (float) layout.scale(), 1);
      var c = new CyberCanvas(g, fade);
      glow(g, "red_glow", 650, 20, 670, 850, .8f * fade);
      glow(g, "cyan_glow", 775, 420, 400, 550, .3f * fade);
      for (int y = 0; y < 1080; y += 4) c.rect(0, y, 1920, .45f, 0x0a96384c);
      c.line(96, 111, 1824, 111, 1, 0x493f515b);
      c.glowText("CYBERWARE", 96, 42, 36, RED);
      c.tracked("NEURAL INTERFACE / EQUIPMENT", 98, 86, 12, 0xffa35e6a, 1.8f);
      c.right(
          minecraft.player.getName().getString().toUpperCase(Locale.ROOT),
          1824,
          48,
          20,
          0xffe8adb4);
      c.right(
          menu.installedCount() + " INSTALLED / " + CyberwareCategory.totalSlots() + " SOCKETS",
          1824,
          80,
          13,
          MUTED);
      c.dots(1850, 44, 3, 6, 0x58a33948);
      c.dots(60, 47, 3, 6, 0x58a33948);
      c.tracked("CYBERNETICS 2077", 867, 53, 15, 0xffa36b79, 2);
      c.center("BIOMETRIC LINK", 960, 81, 11, 0xff56838b);
      float pulse = (float) (.5 + .5 * Math.sin(System.nanoTime() / 8e8));
      c.rect(841, 82, 3, 3, ((int) (120 + 100 * pulse) << 24) | 0x69e4e9);
      meters(c);
      // A depth-isolated native Minecraft mesh keeps hologram rendering away from GUI state.
      CyberBatch.flush(g);
      CyberBatch.begin();
      HologramRenderer.draw(
          g,
          selected >= 0 ? CyberwareCategory.forSlot(selected) : category,
          bodyView,
          fade,
          (float) Math.clamp((layout.virtualX(mouseX) - 960) / 85, -3, 3) * (1 - zoom));
      if (selected < 0) {
        c.g.pose().pushPose();
        c.g.pose().translate(960, BodyView.OVERVIEW.canvasY(1.52f), 0);
        c.g.pose().scale(1, .14f, 1);
        c.glowArc(0, 0, 110, 0, (float) (Math.PI * 2), 1, 0x7057c5d7);
        c.g.pose().popPose();
        if (category != null) connection(c, category);
        for (var row : CyberLayout.ROWS) row(c, row, hovered);
        details(c, focus);
      } else categoryView(c, hovered);
      footer(c);
      g.pose().popPose();
    } finally {
      CyberBatch.flush(g);
    }
  }

  private static void glow(
      GuiGraphics g, String name, float x, float y, float w, float h, float alpha) {
    var p = new org.joml.Matrix4f(g.pose().last().pose());
    int tint = ((int) (alpha * 255) << 24) | 0xffffff;
    CyberBatch.submit(
        g,
        Cybernetics2077.id("textures/gui/" + name + ".png"),
        0,
        out -> {
          out.addVertex(p, x, y, 0).setUv(0, 0).setColor(tint);
          out.addVertex(p, x, y + h, 0).setUv(0, 1).setColor(tint);
          out.addVertex(p, x + w, y + h, 0).setUv(1, 1).setColor(tint);
          out.addVertex(p, x + w, y, 0).setUv(1, 0).setColor(tint);
        });
  }

  private void meters(CyberCanvas c) {
    c.tracked("CAPACITY", 104, 145, 13, 0xffb99759, 1.8f);
    c.text(
        menu.capacityUsed() + " / " + (menu.capacityLimit() > 0 ? menu.capacityLimit() : "—"),
        104,
        167,
        24,
        0xffe5bc65);
    c.tracked("TOTAL ARMOR", 1670, 145, 13, 0xff62aebc, 1.8f);
    c.right(format(menu.armor()), 1816, 167, 24, CYAN);
    CyberGlyphs.category(c, CyberwareCategory.INTEGUMENTARY_SYSTEM, 1745, 202, 38, 0xff66d2df);
    CyberGlyphs.category(c, CyberwareCategory.FRONTAL_CORTEX, 121, 202, 38, 0xffd5ad57);
    meter(
        c,
        126,
        263,
        54,
        582,
        menu.capacityUsed(),
        Math.max(1, menu.capacityLimit()),
        0xffffc456,
        false);
    meter(c, 1748, 263, 54, 582, menu.armor(), 20, 0xff63dae7, true);
    c.tracked("CYBERWARE LOAD", 104, 865, 10, 0xff79595e, 1);
    c.right("ARMOR POINTS", 1814, 865, 10, 0xff446b76);
  }

  private void meter(
      CyberCanvas c,
      float x,
      float y,
      float w,
      float h,
      double value,
      double limit,
      int tint,
      boolean right) {
    int count = 54;
    float step = h / count;
    double ratio = Math.clamp(value / limit, 0, 1);
    c.line(x + w / 2, y, x + w / 2, y + h, 1, 0x42425660);
    for (int n = 0; n < count; n++) {
      boolean filled = n < Math.floor(ratio * count);
      float yy = y + h - n * step;
      c.rect(x, yy, w, 3.1f, filled ? tint : 0x4a37414a);
      c.rect(x + w + 7, yy, 4, 3.1f, filled ? (tint & 0xffffff) | 0x55000000 : 0x1937414a);
    }
    float marker = (float) (y + h - ratio * h);
    c.glowLine(x - 13, marker, x + w + 15, marker, 1.3f, tint);
    float start = right ? x - 90 : x + w + 14;
    c.rect(start, marker - 13, 73, 26, tint);
    c.text(format(value), start + 8, marker - 10, 22, 0xff11212a);
    c.line(x - 8, y - 9, x + w + 9, y - 9, .8f, 0xff923c49);
  }

  private float slotX(CyberLayout.Socket slot) {
    return selected < 0 ? slot.x() : 410 + slot.ordinal() * 112;
  }

  private float slotY(CyberLayout.Socket slot) {
    return selected < 0 ? slot.y() : 230;
  }

  private void row(CyberCanvas c, CyberLayout.Row row, int hovered) {
    float w = row.category().sockets * 112 - 16;
    String title = I18n.get(row.category().translation()).toUpperCase(Locale.ROOT);
    if (row.left()) c.right(title, row.x() + w, row.y() - 33, 25, RED);
    else c.text(title, row.x(), row.y() - 33, 25, RED);
    for (var slot : CyberLayout.SOCKETS)
      if (slot.category() == row.category()) drawSocket(c, slot, slot.x(), slot.y(), hovered);
  }

  private void drawSocket(CyberCanvas c, CyberLayout.Socket slot, float x, float y, int hovered) {
    boolean hot = slot.index() == hovered, chosen = slot.index() == selected;
    var stack = menu.getSlot(slot.index()).getItem();
    int tierColor =
        stack.getItem() instanceof CyberwareItem i ? i.tier().color : CyberwareTier.ONE.color;
    int border = hot ? CYAN : chosen ? 0xfff07d86 : stack.isEmpty() ? 0xa16b3d48 : tierColor;
    var shape = Geometry.data(x, y, 96, 96);
    if (hot) c.halo(shape, CYAN);
    c.shape(shape, hot ? 0xe3182630 : chosen ? 0xed29131f : 0xbc160e17, border);
    if (!stack.isEmpty()) {
      c.rect(x + 1, y + 1, 5, 93, tierColor);
      drawItem(c, stack, x + 21, y + 18, 3.6f);
      c.stroke(
          List.of(
              new Point(x + 77, y + 6),
              new Point(x + 89, y + 6),
              new Point(x + 89, y + 18),
              new Point(x + 77, y + 18)),
          .7f,
          tierColor);
      c.text(
          stack.getItem() instanceof CyberwareItem i ? Integer.toString(i.tier().level) : "?",
          x + 80,
          y + 6,
          11,
          tierColor);
    } else {
      CyberGlyphs.category(c, slot.category(), x + 26, y + 15, 44, hot ? 0x6269cbd4 : 0x566e919d);
      c.stroke(
          List.of(
              new Point(x + 23, y + 69),
              new Point(x + 72, y + 69),
              new Point(x + 72, y + 77),
              new Point(x + 23, y + 77)),
          .7f,
          hot ? 0xc76ac8d3 : 0x874e2933);
      c.line(x + 25, y + 70, x + 70, y + 76, .7f, hot ? 0xc76ac8d3 : 0x87582b37);
    }
    if (hot || chosen) {
      c.rect(x + 5, y + 5, 3, 3, hot ? CYAN : RED);
      c.tracked(
          String.format(Locale.ROOT, "%02d", slot.ordinal() + 1),
          x + 11,
          y + 5,
          9,
          hot ? CYAN : RED,
          .4f);
    }
  }

  private void categoryView(CyberCanvas base, int hovered) {
    var c = base.fade(zoom);
    var category = CyberwareCategory.forSlot(selected);
    c.line(410, 196, 925, 196, 1, 0xff6dbcca);
    c.glowText(I18n.get(category.translation()).toUpperCase(Locale.ROOT), 452, 158, 28, CYAN);
    c.text("< A", 410, 165, 15, previous.isHoveredOrFocused() ? WHITE : RED);
    c.right("D >", 925, 165, 15, next.isHoveredOrFocused() ? WHITE : RED);
    for (var slot : CyberLayout.SOCKETS)
      if (slot.category() == category) drawSocket(c, slot, slotX(slot), slotY(slot), hovered);
    var stack = menu.getSlot(selected).getItem();
    c.panel(410, 371, 515, 390, 12, 0xe70a101b, 0xff5a8492);
    c.rect(410, 371, 4, 390, CYAN);
    c.text(
        stack.isEmpty()
            ? "EMPTY SOCKET"
            : c.fit(stack.getHoverName().getString().toUpperCase(Locale.ROOT), 465, 30, true),
        434,
        397,
        30,
        stack.isEmpty() ? MUTED : WHITE);
    c.tracked(
        "SOCKET " + (selected - category.firstSlot() + 1) + " / " + category.sockets,
        434,
        440,
        14,
        CYAN,
        1);
    c.line(434, 477, 900, 477, 1, 0x80568293);
    if (stack.getItem() instanceof CyberwareItem item) {
      c.text("TIER " + item.tier().level, 434, 499, 23, item.tier().color);
      c.right(item.capacityCost() + " CAPACITY", 900, 505, 17, 0xffe3b76f);
      if (stack.is(Cybernetics2077.OPTICS.get())) {
        c.glowText("OPTICAL SCANNER", 434, 548, 23, CYAN);
        c.body("Identify living targets within 32 blocks.", 434, 589, 19, WHITE);
        c.body("View health, armor and combat data.", 434, 617, 19, WHITE);
        c.body(
            "Hold "
                + ScannerClient.KEY.getTranslatedKeyMessage().getString()
                + " to activate the scanner.",
            434,
            663,
            19,
            CYAN);
      }
    } else {
      c.body("No implant installed in this socket.", 434, 510, 21, WHITE);
      c.body("Install compatible cyberware at a", 434, 563, 19, MUTED);
      c.body("Cybernetics Bench.", 434, 591, 19, MUTED);
    }
    c.line(434, 704, 900, 704, 1, 0x80568293);
    c.tracked("EQUIPMENT VIEW / INSTALLATION LOCKED", 434, 725, 12, 0xffcf7c89, .7f);
    c.panel(410, 805, 515, 76, 10, 0xc9071019, 0x8077c8d8);
    c.rect(410, 805, 4, 76, CYAN);
    c.body("Visit a Cybernetics Bench to install", 432, 824, 19, CYAN);
    c.body("or remove currently equipped cyberware.", 432, 851, 17, 0xff93bac6);
  }

  private void drawItem(CyberCanvas c, ItemStack stack, float x, float y, float scale) {
    // Items are rare in the view-only first release; preserve the native item renderer and tooltip
    // data.
    c.g.flush();
    c.g.pose().pushPose();
    c.g.pose().translate(x, y, 0);
    c.g.pose().scale(scale, scale, 1);
    c.g.renderItem(stack, 0, 0);
    c.g.pose().popPose();
  }

  private void connection(CyberCanvas c, CyberwareCategory category) {
    var row =
        CyberLayout.ROWS.stream().filter(r -> r.category() == category).findFirst().orElseThrow();
    float x = row.left() ? row.x() + row.category().sockets * 112 - 2 : row.x() - 14,
        y = row.y() + 48;
    float[] point =
        switch (category) {
          case FRONTAL_CORTEX, FACE -> new float[] {960, 244};
          case OPERATING_SYSTEM -> new float[] {977, 342};
          case ARMS -> new float[] {850, 422};
          case HANDS -> new float[] {1072, 519};
          case SKELETON -> new float[] {950, 566};
          case NERVOUS_SYSTEM -> new float[] {970, 460};
          case CIRCULATORY_SYSTEM -> new float[] {960, 404};
          case INTEGUMENTARY_SYSTEM -> new float[] {1010, 607};
          case LEGS -> new float[] {984, 741};
        };
    float bend = row.left() ? Math.min(x + 52, point[0] - 20) : Math.max(x - 52, point[0] + 20);
    c.glowLine(x, y, bend, y, .8f, 0x7960dbe9);
    c.line(bend, y, point[0], point[1], .8f, 0x7960dbe9);
    c.glowArc(point[0], point[1], 5, 0, (float) (Math.PI * 2), 1, 0xc86eeaf4);
  }

  private void details(CyberCanvas c, int focus) {
    c.shape(Geometry.data(350, 920, 1218, 77), 0xc80b1018, 0x705b343e);
    if (focus < 0) {
      c.text("SELECT A SOCKET", 376, 936, 23, 0xffd89ba5);
      c.body("Inspect your installed cyberware.", 376, 968, 15, MUTED);
      c.right("IMPLANT INVENTORY", 1540, 944, 13, 0xff81515d);
    } else {
      var category = CyberwareCategory.forSlot(focus);
      var stack = menu.getSlot(focus).getItem();
      c.text(I18n.get(category.translation()).toUpperCase(Locale.ROOT), 376, 934, 22, CYAN);
      c.body(
          "SOCKET "
              + (focus - category.firstSlot() + 1)
              + "  /  "
              + (stack.isEmpty() ? "EMPTY" : stack.getHoverName().getString()),
          376,
          968,
          15,
          WHITE);
      String detail =
          stack.isEmpty()
              ? "No cyberware installed."
              : "CAPACITY  +"
                  + (stack.getItem() instanceof CyberwareItem item ? item.capacityCost() : 0);
      c.right(detail, 1540, 938, 19, stack.isEmpty() ? MUTED : 0xffdfb467);
      c.right(I18n.get(category.translation() + ".description"), 1540, 969, 14, 0xff9c8792);
    }
  }

  private void footer(CyberCanvas c) {
    int tint = back.isHoveredOrFocused() ? CYAN : RED;
    c.line(96, 1021, 88, 1029, 1, tint);
    c.line(88, 1029, 96, 1037, 1, tint);
    c.tracked("ESC", 112, 1021, 14, tint, .9f);
    c.text(selected >= 0 ? "ALL CYBERWARE" : "BACK TO INVENTORY", 155, 1019, 20, tint);
    c.tracked("LEFT MOUSE  /  INSPECT", 1291, 1023, 12, 0xffba6977, .8f);
    c.tracked("TAB  /  NAVIGATE", 1628, 1023, 12, 0xff77aab6, .8f);
    c.dots(1850, 1015, 3, 6, 0x58623443);
    c.dots(60, 1015, 3, 6, 0x58623443);
  }

  private static String format(double value) {
    return value == (long) value
        ? Long.toString((long) value)
        : String.format(Locale.ROOT, "%.1f", value);
  }

  @Override
  protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {}

  @Override
  public boolean mouseClicked(double x, double y, int button) {
    for (var child : children())
      if (child.mouseClicked(x, y, button)) {
        setFocused(child);
        return true;
      }
    return true;
  }

  @Override
  public boolean mouseReleased(double x, double y, int button) {
    return true;
  }

  @Override
  public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
    return true;
  }

  @Override
  public boolean keyPressed(int key, int scan, int modifiers) {
    if (selected >= 0) {
      if (key == 256) {
        selected = -1;
        return true;
      }
      if (key == 65 || key == 263) {
        cycle(-1);
        return true;
      }
      if (key == 68 || key == 262) {
        cycle(1);
        return true;
      }
    }
    if (key == 256 || minecraft.options.keyInventory.matches(key, scan)) {
      onClose();
      return true;
    }
    return super.keyPressed(key, scan, modifiers);
  }

  @Override
  public void onClose() {
    if (minecraft.player == null) {
      minecraft.setScreen(null);
      return;
    }
    minecraft.player.closeContainer();
    minecraft.setScreen(new InventoryScreen(minecraft.player));
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private final class SocketButton extends AbstractButton {
    final CyberLayout.Socket socket;

    SocketButton(CyberLayout.Socket slot) {
      super(
          layout.screenX(slot.x()),
          layout.screenY(slot.y()),
          layout.size(96),
          layout.size(96),
          slotName(slot));
      socket = slot;
    }

    @Override
    protected void updateWidgetNarration(
        net.minecraft.client.gui.narration.NarrationElementOutput out) {
      defaultButtonNarrationText(out);
    }

    @Override
    public void onPress() {
      selected = socket.index();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {}
  }

  private void cycle(int direction) {
    var c = CyberwareCategory.forSlot(selected);
    selected =
        CyberwareCategory.values()[
            Math.floorMod(c.ordinal() + direction, CyberwareCategory.values().length)]
            .firstSlot();
  }

  private final class CategoryButton extends AbstractButton {
    final int direction;

    CategoryButton(int direction) {
      super(
          layout.screenX(direction < 0 ? 402 : 882),
          layout.screenY(148),
          layout.size(50),
          layout.size(48),
          Component.translatable(
              direction < 0 ? "cybernetics2077.previous" : "cybernetics2077.next"));
      this.direction = direction;
    }

    @Override
    protected void updateWidgetNarration(
        net.minecraft.client.gui.narration.NarrationElementOutput out) {
      defaultButtonNarrationText(out);
    }

    @Override
    public void onPress() {
      if (selected >= 0) cycle(direction);
    }

    @Override
    protected void renderWidget(GuiGraphics g, int x, int y, float p) {}
  }

  private final class BackButton extends AbstractButton {
    BackButton() {
      super(
          layout.screenX(80),
          layout.screenY(1005),
          layout.size(310),
          layout.size(42),
          Component.translatable("cybernetics2077.button.back"));
    }

    @Override
    protected void updateWidgetNarration(
        net.minecraft.client.gui.narration.NarrationElementOutput out) {
      defaultButtonNarrationText(out);
    }

    @Override
    public void onPress() {
      if (selected >= 0) selected = -1;
      else onClose();
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partial) {}
  }
}
