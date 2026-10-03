import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';
import 'refund_tracking_screen.dart';

class CancelBookingScreen extends StatefulWidget {
  final SubOrderModel subOrder;

  const CancelBookingScreen({
    super.key,
    required this.subOrder,
  });

  @override
  State<CancelBookingScreen> createState() => _CancelBookingScreenState();
}

class _CancelBookingScreenState extends State<CancelBookingScreen> {
  RefundReason _selectedReason = RefundReason.customerCancel;
  final TextEditingController _noteController = TextEditingController();

  @override
  void dispose() {
    _noteController.dispose();
    super.dispose();
  }

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

  void _submitCancelRequest() {
    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: LocalizedText('Đã gửi yêu cầu hủy dịch vụ thành công! Hệ thống đang xử lý đối soát.'),
        backgroundColor: AppColors.secondary,
      ),
    );
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (_) => const RefundTrackingScreen(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final sub = widget.subOrder;

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
          'Yêu cầu hủy dịch vụ',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // SELECTED SUB-ORDER CARD
            LocalizedText(
              'Dịch vụ con yêu cầu hủy',
              style: AppTypography.labelLg(color: AppColors.onSurface),
            ),
            const SizedBox(height: 8),
            Container(
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
                    children: [
                      Flexible(
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: AppColors.secondaryContainer,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: LocalizedText(
                            sub.vendorName,
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: AppTypography.labelSm(
                              color: AppColors.onSecondaryContainer,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 6),
                      LocalizedText(
                        '#${sub.id}',
                        style: const TextStyle(
                          fontSize: 11,
                          fontFamily: 'monospace',
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  LocalizedText(
                    sub.serviceName,
                    style: AppTypography.headlineSm(color: AppColors.onSurface),
                  ),
                  const SizedBox(height: 4),
                  LocalizedText(
                    '${sub.slotTime} • ${sub.slotDate.day}/${sub.slotDate.month}/${sub.slotDate.year}',
                    style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
                  ),
                  const Divider(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: LocalizedText(
                          'Số lượng vé: ${sub.quantity} vé',
                          style: AppTypography.labelMd(color: AppColors.onSurface),
                        ),
                      ),
                      LocalizedText(
                        _formatPrice(sub.subtotalAmount),
                        style: AppTypography.headlineSm(color: AppColors.primary),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // REASON SELECTION
            LocalizedText(
              'Lý do yêu cầu hủy',
              style: AppTypography.labelLg(color: AppColors.onSurface),
            ),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Column(
                children: [
                  InkWell(
                    onTap: () => setState(() =>
                        _selectedReason = RefundReason.customerCancel),
                    borderRadius: const BorderRadius.vertical(
                        top: Radius.circular(AppShapes.radiusDefaultValue)),
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            width: 20,
                            height: 20,
                            margin: const EdgeInsets.only(top: 2, right: 12),
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: _selectedReason ==
                                        RefundReason.customerCancel
                                    ? AppColors.primary
                                    : AppColors.outline,
                                width: 2,
                              ),
                            ),
                            child: _selectedReason ==
                                    RefundReason.customerCancel
                                ? Center(
                                    child: Container(
                                      width: 10,
                                      height: 10,
                                      decoration: const BoxDecoration(
                                        shape: BoxShape.circle,
                                        color: AppColors.primary,
                                      ),
                                    ),
                                  )
                                : null,
                          ),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                LocalizedText(
                                  'Khách chủ động yêu cầu hủy',
                                  style: AppTypography.labelMd(
                                      color: AppColors.onSurface),
                                ),
                                const SizedBox(height: 4),
                                LocalizedText(
                                  'Khách chủ động hủy theo quy định và điều kiện dịch vụ.',
                                  style: AppTypography.bodySm(
                                          color: AppColors.onSurfaceVariant)
                                      .copyWith(fontSize: 11),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const Divider(height: 1),
                  InkWell(
                    onTap: () =>
                        setState(() => _selectedReason = RefundReason.weather),
                    borderRadius: const BorderRadius.vertical(
                        bottom: Radius.circular(AppShapes.radiusDefaultValue)),
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            width: 20,
                            height: 20,
                            margin: const EdgeInsets.only(top: 2, right: 12),
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              border: Border.all(
                                color: _selectedReason == RefundReason.weather
                                    ? AppColors.secondary
                                    : AppColors.outline,
                                width: 2,
                              ),
                            ),
                            child: _selectedReason == RefundReason.weather
                                ? Center(
                                    child: Container(
                                      width: 10,
                                      height: 10,
                                      decoration: const BoxDecoration(
                                        shape: BoxShape.circle,
                                        color: AppColors.secondary,
                                      ),
                                    ),
                                  )
                                : null,
                          ),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                LocalizedText(
                                  'Thời tiết biển bất lợi / Gió sóng vượt ngưỡng an toàn',
                                  style: AppTypography.labelMd(
                                      color: AppColors.onSurface),
                                ),
                                const SizedBox(height: 4),
                                LocalizedText(
                                  'Thời tiết biển bất lợi hoặc vượt ngưỡng an toàn theo quy định hệ thống.',
                                  style: AppTypography.bodySm(
                                          color: AppColors.onSurfaceVariant)
                                      .copyWith(fontSize: 11),
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
            ),
            const SizedBox(height: 16),

            // EXPLANATION NOTE TEXTFIELD
            LocalizedText(
              'Ghi chú thêm (không bắt buộc)',
              style: AppTypography.labelLg(color: AppColors.onSurface),
            ),
            const SizedBox(height: 6),
            Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: TextField(
                controller: _noteController,
                maxLines: 3,
                decoration: InputDecoration(
                  hintText: tr(context, 'Nhập lý do cụ thể hoặc yêu cầu hỗ trợ đặc biệt...'),
                  hintStyle: AppTypography.bodySm(color: AppColors.outline),
                  contentPadding: const EdgeInsets.all(12),
                  border: InputBorder.none,
                ),
              ),
            ),
            const SizedBox(height: 16),

            // LIVE OCEAN WEATHER MONITOR CARD
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.secondaryFixed.withValues(alpha: 0.35),
                borderRadius: AppShapes.radiusDefault,
              ),
              child: Row(
                children: [
                  const Icon(Icons.sensors,
                      size: 22, color: AppColors.onSecondaryFixedVariant),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        LocalizedText(
                          'Dữ liệu quan trắc thời tiết biển Đà Nẵng',
                          style: AppTypography.labelSm(
                            color: AppColors.onSecondaryFixedVariant,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        LocalizedText(
                          'Cập nhật: 06:00 • Sóng 0.4m • Gió 8 km/h • Mưa 0mm',
                          style: AppTypography.bodySm(
                            color: AppColors.onSurfaceVariant,
                          ).copyWith(fontSize: 11),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // ESTIMATE REFUND POLICY NOTICE
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: AppShapes.radiusDefault,
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.info_outline,
                      size: 18, color: AppColors.secondary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: LocalizedText(
                      'Yêu cầu hoàn tiền sẽ được hệ thống tiếp nhận và xử lý căn cứ theo bảng hoàn tiền (refunds) và quy định dịch vụ.',
                      style: AppTypography.bodySm(
                        color: AppColors.onSurface,
                      ).copyWith(fontSize: 11),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // SUBMIT BUTTON
            AppPillButton(
              label: 'Xác nhận gửi yêu cầu hủy',
              variant: AppButtonVariant.primary,
              width: double.infinity,
              onPressed: _submitCancelRequest,
            ),
          ],
        ),
      ),
    );
  }
}
