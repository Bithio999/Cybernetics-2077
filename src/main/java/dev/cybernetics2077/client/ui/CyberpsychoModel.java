package dev.cybernetics2077.client.ui;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.mob.Cyberpsycho;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;

/** Three baked, articulated exoskeleton silhouettes. All detail follows native limb animation. */
public final class CyberpsychoModel extends PlayerModel<Cyberpsycho> {
  public static final ModelLayerLocation[] LAYERS = {
    new ModelLayerLocation(Cybernetics2077.id("psycho_brute"), "main"),
    new ModelLayerLocation(Cybernetics2077.id("psycho_runner"), "main"),
    new ModelLayerLocation(Cybernetics2077.id("psycho_blitz"), "main")
  };

  public CyberpsychoModel(ModelPart root) {
    super(root, false);
  }

  public static LayerDefinition create(int role) {
    var mesh = PlayerModel.createMesh(CubeDeformation.NONE, false);
    var root = mesh.getRoot();
    var body = root.getChild("body");
    for (int i = 0; i < 7; i++)
      body.addOrReplaceChild(
          "spine_" + i,
          CubeListBuilder.create().texOffs(32, 20).addBox(-1, 1 + i * 1.45f, 2.05f, 2, 1.1f, 1.3f),
          PartPose.ZERO);
    for (int i = 0; i < 4; i++) {
      body.addOrReplaceChild(
          "rib_l_" + i,
          CubeListBuilder.create()
              .texOffs(20, 20)
              .addBox(-4.35f, 2 + i * 1.8f, -2.5f, 2.4f, .65f, .9f),
          PartPose.ZERO);
      body.addOrReplaceChild(
          "rib_r_" + i,
          CubeListBuilder.create()
              .texOffs(20, 20)
              .addBox(1.95f, 2 + i * 1.8f, -2.5f, 2.4f, .65f, .9f),
          PartPose.ZERO);
    }
    root.getChild("head")
        .addOrReplaceChild(
            "optics",
            CubeListBuilder.create().texOffs(8, 8).addBox(-4.35f, -5.4f, -4.65f, 8.7f, 2.4f, 1.1f),
            PartPose.ZERO);
    root.getChild("head")
        .addOrReplaceChild(
            "jaw",
            CubeListBuilder.create().texOffs(24, 8).addBox(-3.4f, -2.3f, -4.5f, 6.8f, 2.4f, .9f),
            PartPose.ZERO);
    for (String side : new String[] {"left", "right"}) {
      boolean left = side.equals("left");
      float x = left ? -1.8f : -2.2f;
      var arm = root.getChild(side + "_arm");
      arm.addOrReplaceChild(
          "shoulder",
          CubeListBuilder.create().texOffs(44, 20).addBox(x - .3f, -2.5f, -2.5f, 4.6f, 3.3f, 5),
          PartPose.ZERO);
      arm.addOrReplaceChild(
          "bracer",
          CubeListBuilder.create().texOffs(44, 20).addBox(x - .1f, 4.8f, -2.3f, 4.2f, 4.8f, 4.6f),
          PartPose.ZERO);
      for (int i = 0; i < 3; i++)
        arm.addOrReplaceChild(
            "cable_" + i,
            CubeListBuilder.create()
                .texOffs(16, 20)
                .addBox(x + .4f + i, 1.2f, 2.05f, .35f, 3.5f, .5f),
            PartPose.ZERO);
      var leg = root.getChild(side + "_leg");
      leg.addOrReplaceChild(
          "knee",
          CubeListBuilder.create().texOffs(4, 20).addBox(-2.3f, 4, -2.65f, 4.6f, 2.8f, 1),
          PartPose.ZERO);
      leg.addOrReplaceChild(
          "calf",
          CubeListBuilder.create().texOffs(12, 20).addBox(-2.3f, 7, 1.6f, 4.6f, 4.5f, 1.2f),
          PartPose.ZERO);
      if (role == 0)
        arm.addOrReplaceChild(
            "heavy_plate",
            CubeListBuilder.create().texOffs(20, 20).addBox(x - 1.4f, -2.6f, -3, 6.6f, 5, 6),
            PartPose.ZERO);
      if (role == 2)
        leg.addOrReplaceChild(
            "servo",
            CubeListBuilder.create().texOffs(12, 20).addBox(left ? 2 : -3, 2, .8f, 1, 8, 1.4f),
            PartPose.ZERO);
    }
    if (role == 0) {
      body.addOrReplaceChild(
          "tank_l",
          CubeListBuilder.create().texOffs(32, 20).addBox(-4.7f, 1, 2.1f, 2.7f, 7, 2.3f),
          PartPose.ZERO);
      body.addOrReplaceChild(
          "tank_r",
          CubeListBuilder.create().texOffs(32, 20).addBox(2, 1, 2.1f, 2.7f, 7, 2.3f),
          PartPose.ZERO);
    } else if (role == 1) {
      root.getChild("head")
          .addOrReplaceChild(
              "neural_stack",
              CubeListBuilder.create().texOffs(24, 8).addBox(-2.6f, -7.7f, 3, 5.2f, 5, 2.5f),
              PartPose.ZERO);
      body.addOrReplaceChild(
          "deck_pack",
          CubeListBuilder.create().texOffs(32, 20).addBox(-3, 2, 2.5f, 6, 6.5f, 2),
          PartPose.ZERO);
      for (int i = 0; i < 3; i++)
        root.getChild("head")
            .addOrReplaceChild(
                "optic_" + i,
                CubeListBuilder.create()
                    .texOffs(8, 8)
                    .addBox(-4.7f, -7 + i * 1.4f, -2, 1.1f, .8f, 4),
                PartPose.ZERO);
    } else {
      body.addOrReplaceChild(
          "accelerator",
          CubeListBuilder.create().texOffs(32, 20).addBox(-1.5f, .5f, 2.8f, 3, 9, 1.6f),
          PartPose.ZERO);
    }
    return LayerDefinition.create(mesh, 64, 64);
  }
}
