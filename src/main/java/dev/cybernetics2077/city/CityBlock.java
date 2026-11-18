package dev.cybernetics2077.city;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

/** Baked, directional furniture: no block entity, renderer, ticker or particle emitter. */
public final class CityBlock extends HorizontalDirectionalBlock {
  private final VoxelShape[] shapes;
  private final String kind;
  private static final MapCodec<CityBlock> CODEC =
      com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(
          i ->
              i.group(
                      propertiesCodec(),
                      com.mojang.serialization.Codec.STRING.fieldOf("kind").forGetter(b -> b.kind))
                  .apply(i, CityBlock::new));

  public CityBlock(Properties p, String kind) {
    super(p);
    this.kind = kind;
    registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    double[] b =
        switch (kind) {
          case "surgical_chair" -> new double[] {0, 0, 1, 16, 25, 15};
          case "surgical_footrest" -> new double[] {3, 2, 0, 13, 8, 16};
          case "ceiling_truss" -> new double[] {0, 12, 0, 16, 16, 16};
          case "bar_stool" -> new double[] {3, 0, 3, 13, 12, 13};
          case "diagnostic_monitor" -> new double[] {1, 0, 3, 15, 13, 13};
          case "surgical_light" -> new double[] {1, 10, 1, 15, 16, 15};
          case "service_pipe" -> new double[] {5, 0, 5, 11, 16, 11};
          case "ripperdoc_sign", "afterlife_sign" -> new double[] {-16, 0, 14, 32, 16, 16};
          default -> new double[] {0, 0, 0, 16, 16, 16};
        };
    shapes = new VoxelShape[4];
    for (int i = 0; i < 4; i++) {
      shapes[i] = Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
      b = new double[] {16 - b[5], b[1], b[0], 16 - b[2], b[4], b[3]};
    }
  }

  @Override
  protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
    return CODEC;
  }

  @Override
  protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
    b.add(FACING);
  }

  @Override
  public BlockState getStateForPlacement(BlockPlaceContext c) {
    return defaultBlockState().setValue(FACING, c.getHorizontalDirection().getOpposite());
  }

  @Override
  protected BlockState rotate(BlockState s, Rotation r) {
    return s.setValue(FACING, r.rotate(s.getValue(FACING)));
  }

  @Override
  protected BlockState mirror(BlockState s, Mirror m) {
    return rotate(s, m.getRotation(s.getValue(FACING)));
  }

  @Override
  protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
    return shapes[
        s.getValue(FACING).get2DDataValue() == 2
            ? 0
            : (s.getValue(FACING).get2DDataValue() + 2) % 4];
  }
}
