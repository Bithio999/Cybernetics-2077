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
  FORTIFIED_ANKLES("fortified_ankles", CyberwareCategory.LEGS, 2, 6);

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
    return this == BIODYNE_BERSERK || this == BLOOD_PUMP || this == OPTICAL_CAMO;
  }

  public static String number(double value) {
    return String.format(Locale.ROOT, "%.1f", value);
  }

  public List<String> details(int t) {
    return switch (this) {
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
      case BIONIC_JOINTS -> List.of("+" + (t + 1) + " armor while installed.");
      case EPIMORPHIC_SKELETON ->
          List.of(
              "+" + (t * 2 - 2) + " armor while installed.",
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
      case SUBDERMAL_ARMOR -> List.of("+" + t + " armor while installed.");
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
    };
  }
}
