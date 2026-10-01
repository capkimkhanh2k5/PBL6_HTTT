import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';
import '../../../cart/presentation/screens/cart_screen.dart';
import 'payment_result_screen.dart';

class CheckoutScreen extends StatefulWidget {
  final List<CartItemData> selectedItems;

  const CheckoutScreen({
    super.key,
    required this.selectedItems,
  });

  @override
  State<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends State<CheckoutScreen> {
  PaymentProvider _selectedProvider = PaymentProvider.vnpay;
  bool _waiverAccepted = false;
  final TextEditingController _discountController = TextEditingController();
  int _discountAmount = 0;
  String? _appliedCode;

  @override
  void dispose() {
    _discountController.dispose();
    super.dispose();
  }

  int get _subtotal =>
      widget.selectedItems.fold(0, (sum, item) => sum + item.subtotal);

  int get _finalTotal => (_subtotal - _discountAmount).clamp(0, 999999999);

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

  void _applyDiscount() {
    final code = _discountController.text.trim().toUpperCase();
    if (code == 'DANASEA2024') {
      setState(() {
        _discountAmount = 40000;
        _appliedCode = code;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Áp dụng mã giảm giá DANASEA2024 thành công (-40.000 đ)!'),
          backgroundColor: AppColors.secondary,
        ),
      );
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Mã giảm giá không hợp lệ hoặc đã hết hạn.'),
          backgroundColor: AppColors.error,
        ),
      );
    }
  }

  void _processPayment() {
    if (!_waiverAccepted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
            'Vui lòng xác nhận Cam kết miễn trừ trách nhiệm trước khi thanh toán.',
          ),
          backgroundColor: AppColors.primary,
        ),
      );
      return;
    }

    // Build Master Order & Sub Orders according to PostgreSQL Schema
    final now = DateTime.now();
    final orderId = 'DNS-${(1000 + (now.millisecondsSinceEpoch % 9000))}';

    final subOrders = widget.selectedItems.asMap().entries.map((entry) {
      final idx = entry.key + 1;
      final item = entry.value;
      return SubOrderModel(
        id: '$orderId-$idx',
        masterOrderId: orderId,
        vendorId: item.service.vendorId,
        vendorName: item.service.vendorName,
        serviceId: item.service.id,
        serviceName: item.service.name,
        serviceImageUrl: item.service.imageUrls.isNotEmpty
            ? item.service.imageUrls.first
            : '',
        locationName: item.service.locationName,
        slotId: item.slot.id,
        slotDate: item.slot.date,
        slotTime: '${item.slot.startTime} - ${item.slot.endTime}',
        quantity: item.quantity,
        unitPrice: item.service.price,
        subtotalAmount: item.subtotal,
        status: SubOrderStatus.confirmed,
        waiverAccepted: true,
        waiverAcceptedAt: now,
        qrSecret: 'qr-sec-${DateTime.now().millisecondsSinceEpoch}-$idx',
      );
    }).toList();

    final masterOrder = MasterOrderModel(
      id: orderId,
      customerId: MockDatabaseData.currentUser.id,
      status: MasterOrderStatus.paid,
      totalAmount: _finalTotal,
      discountAmount: _discountAmount,
      discountCode: _appliedCode,
      createdAt: now,
      subOrders: subOrders,
    );

    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (_) => PaymentResultScreen(
          masterOrder: masterOrder,
          provider: _selectedProvider,
        ),
      ),
    );
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
              'Thanh toán',
              style: AppTypography.headlineSm(color: AppColors.onSurface),
            ),
            Text(
              'Bước 2 / 2: Hoàn tất giữ chỗ',
              style: AppTypography.labelSm(
                color: AppColors.secondary,
              ).copyWith(fontSize: 11),
            ),
          ],
        ),
        centerTitle: true,
        actions: const [
          Padding(
            padding: EdgeInsets.only(right: 16),
            child: Icon(Icons.lock_outline, color: AppColors.secondary),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 1. COUNTDOWN INVENTORY LOCK BANNER
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.secondaryFixed.withValues(alpha: 0.4),
                borderRadius: AppShapes.radiusDefault,
              ),
              child: Row(
                children: [
                  Container(
                    width: 32,
                    height: 32,
                    decoration: const BoxDecoration(
                      color: AppColors.secondary,
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(Icons.timer,
                        size: 18, color: AppColors.onSecondary),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Thời gian giữ chỗ',
                          style: AppTypography.labelSm(
                            color: AppColors.secondary,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          'Chỗ được khóa tạm thời trên hệ thống',
                          style: AppTypography.bodySm(
                            color: AppColors.onSurfaceVariant,
                          ).copyWith(fontSize: 11),
                        ),
                      ],
                    ),
                  ),
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLowest,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      '09:42',
                      style: AppTypography.labelLg(
                        color: AppColors.secondary,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // 2. CUSTOMER INFO CARD
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
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Row(
                          children: [
                            const Icon(Icons.badge_outlined,
                                size: 18, color: AppColors.secondary),
                            const SizedBox(width: 6),
                            Flexible(
                              child: Text(
                                'Thông tin người đặt',
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                                style: AppTypography.labelLg(
                                  color: AppColors.onSurface,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        'Từ hồ sơ',
                        style: AppTypography.labelSm(
                          color: AppColors.secondary,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Column(
                      children: [
                        _buildContactRow(Icons.person, user.fullName),
                        const SizedBox(height: 6),
                        _buildContactRow(
                            Icons.phone, user.phone ?? '0905 123 456'),
                        const SizedBox(height: 6),
                        _buildContactRow(Icons.email, user.email),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // 3. ORDER SUMMARY BY PROVIDER
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
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Row(
                          children: [
                            const Icon(Icons.sailing,
                                size: 18, color: AppColors.secondary),
                            const SizedBox(width: 6),
                            Flexible(
                              child: Text(
                                'Tóm tắt dịch vụ biển',
                                overflow: TextOverflow.ellipsis,
                                style: AppTypography.labelLg(
                                  color: AppColors.onSurface,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        '${widget.selectedItems.length} dịch vụ',
                        style: AppTypography.labelSm(
                          color: AppColors.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  ...widget.selectedItems.map((item) {
                    return Container(
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLow,
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Row(
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(6),
                            child: SizedBox(
                              width: 48,
                              height: 48,
                              child: Image.network(
                                item.service.imageUrls.isNotEmpty
                                    ? item.service.imageUrls.first
                                    : '',
                                fit: BoxFit.cover,
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  item.service.name,
                                  style: AppTypography.labelMd(
                                    color: AppColors.onSurface,
                                    fontWeight: FontWeight.w700,
                                  ),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                Text(
                                  '${item.service.vendorName} • ${item.quantity} khách',
                                  style: AppTypography.bodySm(
                                    color: AppColors.onSurfaceVariant,
                                  ).copyWith(fontSize: 11),
                                ),
                              ],
                            ),
                          ),
                          Text(
                            _formatPrice(item.subtotal),
                            style: AppTypography.labelMd(
                              color: AppColors.primary,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    );
                  }),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // 4. DISCOUNT CODE SECTION
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Row(
                children: [
                  const Icon(Icons.confirmation_number_outlined,
                      size: 20, color: AppColors.secondary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      controller: _discountController,
                      textCapitalization: TextCapitalization.characters,
                      decoration: InputDecoration(
                        hintText: 'Mã giảm giá (ví dụ DANASEA2024)',
                        hintStyle:
                            AppTypography.bodySm(color: AppColors.outline),
                        border: InputBorder.none,
                        isDense: true,
                      ),
                    ),
                  ),
                  ElevatedButton(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.secondary,
                      padding: const EdgeInsets.symmetric(horizontal: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                    ),
                    onPressed: _applyDiscount,
                    child: Text(
                      'Áp dụng',
                      style: AppTypography.labelSm(color: Colors.white),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // 5. PAYMENT METHOD SELECTION (VNPAY, MOMO, SEPAY)
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
                  Row(
                    children: [
                      const Icon(Icons.payment,
                          size: 18, color: AppColors.secondary),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          'Phương thức thanh toán',
                          style: AppTypography.labelLg(
                            color: AppColors.onSurface,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  _buildPaymentRadio(
                    provider: PaymentProvider.vnpay,
                    title: 'VNPAY Cổng thanh toán',
                    subtitle: 'Hỗ trợ thẻ ATM nội địa, QR Pay, Visa/Mastercard',
                    icon: Icons.credit_card,
                  ),
                  _buildPaymentRadio(
                    provider: PaymentProvider.momo,
                    title: 'Ví điện tử MoMo',
                    subtitle: 'Quét mã MoMo thanh toán liền mạch',
                    icon: Icons.account_balance_wallet,
                  ),
                  _buildPaymentRadio(
                    provider: PaymentProvider.sepay,
                    title: 'Chuyển khoản VietQR (SePay)',
                    subtitle: 'Xác nhận giao dịch tự động 24/7',
                    icon: Icons.qr_code,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // 6. WAIVER AGREEMENT CHECKBOX
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(
                  color: _waiverAccepted
                      ? AppColors.secondary
                      : AppColors.borderSubtle,
                ),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Checkbox(
                    value: _waiverAccepted,
                    activeColor: AppColors.secondary,
                    onChanged: (val) {
                      setState(() => _waiverAccepted = val ?? false);
                    },
                  ),
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Text(
                        'Tôi xác nhận đủ điều kiện sức khỏe và đồng ý với Cam kết miễn trừ trách nhiệm khi tham gia các hoạt động thể thao biển tại Đà Nẵng.',
                        style: AppTypography.bodySm(
                          color: AppColors.onSurface,
                        ).copyWith(fontSize: 12),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // 7. SUMMARY & CONFIRM BUTTON
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Column(
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Tạm tính:',
                          style: AppTypography.bodySm(
                              color: AppColors.onSurfaceVariant)),
                      Text(_formatPrice(_subtotal),
                          style: AppTypography.labelMd(
                              color: AppColors.onSurface)),
                    ],
                  ),
                  if (_discountAmount > 0) ...[
                    const SizedBox(height: 6),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text('Giảm giá ($_appliedCode):',
                            style: AppTypography.bodySm(
                                color: AppColors.secondary)),
                        Text('- ${_formatPrice(_discountAmount)}',
                            style: AppTypography.labelMd(
                                color: AppColors.secondary)),
                      ],
                    ),
                  ],
                  const Divider(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Text('Tổng thanh toán:',
                            style: AppTypography.labelLg(
                                color: AppColors.onSurface)),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        _formatPrice(_finalTotal),
                        style: AppTypography.headlineSm(
                          color: AppColors.primary,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),
                  AppPillButton(
                    label: 'Xác nhận thanh toán ${_formatPrice(_finalTotal)}',
                    variant: AppButtonVariant.primary,
                    width: double.infinity,
                    onPressed: _processPayment,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildContactRow(IconData icon, String text) {
    return Row(
      children: [
        Icon(icon, size: 16, color: AppColors.tertiary),
        const SizedBox(width: 8),
        Expanded(
          child: Text(
            text,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: AppTypography.bodySm(
              color: AppColors.onSurface,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPaymentRadio({
    required PaymentProvider provider,
    required String title,
    required String subtitle,
    required IconData icon,
  }) {
    final isSelected = _selectedProvider == provider;
    return InkWell(
      onTap: () => setState(() => _selectedProvider = provider),
      borderRadius: BorderRadius.circular(8),
      child: Container(
        margin: const EdgeInsets.only(bottom: 8),
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: isSelected
              ? AppColors.secondaryContainer.withValues(alpha: 0.3)
              : AppColors.surfaceContainerLow,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: isSelected ? AppColors.secondary : Colors.transparent,
            width: 1.2,
          ),
        ),
        child: Row(
          children: [
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
              child: Container(
                width: 20,
                height: 20,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  border: Border.all(
                    color: isSelected ? AppColors.secondary : AppColors.outline,
                    width: 2,
                  ),
                ),
                child: isSelected
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
            ),
            Icon(icon, size: 20, color: AppColors.secondary),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: AppTypography.labelMd(
                      color: AppColors.onSurface,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    subtitle,
                    style: AppTypography.bodySm(
                      color: AppColors.onSurfaceVariant,
                    ).copyWith(fontSize: 10),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
