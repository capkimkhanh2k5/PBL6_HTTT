import 'package:flutter/material.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';
import 'e_ticket_qr_screen.dart';

class PaymentResultScreen extends StatelessWidget {
  final MasterOrderModel masterOrder;
  final PaymentProvider provider;
  final bool isSuccess;

  const PaymentResultScreen({
    super.key,
    required this.masterOrder,
    required this.provider,
    this.isSuccess = true,
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

  String _getProviderName(PaymentProvider p) {
    switch (p) {
      case PaymentProvider.vnpay:
        return 'VNPAY Cổng thanh toán';
      case PaymentProvider.momo:
        return 'Ví MoMo';
      case PaymentProvider.sepay:
        return 'VietQR (SePay)';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        automaticallyImplyLeading: false,
        title: Text(
          'Kết quả thanh toán',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          children: [
            const SizedBox(height: 12),

            // SUCCESS / FAILED ICON & STATUS
            Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                color: isSuccess
                    ? AppColors.secondaryContainer
                    : AppColors.errorContainer,
                shape: BoxShape.circle,
              ),
              child: Icon(
                isSuccess ? Icons.check_circle : Icons.error,
                size: 52,
                color: isSuccess
                    ? AppColors.onSecondaryContainer
                    : AppColors.error,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              isSuccess ? 'Thanh toán thành công!' : 'Thanh toán chưa hoàn tất',
              style: AppTypography.headlineLgMobile(
                color: isSuccess ? AppColors.secondary : AppColors.error,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              isSuccess
                  ? 'Đơn đặt chỗ trải nghiệm biển của bạn đã được xác nhận.'
                  : 'Giao dịch bị gián đoạn, vui lòng kiểm tra lại phương thức thanh toán.',
              textAlign: TextAlign.center,
              style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
            ),
            const SizedBox(height: 24),

            // TRANSACTION DETAILS CARD
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceMd),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                boxShadow: AppShapes.shadowLevel1,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Column(
                children: [
                  _buildDetailRow('Mã đơn hàng:', '#${masterOrder.id}'),
                  const Divider(height: 16),
                  _buildDetailRow(
                      'Cổng thanh toán:', _getProviderName(provider)),
                  const Divider(height: 16),
                  _buildDetailRow(
                    'Mã giao dịch:',
                    'TXN-${DateTime.now().millisecondsSinceEpoch.toString().substring(5)}',
                  ),
                  const Divider(height: 16),
                  _buildDetailRow(
                    'Thời gian:',
                    '${masterOrder.createdAt.hour.toString().padLeft(2, '0')}:${masterOrder.createdAt.minute.toString().padLeft(2, '0')} • ${masterOrder.createdAt.day}/${masterOrder.createdAt.month}/${masterOrder.createdAt.year}',
                  ),
                  const Divider(height: 16),
                  _buildDetailRow(
                    'Số lượng dịch vụ:',
                    '${masterOrder.subOrders.length} dịch vụ',
                  ),
                  const Divider(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text(
                          'Tổng tiền đã thanh toán:',
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
            const SizedBox(height: 20),

            // INSTRUCTION BOX
            if (isSuccess)
              Container(
                padding: const EdgeInsets.all(AppShapes.spaceSm),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: AppShapes.radiusDefault,
                ),
                child: Row(
                  children: [
                    const Icon(Icons.mark_email_read_outlined,
                        size: 24, color: AppColors.secondary),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'Vé điện tử QR và thông tin chi tiết đã được gửi đến email an.nguyen@example.com',
                        style: AppTypography.bodySm(
                          color: AppColors.onSurface,
                        ).copyWith(fontSize: 12),
                      ),
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 24),

            // ACTION BUTTONS
            if (isSuccess && masterOrder.subOrders.isNotEmpty)
              AppPillButton(
                label: 'Xem vé QR check-in',
                variant: AppButtonVariant.primary,
                leadingIcon: Icons.qr_code_2,
                width: double.infinity,
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => ETicketQrScreen(
                        subOrder: masterOrder.subOrders.first,
                        allSubOrders: masterOrder.subOrders,
                      ),
                    ),
                  );
                },
              ),
            const SizedBox(height: 12),
            AppPillButton(
              label: 'Tải hóa đơn điện tử (PDF)',
              variant: AppButtonVariant.secondary,
              leadingIcon: Icons.receipt_long,
              width: double.infinity,
              onPressed: () {
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Đang tạo và tải file hóa đơn điện tử PDF...'),
                    backgroundColor: AppColors.secondary,
                  ),
                );
              },
            ),
            const SizedBox(height: 12),
            TextButton.icon(
              onPressed: () {
                Navigator.of(context).popUntil((route) => route.isFirst);
              },
              icon: const Icon(Icons.arrow_back, size: 16),
              label: const Text('Về trang Khám phá'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDetailRow(String label, String value) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(
          label,
          style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Text(
            value,
            textAlign: TextAlign.right,
            style: AppTypography.labelMd(
              color: AppColors.onSurface,
              fontWeight: FontWeight.w700,
            ),
          ),
        ),
      ],
    );
  }
}
