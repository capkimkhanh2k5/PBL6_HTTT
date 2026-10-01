import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/status_badge.dart';
import '../../../../core/widgets/toast_notification.dart';
import '../../../../core/models/vendor_models.dart';
import '../../../../core/data/vendor_mock_database.dart';
import '../../../../core/data/vendor_mock_repositories.dart';

class VendorDocumentsScreen extends StatefulWidget {
  const VendorDocumentsScreen({super.key});

  @override
  State<VendorDocumentsScreen> createState() => _VendorDocumentsScreenState();
}

class _VendorDocumentsScreenState extends State<VendorDocumentsScreen> {
  final VendorDocumentRepository _docRepo = VendorDocumentRepository();
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  // Simulator state for demo / QA preview of the 3 system states
  DocumentReviewStatus _simulatedSafetyCertStatus = DocumentReviewStatus.approved;

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

  void _handleUpload(VendorDocType type, String mockFileName, String mockFileSize) async {
    await _docRepo.uploadOrReplaceDocument(
      docType: type,
      fileName: mockFileName,
      fileSize: mockFileSize,
    );
    if (!mounted) return;
    ToastNotification.show(
      context,
      message: 'Tải lên thành công! Hồ sơ đang ở trạng thái Chờ duyệt bởi Admin DANASEA.',
      type: ToastType.info,
    );
  }

  void _viewDocumentDialog(String title, String fileName) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppShapes.rXl)),
        title: Row(
          children: [
            const Icon(Icons.picture_as_pdf, color: AppColors.primary, size: 24),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                title,
                style: AppTypography.headlineSm.copyWith(color: AppColors.onSurface),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Tệp tin: $fileName',
              style: AppTypography.labelLg.copyWith(color: AppColors.secondary),
            ),
            const SizedBox(height: 12),
            Container(
              width: double.infinity,
              height: 140,
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: BorderRadius.circular(AppShapes.rMd),
                border: Border.all(color: AppColors.outlineVariant),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.verified_user, color: AppColors.secondary, size: 40),
                  const SizedBox(height: 8),
                  Text(
                    'Bản sao hợp thức hóa điện tử',
                    style: AppTypography.labelMd.copyWith(color: AppColors.onSurfaceVariant),
                  ),
                  Text(
                    'Được lưu trữ bảo mật trên DANASEA Cloud',
                    style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Đóng'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final docs = _db.vendorDocuments;
    final businessLicense = docs.firstWhere(
      (d) => d.docType == VendorDocType.businessLicense,
      orElse: () => VendorDocumentModel(
        id: 'vdoc-001',
        vendorId: 'vnd-001',
        docType: VendorDocType.businessLicense,
        fileUrl: 'GPKD_DanangOceanClub_2023.pdf',
        status: DocumentReviewStatus.approved,
        fileName: 'GPKD_DanangOceanClub_2023.pdf',
        fileSize: '1.8 MB',
        uploadedAt: DateTime(2023, 3, 15),
      ),
    );

    final safetyCert = docs.firstWhere(
      (d) => d.docType == VendorDocType.safetyCert,
      orElse: () => VendorDocumentModel(
        id: 'vdoc-002',
        vendorId: 'vnd-001',
        docType: VendorDocType.safetyCert,
        fileUrl: 'ChungNhan_AnToanBien_SHT_2024.pdf',
        status: _simulatedSafetyCertStatus,
        fileName: 'ChungNhan_AnToanBien_SHT_2024.pdf',
        fileSize: '2.4 MB',
        uploadedAt: DateTime(2024, 1, 12),
      ),
    );

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Giấy tờ đối tác'),
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
            // Top Hero Card
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: AppColors.secondary,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(
                    color: Color(0x1A006874),
                    blurRadius: 10,
                    offset: Offset(0, 4),
                  ),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: Colors.white.withAlpha(40),
                      borderRadius: BorderRadius.circular(AppShapes.rFull),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.verified_user, color: AppColors.secondaryFixed, size: 16),
                        const SizedBox(width: 6),
                        Flexible(
                          child: Text(
                            'HỒ SƠ XÁC THỰC PHÁP LÝ',
                            style: AppTypography.labelSm.copyWith(
                              color: Colors.white,
                              letterSpacing: 0.5,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    'Giấy tờ & Chứng nhận',
                    style: AppTypography.headlineLgMobile.copyWith(
                      color: Colors.white,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Danang Ocean Club cam kết minh bạch pháp lý và an toàn chuẩn mực theo quy chuẩn kiểm duyệt dịch vụ thể thao biển DANASEA.',
                    style: AppTypography.bodySm.copyWith(
                      color: AppColors.surfaceContainerHigh.withAlpha(230),
                      height: 1.5,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Row(
                          children: [
                            Container(
                              width: 8,
                              height: 8,
                              decoration: const BoxDecoration(
                                color: AppColors.secondaryFixedDim,
                                shape: BoxShape.circle,
                              ),
                            ),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                'Trạng thái hồ sơ:',
                                style: AppTypography.labelMd.copyWith(color: Colors.white),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                        decoration: BoxDecoration(
                          color: Colors.white.withAlpha(50),
                          borderRadius: BorderRadius.circular(AppShapes.rFull),
                        ),
                        child: Text(
                          '2 Hạng mục chính',
                          style: AppTypography.labelSm.copyWith(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Item 1: Business License Card
            _buildDocCard(
              title: 'Giấy phép đăng ký kinh doanh',
              subtitle: 'Loại: Giấy tờ pháp lý doanh nghiệp',
              icon: Icons.policy,
              doc: businessLicense,
              onView: () => _viewDocumentDialog(
                'Giấy phép kinh doanh',
                businessLicense.fileName,
              ),
              onUploadNew: () => _handleUpload(
                VendorDocType.businessLicense,
                'GPKD_DanangOceanClub_2024_CapNhat.pdf',
                '2.1 MB',
              ),
            ),
            const SizedBox(height: 16),

            // Item 2: Safety Certification Card
            _buildSafetyCertCard(safetyCert),
            const SizedBox(height: 20),

            // Notice Box
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: BorderRadius.circular(AppShapes.rMd),
                border: Border.all(color: AppColors.outlineVariant),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.info_outline, color: AppColors.secondary, size: 22),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Lưu ý về quy trình kiểm duyệt',
                          style: AppTypography.labelLg.copyWith(color: AppColors.onSurface),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Theo quy định DANASEA, giấy tờ mới tải lên sẽ có trạng thái "Đang chờ duyệt". Chỉ Admin hệ thống mới có quyền phê duyệt giấy tờ đối tác. Chứng chỉ an toàn riêng của từng dịch vụ được quản lý độc lập tại mục chỉnh sửa dịch vụ tương ứng.',
                          style: AppTypography.bodySm.copyWith(
                            color: AppColors.onSurfaceVariant,
                            height: 1.4,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }

  Widget _buildDocCard({
    required String title,
    required String subtitle,
    required IconData icon,
    required VendorDocumentModel doc,
    required VoidCallback onView,
    required VoidCallback onUploadNew,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(AppShapes.rLg),
        boxShadow: const [
          BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: AppColors.secondary.withAlpha(25),
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: AppColors.secondary, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: AppTypography.headlineSm.copyWith(
                        color: AppColors.onSurface,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                    ),
                  ],
                ),
              ),
              StatusBadge(status: doc.status.toBadgeStatus()),
            ],
          ),
          const SizedBox(height: 14),

          // File Capsule
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: BorderRadius.circular(AppShapes.rMd),
            ),
            child: Row(
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: AppColors.primaryContainer.withAlpha(40),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.picture_as_pdf, color: AppColors.primary, size: 20),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        doc.fileName,
                        style: AppTypography.labelLg.copyWith(color: AppColors.onSurface),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${doc.fileSize} • Tải lên: ${doc.uploadedAt.day.toString().padLeft(2, '0')}/${doc.uploadedAt.month.toString().padLeft(2, '0')}/${doc.uploadedAt.year}',
                        style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.cloud_download_outlined, color: AppColors.secondary),
                  onPressed: () {
                    ToastNotification.show(
                      context,
                      message: 'Đang tải xuống ${doc.fileName}...',
                      type: ToastType.success,
                    );
                  },
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // Actions
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: onView,
                  icon: const Icon(Icons.visibility_outlined, size: 18),
                  label: const Text('Xem tài liệu'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: AppColors.secondary,
                    side: const BorderSide(color: AppColors.outlineVariant),
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppShapes.rFull)),
                    padding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: onUploadNew,
                  icon: const Icon(Icons.refresh, size: 18),
                  label: const Text('Tải lại file mới'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.secondaryContainer.withAlpha(90),
                    foregroundColor: AppColors.onSecondaryContainer,
                    elevation: 0,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppShapes.rFull)),
                    padding: const EdgeInsets.symmetric(vertical: 12),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSafetyCertCard(VendorDocumentModel safetyCert) {
    final effectiveStatus = _simulatedSafetyCertStatus;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(AppShapes.rLg),
        boxShadow: const [
          BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: AppColors.primaryContainer.withAlpha(35),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.scuba_diving, color: AppColors.primary, size: 22),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Chứng nhận an toàn biển & Cứu hộ',
                      style: AppTypography.headlineSm.copyWith(
                        color: AppColors.onSurface,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Loại: Chứng nhận tiêu chuẩn thể thao biển',
                      style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                    ),
                  ],
                ),
              ),
              StatusBadge(status: effectiveStatus.toBadgeStatus()),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            'Chứng nhận an toàn thể thao biển và năng lực cứu hộ cứu nạn cấp bởi Ban quản lý bán đảo Sơn Trà và các bãi biển du lịch Đà Nẵng.',
            style: AppTypography.bodySm.copyWith(color: AppColors.tertiary, height: 1.4),
          ),
          const SizedBox(height: 14),

          // File Capsule
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: BorderRadius.circular(AppShapes.rMd),
            ),
            child: Row(
              children: [
                Container(
                  width: 36,
                  height: 36,
                  decoration: BoxDecoration(
                    color: AppColors.primaryContainer.withAlpha(40),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.picture_as_pdf, color: AppColors.primary, size: 20),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        safetyCert.fileName,
                        style: AppTypography.labelLg.copyWith(color: AppColors.onSurface),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                      const SizedBox(height: 2),
                      Text(
                        '${safetyCert.fileSize} • Tải lên: 12/01/2024',
                        style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.visibility_outlined, color: AppColors.secondary),
                  onPressed: () => _viewDocumentDialog(
                    'Chứng nhận an toàn biển',
                    safetyCert.fileName,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),

          // Interactive State Switcher for QA Preview (as in revised HTML)
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainer,
              borderRadius: BorderRadius.circular(AppShapes.rMd),
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
                          const Icon(Icons.tune, size: 16, color: AppColors.onSurfaceVariant),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              'KIỂM TRA 3 TRẠNG THÁI HỆ THỐNG',
                              style: AppTypography.labelSm.copyWith(
                                color: AppColors.onSurfaceVariant,
                                fontWeight: FontWeight.bold,
                              ),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      'Xem trước',
                      style: AppTypography.labelSm.copyWith(color: AppColors.tertiary),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    _buildStatePill(
                      label: 'Đã duyệt',
                      status: DocumentReviewStatus.approved,
                      isActive: effectiveStatus == DocumentReviewStatus.approved,
                    ),
                    const SizedBox(width: 6),
                    _buildStatePill(
                      label: 'Chờ duyệt',
                      status: DocumentReviewStatus.pending,
                      isActive: effectiveStatus == DocumentReviewStatus.pending,
                    ),
                    const SizedBox(width: 6),
                    _buildStatePill(
                      label: 'Từ chối',
                      status: DocumentReviewStatus.rejected,
                      isActive: effectiveStatus == DocumentReviewStatus.rejected,
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  _getStateDescription(effectiveStatus),
                  style: AppTypography.bodySm.copyWith(
                    color: AppColors.tertiary,
                    fontStyle: FontStyle.italic,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatePill({
    required String label,
    required DocumentReviewStatus status,
    required bool isActive,
  }) {
    return Expanded(
      child: GestureDetector(
        onTap: () {
          setState(() {
            _simulatedSafetyCertStatus = status;
          });
        },
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            color: isActive ? AppColors.surfaceContainerLowest : Colors.transparent,
            borderRadius: BorderRadius.circular(AppShapes.rFull),
            boxShadow: isActive
                ? const [
                    BoxShadow(color: Color(0x14000000), blurRadius: 4, offset: Offset(0, 1)),
                  ]
                : null,
          ),
          alignment: Alignment.center,
          child: Text(
            label,
            style: AppTypography.labelSm.copyWith(
              color: isActive ? AppColors.secondary : AppColors.tertiary,
              fontWeight: isActive ? FontWeight.bold : FontWeight.normal,
            ),
          ),
        ),
      ),
    );
  }

  String _getStateDescription(DocumentReviewStatus status) {
    switch (status) {
      case DocumentReviewStatus.approved:
        return '• Giấy tờ đã được Admin phê duyệt. Dịch vụ gắn chứng chỉ này được phép mở bán.';
      case DocumentReviewStatus.pending:
        return '• Hồ sơ đang trong hàng đợi thẩm định của ban an toàn thể thao biển.';
      case DocumentReviewStatus.rejected:
        return '• Giấy tờ bị từ chối do mờ hoặc hết hạn. Vui lòng tải lại bản công chứng mới.';
    }
  }
}
