package dev.cybernetics2077.city;

import dev.cybernetics2077.Cybernetics2077;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.*;

public final class CityContent {
  private static final DeferredRegister<Block> BLOCKS =
      DeferredRegister.create(Registries.BLOCK, Cybernetics2077.ID);
  private static final DeferredRegister<Item> ITEMS =
      DeferredRegister.create(Registries.ITEM, Cybernetics2077.ID);
  private static final DeferredRegister<EntityType<?>> ENTITIES =
      DeferredRegister.create(Registries.ENTITY_TYPE, Cybernetics2077.ID);
  private static final DeferredRegister<MenuType<?>> MENUS =
      DeferredRegister.create(Registries.MENU, Cybernetics2077.ID);
  private static final DeferredRegister<AttachmentType<?>> DATA =
      DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Cybernetics2077.ID);
  private static final DeferredRegister<StructurePoolElementType<?>> ELEMENTS =
      DeferredRegister.create(Registries.STRUCTURE_POOL_ELEMENT, Cybernetics2077.ID);
  public static final Map<String, Supplier<Block>> BLOCK_SET = new LinkedHashMap<>();
  public static final Map<String, Supplier<Item>> BLOCK_ITEMS = new LinkedHashMap<>();

  static {
    for (var name :
        List.of(
            "reinforced_panel",
            "clinic_tile",
            "ribbed_floor",
            "medical_cabinet",
            "vent_grille",
            "diagnostic_monitor",
            "surgical_chair",
            "surgical_footrest",
            "ceiling_truss",
            "surgical_light",
            "service_pipe",
            "cyan_strip",
            "red_strip",
            "green_strip",
            "bar_counter",
            "bar_stool",
            "ripperdoc_sign",
            "afterlife_sign",
            "bottle_shelf")) {
      boolean prop =
          List.of(
                  "diagnostic_monitor",
                  "surgical_chair",
                  "surgical_footrest",
                  "ceiling_truss",
                  "surgical_light",
                  "service_pipe",
                  "bar_stool",
                  "ripperdoc_sign",
                  "afterlife_sign")
              .contains(name);
      int light =
          switch (name) {
            case "cyan_strip", "red_strip", "green_strip", "surgical_light" -> 14;
            case "diagnostic_monitor", "ripperdoc_sign", "afterlife_sign" -> 9;
            default -> 0;
          };
      var block =
          BLOCKS.register(
              name,
              () -> {
                var p =
                    BlockBehaviour.Properties.of()
                        .strength(3.5f)
                        .sound(SoundType.METAL)
                        .lightLevel(s -> light);
                if (prop) p.noOcclusion();
                return new CityBlock(p, name);
              });
      BLOCK_SET.put(name, block::get);
      BLOCK_ITEMS.put(
          name, ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties())));
    }
  }

  public static final Supplier<Item> EDDIES =
      ITEMS.register("eurodollar_chip", EurodollarChip::new);
  public static final Supplier<EntityType<CityResident>> RIPPERDOC = resident("ripperdoc");
  public static final Supplier<EntityType<CityResident>> FIXER = resident("fixer");
  public static final Supplier<Item> DOC_EGG =
      ITEMS.register(
          "ripperdoc_spawn_egg",
          () ->
              new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                  RIPPERDOC, 0x24373b, 0x6cdbd5, new Item.Properties()));
  public static final Supplier<Item> FIXER_EGG =
      ITEMS.register(
          "fixer_spawn_egg",
          () ->
              new net.neoforged.neoforge.common.DeferredSpawnEggItem(
                  FIXER, 0x17282b, 0xb99b56, new Item.Properties()));
  public static final Supplier<MenuType<RipperdocMenu>> RIPPER_MENU =
      MENUS.register(
          "ripperdoc",
          () ->
              net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(
                  RipperdocMenu::new));
  public static final Supplier<MenuType<FixerMenu>> FIXER_MENU =
      MENUS.register(
          "fixer",
          () -> net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(FixerMenu::new));
  public static final Supplier<AttachmentType<StreetAccount>> ACCOUNT =
      DATA.register(
          "street_account",
          () -> AttachmentType.serializable(StreetAccount::new).copyOnDeath().build());
  public static final Supplier<StructurePoolElementType<CityPoolElement>> POOL_ELEMENT =
      ELEMENTS.register("supported_building", () -> () -> CityPoolElement.CODEC);

  private static Supplier<EntityType<CityResident>> resident(String name) {
    return ENTITIES.register(
        name,
        () ->
            EntityType.Builder.of(CityResident::new, MobCategory.MISC)
                .sized(.6f, 1.8f)
                .eyeHeight(1.62f)
                .clientTrackingRange(8)
                .build(Cybernetics2077.ID + ":" + name));
  }

  public static void register(IEventBus bus) {
    BLOCKS.register(bus);
    ITEMS.register(bus);
    ENTITIES.register(bus);
    MENUS.register(bus);
    DATA.register(bus);
    ELEMENTS.register(bus);
    bus.addListener(
        (net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) -> {
          e.put(RIPPERDOC.get(), CityResident.attributes().build());
          e.put(FIXER.get(), CityResident.attributes().build());
        });
    bus.addListener(
        (net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent e) -> {
          if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
            for (var item : BLOCK_ITEMS.values()) e.accept(item.get());
          if (e.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) e.accept(EDDIES.get());
          if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(DOC_EGG.get());
            e.accept(FIXER_EGG.get());
          }
        });
  }

  private CityContent() {}
}
