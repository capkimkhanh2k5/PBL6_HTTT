import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';

class HomeFooter extends StatelessWidget {
  const HomeFooter({super.key});

  @override
  Widget build(BuildContext context) {
    final links = [
      'Khám phá',
      'Gói trải nghiệm',
      'Lên lịch cùng AI',
      'Chính sách đặt/hủy',
      'Điều khoản dịch vụ',
      'Hỗ trợ khách hàng',
    ];

    return Container(
      width: double.infinity,
      decoration: const BoxDecoration(
        color: AppColors.surfaceContainerLow,
        borderRadius: BorderRadius.only(
          topLeft: Radius.circular(AppShapes.radiusLgValue),
          topRight: Radius.circular(AppShapes.radiusLgValue),
        ),
      ),
      padding: const EdgeInsets.symmetric(
        horizontal: AppShapes.gutterMobile,
        vertical: AppShapes.spaceXl,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Brand Logo + Title
          Row(
            children: [
              Container(
                width: 28,
                height: 28,
                decoration: BoxDecoration(
                  color: AppColors.secondary.withValues(alpha: 0.12),
                  shape: BoxShape.circle,
                ),
                child: const Icon(
                  Icons.sailing,
                  color: AppColors.secondary,
                  size: 16,
                ),
              ),
              const SizedBox(width: 8),
              Text(
                'DANASEA',
                style: AppTypography.headlineSm(
                  color: AppColors.secondary,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            'Chạm sóng biển, mở chuyến đi riêng',
            style: AppTypography.headlineSm(
              color: AppColors.onSurface,
              fontWeight: FontWeight.w700,
            ).copyWith(fontSize: 18),
          ),
          const SizedBox(height: 16),

          // 2-Column Links
          LayoutBuilder(
            builder: (context, constraints) {
              final itemWidth = ((constraints.maxWidth - 24) / 2).clamp(100.0, double.infinity);
              return Wrap(
                spacing: 24,
                runSpacing: 10,
                children: links.map((link) {
                  return SizedBox(
                    width: itemWidth,
                    child: Text(
                      link,
                      style: AppTypography.labelLg(
                        color: AppColors.onSurfaceVariant,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  );
                }).toList(),
              );
            },
          ),
          const SizedBox(height: 20),

          // Divider
          Divider(
            color: AppColors.borderSubtle.withValues(alpha: 0.8),
            height: 1,
          ),
          const SizedBox(height: 14),

          // Support Info & Language
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    const Icon(
                      Icons.contact_support_outlined,
                      size: 18,
                      color: AppColors.secondary,
                    ),
                    const SizedBox(width: 4),
                    Flexible(
                      child: Text(
                        'Hỗ trợ demo: Da Nang, VN',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.bodySm(
                          color: AppColors.secondary,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Tiếng Việt',
                    style: AppTypography.labelMd(
                      color: AppColors.secondary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const Icon(
                    Icons.expand_more,
                    size: 16,
                    color: AppColors.outline,
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 8),

          // Copyright
          Text(
            '© 2025 DANASEA Vietnam. All rights reserved.',
            style: AppTypography.labelSm(
              color: AppColors.outline,
            ),
          ),
        ],
      ),
    );
  }
}
