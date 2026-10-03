import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import 'create_dispute_screen.dart';

class MyDisputesScreen extends StatefulWidget {
  const MyDisputesScreen({super.key});

  @override
  State<MyDisputesScreen> createState() => _MyDisputesScreenState();
}

class _MyDisputesScreenState extends State<MyDisputesScreen> {
  int _selectedFilterIndex = 0;

  final List<String> _filters = [
    'Tất cả',
    'Đang mở',
    'Đang thẩm định',
    'Đã giải quyết',
    'Bị từ chối',
  ];

  List<DisputeModel> get _filteredDisputes {
    final all = MockDatabaseData.disputes;
    if (_selectedFilterIndex == 0) return all;
    if (_selectedFilterIndex == 1) {
      return all.where((d) => d.status == DisputeStatus.open).toList();
    }
    if (_selectedFilterIndex == 2) {
      return all.where((d) => d.status == DisputeStatus.inReview).toList();
    }
    if (_selectedFilterIndex == 3) {
      return all.where((d) => d.status == DisputeStatus.resolved).toList();
    }
    return all.where((d) => d.status == DisputeStatus.rejected).toList();
  }

  String _getStatusName(DisputeStatus s) {
    switch (s) {
      case DisputeStatus.open:
        return 'Đang mở';
      case DisputeStatus.inReview:
        return 'Đang thẩm định';
      case DisputeStatus.resolved:
        return 'Đã giải quyết';
      case DisputeStatus.rejected:
        return 'Bị từ chối';
    }
  }

  Color _getStatusColor(DisputeStatus s) {
    switch (s) {
      case DisputeStatus.resolved:
        return AppColors.secondary;
      case DisputeStatus.inReview:
        return AppColors.secondaryTeal;
      case DisputeStatus.open:
        return AppColors.primary;
      case DisputeStatus.rejected:
        return AppColors.error;
    }
  }

  @override
  Widget build(BuildContext context) {
    final disputes = _filteredDisputes;

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
          'Khiếu nại của tôi',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.add_circle_outline,
                color: AppColors.secondary),
            tooltip: tr(context, 'Gửi khiếu nại'),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => const CreateDisputeScreen(),
                ),
              );
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // BANNER
          Padding(
            padding: const EdgeInsets.symmetric(
                horizontal: AppShapes.gutterMobile, vertical: 4),
            child: Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.secondaryContainer.withValues(alpha: 0.35),
                borderRadius: AppShapes.radiusDefault,
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.shield_outlined,
                      size: 20, color: AppColors.secondary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        LocalizedText(
                          'Bảo vệ quyền lợi hành trình biển',
                          style: AppTypography.labelMd(
                            color: AppColors.onSecondaryContainer,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        LocalizedText(
                          'Tất cả khiếu nại được tiếp nhận và giải quyết căn cứ theo chứng từ và thỏa thuận bảo vệ khách hàng của DANASEA.',
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
          ),

          // FILTER TABS
          Container(
            height: 48,
            padding: const EdgeInsets.symmetric(vertical: 6),
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(
                  horizontal: AppShapes.gutterMobile),
              itemCount: _filters.length,
              separatorBuilder: (_, unused) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final isSel = _selectedFilterIndex == index;
                return InkWell(
                  onTap: () => setState(() => _selectedFilterIndex = index),
                  borderRadius: BorderRadius.circular(18),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 14, vertical: 6),
                    decoration: BoxDecoration(
                      color: isSel
                          ? AppColors.secondary
                          : AppColors.surfaceContainerHigh,
                      borderRadius: BorderRadius.circular(18),
                    ),
                    child: Center(
                      child: LocalizedText(
                        _filters[index],
                        style: AppTypography.labelSm(
                          color: isSel ? Colors.white : AppColors.onSurfaceVariant,
                          fontWeight: isSel ? FontWeight.w700 : FontWeight.w500,
                        ),
                      ),
                    ),
                  ),
                );
              },
            ),
          ),

          // DISPUTES LIST
          Expanded(
            child: disputes.isEmpty
                ? Center(
                    child: LocalizedText(
                      'Không có khiếu nại nào trong mục này',
                      style: AppTypography.bodySm(
                          color: AppColors.onSurfaceVariant),
                    ),
                  )
                : ListView.separated(
                    padding: const EdgeInsets.all(AppShapes.gutterMobile),
                    itemCount: disputes.length,
                    separatorBuilder: (_, unused) =>
                        const SizedBox(height: AppShapes.spaceMd),
                    itemBuilder: (context, index) {
                      final disp = disputes[index];
                      final statusColor = _getStatusColor(disp.status);
                      return Container(
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
                                  child: Column(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      LocalizedText(
                                        '#${disp.id}',
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                        style: AppTypography.headlineSm(
                                          color: AppColors.secondary,
                                        ),
                                      ),
                                      LocalizedText(
                                        '${disp.createdAt.day}/${disp.createdAt.month}/${disp.createdAt.year}',
                                        style: AppTypography.bodySm(
                                                color: AppColors.outline)
                                            .copyWith(fontSize: 11),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 8, vertical: 3),
                                  decoration: BoxDecoration(
                                    color: statusColor.withValues(alpha: 0.15),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: LocalizedText(
                                    _getStatusName(disp.status),
                                    style: AppTypography.labelSm(
                                      color: statusColor,
                                      fontWeight: FontWeight.w700,
                                    ).copyWith(fontSize: 10),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 8),

                            // Linked service card
                            Container(
                              padding: const EdgeInsets.all(8),
                              decoration: BoxDecoration(
                                color: AppColors.surfaceContainerLow,
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Row(
                                children: [
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        LocalizedText(
                                          'Đơn con #${disp.subOrderCode}',
                                          style: AppTypography.labelSm(
                                            color: AppColors.secondary,
                                            fontWeight: FontWeight.w700,
                                          ).copyWith(fontSize: 10),
                                        ),
                                        LocalizedText(
                                          disp.serviceName,
                                          style: AppTypography.labelMd(
                                            color: AppColors.onSurface,
                                            fontWeight: FontWeight.w700,
                                          ),
                                        ),
                                        LocalizedText(
                                          disp.vendorName,
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
                            const SizedBox(height: 8),

                            LocalizedText(
                              'Phân loại: ${disp.category}',
                              style: AppTypography.labelSm(
                                color: AppColors.primary,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const SizedBox(height: 4),
                            LocalizedText(
                              disp.description,
                              style: AppTypography.bodySm(
                                color: AppColors.onSurface,
                              ),
                            ),

                            // Resolution note if present
                            if (disp.resolutionNote != null) ...[
                              const SizedBox(height: 10),
                              Container(
                                padding: const EdgeInsets.all(8),
                                decoration: BoxDecoration(
                                  color: disp.status == DisputeStatus.rejected
                                      ? AppColors.error.withValues(alpha: 0.1)
                                      : AppColors.secondaryFixed
                                          .withValues(alpha: 0.25),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Icon(
                                      disp.status == DisputeStatus.rejected
                                          ? Icons.cancel_outlined
                                          : Icons.verified,
                                      size: 16,
                                      color: disp.status == DisputeStatus.rejected
                                          ? AppColors.error
                                          : AppColors.secondary,
                                    ),
                                    const SizedBox(width: 6),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          LocalizedText(
                                            disp.status == DisputeStatus.rejected
                                                ? 'Lý do từ chối từ Admin:'
                                                : 'Kết luận xử lý từ Admin:',
                                            style: AppTypography.labelSm(
                                              color: disp.status ==
                                                      DisputeStatus.rejected
                                                  ? AppColors.error
                                                  : AppColors.secondary,
                                              fontWeight: FontWeight.w700,
                                            ),
                                          ),
                                          LocalizedText(
                                            disp.resolutionNote!,
                                            style: AppTypography.bodySm(
                                              color: AppColors.onSurface,
                                            ).copyWith(fontSize: 11),
                                          ),
                                        ],
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                            ],
                          ],
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        backgroundColor: AppColors.primary,
        onPressed: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => const CreateDisputeScreen(),
            ),
          );
        },
        icon: const Icon(Icons.add, color: Colors.white),
        label: const LocalizedText('Gửi khiếu nại', style: TextStyle(color: Colors.white)),
      ),
    );
  }
}
