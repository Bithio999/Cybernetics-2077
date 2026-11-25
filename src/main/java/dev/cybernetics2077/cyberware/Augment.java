package dev.cybernetics2077.cyberware;

import java.util.List;
import java.util.Locale;

/** Current 2.x implant families. Starting tiers come from their obtainable versions. */
public enum Augment {
  EX_DISK("ex_disk", CyberwareCategory.FRONTAL_CORTEX, 3, 10),
  MEMORY_BOOST("memory_boost", CyberwareCategory.FRONTAL_CORTEX, 2, 18),
  BIODYNE_BERSERK("biodyne_berserk", CyberwareCategory.OPERATING_SYSTEM, 2, 20),
  CLAIRVOYANT("kiroshi_clairvoyant_optics", CyberwareCategory.FACE, 3, 8),
  BIONIC_JOINTS("bionic_joints", CyberwareCategory.SKELETON, 1, 4),
  EPIMORPHIC_SKELETON("epimorphic_skeleton", CyberwareCategory.SKELETON, 4, 32),
  NEOFIBER("neofiber", CyberwareCategory.NERVOUS_SYSTEM, 3, 14),
  ADRENALINE_CONVERTER("adrenaline_converter", CyberwareCategory.NERVOUS_SYSTEM, 1, 6),
  BLOOD_PUMP("blood_pump", CyberwareCategory.CIRCULATORY_SYSTEM, 2, 15),
  HEAL_ON_KILL("heal_on_kill", CyberwareCategory.CIRCULATORY_SYSTEM, 1, 10),
  SUBDERMAL_ARMOR("subdermal_armor", CyberwareCategory.INTEGUMENTARY_SYSTEM, 1, 4),
  OPTICAL_CAMO("optical_camo", CyberwareCategory.INTEGUMENTARY_SYSTEM, 2, 16),
  REINFORCED_TENDONS("reinforced_tendons", CyberwareCategory.LEGS, 2, 8),
  FORTIFIED_ANKLES("fortified_ankles", CyberwareCategory.LEGS, 2, 6),
  RAM_UPGRADE("ram_upgrade", CyberwareCategory.FRONTAL_CORTEX, 1, 8),
  SELF_ICE("self_ice", CyberwareCategory.FRONTAL_CORTEX, 3, 5),
  DOOMSAYER("kiroshi_doomsayer_optics", CyberwareCategory.FACE, 1, 6),
  STALKER("kiroshi_stalker_optics", CyberwareCategory.FACE, 1, 6),
  DENSE_MARROW("dense_marrow", CyberwareCategory.SKELETON, 2, 16),
  PARA_BELLUM("para_bellum", CyberwareCategory.SKELETON, 3, 25),
  STABBER("stabber", CyberwareCategory.NERVOUS_SYSTEM, 2, 12),
  VISUAL_CORTEX_SUPPORT("visual_cortex_support", CyberwareCategory.NERVOUS_SYSTEM, 1, 16),
  BIOMONITOR("biomonitor", CyberwareCategory.CIRCULATORY_SYSTEM, 1, 14),
  SECOND_HEART("second_heart", CyberwareCategory.CIRCULATORY_SYSTEM, 4, 30),
  SHOCK_N_AWE("shock_n_awe", CyberwareCategory.INTEGUMENTARY_SYSTEM, 1, 25),
  PAIN_EDITOR("pain_editor", CyberwareCategory.INTEGUMENTARY_SYSTEM, 4, 35),
  LYNX_PAWS("lynx_paws", CyberwareCategory.LEGS, 2, 5),
  JENKINS_TENDONS("jenkins_tendons", CyberwareCategory.LEGS, 2, 6),
  DYNALAR_SANDEVISTAN("dynalar_sandevistan", CyberwareCategory.OPERATING_SYSTEM, 2, 18),
  SMART_LINK("smart_link", CyberwareCategory.HANDS, 1, 4),
  BALLISTIC_COPROCESSOR("ballistic_coprocessor", CyberwareCategory.HANDS, 1, 2),
  MICROGENERATOR("microgenerator", CyberwareCategory.HANDS, 1, 9),
  HANDLE_WRAP("handle_wrap", CyberwareCategory.HANDS, 3, 8),
  SHOCK_ABSORBER("shock_absorber", CyberwareCategory.HANDS, 1, 12),
  IMMOVABLE_FORCE("immovable_force", CyberwareCategory.HANDS, 2, 35);

  public final String id;
  public final CyberwareCategory category;
  public final int firstTier, capacity;

  Augment(String id, CyberwareCategory category, int firstTier, int capacity) {
    this.id = id;
    this.category = category;
    this.firstTier = firstTier;
    this.capacity = capacity;
  }

  public String translation() {
    return "cybernetics2077.augment." + id;
  }

  public boolean active() {
    return this == BIODYNE_BERSERK
        || this == BLOOD_PUMP
        || this == OPTICAL_CAMO
        || this == DYNALAR_SANDEVISTAN;
  }

  public boolean optics() {
    return category == CyberwareCategory.FACE;
  }

  public int scanRange(int tier) {
    return this == DOOMSAYER || this == STALKER
        ? 24 + 4 * tier
        : CyberwareTier.of(tier).scanRange();
  }

  public double armor(int tier) {
    if (tier == 0) return 0;
    return switch (this) {
      case BIONIC_JOINTS -> tier + 1;
      case EPIMORPHIC_SKELETON -> tier * 2 - 2;
      case SUBDERMAL_ARMOR, PAIN_EDITOR -> tier;
      case SHOCK_N_AWE -> (tier + 1) / 2;
      default -> 0;
    };
  }

  public double toughness(int tier) {
    return tier
        * switch (this) {
          case BIONIC_JOINTS, PARA_BELLUM -> .3;
          case EPIMORPHIC_SKELETON, PAIN_EDITOR -> .4;
          case SUBDERMAL_ARMOR, SHOCK_N_AWE -> .2;
          default -> 0;
        };
  }

  public static String number(double value) {
    return String.format(Locale.ROOT, "%.1f", value);
  }

  public List<String> details(int t) {
    return switch (this) {
      case SMART_LINK ->
          List.of(
              "Scanning an enemy locks arrows / thrown tridents onto it.",
              "Lock: " + (2 + t) + " seconds; range: " + (24 + 4 * t) + " blocks.",
              "Limited steering; requires optics, line of sight and an unobstructed flight path.");
      case BALLISTIC_COPROCESSOR ->
          List.of(
              "Arrows ricochet once from solid surfaces; preview while drawing a bow.",
              "Retains " + (60 + 5 * t) + "% speed; +" + (5 * t) + "% damage after ricochet.",
              "Bank shots follow the reflected trajectory. Tridents are not bounced.");
      case MICROGENERATOR ->
          List.of(
              "A successful weapon hit discharges "
                  + (2 + 2 * t)
                  + " electrical damage within 3 blocks.",
              "At most 4 enemies; recharge: " + (12 - t) + " seconds.",
              "Supports melee, Mantis Blades, arrows and thrown tridents; cannot chain itself.");
      case HANDLE_WRAP ->
          List.of(
              "Drawing a melee weapon, deploying blades or throwing a trident primes a 6-second"
                  + " critical window.",
              "+" + (t * 4) + "% weapon crit chance during that window; 10-second rearm.",
              "Shares the existing 50% cyberware critical-chance cap.");
      case SHOCK_ABSORBER ->
          List.of(
              "Reduces knockback by "
                  + (30 + 10 * t)
                  + "% while drawing a bow, crossbow or trident.",
              "Does not grant damage reduction or affect other knockback.");
      case IMMOVABLE_FORCE ->
          List.of(
              "Standing still on the ground accelerates bow/crossbow/trident charging.",
              "One extra charge tick every "
                  + (8 - t)
                  + " ticks; fired arrows lose "
                  + (20 + 5 * t)
                  + "% less height to gravity.",
              "Charges and projectile flight remain server-authoritative.");
      case EX_DISK ->
          List.of(
              "+" + t + " maximum RAM with a cyberdeck.",
              "+" + (t * 5) + "% quickhack upload speed.");
      case MEMORY_BOOST ->
          List.of(
              "+1 maximum RAM with a cyberdeck.",
              "Kills restore " + number(.35 + .2 * t) + " RAM.");
      case BIODYNE_BERSERK ->
          List.of(
              "I: activate Berserk for " + (6 + t) + " seconds.",
              (t * 10) + "% damage reduction; 25% HP floor.",
              "+" + (5 + t * 5) + "% melee attack speed.",
              (t * 5 - 5) + "% crit chance; +" + ((t - 1) * 25) + "% crit damage.",
              "Melee only; no item use while active.",
              "On expiry: heal 25% HP per enemy killed.",
              "Cooldown: " + (60 - t * 5) + " seconds.");
      case CLAIRVOYANT ->
          List.of(
              "Enables optical scanning (" + CyberwareTier.of(t).scanRange() + " blocks).",
              "Scanning marks visible enemies within " + (t * 4) + " blocks.",
              "Marks remain highlighted for 60 seconds.");
      case BIONIC_JOINTS ->
          List.of("+" + (t + 1) + " armor and +" + number(toughness(t)) + " armor toughness.");
      case EPIMORPHIC_SKELETON ->
          List.of(
              "+" + (t * 2 - 2) + " armor and +" + number(toughness(t)) + " armor toughness.",
              "+" + ((t - 2) * 5) + "% maximum health.");
      case NEOFIBER ->
          List.of(
              (t * 2) + "% chance to mitigate incoming damage.",
              "Mitigation reduces that hit by " + (50 + t * 2) + "%.");
      case ADRENALINE_CONVERTER ->
          List.of(
              "Entering combat grants +" + (t * 8) + "% speed.",
              "Duration: " + (4 + t) + " seconds; 20-second rearm.",
              "Requires 5 quiet seconds before a new fight.");
      case BLOOD_PUMP ->
          List.of(
              "H: restore " + (10 + t * 5) + "% maximum HP immediately.",
              "Then heal " + number(t * .5) + "% HP/s for 6 seconds.",
              "2 charges; recharge: " + (50 - t * 5) + " seconds.");
      case HEAL_ON_KILL -> List.of("Kills restore " + (t + 1) + "% maximum HP.");
      case SUBDERMAL_ARMOR ->
          List.of("+" + t + " armor and +" + number(toughness(t)) + " armor toughness.");
      case OPTICAL_CAMO ->
          List.of(
              "C: cloak for " + (3 + t) + " seconds.",
              "Distant mobs cannot acquire you while cloaked.",
              "Cooldown: " + (70 - t * 5) + " seconds.");
      case REINFORCED_TENDONS ->
          List.of(
              "Press jump again in midair to double-jump.",
              "Second-jump impulse: " + number(.42 + t * .035) + ".",
              "Once per landing; normal fall damage applies.");
      case FORTIFIED_ANKLES ->
          List.of(
              "Hold jump on ground, then release to launch.",
              "Full charge: 1 second. Maximum impulse: " + number(.6 + t * .08) + ".",
              "Normal jumps work with a quick tap.");
      case DYNALAR_SANDEVISTAN ->
          List.of(
              "O: toggle Sandevistan. 50% relative slowing at every tier.",
              "32-block time bubble; overlapping fields never stack.",
              "Slows machines, block/fluid updates, loose objects and rain.",
              "Maximum duration: "
                  + dev.cybernetics2077.temporal.TemporalRules.duration(t)
                  + " seconds.",
              "Full recharge: "
                  + dev.cybernetics2077.temporal.TemporalRules.recharge(t)
                  + " seconds; charge is saved.",
              "+" + (t * 3) + "% weapon critical chance and critical damage while active.",
              "Early deactivation preserves charge; rearm: 1 second, minimum 20% charge.");
      case RAM_UPGRADE ->
          List.of(
              "+" + (1 + t / 3) + " maximum RAM with a cyberdeck.",
              "+" + String.format(Locale.ROOT, "%.2f", .06 * t) + " RAM recovered per second.");
      case SELF_ICE ->
          List.of(
              "Automatically blocks one incoming quickhack, including controls.",
              "+2 maximum RAM with a cyberdeck.",
              "Recharge: " + (50 - 6 * t) + " seconds. Reinstallation does not reset it.");
      case DOOMSAYER ->
          List.of(
              "Enables optical scanning (" + scanRange(t) + " blocks).",
              "Scanning marks creepers, primed TNT and fireballs within "
                  + (8 + t * 4)
                  + " blocks.",
              "Danger marks persist for 60 seconds; maximum 64 marks.");
      case STALKER ->
          List.of(
              "Enables optical scanning (" + scanRange(t) + " blocks).",
              "Scanning reveals enemies through cover in a forward cone, up to "
                  + (12 + t * 4)
                  + " blocks.",
              "Marks fade 3 seconds after losing focus. Hacking still needs line of sight.");
      case DENSE_MARROW ->
          List.of(
              "+" + (t * 6) + "% melee damage, including Mantis Blades.",
              "Melee hits add "
                  + String.format(Locale.ROOT, "%.3f", t * .004)
                  + " hunger exhaustion.");
      case PARA_BELLUM ->
          List.of(
              "+" + (t * 4) + "% total armor.", "+" + number(toughness(t)) + " armor toughness.");
      case STABBER ->
          List.of(
              "+" + (t * 4) + "% critical chance with swords, Mantis Blades and thrown tridents.",
              "Weapon criticals deal 1.5x damage before Berserk bonuses.");
      case VISUAL_CORTEX_SUPPORT ->
          List.of(
              "Weapon critical chance increases with distance.",
              "Up to +" + (t * 6) + "% at 32 blocks. Does not affect quickhacks.");
      case BIOMONITOR ->
          List.of(
              "Below 50% HP, automatically uses a Blood Pump charge or a healing potion.",
              "+" + (t * 4) + "% healing received. Checks health every half-second.",
              "Consumes real charges/items; at most once every 2 seconds.");
      case SECOND_HEART ->
          List.of(
              "A fatal hit restores 100% maximum HP.",
              "Recharge: " + (400 - t * 40) + " seconds; survives logout/reinstallation.",
              "Does not protect against the void or /kill.");
      case SHOCK_N_AWE ->
          List.of(
              "+" + number(armor(t)) + " armor and +" + number(toughness(t)) + " armor toughness.",
              (t * 2)
                  + "% chance when hurt to discharge "
                  + (2 + 2 * t)
                  + " electrical damage within 4 blocks.",
              "Maximum 8 enemies; 2-second internal cooldown; no chain reactions.");
      case PAIN_EDITOR ->
          List.of(
              "+" + t + " armor and +" + number(toughness(t)) + " armor toughness.",
              "Reduces incoming damage by " + (t * 2) + "%, including damaging quickhacks.");
      case LYNX_PAWS ->
          List.of(
              "50% quieter footsteps; +" + (t * 4) + "% crouching speed.",
              "Reduces fall damage by " + (t * 4) + "%. Does not silence sculk vibrations.");
      case JENKINS_TENDONS ->
          List.of(
              "Sprinting starts at +" + (t * 12) + "% movement speed.",
              "Tapers to +" + (t * 2) + "% over 5 seconds; recovers while not sprinting.");
    };
  }
}
