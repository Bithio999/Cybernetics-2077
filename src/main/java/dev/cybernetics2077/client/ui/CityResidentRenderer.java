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
      MARA = Cybernetics2077.id("textures/entity/fixer_mara.png"),
      REN = Cybernetics2077.id("textures/entity/fixer_ren.png");

  public CityResidentRenderer(EntityRendererProvider.Context c) {
    super(c, new PlayerModel<>(c.bakeLayer(ModelLayers.PLAYER), false), .45f);
  }

  @Override
  public ResourceLocation getTextureLocation(CityResident n) {
    return !n.fixer() ? DOC : n.style() == 0 ? MARA : REN;
  }
}
