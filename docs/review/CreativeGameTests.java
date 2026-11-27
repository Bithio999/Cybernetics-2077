package dev.cybernetics2077.review;

import dev.cybernetics2077.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Cybernetics2077.ID)
@PrefixGameTestTemplate(false)
public final class CreativeGameTests {
  @GameTest(template = "empty", timeoutTicks = 60)
  public static void creativeTabContainsEveryCurrentItemOnceAndKeepsTierOrder(GameTestHelper h) {
    var list = CreativeContent.items();
    var actual = new HashSet<>(list);
    var expected = new HashSet<Item>();
    for (var item : BuiltInRegistries.ITEM) {
      var id = BuiltInRegistries.ITEM.getKey(item);
      if (id.getNamespace().equals(Cybernetics2077.ID)
          && !id.getPath().equals("mantis_claws_tier_1")
          && !id.getPath().startsWith("review_")) expected.add(item);
    }
    h.assertTrue(
        actual.size() == list.size() && actual.equals(expected),
        "Creative coverage mismatch: expected "
            + expected.size()
            + " / got "
            + list.size()
            + " missing="
            + expected.stream()
                .filter(i -> !actual.contains(i))
                .map(i -> BuiltInRegistries.ITEM.getKey(i).toString())
                .toList());
    for (var family :
        List.of(
            Cybernetics2077.OPTICS_TIERS,
            Cybernetics2077.CYBERDECK_TIERS,
            Cybernetics2077.MANTIS_TIERS)) {
      int prior = -1;
      for (var item : family) {
        int index = list.indexOf(item.get());
        h.assertTrue(index > prior, "Legacy registry name broke family tier order");
        prior = index;
      }
    }
    var tab = CreativeContent.TAB.get();
    tab.buildContents(
        new CreativeModeTab.ItemDisplayParameters(
            FeatureFlags.DEFAULT_FLAGS, false, h.getLevel().registryAccess()));
    h.assertTrue(
        tab.hasSearchBar()
            && tab.getDisplayItems().size() == expected.size()
            && tab.getSearchTabDisplayItems().size() == expected.size(),
        "Native tab/search content missing");
    h.succeed();
  }
}
