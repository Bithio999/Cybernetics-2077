package dev.cybernetics2077.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.cybernetics2077.Cybernetics2077;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Reuses each vanilla animated mesh. The synchronized name is a cosmetic marker only. */
public final class EnhancedMobLayer<T extends LivingEntity, M extends EntityModel<T>>
    extends RenderLayer<T, M> {
  private final ResourceLocation plate, glow;

  private EnhancedMobLayer(RenderLayerParent<T, M> parent, boolean skeleton) {
    super(parent);
    String name = skeleton ? "chromed_skeleton" : "chromed_humanoid";
    plate = Cybernetics2077.id("textures/entity/" + name + ".png");
    glow = Cybernetics2077.id("textures/entity/" + name + "_glow.png");
  }

  @Override
  public void render(
      PoseStack pose,
      MultiBufferSource buffers,
      int light,
      T mob,
      float limbSwing,
      float limbAmount,
      float partial,
      float age,
      float yaw,
      float pitch) {
    if (mob.isInvisible()
        || mob.getCustomName() == null
        || !(mob.getCustomName().getContents() instanceof TranslatableContents name)
        || !name.getKey().equals("cybernetics2077.enhanced.name")) return;
    // The decal render type offsets depth; do not inflate the body or duplicate its geometry.
    getParentModel()
        .renderToBuffer(
            pose,
            buffers.getBuffer(RenderType.entityDecal(plate)),
            light,
            LivingEntityRenderer.getOverlayCoords(mob, 0),
            -1);
    getParentModel()
        .renderToBuffer(
            pose,
            buffers.getBuffer(RenderType.eyes(glow)),
            15728640,
            OverlayTexture.NO_OVERLAY,
            -1);
  }

  @EventBusSubscriber(
      modid = Cybernetics2077.ID,
      value = Dist.CLIENT,
      bus = EventBusSubscriber.Bus.MOD)
  public static final class Setup {
    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void layers(EntityRenderersEvent.AddLayers e) {
      for (var type :
          new EntityType<?>[] {
            EntityType.ZOMBIE,
            EntityType.HUSK,
            EntityType.DROWNED,
            EntityType.SKELETON,
            EntityType.STRAY,
            EntityType.BOGGED,
            EntityType.PILLAGER,
            EntityType.VINDICATOR
          })
        if (e.getRenderer(type) instanceof LivingEntityRenderer renderer) {
          renderer.addLayer(new dev.cybernetics2077.client.mantis.MobMantisLayer(renderer));
          renderer.addLayer(
              new EnhancedMobLayer(
                  renderer,
                  type == EntityType.SKELETON
                      || type == EntityType.STRAY
                      || type == EntityType.BOGGED));
        }
    }
  }
}
