import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';

class HowItWorksSection extends StatelessWidget {
  const HowItWorksSection({super.key});

  @override
  Widget build(BuildContext context) {
    final steps = [
      {
        'num': '01',
        'title': 'Chọn trải nghiệm & giờ',
        'desc':
            'Lựa chọn môn thể thao nước bạn yêu thích, xem tình trạng sóng biển và chọn khung giờ hoàng kim.',
      },
      {
        'num': '02',
        'title': 'Xác nhận & thanh toán',
        'desc':
            'Chọn số lượng người tham gia, áp dụng ưu đãi và thanh toán trực tuyến bảo mật đa kênh tiện lợi.',
      },
      {
        'num': '03',
        'title': 'Nhận vé QR & ra biển',
        'desc':
            'Vé điện tử lưu ngay trên điện thoại. Xuất trình tại trạm phục vụ bãi biển để bắt đầu đón sóng!',
      },
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Title
        LocalizedText(
          'Đặt trải nghiệm dễ dàng',
          style: AppTypography.headlineMd(
            color: AppColors.onSurface,
          ),
        ),
        const SizedBox(height: 2),
        LocalizedText(
          'Chỉ với 3 bước chạm cho chuyến phiêu lưu hoàn hảo',
          style: AppTypography.bodySm(
            color: AppColors.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 14),

        // 3 Numbered Step Cards
        ...steps.map((step) {
          return Padding(
            padding: const EdgeInsets.only(bottom: 12),
            child: Container(
              padding: const EdgeInsets.all(AppShapes.spaceMd),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: AppShapes.radiusLg,
                border: Border.all(
                  color: AppColors.borderSubtle.withValues(alpha: 0.5),
                  width: 0.6,
                ),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  LocalizedText(
                    step['num']!,
                    style: AppTypography.displayHeroMobile(
                      color: AppColors.secondary.withValues(alpha: 0.28),
                    ).copyWith(
                      fontWeight: FontWeight.w900,
                      height: 1.0,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        LocalizedText(
                          step['title']!,
                          style: AppTypography.headlineSm(
                            color: AppColors.onSurface,
                            fontWeight: FontWeight.w700,
                          ).copyWith(fontSize: 17),
                        ),
                        const SizedBox(height: 4),
                        LocalizedText(
                          step['desc']!,
                          style: AppTypography.bodySm(
                            color: AppColors.onSurfaceVariant,
                          ).copyWith(height: 1.4),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          );
        }),
      ],
    );
  }
}
