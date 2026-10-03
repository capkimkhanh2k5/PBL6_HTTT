import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_shapes.dart';
import '../theme/app_typography.dart';

enum VendorButtonVariant {
  primary,
  secondary,
  coral,
  surface,
  outline,
}

class VendorButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final IconData? icon;
  final bool isLoading;
  final VendorButtonVariant variant;
  final double? height;
  final double? width;

  const VendorButton({
    super.key,
    String? label,
    String? text,
    this.onPressed,
    this.icon,
    this.isLoading = false,
    this.variant = VendorButtonVariant.secondary,
    this.height = 48,
    this.width,
  }) : label = label ?? text ?? '';

  @override
  Widget build(BuildContext context) {
    Color bg;
    Color fg;
    BorderSide borderSide = BorderSide.none;

    switch (variant) {
      case VendorButtonVariant.primary:
      case VendorButtonVariant.secondary:
        bg = AppColors.secondary;
        fg = AppColors.onSecondary;
        break;
      case VendorButtonVariant.coral:
        bg = AppColors.primaryContainer;
        fg = AppColors.onPrimaryContainer;
        break;
      case VendorButtonVariant.surface:
        bg = AppColors.surfaceContainerHigh;
        fg = AppColors.onSurface;
        break;
      case VendorButtonVariant.outline:
        bg = Colors.transparent;
        fg = AppColors.secondary;
        borderSide = const BorderSide(color: AppColors.secondary, width: 1.5);
        break;
    }

    final isEnabled = onPressed != null && !isLoading;

    return SizedBox(
      height: height,
      width: width ?? double.infinity,
      child: ElevatedButton(
        onPressed: isEnabled ? onPressed : null,
        style: ElevatedButton.styleFrom(
          backgroundColor: bg,
          foregroundColor: fg,
          disabledBackgroundColor: bg.withAlpha(100),
          disabledForegroundColor: fg.withAlpha(150),
          elevation: 0,
          side: borderSide,
          shape: RoundedRectangleBorder(
            borderRadius: AppShapes.radiusFull,
          ),
          padding: const EdgeInsets.symmetric(horizontal: 20),
        ),
        child: isLoading
            ? SizedBox(
                width: 20,
                height: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  valueColor: AlwaysStoppedAnimation<Color>(fg),
                ),
              )
            : Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  if (icon != null) ...[
                    Icon(icon, size: 18),
                    const SizedBox(width: 8),
                  ],
                  Flexible(
                    child: LocalizedText(
                      label,
                      style: AppTypography.labelLg(color: fg),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
      ),
    );
  }
}
