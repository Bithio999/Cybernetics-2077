package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.city.*;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Compact dossiers, stable widgets and explicit actions; no player inventory is obscured by text.
 */
public final class FixerScreen extends AbstractContainerScreen<FixerMenu> {
  private CyberActionButton accept, abandon;
  private final OfferButton[] cards = new OfferButton[4];
  private int tab, selection, ticks, confirmUntil, leftWidth;
  private String lastJob = "", lastReport = "";
  private boolean initialized;
  private CompoundTag state = new CompoundTag();
  private static final int GREEN = 0xff9af2b6, MUTED = 0xff96b4a4, GOLD = 0xffe7bf73;

  public FixerScreen(FixerMenu menu, Inventory inventory, Component title) {
    super(menu, inventory, title);
    imageWidth = 340;
    imageHeight = 220;
    inventoryLabelY = titleLabelY = -1000;
  }

  @Override
  protected void init() {
    imageWidth = Math.min(340, width - 12);
    imageHeight = 220;
    super.init();
    leftWidth = Math.min(108, (imageWidth - 36) / 3);
    String[] names = {"BOUNTIES", "DELIVERY", "DEBRIEF"};
    int tw = (imageWidth - 28) / 3;
    for (int i = 0; i < 3; i++) {
      final int t = i;
      addRenderableWidget(
          new TabButton(leftPos + 12 + i * (tw + 2), topPos + 40, tw, 18, names[i], t));
    }
    for (int i = 0; i < 4; i++) {
      cards[i] = addRenderableWidget(new OfferButton(i));
    }
    accept =
        addRenderableWidget(
            new CyberActionButton(
                leftPos + leftWidth + 20,
                topPos + 194,
                imageWidth - leftWidth - 108,
                18,
                Component.literal("ACCEPT BOUNTY"),
                this::mainAction));
    abandon =
        addRenderableWidget(
            new CyberActionButton(
                leftPos + 12,
                topPos + 194,
                leftWidth,
                18,
                Component.literal("ABANDON"),
                () -> {
                  if (ticks < confirmUntil) {
                    send(3);
                    confirmUntil = 0;
                  } else confirmUntil = ticks + 60;
                }));
    addRenderableWidget(
        new CyberActionButton(
            leftPos + imageWidth - 76,
            topPos + 194,
            64,
            16,
            Component.literal("LEAVE"),
            this::onClose));
  }

  private void send(int button) {
    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
  }

  private CompoundTag job() {
    return state.getCompound("job");
  }

  private CompoundTag offer(int slot) {
    var offers = state.getList("offers", Tag.TAG_COMPOUND);
    return slot >= 0 && slot < offers.size() ? offers.getCompound(slot) : new CompoundTag();
  }

  private CompoundTag selected() {
    return selection < 0 ? job() : offer(selection);
  }

  private void mainAction() {
    if (tab == 1) {
      send(1);
      return;
    }
    if (tab == 2) {
      tab = 0;
      return;
    }
    if (selection >= 0 && job().isEmpty()) send(state.getInt("acceptBase") + selection);
    else if (selection < 0 && job().getInt("stage") == 2) send(2);
    else if (selection < 0 && job().getInt("stage") == 3) send(3);
    else if (selection < 0 && !job().isEmpty()) send(4);
  }

  @Override
  protected void containerTick() {
    super.containerTick();
    ticks++;
  }

  private void update() {
    state = menu.board();
    String currentJob = job().hasUUID("token") ? job().getUUID("token").toString() : "";
    var report = state.getCompound("report");
    String currentReport = report.hasUUID("token") ? report.getUUID("token").toString() : "";
    if (!initialized && state.contains("offers")) {
      initialized = true;
      selection = currentJob.isEmpty() ? 0 : -1;
    } else if (!currentJob.equals(lastJob) && !currentJob.isEmpty()) {
      selection = -1;
      tab = 0;
    } else if (!currentReport.equals(lastReport) && !lastJob.isEmpty() && currentJob.isEmpty())
      tab = 2;
    if (currentJob.isEmpty() && selection < 0) selection = 0;
    lastJob = currentJob;
    lastReport = currentReport;
    boolean active = !job().isEmpty();
    int rows = active ? 4 : 3, height = 112 / rows - 2;
    for (int i = 0; i < 4; i++) {
      var card = cards[i];
      card.visible = tab == 0 && i < rows && initialized;
      card.setX(leftPos + 12);
      card.setY(topPos + 62 + i * (height + 2));
      card.setWidth(leftWidth);
      card.setHeight(height);
      card.slot = active ? i - 1 : i;
      card.dossier = card.slot < 0 ? job() : offer(card.slot);
      card.setMessage(Component.literal(card.dossier.getString("title")));
    }
    abandon.visible = tab == 0 && active;
    abandon.active = active;
    abandon.setMessage(Component.literal(ticks < confirmUntil ? "CONFIRM ABANDON" : "ABANDON JOB"));
    accept.active = initialized;
    if (tab == 1) {
      accept.active = menu.stage() != 2 && (menu.stage() == 1 || menu.hasRoute());
      accept.setMessage(
          Component.literal(
              menu.stage() == 2
                  ? "COMPLETED"
                  : menu.stage() == 1 ? "REISSUE CONTRACT" : "ACCEPT CONTRACT"));
    } else if (tab == 2) accept.setMessage(Component.literal("BACK TO BOUNTIES"));
    else if (!active) {
      accept.setMessage(
          Component.literal(
              offer(selection).getBoolean("used") ? "ALREADY TAKEN" : "ACCEPT BOUNTY"));
      accept.active &= selection >= 0 && !offer(selection).getBoolean("used");
    } else if (selection >= 0) {
      accept.setMessage(Component.literal("JOB ALREADY ACTIVE"));
      accept.active = false;
    } else if (job().getInt("stage") == 2) {
      boolean correct = state.getInt("profile") == job().getInt("profile");
      accept.setMessage(
          Component.literal(
              correct
                  ? "CLAIM PAYMENT"
                  : "RETURN TO " + (job().getInt("profile") == 0 ? "MARA" : "REN")));
      accept.active &= correct;
    } else
      accept.setMessage(
          Component.literal(job().getInt("stage") == 3 ? "CLEAR DOSSIER" : "REISSUE SHARD"));
  }

  @Override
  protected void renderBg(GuiGraphics g, float partial, int mouseX, int mouseY) {
    CyberBatch.begin();
    g.pose().pushPose();
    g.pose().translate(leftPos, topPos, 0);
    try {
      var c = new CyberCanvas(g, 1);
      ServiceStyle.frame(c, imageWidth, imageHeight, GREEN);
      c.text("AFTERLIFE / FIXER NETWORK", 17, 7, 12, GREEN);
      c.body(c.fit(title.getString(), imageWidth - 140, 9, false), 12, 28, 9, MUTED);
      c.right(
          "SC " + menu.streetCred() + " / €$" + String.format(Locale.ROOT, "%,d", menu.eddies()),
          imageWidth - 12,
          28,
          9,
          GOLD);
      if (tab == 0) bounty(c);
      else if (tab == 1) delivery(c);
      else report(c);
      String status = status();
      c.body(
          c.fit(status, imageWidth - 24, 8, false), 12, 184, 8, menu.result() == 0 ? MUTED : GOLD);
    } finally {
      CyberBatch.flush(g);
      g.pose().popPose();
    }
  }

  private void bounty(CyberCanvas c) {
    int x = leftWidth + 20, w = imageWidth - x - 12;
    ServiceStyle.surface(c, x, 62, w, 112);
    var t = selected();
    if (t.isEmpty()) {
      c.body("Connecting to fixer network...", x + 8, 80, 9, MUTED);
      return;
    }
    c.text(c.fit(t.getString("title"), w - 16, 11, true), x + 8, 73, 11, GREEN);
    c.body(
        BountyRules.rankName(t.getInt("rank"))
            + " / "
            + t.getInt("implants")
            + " implants / "
            + t.getInt("guards")
            + " guards",
        x + 8,
        91,
        8,
        MUTED);
    c.text(
        "€$"
            + String.format(Locale.ROOT, "%,d", t.getInt("eddies"))
            + " + bonuses / +"
            + t.getInt("cred")
            + " SC",
        x + 8,
        105,
        10,
        GOLD);
    line(c, BountyContracts.objective(t), x, 120, w, 8, GREEN);
    line(c, BountyRules.specialty(t.getInt("profile")), x, 132, w, 8, MUTED);
    c.body("No civilian kills / +10% payment", x + 8, 144, 8, MUTED);
    c.body("Survive the hunt / +10% payment", x + 8, 155, 8, MUTED);
    String objective =
        selection >= 0
            ? "Specialty bonus: +20% and +1 SC"
            : t.getInt("stage") == 2
                ? "OBJECTIVE COMPLETE // RETURN FOR PAYMENT"
                : t.getInt("stage") == 3
                    ? "MARK LOST // CLEAR OR ABANDON"
                    : t.getInt("stage") == 4
                        ? "RECOVER CACHE // USE CONTRACT SHARD"
                        : t.getBoolean("spawned")
                            ? "TARGET LOCATED // HOLD SCAN TO TRACK"
                            : "SEARCH THE LEAD // HOLD SCAN FOR ROUTE";
    line(c, objective, x, 166, w, 8, t.getInt("stage") == 2 ? GREEN : GOLD);
    int restock = state.getInt("restock");
    c.body(
        "DAWN " + String.format(Locale.ROOT, "%02d:%02d", restock / 60, restock % 60),
        12,
        176,
        8,
        MUTED);
    c.right(
        "LOYALTY " + (state.getInt("favor") % 3) + "/3 // THIRD JOB +25%",
        imageWidth - 12,
        176,
        8,
        MUTED);
  }

  private void delivery(CyberCanvas c) {
    ServiceStyle.surface(c, 12, 62, imageWidth - 24, 112);
    c.text("FIRST IMPRESSIONS", 22, 76, 13, GREEN);
    c.body("Deliver a sealed diagnostic packet to a ripperdoc.", 22, 99, 9, MUTED);
    c.body("Use the contract shard on the doctor to hand it over.", 22, 114, 9, MUTED);
    c.body("Hold scan for directions, even without optics.", 22, 130, 9, MUTED);
    c.text("€$1,000 / +5 STREET CRED / ONE-TIME", 22, 151, 10, GOLD);
    c.body(
        menu.stage() == 2
            ? "Completed"
            : menu.stage() == 1
                ? "Active // lost shard? Reissue below."
                : "A clean introduction to the clinic network.",
        22,
        166,
        8,
        MUTED);
  }

  private void report(CyberCanvas c) {
    var t = state.getCompound("report");
    ServiceStyle.surface(c, 12, 62, imageWidth - 24, 112);
    if (t.isEmpty()) {
      c.text("NO COMPLETED BOUNTIES", 22, 79, 12, GREEN);
      c.body("Finish a hunt and collect payment for a debrief.", 22, 103, 9, MUTED);
      return;
    }
    int split = 112;
    c.text("RATING " + t.getString("rating"), 22, 76, 22, GOLD);
    c.text("€$" + String.format(Locale.ROOT, "%,d", t.getInt("payout")), 22, 105, 14, GREEN);
    c.body("+" + t.getInt("credEarned") + " STREET CRED", 22, 124, 9, MUTED);
    c.body("Base " + t.getInt("eddies"), 22, 142, 8, MUTED);
    c.body("Bonus " + t.getInt("bonus") + " / Chain " + t.getInt("chainBonus"), 22, 156, 8, MUTED);
    int x = split + 32, w = imageWidth - x - 14;
    line(c, t.getString("title"), x - 8, 78, w + 8, 11, GREEN);
    c.body((t.getBoolean("specialty") ? "PASS" : "MISS") + " // Fixer specialty", x, 101, 9, MUTED);
    c.body(
        (t.getBoolean("civilians") ? "PASS" : "MISS") + " // No civilian kills", x, 116, 9, MUTED);
    c.body(
        (t.getBoolean("survival") ? "PASS" : "MISS") + " // Survived the hunt", x, 131, 9, MUTED);
    line(
        c,
        t.getBoolean("chip") ? "TIER 5 ICONIC CHIP AWARDED" : "PAYMENT CLEARED // DOSSIER CLOSED",
        x - 8,
        158,
        w + 8,
        8,
        GOLD);
    c.body(
        "ELITE CHAIN " + Math.min(3, state.getInt("legends")) + "/3 // ONE-TIME T5 ICONIC CHIP",
        12,
        176,
        8,
        MUTED);
  }

  private void line(CyberCanvas c, String text, int x, int y, int width, int size, int color) {
    c.body(c.fit(text, width - 16, size, false), x + 8, y, size, color);
  }

  private String status() {
    return switch (menu.result()) {
      case 1 -> "Visit a ripperdoc clinic, then return for the delivery.";
      case 2 -> "Free an inventory slot for the contract shard.";
      case 3 -> "The introductory delivery has already been completed.";
      case 10 -> "One bounty at a time. Finish or abandon your current job.";
      case 11 -> "That offer has already been taken today.";
      case 12 -> "Offers changed. Review the refreshed board before accepting.";
      case 13 -> "Bounty hunting is unavailable in Peaceful difficulty.";
      case 14 -> "Bounties require an Overworld fixer and a lead inside the world border.";
      case 20 -> "Complete your assigned objective before claiming payment.";
      case 21 -> "Return to the issuing fixer's network for payment.";
      default ->
          tab == 0
              ? "Accepted jobs survive dawn, logout and death. Optional bonuses do not affect the"
                  + " base payment."
              : tab == 1
                  ? "Virtual cargo // no separate delivery item required."
                  : "Optional objectives improve the rating; the base payment is guaranteed for a"
                      + " confirmed mark.";
    };
  }

  @Override
  protected void renderLabels(GuiGraphics g, int x, int y) {}

  @Override
  public void render(GuiGraphics g, int x, int y, float partial) {
    update();
    super.render(g, x, y, partial);
    renderTooltip(g, x, y);
  }

  private final class TabButton extends AbstractButton {
    private final int index;

    TabButton(int x, int y, int w, int h, String name, int index) {
      super(x, y, w, h, Component.literal(name));
      this.index = index;
    }

    @Override
    public void onPress() {
      tab = index;
      confirmUntil = 0;
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
        var c = new CyberCanvas(g, 1);
        boolean selected = tab == index;
        if (selected || isHoveredOrFocused()) c.rect(getX(), getY(), width, height, 0xff18312a);
        c.line(
            getX(),
            getY() + height - 1,
            getX() + width,
            getY() + height - 1,
            .65f,
            selected ? GREEN : ServiceStyle.LINE);
        c.center(
            getMessage().getString(),
            getX() + width / 2f,
            getY() + 5,
            10,
            selected ? GREEN : MUTED);
      } finally {
        CyberBatch.flush(g);
      }
    }
  }

  private final class OfferButton extends AbstractButton {
    int slot;
    CompoundTag dossier = new CompoundTag();

    OfferButton(int row) {
      super(0, 0, 10, 10, Component.literal("Bounty dossier"));
    }

    @Override
    public void onPress() {
      selection = slot;
      confirmUntil = 0;
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
        var c = new CyberCanvas(g, 1);
        boolean selected = selection == slot;
        if (selected || isHoveredOrFocused()) c.rect(getX(), getY(), width, height, 0xff18312a);
        c.rect(getX(), getY() + 3, 1, height - 6, selected ? GREEN : ServiceStyle.LINE);
        c.line(
            getX() + 4, getY() + height, getX() + width, getY() + height, .5f, ServiceStyle.LINE);
        c.text(
            c.fit(
                slot < 0 ? "CURRENT JOB" : BountyRules.Kind.from(dossier.getString("kind")).label,
                width - 12,
                9,
                true),
            getX() + 6,
            getY() + 4,
            9,
            GREEN);
        c.body(
            dossier.getBoolean("used")
                ? "TAKEN TODAY"
                : slot < 0
                    ? (dossier.getInt("stage") == 2 ? "READY FOR PAYMENT" : "TRACK DOSSIER")
                    : BountyRules.rankName(dossier.getInt("rank"))
                        + " / €$"
                        + dossier.getInt("eddies"),
            getX() + 6,
            getY() + height - 11,
            8,
            GOLD);
      } finally {
        CyberBatch.flush(g);
      }
    }
  }
}
