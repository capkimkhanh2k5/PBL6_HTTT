import 'package:flutter/material.dart';

/// Coastal Avant-Garde Shapes, Spacing and Elevation Shadows
/// Reference: stitch_reference/coastal_avant_garde/DESIGN.md
class AppShapes {
  AppShapes._();

  // ---------------- CORNER RADIUS ----------------
  static const double radiusSmValue = 8.0;
  static const double radiusDefaultValue = 16.0;
  static const double radiusMdValue = 24.0;
  static const double radiusLgValue = 32.0;
  static const double radiusXlValue = 48.0;
  static const double radiusFullValue = 9999.0;

  static const BorderRadius radiusSm = BorderRadius.all(Radius.circular(radiusSmValue));
  static const BorderRadius radiusDefault = BorderRadius.all(Radius.circular(radiusDefaultValue));
  static const BorderRadius radiusMd = BorderRadius.all(Radius.circular(radiusMdValue));
  static const BorderRadius radiusLg = BorderRadius.all(Radius.circular(radiusLgValue));
  static const BorderRadius radiusXl = BorderRadius.all(Radius.circular(radiusXlValue));
  static const BorderRadius radiusFull = BorderRadius.all(Radius.circular(radiusFullValue));

  // ---------------- SPACING SYSTEM (8PT GRID) ----------------
  static const double space2xs = 4.0;
  static const double spaceXs = 8.0;
  static const double spaceSm = 12.0;
  static const double spaceMd = 16.0;
  static const double spaceLg = 24.0;
  static const double spaceXl = 32.0;
  static const double space2xl = 48.0;
  static const double space3xl = 72.0;
  static const double gutterMobile = 16.0;

  // ---------------- ELEVATION SHADOWS ----------------
  /// Level 1: Flat/Card rest (subtle teal-tinted depth)
  static const List<BoxShadow> shadowLevel1 = [
    BoxShadow(
      color: Color(0x0D102F3A),
      blurRadius: 20,
      offset: Offset(0, 4),
    ),
    BoxShadow(
      color: Color(0x0A087F8C),
      blurRadius: 6,
      offset: Offset(0, 2),
    ),
  ];

  /// Level 2: Floating capsule / search bar / hovering cards
  static const List<BoxShadow> shadowLevel2 = [
    BoxShadow(
      color: Color(0x14102F3A),
      blurRadius: 32,
      offset: Offset(0, 12),
    ),
    BoxShadow(
      color: Color(0x14087F8C),
      blurRadius: 12,
      offset: Offset(0, 4),
    ),
  ];

  /// Level 3: Modal & Bottom Sheet
  static const List<BoxShadow> shadowLevel3 = [
    BoxShadow(
      color: Color(0x29102F3A),
      blurRadius: 48,
      offset: Offset(0, 24),
    ),
    BoxShadow(
      color: Color(0x14FF735C),
      blurRadius: 16,
      offset: Offset(0, 8),
    ),
  ];

  /// Top Bar Shadow
  static const List<BoxShadow> headerShadow = [
    BoxShadow(
      color: Color(0x0A000000),
      blurRadius: 8,
      offset: Offset(0, 1),
    ),
  ];

  /// Bottom Navigation Bar Shadow
  static const List<BoxShadow> bottomNavShadow = [
    BoxShadow(
      color: Color(0x0D000000),
      blurRadius: 12,
      offset: Offset(0, -2),
    ),
  ];
}
