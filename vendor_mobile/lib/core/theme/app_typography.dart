import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'app_colors.dart';

/// Extension of TextStyle that is callable as a function (e.g. `AppTypography.bodyMd(color: ...)`)
/// while also functioning directly as a standard `TextStyle` with `.copyWith()` and direct usage.
class CallableTextStyle extends TextStyle {
  const CallableTextStyle({
    super.inherit,
    super.color,
    super.backgroundColor,
    super.fontSize,
    super.fontWeight,
    super.fontStyle,
    super.letterSpacing,
    super.wordSpacing,
    super.textBaseline,
    super.height,
    super.leadingDistribution,
    super.locale,
    super.foreground,
    super.background,
    super.shadows,
    super.fontFeatures,
    super.fontVariations,
    super.decoration,
    super.decorationColor,
    super.decorationStyle,
    super.decorationThickness,
    super.debugLabel,
    super.fontFamily,
    super.fontFamilyFallback,
    super.package,
    super.overflow,
  });

  CallableTextStyle call({Color? color}) {
    if (color != null) {
      return CallableTextStyle(
        inherit: inherit,
        color: color,
        backgroundColor: backgroundColor,
        fontSize: fontSize,
        fontWeight: fontWeight,
        fontStyle: fontStyle,
        letterSpacing: letterSpacing,
        wordSpacing: wordSpacing,
        textBaseline: textBaseline,
        height: height,
        leadingDistribution: leadingDistribution,
        locale: locale,
        foreground: foreground,
        background: background,
        shadows: shadows,
        fontFeatures: fontFeatures,
        fontVariations: fontVariations,
        decoration: decoration,
        decorationColor: decorationColor,
        decorationStyle: decorationStyle,
        decorationThickness: decorationThickness,
        debugLabel: debugLabel,
        fontFamily: fontFamily,
        fontFamilyFallback: fontFamilyFallback,
        overflow: overflow,
      );
    }
    return this;
  }
}

/// Coastal Avant-Garde Typography System for DANASEA Vendor
/// Pairs kinetic 'Syne' for Display/Headlines with clear 'Plus Jakarta Sans' for Body/Labels.
class AppTypography {
  AppTypography._();

  static CallableTextStyle _wrap(TextStyle style) {
    return CallableTextStyle(
      inherit: style.inherit,
      color: style.color,
      backgroundColor: style.backgroundColor,
      fontSize: style.fontSize,
      fontWeight: style.fontWeight,
      fontStyle: style.fontStyle,
      letterSpacing: style.letterSpacing,
      wordSpacing: style.wordSpacing,
      textBaseline: style.textBaseline,
      height: style.height,
      leadingDistribution: style.leadingDistribution,
      locale: style.locale,
      foreground: style.foreground,
      background: style.background,
      shadows: style.shadows,
      fontFeatures: style.fontFeatures,
      fontVariations: style.fontVariations,
      decoration: style.decoration,
      decorationColor: style.decorationColor,
      decorationStyle: style.decorationStyle,
      decorationThickness: style.decorationThickness,
      debugLabel: style.debugLabel,
      fontFamily: style.fontFamily,
      fontFamilyFallback: style.fontFamilyFallback,
      overflow: style.overflow,
    );
  }

  // ---------------- DISPLAY & HEADLINES (SYNE) ----------------
  static CallableTextStyle get displayHero => _wrap(GoogleFonts.syne(
        fontSize: 56,
        fontWeight: FontWeight.w800,
        height: 64 / 56,
        letterSpacing: -0.03 * 56,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get displayHeroMobile => _wrap(GoogleFonts.syne(
        fontSize: 36,
        fontWeight: FontWeight.w800,
        height: 42 / 36,
        letterSpacing: -0.02 * 36,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get headlineLg => _wrap(GoogleFonts.syne(
        fontSize: 32,
        fontWeight: FontWeight.w700,
        height: 40 / 32,
        letterSpacing: -0.02 * 32,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get headlineLgMobile => _wrap(GoogleFonts.syne(
        fontSize: 26,
        fontWeight: FontWeight.w700,
        height: 32 / 26,
        letterSpacing: -0.01 * 26,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get headlineMd => _wrap(GoogleFonts.syne(
        fontSize: 22,
        fontWeight: FontWeight.w700,
        height: 28 / 22,
        letterSpacing: -0.01 * 22,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get headlineSm => _wrap(GoogleFonts.syne(
        fontSize: 18,
        fontWeight: FontWeight.w600,
        height: 24 / 18,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  // ---------------- BODY & LABELS (PLUS JAKARTA SANS) ----------------
  static CallableTextStyle get bodyLg => _wrap(GoogleFonts.plusJakartaSans(
        fontSize: 17,
        fontWeight: FontWeight.w400,
        height: 26 / 17,
        letterSpacing: -0.01 * 17,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get bodyMd => _wrap(GoogleFonts.plusJakartaSans(
        fontSize: 14,
        fontWeight: FontWeight.w400,
        height: 22 / 14,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get bodySm => _wrap(GoogleFonts.plusJakartaSans(
        fontSize: 12,
        fontWeight: FontWeight.w400,
        height: 18 / 12,
        letterSpacing: 0.01 * 12,
        color: AppColors.onSurfaceVariant,
      ));

  static CallableTextStyle get labelLg => _wrap(GoogleFonts.plusJakartaSans(
        fontSize: 14,
        fontWeight: FontWeight.w700,
        height: 20 / 14,
        letterSpacing: 0.02 * 14,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get labelMd => _wrap(GoogleFonts.plusJakartaSans(
        fontSize: 12,
        fontWeight: FontWeight.w600,
        height: 16 / 12,
        letterSpacing: 0.03 * 12,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get labelSm => _wrap(GoogleFonts.plusJakartaSans(
        fontSize: 10,
        fontWeight: FontWeight.w700,
        height: 14 / 10,
        letterSpacing: 0.06 * 10,
        color: AppColors.onSurfaceVariant,
      ));
}
