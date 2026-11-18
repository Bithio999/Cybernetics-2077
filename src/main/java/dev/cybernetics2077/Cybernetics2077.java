package dev.cybernetics2077;

import dev.cybernetics2077.bench.*;
import dev.cybernetics2077.combat.CyberwareNetwork;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.deck.*;
import dev.cybernetics2077.network.OpenCyberware;
import dev.cybernetics2077.scanner.ScannerNetwork;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.*;

@Mod(Cybernetics2077.ID)
public final class Cybernetics2077 {
  public static final String ID = "cybernetics2077";
  private static final DeferredRegister<net.minecraft.world.item.Item> ITEMS =
      DeferredRegister.create(Registries.ITEM, ID);
  private static final DeferredRegister<net.minecraft.world.level.block.Block> BLOCKS =
      DeferredRegister.create(Registries.BLOCK, ID);
  public static final Supplier<net.minecraft.world.level.block.Block> BENCH =
      BLOCKS.register(
          "cybernetics_bench",
          () ->
              new CyberneticsBenchBlock(
                  net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
                      .noOcclusion()
                      .strength(3.5f)
                      .sound(net.minecraft.world.level.block.SoundType.METAL)));
  public static final Supplier<net.minecraft.world.item.Item> BENCH_ITEM =
      ITEMS.register(
          "cybernetics_bench",
          () ->
              new net.minecraft.world.item.BlockItem(
                  BENCH.get(), new net.minecraft.world.item.Item.Properties()));
  // Keep the original tier-one registry IDs for existing saves and installed item components.
  public static final java.util.List<Supplier<OpticsItem>> OPTICS_TIERS =
      tiers("optical_scanner", "kiroshi_optics", OpticsItem::new);
  public static final java.util.List<Supplier<CyberdeckItem>> CYBERDECK_TIERS =
      tiers("cyberdeck_tier_1", "militech_paraline", CyberdeckItem::new);
  public static final java.util.List<Supplier<QuickhackItem>> SHORT_CIRCUIT_TIERS =
      tiers("short_circuit_tier_1", "short_circuit", QuickhackItem::new);
  public static final java.util.List<Supplier<MantisBladesItem>> MANTIS_TIERS =
      java.util.stream.IntStream.rangeClosed(2, 5)
          .<Supplier<MantisBladesItem>>mapToObj(
              t ->
                  ITEMS.register(
                      "mantis_blades_tier_" + t, () -> new MantisBladesItem(CyberwareTier.of(t))))
          .toList();
  // Compatibility alias: existing stacks are tier TWO immediately, never tier one.
  public static final Supplier<MantisBladesItem> LEGACY_MANTIS =
      ITEMS.register("mantis_claws_tier_1", () -> new MantisBladesItem(CyberwareTier.TWO));
  public static final java.util.Map<Augment, java.util.List<Supplier<AugmentItem>>> AUGMENTS =
      registerAugments();
  public static final Supplier<OpticsItem> OPTICS = OPTICS_TIERS.get(0);
  public static final Supplier<CyberdeckItem> CYBERDECK = CYBERDECK_TIERS.get(0);
  public static final Supplier<QuickhackItem> SHORT_CIRCUIT = SHORT_CIRCUIT_TIERS.get(0);
  public static final Supplier<MantisBladesItem> MANTIS = MANTIS_TIERS.get(0);

  private static java.util.Map<Augment, java.util.List<Supplier<AugmentItem>>> registerAugments() {
    var result =
        new java.util.EnumMap<Augment, java.util.List<Supplier<AugmentItem>>>(Augment.class);
    for (var kind : Augment.values()) {
      var entries = new java.util.ArrayList<Supplier<AugmentItem>>();
      for (int t = kind.firstTier; t <= 5; t++) {
        var tier = CyberwareTier.of(t);
        entries.add(ITEMS.register(kind.id + "_tier_" + t, () -> new AugmentItem(kind, tier)));
      }
      result.put(kind, java.util.List.copyOf(entries));
    }
    return java.util.Collections.unmodifiableMap(result);
  }

  public static AugmentItem augment(Augment kind, int tier) {
    if (tier < kind.firstTier || tier > 5)
      throw new IllegalArgumentException("Unavailable implant tier");
    return AUGMENTS.get(kind).get(tier - kind.firstTier).get();
  }

  private static <T extends net.minecraft.world.item.Item> java.util.List<Supplier<T>> tiers(
      String originalId, String prefix, java.util.function.Function<CyberwareTier, T> factory) {
    var result = new java.util.ArrayList<Supplier<T>>();
    for (var tier : CyberwareTier.values())
      result.add(
          ITEMS.register(
              tier == CyberwareTier.ONE ? originalId : prefix + "_tier_" + tier.level,
              () -> factory.apply(tier)));
    return java.util.List.copyOf(result);
  }

  private static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, ID);
  private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ID);
  public static final Supplier<MenuType<CyberwareMenu>> MENU =
      MENUS.register(
          "cyberware", () -> new MenuType<>(CyberwareMenu::new, FeatureFlags.DEFAULT_FLAGS));
  public static final Supplier<MenuType<CyberneticsBenchMenu>> BENCH_MENU =
      MENUS.register(
          "cybernetics_bench",
          () -> new MenuType<>(CyberneticsBenchMenu::new, FeatureFlags.DEFAULT_FLAGS));
  public static final Supplier<MenuType<CyberdeckMenu>> DECK_MENU =
      MENUS.register(
          "cyberdeck",
          () ->
              net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                  CyberdeckMenu::new));
  public static final Supplier<AttachmentType<CyberwareInventory>> CYBERWARE =
      ATTACHMENTS.register(
          "cyberware",
          () -> AttachmentType.serializable(CyberwareInventory::new).copyOnDeath().build());
  public static final Supplier<AttachmentType<AugmentState>> AUGMENT_STATE =
      ATTACHMENTS.register(
          "augment_state",
          () -> AttachmentType.serializable(AugmentState::new).copyOnDeath().build());
  private static final ModConfigSpec.Builder CONFIG = new ModConfigSpec.Builder();
  public static final ModConfigSpec.IntValue CAPACITY =
      CONFIG
          .comment("Maximum total capacity available for installed cyberware.")
          .defineInRange("baseCapacity", 100, 1, 100000);
  private static final ModConfigSpec SPEC = CONFIG.build();

  public Cybernetics2077(IEventBus bus, ModContainer container) {
    dev.cybernetics2077.city.CityContent.register(bus);
    BLOCKS.register(bus);
    ITEMS.register(bus);
    MENUS.register(bus);
    ATTACHMENTS.register(bus);
    container.registerConfig(ModConfig.Type.SERVER, SPEC);
    bus.addListener(this::network);
    bus.addListener(ScannerNetwork::register);
    bus.addListener(CyberwareNetwork::register);
    bus.addListener(this::creative);
  }

  private void creative(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) {
    if (e.getTabKey() == net.minecraft.world.item.CreativeModeTabs.FUNCTIONAL_BLOCKS)
      e.accept(BENCH_ITEM.get());
    if (e.getTabKey() == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
      for (var family :
          java.util.List.of(OPTICS_TIERS, CYBERDECK_TIERS, SHORT_CIRCUIT_TIERS, MANTIS_TIERS))
        for (var item : family) e.accept(item.get());
      for (var family : AUGMENTS.values()) for (var item : family) e.accept(item.get());
    }
  }

  private void network(RegisterPayloadHandlersEvent e) {
    e.registrar("1")
        .playToServer(
            OpenCyberware.TYPE,
            OpenCyberware.CODEC,
            (payload, context) ->
                context.enqueueWork(
                    () -> {
                      if (context.player() instanceof ServerPlayer p
                          && p.isAlive()
                          && (p.containerMenu == p.inventoryMenu
                              || p.containerMenu instanceof dev.cybernetics2077.deck.CyberdeckMenu
                                  && p.containerMenu.stillValid(p))
                          && p.containerMenu.getCarried().isEmpty())
                        p.openMenu(
                            new SimpleMenuProvider(
                                (id, inventory, owner) -> CyberwareMenu.server(id, inventory),
                                Component.translatable("cybernetics2077.screen.title")));
                    }));
  }

  public static ResourceLocation id(String path) {
    return ResourceLocation.fromNamespaceAndPath(ID, path);
  }
}
