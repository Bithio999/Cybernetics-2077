package dev.cybernetics2077.review;

import com.mojang.authlib.GameProfile;
import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.city.*;
import dev.cybernetics2077.cyberware.*;
import dev.cybernetics2077.mixin.VillagePoolAccess;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pools.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class CityGameTests {
  private static ServerPlayer player(GameTestHelper h) {
    return FakePlayerFactory.get(
        h.getLevel(), new GameProfile(UUID.randomUUID(), "CityServiceTest"));
  }

  private static CityResident resident(GameTestHelper h, ServerPlayer p, boolean fixer) {
    var n = (fixer ? CityContent.FIXER : CityContent.RIPPERDOC).get().create(h.getLevel());
    var pos = h.absolutePos(new BlockPos(1, 30, 1));
    n.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
    h.getLevel().addFreshEntity(n);
    p.setPos(n.position().add(1, 0, 0));
    return n;
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void everyVanillaVillageGetsWeightedCompatiblePools(GameTestHelper h) {
    var registry = h.getLevel().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
    for (var biome : List.of("plains", "desert", "savanna", "snowy", "taiga")) {
      var pool =
          registry.get(ResourceLocation.withDefaultNamespace("village/" + biome + "/houses"));
      var raw = ((VillagePoolAccess) pool).cyber$raw();
      h.assertTrue(
          raw.stream()
                  .filter(p -> p.getFirst() instanceof CityPoolElement)
                  .mapToInt(p -> p.getSecond())
                  .sum()
              == 17,
          "Missing city weights for " + biome);
      int old = pool.size();
      VillageServices.inject(pool);
      h.assertTrue(pool.size() == old, "Duplicate pool injection");
      h.assertTrue(
          raw.stream().anyMatch(p -> !(p.getFirst() instanceof CityPoolElement)),
          "Vanilla houses replaced");
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void cityPoolElementSurvivesSaveCodec(GameTestHelper h) {
    var ops = RegistryOps.create(NbtOps.INSTANCE, h.getLevel().registryAccess());
    var value = new CityPoolElement("afterlife");
    var encoded = StructurePoolElement.CODEC.encodeStart(ops, value).getOrThrow();
    var restored = StructurePoolElement.CODEC.parse(ops, encoded).getOrThrow();
    h.assertTrue(
        restored instanceof CityPoolElement, "Custom foundation element lost after save/load");
    h.assertTrue(
        restored
            .getSize(h.getLevel().getStructureManager(), Rotation.NONE)
            .equals(new Vec3i(13, 10, 13)),
        "Template size lost");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void ripperdocPurchaseAndInstallationAreSeparate(GameTestHelper h) {
    var p = player(h);
    var npc = resident(h, p, false);
    var m = new RipperdocMenu(1, p.getInventory(), npc.getId());
    p.containerMenu = m;
    p.getData(CityContent.ACCOUNT).credit(10000);
    h.assertTrue(m.clickMenuButton(p, 1000), "Valid purchase rejected");
    h.assertTrue(
        p.getData(CityContent.ACCOUNT).eddies() == 9000
            && p.getInventory().getItem(0).is(Cybernetics2077.OPTICS.get()),
        "Wrong charge or missing item");
    h.assertTrue(
        m.quickMoveStack(p, 48).isEmpty() && !m.getSlot(5).isActive(),
        "Purchase tab exposed installation");
    m.clickMenuButton(p, 11);
    h.assertTrue(
        m.quickMoveStack(p, 48).is(Cybernetics2077.OPTICS.get()) && m.getSlot(5).hasItem(),
        "NPC installation failed");
    h.assertTrue(!m.clickMenuButton(p, 1000), "Buy packet accepted in install tab");
    m.clickMenuButton(p, 10);
    m.clicked(5, 0, ClickType.PICKUP, p);
    h.assertTrue(m.getSlot(5).hasItem(), "Forged slot click stole installed item");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void purchaseRejectsFullInventoryNoMoneyDistanceAndDeadNpc(GameTestHelper h) {
    var p = player(h);
    var npc = resident(h, p, false);
    var m = new RipperdocMenu(1, p.getInventory(), npc.getId());
    p.containerMenu = m;
    h.assertTrue(!m.clickMenuButton(p, 1000), "Unfunded purchase allowed");
    p.getData(CityContent.ACCOUNT).credit(5000);
    for (int i = 0; i < 36; i++) p.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
    h.assertTrue(
        !m.clickMenuButton(p, 1000) && p.getData(CityContent.ACCOUNT).eddies() == 5000,
        "Full inventory charged");
    p.getInventory().clearContent();
    p.setPos(p.position().add(20, 0, 0));
    h.assertTrue(!m.stillValid(p) && !m.clickMenuButton(p, 1000), "Remote purchase allowed");
    p.setPos(npc.position().add(1, 0, 0));
    npc.discard();
    h.assertTrue(!m.stillValid(p), "Removed doctor remained usable");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void walletRoundTripAndFixerHaveNoFakeContracts(GameTestHelper h) {
    var p = player(h);
    var a = p.getData(CityContent.ACCOUNT);
    a.credit(123456789);
    var b = new StreetAccount();
    b.deserializeNBT(h.getLevel().registryAccess(), a.serializeNBT(h.getLevel().registryAccess()));
    h.assertTrue(b.eddies() == 123456789 && b.streetCred() == 0, "Wallet serialization lost value");
    var n = resident(h, p, true);
    var m = new FixerMenu(1, p.getInventory(), n.getId());
    h.assertTrue(
        m.stillValid(p) && m.slots.isEmpty() && !m.clickMenuButton(p, 0),
        "Fixer exposes an unfinished contract/reward");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void allTemplatesRotateAndEveryBlockSurvives(GameTestHelper h) {
    var level = h.getLevel();
    var mgr = level.getStructureManager();
    int reviewed = 0;
    for (var name : List.of("clinic_small", "clinic_large", "afterlife")) {
      var template = mgr.getOrCreate(Cybernetics2077.id("village/" + name));
      for (var rot : Rotation.values()) {
        var origin = h.absolutePos(new BlockPos(0, 40 + rot.ordinal() * 15, 0));
        var settings =
            new StructurePlaceSettings()
                .setKnownShape(true)
                .setRotation(rot)
                .setIgnoreEntities(true)
                .addProcessor(JigsawReplacementProcessor.INSTANCE);
        h.assertTrue(
            template.placeInWorld(level, origin, origin, settings, level.random, 18),
            "Template failed " + name + rot);
        var authored = template.save(new net.minecraft.nbt.CompoundTag());
        var palette = authored.getList("palette", 10);
        for (var raw : authored.getList("blocks", 10)) {
          var tag = (net.minecraft.nbt.CompoundTag) raw;
          var coords = tag.getList("pos", 3);
          var local = new BlockPos(coords.getInt(0), coords.getInt(1), coords.getInt(2));
          var pos = StructureTemplate.calculateRelativePosition(settings, local).offset(origin);
          var expected =
              net.minecraft.nbt.NbtUtils.readBlockState(
                      level.holderLookup(Registries.BLOCK),
                      palette.getCompound(tag.getInt("state")))
                  .rotate(rot);
          if (expected.is(Blocks.JIGSAW)) expected = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
          h.assertTrue(
              level.getBlockState(pos).equals(expected),
              "Authored block lost during placement "
                  + name
                  + rot
                  + " "
                  + local
                  + " expected "
                  + expected
                  + " got "
                  + level.getBlockState(pos));
        }
        var size = template.getSize();
        for (int x = 0; x < size.getX(); x++)
          for (int y = 0; y < size.getY(); y++)
            for (int z = 0; z < size.getZ(); z++) {
              var pos =
                  StructureTemplate.calculateRelativePosition(settings, new BlockPos(x, y, z))
                      .offset(origin);
              var state = level.getBlockState(pos);
              h.assertTrue(
                  state.canSurvive(level, pos),
                  "Unsupported block " + name + rot + " " + pos + " " + state);
              if (!state.isAir()) reviewed++;
            }
      }
    }
    h.assertTrue(reviewed > 8000, "Incomplete structure audit");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void templatesSpawnExactlyTheirIntendedResidents(GameTestHelper h) {
    var level = h.getLevel();
    var mgr = level.getStructureManager();
    var positions = new ArrayList<BlockPos>();
    int dy = 110;
    for (var name : List.of("clinic_small", "clinic_large", "afterlife")) {
      var origin = h.absolutePos(new BlockPos(0, dy, 0));
      positions.add(origin);
      var template = mgr.getOrCreate(Cybernetics2077.id("village/" + name));
      template.placeInWorld(
          level,
          origin,
          origin,
          new StructurePlaceSettings()
              .setKnownShape(true)
              .setFinalizeEntities(true)
              .addProcessor(JigsawReplacementProcessor.INSTANCE),
          level.random,
          18);
      dy += 22;
    }
    h.runAfterDelay(
        5,
        () -> {
          int index = 0;
          for (var name : List.of("clinic_small", "clinic_large", "afterlife")) {
            var origin = positions.get(index++);
            var template = mgr.getOrCreate(Cybernetics2077.id("village/" + name));
            var residents =
                level.getEntitiesOfClass(
                    CityResident.class,
                    new AABB(
                        Vec3.atLowerCornerOf(origin),
                        Vec3.atLowerCornerOf(origin.offset(template.getSize()))));
            h.assertTrue(
                residents.size() == (name.equals("afterlife") ? 2 : 1),
                "Missing/duplicate residents " + name + ": " + residents.size());
            for (var n : residents)
              h.assertTrue(
                  n.isPersistenceRequired() && n.fixer() == name.equals("afterlife"),
                  "Wrong resident role/persistence");
          }
          h.succeed();
        });
  }

  @GameTest(template = "empty", timeoutTicks = 100)
  public static void foundationsFillTerrainGapsOnlyOnceAtGeneration(GameTestHelper h) {
    var level = h.getLevel();
    var origin = h.absolutePos(new BlockPos(0, 200, 0));
    for (int x = 0; x < 11; x++)
      for (int z = 0; z < 13; z++) {
        level.setBlock(origin.offset(x, -4, z), Blocks.STONE.defaultBlockState(), 3);
        for (int y = -3; y < 9; y++)
          level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
      }
    var element = new CityPoolElement("clinic_small");
    var clip =
        new BoundingBox(
            origin.getX(),
            level.getMinBuildHeight(),
            origin.getZ(),
            origin.getX() + 10,
            level.getMaxBuildHeight(),
            origin.getZ() + 12);
    h.assertTrue(
        element.place(
            level.getStructureManager(),
            level,
            level.structureManager(),
            level.getChunkSource().getGenerator(),
            origin,
            origin,
            Rotation.NONE,
            clip,
            level.random,
            LiquidSettings.IGNORE_WATERLOGGING,
            false),
        "Foundation placement failed");
    for (int x = 0; x < 11; x++)
      for (int z = 0; z < 13; z++)
        for (int y = -3; y < 0; y++)
          h.assertTrue(
              level.getBlockState(origin.offset(x, y, z)).is(Blocks.DEEPSLATE_BRICKS),
              "Foundation gap remains");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 1200)
  public static void naturalVillageLayoutsCanSelectAllThreeBuildings(GameTestHelper h) {
    var l = h.getLevel();
    var generator = l.getChunkSource().getGenerator();
    var registry = l.registryAccess().registryOrThrow(Registries.STRUCTURE);
    for (var biome : List.of("plains", "desert", "savanna", "snowy", "taiga")) {
      var structure = registry.get(ResourceLocation.withDefaultNamespace("village_" + biome));
      var counts = new TreeMap<String, Integer>();
      int villages = 0;
      for (int seed = 0; seed < 192; seed++) {
        var start =
            structure.generate(
                l.registryAccess(),
                generator,
                generator.getBiomeSource(),
                l.getChunkSource().randomState(),
                l.getStructureManager(),
                seed * 9973L,
                new net.minecraft.world.level.ChunkPos(seed * 32, 2048),
                0,
                l,
                b -> true);
        h.assertTrue(start.isValid(), "Village layout invalid " + biome + "/" + seed);
        villages++;
        for (var piece : start.getPieces())
          if (piece instanceof PoolElementStructurePiece p
              && p.getElement() instanceof CityPoolElement city) {
            counts.merge(city.toString(), 1, Integer::sum);
            var jigs =
                city.getShuffledJigsawBlocks(
                    l.getStructureManager(), p.getPosition(), p.getRotation(), l.random);
            h.assertTrue(jigs.size() == 3, "Invalid road attachment count");
            h.assertTrue(
                jigs.stream()
                    .anyMatch(
                        connection -> {
                          var outside =
                              connection
                                  .pos()
                                  .relative(JigsawBlock.getFrontFacing(connection.state()));
                          return start.getPieces().stream()
                              .anyMatch(
                                  other -> other != p && other.getBoundingBox().isInside(outside));
                        }),
                "Building does not connect to village road " + city);
          }
      }
      h.assertTrue(
          counts.keySet().stream().anyMatch(k -> k.contains("clinic_small"))
              && counts.keySet().stream().anyMatch(k -> k.contains("clinic_large"))
              && counts.keySet().stream().anyMatch(k -> k.contains("afterlife")),
          "Missing building in natural layouts " + biome + ": " + counts);
      System.out.println("Village layout audit " + biome + " (" + villages + " seeds): " + counts);
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void everyAuthoredPaletteEntryResolves(GameTestHelper h) throws Exception {
    for (var name : List.of("clinic_small", "clinic_large", "afterlife")) {
      var resource =
          h.getLevel()
              .getServer()
              .getResourceManager()
              .getResourceOrThrow(Cybernetics2077.id("structure/village/" + name + ".nbt"));
      try (var stream = resource.open()) {
        var nbt =
            net.minecraft.nbt.NbtIo.readCompressed(
                stream, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        for (var tag : nbt.getList("palette", 10)) {
          var n = (net.minecraft.nbt.CompoundTag) tag;
          var id = ResourceLocation.parse(n.getString("Name"));
          h.assertTrue(
              net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id),
              "Unknown authored block " + name + ": " + id);
          var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id);
          var props = n.getCompound("Properties");
          for (var key : props.getAllKeys()) {
            var property = block.getStateDefinition().getProperty(key);
            h.assertTrue(
                property != null && property.getValue(props.getString(key)).isPresent(),
                "Invalid block state " + id + " " + key);
          }
        }
      }
    }
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void supplyBarrelsProvideSpendableCurrency(GameTestHelper h) {
    var level = h.getLevel();
    var origin = h.absolutePos(new BlockPos(0, 150, 0));
    var template =
        level.getStructureManager().getOrCreate(Cybernetics2077.id("village/clinic_small"));
    template.placeInWorld(
        level,
        origin,
        origin,
        new StructurePlaceSettings()
            .setKnownShape(true)
            .setIgnoreEntities(true)
            .addProcessor(JigsawReplacementProcessor.INSTANCE),
        level.random,
        18);
    var locker =
        (net.minecraft.world.level.block.entity.BarrelBlockEntity)
            level.getBlockEntity(origin.offset(2, 1, 10));
    int credits = 0;
    for (int slot = 0; slot < locker.getContainerSize(); slot++)
      if (locker.getItem(slot).is(CityContent.EDDIES.get()))
        credits += locker.getItem(slot).getCount();
    h.assertTrue(credits >= 1 && credits <= 3, "Clinic currency loot missing/incorrect");
    h.succeed();
  }

  @GameTest(template = "empty", timeoutTicks = 60)
  public static void afterlifeBasementKeepsItsEntranceAtStreetHeight(GameTestHelper h) {
    var level = h.getLevel();
    var origin = h.absolutePos(new BlockPos(0, 250, 0));
    var city = new CityPoolElement("afterlife");
    var clip =
        new BoundingBox(
            origin.getX(),
            level.getMinBuildHeight(),
            origin.getZ(),
            origin.getX() + 12,
            level.getMaxBuildHeight() - 1,
            origin.getZ() + 12);
    h.assertTrue(
        city.getBoundingBox(level.getStructureManager(), origin, Rotation.NONE).minY()
            == origin.getY(),
        "Lot did not reserve street height");
    h.assertTrue(
        city.place(
            level.getStructureManager(),
            level,
            level.structureManager(),
            level.getChunkSource().getGenerator(),
            origin,
            origin,
            Rotation.NONE,
            clip,
            level.random,
            LiquidSettings.IGNORE_WATERLOGGING,
            false),
        "Sunken bar placement failed");
    h.assertTrue(
        level.getBlockState(origin.offset(6, 0, 0)).is(Blocks.POLISHED_DEEPSLATE),
        "Entrance did not align to street");
    h.assertTrue(
        level
            .getBlockState(origin.offset(6, -2, 6))
            .is(CityContent.BLOCK_SET.get("ribbed_floor").get()),
        "Bar floor not two blocks below street");
    h.succeed();
  }
}
