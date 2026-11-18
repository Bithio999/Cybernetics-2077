package dev.cybernetics2077.mixin;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.world.level.levelgen.structure.pools.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(StructureTemplatePool.class)
public interface VillagePoolAccess {
  @Accessor("rawTemplates")
  List<Pair<StructurePoolElement, Integer>> cyber$raw();

  @Mutable
  @Accessor("rawTemplates")
  void cyber$raw(List<Pair<StructurePoolElement, Integer>> values);

  @Accessor("templates")
  ObjectArrayList<StructurePoolElement> cyber$weighted();

  @Accessor("maxSize")
  void cyber$maxSize(int v);
}
