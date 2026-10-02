import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../data/mock_home_data.dart';
import '../../domain/models/home_models.dart';

class WeatherOceanSection extends StatefulWidget {
  const WeatherOceanSection({super.key});

  @override
  State<WeatherOceanSection> createState() => _WeatherOceanSectionState();
}

class _WeatherOceanSectionState extends State<WeatherOceanSection> {
  int _selectedLocation = 0;

  @override
  Widget build(BuildContext context) {
    final WeatherOceanData current = _selectedLocation == 0
        ? MockHomeData.myKheWeather
        : MockHomeData.sonTraWeather;

    return Container(
      padding: const EdgeInsets.all(AppShapes.spaceMd),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusLg,
        boxShadow: AppShapes.shadowLevel1,
        border: Border.all(
          color: AppColors.borderSubtle.withValues(alpha: 0.6),
          width: 0.8,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header Row
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    const Icon(
                      Icons.waves,
                      color: AppColors.secondary,
                      size: 22,
                    ),
                    const SizedBox(width: 8),
                    Flexible(
                      child: Text(
                        'Điều kiện biển & Thời tiết',
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.headlineSm(
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
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: AppColors.secondaryContainer,
                  borderRadius: AppShapes.radiusFull,
                ),
                child: Text(
                  current.updatedAt,
                  style: AppTypography.labelSm(
                    color: AppColors.onSecondaryContainer,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Location Switcher Tabs
          Row(
            children: [
              Expanded(
                child: _buildLocationTab(
                  index: 0,
                  label: 'Bãi biển Mỹ Khê',
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildLocationTab(
                  index: 1,
                  label: 'Bán đảo Sơn Trà',
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),

          // 3 Weather Metric Cards
          Row(
            children: [
              Expanded(
                child: _buildMetricCard(
                  icon: Icons.air,
                  value: current.windSpeed,
                  label: current.windDesc,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildMetricCard(
                  icon: Icons.tsunami,
                  value: current.waveHeight,
                  label: current.waveDesc,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _buildMetricCard(
                  icon: Icons.water_drop_outlined,
                  value: current.rainLevel,
                  label: current.rainDesc,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Safety Advisory Notice
          Container(
            padding: const EdgeInsets.all(AppShapes.spaceSm),
            decoration: BoxDecoration(
              color: AppColors.secondaryContainer.withValues(alpha: 0.25),
              borderRadius: AppShapes.radiusDefault,
            ),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(
                  Icons.warning_amber_rounded,
                  color: AppColors.secondary,
                  size: 20,
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Khuyến nghị an toàn',
                        style: AppTypography.labelMd(
                          color: AppColors.onSecondaryFixedVariant,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        current.safetyTip,
                        style: AppTypography.bodySm(
                          color: AppColors.onSecondaryFixedVariant,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 8),

          // Footer info
          Text(
            'Cập nhật 15 phút trước',
            style: AppTypography.labelSm(
              color: AppColors.outline,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLocationTab({required int index, required String label}) {
    final isSelected = _selectedLocation == index;
    return InkWell(
      onTap: () => setState(() => _selectedLocation = index),
      borderRadius: AppShapes.radiusFull,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
        decoration: BoxDecoration(
          color: isSelected ? AppColors.secondary : AppColors.surfaceContainer,
          borderRadius: AppShapes.radiusFull,
        ),
        child: Text(
          label,
          style: AppTypography.labelSm(
            color: isSelected ? AppColors.onSecondary : AppColors.onSurface,
            fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
          ),
        ),
      ),
    );
  }

  Widget _buildMetricCard({
    required IconData icon,
    required String value,
    required String label,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 6),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLow,
        borderRadius: AppShapes.radiusDefault,
      ),
      child: Column(
        children: [
          Icon(icon, size: 20, color: AppColors.secondary),
          const SizedBox(height: 4),
          Text(
            value,
            style: AppTypography.headlineSm(
              color: AppColors.onSurface,
              fontWeight: FontWeight.w700,
            ).copyWith(fontSize: 16),
          ),
          const SizedBox(height: 2),
          Text(
            label,
            style: AppTypography.labelSm(
              color: AppColors.onSurfaceVariant,
            ),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }
}
