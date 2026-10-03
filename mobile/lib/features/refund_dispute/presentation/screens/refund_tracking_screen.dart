import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';

class RefundTrackingScreen extends StatelessWidget {
  const RefundTrackingScreen({super.key});

  String _formatPrice(int price) {
    final str = price.toString();
    final buffer = StringBuffer();
    int count = 0;
    for (int i = str.length - 1; i >= 0; i--) {
      buffer.write(str[i]);
      count++;
      if (count % 3 == 0 && i > 0) {
        buffer.write('.');
      }
    }
    return '${buffer.toString().split('').reversed.join()} đ';
  }

  String _getReasonText(RefundReason reason) {
    switch (reason) {
      case RefundReason.customerCancel:
        return 'Khách yêu cầu hủy';
      case RefundReason.weather:
        return 'Thời tiết biển bất lợi';
      case RefundReason.dispute:
        return 'Bồi thường khiếu nại';
      case RefundReason.compensation:
        return 'Bù trừ tách đơn';
    }
  }

  @override
  Widget build(BuildContext context) {
    final refunds = MockDatabaseData.refunds;
    final totalAmount =
        refunds.fold(0, (sum, item) => sum + item.amount);

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        title: LocalizedText(
          'Theo dõi hoàn tiền',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          children: [
            // METRIC CARDS ROW
            Row(
              children: [
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.all(AppShapes.spaceSm),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLowest,
                      borderRadius: AppShapes.radiusDefault,
                      boxShadow: AppShapes.shadowLevel1,
                      border: Border.all(color: AppColors.borderSubtle),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        LocalizedText(
                          'Tổng yêu cầu',
                          style: AppTypography.bodySm(
                            color: AppColors.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 4),
                        LocalizedText(
                          '${refunds.length} dịch vụ',
                          style: AppTypography.headlineSm(
                            color: AppColors.onSurface,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.all(AppShapes.spaceSm),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLowest,
                      borderRadius: AppShapes.radiusDefault,
                      boxShadow: AppShapes.shadowLevel1,
                      border: Border.all(color: AppColors.borderSubtle),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        LocalizedText(
                          'Tổng tiền yêu cầu',
                          style: AppTypography.bodySm(
                            color: AppColors.onSurfaceVariant,
                          ),
                        ),
                        const SizedBox(height: 4),
                        LocalizedText(
                          _formatPrice(totalAmount),
                          style: AppTypography.headlineSm(
                            color: AppColors.primary,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),

            // TRANSPARENCY NOTICE
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.secondaryFixed.withValues(alpha: 0.35),
                borderRadius: AppShapes.radiusDefault,
              ),
              child: Row(
                children: [
                  const Icon(Icons.shield_outlined,
                      size: 20, color: AppColors.secondary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: LocalizedText(
                      'Hoàn trả minh bạch kết nối trực tiếp cổng thanh toán gốc, theo đúng lịch đối soát của ngân hàng.',
                      style: AppTypography.bodySm(
                        color: AppColors.onSecondaryFixedVariant,
                      ).copyWith(fontSize: 11),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // REFUND CARDS LIST
            ...refunds.map((ref) => _buildRefundCard(context, ref)),
          ],
        ),
      ),
    );
  }

  Widget _buildRefundCard(BuildContext context, RefundModel ref) {
    final isProcessed = ref.status == RefundStatus.processed;

    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.all(AppShapes.spaceSm),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusDefault,
        boxShadow: AppShapes.shadowLevel1,
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    Flexible(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 2),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerHigh,
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: LocalizedText(
                          ref.subOrderCode,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                              fontSize: 10, fontFamily: 'monospace'),
                        ),
                      ),
                    ),
                    const SizedBox(width: 4),
                    Flexible(
                      child: LocalizedText(
                        '${ref.createdAt.day}/${ref.createdAt.month}/${ref.createdAt.year}',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.bodySm(color: AppColors.outline)
                            .copyWith(fontSize: 10),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 6),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: isProcessed
                      ? AppColors.secondaryContainer
                      : AppColors.surfaceContainerHigh,
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      isProcessed ? Icons.check_circle : Icons.sync,
                      size: 12,
                      color: isProcessed
                          ? AppColors.onSecondaryContainer
                          : AppColors.onSurfaceVariant,
                    ),
                    const SizedBox(width: 4),
                    LocalizedText(
                      isProcessed ? 'Đã hoàn tất' : 'Đang xử lý',
                      style: AppTypography.labelSm(
                        color: isProcessed
                            ? AppColors.onSecondaryContainer
                            : AppColors.onSurfaceVariant,
                        fontWeight: FontWeight.w700,
                      ).copyWith(fontSize: 10),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          LocalizedText(
            ref.serviceName,
            style: AppTypography.headlineSm(
              color: AppColors.onSurface,
            ),
          ),
          const SizedBox(height: 10),

          // Financial detail strip
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: BorderRadius.circular(8),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      LocalizedText('Số tiền hoàn:',
                          style: AppTypography.bodySm(
                              color: AppColors.onSurfaceVariant)),
                      LocalizedText(
                        _formatPrice(ref.amount),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.headlineSm(
                          color: AppColors.secondary,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainerLowest,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: LocalizedText(
                    'Tỷ lệ: ${ref.refundPercentage.toInt()}%',
                    style: AppTypography.labelSm(
                      color: AppColors.secondary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),

          _buildRow('Lý do hoàn:', _getReasonText(ref.reason)),
          const SizedBox(height: 4),
          _buildRow('Phương thức nhận:', ref.originalPaymentMethod),
          const Divider(height: 16),

          OutlinedButton.icon(
            style: OutlinedButton.styleFrom(
              side: const BorderSide(color: AppColors.borderSubtle),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(18),
              ),
              minimumSize: const Size(double.infinity, 38),
            ),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: LocalizedText('Xem chi tiết đơn ${ref.subOrderCode}'),
                  duration: const Duration(seconds: 1),
                ),
              );
            },
            icon: const Icon(Icons.receipt_long,
                size: 16, color: AppColors.secondary),
            label: LocalizedText(
              'Xem mã đơn gốc & hóa đơn',
              style: AppTypography.labelSm(color: AppColors.secondary),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        LocalizedText(label,
            style: AppTypography.bodySm(color: AppColors.onSurfaceVariant)),
        const SizedBox(width: 12),
        Expanded(
          child: LocalizedText(
            value,
            textAlign: TextAlign.right,
            style: AppTypography.labelMd(
                color: AppColors.onSurface, fontWeight: FontWeight.w600),
          ),
        ),
      ],
    );
  }
}
