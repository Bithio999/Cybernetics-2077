package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.CityResident;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class CityResidentRenderer
    extends HumanoidMobRenderer<CityResident, PlayerModel<CityResident>> {
  private static final ResourceLocation DOC = Cybernetics2077.id("textures/entity/ripperdoc.png"),
      IRIS = Cybernetics2077.id("textures/entity/ripperdoc_iris.png"),
      MARA = Cybernetics2077.id("textures/entity/fixer_mara.png"),
      REN = Cybernetics2077.id("textures/entity/fixer_ren.png");
  private final PlayerModel<CityResident> classic, slim;

  public CityResidentRenderer(EntityRendererProvider.Context c) {
    super(c, new PlayerModel<>(c.bakeLayer(ModelLayers.PLAYER), false), .45f);
    classic = model;
    slim = new PlayerModel<>(c.bakeLayer(ModelLayers.PLAYER_SLIM), true);
  }

  @Override
  public void render(
      CityResident n,
      float yaw,
      float partial,
      com.mojang.blaze3d.vertex.PoseStack pose,
      net.minecraft.client.renderer.MultiBufferSource buffers,
      int light) {
    model = n.slim() ? slim : classic;
    super.render(n, yaw, partial, pose, buffers, light);
  }

  @Override
  public ResourceLocation getTextureLocation(CityResident n) {
    return !n.fixer() ? (n.style() == 0 ? DOC : IRIS) : n.style() == 0 ? MARA : REN;
  }
}
