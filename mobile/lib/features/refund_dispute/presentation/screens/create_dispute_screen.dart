import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';

class CreateDisputeScreen extends StatefulWidget {
  final SubOrderModel? subOrder;

  const CreateDisputeScreen({super.key, this.subOrder});

  @override
  State<CreateDisputeScreen> createState() => _CreateDisputeScreenState();
}

class _CreateDisputeScreenState extends State<CreateDisputeScreen> {
  late SubOrderModel _selectedSubOrder;
  String _selectedCategory = 'Chất lượng không đúng cam kết';
  final TextEditingController _descController = TextEditingController();
  final List<String> _attachedImages = [];

  final List<String> _categories = [
    'Chất lượng không đúng cam kết',
    'Thiếu trang bị an toàn / phao cứu sinh',
    'Tự ý hủy hoặc dời giờ xuất phát quá 60p',
    'Thái độ nhân viên hoặc hướng dẫn viên',
    'Sự cố phát sinh ngoài thỏa thuận',
  ];

  @override
  void initState() {
    super.initState();
    _selectedSubOrder = widget.subOrder ??
        MockDatabaseData.sampleOrderPaid.subOrders.first;
  }

  @override
  void dispose() {
    _descController.dispose();
    super.dispose();
  }

  void _submitDispute() {
    if (_descController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng mô tả chi tiết sự việc khiếu nại.'),
          backgroundColor: AppColors.primary,
        ),
      );
      return;
    }

    final newDispute = DisputeModel(
      id: 'DISP-${DateTime.now().millisecondsSinceEpoch % 9000 + 1000}',
      subOrderId: _selectedSubOrder.id,
      subOrderCode: _selectedSubOrder.id,
      serviceName: _selectedSubOrder.serviceName,
      vendorName: _selectedSubOrder.vendorName,
      raisedBy: MockDatabaseData.currentUser.id,
      category: _selectedCategory,
      description: _descController.text.trim(),
      status: DisputeStatus.open,
      createdAt: DateTime.now(),
      attachmentUrls: _attachedImages,
    );

    MockDatabaseData.disputes.insert(0, newDispute);

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Đã gửi khiếu nại thành công! Ban quản trị DANASEA sẽ phản hồi trong 24h.'),
        backgroundColor: AppColors.secondary,
      ),
    );

    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        title: Text(
          'Gửi khiếu nại dịch vụ',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // NOTICE
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.secondaryContainer.withValues(alpha: 0.3),
                borderRadius: AppShapes.radiusDefault,
              ),
              child: Row(
                children: [
                  const Icon(Icons.policy_outlined,
                      size: 20, color: AppColors.secondary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'Hồ sơ khiếu nại được gửi trực tiếp đến Bộ phận Quản trị & Đối soát DANASEA để xác minh độc lập với nhà cung cấp.',
                      style: AppTypography.bodySm(
                        color: AppColors.onSecondaryContainer,
                      ).copyWith(fontSize: 11),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // 1. SELECT SERVICE
            Text('1. Dịch vụ cần khiếu nại',
                style: AppTypography.labelLg(color: AppColors.onSurface)),
            const SizedBox(height: 6),
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Row(
                children: [
                  const Icon(Icons.sailing, size: 20, color: AppColors.secondary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          _selectedSubOrder.serviceName,
                          style: AppTypography.labelMd(
                            color: AppColors.onSurface,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          '${_selectedSubOrder.vendorName} • Mã đơn: #${_selectedSubOrder.id}',
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

            // 2. CATEGORY DROPDOWN
            Text('2. Phân loại vấn đề',
                style: AppTypography.labelLg(color: AppColors.onSurface)),
            const SizedBox(height: 6),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: _selectedCategory,
                  isExpanded: true,
                  items: _categories.map((cat) {
                    return DropdownMenuItem(
                      value: cat,
                      child: Text(cat, style: AppTypography.bodySm(color: AppColors.onSurface)),
                    );
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _selectedCategory = val);
                  },
                ),
              ),
            ),
            const SizedBox(height: 16),

            // 3. DESCRIPTION
            Text('3. Mô tả chi tiết sự việc',
                style: AppTypography.labelLg(color: AppColors.onSurface)),
            const SizedBox(height: 6),
            Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: TextField(
                controller: _descController,
                maxLines: 4,
                decoration: InputDecoration(
                  hintText:
                      'Trình bày thời gian, diễn biến sự việc và yêu cầu xử lý...',
                  hintStyle: AppTypography.bodySm(color: AppColors.outline),
                  contentPadding: const EdgeInsets.all(12),
                  border: InputBorder.none,
                ),
              ),
            ),
            const SizedBox(height: 16),

            // 4. ATTACHMENTS
            Text('4. Hình ảnh / Video minh chứng',
                style: AppTypography.labelLg(color: AppColors.onSurface)),
            const SizedBox(height: 6),
            Row(
              children: [
                InkWell(
                  onTap: () {
                    setState(() {
                      _attachedImages.add(
                        'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=500&auto=format&fit=crop&q=80',
                      );
                    });
                  },
                  borderRadius: BorderRadius.circular(10),
                  child: Container(
                    width: 72,
                    height: 72,
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(
                        color: AppColors.secondary,
                        style: BorderStyle.solid,
                      ),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.add_a_photo_outlined,
                            size: 22, color: AppColors.secondary),
                        const SizedBox(height: 4),
                        Text('Thêm ảnh',
                            style: AppTypography.labelSm(
                                color: AppColors.secondary).copyWith(fontSize: 10)),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ..._attachedImages.map((img) {
                  return Stack(
                    children: [
                      Container(
                        margin: const EdgeInsets.only(right: 8),
                        width: 72,
                        height: 72,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(10),
                          image: DecorationImage(
                            image: NetworkImage(img),
                            fit: BoxFit.cover,
                          ),
                        ),
                      ),
                      Positioned(
                        top: 2,
                        right: 10,
                        child: InkWell(
                          onTap: () {
                            setState(() => _attachedImages.remove(img));
                          },
                          child: Container(
                            padding: const EdgeInsets.all(2),
                            decoration: const BoxDecoration(
                              color: Colors.black54,
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(Icons.close,
                                size: 12, color: Colors.white),
                          ),
                        ),
                      ),
                    ],
                  );
                }),
              ],
            ),
            const SizedBox(height: 24),

            // SUBMIT BUTTON
            AppPillButton(
              label: 'Gửi hồ sơ khiếu nại',
              variant: AppButtonVariant.primary,
              width: double.infinity,
              onPressed: _submitDispute,
            ),
          ],
        ),
      ),
    );
  }
}
