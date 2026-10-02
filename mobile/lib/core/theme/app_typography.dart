import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'app_colors.dart';

/// Coastal Avant-Garde Typography System
/// Pairs kinetic 'Syne' for Display/Headlines with clear 'Plus Jakarta Sans' for Body/Labels.
/// Reference: stitch_reference/coastal_avant_garde/DESIGN.md
class AppTypography {
  AppTypography._();

  // ---------------- DISPLAY & HEADLINES (SYNE) ----------------

  /// Display Hero: 56px, w800, line-height 64px, tracking -0.03em
  static TextStyle displayHero({Color color = AppColors.onSurface}) {
    return GoogleFonts.syne(
      fontSize: 56,
      fontWeight: FontWeight.w800,
      height: 64 / 56,
      letterSpacing: -0.03 * 56,
      color: color,
    );
  }

  /// Display Hero Mobile: 38px, w800, line-height 44px, tracking -0.02em
  static TextStyle displayHeroMobile({Color color = AppColors.onSurface}) {
    return GoogleFonts.syne(
      fontSize: 38,
      fontWeight: FontWeight.w800,
      height: 44 / 38,
      letterSpacing: -0.02 * 38,
      color: color,
    );
  }

  /// Headline Lg: 36px, w700, line-height 44px, tracking -0.02em
  static TextStyle headlineLg({Color color = AppColors.onSurface}) {
    return GoogleFonts.syne(
      fontSize: 36,
      fontWeight: FontWeight.w700,
      height: 44 / 36,
      letterSpacing: -0.02 * 36,
      color: color,
    );
  }

  /// Headline Lg Mobile: 28px, w700, line-height 34px, tracking -0.01em
  static TextStyle headlineLgMobile({Color color = AppColors.onSurface}) {
    return GoogleFonts.syne(
      fontSize: 28,
      fontWeight: FontWeight.w700,
      height: 34 / 28,
      letterSpacing: -0.01 * 28,
      color: color,
    );
  }

  /// Headline Md: 24px, w700, line-height 32px, tracking -0.01em
  static TextStyle headlineMd({Color color = AppColors.onSurface}) {
    return GoogleFonts.syne(
      fontSize: 24,
      fontWeight: FontWeight.w700,
      height: 32 / 24,
      letterSpacing: -0.01 * 24,
      color: color,
    );
  }

  /// Headline Sm: 20px, w600 or w700, line-height 28px
  static TextStyle headlineSm({
    Color color = AppColors.onSurface,
    FontWeight fontWeight = FontWeight.w700,
  }) {
    return GoogleFonts.syne(
      fontSize: 20,
      fontWeight: fontWeight,
      height: 28 / 20,
      letterSpacing: 0,
      color: color,
    );
  }

  // ---------------- BODY & LABELS (PLUS JAKARTA SANS) ----------------

  /// Body Lg: 18px, w400, line-height 28px
  static TextStyle bodyLg({
    Color color = AppColors.onSurface,
    FontWeight fontWeight = FontWeight.w400,
  }) {
    return GoogleFonts.plusJakartaSans(
      fontSize: 18,
      fontWeight: fontWeight,
      height: 28 / 18,
      letterSpacing: -0.01 * 18,
      color: color,
    );
  }

  /// Body Md: 15px, w400/w500, line-height 24px
  static TextStyle bodyMd({
    Color color = AppColors.onSurface,
    FontWeight fontWeight = FontWeight.w400,
  }) {
    return GoogleFonts.plusJakartaSans(
      fontSize: 15,
      fontWeight: fontWeight,
      height: 24 / 15,
      letterSpacing: 0,
      color: color,
    );
  }

  /// Body Sm: 13px, w400, line-height 20px
  static TextStyle bodySm({
    Color color = AppColors.onSurfaceVariant,
    FontWeight fontWeight = FontWeight.w400,
  }) {
    return GoogleFonts.plusJakartaSans(
      fontSize: 13,
      fontWeight: fontWeight,
      height: 20 / 13,
      letterSpacing: 0.01 * 13,
      color: color,
    );
  }

  /// Label Lg: 14px, w700, line-height 20px
  static TextStyle labelLg({
    Color color = AppColors.onSurface,
    FontWeight fontWeight = FontWeight.w700,
  }) {
    return GoogleFonts.plusJakartaSans(
      fontSize: 14,
      fontWeight: fontWeight,
      height: 20 / 14,
      letterSpacing: 0.02 * 14,
      color: color,
    );
  }

  /// Label Md: 12px, w600/w700, line-height 16px
  static TextStyle labelMd({
    Color color = AppColors.onSurface,
    FontWeight fontWeight = FontWeight.w600,
  }) {
    return GoogleFonts.plusJakartaSans(
      fontSize: 12,
      fontWeight: fontWeight,
      height: 16 / 12,
      letterSpacing: 0.03 * 12,
      color: color,
    );
  }

  /// Label Sm: 10px, w700, line-height 14px
  static TextStyle labelSm({
    Color color = AppColors.onSurfaceVariant,
    FontWeight fontWeight = FontWeight.w700,
  }) {
    return GoogleFonts.plusJakartaSans(
      fontSize: 10,
      fontWeight: fontWeight,
      height: 14 / 10,
      letterSpacing: 0.06 * 10,
      color: color,
    );
  }
}
