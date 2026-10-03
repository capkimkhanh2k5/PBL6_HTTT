import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/data/vendor_mock_database.dart';
import '../../../../core/models/vendor_models.dart';
import '../../../../core/widgets/vendor_button.dart';
import '../../../../core/widgets/toast_notification.dart';
import 'sub_order_detail_screen.dart';

class QrCheckinScreen extends StatefulWidget {
  final String? initialLookupCode;

  const QrCheckinScreen({super.key, this.initialLookupCode});

  @override
  State<QrCheckinScreen> createState() => _QrCheckinScreenState();
}

class _QrCheckinScreenState extends State<QrCheckinScreen> {
  final _db = VendorMockDatabase.instance;
  final TextEditingController _lookupController = TextEditingController();

  bool _flashOn = false;
  String _activeTab = 'valid'; // valid, used, invalid
  SubOrderModel? _scannedOrder;

  @override
  void initState() {
    super.initState();
    if (widget.initialLookupCode != null) {
      _lookupController.text = widget.initialLookupCode!;
      _performLookup(widget.initialLookupCode!);
    } else {
      _lookupController.text = 'DNS-8924-1';
      _performLookup('DNS-8924-1');
    }
  }

  @override
  void dispose() {
    _lookupController.dispose();
    super.dispose();
  }

  void _performLookup(String code) {
    final cleaned = code.replaceAll('#', '').trim().toLowerCase();
    final match = _db.subOrders.cast<SubOrderModel?>().firstWhere(
          (o) => o?.subOrderCode.toLowerCase() == cleaned || o?.masterOrderCode.toLowerCase() == cleaned,
          orElse: () => null,
        );

    setState(() {
      _scannedOrder = match;
      if (match == null) {
        _activeTab = 'invalid';
      } else if (match.checkedInAt != null) {
        _activeTab = 'used';
      } else {
        _activeTab = 'valid';
      }
    });
  }

  void _simulateScan(String mode) {
    setState(() {
      _activeTab = mode;
      if (mode == 'valid') {
        _lookupController.text = 'DNS-8924-1';
        _scannedOrder = _db.subOrders.firstWhere((o) => o.subOrderCode == 'DNS-8924-1');
      } else if (mode == 'used') {
        _lookupController.text = 'DNS-7832-1';
        _scannedOrder = _db.subOrders.firstWhere((o) => o.subOrderCode == 'DNS-7832-1');
      } else {
        _lookupController.text = 'OTHER-VENDOR-999';
        _scannedOrder = null;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.secondary),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: const LocalizedText('Quét mã check-in', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Flashlight & Camera Tools Floating Bar
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: BorderRadius.circular(24),
                  boxShadow: AppShapes.shadowSm,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Row(
                        children: [
                          Container(width: 8, height: 8, decoration: const BoxDecoration(shape: BoxShape.circle, color: AppColors.secondary)),
                          const SizedBox(width: 6),
                          const Expanded(
                            child: LocalizedText('Ống kính sẵn sàng', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.secondary), maxLines: 1, overflow: TextOverflow.ellipsis),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 6),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          padding: EdgeInsets.zero,
                          constraints: const BoxConstraints(),
                          icon: Icon(_flashOn ? Icons.flash_on : Icons.flash_off, color: _flashOn ? Colors.amber : AppColors.tertiary, size: 20),
                          onPressed: () => setState(() => _flashOn = !_flashOn),
                          tooltip: tr(context, 'Bật/tắt đèn flash'),
                        ),
                        const SizedBox(width: 6),
                        ElevatedButton.icon(
                          onPressed: () => _simulateScan('valid'),
                          icon: const Icon(Icons.flip_camera_ios, size: 14),
                          label: const LocalizedText('Mô phỏng', style: TextStyle(fontSize: 11)),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppColors.primaryContainer,
                            foregroundColor: AppColors.onPrimary,
                            elevation: 0,
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                            minimumSize: const Size(0, 30),
                            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),

              // Viewfinder Stage with animated reticle
              Container(
                height: 240,
                width: double.infinity,
                decoration: BoxDecoration(
                  borderRadius: AppShapes.radiusLg,
                  color: AppColors.inverseSurface,
                  boxShadow: AppShapes.shadowMd,
                ),
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    // Mock ocean background
                    Opacity(
                      opacity: 0.35,
                      child: Container(
                        decoration: BoxDecoration(
                          borderRadius: AppShapes.radiusLg,
                          gradient: const LinearGradient(
                            colors: [AppColors.secondary, Color(0xFF001F24)],
                            begin: Alignment.topCenter,
                            end: Alignment.bottomCenter,
                          ),
                        ),
                      ),
                    ),
                    // Scanning reticle box
                    Container(
                      width: 170,
                      height: 170,
                      decoration: BoxDecoration(
                        border: Border.all(color: AppColors.secondaryFixed, width: 2),
                        borderRadius: BorderRadius.circular(16),
                      ),
                      child: Stack(
                        alignment: Alignment.center,
                        children: [
                          const Icon(Icons.qr_code_2, size: 110, color: Colors.white24),
                          Container(
                            height: 2,
                            width: 150,
                            color: AppColors.secondaryContainer,
                          ),
                        ],
                      ),
                    ),
                    Positioned(
                      bottom: 12,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.black.withOpacity(0.6),
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: const LocalizedText('Hướng camera về mã QR trên app khách hàng', style: TextStyle(fontSize: 11, color: Colors.white)),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),

              // Manual Code Lookup Input
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 2),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: BorderRadius.circular(24),
                  boxShadow: AppShapes.shadowSm,
                ),
                child: Row(
                  children: [
                    const Icon(Icons.pin, size: 18, color: AppColors.tertiary),
                    const SizedBox(width: 8),
                    Expanded(
                      child: TextField(
                        controller: _lookupController,
                        decoration:  InputDecoration(
                          hintText: tr(context, 'Nhập mã đơn con (vd: DNS-8924-1)...'),
                          hintStyle: TextStyle(fontSize: 12, color: AppColors.outline),
                          border: InputBorder.none,
                        ),
                        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                        onSubmitted: (val) => _performLookup(val),
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.search, size: 20, color: AppColors.secondary),
                      onPressed: () => _performLookup(_lookupController.text),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),

              // Encryption and safety note
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: AppShapes.radiusSm,
                ),
                child: const Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Icon(Icons.verified_user, size: 16, color: AppColors.secondary),
                    SizedBox(width: 8),
                    Expanded(
                      child: LocalizedText(
                        'Hệ thống chỉ giải mã và đối soát đơn hàng thuộc Danang Ocean Club. Khóa bảo mật được mã hóa đầu cuối, đảm bảo chống gian lận vé.',
                        style: TextStyle(fontSize: 11, color: AppColors.onSurfaceVariant),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // Simulation Tab Switcher
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Expanded(
                    child: LocalizedText('Kết quả tra cứu', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.onSurface), maxLines: 1, overflow: TextOverflow.ellipsis),
                  ),
                  const SizedBox(width: 8),
                  const LocalizedText('MÔ PHỎNG KIỂM TRA', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.tertiary)),
                ],
              ),
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(3),
                decoration: BoxDecoration(color: AppColors.surfaceContainer, borderRadius: BorderRadius.circular(24)),
                child: Row(
                  children: [
                    _buildSimTab('valid', 'Hợp lệ'),
                    _buildSimTab('used', 'Đã dùng'),
                    _buildSimTab('invalid', 'Sai đối tác'),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // Result Card by Active Tab
              if (_activeTab == 'valid') _buildValidResult(),
              if (_activeTab == 'used') _buildUsedResult(),
              if (_activeTab == 'invalid') _buildInvalidResult(),

              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSimTab(String tab, String label) {
    final active = _activeTab == tab;
    return Expanded(
      child: GestureDetector(
        onTap: () => _simulateScan(tab),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            color: active ? AppColors.surfaceContainerLowest : Colors.transparent,
            borderRadius: BorderRadius.circular(20),
            boxShadow: active ? AppShapes.shadowSm : null,
          ),
          child: Center(
            child: LocalizedText(
              label,
              style: TextStyle(
                fontSize: 12,
                fontWeight: active ? FontWeight.bold : FontWeight.normal,
                color: active ? AppColors.secondary : AppColors.tertiary,
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildValidResult() {
    final o = _scannedOrder ?? _db.subOrders.first;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusMd,
        boxShadow: AppShapes.shadowSm,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: const BoxDecoration(shape: BoxShape.circle, color: AppColors.secondaryContainer),
                child: const Icon(Icons.check_circle, color: AppColors.onSecondaryContainer, size: 22),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    LocalizedText('Xác nhận thành công!', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                    LocalizedText('Vé hợp lệ & Đủ điều kiện ra bãi', style: TextStyle(fontSize: 11, color: AppColors.secondary, fontWeight: FontWeight.w600)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: AppColors.surfaceContainerLow, borderRadius: BorderRadius.circular(10)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Flexible(
                      child: LocalizedText(
                        '#${o.subOrderCode}',
                        style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.secondary),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Flexible(
                      child: LocalizedText(
                        '${o.slotDate} • ${o.slotTime}',
                        style: const TextStyle(fontSize: 11, color: AppColors.tertiary),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                LocalizedText(o.serviceName, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                LocalizedText('Khách hàng: ${o.customerName}', style: const TextStyle(fontSize: 12, color: AppColors.onSurfaceVariant)),
                LocalizedText('Số lượng: ${o.quantity} người lớn (${o.quantity} ván SUP)', style: const TextStyle(fontSize: 11, color: AppColors.tertiary)),
              ],
            ),
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(color: AppColors.surfaceContainerLow, borderRadius: BorderRadius.circular(8)),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Expanded(
                  child: Row(
                    children: [
                      Icon(Icons.draw, size: 15, color: AppColors.secondary),
                      SizedBox(width: 6),
                      Expanded(
                        child: LocalizedText('Cam kết an toàn biển', style: TextStyle(fontSize: 11, color: AppColors.onSurface), maxLines: 1, overflow: TextOverflow.ellipsis),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 6),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    Icon(Icons.verified, size: 13, color: AppColors.secondary),
                    SizedBox(width: 3),
                    LocalizedText('Đã ký điện tử', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.secondary)),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          VendorButton(
            text: 'Xác nhận vào bãi nhận SUP',
            icon: Icons.kayaking,
            onPressed: () {
              _db.checkInSubOrder(o.subOrderId);
              ToastNotification.showSuccess(context, 'Check-in thành công cho #${o.subOrderCode}!');
              setState(() {
                _activeTab = 'used';
              });
            },
          ),
          const SizedBox(height: 8),
          OutlinedButton.icon(
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => SubOrderDetailScreen(subOrder: o),
                ),
              );
            },
            icon: const Icon(Icons.receipt_long, size: 16, color: AppColors.secondary),
            label: const LocalizedText('Xem chi tiết đơn hàng', style: TextStyle(fontSize: 12, color: AppColors.secondary)),
            style: OutlinedButton.styleFrom(
              minimumSize: const Size(double.infinity, 40),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildUsedResult() {
    final o = _scannedOrder ?? _db.subOrders.first;
    final checkInTime = o.checkedInAt ?? DateTime.now();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusMd,
        boxShadow: AppShapes.shadowSm,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: const BoxDecoration(shape: BoxShape.circle, color: AppColors.primaryFixed),
                child: const Icon(Icons.history, color: AppColors.primary, size: 22),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    LocalizedText('Vé này đã được sử dụng', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                    LocalizedText('Không thể check-in lần thứ hai', style: TextStyle(fontSize: 11, color: AppColors.primary, fontWeight: FontWeight.w600)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: AppColors.surfaceContainerLow, borderRadius: BorderRadius.circular(10)),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LocalizedText('Mã đơn: #${o.subOrderCode}', style: const TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                const SizedBox(height: 4),
                LocalizedText('Khách hàng: ${o.customerName}', style: const TextStyle(fontSize: 12, color: AppColors.onSurfaceVariant)),
                const SizedBox(height: 4),
                Row(
                  children: [
                    const Icon(Icons.access_time, size: 14, color: AppColors.primary),
                    const SizedBox(width: 4),
                    Expanded(
                      child: LocalizedText(
                        'Đã check-in lúc: ${checkInTime.hour}:${checkInTime.minute.toString().padLeft(2, '0')} ngày ${checkInTime.day}/${checkInTime.month}',
                        style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.primary),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => SubOrderDetailScreen(subOrder: o),
                      ),
                    );
                  },
                  icon: const Icon(Icons.receipt_long, size: 16, color: AppColors.secondary),
                  label: const LocalizedText('Chi tiết đơn', style: TextStyle(fontSize: 12, color: AppColors.secondary)),
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 44),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () => _simulateScan('valid'),
                  icon: const Icon(Icons.qr_code_scanner, size: 16),
                  label: const LocalizedText('Quét mã khác', style: TextStyle(fontSize: 12)),
                  style: OutlinedButton.styleFrom(
                    minimumSize: const Size(0, 44),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildInvalidResult() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusMd,
        boxShadow: AppShapes.shadowSm,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: const BoxDecoration(shape: BoxShape.circle, color: AppColors.errorContainer),
                child: const Icon(Icons.error_outline, color: AppColors.error, size: 22),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    LocalizedText('Mã QR không hợp lệ', style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                    LocalizedText('Không thuộc đơn vị Danang Ocean Club', style: TextStyle(fontSize: 11, color: AppColors.error, fontWeight: FontWeight.w600)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(color: AppColors.errorContainer.withOpacity(0.3), borderRadius: BorderRadius.circular(10)),
            child: const LocalizedText(
              'Mã đơn không tồn tại trong hệ thống của bạn hoặc thuộc về một đơn vị đối tác khác tại Đà Nẵng. Vui lòng hướng dẫn khách kiểm tra lại thông tin trên ứng dụng.',
              style: TextStyle(fontSize: 11, color: AppColors.onErrorContainer),
            ),
          ),
          const SizedBox(height: 14),
          OutlinedButton.icon(
            onPressed: () => _simulateScan('valid'),
            icon: const Icon(Icons.qr_code_scanner, size: 16),
            label: const LocalizedText('Thử quét lại', style: TextStyle(fontSize: 12)),
            style: OutlinedButton.styleFrom(
              minimumSize: const Size(double.infinity, 44),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            ),
          ),
        ],
      ),
    );
  }
}
