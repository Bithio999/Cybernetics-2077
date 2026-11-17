package dev.cybernetics2077.review;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.client.mantis.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;

/** Compares the new rest hand to the actual vanilla renderer, not a duplicated expected formula. */
@EventBusSubscriber(modid = Cybernetics2077.ID, value = Dist.CLIENT)
public final class NativeHandReferenceReview {
  private static Matrix4f expected;
  public static int samples;

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void before(RenderHandEvent e) {
    var m = Minecraft.getInstance();
    if (e.getHand() != InteractionHand.MAIN_HAND
        || m.player == null
        || CyberwareClient.visible(m.player)
        || !e.getItemStack().isEmpty()) {
      expected = null;
      return;
    }
    boolean left = m.player.getMainArm() == HumanoidArm.LEFT,
        slim = m.player.getSkin().model() == PlayerSkin.Model.SLIM;
    var rest = MantisMotion.sample(false, 100, 100, 0, false, 0, left);
    expected =
        new Matrix4f(e.getPoseStack().last().pose())
            .mul(
                MantisView.frame(
                    left, true, slim, e.getEquipProgress(), e.getSwingProgress(), rest));
  }

  @SubscribeEvent
  public static void nativeArm(RenderArmEvent e) {
    if (expected == null || e.getArm() != e.getPlayer().getMainArm()) return;
    var renderer =
        (PlayerRenderer)
            Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e.getPlayer());
    boolean left = e.getArm() == HumanoidArm.LEFT,
        slim = e.getPlayer().getSkin().model() == PlayerSkin.Model.SLIM;
    var part = left ? renderer.getModel().leftArm : renderer.getModel().rightArm;
    var initial = part.getInitialPose();
    var actual =
        new Matrix4f(e.getPoseStack().last().pose())
            .translate(initial.x / 16, initial.y / 16, initial.z / 16)
            .rotateZ(left ? -.1f : .1f)
            .translate(MantisRig.centerOffset(left, slim), 0, 0);
    float[] a = actual.get(new float[16]), b = expected.get(new float[16]);
    for (int i = 0; i < 16; i++)
      if (Math.abs(a[i] - b[i]) > .00002f) {
        CyberwareClientReview.error =
            "Rest hand differs from native renderer at matrix element "
                + i
                + ": "
                + a[i]
                + " vs "
                + b[i];
        return;
      }
    samples++;
    expected = null;
  }
}
