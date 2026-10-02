import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_typography.dart';

enum ToastType { success, error, info }

class ToastNotification {
  static void show(
    BuildContext context, {
    required String message,
    ToastType type = ToastType.info,
  }) {
    showVendorToast(
      context,
      message: message,
      isError: type == ToastType.error,
    );
  }

  static void showSuccess(BuildContext context, String message) {
    showVendorToast(context, message: message, isError: false);
  }

  static void showError(BuildContext context, String message) {
    showVendorToast(context, message: message, isError: true);
  }

  static void showInfo(BuildContext context, String message) {
    showVendorToast(context, message: message, isError: false);
  }
}

void showVendorToast(
  BuildContext context, {
  required String message,
  bool isError = false,
  IconData? icon,
}) {
  ScaffoldMessenger.of(context).hideCurrentSnackBar();
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(
      behavior: SnackBarBehavior.floating,
      margin: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      backgroundColor: isError ? AppColors.error : AppColors.inverseSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(9999),
      ),
      content: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            icon ?? (isError ? Icons.error_outline_rounded : Icons.check_circle_rounded),
            color: isError ? AppColors.onError : AppColors.secondaryFixed,
            size: 20,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              message,
              style: AppTypography.bodyMd(color: AppColors.inverseOnSurface),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
      duration: const Duration(seconds: 3),
    ),
  );
}
