import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';

class CategoryTabsWidget extends StatefulWidget {
  final ValueChanged<int>? onCategoryChanged;

  const CategoryTabsWidget({
    super.key,
    this.onCategoryChanged,
  });

  @override
  State<CategoryTabsWidget> createState() => _CategoryTabsWidgetState();
}

class _CategoryTabsWidgetState extends State<CategoryTabsWidget> {
  int _selectedIndex = 0;

  final List<Map<String, dynamic>> _categories = const [
    {'name': 'Chèo SUP', 'icon': Icons.surfing},
    {'name': 'Cano lướt sóng', 'icon': Icons.speed},
    {'name': 'Lặn san hô', 'icon': Icons.scuba_diving},
    {'name': 'Chèo kayak', 'icon': Icons.kayaking},
    {'name': 'Tour biển đảo', 'icon': Icons.sailing},
  ];

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Section Title
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  'Khám phá theo sở thích',
                  overflow: TextOverflow.ellipsis,
                  style: AppTypography.headlineSm(
                    color: AppColors.onSurface,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              InkWell(
                onTap: () {},
                borderRadius: BorderRadius.circular(8),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                  child: Text(
                    'Xem tất cả',
                    style: AppTypography.labelMd(
                      color: AppColors.secondary,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        // Horizontal Scrollable Category Pills
        SizedBox(
          height: 44,
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            scrollDirection: Axis.horizontal,
            itemCount: _categories.length,
            separatorBuilder: (context, index) => const SizedBox(width: 8),
            itemBuilder: (context, index) {
              final isSelected = _selectedIndex == index;
              final item = _categories[index];

              return InkWell(
                onTap: () {
                  setState(() => _selectedIndex = index);
                  widget.onCategoryChanged?.call(index);
                },
                borderRadius: AppShapes.radiusFull,
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 200),
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  decoration: BoxDecoration(
                    color: isSelected
                        ? AppColors.secondary
                        : AppColors.surfaceContainerHigh,
                    borderRadius: AppShapes.radiusFull,
                    boxShadow: isSelected
                        ? [
                            BoxShadow(
                              color: AppColors.secondary.withValues(alpha: 0.25),
                              blurRadius: 8,
                              offset: const Offset(0, 3),
                            ),
                          ]
                        : null,
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Icon(
                        item['icon'] as IconData,
                        size: 18,
                        color: isSelected
                            ? AppColors.onSecondary
                            : AppColors.onSurface,
                      ),
                      const SizedBox(width: 8),
                      Text(
                        item['name'] as String,
                        style: AppTypography.labelMd(
                          color: isSelected
                              ? AppColors.onSecondary
                              : AppColors.onSurface,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }
}
