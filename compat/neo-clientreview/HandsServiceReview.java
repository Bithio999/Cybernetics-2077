package dev.cybernetics2077.compat.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.bench.CyberneticsBenchMenu;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.client.GuidedBulletClient;
import dev.cybernetics2077.client.ui.*;
import dev.cybernetics2077.combat.*;
import dev.cybernetics2077.compat.TacZCompat;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.CyberdeckMenu;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native screen/ADS/trajectory review; never included in published builds. */
public final class HandsServiceReview {
  private static volatile Throwable failure;
  private static volatile int doc, fixer, round = -1;
  private static volatile UUID roundUuid;
  private static int managedFrames;
  private static Vec3 last;

  private static void check(boolean value, String message) {
    if (!value) throw new IllegalStateException(message);
  }

  private static void server(Consumer<ServerPlayer> action) {
    var mc = Minecraft.getInstance();
    mc.getSingleplayerServer()
        .execute(
            () -> {
              try {
                action.accept(
                    mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()));
              } catch (Throwable t) {
                failure = t;
              }
            });
  }

  private static Object call(Object o, String method, Object... args) throws Exception {
    return Arrays.stream(o.getClass().getMethods())
        .filter(m -> m.getName().equals(method) && m.getParameterCount() == args.length)
        .findFirst()
        .orElseThrow()
        .invoke(o, args);
  }

  private static void button(String text) {
    var mc = Minecraft.getInstance();
    mc.screen.children().stream()
        .filter(AbstractButton.class::isInstance)
        .map(AbstractButton.class::cast)
        .filter(b -> b.active && b.visible && b.getMessage().getString().equals(text))
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("Button missing " + text))
        .onPress();
  }

  private static void shot(Path out, String name) {
    Minecraft.getInstance().getToasts().clear();
    Screenshot.grab(
        out.toFile(), name + ".png", Minecraft.getInstance().getMainRenderTarget(), m -> {});
  }

  private static void close() {
    var mc = Minecraft.getInstance();
    mc.player.closeContainer();
    mc.setScreen(null);
  }

  private static void openDoc(ServerPlayer p) {
    p.openMenu(
        new SimpleMenuProvider(
            (id, inv, o) -> new RipperdocMenu(id, inv, doc), Component.literal("Ripperdoc")),
        b -> b.writeVarInt(doc));
  }

  private static void equip(ServerPlayer p, Augment a, int t, int n) {
    p.getData(Cybernetics2077.CYBERWARE)
        .setStackInSlot(a.category.firstSlot() + n, new ItemStack(Cybernetics2077.augment(a, t)));
  }

  private static void sync(ServerPlayer p) {
    dev.cybernetics2077.scanner.ScannerNetwork.sync(p);
    CyberwareRuntime.broadcast(p);
  }

  private static void aim(boolean value) throws Exception {
    ((KeyMapping) Class.forName("com.tacz.guns.client.input.AimKey").getField("AIM_KEY").get(null))
        .setDown(value);
    var p = Minecraft.getInstance().player;
    Object op =
        Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator")
            .getMethod("fromLocalPlayer", net.minecraft.client.player.LocalPlayer.class)
            .invoke(null, p);
    call(op, "aim", value);
  }

  private static void checkBounds() {
    var mc = Minecraft.getInstance();
    for (var child : mc.screen.children())
      if (child instanceof AbstractWidget widget && widget.visible)
        check(
            widget.getX() >= 0
                && widget.getY() >= 0
                && widget.getX() + widget.getWidth() <= mc.screen.width
                && widget.getY() + widget.getHeight() <= mc.screen.height,
            "Visible widget clipped in minimum GUI");
  }

  public static void tick(int tick, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (failure != null) throw new IllegalStateException("Server fixture", failure);
    if (tick == 0) {
      mc.options.pauseOnLostFocus = false;
      mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
      mc.getToasts().clear();
      mc.gui.getChat().clearMessages(false);
      server(
          p -> {
            p.teleportTo(0, 90, 0);
            p.getInventory().clearContent();
            for (int i = 0; i < 21; i++)
              p.getData(Cybernetics2077.CYBERWARE).setStackInSlot(i, ItemStack.EMPTY);
            p.getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(
                    CyberwareCategory.FACE.firstSlot(),
                    new ItemStack(Cybernetics2077.OPTICS.get()));
            p.getData(Cybernetics2077.CYBERWARE)
                .setStackInSlot(
                    CyberwareCategory.OPERATING_SYSTEM.firstSlot(),
                    new ItemStack(Cybernetics2077.CYBERDECK_TIERS.get(4).get()));
            equip(p, Augment.BALLISTIC_COPROCESSOR, 5, 0);
            equip(p, Augment.IMMOVABLE_FORCE, 5, 1);
            p.setData(CityContent.ACCOUNT, new StreetAccount());
            p.getData(CityContent.ACCOUNT).reputation(50);
            p.getData(CityContent.ACCOUNT).credit(250000);
            p.getInventory()
                .setItem(1, new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get(), 2));
            p.getInventory().setItem(2, new ItemStack(Cybernetics2077.SHORT_CIRCUIT.get()));
            for (var mob :
                p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(64)))
              mob.discard();
            var npc = CityContent.RIPPERDOC.get().create(p.serverLevel());
            npc.moveTo(2, 90, -1, 0, 0);
            npc.setNoAi(true);
            p.serverLevel().addFreshEntity(npc);
            doc = npc.getId();
            var f = CityContent.FIXER.get().create(p.serverLevel());
            f.moveTo(-2, 90, -1, 0, 0);
            f.setNoAi(true);
            p.serverLevel().addFreshEntity(f);
            fixer = f.getId();
            try {
              Object builder =
                  Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder")
                      .getMethod("create")
                      .invoke(null);
              call(builder, "setId", ResourceLocation.parse("tacz:ak47"));
              call(builder, "setAmmoCount", 30);
              call(builder, "setAmmoInBarrel", true);
              call(
                  builder,
                  "setFireMode",
                  Class.forName("com.tacz.guns.api.item.gun.FireMode").getField("AUTO").get(null));
              p.getInventory().setItem(0, (ItemStack) call(builder, "build", p.registryAccess()));
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
            p.getInventory().selected = 0;
            sync(p);
            var offer = CyberwareCatalogue.offers();
            int i =
                java.util.stream.IntStream.range(0, offer.size())
                    .filter(
                        n ->
                            offer.get(n).item()
                                == Cybernetics2077.augment(Augment.IMMOVABLE_FORCE, 5))
                    .findFirst()
                    .orElseThrow();
            var menu = new RipperdocMenu(1, p.getInventory(), doc);
            for (int d = 0; d < 512 && !menu.stocked(i); d++)
              p.server.overworld().setDayTime(d * 24000L);
            openDoc(p);
          });
    }
    if (java.util.Set.of(18, 38, 58, 78, 98, 118, 138, 158).contains(tick))
      org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 2, 2);
    if (tick == 20) {
      check(mc.screen instanceof RipperdocScreen, "Ripperdoc missing");
      check(
          ((RipperdocMenu) mc.player.containerMenu).capacityLimit() == 150,
          "Client capacity not synced");
      shot(out, "ripperdoc-purchase");
    }
    if (tick == 21) button(">");
    if (tick == 24) {
      var selection = RipperdocScreen.class.getDeclaredField("selected");
      selection.setAccessible(true);
      var method = RipperdocScreen.class.getDeclaredMethod("filtered");
      method.setAccessible(true);
      var offers = (java.util.List<?>) method.invoke(mc.screen);
      check(
          offers.size() > 6 && selection.getInt(mc.screen) == (Integer) offers.get(6),
          "Purchase selection stayed on previous page");
    }
    if (tick == 25) {
      var field = RipperdocScreen.class.getDeclaredField("search");
      field.setAccessible(true);
      ((EditBox) field.get(mc.screen)).setValue("Immovable");
    }
    if (tick == 40) shot(out, "immovable-iconic-shop");
    if (tick == 45) button("INSTALL");
    if (tick == 60) shot(out, "ripperdoc-install");
    if (tick == 65)
      server(
          p ->
              p.openMenu(
                  new SimpleMenuProvider(
                      (id, inv, o) -> new CyberneticsBenchMenu(id, inv),
                      Component.literal("Cybernetics Bench"))));
    if (tick == 80) {
      check(mc.screen instanceof CyberneticsBenchScreen, "Bench missing");
      shot(out, "cybernetics-bench");
    }
    if (tick == 85)
      server(
          p -> {
            var deck =
                p.getData(Cybernetics2077.CYBERWARE)
                    .getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot());
            p.openMenu(
                new SimpleMenuProvider(
                    (id, inv, o) -> new CyberdeckMenu(id, inv, ContainerLevelAccess.NULL, deck),
                    Component.literal("Quickhacks")),
                b -> ItemStack.STREAM_CODEC.encode(b, deck));
          });
    if (tick == 100) {
      check(mc.screen instanceof CyberdeckScreen, "Deck missing");
      shot(out, "quickhack-loadout");
    }
    if (tick == 105)
      server(
          p ->
              p.openMenu(
                  new SimpleMenuProvider(
                      (id, inv, o) -> new FixerMenu(id, inv, fixer), Component.literal("Fixer")),
                  b -> b.writeVarInt(fixer)));
    if (tick == 120) {
      check(mc.screen instanceof FixerScreen, "Fixer missing");
      shot(out, "fixer-bounties");
    }
    if (tick == 125) button("DELIVERY");
    if (tick == 140) shot(out, "fixer-delivery");
    if (tick == 145) button("DEBRIEF");
    if (tick == 160) shot(out, "fixer-debrief");
    if (tick == 165) {
      close();
      server(
          p -> {
            p.setYRot(-25);
            p.setXRot(0);
            for (int x = -8; x <= 8; x++)
              for (int y = 90; y <= 95; y++)
                p.serverLevel()
                    .setBlock(new BlockPos(x, y, 12), Blocks.STONE.defaultBlockState(), 3);
          });
      mc.player.setYRot(-25);
      mc.player.setXRot(0);
    }
    if (tick == 175) aim(true);
    if (tick == 190) {
      check(TacZCompat.aiming(mc.player), "Native ADS missing");
      var profile = TacZCompat.ballistics(mc.player);
      check(profile != null && profile.speed() > 1, "Native ballistic profile missing");
      var path = HandsClient.prediction();
      check(path.stream().anyMatch(HandsClient.Segment::bounced), "Gun preview did not bounce");
      shot(out, "gun-ricochet-preview");
    }
    if (tick == 195) {
      aim(false);
      mc.player.setYRot(0);
      server(
          p -> {
            p.setYRot(0);
            for (int x = -8; x <= 8; x++)
              for (int y = 90; y <= 95; y++)
                p.serverLevel().setBlock(new BlockPos(x, y, 12), Blocks.AIR.defaultBlockState(), 3);
            equip(p, Augment.SMART_LINK, 5, 0);
            var target = EntityType.HUSK.create(p.serverLevel());
            target.moveTo(3, 90, 24, 180, 0);
            target.setNoAi(true);
            target.setNoGravity(true);
            target
                .getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(1000);
            target.setHealth(1000);
            p.serverLevel().addFreshEntity(target);
            HandsRuntime.lock(p, target);
            sync(p);
          });
    }
    if (tick == 205) aim(true);
    if (tick == 220) {
      shot(out, "gun-smart-lock");
      server(
          p -> {
            try {
              var stack = p.getMainHandItem();
              var gunId = call(stack.getItem(), "getGunId", stack);
              var index =
                  (Optional<?>)
                      Class.forName("com.tacz.guns.api.TimelessAPI")
                          .getMethod("getCommonGunIndex", ResourceLocation.class)
                          .invoke(null, gunId);
              var data = call(index.orElseThrow(), "getGunData");
              var bullet = call(data, "getBulletData");
              var type = Class.forName("com.tacz.guns.entity.EntityKineticBullet");
              var ctor =
                  Arrays.stream(type.getConstructors())
                      .filter(c -> c.getParameterCount() == 8)
                      .findFirst()
                      .orElseThrow();
              var b =
                  (Projectile)
                      ctor.newInstance(
                          p.level(),
                          p,
                          stack,
                          ResourceLocation.parse("tacz:762x39"),
                          gunId,
                          true,
                          data,
                          bullet);
              var gravity = type.getDeclaredField("gravity");
              gravity.setAccessible(true);
              gravity.setFloat(b, 0);
              var friction = type.getDeclaredField("friction");
              friction.setAccessible(true);
              friction.setFloat(b, 0);
              b.setPos(p.getEyePosition().add(0, 0, 1));
              b.setDeltaMovement(0, 0, .45);
              p.serverLevel().addFreshEntity(b);
              round = b.getId();
              roundUuid = b.getUUID();
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
          });
    }
    if (tick >= 225 && tick <= 250 && round >= 0) {
      var b = mc.level.getEntity(round);
      if (b != null && GuidedBulletClient.managed(b)) {
        check(b.getUUID().equals(roundUuid), "Guided UUID mismatch");
        if (last != null)
          check(b.position().distanceTo(last) < 3, "Guided round jumped to a second path");
        last = b.position();
        managedFrames++;
        check(
            java.util.stream.StreamSupport.stream(
                        mc.level.entitiesForRendering().spliterator(), false)
                    .filter(TacZCompat::bullet)
                    .count()
                == 1,
            "Duplicate bullet entity");
      }
    }
    if (tick == 240) shot(out, "gun-guided-round");
    if (tick == 260) {
      check(managedFrames >= 10, "No controlled native client bullet samples: " + managedFrames);
      aim(false);
      org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 640, 480);
      server(HandsServiceReview::openDoc);
    }
    if (tick == 280) {
      checkBounds();
      shot(out, "ripperdoc-small");
      button("INSTALL");
    }
    if (tick == 295) {
      checkBounds();
      shot(out, "install-small");
      server(
          p ->
              p.openMenu(
                  new SimpleMenuProvider(
                      (id, inv, o) -> new CyberneticsBenchMenu(id, inv),
                      Component.literal("Bench"))));
    }
    if (tick == 310) {
      checkBounds();
      shot(out, "bench-small");
      server(
          p -> {
            var deck =
                p.getData(Cybernetics2077.CYBERWARE)
                    .getStackInSlot(CyberwareCategory.OPERATING_SYSTEM.firstSlot());
            p.openMenu(
                new SimpleMenuProvider(
                    (id, inv, o) -> new CyberdeckMenu(id, inv, ContainerLevelAccess.NULL, deck),
                    Component.literal("Quickhacks")),
                b -> ItemStack.STREAM_CODEC.encode(b, deck));
          });
    }
    if (tick == 325) {
      checkBounds();
      shot(out, "deck-small");
      server(
          p ->
              p.openMenu(
                  new SimpleMenuProvider(
                      (id, inv, o) -> new FixerMenu(id, inv, fixer), Component.literal("Fixer")),
                  b -> b.writeVarInt(fixer)));
    }
    if (tick == 340) {
      checkBounds();
      shot(out, "fixer-small");
      org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 854, 480);
      Files.writeString(
          out.resolve("passed.txt"),
          "PASS: compact ripperdoc, bench, deck, fixer tabs; synchronized SC50 capacity150; native"
              + " TaCZ ADS ricochet preview; Smart Link brackets; one native guided projectile, no"
              + " extra entity, smooth bounded samples.\n");
      mc.stop();
    }
  }
}
