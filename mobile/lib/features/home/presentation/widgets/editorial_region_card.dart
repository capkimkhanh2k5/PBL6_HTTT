import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../domain/models/home_models.dart';

class EditorialRegionCard extends StatelessWidget {
  final BeachRegionItem item;
  final double height;
  final VoidCallback? onTap;

  const EditorialRegionCard({
    super.key,
    required this.item,
    this.height = 200.0,
    this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: height,
      decoration: const BoxDecoration(
        borderRadius: AppShapes.radiusLg,
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        fit: StackFit.expand,
        children: [
          // Image
          Image.network(
            item.imageUrl,
            fit: BoxFit.cover,
            errorBuilder: (context, error, stackTrace) {
              return Container(
                color: AppColors.secondary.withValues(alpha: 0.6),
                child: const Center(
                  child: Icon(Icons.beach_access, color: Colors.white, size: 48),
                ),
              );
            },
          ),
          // Dark Gradient Overlay
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                begin: Alignment.topCenter,
                end: Alignment.bottomCenter,
                colors: [
                  Colors.transparent,
                  AppColors.inverseSurface.withValues(alpha: 0.30),
                  AppColors.inverseSurface.withValues(alpha: 0.90),
                ],
                stops: const [0.0, 0.45, 1.0],
              ),
            ),
          ),
          // Content
          Padding(
            padding: const EdgeInsets.all(AppShapes.spaceMd),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.end,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Category Tag
                Text(
                  item.tag.toUpperCase(),
                  style: AppTypography.labelSm(
                    color: AppColors.secondaryFixed,
                    fontWeight: FontWeight.w700,
                  ).copyWith(
                    letterSpacing: 0.8,
                  ),
                ),
                const SizedBox(height: 2),
                // Title
                Text(
                  item.title,
                  style: AppTypography.headlineSm(
                    color: AppColors.inverseOnSurface,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 2),
                // Description
                Text(
                  item.description,
                  style: AppTypography.bodySm(
                    color: AppColors.surfaceVariant,
                  ),
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 6),
                // Link button
                InkWell(
                  onTap: onTap,
                  borderRadius: BorderRadius.circular(4),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        'Khám phá ngay',
                        style: AppTypography.labelMd(
                          color: AppColors.secondaryFixed,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(width: 4),
                      const Icon(
                        Icons.arrow_forward,
                        size: 16,
                        color: AppColors.secondaryFixed,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
