import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/status_badge.dart';
import '../../../../core/models/vendor_models.dart';
import '../../../../core/data/vendor_mock_database.dart';
import 'payout_detail_screen.dart';

class SettlementRevenueScreen extends StatefulWidget {
  const SettlementRevenueScreen({super.key});

  @override
  State<SettlementRevenueScreen> createState() => _SettlementRevenueScreenState();
}

class _SettlementRevenueScreenState extends State<SettlementRevenueScreen> {
  final VendorMockDatabase _db = VendorMockDatabase.instance;
  String _filterStatus = 'all'; // all, pending, paid

  @override
  void initState() {
    super.initState();
    _db.addListener(_onDbChanged);
  }

  @override
  void dispose() {
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

  @override
  Widget build(BuildContext context) {
    final settlements = _db.settlements;
    final paidSettlements = settlements.where((s) => s.status == SettlementStatus.paid).toList();
    final pendingSettlements = settlements.where((s) => s.status == SettlementStatus.pending).toList();

    List<SettlementModel> filtered = settlements;
    if (_filterStatus == 'pending') {
      filtered = pendingSettlements;
    } else if (_filterStatus == 'paid') {
      filtered = paidSettlements;
    }

    final latestPaid = paidSettlements.isNotEmpty ? paidSettlements.first : null;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const LocalizedText('Đối soát doanh thu'),
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
            // Ambient Top Atmosphere Banner
            if (latestPaid != null) ...[
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: BorderRadius.circular(AppShapes.rLg),
                  boxShadow: const [
                    BoxShadow(color: Color(0x08000000), blurRadius: 8, offset: Offset(0, 2)),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      alignment: WrapAlignment.spaceBetween,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      spacing: 8,
                      runSpacing: 4,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: AppColors.secondary,
                            borderRadius: BorderRadius.circular(AppShapes.rFull),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.verified, color: Colors.white, size: 14),
                              const SizedBox(width: 4),
                              LocalizedText(
                                'KỲ ĐÃ QUYẾT TOÁN',
                                style: AppTypography.labelSm.copyWith(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ),
                        LocalizedText(
                          '${latestPaid.periodStart.day}/${latestPaid.periodStart.month} - ${latestPaid.periodEnd.day}/${latestPaid.periodEnd.month}/${latestPaid.periodEnd.year}',
                          style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    LocalizedText(
                      'Thực nhận về tài khoản đối tác',
                      style: AppTypography.bodySm.copyWith(color: AppColors.onSurfaceVariant),
                    ),
                    const SizedBox(height: 2),
                    LocalizedText(
                      _formatVnd(latestPaid.netPayableAmount),
                      style: AppTypography.headlineLgMobile.copyWith(
                        color: AppColors.onSurface,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 12),

                    // Financial Metric Breakdown Card
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLowest,
                        borderRadius: BorderRadius.circular(AppShapes.rMd),
                      ),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Row(
                                  children: [
                                    const Icon(Icons.sailing, size: 16, color: AppColors.secondary),
                                    const SizedBox(width: 6),
                                    Expanded(
                                      child: LocalizedText(
                                        'Tổng doanh thu dịch vụ',
                                        style: AppTypography.bodySm.copyWith(color: AppColors.onSurfaceVariant),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 8),
                              LocalizedText(
                                _formatVnd(latestPaid.grossAmount),
                                style: AppTypography.labelLg.copyWith(color: AppColors.onSurface),
                              ),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Row(
                                  children: [
                                    const Icon(Icons.percent, size: 16, color: AppColors.primary),
                                    const SizedBox(width: 6),
                                    Expanded(
                                      child: LocalizedText(
                                        'Phí nền tảng DANASEA (~10%)',
                                        style: AppTypography.bodySm.copyWith(color: AppColors.onSurfaceVariant),
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 8),
                              LocalizedText(
                                '-${_formatVnd(latestPaid.commissionAmount)}',
                                style: AppTypography.labelLg.copyWith(color: AppColors.primary),
                              ),
                            ],
                          ),
                          const Divider(height: 16, color: AppColors.surfaceContainerHigh),
                          Row(
                            children: [
                              const Icon(Icons.account_balance, size: 16, color: AppColors.secondary),
                              const SizedBox(width: 6),
                              Expanded(
                                child: LocalizedText(
                                  'Đã thanh toán ngày 16/10/2024 vào Vietcombank',
                                  style: AppTypography.labelSm.copyWith(color: AppColors.secondary),
                                ),
                              ),
                              const Icon(Icons.check_circle, size: 16, color: AppColors.secondary),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
            ],

            // Filter Pills
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  _buildFilterPill('all', 'Tất cả (${settlements.length})'),
                  const SizedBox(width: 8),
                  _buildFilterPill('pending', 'Chờ đối soát (${pendingSettlements.length})'),
                  const SizedBox(width: 8),
                  _buildFilterPill('paid', 'Đã thanh toán (${paidSettlements.length})'),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Settlements List
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: filtered.length,
              separatorBuilder: (_, __) => const SizedBox(height: 12),
              itemBuilder: (context, index) {
                final item = filtered[index];
                return _buildSettlementCard(item);
              },
            ),
            const SizedBox(height: 24),

            // CTA Button to Screen 22 (Payout Details & Request)
            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton.icon(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (ctx) => const PayoutDetailScreen(),
                    ),
                  );
                },
                icon: const Icon(Icons.payments_outlined, size: 20),
                label: const LocalizedText('Chi tiết đối soát & Nhận tiền (Payout)'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppShapes.rFull)),
                ),
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterPill(String id, String label) {
    final isSelected = _filterStatus == id;
    return GestureDetector(
      onTap: () => setState(() => _filterStatus = id),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? AppColors.secondary : AppColors.surfaceContainer,
          borderRadius: BorderRadius.circular(AppShapes.rFull),
        ),
        child: LocalizedText(
          label,
          style: AppTypography.labelMd.copyWith(
            color: isSelected ? Colors.white : AppColors.onSurfaceVariant,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  Widget _buildSettlementCard(SettlementModel item) {
    final isPending = item.status == SettlementStatus.pending;

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (ctx) => PayoutDetailScreen(initialSettlementId: item.id),
            ),
          );
        },
        borderRadius: BorderRadius.circular(AppShapes.rLg),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: AppColors.surfaceContainerLowest,
            borderRadius: BorderRadius.circular(AppShapes.rLg),
            boxShadow: const [
              BoxShadow(color: Color(0x08000000), blurRadius: 6, offset: Offset(0, 2)),
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
                      'KỲ ĐỐI SOÁT #${item.id}',
                      style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                    ),
                    const SizedBox(height: 2),
                    LocalizedText(
                      '${item.periodStart.day}/${item.periodStart.month} - ${item.periodEnd.day}/${item.periodEnd.month}/${item.periodEnd.year}',
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
              StatusBadge(status: item.status.toBadgeStatus()),
            ],
          ),
          const SizedBox(height: 12),

          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: BorderRadius.circular(AppShapes.rMd),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      LocalizedText('Doanh thu tour', style: AppTypography.labelSm.copyWith(color: AppColors.onSurfaceVariant)),
                      const SizedBox(height: 2),
                      LocalizedText(
                        _formatVnd(item.grossAmount),
                        style: AppTypography.labelLg.copyWith(color: AppColors.onSurface),
                      ),
                    ],
                  ),
                ),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      LocalizedText('Phí sàn', style: AppTypography.labelSm.copyWith(color: AppColors.onSurfaceVariant)),
                      const SizedBox(height: 2),
                      LocalizedText(
                        '-${_formatVnd(item.commissionAmount)}',
                        style: AppTypography.labelLg.copyWith(color: AppColors.primary),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    LocalizedText(
                      isPending ? 'Thực nhận dự kiến' : 'Thực nhận ngân hàng',
                      style: AppTypography.labelSm.copyWith(color: AppColors.onSurfaceVariant),
                      overflow: TextOverflow.ellipsis,
                    ),
                    LocalizedText(
                      _formatVnd(item.netPayableAmount),
                      style: AppTypography.headlineSm.copyWith(
                        color: isPending ? AppColors.tertiary : AppColors.secondary,
                        fontWeight: FontWeight.bold,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              OutlinedButton.icon(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (ctx) => PayoutDetailScreen(initialSettlementId: item.id),
                    ),
                  );
                },
                icon: const Icon(Icons.visibility_outlined, size: 16),
                label: const LocalizedText('Xem chi tiết'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.secondary,
                  side: const BorderSide(color: AppColors.outlineVariant),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppShapes.rFull)),
                ),
              ),
            ],
          ),
        ],
      ),
    ),
  ),
);
  }
}
