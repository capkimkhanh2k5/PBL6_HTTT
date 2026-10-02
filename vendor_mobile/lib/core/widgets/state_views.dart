import 'package:flutter/material.dart';
import '../theme/app_colors.dart';
import '../theme/app_typography.dart';

class VendorLoadingView extends StatelessWidget {
  final String? message;
  const VendorLoadingView({super.key, this.message});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const CircularProgressIndicator(
              valueColor: AlwaysStoppedAnimation<Color>(AppColors.secondary),
              strokeWidth: 2.5,
            ),
            if (message != null) ...[
              const SizedBox(height: 16),
              Text(
                message!,
                style: AppTypography.bodyMd(color: AppColors.tertiary),
                textAlign: TextAlign.center,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class VendorEmptyView extends StatelessWidget {
  final IconData icon;
  final String title;
  final String message;
  final String? actionLabel;
  final VoidCallback? onAction;

  const VendorEmptyView({
    super.key,
    this.icon = Icons.receipt_long_rounded,
    required this.title,
    required this.message,
    this.actionLabel,
    this.onAction,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: BoxDecoration(
                color: AppColors.secondaryContainer.withAlpha(80),
                shape: BoxShape.circle,
              ),
              child: Icon(
                icon,
                size: 36,
                color: AppColors.secondary,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              title,
              style: AppTypography.headlineSm(color: AppColors.onSurface),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              message,
              style: AppTypography.bodyMd(color: AppColors.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
            if (actionLabel != null && onAction != null) ...[
              const SizedBox(height: 20),
              ElevatedButton(
                onPressed: onAction,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.secondary,
                  foregroundColor: AppColors.onSecondary,
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(9999),
                  ),
                ),
                child: Text(actionLabel!),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class VendorErrorBanner extends StatelessWidget {
  final String message;
  final VoidCallback? onRetry;

  const VendorErrorBanner({
    super.key,
    required this.message,
    this.onRetry,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(vertical: 8),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.errorContainer,
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.error_outline_rounded,
            color: AppColors.error,
            size: 22,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Đã xảy ra lỗi',
                  style: AppTypography.labelMd(color: AppColors.onErrorContainer),
                ),
                const SizedBox(height: 2),
                Text(
                  message,
                  style: AppTypography.bodySm(color: AppColors.onErrorContainer),
                ),
                if (onRetry != null) ...[
                  const SizedBox(height: 8),
                  InkWell(
                    onTap: onRetry,
                    child: Text(
                      'Thử lại',
                      style: AppTypography.labelMd(color: AppColors.error).copyWith(
                        decoration: TextDecoration.underline,
                      ),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class VendorNoticeCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String message;
  final Color? iconColor;

  const VendorNoticeCard({
    super.key,
    this.icon = Icons.info_outline_rounded,
    required this.title,
    required this.message,
    this.iconColor,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderOcean.withAlpha(100)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            icon,
            color: iconColor ?? AppColors.secondary,
            size: 20,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: AppTypography.labelMd(color: AppColors.onSurface),
                ),
                const SizedBox(height: 2),
                Text(
                  message,
                  style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
