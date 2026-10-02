import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';
import '../../../experience_detail/presentation/screens/experience_detail_screen.dart';

class WishlistScreen extends StatefulWidget {
  final VoidCallback? onExplore;

  const WishlistScreen({super.key, this.onExplore});

  @override
  State<WishlistScreen> createState() => _WishlistScreenState();
}

class _WishlistScreenState extends State<WishlistScreen> {
  late List<ServiceModel> _wishlist;

  @override
  void initState() {
    super.initState();
    _wishlist =
        MockDatabaseData.services.where((s) => s.isFavorite).toList();
  }

  void _removeFromWishlist(ServiceModel item) {
    setState(() {
      _wishlist.removeWhere((s) => s.id == item.id);
    });
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('Đã bỏ ${item.name} khỏi danh sách yêu thích.'),
        duration: const Duration(seconds: 1),
      ),
    );
  }

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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        automaticallyImplyLeading: false,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Yêu thích (${_wishlist.length})',
              style: AppTypography.headlineSm(color: AppColors.onSurface),
            ),
            Text(
              'Trải nghiệm biển bạn quan tâm & lưu lại',
              style: AppTypography.bodySm(color: AppColors.onSurfaceVariant)
                  .copyWith(fontSize: 11),
            ),
          ],
        ),
      ),
      body: _wishlist.isEmpty
          ? _buildEmptyState()
          : ListView.separated(
              padding: const EdgeInsets.all(AppShapes.gutterMobile),
              itemCount: _wishlist.length,
              separatorBuilder: (_, unused) =>
                  const SizedBox(height: AppShapes.spaceMd),
              itemBuilder: (context, index) {
                final item = _wishlist[index];
                return _buildWishlistCard(item);
              },
            ),
    );
  }

  Widget _buildWishlistCard(ServiceModel item) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusDefault,
        boxShadow: AppShapes.shadowLevel1,
        border: Border.all(color: AppColors.borderSubtle),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Image with Favorite active button
          AspectRatio(
            aspectRatio: 16 / 9,
            child: Stack(
              fit: StackFit.expand,
              children: [
                Image.network(
                  item.imageUrls.isNotEmpty
                      ? item.imageUrls.first
                      : 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=600&auto=format&fit=crop&q=80',
                  fit: BoxFit.cover,
                ),
                // Vendor pill
                Positioned(
                  top: 10,
                  left: 10,
                  child: Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: AppColors.secondary.withValues(alpha: 0.9),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.verified,
                            size: 12, color: AppColors.onSecondary),
                        const SizedBox(width: 4),
                        Text(
                          item.vendorName,
                          style: AppTypography.labelSm(
                            color: AppColors.onSecondary,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                // Remove favorite button
                Positioned(
                  top: 8,
                  right: 8,
                  child: CircleAvatar(
                    backgroundColor: Colors.white.withValues(alpha: 0.85),
                    radius: 18,
                    child: IconButton(
                      icon: const Icon(Icons.favorite,
                          color: AppColors.primary, size: 20),
                      splashRadius: 18,
                      onPressed: () => _removeFromWishlist(item),
                    ),
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(AppShapes.spaceSm),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    const Icon(Icons.star,
                        size: 15, color: AppColors.starRating),
                    const SizedBox(width: 4),
                    Text(
                      item.avgRating.toStringAsFixed(1),
                      style: AppTypography.labelMd(
                        color: AppColors.onSurface,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(width: 4),
                    Text('(${item.ratingCount})',
                        style: AppTypography.bodySm(color: AppColors.outline)),
                    const Spacer(),
                    Text(
                      '${item.durationMinutes} phút',
                      style: AppTypography.labelSm(color: AppColors.secondary),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                Text(
                  item.name,
                  style: AppTypography.headlineSm(color: AppColors.onSurface),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 4),
                Row(
                  children: [
                    const Icon(Icons.location_on,
                        size: 14, color: AppColors.tertiary),
                    const SizedBox(width: 4),
                    Expanded(
                      child: Text(
                        item.locationName,
                        style: AppTypography.bodySm(
                            color: AppColors.onSurfaceVariant),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Giá trọn gói',
                              style: AppTypography.labelSm(
                                  color: AppColors.onSurfaceVariant)),
                          Text(
                            _formatPrice(item.price),
                            style: AppTypography.headlineSm(
                                color: AppColors.primary),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    AppPillButton(
                      label: 'Xem & Đặt chỗ',
                      variant: AppButtonVariant.primary,
                      onPressed: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) =>
                                ExperienceDetailScreen(service: item),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEmptyState() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 72,
              height: 72,
              decoration: const BoxDecoration(
                color: AppColors.surfaceContainerHigh,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.favorite_border,
                  size: 36, color: AppColors.primary),
            ),
            const SizedBox(height: 16),
            Text(
              'Chưa có trải nghiệm yêu thích',
              style: AppTypography.headlineSm(color: AppColors.onSurface),
            ),
            const SizedBox(height: 8),
            Text(
              'Khám phá các hoạt động chèo SUP, lặn biển, cano tại Đà Nẵng và nhấn biểu tượng trái tim để lưu lại.',
              textAlign: TextAlign.center,
              style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
            ),
            const SizedBox(height: 20),
            if (widget.onExplore != null)
              AppPillButton(
                label: 'Khám phá ngay',
                variant: AppButtonVariant.primary,
                onPressed: widget.onExplore,
              ),
          ],
        ),
      ),
    );
  }
}
