package dev.cybernetics2077.city;

import com.mojang.datafixers.util.Pair;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.mixin.VillagePoolAccess;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class VillageServices {
  @SubscribeEvent
  public static void start(ServerAboutToStartEvent e) {
    var registry = e.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
    for (var biome : List.of("plains", "desert", "savanna", "snowy", "taiga")) {
      var pool =
          registry.get(ResourceLocation.withDefaultNamespace("village/" + biome + "/houses"));
      if (pool != null) inject(pool);
    }
  }

  public static void inject(StructureTemplatePool pool) {
    var access = (VillagePoolAccess) pool;
    if (access.cyber$raw().stream().anyMatch(p -> p.getFirst() instanceof CityPoolElement)) return;
    var entries = new ArrayList<>(access.cyber$raw());
    // Weights describe selection at a valid house socket, not a promise of buildings per village.
    for (var pair :
        List.of(Pair.of("clinic_small", 12), Pair.of("clinic_large", 2), Pair.of("afterlife", 3))) {
      var element = new CityPoolElement(pair.getFirst());
      entries.add(Pair.of(element, pair.getSecond()));
      for (int n = 0; n < pair.getSecond(); n++) access.cyber$weighted().add(element);
    }
    access.cyber$raw(entries);
    access.cyber$maxSize(Integer.MIN_VALUE);
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent e) {
    e.getDispatcher()
        .register(
            Commands.literal("cybernetics")
                .requires(s -> s.hasPermission(2))
                .then(
                    Commands.literal("eddies")
                        .then(
                            Commands.literal("add")
                                .then(
                                    Commands.argument(
                                            "player",
                                            net.minecraft.commands.arguments.EntityArgument
                                                .player())
                                        .then(
                                            Commands.argument(
                                                    "amount",
                                                    com.mojang.brigadier.arguments.LongArgumentType
                                                        .longArg(1, 1_000_000_000L))
                                                .executes(
                                                    c -> {
                                                      var p =
                                                          net.minecraft.commands.arguments
                                                              .EntityArgument.getPlayer(
                                                              c, "player");
                                                      p.getData(CityContent.ACCOUNT)
                                                          .credit(
                                                              com.mojang.brigadier.arguments
                                                                  .LongArgumentType.getLong(
                                                                  c, "amount"));
                                                      c.getSource()
                                                          .sendSuccess(
                                                              () ->
                                                                  Component.translatable(
                                                                      "cybernetics2077.city.balance",
                                                                      p.getName(),
                                                                      p.getData(CityContent.ACCOUNT)
                                                                          .eddies()),
                                                              true);
                                                      return 1;
                                                    }))))));
  }
}
