import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';
import '../../data/mock_home_data.dart';

class HeroSearchWidget extends StatelessWidget {
  final VoidCallback? onExploreTap;
  final VoidCallback? onAiSuggestTap;
  final VoidCallback? onSearchTap;

  const HeroSearchWidget({
    super.key,
    this.onExploreTap,
    this.onAiSuggestTap,
    this.onSearchTap,
  });

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // ---------------- HERO BANNER ----------------
        Stack(
          children: [
            // Background Image
            AspectRatio(
              aspectRatio: 4 / 4.8,
              child: Image.network(
                MockHomeData.heroImageUrl,
                fit: BoxFit.cover,
                errorBuilder: (context, error, stackTrace) {
                  return Container(
                    color: AppColors.secondary.withValues(alpha: 0.8),
                    child: const Center(
                      child: Icon(Icons.beach_access, size: 64, color: Colors.white),
                    ),
                  );
                },
              ),
            ),
            // Gradient Scrim
            Positioned.fill(
              child: Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      AppColors.inverseSurface.withValues(alpha: 0.35),
                      AppColors.inverseSurface.withValues(alpha: 0.85),
                      AppColors.inverseSurface.withValues(alpha: 0.98),
                    ],
                    stops: const [0.0, 0.45, 0.75, 1.0],
                  ),
                ),
              ),
            ),
            // Hero Content Overlay
            Positioned(
              left: AppShapes.gutterMobile,
              right: AppShapes.gutterMobile,
              bottom: 40,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Pill Tag
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppColors.secondary,
                      borderRadius: AppShapes.radiusFull,
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.2),
                          blurRadius: 6,
                          offset: const Offset(0, 2),
                        ),
                      ],
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.waves, size: 14, color: AppColors.onSecondary),
                        const SizedBox(width: 6),
                        Flexible(
                          child: Text(
                            'TRẢI NGHIỆM BIỂN ĐÀ NẴNG',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AppTypography.labelSm(
                              color: AppColors.onSecondary,
                              fontWeight: FontWeight.w700,
                            ).copyWith(
                              letterSpacing: 0.4,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  // Headline
                  Text(
                    'Chạm sóng biển,\nmở chuyến đi riêng.',
                    style: AppTypography.headlineLgMobile(
                      color: AppColors.inverseOnSurface,
                    ).copyWith(
                      fontWeight: FontWeight.w800,
                      height: 1.15,
                    ),
                  ),
                  const SizedBox(height: 8),
                  // Subtitle
                  Text(
                    'Khám phá những trải nghiệm biển phù hợp với bạn. Chọn hoạt động, đặt lịch và sẵn sàng tận hưởng Đà Nẵng.',
                    style: AppTypography.bodyMd(
                      color: AppColors.surfaceVariant,
                    ).copyWith(
                      height: 1.4,
                    ),
                  ),
                  const SizedBox(height: 20),
                  // CTA 1
                  AppPillButton(
                    label: 'Khám phá trải nghiệm',
                    variant: AppButtonVariant.primary,
                    trailingIcon: Icons.arrow_forward,
                    width: double.infinity,
                    onPressed: onExploreTap,
                  ),
                  const SizedBox(height: 10),
                  // CTA 2
                  AppPillButton(
                    label: 'Gợi ý lịch trình cho tôi',
                    variant: AppButtonVariant.frosted,
                    leadingIcon: Icons.auto_awesome,
                    width: double.infinity,
                    onPressed: onAiSuggestTap,
                  ),
                ],
              ),
            ),
          ],
        ),

        // ---------------- FLOATING SEARCH CARD ----------------
        Transform.translate(
          offset: const Offset(0, -26),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            child: Container(
              padding: const EdgeInsets.all(AppShapes.spaceMd),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusLg,
                boxShadow: AppShapes.shadowLevel2,
                border: Border.all(
                  color: AppColors.borderOcean.withValues(alpha: 0.5),
                  width: 0.8,
                ),
              ),
              child: Column(
                children: [
                  // Row 1: Destination
                  Container(
                    padding: const EdgeInsets.all(AppShapes.spaceXs),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: AppShapes.radiusDefault,
                    ),
                    child: Row(
                      children: [
                        Container(
                          width: 36,
                          height: 36,
                          decoration: const BoxDecoration(
                            color: AppColors.secondaryContainer,
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(
                            Icons.location_on,
                            color: AppColors.onSecondaryContainer,
                            size: 20,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Khu vực biển',
                                style: AppTypography.labelSm(
                                  color: AppColors.onSurfaceVariant,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                              Text(
                                'Mỹ Khê, Sơn Trà, Non Nước',
                                style: AppTypography.labelLg(
                                  color: AppColors.onSurface,
                                  fontWeight: FontWeight.w700,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        const Icon(
                          Icons.expand_more,
                          color: AppColors.outline,
                          size: 20,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 10),

                  // Row 2: Date & Guests
                  Row(
                    children: [
                      // Date
                      Expanded(
                        child: Container(
                          padding: const EdgeInsets.all(AppShapes.spaceXs),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLow,
                            borderRadius: AppShapes.radiusDefault,
                          ),
                          child: Row(
                            children: [
                              Container(
                                width: 32,
                                height: 32,
                                decoration: const BoxDecoration(
                                  color: AppColors.secondaryContainer,
                                  shape: BoxShape.circle,
                                ),
                                child: const Icon(
                                  Icons.calendar_today,
                                  color: AppColors.onSecondaryContainer,
                                  size: 16,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Thời gian',
                                      style: AppTypography.labelSm(
                                        color: AppColors.onSurfaceVariant,
                                      ),
                                    ),
                                    Text(
                                      'Ngày mai',
                                      style: AppTypography.labelMd(
                                        color: AppColors.onSurface,
                                        fontWeight: FontWeight.w700,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      // Guests
                      Expanded(
                        child: Container(
                          padding: const EdgeInsets.all(AppShapes.spaceXs),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLow,
                            borderRadius: AppShapes.radiusDefault,
                          ),
                          child: Row(
                            children: [
                              Container(
                                width: 32,
                                height: 32,
                                decoration: const BoxDecoration(
                                  color: AppColors.secondaryContainer,
                                  shape: BoxShape.circle,
                                ),
                                child: const Icon(
                                  Icons.group,
                                  color: AppColors.onSecondaryContainer,
                                  size: 18,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Số lượng',
                                      style: AppTypography.labelSm(
                                        color: AppColors.onSurfaceVariant,
                                      ),
                                    ),
                                    Text(
                                      '2 khách',
                                      style: AppTypography.labelMd(
                                        color: AppColors.onSurface,
                                        fontWeight: FontWeight.w700,
                                      ),
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),

                  // Submit Search Button
                  AppPillButton(
                    label: 'Tìm trải nghiệm',
                    variant: AppButtonVariant.primary,
                    leadingIcon: Icons.search,
                    width: double.infinity,
                    onPressed: onSearchTap,
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}
