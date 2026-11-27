package dev.cybernetics2077.compat.review;

import com.google.gson.*;
import com.mojang.blaze3d.platform.NativeImage;
import dev.cybernetics2077.Cybernetics2077;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/**
 * Review-only comparison: real item models, original textures, 16px inventory size and 2x detail.
 */
public final class IconIdentityClientReview {
  private static JsonArray sheets;
  private static int rendered;

  public static void tick(int t, Path out) throws Exception {
    var mc = Minecraft.getInstance();
    if (t == 0) {
      Files.createDirectories(out);
      mc.options.guiScale().set(2);
      org.lwjgl.glfw.GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1280, 800);
      mc.resizeDisplay();
      sheets =
          JsonParser.parseString(
                  Files.readString(
                      Path.of("/workspace/Cybernetics-2077/art/icon-identity/manifest.json")))
              .getAsJsonObject()
              .getAsJsonArray("sheets");
    }
    if (t >= 5 && t < 180 && (t - 5) % 25 == 0)
      mc.setScreen(new Comparison(sheets.get((t - 5) / 25).getAsJsonObject()));
    if (t >= 5 && t < 180 && (t - 5) % 25 == 15) {
      String name = sheets.get((t - 5) / 25).getAsJsonObject().get("name").getAsString();
      Screenshot.grab(out.toFile(), "icons-" + name + ".png", mc.getMainRenderTarget(), m -> {});
    }
    if (t == 180) {
      if (rendered != 84)
        throw new IllegalStateException("Missing native icon comparisons: " + rendered);
      Files.writeString(
          out.resolve("icon-identity-passed.txt"),
          "PASS: 84 real item models compared with original textures at native 16px and enlarged"
              + " 32px; all registry IDs valid and all review panels fit the viewport.\n");
    }
    if (t == 180) mc.setScreen(new CurrentGallery());
    if (t == 200)
      Screenshot.grab(
          out.toFile(), "icons-current-catalogue.png", mc.getMainRenderTarget(), m -> {});
    if (t == 215) {
      mc.setScreen(null);
      mc.player.closeContainer();
    }
    if (t >= 215) CreativeClientReview.tick(t - 215, out);
  }

  private static final class CurrentGallery extends Screen {
    private final List<ItemStack> items =
        dev.cybernetics2077.CreativeContent.items().stream()
            .filter(
                item ->
                    item instanceof dev.cybernetics2077.cyberware.CyberwareItem implant
                            && implant.tier().level == 5
                        || item instanceof dev.cybernetics2077.deck.QuickhackItem program
                            && program.tier().level == 5)
            .map(ItemStack::new)
            .toList();

    CurrentGallery() {
      super(Component.literal("Cyberware"));
      if (items.size() != 116)
        throw new IllegalStateException("Current family gallery incomplete: " + items.size());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
      g.fill(0, 0, width, height, 0xff0a131b);
      g.drawCenteredString(font, "CYBERNETICS 2077 // CYBERWARE", width / 2, 12, 0xffa9d5d6);
      int x0 = (width - 12 * 32) / 2;
      for (int i = 0; i < items.size(); i++) {
        int x = x0 + i % 12 * 32, y = 40 + i / 12 * 32;
        g.fill(x, y, x + 28, y + 28, 0xff14212a);
        g.renderItem(items.get(i), x + 6, y + 6);
      }
    }
  }

  private record Row(ItemStack item, ResourceLocation before) {}

  private static final class Comparison extends Screen {
    private final List<Row> rows = new ArrayList<>();

    Comparison(JsonObject sheet) throws Exception {
      super(Component.literal(sheet.get("name").getAsString().toUpperCase(Locale.ROOT)));
      var mc = Minecraft.getInstance();
      for (var e : sheet.getAsJsonArray("icons")) {
        String id = e.getAsJsonObject().get("id").getAsString();
        String registry =
            id.equals("cyberdeck_tier_1") ? "militech_paraline_tier_5" : id + "_tier_5";
        var item = BuiltInRegistries.ITEM.get(Cybernetics2077.id(registry));
        if (item == Items.AIR) throw new IllegalStateException("No model for " + id);
        try (var input =
            Files.newInputStream(Path.of("/workspace/icon-identity-before", id + ".png"))) {
          var image = NativeImage.read(input);
          var texture =
              mc.getTextureManager().register("icon_before_" + id, new DynamicTexture(image));
          rows.add(new Row(new ItemStack(item), texture));
        }
        rendered++;
      }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
      g.fill(0, 0, width, height, 0xff0a131b);
      g.drawCenteredString(
          font, "CYBERNETICS 2077 // " + title.getString(), width / 2, 12, 0xffa9d5d6);
      g.drawCenteredString(font, "OLD 16px     NEW 16px     NEW 32px", width / 2, 28, 0xff8da0a4);
      int cell = 144, start = (width - cell * 4) / 2, top = 48;
      if (start < 0 || top + 3 * 104 > height)
        throw new IllegalStateException("Comparison panel exceeds viewport");
      for (int i = 0; i < rows.size(); i++) {
        var row = rows.get(i);
        int x = start + i % 4 * cell, y = top + i / 4 * 104;
        g.fill(x, y, x + cell - 5, y + 98, 0xff14212a);
        var lines = font.split(row.item.getHoverName(), cell - 17);
        int line = 0;
        for (var text : lines) {
          if (line == 3) break;
          g.drawString(font, text, x + 6, y + 5 + line * 9, 0xffd5dad5, false);
          line++;
        }
        int iy = y + 42;
        g.blit(row.before, x + 8, iy, 16, 16, 0f, 0f, 32, 32, 32, 32);
        g.renderItem(row.item, x + 44, iy);
        g.pose().pushPose();
        g.pose().translate(x + 87, iy - 8, 0);
        g.pose().scale(2, 2, 1);
        g.renderItem(row.item, 0, 0);
        g.pose().popPose();
        g.drawString(font, "old", x + 8, y + 79, 0xff82949a, false);
        g.drawString(font, "new", x + 44, y + 79, 0xffb4ddd5, false);
        g.drawString(font, "detail", x + 87, y + 79, 0xff82949a, false);
      }
    }
  }
}
