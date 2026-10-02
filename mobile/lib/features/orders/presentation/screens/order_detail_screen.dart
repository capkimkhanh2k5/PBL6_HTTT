import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../checkout/presentation/screens/e_ticket_qr_screen.dart';
import '../../../refund_dispute/presentation/screens/cancel_booking_screen.dart';
import '../../../refund_dispute/presentation/screens/create_dispute_screen.dart';
import '../../../reviews/presentation/screens/review_experience_screen.dart';

class OrderDetailScreen extends StatelessWidget {
  final MasterOrderModel masterOrder;

  const OrderDetailScreen({
    super.key,
    required this.masterOrder,
  });

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

  @override
  Widget build(BuildContext context) {
    final user = MockDatabaseData.currentUser;

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        title: Column(
          children: [
            Text(
              'Chi tiết đơn #${masterOrder.id}',
              style: AppTypography.headlineSm(color: AppColors.onSurface),
            ),
            Text(
              'Đặt chỗ trực tuyến',
              style: AppTypography.labelSm(
                color: AppColors.secondary,
              ).copyWith(fontSize: 10),
            ),
          ],
        ),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.share, color: AppColors.onSurface),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: Text('Đã sao chép mã đơn hàng!')),
              );
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // OVERALL STATUS CARD
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
                      Container(
                        width: 36,
                        height: 36,
                        decoration: const BoxDecoration(
                          color: AppColors.secondaryContainer,
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(
                          Icons.check_circle,
                          color: AppColors.secondary,
                          size: 22,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Đã thanh toán thành công',
                              style: AppTypography.headlineSm(
                                color: AppColors.secondary,
                              ),
                            ),
                            Text(
                              '${masterOrder.createdAt.hour.toString().padLeft(2, '0')}:${masterOrder.createdAt.minute.toString().padLeft(2, '0')} • ${masterOrder.createdAt.day}/${masterOrder.createdAt.month}/${masterOrder.createdAt.year}',
                              style: AppTypography.bodySm(
                                color: AppColors.onSurfaceVariant,
                              ).copyWith(fontSize: 11),
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerHigh,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          '${masterOrder.subOrders.length} dịch vụ',
                          style: AppTypography.labelSm(
                            color: AppColors.onSurfaceVariant,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  // Weather tip pill
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.waves,
                            size: 14, color: AppColors.secondary),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            'Biển Mỹ Khê & Bãi Bụt sáng êm, gió nhẹ 8km/h, sóng 0.4m trong lành.',
                            style: AppTypography.bodySm(
                              color: AppColors.onSecondaryFixedVariant,
                            ).copyWith(fontSize: 11),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // SUB-ORDERS JOURNEY LIST
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Text(
                    'Hành trình trải nghiệm',
                    overflow: TextOverflow.ellipsis,
                    style: AppTypography.headlineSm(
                      color: AppColors.onSurface,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                Text(
                  'Độc lập từng chặng',
                  style: AppTypography.labelSm(
                    color: AppColors.secondary,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),

            ...masterOrder.subOrders.map((sub) => _buildSubOrderDetailCard(context, sub)),

            const SizedBox(height: 16),

            // FINANCIAL SUMMARY CARD
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
                boxShadow: AppShapes.shadowLevel1,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Hóa đơn & Thanh toán',
                    style: AppTypography.headlineSm(color: AppColors.onSurface),
                  ),
                  const SizedBox(height: 12),
                  _buildPriceRow(
                    'Tổng giá dịch vụ:',
                    _formatPrice(masterOrder.subtotalAmount),
                  ),
                  if (masterOrder.discountAmount > 0) ...[
                    const SizedBox(height: 6),
                    _buildPriceRow(
                      'Mã giảm giá (${masterOrder.discountCode ?? 'DISCOUNT'}):',
                      '- ${_formatPrice(masterOrder.discountAmount)}',
                      valueColor: AppColors.secondary,
                    ),
                  ],
                  const Divider(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          'Tổng thực thu:',
                          style: AppTypography.labelLg(
                            color: AppColors.onSurface,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        _formatPrice(masterOrder.totalAmount),
                        style: AppTypography.headlineSm(
                          color: AppColors.primary,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // CUSTOMER INFO
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Thông tin liên lạc',
                    style: AppTypography.labelLg(
                      color: AppColors.onSurface,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text('${user.fullName} • ${user.phone ?? '0905 123 456'}',
                      style: AppTypography.bodySm(color: AppColors.onSurface)),
                  Text(user.email,
                      style: AppTypography.bodySm(color: AppColors.outline)),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // INVOICE DOWNLOAD BUTTON
            OutlinedButton.icon(
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: AppColors.secondary),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(20),
                ),
                minimumSize: const Size(double.infinity, 44),
              ),
              icon: const Icon(Icons.receipt_long, color: AppColors.secondary),
              label: Text(
                'Tải hóa đơn điện tử VAT (PDF)',
                style: AppTypography.labelMd(color: AppColors.secondary),
              ),
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Đang tải hóa đơn điện tử PDF...'),
                    backgroundColor: AppColors.secondary,
                  ),
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSubOrderDetailCard(BuildContext context, SubOrderModel sub) {
    final isConfirmed = sub.status == SubOrderStatus.confirmed ||
        sub.status == SubOrderStatus.completed;
    final isCompleted = sub.status == SubOrderStatus.completed;

    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.all(AppShapes.spaceSm),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusDefault,
        border: Border.all(color: AppColors.borderSubtle),
        boxShadow: AppShapes.shadowLevel1,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Sub-order header
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Flexible(
                      child: Text(
                        sub.vendorName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.labelSm(
                          color: AppColors.secondary,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    const SizedBox(width: 4),
                    Flexible(
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 5, vertical: 1),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerHigh,
                          borderRadius: BorderRadius.circular(6),
                        ),
                        child: Text(
                          '#${sub.id}',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                              fontSize: 10, fontFamily: 'monospace'),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 6),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: isConfirmed
                      ? AppColors.secondaryContainer
                      : AppColors.surfaceContainerHigh,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Text(
                  isConfirmed ? 'Đã xác nhận' : 'Đang xử lý',
                  style: AppTypography.labelSm(
                    color: isConfirmed
                        ? AppColors.onSecondaryContainer
                        : AppColors.onSurfaceVariant,
                    fontWeight: FontWeight.w700,
                  ).copyWith(fontSize: 10),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            sub.serviceName,
            style: AppTypography.headlineSm(
              color: AppColors.onSurface,
            ),
          ),
          const SizedBox(height: 4),
          Row(
            children: [
              const Icon(Icons.schedule, size: 14, color: AppColors.tertiary),
              const SizedBox(width: 4),
              Expanded(
                child: Text(
                  '${sub.slotTime} • ${sub.slotDate.day}/${sub.slotDate.month}/${sub.slotDate.year}',
                  overflow: TextOverflow.ellipsis,
                  style: AppTypography.bodySm(
                    color: AppColors.onSurfaceVariant,
                  ).copyWith(fontSize: 11),
                ),
              ),
              const SizedBox(width: 8),
              Text(
                _formatPrice(sub.subtotalAmount),
                style: AppTypography.labelLg(
                  color: AppColors.primary,
                ),
              ),
            ],
          ),
          const Divider(height: 16),

          // Sub-order Action Buttons Row
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              if (isConfirmed)
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.primary,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    padding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 6),
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => ETicketQrScreen(
                          subOrder: sub,
                          allSubOrders: masterOrder.subOrders,
                        ),
                      ),
                    );
                  },
                  icon: const Icon(Icons.qr_code_2,
                      size: 16, color: Colors.white),
                  label: const Text('Xem vé QR',
                      style: TextStyle(color: Colors.white, fontSize: 11)),
                ),
              OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: AppColors.borderSubtle),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                ),
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => CancelBookingScreen(subOrder: sub),
                    ),
                  );
                },
                icon: const Icon(Icons.cancel_outlined,
                    size: 14, color: AppColors.onSurfaceVariant),
                label: Text('Yêu cầu hủy',
                    style: AppTypography.labelSm(
                        color: AppColors.onSurfaceVariant)),
              ),
              OutlinedButton.icon(
                style: OutlinedButton.styleFrom(
                  side: const BorderSide(color: AppColors.borderSubtle),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                ),
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => CreateDisputeScreen(subOrder: sub),
                    ),
                  );
                },
                icon: const Icon(Icons.report_problem_outlined,
                    size: 14, color: AppColors.onSurfaceVariant),
                label: Text('Khiếu nại',
                    style: AppTypography.labelSm(
                        color: AppColors.onSurfaceVariant)),
              ),
              if (isCompleted)
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.secondary,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                    ),
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  ),
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => ReviewExperienceScreen(subOrder: sub),
                      ),
                    );
                  },
                  icon: const Icon(Icons.star, size: 14, color: Colors.white),
                  label: const Text('Đánh giá',
                      style: TextStyle(color: Colors.white, fontSize: 11)),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPriceRow(String label, String value, {Color? valueColor}) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Expanded(
          child: Text(label,
              style: AppTypography.bodySm(color: AppColors.onSurfaceVariant)),
        ),
        const SizedBox(width: 8),
        Text(
          value,
          style: AppTypography.labelMd(
            color: valueColor ?? AppColors.onSurface,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    );
  }
}
