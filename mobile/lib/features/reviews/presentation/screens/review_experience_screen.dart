import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';

class ReviewExperienceScreen extends StatefulWidget {
  final SubOrderModel subOrder;

  const ReviewExperienceScreen({
    super.key,
    required this.subOrder,
  });

  @override
  State<ReviewExperienceScreen> createState() => _ReviewExperienceScreenState();
}

class _ReviewExperienceScreenState extends State<ReviewExperienceScreen> {
  int _rating = 5;
  final TextEditingController _commentController = TextEditingController();
  final List<String> _photos = [];

  final List<String> _ratingLabels = [
    '',
    'Chưa hài lòng',
    'Tạm được',
    'Khá ổn',
    'Rất tốt và an toàn',
    'Tuyệt vời trên cả mong đợi!',
  ];

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  void _submitReview() {
    if (_commentController.text.trim().isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng chia sẻ cảm nhận của bạn.'),
          backgroundColor: AppColors.primary,
        ),
      );
      return;
    }

    final newReview = ReviewModel(
      id: 'rev-${DateTime.now().millisecondsSinceEpoch}',
      subOrderId: widget.subOrder.id,
      customerId: MockDatabaseData.currentUser.id,
      customerName: MockDatabaseData.currentUser.fullName,
      customerAvatar: MockDatabaseData.currentUser.avatarUrl,
      vendorId: widget.subOrder.vendorId,
      serviceId: widget.subOrder.serviceId,
      rating: _rating,
      comment: _commentController.text.trim(),
      images: _photos,
      createdAt: DateTime.now(),
    );

    MockDatabaseData.reviews.insert(0, newReview);

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(
        content: Text('Cảm ơn bạn đã gửi đánh giá trải nghiệm biển!'),
        backgroundColor: AppColors.secondary,
      ),
    );

    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    final sub = widget.subOrder;

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
          'Đánh giá trải nghiệm',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // SERVICE HEADER
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceSm),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                boxShadow: AppShapes.shadowLevel1,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Row(
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(10),
                    child: SizedBox(
                      width: 64,
                      height: 64,
                      child: Image.network(
                        sub.serviceImageUrl.isNotEmpty
                            ? sub.serviceImageUrl
                            : '',
                        fit: BoxFit.cover,
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          sub.vendorName,
                          style: AppTypography.labelSm(
                            color: AppColors.secondary,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        Text(
                          sub.serviceName,
                          style: AppTypography.headlineSm(
                            color: AppColors.onSurface,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          'Hoàn thành chuyến đi • #${sub.id}',
                          style: AppTypography.bodySm(
                            color: AppColors.outline,
                          ).copyWith(fontSize: 11),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // STAR RATING SECTION (CORAL STARS ONLY, NO YELLOW)
            Container(
              padding: const EdgeInsets.all(AppShapes.spaceMd),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: Column(
                children: [
                  Text(
                    'Bạn thấy trải nghiệm biển thế nào?',
                    style: AppTypography.labelLg(color: AppColors.onSurface),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(5, (index) {
                      final starNum = index + 1;
                      return IconButton(
                        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
                        constraints: const BoxConstraints(),
                        iconSize: 32,
                        icon: Icon(
                          starNum <= _rating ? Icons.star : Icons.star_border,
                          color: AppColors.starRating, // Coral #AA3524, NO YELLOW
                        ),
                        onPressed: () {
                          setState(() => _rating = starNum);
                        },
                      );
                    }),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _ratingLabels[_rating],
                    style: AppTypography.labelMd(
                      color: AppColors.primary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // COMMENT FIELD
            Text(
              'Cảm nhận thực tế của bạn',
              style: AppTypography.labelLg(color: AppColors.onSurface),
            ),
            const SizedBox(height: 6),
            Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusDefault,
                border: Border.all(color: AppColors.borderSubtle),
              ),
              child: TextField(
                controller: _commentController,
                maxLines: 4,
                decoration: InputDecoration(
                  hintText:
                      'Chia sẻ về chất lượng ván SUP, kính lặn, hướng dẫn viên đồng hành, độ trong của nước biển...',
                  hintStyle: AppTypography.bodySm(color: AppColors.outline),
                  contentPadding: const EdgeInsets.all(12),
                  border: InputBorder.none,
                ),
              ),
            ),
            const SizedBox(height: 16),

            // PHOTO ATTACHMENTS
            Text(
              'Hình ảnh chuyến đi (tùy chọn)',
              style: AppTypography.labelLg(color: AppColors.onSurface),
            ),
            const SizedBox(height: 6),
            Row(
              children: [
                InkWell(
                  onTap: () {
                    setState(() {
                      _photos.add(
                        'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=500&auto=format&fit=crop&q=80',
                      );
                    });
                  },
                  borderRadius: BorderRadius.circular(10),
                  child: Container(
                    width: 72,
                    height: 72,
                    decoration: BoxDecoration(
                      color: AppColors.secondaryContainer.withValues(alpha: 0.3),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.add_a_photo,
                            size: 22, color: AppColors.secondary),
                        const SizedBox(height: 4),
                        Text(
                          'Thêm ảnh',
                          style: AppTypography.labelSm(
                            color: AppColors.secondary,
                          ).copyWith(fontSize: 10),
                        ),
                      ],
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                ..._photos.map((p) {
                  return Stack(
                    children: [
                      Container(
                        margin: const EdgeInsets.only(right: 8),
                        width: 72,
                        height: 72,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(10),
                          image: DecorationImage(
                            image: NetworkImage(p),
                            fit: BoxFit.cover,
                          ),
                        ),
                      ),
                      Positioned(
                        top: 2,
                        right: 10,
                        child: InkWell(
                          onTap: () => setState(() => _photos.remove(p)),
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
              label: 'Gửi đánh giá trải nghiệm',
              variant: AppButtonVariant.primary,
              width: double.infinity,
              onPressed: _submitReview,
            ),
          ],
        ),
      ),
    );
  }
}
