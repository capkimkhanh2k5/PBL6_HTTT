import 'package:flutter/material.dart';

/// Coastal Avant-Garde Shapes, Spacing and Elevation Shadows for Vendor App
class AppShapes {
  AppShapes._();

  // ---------------- CORNER RADIUS VALUES ----------------
  static const double radiusSmValue = 8.0;
  static const double radiusDefaultValue = 16.0;
  static const double radiusMdValue = 20.0;
  static const double radiusLgValue = 24.0;
  static const double radiusXlValue = 32.0;
  static const double radiusFullValue = 9999.0;

  // Short aliases for radius values
  static const double rSm = radiusSmValue;
  static const double rDefault = radiusDefaultValue;
  static const double rMd = radiusMdValue;
  static const double rLg = radiusLgValue;
  static const double rXl = radiusXlValue;
  static const double rFull = radiusFullValue;

  // BorderRadius objects
  static const BorderRadius radiusSm = BorderRadius.all(Radius.circular(radiusSmValue));
  static const BorderRadius radiusDefault = BorderRadius.all(Radius.circular(radiusDefaultValue));
  static const BorderRadius radiusMd = BorderRadius.all(Radius.circular(radiusMdValue));
  static const BorderRadius radiusLg = BorderRadius.all(Radius.circular(radiusLgValue));
  static const BorderRadius radiusXl = BorderRadius.all(Radius.circular(radiusXlValue));
  static const BorderRadius radiusFull = BorderRadius.all(Radius.circular(radiusFullValue));

  // ---------------- SPACING SYSTEM ----------------
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
  static const List<BoxShadow> shadowLevel1 = [
    BoxShadow(
      color: Color(0x0D102F3A),
      blurRadius: 16,
      offset: Offset(0, 4),
    ),
    BoxShadow(
      color: Color(0x0A087F8C),
      blurRadius: 6,
      offset: Offset(0, 2),
    ),
  ];

  static const List<BoxShadow> shadowLevel2 = [
    BoxShadow(
      color: Color(0x14102F3A),
      blurRadius: 24,
      offset: Offset(0, 8),
    ),
    BoxShadow(
      color: Color(0x10087F8C),
      blurRadius: 12,
      offset: Offset(0, 4),
    ),
  ];

  // Aliases for shadows
  static const List<BoxShadow> shadowSm = shadowLevel1;
  static const List<BoxShadow> shadowMd = shadowLevel2;
}
