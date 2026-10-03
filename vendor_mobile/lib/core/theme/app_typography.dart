import 'package:flutter/material.dart';
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
/// Bundled Noto Sans for consistent Vietnamese, English and numeric text.
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

  // ---------------- DISPLAY & HEADLINES (NOTO SANS) ----------------
  static CallableTextStyle get displayHero => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 56,
        fontWeight: FontWeight.w800,
        height: 64 / 56,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get displayHeroMobile => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 36,
        fontWeight: FontWeight.w800,
        height: 42 / 36,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get headlineLg => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 32,
        fontWeight: FontWeight.w700,
        height: 40 / 32,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get headlineLgMobile => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 26,
        fontWeight: FontWeight.w700,
        height: 32 / 26,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get headlineMd => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 22,
        fontWeight: FontWeight.w700,
        height: 28 / 22,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get headlineSm => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 18,
        fontWeight: FontWeight.w600,
        height: 24 / 18,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  // ---------------- BODY & LABELS (NOTO SANS) ----------------
  static CallableTextStyle get bodyLg => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 17,
        fontWeight: FontWeight.w400,
        height: 26 / 17,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get bodyMd => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 14,
        fontWeight: FontWeight.w400,
        height: 22 / 14,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get bodySm => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 12,
        fontWeight: FontWeight.w400,
        height: 18 / 12,
        letterSpacing: 0,
        color: AppColors.onSurfaceVariant,
      ));

  static CallableTextStyle get labelLg => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 14,
        fontWeight: FontWeight.w700,
        height: 20 / 14,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get labelMd => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 12,
        fontWeight: FontWeight.w600,
        height: 16 / 12,
        letterSpacing: 0,
        color: AppColors.onSurface,
      ));

  static CallableTextStyle get labelSm => _wrap(TextStyle(
      fontFamily: 'NotoSans',
        fontSize: 10,
        fontWeight: FontWeight.w700,
        height: 14 / 10,
        letterSpacing: 0,
        color: AppColors.onSurfaceVariant,
      ));
}
