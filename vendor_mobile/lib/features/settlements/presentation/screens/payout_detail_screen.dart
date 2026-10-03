import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/status_badge.dart';
import '../../../../core/widgets/toast_notification.dart';
import '../../../../core/models/vendor_models.dart';
import '../../../../core/data/vendor_mock_database.dart';
import '../../../../core/data/vendor_mock_repositories.dart';
import '../../../profile/presentation/screens/edit_profile_bank_screen.dart';
import '../../../orders/presentation/screens/sub_order_detail_screen.dart';

class PayoutDetailScreen extends StatefulWidget {
  final String? initialSettlementId;

  const PayoutDetailScreen({super.key, this.initialSettlementId});

  @override
  State<PayoutDetailScreen> createState() => _PayoutDetailScreenState();
}

class _PayoutDetailScreenState extends State<PayoutDetailScreen> {
  final VendorSettlementRepository _settlementRepo = VendorSettlementRepository();
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  late String _currentSettlementId;
  bool _isCombined = false;
  late TextEditingController _amountController;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _currentSettlementId = widget.initialSettlementId ??
        (_db.settlements.isNotEmpty ? _db.settlements.first.id : 'SETTLE-202410-01');

    final currentSettlement = _db.settlements.firstWhere(
      (s) => s.id == _currentSettlementId,
      orElse: () => _db.settlements.first,
    );
    _amountController = TextEditingController(
      text: currentSettlement.netPayableAmount.toInt().toString(),
    );

    _db.addListener(_onDbChanged);
  }

  @override
  void dispose() {
    _amountController.dispose();
    _db.removeListener(_onDbChanged);
    super.dispose();
  }

  void _onDbChanged() {
    if (mounted) setState(() {});
  }

  String _formatVnd(double amount) {
    final intVal = amount.toInt();
    final str = intVal.toString();
    final buffer = StringBuffer();
    int count = 0;
    for (int i = str.length - 1; i >= 0; i--) {
      buffer.write(str[i]);
      count++;
      if (count % 3 == 0 && i != 0) {
        buffer.write('.');
      }
    }
    return '${buffer.toString().split('').reversed.join()} đ';
  }

  double _getEligibleMaxAmount() {
    if (_isCombined) {
      return _db.settlements
          .where((s) => s.status == SettlementStatus.pending)
          .fold(0.0, (acc, item) => acc + item.netPayableAmount);
    }
    final current = _db.settlements.firstWhere(
      (s) => s.id == _currentSettlementId,
      orElse: () => _db.settlements.first,
    );
    return current.netPayableAmount;
  }

  Future<void> _handleSubmitPayout() async {
    final amount = double.tryParse(_amountController.text.trim()) ?? 0;
    final maxAmount = _getEligibleMaxAmount();

    if (amount <= 0) {
      ToastNotification.show(context, message: 'Vui lòng nhập số tiền hợp lệ.', type: ToastType.error);
      return;
    }
    if (amount > maxAmount) {
      ToastNotification.show(
        context,
        message: 'Số tiền rút (${_formatVnd(amount)}) vượt quá hạn mức khả dụng (${_formatVnd(maxAmount)}).',
        type: ToastType.error,
      );
      return;
    }

    setState(() => _isSubmitting = true);
    try {
      await _settlementRepo.requestPayout(
        settlementId: _isCombined ? null : _currentSettlementId,
        amount: amount,
      );

      if (!mounted) return;
      setState(() => _isSubmitting = false);
      ToastNotification.show(
        context,
        message: 'Yêu cầu nhận tiền ${_formatVnd(amount)} đã được gửi tới ban tài chính DANASEA!',
        type: ToastType.success,
      );
    } catch (e) {
      if (!mounted) return;
      setState(() => _isSubmitting = false);
      ToastNotification.show(context, message: 'Lỗi gửi yêu cầu: $e', type: ToastType.error);
    }
  }

  @override
  Widget build(BuildContext context) {
    final currentSettlement = _db.settlements.firstWhere(
      (s) => s.id == _currentSettlementId,
      orElse: () => _db.settlements.first,
    );
    final vendor = _db.currentVendor;
    final maxEligible = _getEligibleMaxAmount();
    final payoutRequests = _db.payoutRequests;

    // Sub-orders associated with vendor for sample breakdown in period
    final sampleSubOrders = _db.subOrders.take(3).toList();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const LocalizedText('Chi tiết đối soát & Nhận tiền'),
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.maybePop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Cycle Meta Capsule
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x08000000), blurRadius: 6, offset: Offset(0, 2)),
                ],
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainer,
                            borderRadius: BorderRadius.circular(AppShapes.rFull),
                          ),
                          child: LocalizedText(
                            'KỲ ĐỐI SOÁT #${currentSettlement.id}',
                            style: AppTypography.labelSm.copyWith(color: AppColors.onSurfaceVariant),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const SizedBox(height: 6),
                        LocalizedText(
                          '${currentSettlement.periodStart.day}/${currentSettlement.periodStart.month} - ${currentSettlement.periodEnd.day}/${currentSettlement.periodEnd.month}/${currentSettlement.periodEnd.year}',
                          style: AppTypography.headlineSm.copyWith(
                            color: AppColors.onSurface,
                            fontWeight: FontWeight.bold,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  StatusBadge(status: currentSettlement.status.toBadgeStatus()),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Financial Summary Hero Card
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [AppColors.surfaceContainerLow, AppColors.surfaceContainer],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(AppShapes.rXl),
                boxShadow: const [
                  BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            LocalizedText(
                              'THỰC NHẬN KHẢ DỤNG',
                              style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                            ),
                            const SizedBox(height: 2),
                            LocalizedText(
                              _formatVnd(currentSettlement.netPayableAmount),
                              style: AppTypography.headlineLgMobile.copyWith(
                                color: AppColors.primary,
                                fontWeight: FontWeight.bold,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        width: 44,
                        height: 44,
                        decoration: BoxDecoration(
                          color: AppColors.primaryContainer.withAlpha(35),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.account_balance_wallet, color: AppColors.primary, size: 24),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Metrics Bento
                  Row(
                    children: [
                      Expanded(
                        child: Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLowest.withAlpha(220),
                            borderRadius: BorderRadius.circular(AppShapes.rMd),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              LocalizedText(
                                'Doanh thu (${currentSettlement.orderCount} đơn)',
                                style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 2),
                              LocalizedText(
                                _formatVnd(currentSettlement.grossAmount),
                                style: AppTypography.labelLg.copyWith(
                                  color: AppColors.onSurface,
                                  fontWeight: FontWeight.bold,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLowest.withAlpha(220),
                            borderRadius: BorderRadius.circular(AppShapes.rMd),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              LocalizedText(
                                'Phí sàn (~10%)',
                                style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 2),
                              LocalizedText(
                                '-${_formatVnd(currentSettlement.commissionAmount)}',
                                style: AppTypography.labelLg.copyWith(
                                  color: AppColors.primary,
                                  fontWeight: FontWeight.bold,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),

                  // Distribution Gauge
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Flexible(
                        child: LocalizedText(
                          'Tỷ lệ phân phối sàn & đối tác',
                          style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Flexible(
                        child: LocalizedText(
                          '90% Đối tác • 10% Phí sàn',
                          style: AppTypography.labelSm.copyWith(color: AppColors.secondary, fontWeight: FontWeight.bold),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  ClipRRect(
                    borderRadius: BorderRadius.circular(AppShapes.rFull),
                    child: SizedBox(
                      height: 8,
                      child: Row(
                        children: [
                          Expanded(flex: 90, child: Container(color: AppColors.secondary)),
                          Expanded(flex: 10, child: Container(color: AppColors.primaryContainer)),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Detailed Service Bookings in Cycle
            LocalizedText('Đơn dịch vụ trong kỳ', style: AppTypography.headlineSm.copyWith(color: AppColors.onSurface)),
            const SizedBox(height: 4),
            LocalizedText('Các đơn đã hoàn thành thực tế được chốt đối soát', style: AppTypography.bodySm.copyWith(color: AppColors.tertiary)),
            const SizedBox(height: 10),

            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: sampleSubOrders.length,
              separatorBuilder: (_, __) => const SizedBox(height: 8),
              itemBuilder: (context, idx) {
                final o = sampleSubOrders[idx];
                return Material(
                  color: Colors.transparent,
                  child: InkWell(
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (_) => SubOrderDetailScreen(subOrder: o),
                        ),
                      );
                    },
                    borderRadius: BorderRadius.circular(AppShapes.rMd),
                    child: Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLowest,
                        borderRadius: BorderRadius.circular(AppShapes.rMd),
                        border: Border.all(color: AppColors.outlineVariant),
                      ),
                      child: Row(
                        children: [
                          ClipRRect(
                            borderRadius: BorderRadius.circular(AppShapes.rSm),
                            child: Image.network(
                              o.serviceImageUrl,
                              width: 44,
                              height: 44,
                              fit: BoxFit.cover,
                              errorBuilder: (_, __, ___) => Container(
                                width: 44,
                                height: 44,
                                color: AppColors.surfaceContainerLow,
                                child: const Icon(Icons.surfing, size: 20, color: AppColors.secondary),
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                LocalizedText(
                                  o.serviceName,
                                  style: AppTypography.labelMd.copyWith(color: AppColors.onSurface, fontWeight: FontWeight.bold),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                LocalizedText(
                                  '#${o.id} • ${o.quantity} khách • ${o.slotDate}',
                                  style: AppTypography.bodySm.copyWith(color: AppColors.tertiary, fontSize: 12),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ],
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              LocalizedText(
                                _formatVnd(o.vendorPayoutAmount),
                                style: AppTypography.labelLg.copyWith(color: AppColors.secondary, fontWeight: FontWeight.bold),
                              ),
                              LocalizedText(
                                'Phí -${_formatVnd(o.commissionAmount)}',
                                style: AppTypography.bodySm.copyWith(color: AppColors.primary, fontSize: 11),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 24),

            // Payout Request Form
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(AppShapes.rXl),
                boxShadow: const [
                  BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        width: 32,
                        height: 32,
                        decoration: BoxDecoration(
                          color: AppColors.primaryContainer.withAlpha(35),
                          shape: BoxShape.circle,
                        ),
                        child: const Icon(Icons.payments, color: AppColors.primary, size: 18),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: LocalizedText(
                          'Gửi yêu cầu nhận tiền',
                          style: AppTypography.headlineSm.copyWith(color: AppColors.onSurface),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),

                  // Beneficiary Card
                  Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(builder: (_) => const EditProfileBankScreen()),
                        );
                      },
                      borderRadius: BorderRadius.circular(AppShapes.rMd),
                      child: Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(AppShapes.rMd),
                        ),
                        child: Row(
                          children: [
                            Container(
                              width: 38,
                              height: 38,
                              decoration: const BoxDecoration(
                                color: AppColors.surfaceContainerHighest,
                                shape: BoxShape.circle,
                              ),
                              alignment: Alignment.center,
                              child: LocalizedText(
                                'VCB',
                                style: AppTypography.labelMd.copyWith(
                                  color: AppColors.secondary,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  LocalizedText(
                                    vendor.bankAccountNumber ?? '0041000889988',
                                    style: AppTypography.labelLg.copyWith(
                                      color: AppColors.onSurface,
                                      fontWeight: FontWeight.bold,
                                      letterSpacing: 1.0,
                                    ),
                                  ),
                                  LocalizedText(
                                    '${vendor.bankAccountHolder ?? "TRAN HAI DANG"} • ${vendor.bankName ?? "Vietcombank"} CN Đà Nẵng',
                                    style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ],
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                              decoration: BoxDecoration(
                                color: AppColors.secondaryFixed.withAlpha(80),
                                borderRadius: BorderRadius.circular(AppShapes.rFull),
                              ),
                              child: LocalizedText(
                                'Thay đổi',
                                style: AppTypography.labelSm.copyWith(
                                  color: AppColors.onSecondaryFixedVariant,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            const SizedBox(width: 4),
                            const Icon(Icons.chevron_right, size: 18, color: AppColors.outlineVariant),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Combined payout option toggle
                  Material(
                    color: Colors.transparent,
                    child: CheckboxListTile(
                      value: _isCombined,
                      onChanged: (val) {
                        setState(() {
                          _isCombined = val ?? false;
                          _amountController.text = _getEligibleMaxAmount().toInt().toString();
                        });
                      },
                      title: LocalizedText(
                        'Rút gộp tất cả các kỳ chờ đối soát (${_formatVnd(_getEligibleMaxAmount())})',
                        style: AppTypography.labelMd.copyWith(color: AppColors.onSurface),
                      ),
                      contentPadding: EdgeInsets.zero,
                      controlAffinity: ListTileControlAffinity.leading,
                      activeColor: AppColors.secondary,
                    ),
                  ),
                  const SizedBox(height: 8),

                  // Amount Input
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Flexible(
                        child: LocalizedText(
                          'Số tiền yêu cầu rút',
                          style: AppTypography.labelMd.copyWith(color: AppColors.tertiary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Flexible(
                        child: LocalizedText(
                          'Tối đa: ${_formatVnd(maxEligible)}',
                          style: AppTypography.labelSm.copyWith(color: AppColors.secondary, fontWeight: FontWeight.bold),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  TextField(
                    controller: _amountController,
                    keyboardType: TextInputType.number,
                    style: AppTypography.headlineSm.copyWith(color: AppColors.onSurface),
                    decoration: InputDecoration(
                      suffixText: 'đ',
                      suffixStyle: AppTypography.headlineSm.copyWith(color: AppColors.primary),
                      filled: true,
                      fillColor: AppColors.surfaceContainerLow,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(AppShapes.rFull),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
                    ),
                  ),
                  const SizedBox(height: 8),

                  Align(
                    alignment: Alignment.centerLeft,
                    child: TextButton(
                      onPressed: () {
                        setState(() {
                          _amountController.text = maxEligible.toInt().toString();
                        });
                      },
                      child: const LocalizedText('Điền số tiền tối đa', overflow: TextOverflow.ellipsis),
                    ),
                  ),
                  const SizedBox(height: 12),

                  // Honest Platform Notice Box
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(AppShapes.rMd),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.verified_user, size: 16, color: AppColors.primaryContainer),
                            const SizedBox(width: 6),
                            Expanded(
                              child: LocalizedText(
                                'Chính sách thanh toán DANASEA',
                                style: AppTypography.labelSm.copyWith(
                                  color: AppColors.onSurface,
                                  fontWeight: FontWeight.bold,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        LocalizedText(
                          'Đối tác gửi yêu cầu nhận tiền trực tiếp đến ban tài chính DANASEA. Hệ thống không tự động phê duyệt tức thì nhằm rà soát tính hợp lệ của tour đã hoàn tất. Thời gian xử lý được cập nhật theo tiến độ yêu cầu thanh toán.',
                          style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Submit Payout Button
                  SizedBox(
                    width: double.infinity,
                    height: 50,
                    child: ElevatedButton.icon(
                      onPressed: _isSubmitting ? null : _handleSubmitPayout,
                      icon: const Icon(Icons.send, size: 18),
                      label: const LocalizedText('Gửi yêu cầu nhận tiền', overflow: TextOverflow.ellipsis),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(AppShapes.rFull),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Payout Cycle History & Request Audit Trail
            LocalizedText('Lịch sử yêu cầu nhận tiền', style: AppTypography.headlineSm.copyWith(color: AppColors.onSurface)),
            const SizedBox(height: 4),
            LocalizedText('Trạng thái thẩm định và chi trả qua các kỳ', style: AppTypography.bodySm.copyWith(color: AppColors.tertiary)),
            const SizedBox(height: 12),

            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: payoutRequests.length,
              separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (context, idx) {
                final p = payoutRequests[idx];
                return Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainerLowest,
                    borderRadius: BorderRadius.circular(AppShapes.rLg),
                    boxShadow: const [
                      BoxShadow(color: Color(0x06000000), blurRadius: 6, offset: Offset(0, 2)),
                    ],
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: LocalizedText(
                              '#${p.id}',
                              style: AppTypography.labelLg.copyWith(
                                color: AppColors.onSurface,
                                fontWeight: FontWeight.bold,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Flexible(
                            child: StatusBadge(status: p.status.toBadgeStatus()),
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: LocalizedText(
                              'Yêu cầu rút: ${_formatVnd(p.amount)}',
                              style: AppTypography.headlineSm.copyWith(
                                color: AppColors.secondary,
                                fontWeight: FontWeight.bold,
                              ),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          const SizedBox(width: 8),
                          LocalizedText(
                            '${p.createdAt.day}/${p.createdAt.month}/${p.createdAt.year}',
                            style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      LocalizedText(
                        'Tài khoản thụ hưởng: ${p.bankName} ${p.bankAccountNumber}',
                        style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                      ),
                    ],
                  ),
                );
              },
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}
