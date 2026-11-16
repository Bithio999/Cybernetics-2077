package dev.cybernetics2077.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Animate the native panes themselves; no framebuffer copies or stale HUD screenshots. */
public final class NativeHudTransition {
  public enum Group {
    STATUS,
    SKILLS,
    HOTBAR,
    CROSSHAIR
  }

  private static float multiplier = 1;

  public static float modulate(float alpha) {
    return RenderSystem.isOnRenderThread() ? alpha * multiplier : alpha;
  }

  public static double visible(Group group) {
    var frame = ScannerClient.hudMotion();
    return switch (group) {
      case STATUS -> frame.nativeStatus();
      case SKILLS -> frame.nativeSkills();
      case HOTBAR -> frame.nativeHotbar();
      case CROSSHAIR -> frame.nativeCrosshair();
    };
  }

  public static Group vanilla(ResourceLocation name) {
    if (!name.getNamespace().equals("minecraft")) return null;
    return switch (name.getPath()) {
      case "hotbar", "experience_bar", "experience_level", "jump_meter" -> Group.HOTBAR;
      case "chat", "overlay_message" -> Group.SKILLS;
      case "player_health", "armor_level", "food_level", "air_level", "vehicle_health" ->
          Group.STATUS;
      case "crosshair" -> Group.CROSSHAIR;
      default -> null;
    };
  }

  public static void vanilla(GuiGraphics g, Group group, Runnable render) {
    double visible = visible(group);
    if (visible >= .99999) {
      render.run();
      return;
    }
    if (visible <= .00001) return;
    try (var scope = new Scope(g, (float) visible)) {
      if (group == Group.HOTBAR) g.pose().translate(0, 24 * (1 - visible), 0);
      if (group == Group.CROSSHAIR) {
        g.pose().translate(g.guiWidth() / 2f, g.guiHeight() / 2f, 0);
        g.pose().scale((float) visible, (float) visible, 1);
        g.pose().translate(-g.guiWidth() / 2f, -g.guiHeight() / 2f, 0);
      }
      render.run();
    }
  }

  private static final class Scope implements AutoCloseable {
    final GuiGraphics g;
    final float[] color;
    final float previous;
    final boolean blend;
    final int srcRgb, dstRgb, srcAlpha, dstAlpha;
    boolean scissor;

    Scope(GuiGraphics g, float alpha) {
      this.g = g;
      g.flush();
      color = RenderSystem.getShaderColor().clone();
      previous = multiplier;
      blend = GL11.glIsEnabled(GL11.GL_BLEND);
      srcRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB);
      dstRgb = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
      srcAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA);
      dstAlpha = GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
      g.pose().pushPose();
      multiplier *= alpha;
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.setShaderColor(color[0], color[1], color[2], color[3] / previous);
    }

    @Override
    public void close() {
      try {
        g.flush();
      } finally {
        if (scissor) g.disableScissor();
        g.pose().popPose();
        multiplier = previous;
        RenderSystem.setShaderColor(color[0], color[1], color[2], color[3] / previous);
        RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
        if (blend) RenderSystem.enableBlend();
        else RenderSystem.disableBlend();
      }
    }
  }

  private NativeHudTransition() {}
}
