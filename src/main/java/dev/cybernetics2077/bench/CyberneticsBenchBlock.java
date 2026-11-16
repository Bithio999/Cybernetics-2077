package dev.cybernetics2077.bench;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Temporary, stateless workstation; equipment belongs to the player, never to the block. */
public final class CyberneticsBenchBlock extends Block {
  public CyberneticsBenchBlock(Properties properties) {
    super(properties);
  }

  @Override
  protected net.minecraft.world.phys.shapes.VoxelShape getShape(
      BlockState state,
      BlockGetter level,
      BlockPos pos,
      net.minecraft.world.phys.shapes.CollisionContext context) {
    return net.minecraft.world.phys.shapes.Shapes.or(
        Block.box(0, 10, 0, 16, 14.3, 16),
        Block.box(1, 0, 1, 3, 10, 3),
        Block.box(13, 0, 1, 15, 10, 3),
        Block.box(1, 0, 13, 3, 10, 15),
        Block.box(13, 0, 13, 15, 10, 15));
  }

  @Override
  protected InteractionResult useWithoutItem(
      BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    if (!level.isClientSide)
      player.openMenu(
          new SimpleMenuProvider(
              (id, inv, p) ->
                  new CyberneticsBenchMenu(
                      id,
                      inv,
                      net.minecraft.world.inventory.ContainerLevelAccess.create(level, pos)),
              Component.translatable("block.cybernetics2077.cybernetics_bench")));
    return InteractionResult.sidedSuccess(level.isClientSide);
  }
}
