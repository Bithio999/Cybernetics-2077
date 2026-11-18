package dev.cybernetics2077.city;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;

/** Vanilla jigsaw placement plus a bounded, generation-only foundation below its solid floor. */
public final class CityPoolElement extends SinglePoolElement {
  public static final MapCodec<CityPoolElement> CODEC =
      RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      templateCodec(),
                      processorsCodec(),
                      projectionCodec(),
                      overrideLiquidSettingsCodec())
                  .apply(i, CityPoolElement::new));

  public CityPoolElement(
      Either<ResourceLocation, StructureTemplate> template,
      Holder<StructureProcessorList> processors,
      StructureTemplatePool.Projection projection,
      Optional<LiquidSettings> liquid) {
    super(template, processors, projection, liquid);
  }

  public CityPoolElement(String name) {
    this(
        Either.left(dev.cybernetics2077.Cybernetics2077.id("village/" + name)),
        Holder.direct(new StructureProcessorList(java.util.List.of())),
        StructureTemplatePool.Projection.RIGID,
        Optional.of(LiquidSettings.IGNORE_WATERLOGGING));
  }

  private boolean sunken() {
    return template.left().map(id -> id.getPath().equals("village/afterlife")).orElse(false);
  }

  @Override
  public java.util.List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(
      StructureTemplateManager manager, BlockPos pos, Rotation rotation, RandomSource random) {
    return super.getShuffledJigsawBlocks(manager, sunken() ? pos.below(2) : pos, rotation, random);
  }

  @Override
  public BoundingBox getBoundingBox(
      StructureTemplateManager manager, BlockPos pos, Rotation rotation) {
    if (!sunken()) return super.getBoundingBox(manager, pos, rotation);
    var box = super.getBoundingBox(manager, pos.below(2), rotation);
    // Village road lots reserve space from street level upward. The shallow basement
    // is carved at placement; its entire X/Z footprint is still reserved by this box.
    return new BoundingBox(box.minX(), pos.getY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
  }

  @Override
  public StructurePoolElementType<?> getType() {
    return CityContent.POOL_ELEMENT.get();
  }

  @Override
  public boolean place(
      StructureTemplateManager manager,
      WorldGenLevel level,
      StructureManager structures,
      ChunkGenerator generator,
      BlockPos origin,
      BlockPos pivot,
      Rotation rotation,
      BoundingBox clip,
      RandomSource random,
      LiquidSettings liquid,
      boolean keepJigsaws) {
    if (sunken()) {
      origin = origin.below(2);
      pivot = pivot.below(2);
    }
    if (!super.place(
        manager,
        level,
        structures,
        generator,
        origin,
        pivot,
        rotation,
        clip,
        random,
        liquid,
        keepJigsaws)) return false;
    var size = getSize(manager, Rotation.NONE);
    var settings = new StructurePlaceSettings().setRotation(rotation);
    for (int x = 0; x < size.getX(); x++)
      for (int z = 0; z < size.getZ(); z++) {
        var floor =
            StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, 0, z))
                .offset(origin);
        if (floor.getX() < clip.minX()
            || floor.getX() > clip.maxX()
            || floor.getZ() < clip.minZ()
            || floor.getZ() > clip.maxZ()) continue;
        // Templates have a continuous solid base, including the entrance apron.
        for (int d = 1; d <= 32 && floor.getY() - d > level.getMinBuildHeight(); d++) {
          var pos = floor.below(d);
          var old = level.getBlockState(pos);
          if (old.canOcclude()
              && old.isFaceSturdy(level, pos, Direction.UP)
              && old.getFluidState().isEmpty()) break;
          level.setBlock(pos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 2);
        }
      }
    return true;
  }
}
