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
  private static int scanRange = 32;
  private static Object level;
  private static ScannerMotion motion = new ScannerMotion();
  private static ScannerMotion.Frame frame = new ScannerMotion.Frame(0, 0, false);
  private static CompoundTag target = new CompoundTag();
  private static long received;
  private static int focus = -1, focusTicks;
  private static long lastHack;
  private static final org.joml.Matrix4f worldView = new org.joml.Matrix4f(),
      worldProjection = new org.joml.Matrix4f();
  private static net.minecraft.world.phys.Vec3 worldCamera = net.minecraft.world.phys.Vec3.ZERO;
  private static boolean matrices;

  @SubscribeEvent
  public static void capture(RenderLevelStageEvent e) {
    if (e.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
      worldView.set(e.getModelViewMatrix());
      worldProjection.set(e.getProjectionMatrix());
      worldCamera = e.getCamera().getPosition();
      matrices = true;
    }
  }

  public static void quickhack() {
    var own = dev.cybernetics2077.client.mantis.CyberwareClient.own();
    long now = System.nanoTime();
    if (active()
        && frame.data() > .95
        && focus >= 0
        && own.getBoolean("shortCircuit")
        && own.getFloat("ram") >= own.getInt("hackCost")
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
    scanRange = Math.clamp(data.getInt("range"), 0, 64);
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
    return active() && frame.data() >= .98 && e.getId() == focus
        || dev.cybernetics2077.client.ui.AugmentClient.highlighted(e);
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
      if (m.player != null) dev.cybernetics2077.client.ui.AugmentClient.action(down ? 10 : 11);
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
      matrices = false;
    }
    held();
    if (!active() || m.level == null) return;
    if (m.player.tickCount % 8 == 0) dev.cybernetics2077.client.ui.AugmentClient.action(10);
    var eye = m.player.getEyePosition();
    var end = eye.add(m.player.getLookAngle().scale(scanRange));
    LivingEntity hit = null;
    double nearest = Double.MAX_VALUE;
    for (var entity :
        m.level.getEntitiesOfClass(
            LivingEntity.class,
            m.player.getBoundingBox().inflate(scanRange),
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
      overhead(g, w, h);
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
    var equipment = dev.cybernetics2077.client.mantis.CyberwareClient.own();
    boolean deck = equipment.getBoolean("deck"), chip = equipment.getBoolean("shortCircuit");
    resource(root.fade(f.resource()), f, equipment, deck);
    hacks(root, f, equipment, deck, chip);
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

  private static void resource(
      CyberCanvas c, ScannerMotion.Frame f, CompoundTag equipment, boolean deck) {
    c.dots(693, 97, 3, 5, 0x99ff323e);
    if (!deck) {
      c.glowText("OPTICAL LINK", 719, 108, 23, RED);
      return;
    }
    float ram = equipment.getFloat("ram");
    int max = Math.max(1, equipment.getInt("ramMax"));
    c.glowText(String.format(Locale.ROOT, "CYBERDECK RAM: %.1f / %d", ram, max), 719, 108, 23, RED);
    c.glowLine(693, 151, 1257, 151, 1, 0xcfe72c38);
    c.tracked("NEURAL RESERVE", 696, 146, 7, 0x99a52a30, 1);
    for (int k = 0; k < max; k++) {
      var cell = c.fade(f.layer(230 + k * 4, 140) / Math.max(.0001, f.resource()));
      float x = 720 + k * 17.55f, y = 163, w = 13.55f;
      var path = Geometry.pip(x, y, w, 44);
      float amount = Math.clamp(ram - k, 0, 1);
      cell.shape(path, 0xd9190a0d, amount > 0 ? 0xff56dceb : 0xd9ff323e);
      if (amount > 0) {
        cell.polygon(path, amount >= 1 ? 0xf256dceb : 0x8056dceb);
        cell.rect(x + 3, y + 4, 3, 4, 0xd0043e53);
        cell.line(x + 1, y + 1, x + w - 1, y + 1, .65f, 0xb3b3f7ff);
      }
    }
  }

  private static void hacks(
      CyberCanvas root, ScannerMotion.Frame f, CompoundTag equipment, boolean deck, boolean chip) {
    var title = root.fade(f.hacks());
    title.dots(181, 297, 3, 5, 0xb3cd3538);
    title.tracked("NEURAL INTERFACE v.01", 195, 293, 12, 0xb3ac3335, .7f);
    title.glowText(deck ? "AVAILABLE QUICKHACKS:" : "OPTICAL SCANNER", 200, 315, 26, CYAN);
    title.glowLine(200, 361, 598, 361, 1, 0xffd62f39);
    if (!chip) {
      title.body(deck ? "NO QUICKHACKS INSTALLED" : "OPTICAL DATA ONLY", 230, 389, 17, MUTED);
      return;
    }
    var c = root.fade(f.row(0));
    float x = 210, y = 379, w = 410;
    boolean poor = equipment.getFloat("ram") < equipment.getInt("hackCost"),
        queued = !equipment.getList("uploads", 10).isEmpty();
    int tint = poor ? 0xffff565c : CYAN;
    var path = Geometry.chip(x, y, w, 57);
    c.halo(path, poor ? RED : CYAN);
    c.shape(path, poor ? 0xf0421117 : 0xf2123c4f, poor ? 0xffd7414c : 0xff70f2ff);
    c.glowText("SHORT CIRCUIT  /  T" + equipment.getInt("hackTier"), x + 23, y + 7, 22, tint);
    String status = queued ? "QUEUED" : poor ? "LOW RAM" : "READY";
    float badge = c.width(status, 12, true) + 8;
    c.stroke(Geometry.panel(x + 23, y + 35, badge, 16, 0), .7f, tint);
    c.text(status, x + 27, y + 37, 12, tint);
    c.stroke(Geometry.panel(x + 28 + badge, y + 35, 58, 16, 0), .7f, 0xfff7b05e);
    c.text("COMBAT", x + 32 + badge, y + 37, 11, 0xfff7b05e);
    float iconX = x + w - 49;
    c.stroke(
        List.of(
            new Point(iconX, y + 4.5f),
            new Point(iconX + 45, y + 4.5f),
            new Point(iconX + 45, y + 52.5f),
            new Point(iconX, y + 52.5f)),
        .75f,
        tint);
    c.image(
        Cybernetics2077.id("textures/item/short_circuit_tier_1.png"),
        iconX + 5.5f,
        y + 10.5f,
        34,
        36,
        0xffffffff);
    c.right("" + equipment.getInt("hackCost"), iconX - 35, y + 10, 31, tint);
    c.line(iconX - 29, y + 16, iconX - 29, y + 38, .75f, tint);
    c.line(iconX - 9, y + 16, iconX - 9, y + 38, .75f, tint);
    c.text("RAM", iconX - 26, y + 23, 9, tint);
    var help = root.fade(f.layer(570, 180));
    help.g.pose().pushPose();
    help.g.pose().translate(231, 466, 0);
    help.g.pose().scale(.65f, .65f, 1);
    help.mouse(0, 0, 0);
    help.g.pose().popPose();
    help.tracked("CLICK TO UPLOAD", 252, 468, 13, 0xffd35a60, .6f);
    help.body(
        String.format(
            Locale.ROOT,
            "%.1f ELECTRICAL DAMAGE / %.1f s UPLOAD",
            equipment.getFloat("hackDamage"),
            equipment.getInt("uploadTicks") / 20f),
        230,
        503,
        14,
        MUTED);
  }

  private static void overhead(net.minecraft.client.gui.GuiGraphics g, float w, float h) {
    var m = Minecraft.getInstance();
    if (!matrices || m.level == null) return;
    Map<Integer, List<CompoundTag>> groups = new LinkedHashMap<>();
    for (var n : dev.cybernetics2077.client.mantis.CyberwareClient.own().getList("uploads", 10)) {
      var q = (CompoundTag) n;
      groups.computeIfAbsent(q.getInt("target"), id -> new ArrayList<>()).add(q);
    }
    if (active() && focus >= 0) groups.computeIfAbsent(focus, id -> new ArrayList<>());
    for (var group : groups.entrySet()) {
      var entity = m.level.getEntity(group.getKey());
      if (entity == null) continue;
      var at =
          entity
              .getPosition(m.getTimer().getGameTimeDeltaPartialTick(false))
              .add(0, entity.getBbHeight() + .5, 0)
              .subtract(worldCamera);
      var clip =
          new org.joml.Vector4f((float) at.x, (float) at.y, (float) at.z, 1)
              .mul(worldView)
              .mul(worldProjection);
      if (clip.w <= 0) continue;
      clip.div(clip.w);
      if (Math.abs(clip.x) > 1.1 || Math.abs(clip.y) > 1.1) continue;
      g.pose().pushPose();
      g.pose().translate((clip.x + 1) * w / 2, (1 - clip.y) * h / 2, 0);
      float scale = Math.min(w / 1920, h / 1080);
      g.pose().scale(scale, scale, 1);
      var c = new CyberCanvas(g, (float) frame.chrome());
      String name = c.fit(entity.getName().getString().toUpperCase(Locale.ROOT), 440, 25, true);
      c.glowText(name, -c.width(name, 25, true) / 2, -37, 25, 0xff71e9fa);
      c.line(-5, -8, 0, -2, 1, CYAN);
      c.line(0, -2, 5, -8, 1, CYAN);
      var queue = group.getValue();
      double now = m.level.getGameTime() + m.getTimer().getGameTimeDeltaPartialTick(false);
      for (int k = 0; k < queue.size(); k++) {
        var q = queue.get(k);
        float x = -queue.size() * 25 + 4 + k * 50, y = -98;
        float progress = (float) Math.clamp((now - q.getLong("start")) / 20, 0, 1);
        var tile = Geometry.data(x, y, 42, 42);
        c.halo(tile, RED);
        c.shape(tile, 0xde3d131a, 0xfffa3b46);
        if (now >= q.getLong("start"))
          c.rect(x + 1, y + 41 - 40 * progress, 40, 40 * progress, 0x807c323d);
        c.image(
            Cybernetics2077.id("textures/item/short_circuit_tier_1.png"),
            x + 6,
            y + 5,
            30,
            30,
            RED);
        if (now >= q.getLong("start")) c.text((int) (progress * 100) + "%", x - 1, y - 20, 16, RED);
      }
      g.pose().popPose();
    }
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
