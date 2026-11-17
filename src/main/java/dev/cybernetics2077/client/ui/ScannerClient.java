package dev.cybernetics2077.client.ui;

import static dev.cybernetics2077.client.ui.Geometry.*;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.scanner.ScannerNetwork;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Optical data and server-authorized cyberdeck uploads share the held scanner overlay. */
@EventBusSubscriber(modid = Cybernetics2077.ID, value = Dist.CLIENT)
public final class ScannerClient {
  public static final KeyMapping KEY =
      new KeyMapping(
          "key.cybernetics2077.scan",
          net.neoforged.neoforge.client.settings.KeyConflictContext.IN_GAME,
          com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
          GLFW.GLFW_KEY_V,
          "key.categories.cybernetics2077");
  private static final int RED = 0xffff3742, CYAN = 0xff65e5f5, MUTED = 0xff7badae;
  private static boolean installed, down, prepared;
  private static Object level;
  private static ScannerMotion motion = new ScannerMotion();
  private static ScannerMotion.Frame frame = new ScannerMotion.Frame(0, 0, false);
  private static CompoundTag target = new CompoundTag();
  private static long received;
  private static int focus = -1, focusTicks;
  private static long lastHack;

  public static void quickhack() {
    var own = dev.cybernetics2077.client.mantis.CyberwareClient.own();
    long now = System.nanoTime();
    if (active()
        && frame.data() > .95
        && focus >= 0
        && own.getBoolean("shortCircuit")
        && own.getFloat("ram") >= 2
        && now - lastHack > 180_000_000L) {
      lastHack = now;
      PacketDistributor.sendToServer(new dev.cybernetics2077.combat.CyberwareNetwork.Hack(focus));
    }
  }

  @EventBusSubscriber(
      modid = Cybernetics2077.ID,
      value = Dist.CLIENT,
      bus = EventBusSubscriber.Bus.MOD)
  public static final class Setup {
    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
      e.register(KEY);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
      e.enqueueWork(() -> ScannerNetwork.receiver = ScannerClient::receive);
    }
  }

  public static void receive(CompoundTag data) {
    installed = data.getBoolean("installed");
    if (data.contains("id")) {
      target = data;
      received = System.nanoTime();
    } else target = new CompoundTag();
  }

  public static boolean available() {
    return installed;
  }

  public static boolean active() {
    var m = Minecraft.getInstance();
    return down && installed && m.player != null && m.screen == null && !m.options.hideGui;
  }

  public static ScannerMotion.Frame hudMotion() {
    var m = Minecraft.getInstance();
    return m.player != null && m.level != null && m.screen == null && !m.options.hideGui
        ? frame
        : new ScannerMotion.Frame(0, 0, false);
  }

  public static boolean outlined(Entity e) {
    return active() && frame.data() >= .98 && e.getId() == focus;
  }

  private static void held() {
    var m = Minecraft.getInstance();
    boolean value =
        installed
            && KEY.isDown()
            && m.player != null
            && m.player.isAlive()
            && m.screen == null
            && !m.options.hideGui;
    if (down != value) {
      down = value;
      motion.setHeld(value, System.nanoTime());
      if (!down) {
        while (m.options.keyAttack.consumeClick()) {}
        while (m.options.keyUse.consumeClick()) {}
      }
    }
  }

  @SubscribeEvent
  public static void tick(ClientTickEvent.Post e) {
    var m = Minecraft.getInstance();
    if (level != m.level) {
      // A login payload can arrive just before the first level tick; do not discard that payload.
      if (m.level == null) installed = false;
      target = new CompoundTag();
      level = m.level;
      focus = -1;
      down = false;
      motion = new ScannerMotion();
      prepared = false;
    }
    held();
    if (!active() || m.level == null) return;
    var eye = m.player.getEyePosition();
    var end = eye.add(m.player.getLookAngle().scale(32));
    LivingEntity hit = null;
    double nearest = Double.MAX_VALUE;
    for (var entity :
        m.level.getEntitiesOfClass(
            LivingEntity.class,
            m.player.getBoundingBox().inflate(32),
            t -> t != m.player && t.isAlive() && !t.isSpectator() && !t.isInvisibleTo(m.player))) {
      var clip = entity.getBoundingBox().inflate(.24).clip(eye, end);
      if (clip.isPresent()
          && m.player.hasLineOfSight(entity)
          && eye.distanceToSqr(clip.get()) < nearest) {
        nearest = eye.distanceToSqr(clip.get());
        hit = entity;
      }
    }
    if (hit != null) {
      focus = hit.getId();
      focusTicks = 4;
    } else if (--focusTicks <= 0) {
      focus = -1;
      target = new CompoundTag();
    }
    if (m.player.tickCount % 4 == 0 && focus >= 0)
      PacketDistributor.sendToServer(new ScannerNetwork.Request(focus));
  }

  @SubscribeEvent
  public static void scroll(net.neoforged.neoforge.client.event.InputEvent.MouseScrollingEvent e) {
    if (active()) e.setCanceled(true);
  }

  @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
  public static void beforeHud(RenderGuiEvent.Pre e) {
    held();
    long now = System.nanoTime();
    motion.renderFrame(now);
    frame = motion.sample(now);
  }

  @SubscribeEvent
  public static void render(RenderGuiEvent.Post e) {
    var m = Minecraft.getInstance();
    if (m.player == null || m.level == null || m.screen != null || m.options.hideGui) return;
    var g = e.getGuiGraphics();
    CyberBatch.begin();
    try {
      if (installed && !prepared) {
        prepared = true;
        m.levelRenderer.requestOutlineEffect();
        g.pose().pushPose();
        g.pose().translate(-8192, -8192, 0);
        scanner(new CyberCanvas(g, 1), new ScannerMotion.Frame(1000, 1, true));
        g.pose().popPose();
      }
      if (frame.lens() <= 0) return;
      float w = g.guiWidth(), h = g.guiHeight();
      var full = new CyberCanvas(g, (float) frame.lens());
      full.rect(0, 0, w, h, 0x24134732);
      full.gradient(0, 0, w * .28f, h, 0xa636100e, 0x0036100e, 0x0036100e, 0xa636100e);
      full.gradient(w * .72f, 0, w * .28f, h, 0x0036100e, 0xa636100e, 0xa636100e, 0x0036100e);
      full.gradient(0, 0, w, h * .22f, 0x93200e0c, 0x93200e0c, 0x00200e0c, 0x00200e0c);
      full.gradient(0, h * .78f, w, h * .22f, 0x00200e0c, 0x00200e0c, 0x93200e0c, 0x93200e0c);
      for (float y = 0; y < h; y += 2.5f) full.rect(0, y, w, .35f, 0x072e0808);
      var layout = CyberLayout.fit((int) w, (int) h);
      g.pose().pushPose();
      g.pose().translate(layout.left(), layout.top(), 0);
      g.pose().scale((float) layout.scale(), (float) layout.scale(), 1);
      scanner(new CyberCanvas(g, 1), frame);
      g.pose().popPose();
    } finally {
      CyberBatch.flush(g);
    }
  }

  private static void scanner(CyberCanvas root, ScannerMotion.Frame f) {
    var lens = root.fade(f.lens());
    var chrome = root.fade(f.chrome());
    float r =
        (float) (100 + 365 * net.minecraft.util.Mth.smoothstep(Math.clamp(f.age() / 450, 0, 1)));
    lens.glowArc(960, 540, r, 0, (float) (Math.PI * 2), .8f, 0x85a8272e);
    lens.glowArc(960, 540, 57, 0, (float) (Math.PI * 2), .85f, 0xbfff3742);
    lens.arc(960, 540, 45, 0, (float) (Math.PI * 2), .85f, 0x80ff3742);
    lens.line(944, 540, 976, 540, .85f, RED);
    lens.stroke(List.of(new Point(953, 538), new Point(967, 538), new Point(960, 550)), .85f, RED);
    for (int row = 0; row < 3; row++)
      for (int col = 0; col < 5; col++) {
        float x = 775 + 92.5f * col, y = 432 + 108 * row;
        var p =
            List.of(
                new Point(x - 7, y - 7),
                new Point(x + 7, y - 7),
                new Point(x + 7, y + 7),
                new Point(x - 7, y + 7));
        chrome.halo(p, RED);
        chrome.stroke(p, .85f, 0xcfff3742);
      }
    for (int k = 0; k < 13; k++) {
      float y = 455 + k * 18;
      chrome.glowLine(716, y - 3, 725, y, .85f, 0xcfff3742);
      chrome.line(725, y, 716, y + 3, .85f, 0xcfff3742);
      chrome.glowLine(1213, y - 3, 1204, y, .85f, 0xcfff3742);
      chrome.line(1204, y, 1213, y + 3, .85f, 0xcfff3742);
    }
    chrome.text("1x", 675, 540, 17, RED);
    var mc = Minecraft.getInstance();
    var entity = mc.level.getEntity(focus);
    chrome.text(
        entity == null ? "—" : String.format(Locale.ROOT, "%.1f m", mc.player.distanceTo(entity)),
        1237,
        540,
        17,
        RED);
    edge(chrome, f);
    var link = root.fade(f.resource());
    link.dots(695, 111, 3, 5, RED);
    var equipment = dev.cybernetics2077.client.mantis.CyberwareClient.own();
    boolean deck = equipment.getBoolean("deck"), chip = equipment.getBoolean("shortCircuit");
    link.glowText(deck ? "CYBERDECK RAM" : "OPTICAL LINK", 720, 106, 23, CYAN);
    if (deck) {
      float ram = equipment.getFloat("ram");
      for (int i = 0; i < 4; i++) {
        float x = 997 + i * 38;
        link.shape(Geometry.panel(x, 111, 29, 22, 4), 0xff10232b, 0xff4b8994);
        link.rect(x + 3, 114, 23 * Math.clamp(ram - i, 0, 1), 16, 0xff74e2ed);
        link.line(x + 8, 115, x + 8, 129, .8f, 0x9a142e39);
        link.line(x + 20, 115, x + 20, 129, .8f, 0x9a142e39);
      }
      link.right(String.format(Locale.ROOT, "%.1f / 4", ram), 1255, 111, 16, 0xffb3dce2);
    } else link.right("TIER 1", 1255, 111, 16, 0xffb3dce2);
    link.glowLine(695, 151, 1255, 151, 1, 0x89e72c38);
    var left = root.fade(f.layer(420, 230));
    left.tracked(deck ? "AVAILABLE QUICKHACKS" : "OPTICAL SCANNER", 177, 315, 19, CYAN, 1);
    left.line(177, 352, 552, 352, 1, 0x99cf3c45);
    if (chip) {
      boolean ready =
          focus >= 0
              && equipment.getFloat("ram") >= 2
              && equipment.getList("uploads", 10).size() < 2;
      left.shape(
          Geometry.data(177, 370, 375, 77),
          ready ? 0xd4371b28 : 0xd0141923,
          ready ? 0xffe85565 : 0xff4c5963);
      left.rect(177, 370, 4, 77, ready ? RED : 0xff64727a);
      left.glowText("SHORT CIRCUIT", 245, 384, 24, ready ? 0xfff07983 : 0xff8799a5);
      left.right("2", 532, 386, 23, CYAN);
      left.tracked("RAM", 505, 417, 11, MUTED, .8f);
      CyberGlyphs.quickhack(left, 194, 389, 33, ready ? 0xffff7882 : 0xff617889);
      left.body("8 electrical damage  /  1 s upload", 177, 471, 17, 0xffa9c9d2);
      left.body(
          ready
              ? "[LMB] UPLOAD SHORT CIRCUIT"
              : focus < 0 ? "AIM AT A LIVING TARGET" : "RAM OR QUEUE UNAVAILABLE",
          177,
          501,
          15,
          ready ? CYAN : MUTED);
      left.tracked("2 UPLOADS MAX  /  RAM REGENERATES", 177, 537, 11, MUTED, .7f);
    } else {
      left.body(
          deck
              ? "NO QUICKHACKS INSTALLED"
              : entity == null ? "SEARCHING FOR A TARGET" : "LIVE TARGET ACQUIRED",
          177,
          373,
          16,
          0xffc5898f);
      left.tracked(deck ? "MANAGE CHIPS AT THE BENCH" : "RANGE / 32 m", 177, 405, 12, MUTED, .8f);
    }
    data(root.fade(f.data()), f, entity);
    var controls = root.fade(f.layer(550, 180));
    controls.right(
        "RELEASE "
            + KEY.getTranslatedKeyMessage().getString().toUpperCase(Locale.ROOT)
            + "  /  CLOSE SCANNER",
        1810,
        1005,
        21,
        RED);
  }

  private static void edge(CyberCanvas c, ScannerMotion.Frame f) {
    String timer =
        String.format(
            Locale.ROOT, "%02d:%02d", (int) (f.age() / 60000), (int) (f.age() / 1000) % 60);
    var micro = c.fade(.55);
    for (int x : new int[] {55, 1840}) {
      micro.dots(x, 38, 3, 2, RED);
      micro.dots(x, 1017, 3, 2, RED);
    }
    micro.tracked(timer, 55, 51, 10, RED, 1);
    micro.tracked("LINK", 55, 62, 10, RED, 1);
    micro.tracked(timer, 1832, 38, 10, RED, 1);
    micro.tracked("LINK", 55, 1031, 10, RED, 1);
    micro.tracked(timer, 1832, 1031, 10, RED, 1);
    c.g.pose().pushPose();
    c.g.pose().translate(190, 104, 0);
    c.g.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(3));
    c.tracked("SCANNER", 0, 0, 13, 0xb3f3413e, .8f);
    c.tracked("OPTICS", 99, 6, 8, 0x80f3413e, .7f);
    c.line(0, 22, 187, 22, 1, 0x99ab272a);
    c.g.pose().popPose();
    c.glowLine(1549, 131, 1730, 121, .85f, 0x99b1272a);
    c.glowLine(190, 950, 371, 940, .85f, 0x99b1272a);
  }

  private static void data(CyberCanvas c, ScannerMotion.Frame f, Entity entity) {
    if (c.opacity <= 0) return;
    boolean has =
        target.contains("id")
            && target.getInt("id") == focus
            && entity != null
            && System.nanoTime() - received < 1_200_000_000L;
    c.g.pose().pushPose();
    c.g.pose().translate((1 - ease((f.age() - 500) / 230)) * 12, 0, 0);
    var tab =
        List.of(
            new Point(1390, 299),
            new Point(1584, 299),
            new Point(1593, 308),
            new Point(1593, 347),
            new Point(1390, 347));
    c.polygon(tab, 0xf20b0e1e);
    for (int i = 0; i < tab.size(); i++)
      if (i != 3) {
        var a = tab.get(i);
        var b = tab.get((i + 1) % tab.size());
        c.line(a.x(), a.y(), b.x(), b.y(), .85f, 0xff61d9ed);
      }
    c.text("DATA", 1403, 308, 25, 0xff70e9fa);
    c.shape(Geometry.data(1390, 347, 418, 475), 0xf2090b1a, 0xff499bb2);
    c.text(
        has ? c.fit(target.getString("name").toUpperCase(Locale.ROOT), 378, 28, true) : "NO TARGET",
        1410,
        369,
        28,
        0xffc8eff5);
    String kind =
        entity instanceof net.minecraft.world.entity.player.Player
            ? "PLAYER"
            : entity instanceof net.minecraft.world.entity.monster.Enemy
                ? "HOSTILE"
                : "LIVING ENTITY";
    c.tracked(has ? kind : "AIM AT A LIVING TARGET", 1410, 414, 14, 0xffb4e1eb, .8f);
    c.body(
        has ? c.fit(target.getString("type"), 378, 16, false) : "Line of sight required",
        1410,
        447,
        16,
        MUTED);
    c.line(1410, 480, 1788, 480, 1, 0x88426576);
    float hp = has ? target.getFloat("hp") : 0, max = has ? target.getFloat("maxHp") : 0;
    c.body("HEALTH", 1410, 503, 19, CYAN);
    c.right(has ? number(hp) + " / " + number(max) : "—", 1788, 503, 19, 0xffd3e2e4);
    c.rect(1410, 535, 378, 3, 0x5237657b);
    c.rect(1410, 535, (float) (378 * Math.clamp(hp / Math.max(1, max), 0, 1)), 3, RED);
    String[] labels = {"ARMOR", "ATTACK DAMAGE", "ABSORPTION"};
    String[] keys = {"armor", "attack", "absorption"};
    for (int i = 0; i < 3; i++) {
      c.body(labels[i], 1410, 560 + i * 35, 17, 0xff84d9e6);
      c.right(has ? number(target.getDouble(keys[i])) : "—", 1788, 560 + i * 35, 19, 0xffd3e2e4);
    }
    c.line(1410, 671, 1788, 671, 1, 0x80436a79);
    c.tracked("ACTIVE EFFECTS", 1410, 690, 15, CYAN, .5f);
    var effects = target.getList("effects", 8);
    if (has && effects.isEmpty()) c.body("No visible status effects", 1410, 721, 17, MUTED);
    for (int i = 0; i < Math.min(2, effects.size()); i++)
      c.body(
          c.fit(
              net.minecraft.client.resources.language.I18n.get(effects.getString(i)),
              378,
              17,
              false),
          1410,
          721 + i * 27,
          17,
          0xffc9dce3);
    if (effects.size() > 2) c.body("+" + (effects.size() - 2) + " more", 1410, 780, 13, MUTED);
    c.tracked("CYBERNETICS 2077 // OPTICAL DATA", 1412, 838, 12, 0xff3c7d93, 1.2f);
    c.g.pose().popPose();
  }

  private static String number(double n) {
    return String.format(Locale.ROOT, n == (long) n ? "%,.0f" : "%,.1f", n);
  }
}
