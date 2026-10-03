import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';

class ETicketQrScreen extends StatefulWidget {
  final SubOrderModel subOrder;
  final List<SubOrderModel>? allSubOrders;

  const ETicketQrScreen({
    super.key,
    required this.subOrder,
    this.allSubOrders,
  });

  @override
  State<ETicketQrScreen> createState() => _ETicketQrScreenState();
}

class _ETicketQrScreenState extends State<ETicketQrScreen> {
  late SubOrderModel _currentSubOrder;

  @override
  void initState() {
    super.initState();
    _currentSubOrder = widget.subOrder;
  }

  @override
  Widget build(BuildContext context) {
    final tickets = widget.allSubOrders ?? [_currentSubOrder];
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
        title: LocalizedText(
          'Vé điện tử QR',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.share, color: AppColors.onSurface),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: LocalizedText('Đã tạo liên kết chia sẻ vé điện tử!'),
                  backgroundColor: AppColors.secondary,
                ),
              );
            },
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(
          horizontal: AppShapes.gutterMobile,
          vertical: 8,
        ),
        child: Column(
          children: [
            // MULTI-TICKET SWITCHER (IF MORE THAN 1 SUB-ORDER)
            if (tickets.length > 1)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: tickets.asMap().entries.map((entry) {
                    final idx = entry.key;
                    final item = entry.value;
                    final isSel = item.id == _currentSubOrder.id;
                    return InkWell(
                      onTap: () {
                        setState(() => _currentSubOrder = item);
                      },
                      child: Container(
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: isSel
                              ? AppColors.secondary
                              : AppColors.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(16),
                        ),
                        child: LocalizedText(
                          'Vé ${idx + 1}/${tickets.length}',
                          style: AppTypography.labelSm(
                            color: isSel ? Colors.white : AppColors.onSurface,
                            fontWeight: isSel ? FontWeight.w700 : FontWeight.w500,
                          ),
                        ),
                      ),
                    );
                  }).toList(),
                ),
              ),

            // BOARDING PASS / E-TICKET CARD
            Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusLg,
                boxShadow: AppShapes.shadowLevel2,
                border: Border.all(
                  color: AppColors.borderSubtle.withValues(alpha: 0.8),
                ),
              ),
              clipBehavior: Clip.antiAlias,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // TOP HERO BANNER OF TICKET
                  SizedBox(
                    height: 110,
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        Image.network(
                          _currentSubOrder.serviceImageUrl.isNotEmpty
                              ? _currentSubOrder.serviceImageUrl
                              : 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=600&auto=format&fit=crop&q=80',
                          fit: BoxFit.cover,
                        ),
                        Container(
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              begin: Alignment.topCenter,
                              end: Alignment.bottomCenter,
                              colors: [
                                Colors.black.withValues(alpha: 0.3),
                                Colors.black.withValues(alpha: 0.6),
                              ],
                            ),
                          ),
                        ),
                        // Top live ocean pill
                        Positioned(
                          top: 10,
                          left: 10,
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 3),
                            decoration: BoxDecoration(
                              color: AppColors.surface.withValues(alpha: 0.9),
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Row(
                              children: [
                                Container(
                                  width: 6,
                                  height: 6,
                                  decoration: const BoxDecoration(
                                    color: AppColors.secondary,
                                    shape: BoxShape.circle,
                                  ),
                                ),
                                const SizedBox(width: 4),
                                LocalizedText(
                                  'Sóng 0.4m • Gió 8 km/h',
                                  style: AppTypography.labelSm(
                                    color: AppColors.onSurface,
                                    fontWeight: FontWeight.w700,
                                  ).copyWith(fontSize: 10),
                                ),
                              ],
                            ),
                          ),
                        ),
                        // Valid check-in badge
                        Positioned(
                          top: 10,
                          right: 10,
                          child: Container(
                            padding: const EdgeInsets.symmetric(
                                horizontal: 8, vertical: 3),
                            decoration: BoxDecoration(
                              color: AppColors.secondaryContainer,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Row(
                              children: [
                                const Icon(Icons.verified,
                                    size: 12,
                                    color: AppColors.onSecondaryContainer),
                                const SizedBox(width: 4),
                                LocalizedText(
                                  'HỢP LỆ CHECK-IN',
                                  style: AppTypography.labelSm(
                                    color: AppColors.onSecondaryContainer,
                                    fontWeight: FontWeight.w800,
                                  ).copyWith(fontSize: 10),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),

                  // TICKET MAIN BODY
                  Padding(
                    padding: const EdgeInsets.all(AppShapes.spaceMd),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            const Icon(Icons.surfing,
                                size: 14, color: AppColors.secondary),
                            const SizedBox(width: 4),
                            Expanded(
                              child: LocalizedText(
                                '${_currentSubOrder.vendorName} • ${_currentSubOrder.locationName}',
                                style: AppTypography.labelSm(
                                  color: AppColors.secondary,
                                  fontWeight: FontWeight.w700,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        LocalizedText(
                          _currentSubOrder.serviceName,
                          style: AppTypography.headlineSm(
                            color: AppColors.onSurface,
                          ),
                        ),
                        const SizedBox(height: 14),

                        // Key Metadata 2x2 Grid
                        Row(
                          children: [
                            Expanded(
                              child: _buildMetaBox(
                                Icons.calendar_today,
                                'Ngày trải nghiệm',
                                '${_currentSubOrder.slotDate.day}/${_currentSubOrder.slotDate.month}/${_currentSubOrder.slotDate.year}',
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: _buildMetaBox(
                                Icons.schedule,
                                'Khung giờ biển',
                                _currentSubOrder.slotTime,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            Expanded(
                              child: _buildMetaBox(
                                Icons.tag,
                                'Mã vé / Mã đơn',
                                '#${_currentSubOrder.id}',
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: _buildMetaBox(
                                Icons.group,
                                'Số lượng khách',
                                '${_currentSubOrder.quantity} người lớn',
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),

                        // Primary Attendee
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 10, vertical: 8),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLow,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Row(
                                  children: [
                                    const Icon(Icons.person,
                                        size: 16, color: AppColors.secondary),
                                    const SizedBox(width: 6),
                                    Flexible(
                                      child: LocalizedText(
                                        user.fullName,
                                        overflow: TextOverflow.ellipsis,
                                        style: AppTypography.labelMd(
                                          color: AppColors.onSurface,
                                          fontWeight: FontWeight.w700,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: AppColors.surfaceContainerLowest,
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: LocalizedText(
                                  'Người đại diện',
                                  style: AppTypography.labelSm(
                                    color: AppColors.secondary,
                                  ).copyWith(fontSize: 10),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),

                  // TEAR-OFF PERFORATION DIVIDER
                  Row(
                    children: [
                      Container(
                        width: 14,
                        height: 24,
                        decoration: const BoxDecoration(
                          color: AppColors.surface,
                          borderRadius: BorderRadius.horizontal(
                            right: Radius.circular(14),
                          ),
                        ),
                      ),
                      Expanded(
                        child: LayoutBuilder(
                          builder: (context, constraints) {
                            final dashCount = (constraints.constrainWidth() / 10).floor();
                            return Flex(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              direction: Axis.horizontal,
                              children: List.generate(
                                dashCount,
                                (_) => const SizedBox(
                                  width: 5,
                                  height: 1.5,
                                  child: DecoratedBox(
                                    decoration: BoxDecoration(
                                      color: AppColors.outlineVariant,
                                    ),
                                  ),
                                ),
                              ),
                            );
                          },
                        ),
                      ),
                      Container(
                        width: 14,
                        height: 24,
                        decoration: const BoxDecoration(
                          color: AppColors.surface,
                          borderRadius: BorderRadius.horizontal(
                            left: Radius.circular(14),
                          ),
                        ),
                      ),
                    ],
                  ),

                  // QR CODE COMPARTMENT
                  Padding(
                    padding: const EdgeInsets.all(AppShapes.spaceMd),
                    child: Column(
                      children: [
                        // Outdoor High-Contrast QR Frame
                        Container(
                          width: 200,
                          height: 200,
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(16),
                            boxShadow: [
                              BoxShadow(
                                color: Colors.black.withValues(alpha: 0.08),
                                blurRadius: 16,
                              ),
                            ],
                            border: Border.all(
                              color: AppColors.borderOcean,
                              width: 1.5,
                            ),
                          ),
                          child: Stack(
                            children: [
                              // Reticle Corners
                              Positioned(
                                top: 0,
                                left: 0,
                                child: Container(
                                  width: 16,
                                  height: 16,
                                  decoration: const BoxDecoration(
                                    border: Border(
                                      top: BorderSide(
                                          color: AppColors.primary, width: 3),
                                      left: BorderSide(
                                          color: AppColors.primary, width: 3),
                                    ),
                                  ),
                                ),
                              ),
                              Positioned(
                                top: 0,
                                right: 0,
                                child: Container(
                                  width: 16,
                                  height: 16,
                                  decoration: const BoxDecoration(
                                    border: Border(
                                      top: BorderSide(
                                          color: AppColors.primary, width: 3),
                                      right: BorderSide(
                                          color: AppColors.primary, width: 3),
                                    ),
                                  ),
                                ),
                              ),
                              Positioned(
                                bottom: 0,
                                left: 0,
                                child: Container(
                                  width: 16,
                                  height: 16,
                                  decoration: const BoxDecoration(
                                    border: Border(
                                      bottom: BorderSide(
                                          color: AppColors.primary, width: 3),
                                      left: BorderSide(
                                          color: AppColors.primary, width: 3),
                                    ),
                                  ),
                                ),
                              ),
                              Positioned(
                                bottom: 0,
                                right: 0,
                                child: Container(
                                  width: 16,
                                  height: 16,
                                  decoration: const BoxDecoration(
                                    border: Border(
                                      bottom: BorderSide(
                                          color: AppColors.primary, width: 3),
                                      right: BorderSide(
                                          color: AppColors.primary, width: 3),
                                    ),
                                  ),
                                ),
                              ),
                              // QR Pattern Representation
                              Center(
                                child: Column(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    const Icon(
                                      Icons.qr_code_2,
                                      size: 140,
                                      color: Color(0xFF1A1C1A),
                                    ),
                                    LocalizedText(
                                      _currentSubOrder.id,
                                      style: const TextStyle(
                                        fontFamily: 'monospace',
                                        fontSize: 10,
                                        fontWeight: FontWeight.bold,
                                        color: AppColors.secondary,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 14),
                        LocalizedText(
                          'Đưa mã QR cho đối tác quét xác nhận check-in',
                          style: AppTypography.labelMd(
                            color: AppColors.onSurface,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        const SizedBox(height: 4),
                        LocalizedText(
                          'Mã bảo mật: ${_currentSubOrder.qrSecret}',
                          style: AppTypography.bodySm(
                            color: AppColors.outline,
                          ).copyWith(fontSize: 10),
                          textAlign: TextAlign.center,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // SUPPORT NOTICE
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: AppShapes.radiusDefault,
              ),
              child: Row(
                children: [
                  const Icon(Icons.headset_mic_outlined,
                      size: 20, color: AppColors.secondary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: LocalizedText(
                      'Cần hỗ trợ tại bãi biển? Gọi hotline điều phối Danang Ocean: 1900 6868',
                      style: AppTypography.bodySm(
                        color: AppColors.onSurfaceVariant,
                      ).copyWith(fontSize: 11),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }

  Widget _buildMetaBox(IconData icon, String label, String value) {
    return Container(
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLow,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 12, color: AppColors.secondary),
              const SizedBox(width: 4),
              Expanded(
                child: LocalizedText(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: AppTypography.bodySm(
                    color: AppColors.onSurfaceVariant,
                  ).copyWith(fontSize: 10),
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          LocalizedText(
            value,
            style: AppTypography.labelMd(
              color: AppColors.onSurface,
              fontWeight: FontWeight.w700,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }
}
