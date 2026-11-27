package dev.cybernetics2077.city;

import dev.cybernetics2077.Cybernetics2077;
import dev.cybernetics2077.mob.*;
import dev.cybernetics2077.scanner.ScannerNetwork;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Owner-bound, persistent bounty objectives. All mutations and rewards run on the server thread.
 */
@EventBusSubscriber(modid = Cybernetics2077.ID)
public final class BountyContracts {
  public static final String MOB_DATA = "cybernetics2077:bounty";

  public static BountyRules.Kind kind(CompoundTag job) {
    return BountyRules.Kind.from(job.getString("kind"));
  }

  public static String objective(CompoundTag job) {
    if (job.getInt("stage") == 4) return "RECOVER CACHE / USE CONTRACT SHARD NEARBY";
    return kind(job).objective;
  }

  public static long day(MinecraftServer server) {
    return Math.floorDiv(server.overworld().getDayTime(), 24000);
  }

  public static CompoundTag job(ServerPlayer p) {
    return BountyLedger.get(p.server).job(p.getUUID());
  }

  private static void save(ServerPlayer p, CompoundTag job) {
    BountyLedger.get(p.server).put(p.getUUID(), job);
  }

  public static List<BountyRules.Offer> offers(ServerPlayer p, CityResident fixer) {
    return BountyRules.offers(
        fixer.getUUID(),
        p.getUUID(),
        day(p.server),
        p.getData(CityContent.ACCOUNT).streetCred(),
        fixer.style());
  }

  public static CompoundTag offerTag(BountyRules.Offer offer) {
    var t = new CompoundTag();
    t.putUUID("offer", offer.id());
    t.putLong("day", offer.day());
    t.putInt("slot", offer.slot());
    t.putInt("rank", offer.rank());
    t.putInt("profile", offer.profile());
    t.putString("kind", offer.kind().id);
    t.putString("title", offer.kind().label + ": " + offer.alias());
    t.putString("alias", offer.alias());
    t.putString("mob", offer.mob());
    t.putInt("tier", BountyRules.tier(offer.rank()));
    t.putInt("implants", BountyRules.implants(offer.rank(), offer.kind()));
    t.putInt("guards", BountyRules.guards(offer.rank()));
    t.putInt("eddies", BountyRules.pay(offer.rank(), offer.kind()));
    t.putInt("cred", BountyRules.cred(offer.rank()));
    return t;
  }

  private static boolean valid(ServerPlayer p, CityResident fixer) {
    return p.isAlive()
        && !p.isSpectator()
        && fixer.isAlive()
        && fixer.fixer()
        && p.level() == fixer.level()
        && p.distanceToSqr(fixer) <= 64;
  }

  /** 10 active, 11 already taken, 12 stale offer, 13 Peaceful, 14 wrong dimension, 2 full. */
  public static int accept(ServerPlayer p, CityResident fixer, BountyRules.Offer offer) {
    if (!valid(p, fixer)) return 12;
    if (p.level().dimension() != Level.OVERWORLD) return 14;
    if (p.level().getDifficulty() == Difficulty.PEACEFUL) return 13;
    if (!job(p).isEmpty()) return 10;
    if (!offers(p, fixer).contains(offer)) return 12;
    var account = p.getData(CityContent.ACCOUNT);
    if (account.bountyUsed(offer.day(), offer.id())) return 11;
    if (p.getInventory().getFreeSlot() < 0 && !hasShard(p)) return 2;
    var t = offerTag(offer);
    t.putUUID("owner", p.getUUID());
    t.putUUID("token", UUID.randomUUID());
    t.putUUID("fixer", fixer.getUUID());
    t.putString("fixerName", fixer.getName().getString());
    t.putInt("fixerX", fixer.getBlockX());
    t.putInt("fixerY", fixer.getBlockY());
    t.putInt("fixerZ", fixer.getBlockZ());
    t.putString("dimension", Level.OVERWORLD.location().toString());
    var random = new SplittableRandom(offer.id().getMostSignificantBits());
    double angle = random.nextDouble() * Math.PI * 2,
        distance = 192 + offer.rank() * 48 + random.nextInt(97);
    int x = fixer.getBlockX() + (int) Math.round(Math.cos(angle) * distance);
    int z = fixer.getBlockZ() + (int) Math.round(Math.sin(angle) * distance);
    if (!p.level().getWorldBorder().isWithinBounds(new BlockPos(x, fixer.getBlockY(), z)))
      return 14;
    t.putInt("x", x);
    t.putInt("y", fixer.getBlockY());
    t.putInt("z", z);
    t.putInt("stage", 1);
    t.putBoolean("civilians", true);
    t.putBoolean("survival", true);
    t.putLong("accepted", p.server.overworld().getGameTime());
    removeShards(p);
    var shard = shard(t);
    if (!p.getInventory().add(shard)) return 2;
    save(p, t);
    account.reserveBounty(offer.day(), offer.id());
    changed(p);
    say(
        p,
        fixer,
        offer.kind() == BountyRules.Kind.CYBERPSYCHO
            ? "Bring them down to critical health. Your last hit will contain them. Keep them alive"
                + " and return for debrief."
            : offer.kind() == BountyRules.Kind.RECOVERY
                ? "Find the courier. Once they're down, use your contract shard beside the sealed"
                    + " cache. Then bring the payload back."
                : fixer.style() == 0
                    ? "The dossier is yours. Confirm the mark through your scanner before you"
                        + " engage. Keep civilians out of it."
                    : "That's your mark. Once the fight starts, finish it inside ninety seconds."
                        + " Get back here for payment.");
    return 0;
  }

  private static ItemStack shard(CompoundTag job) {
    var stack = new ItemStack(CityContent.BOUNTY_CONTRACT.get());
    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(job.copy()));
    return stack;
  }

  private static boolean owned(ItemStack stack, ServerPlayer p) {
    var t = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    return stack.is(CityContent.BOUNTY_CONTRACT.get())
        && t.hasUUID("owner")
        && t.getUUID("owner").equals(p.getUUID());
  }

  private static boolean hasShard(ServerPlayer p) {
    for (int i = 0; i < 36; i++) if (owned(p.getInventory().getItem(i), p)) return true;
    return false;
  }

  private static void removeShards(ServerPlayer p) {
    for (int i = 0; i < p.getInventory().getContainerSize(); i++)
      if (owned(p.getInventory().getItem(i), p)) p.getInventory().setItem(i, ItemStack.EMPTY);
  }

  public static int reissue(ServerPlayer p, CityResident fixer) {
    var job = job(p);
    if (!valid(p, fixer) || job.isEmpty()) return 10;
    if (p.getInventory().getFreeSlot() < 0 && !hasShard(p)) return 2;
    removeShards(p);
    if (!p.getInventory().add(shard(job))) return 2;
    changed(p);
    return 0;
  }

  private static void changed(ServerPlayer p) {
    var current = job(p);
    for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
      var stack = p.getInventory().getItem(i);
      if (owned(stack, p) && !current.isEmpty())
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(current.copy()));
    }
    p.getInventory().setChanged();
    p.inventoryMenu.broadcastChanges();
    ScannerNetwork.sync(p);
  }

  private static void say(ServerPlayer p, CityResident fixer, String text) {
    p.sendSystemMessage(
        Component.empty()
            .append(fixer.getName().copy().withStyle(ChatFormatting.AQUA))
            .append(Component.literal(": " + text).withStyle(ChatFormatting.WHITE)));
  }

  public static boolean specialty(CompoundTag job) {
    return job.getInt("profile") == 0
        ? job.getBoolean("scanned")
        : job.getBoolean("engaged") && job.getLong("finished") - job.getLong("engagement") <= 1800;
  }

  public static int claim(ServerPlayer p, CityResident fixer) {
    var job = job(p);
    if (!valid(p, fixer) || job.getInt("stage") != 2) return 20;
    if (fixer.style() != job.getInt("profile")) return 21;
    var account = p.getData(CityContent.ACCOUNT);
    boolean special = specialty(job),
        civilians = job.getBoolean("civilians"),
        survival = job.getBoolean("survival");
    int percent = BountyRules.bonusPercent(special, civilians, survival);
    boolean chain = (account.favor(fixer.style()) + 1) % 3 == 0;
    int base = job.getInt("eddies"),
        bonus = base * percent / 100,
        chainBonus = chain ? base / 4 : 0;
    int payout = base + bonus + chainBonus;
    int before = account.streetCred();
    // Consume the authoritative job before paying. A duplicate packet/shard cannot pay again.
    BountyLedger.get(p.server).remove(p.getUUID());
    cleanup(p.server, job);
    account.credit(payout);
    account.reputation(job.getInt("cred") + (special ? 1 : 0));
    account.completedBounty(fixer.style(), job.getInt("rank"));
    boolean chip = account.legends() >= 3 && !account.iconicRewardClaimed();
    if (chip) {
      account.iconicRewardClaimed(true);
      var reward = new ItemStack(Cybernetics2077.ICONIC_REQUISITIONS.get(4).get());
      if (!p.getInventory().add(reward)) p.drop(reward, false);
    }
    var report = job.copy();
    report.putInt("eddies", base);
    report.putInt("stage", 3);
    report.putBoolean("specialty", special);
    report.putInt("bonus", bonus);
    report.putInt("chainBonus", chainBonus);
    report.putInt("payout", payout);
    report.putInt("credEarned", account.streetCred() - before);
    report.putBoolean("chip", chip);
    report.putString(
        "rating", BountyRules.rating(BountyRules.optionalCount(special, civilians, survival)));
    account.bountyReport(report);
    removeShards(p);
    changed(p);
    say(
        p,
        fixer,
        chain
            ? "Three jobs confirmed. Your loyalty bonus is included."
            : fixer.style() == 0
                ? "Mark confirmed. Your work has been reviewed; payment is cleared."
                : "Confirmed. Good hunting. Payment is yours.");
    p.sendSystemMessage(
        Component.literal(
                "BOUNTY "
                    + report.getString("rating")
                    + " // €$"
                    + payout
                    + " / +"
                    + report.getInt("credEarned")
                    + " Street Cred")
            .withStyle(ChatFormatting.GOLD));
    return 0;
  }

  public static boolean abandon(ServerPlayer p, CityResident fixer) {
    var job = job(p);
    if (!valid(p, fixer) || job.isEmpty()) return false;
    BountyLedger.get(p.server).remove(p.getUUID());
    cleanup(p.server, job);
    removeShards(p);
    changed(p);
    return true;
  }

  private static void cleanup(MinecraftServer server, CompoundTag job) {
    var level = server.overworld();
    if (job.hasUUID("cache")) {
      var cache = level.getEntity(job.getUUID("cache"));
      if (cache != null) cache.discard();
    }
    if (job.hasUUID("target")) {
      var e = level.getEntity(job.getUUID("target"));
      if (e != null && e.isAlive()) e.discard();
    }
    for (var raw : job.getList("guardIds", Tag.TAG_STRING)) {
      try {
        var e = level.getEntity(UUID.fromString(raw.getAsString()));
        if (e != null && e.isAlive()) e.discard();
      } catch (IllegalArgumentException ignored) {
      }
    }
  }

  public static CompoundTag navigation(ServerPlayer p) {
    var job = job(p);
    if (job.isEmpty() || job.getInt("stage") == 3) return DeliveryContracts.navigation(p);
    var nav = new CompoundTag();
    nav.putInt("stage", 1);
    nav.putString("dimension", job.getString("dimension"));
    boolean returning = job.getInt("stage") == 2;
    nav.putInt("x", job.getInt(returning ? "fixerX" : "x"));
    nav.putInt("y", job.getInt(returning ? "fixerY" : "y"));
    nav.putInt("z", job.getInt(returning ? "fixerZ" : "z"));
    nav.putString("title", job.getString("title"));
    nav.putString(
        "objective",
        returning
            ? "RETURN TO " + BountyRules.profileName(job.getInt("profile"))
            : job.getInt("stage") == 4
                ? "RECOVER CACHE / USE CONTRACT SHARD NEARBY"
                : job.getBoolean("spawned") ? objective(job) : "SEARCH AREA / APPROACH THE LEAD");
    return nav;
  }

  public static CompoundTag board(ServerPlayer p, CityResident fixer) {
    var t = new CompoundTag();
    t.putLong("day", day(p.server));
    t.putInt("profile", fixer.style());
    t.putInt(
        "restock",
        (int) ((24000 - Math.floorMod(p.server.overworld().getDayTime(), 24000) + 19) / 20));
    var account = p.getData(CityContent.ACCOUNT);
    var list = new ListTag();
    for (var offer : offers(p, fixer)) {
      var row = offerTag(offer);
      row.putBoolean("used", account.bountyUsed(offer.day(), offer.id()));
      list.add(row);
    }
    t.put("offers", list);
    t.put("job", job(p));
    t.put("report", account.bountyReport());
    t.putInt("favor", account.favor(fixer.style()));
    t.putInt("legends", account.legends());
    t.putBoolean("iconicReward", account.iconicRewardClaimed());
    return t;
  }

  /** Only already-loaded ground is inspected. The objective never edits terrain. */
  public static Optional<BlockPos> safeGround(
      ServerLevel level, BlockPos center, ServerPlayer player, Set<BlockPos> occupied) {
    for (int i = 0; i < 96; i++) {
      double a = i * 2.3999632297;
      int radius = i == 0 ? 0 : 4 + i / 3;
      int x = center.getX() + (int) Math.round(Math.cos(a) * radius),
          z = center.getZ() + (int) Math.round(Math.sin(a) * radius);
      var column = new BlockPos(x, 0, z);
      if (!level.hasChunkAt(column) || !level.getWorldBorder().isWithinBounds(column)) continue;
      int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
      var feet = new BlockPos(x, y, z);
      if (Math.abs(y - player.getY()) > 24) continue;
      if (y < level.getMinBuildHeight() + 1
          || y + 2 >= level.getMaxBuildHeight()
          || player.distanceToSqr(x + .5, y, z + .5) < 16 * 16) continue;
      if (occupied.stream().anyMatch(p -> p.distSqr(feet) < 9)) continue;
      if (!level.getFluidState(feet).isEmpty()
          || !level.getFluidState(feet.below()).isEmpty()
          || !level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
          || !level.getBlockState(feet.below()).isValidSpawn(level, feet.below(), EntityType.HUSK))
        continue;
      if (!level.noCollision(
          new net.minecraft.world.phys.AABB(x + .1, y, z + .1, x + .9, y + 2, z + .9))) continue;
      return Optional.of(feet);
    }
    return Optional.empty();
  }

  public static boolean deploy(ServerPlayer player) {
    var job = job(player);
    if (job.getInt("stage") != 1
        || job.getBoolean("spawned")
        || player.level().dimension() != Level.OVERWORLD
        || player.level().getDifficulty() == Difficulty.PEACEFUL
        || player.isSpectator()
        || !player.isAlive()) return false;
    var level = player.serverLevel();
    double dx = job.getInt("x") + .5 - player.getX(), dz = job.getInt("z") + .5 - player.getZ();
    if (dx * dx + dz * dz > 96 * 96) return false;
    double length = Math.max(1, Math.hypot(dx, dz));
    var center =
        player.blockPosition().offset((int) (dx / length * 32), 0, (int) (dz / length * 32));
    var places = new ArrayList<BlockPos>();
    var occupied = new HashSet<BlockPos>();
    for (int i = 0; i <= job.getInt("guards"); i++) {
      var pos = safeGround(level, center, player, occupied);
      if (pos.isEmpty()) return false;
      places.add(pos.get());
      occupied.add(pos.get());
    }
    cleanup(player.server, job);
    var entities = new ArrayList<Mob>();
    var guards = new ListTag();
    for (int i = 0; i < places.size(); i++) {
      var id =
          ResourceLocation.tryParse(
              i == 0
                  ? job.getString("mob")
                  : (job.getInt("profile") == 0 ? "minecraft:skeleton" : "minecraft:husk"));
      var entity = BuiltInRegistries.ENTITY_TYPE.get(id).create(level);
      if (!(entity instanceof Mob mob)) return false;
      var pos = places.get(i);
      mob.moveTo(pos.getX() + .5, pos.getY(), pos.getZ() + .5, 0, 0);
      int tier = i == 0 ? job.getInt("tier") : Math.max(1, job.getInt("tier") - 1);
      EnhancedMobs.install(
          mob,
          i == 0
              ? BountyCombat.loadout(tier, job.getInt("implants"))
              : MobCyberwarePool.rollExactTier(
                  tier, Math.min(3, job.getInt("implants")), mob.getRandom()));
      if (i == 0)
        BountyCombat.apply(
            mob, job.getInt("rank"), kind(job) == BountyRules.Kind.CYBERPSYCHO, true);
      var marker = new CompoundTag();
      marker.putUUID("owner", player.getUUID());
      marker.putUUID("token", job.getUUID("token"));
      marker.putInt("role", i == 0 ? 0 : 1);
      marker.putInt("rank", job.getInt("rank"));
      marker.putString("kind", kind(job).id);
      mob.getPersistentData().put(MOB_DATA, marker);
      mob.setPersistenceRequired();
      mob.setCanPickUpLoot(false);
      Item weapon =
          mob.getType() == EntityType.SKELETON
              ? Items.BOW
              : mob.getType() == EntityType.PILLAGER
                  ? Items.CROSSBOW
                  : mob.getType() == EntityType.VINDICATOR
                      ? Items.IRON_AXE
                      : tier >= 3 ? Items.IRON_SWORD : Items.STONE_SWORD;
      mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon));
      mob.setDropChance(EquipmentSlot.MAINHAND, 0);
      var helmet = new ItemStack(Items.IRON_HELMET);
      helmet.set(DataComponents.UNBREAKABLE, new Unbreakable(false));
      mob.setItemSlot(EquipmentSlot.HEAD, helmet);
      mob.setDropChance(EquipmentSlot.HEAD, 0);
      if (mob instanceof net.minecraft.world.entity.monster.Zombie zombie) zombie.setBaby(false);
      mob.setTarget(player);
      entities.add(mob);
      if (i == 0) job.putUUID("target", mob.getUUID());
      else guards.add(StringTag.valueOf(mob.getUUID().toString()));
    }
    job.put("guardIds", guards);
    job.putBoolean("spawned", true);
    job.remove("missingSince");
    var pos = places.getFirst();
    job.putInt("x", pos.getX());
    job.putInt("y", pos.getY());
    job.putInt("z", pos.getZ());
    save(player, job);
    for (var mob : entities)
      if (!level.addFreshEntity(mob)) {
        entities.forEach(Entity::discard);
        job.putBoolean("spawned", false);
        save(player, job);
        return false;
      }
    changed(player);
    return true;
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void join(EntityJoinLevelEvent e) {
    if (e.getLevel().isClientSide) return;
    var entity = e.getEntity();
    var marker = entity.getPersistentData().getCompound(MOB_DATA);
    if (marker.getInt("role") == 2 && marker.hasUUID("owner")) {
      var job = BountyLedger.get(e.getLevel().getServer()).job(marker.getUUID("owner"));
      if (job.getInt("stage") != 4
          || !job.hasUUID("cache")
          || !job.getUUID("cache").equals(entity.getUUID())
          || !marker.hasUUID("token")
          || !job.hasUUID("token")
          || !marker.getUUID("token").equals(job.getUUID("token"))) {
        e.setCanceled(true);
        entity.discard();
      }
      return;
    }
    if (!(entity instanceof Mob mob)) return;
    if (!marker.hasUUID("owner") || !marker.hasUUID("token")) return;
    var job = BountyLedger.get(e.getLevel().getServer()).job(marker.getUUID("owner"));
    boolean valid =
        job.hasUUID("token")
            && marker.getUUID("token").equals(job.getUUID("token"))
            && (job.getInt("stage") == 1 || job.getInt("stage") == 2 || job.getInt("stage") == 4);
    if (valid && marker.getInt("role") == 0)
      valid =
          job.hasUUID("target")
              && mob.getUUID().equals(job.getUUID("target"))
              && (job.getInt("stage") == 1 || job.getBoolean("contained"));
    if (valid && marker.getInt("role") != 0)
      valid =
          job.getList("guardIds", Tag.TAG_STRING).stream()
              .anyMatch(t -> t.getAsString().equals(mob.getUUID().toString()));
    if (!valid) {
      e.setCanceled(true);
      mob.discard();
      return;
    }
    if (marker.getInt("role") == 0) {
      marker.putInt("rank", job.getInt("rank"));
      marker.putString("kind", kind(job).id);
    }
    if (marker.getInt("role") == 0)
      BountyCombat.apply(
          mob,
          Math.clamp(job.getInt("rank"), 1, 6),
          kind(job) == BountyRules.Kind.CYBERPSYCHO,
          false);
    if (marker.getInt("role") == 0)
      mob.setCustomName(
          Component.translatable(
                  "cybernetics2077.enhanced.name",
                  job.getString("alias"),
                  String.format(Locale.ROOT, "%.1f", EnhancedMobs.loadout(mob).averageTier()),
                  EnhancedMobs.loadout(mob).count())
              .withStyle(ChatFormatting.AQUA));
  }

  @SubscribeEvent
  public static void tick(ServerTickEvent.Post e) {
    var server = e.getServer();
    if (server.overworld().getGameTime() % 20 != 0) return;
    for (var player : server.getPlayerList().getPlayers()) {
      var job = job(player);
      if (job.getInt("stage") == 4) {
        if (server
                .overworld()
                .hasChunkAt(new BlockPos(job.getInt("x"), job.getInt("y"), job.getInt("z")))
            && (!job.hasUUID("cache")
                || server.overworld().getEntity(job.getUUID("cache")) == null))
          spawnCache(server, player.getUUID(), job);
        continue;
      }
      if (job.getInt("stage") != 1) continue;
      if (!job.getBoolean("spawned")) {
        deploy(player);
        continue;
      }
      var target = server.overworld().getEntity(job.getUUID("target"));
      if (target != null && target.isAlive()) {
        boolean returned = job.contains("missingSince");
        job.remove("missingSince");
        if (returned) save(player, job);
        if (target.getBlockX() != job.getInt("x")
            || target.getBlockY() != job.getInt("y")
            || target.getBlockZ() != job.getInt("z")) {
          job.putInt("x", target.getBlockX());
          job.putInt("y", target.getBlockY());
          job.putInt("z", target.getBlockZ());
          save(player, job);
          changed(player);
        }
      } else if (server
          .overworld()
          .hasChunkAt(new BlockPos(job.getInt("x"), job.getInt("y"), job.getInt("z")))) {
        long now = server.overworld().getGameTime();
        if (!job.contains("missingSince")) {
          job.putLong("missingSince", now);
          save(player, job);
        } else if (now - job.getLong("missingSince") >= 100) {
          job.putBoolean("spawned", false);
          save(player, job);
        }
      }
    }
  }

  public static void scanned(ServerPlayer p, LivingEntity target) {
    var job = job(p);
    if (!ScannerNetwork.installed(p)
        || job.getInt("stage") != 1
        || job.getBoolean("engaged")
        || job.getBoolean("scanned")
        || !job.hasUUID("target")
        || !target.getUUID().equals(job.getUUID("target"))) return;
    job.putBoolean("scanned", true);
    save(p, job);
    changed(p);
  }

  @SubscribeEvent
  public static void damage(LivingIncomingDamageEvent e) {
    if (!(e.getEntity().level() instanceof ServerLevel level) || e.getAmount() <= 0) return;
    var actor = e.getSource().getEntity();
    if (actor instanceof Mob attacker
        && e.getSource().is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) {
      var data = attacker.getPersistentData().getCompound(MOB_DATA);
      if (data.hasUUID("owner") && data.getInt("role") == 0)
        e.setAmount(
            e.getAmount()
                * (float)
                    BountyCombat.damage(
                        Math.clamp(data.getInt("rank"), 1, 6),
                        BountyRules.Kind.from(data.getString("kind"))
                            == BountyRules.Kind.CYBERPSYCHO));
    }
    UUID owner = ownedCredit(actor);
    var marker = e.getEntity().getPersistentData().getCompound(MOB_DATA);
    if (owner == null && e.getEntity() instanceof ServerPlayer p) {
      owner = p.getUUID();
      marker =
          actor instanceof Mob mob
              ? mob.getPersistentData().getCompound(MOB_DATA)
              : new CompoundTag();
    }
    if (owner == null) return;
    var ledger = BountyLedger.get(level.getServer());
    var job = ledger.job(owner);
    if (job.getInt("stage") != 1 || job.getBoolean("engaged")) return;
    if (!marker.hasUUID("token")
        || !job.hasUUID("token")
        || !marker.getUUID("token").equals(job.getUUID("token"))) return;
    job.putBoolean("engaged", true);
    job.putLong("engagement", level.getServer().overworld().getGameTime());
    ledger.put(owner, job);
  }

  private static UUID ownedCredit(Entity source) {
    return source instanceof ServerPlayer player
        ? player.getUUID()
        : source instanceof TamableAnimal pet ? pet.getOwnerUUID() : null;
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void death(LivingDeathEvent e) {
    if (!(e.getEntity().level() instanceof ServerLevel level)) return;
    if (e.getEntity() instanceof ServerPlayer p) {
      var job = job(p);
      if (job.getInt("stage") == 1 || job.getInt("stage") == 4) {
        job.putBoolean("survival", false);
        save(p, job);
      }
    }
    var killer = e.getSource().getEntity();
    var credit = killer instanceof LivingEntity living ? living : e.getEntity().getKillCredit();
    UUID credited = ownedCredit(credit);
    var ledger = BountyLedger.get(level.getServer());
    if (credited != null
        && (e.getEntity() instanceof AbstractVillager || e.getEntity() instanceof CityResident)) {
      var job = ledger.job(credited);
      if (job.getInt("stage") == 1 || job.getInt("stage") == 4) {
        job.putBoolean("civilians", false);
        ledger.put(credited, job);
      }
    }
    var marker = e.getEntity().getPersistentData().getCompound(MOB_DATA);
    if (!marker.hasUUID("owner") || marker.getInt("role") != 0) return;
    var job = ledger.job(marker.getUUID("owner"));
    if (job.getInt("stage") != 1
        || !job.hasUUID("target")
        || !job.getUUID("target").equals(e.getEntity().getUUID())) return;
    boolean success =
        marker.getUUID("owner").equals(credited) && kind(job) != BountyRules.Kind.CYBERPSYCHO;
    job.putInt("stage", success ? kind(job) == BountyRules.Kind.RECOVERY ? 4 : 2 : 3);
    job.putLong("finished", level.getServer().overworld().getGameTime());
    if (success && !job.getBoolean("engaged")) {
      job.putBoolean("engaged", true);
      job.putLong("engagement", job.getLong("finished"));
    }
    if (!success)
      job.putString(
          "failure",
          kind(job) == BountyRules.Kind.CYBERPSYCHO
              ? "The cyberpsycho died before containment."
              : "The mark was eliminated without your kill credit.");
    if (success && kind(job) == BountyRules.Kind.RECOVERY) {
      job.putInt("x", e.getEntity().getBlockX());
      job.putInt("y", e.getEntity().getBlockY());
      job.putInt("z", e.getEntity().getBlockZ());
    }
    ledger.put(marker.getUUID("owner"), job);
    if (success && kind(job) == BountyRules.Kind.RECOVERY)
      spawnCache(level.getServer(), marker.getUUID("owner"), job);
    var owner = level.getServer().getPlayerList().getPlayer(marker.getUUID("owner"));
    if (owner != null) {
      changed(owner);
      owner.sendSystemMessage(
          Component.literal(
                  success
                      ? kind(job) == BountyRules.Kind.RECOVERY
                          ? "COURIER DOWN // Use your contract shard beside the sealed cache."
                          : "BOUNTY CONFIRMED // Return to your fixer for payment."
                      : "BOUNTY LOST // Return to a fixer to clear the dossier.")
              .withStyle(success ? ChatFormatting.AQUA : ChatFormatting.RED));
    }
  }

  private static void spawnCache(MinecraftServer server, UUID owner, CompoundTag job) {
    var level = server.overworld();
    int x = job.getInt("x"), z = job.getInt("z");
    if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    var cache = EntityType.BLOCK_DISPLAY.create(level);
    if (cache == null) return;
    var data = cache.saveWithoutId(new CompoundTag());
    data.put(
        "block_state",
        NbtUtils.writeBlockState(
            net.minecraft.world.level.block.Blocks.BARREL.defaultBlockState()));
    cache.load(data);
    cache.setPos(x, y, z);
    cache.setInvulnerable(true);
    var marker = new CompoundTag();
    marker.putUUID("owner", owner);
    marker.putUUID("token", job.getUUID("token"));
    marker.putInt("role", 2);
    cache.getPersistentData().put(MOB_DATA, marker);
    job.putUUID("cache", cache.getUUID());
    job.putInt("y", y);
    BountyLedger.get(server).put(owner, job);
    level.addFreshEntity(cache);
  }

  public static boolean recover(ServerPlayer p) {
    var job = job(p);
    if (kind(job) != BountyRules.Kind.RECOVERY
        || job.getInt("stage") != 4
        || p.level().dimension() != Level.OVERWORLD
        || !p.isAlive()
        || p.isSpectator()) return false;
    var center =
        new net.minecraft.world.phys.Vec3(
            job.getInt("x") + .5, job.getInt("y") + .4, job.getInt("z") + .5);
    if (p.distanceToSqr(center) > 16
        || p.level()
                .clip(
                    new net.minecraft.world.level.ClipContext(
                        p.getEyePosition(),
                        center,
                        net.minecraft.world.level.ClipContext.Block.COLLIDER,
                        net.minecraft.world.level.ClipContext.Fluid.NONE,
                        p))
                .getType()
            != net.minecraft.world.phys.HitResult.Type.MISS) return false;
    if (!job.hasUUID("cache") || p.serverLevel().getEntity(job.getUUID("cache")) == null)
      return false;
    p.serverLevel().getEntity(job.getUUID("cache")).discard();
    job.putInt("stage", 2);
    job.putBoolean("recovered", true);
    job.putLong("finished", p.server.overworld().getGameTime());
    save(p, job);
    changed(p);
    p.sendSystemMessage(
        Component.literal("CYBERWARE SECURED // Return to your fixer for payment.")
            .withStyle(ChatFormatting.AQUA));
    return true;
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public static void contain(LivingDamageEvent.Pre e) {
    if (!(e.getEntity() instanceof Mob target)
        || !(target.level() instanceof ServerLevel level)
        || e.getNewDamage() <= 0
        || e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
    var marker = target.getPersistentData().getCompound(MOB_DATA);
    if (!marker.hasUUID("owner") || marker.getInt("role") != 0) return;
    var owner = marker.getUUID("owner");
    var job = BountyLedger.get(level.getServer()).job(owner);
    if (kind(job) != BountyRules.Kind.CYBERPSYCHO
        || job.getInt("stage") != 1
        || !job.hasUUID("target")
        || !job.getUUID("target").equals(target.getUUID())
        || !owner.equals(ownedCredit(e.getSource().getEntity()))
        || !marker.hasUUID("token")
        || !job.getUUID("token").equals(marker.getUUID("token"))) return;
    float threshold = Math.max(1, target.getMaxHealth() * .1f);
    if (target.getHealth() - e.getNewDamage() > threshold) return;
    e.setNewDamage(0);
    target.setHealth(Math.min(target.getHealth(), threshold));
    target.setTarget(null);
    target.setNoAi(true);
    target.setInvulnerable(true);
    target.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
    job.putInt("stage", 2);
    job.putBoolean("contained", true);
    job.putLong("finished", level.getServer().overworld().getGameTime());
    if (!job.getBoolean("engaged")) {
      job.putBoolean("engaged", true);
      job.putLong("engagement", job.getLong("finished"));
    }
    BountyLedger.get(level.getServer()).put(owner, job);
    var player = level.getServer().getPlayerList().getPlayer(owner);
    if (player != null) {
      changed(player);
      player.sendSystemMessage(
          Component.literal("CYBERPSYCHO CONTAINED // Return to your fixer.")
              .withStyle(ChatFormatting.AQUA));
    }
  }

  private BountyContracts() {}
}
