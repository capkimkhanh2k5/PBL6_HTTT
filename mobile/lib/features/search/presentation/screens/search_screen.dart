import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../experience_detail/presentation/screens/experience_detail_screen.dart';

class SearchScreen extends StatefulWidget {
  final String? initialQuery;
  final String? initialCategory;

  const SearchScreen({
    super.key,
    this.initialQuery,
    this.initialCategory,
  });

  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  late TextEditingController _searchController;
  bool _isMapView = false;
  String? _selectedLocation;
  String? _selectedCategory;
  String? _selectedTimeOfDay;
  int? _maxPrice;

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController(text: widget.initialQuery ?? '');
    _selectedCategory = widget.initialCategory;
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  int get _activeFilterCount {
    int count = 0;
    if (_selectedLocation != null) count++;
    if (_selectedCategory != null) count++;
    if (_selectedTimeOfDay != null) count++;
    if (_maxPrice != null) count++;
    return count;
  }

  List<ServiceModel> get _filteredServices {
    return MockDatabaseData.services.where((srv) {
      final query = _searchController.text.trim().toLowerCase();
      if (query.isNotEmpty) {
        final matchesName = srv.name.toLowerCase().contains(query);
        final matchesVendor = srv.vendorName.toLowerCase().contains(query);
        final matchesLoc = srv.locationName.toLowerCase().contains(query);
        if (!matchesName && !matchesVendor && !matchesLoc) return false;
      }

      if (_selectedLocation != null &&
          !srv.locationName.contains(_selectedLocation!)) {
        return false;
      }

      if (_selectedCategory != null &&
          srv.categoryName != _selectedCategory) {
        return false;
      }

      if (_maxPrice != null && srv.price > _maxPrice!) {
        return false;
      }

      return true;
    }).toList();
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

  void _openFilterModal() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return Container(
              decoration: const BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
              ),
              padding: EdgeInsets.only(
                bottom: MediaQuery.of(context).padding.bottom + 16,
                top: 12,
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 40,
                      height: 4,
                      decoration: BoxDecoration(
                        color: AppColors.outlineVariant,
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(
                      horizontal: AppShapes.gutterMobile,
                      vertical: 12,
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        LocalizedText(
                          'Bộ lọc trải nghiệm',
                          style: AppTypography.headlineSm(
                            color: AppColors.onSurface,
                          ),
                        ),
                        TextButton(
                          onPressed: () {
                            setModalState(() {
                              _selectedLocation = null;
                              _selectedCategory = null;
                              _selectedTimeOfDay = null;
                              _maxPrice = null;
                            });
                            setState(() {});
                          },
                          child: LocalizedText(
                            'Đặt lại',
                            style: AppTypography.labelMd(
                              color: AppColors.primary,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const Divider(height: 1),
                  Padding(
                    padding: const EdgeInsets.all(AppShapes.gutterMobile),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // VÙNG BIỂN
                        LocalizedText('Vùng biển',
                            style: AppTypography.labelLg(
                                color: AppColors.onSurface)),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          children: ['Mỹ Khê', 'Sơn Trà', 'Non Nước'].map((loc) {
                            final isSel = _selectedLocation == loc;
                            return ChoiceChip(
                              label: LocalizedText(loc),
                              selected: isSel,
                              onSelected: (val) {
                                setModalState(() {
                                  _selectedLocation = val ? loc : null;
                                });
                                setState(() {});
                              },
                              selectedColor: AppColors.secondary,
                              labelStyle: TextStyle(
                                color: isSel ? Colors.white : AppColors.onSurface,
                              ),
                            );
                          }).toList(),
                        ),
                        const SizedBox(height: 16),

                        // DANH MỤC
                        LocalizedText('Loại hoạt động',
                            style: AppTypography.labelLg(
                                color: AppColors.onSurface)),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          children: [
                            'Chèo SUP',
                            'Lặn biển',
                            'Cano lướt sóng',
                            'Lướt ván',
                          ].map((cat) {
                            final isSel = _selectedCategory == cat;
                            return ChoiceChip(
                              label: LocalizedText(cat),
                              selected: isSel,
                              onSelected: (val) {
                                setModalState(() {
                                  _selectedCategory = val ? cat : null;
                                });
                                setState(() {});
                              },
                              selectedColor: AppColors.secondary,
                              labelStyle: TextStyle(
                                color: isSel ? Colors.white : AppColors.onSurface,
                              ),
                            );
                          }).toList(),
                        ),
                        const SizedBox(height: 16),

                        // GIÁ TỐI ĐA
                        LocalizedText('Mức giá',
                            style: AppTypography.labelLg(
                                color: AppColors.onSurface)),
                        const SizedBox(height: 8),
                        Wrap(
                          spacing: 8,
                          children: [
                            {'label': 'Dưới 300k', 'val': 300000},
                            {'label': 'Dưới 500k', 'val': 500000},
                            {'label': 'Dưới 1 triệu', 'val': 1000000},
                          ].map((item) {
                            final val = item['val'] as int;
                            final isSel = _maxPrice == val;
                            return ChoiceChip(
                              label: LocalizedText(item['label'] as String),
                              selected: isSel,
                              onSelected: (selected) {
                                setModalState(() {
                                  _maxPrice = selected ? val : null;
                                });
                                setState(() {});
                              },
                              selectedColor: AppColors.secondary,
                              labelStyle: TextStyle(
                                color: isSel ? Colors.white : AppColors.onSurface,
                              ),
                            );
                          }).toList(),
                        ),
                      ],
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(
                        horizontal: AppShapes.gutterMobile),
                    child: SizedBox(
                      width: double.infinity,
                      height: 48,
                      child: ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(24),
                          ),
                        ),
                        onPressed: () => Navigator.pop(context),
                        child: LocalizedText(
                          'Áp dụng kết quả (${_filteredServices.length})',
                          style: AppTypography.labelLg(color: Colors.white),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final services = _filteredServices;

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        titleSpacing: 0,
        title: Container(
          height: 44,
          margin: const EdgeInsets.only(right: 8),
          decoration: BoxDecoration(
            color: AppColors.surfaceContainerLow,
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: AppColors.borderSubtle),
          ),
          child: TextField(
            controller: _searchController,
            onChanged: (_) => setState(() {}),
            decoration: InputDecoration(
              hintText: tr(context, 'Tìm hoạt động, bãi biển, nhà cung cấp...'),
              hintStyle: AppTypography.bodySm(color: AppColors.outline),
              prefixIcon: const Icon(Icons.search,
                  size: 20, color: AppColors.secondary),
              suffixIcon: _searchController.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.close, size: 18),
                      onPressed: () {
                        _searchController.clear();
                        setState(() {});
                      },
                    )
                  : null,
              border: InputBorder.none,
              contentPadding: const EdgeInsets.symmetric(vertical: 10),
            ),
          ),
        ),
        actions: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              IconButton(
                icon: const Icon(Icons.tune, color: AppColors.onSurface),
                onPressed: _openFilterModal,
              ),
              if (_activeFilterCount > 0)
                Positioned(
                  top: 6,
                  right: 6,
                  child: Container(
                    padding: const EdgeInsets.all(4),
                    decoration: const BoxDecoration(
                      color: AppColors.primary,
                      shape: BoxShape.circle,
                    ),
                    constraints:
                        const BoxConstraints(minWidth: 16, minHeight: 16),
                    child: Center(
                      child: LocalizedText(
                        '$_activeFilterCount',
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 9,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: Column(
        children: [
          // ACTIVE CHIPS ROW
          if (_activeFilterCount > 0)
            Container(
              height: 42,
              padding: const EdgeInsets.symmetric(
                  horizontal: AppShapes.gutterMobile),
              child: ListView(
                scrollDirection: Axis.horizontal,
                children: [
                  if (_selectedLocation != null)
                    _buildActiveChip(_selectedLocation!, () {
                      setState(() => _selectedLocation = null);
                    }),
                  if (_selectedCategory != null)
                    _buildActiveChip(_selectedCategory!, () {
                      setState(() => _selectedCategory = null);
                    }),
                  if (_maxPrice != null)
                    _buildActiveChip('Giá <= ${_formatPrice(_maxPrice!)}', () {
                      setState(() => _maxPrice = null);
                    }),
                  TextButton(
                    onPressed: () {
                      setState(() {
                        _selectedLocation = null;
                        _selectedCategory = null;
                        _selectedTimeOfDay = null;
                        _maxPrice = null;
                      });
                    },
                    child: LocalizedText(
                      'Xóa tất cả',
                      style: AppTypography.labelSm(color: AppColors.primary),
                    ),
                  ),
                ],
              ),
            ),

          // RESULTS COUNTER & VIEW TOGGLE (LIST / MAP)
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppShapes.gutterMobile,
              vertical: 8,
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      LocalizedText(
                        '${services.length} trải nghiệm',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.headlineSm(
                          color: AppColors.onSurface,
                        ),
                      ),
                      LocalizedText(
                        'Phù hợp điều kiện sóng biển hôm nay',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.bodySm(
                          color: AppColors.onSurfaceVariant,
                        ).copyWith(fontSize: 11),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 8),
                Container(
                  padding: const EdgeInsets.all(2),
                  decoration: BoxDecoration(
                    color: AppColors.surfaceContainerHigh,
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      InkWell(
                        onTap: () => setState(() => _isMapView = false),
                        borderRadius: BorderRadius.circular(18),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 5),
                          decoration: BoxDecoration(
                            color: !_isMapView
                                ? AppColors.surfaceContainerLowest
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(18),
                            boxShadow: !_isMapView
                                ? [
                                    BoxShadow(
                                      color: Colors.black.withValues(alpha: 0.06),
                                      blurRadius: 4,
                                    )
                                  ]
                                : null,
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                Icons.view_agenda,
                                size: 15,
                                color: !_isMapView
                                    ? AppColors.secondary
                                    : AppColors.onSurfaceVariant,
                              ),
                              const SizedBox(width: 4),
                              LocalizedText(
                                'Danh sách',
                                style: AppTypography.labelSm(
                                  color: !_isMapView
                                      ? AppColors.secondary
                                      : AppColors.onSurfaceVariant,
                                  fontWeight: !_isMapView
                                      ? FontWeight.w700
                                      : FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      InkWell(
                        onTap: () => setState(() => _isMapView = true),
                        borderRadius: BorderRadius.circular(18),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 5),
                          decoration: BoxDecoration(
                            color: _isMapView
                                ? AppColors.surfaceContainerLowest
                                : Colors.transparent,
                            borderRadius: BorderRadius.circular(18),
                            boxShadow: _isMapView
                                ? [
                                    BoxShadow(
                                      color: Colors.black.withValues(alpha: 0.06),
                                      blurRadius: 4,
                                    )
                                  ]
                                : null,
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(
                                Icons.map,
                                size: 15,
                                color: _isMapView
                                    ? AppColors.secondary
                                    : AppColors.onSurfaceVariant,
                              ),
                              const SizedBox(width: 4),
                              LocalizedText(
                                'Bản đồ',
                                style: AppTypography.labelSm(
                                  color: _isMapView
                                      ? AppColors.secondary
                                      : AppColors.onSurfaceVariant,
                                  fontWeight: _isMapView
                                      ? FontWeight.w700
                                      : FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),

          // CONTENT: LIST OR MAP
          Expanded(
            child: _isMapView
                ? _buildMapViewMock(services)
                : services.isEmpty
                    ? _buildEmptyState()
                    : ListView.separated(
                        padding: const EdgeInsets.symmetric(
                          horizontal: AppShapes.gutterMobile,
                          vertical: 8,
                        ),
                        itemCount: services.length,
                        separatorBuilder: (_, unused) =>
                            const SizedBox(height: AppShapes.spaceMd),
                        itemBuilder: (context, index) {
                          final item = services[index];
                          return _buildExperienceSearchCard(item);
                        },
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildActiveChip(String label, VoidCallback onRemove) {
    return Container(
      margin: const EdgeInsets.only(right: 6, top: 4, bottom: 4),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: AppColors.secondaryContainer.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          LocalizedText(
            label,
            style: AppTypography.labelSm(
              color: AppColors.onSecondaryContainer,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(width: 4),
          InkWell(
            onTap: onRemove,
            child: const Icon(
              Icons.close,
              size: 14,
              color: AppColors.onSecondaryContainer,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildExperienceSearchCard(ServiceModel srv) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusDefault,
        boxShadow: AppShapes.shadowLevel1,
        border: Border.all(color: AppColors.borderSubtle),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () {
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (_) => ExperienceDetailScreen(service: srv),
            ),
          );
        },
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Image with 4:3 ratio & Badges
            AspectRatio(
              aspectRatio: 16 / 9,
              child: Stack(
                fit: StackFit.expand,
                children: [
                  Image.network(
                    srv.imageUrls.isNotEmpty
                        ? srv.imageUrls.first
                        : 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=600&auto=format&fit=crop&q=80',
                    fit: BoxFit.cover,
                  ),
                  // Vendor Badge
                  Positioned(
                    top: 10,
                    left: 10,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: AppColors.secondary.withValues(alpha: 0.9),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.verified,
                              size: 12, color: AppColors.onSecondary),
                          const SizedBox(width: 4),
                          LocalizedText(
                            srv.vendorName,
                            style: AppTypography.labelSm(
                              color: AppColors.onSecondary,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                  // Ocean condition badge
                  Positioned(
                    bottom: 10,
                    left: 10,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceBright.withValues(alpha: 0.92),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.waves,
                              size: 12, color: AppColors.secondary),
                          const SizedBox(width: 4),
                          LocalizedText(
                            'Sóng êm: 0.4m',
                            style: AppTypography.labelSm(
                              color: AppColors.secondary,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
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
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.star,
                              size: 15, color: AppColors.starRating),
                          const SizedBox(width: 4),
                          LocalizedText(
                            srv.avgRating.toStringAsFixed(1),
                            style: AppTypography.labelMd(
                              color: AppColors.onSurface,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                          const SizedBox(width: 4),
                          LocalizedText(
                            '(${srv.ratingCount})',
                            style: AppTypography.bodySm(color: AppColors.outline),
                          ),
                        ],
                      ),
                      const SizedBox(width: 6),
                      Flexible(
                        child: LocalizedText(
                          'Khởi hành 05:00',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: AppTypography.labelSm(
                            color: AppColors.secondary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  LocalizedText(
                    srv.name,
                    style: AppTypography.headlineSm(
                      color: AppColors.onSurface,
                    ),
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
                        child: LocalizedText(
                          srv.locationName,
                          style: AppTypography.bodySm(
                            color: AppColors.onSurfaceVariant,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const Icon(Icons.schedule,
                          size: 14, color: AppColors.tertiary),
                      const SizedBox(width: 4),
                      LocalizedText(
                        '${srv.durationMinutes}p',
                        style: AppTypography.bodySm(
                          color: AppColors.onSurfaceVariant,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            LocalizedText('Giá trọn gói',
                                style: AppTypography.labelSm(
                                    color: AppColors.onSurfaceVariant)),
                            LocalizedText(
                              _formatPrice(srv.price),
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: AppTypography.headlineSm(
                                color: AppColors.primary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      ElevatedButton(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.primary,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(20),
                          ),
                          padding: const EdgeInsets.symmetric(
                              horizontal: 14, vertical: 8),
                        ),
                        onPressed: () {
                          Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) =>
                                  ExperienceDetailScreen(service: srv),
                            ),
                          );
                        },
                        child: LocalizedText(
                          'Xem chi tiết',
                          style: AppTypography.labelMd(color: Colors.white),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMapViewMock(List<ServiceModel> services) {
    return Container(
      color: AppColors.secondaryContainer.withValues(alpha: 0.2),
      child: Stack(
        children: [
          Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Icon(Icons.map_outlined,
                    size: 64, color: AppColors.secondary),
                const SizedBox(height: 12),
                LocalizedText(
                  'Bản đồ vị trí bãi biển Đà Nẵng',
                  style: AppTypography.headlineSm(color: AppColors.secondary),
                ),
                const SizedBox(height: 6),
                LocalizedText(
                  'Đang hiển thị ${services.length} điểm dịch vụ ven biển',
                  style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
                ),
              ],
            ),
          ),
          // Pins simulation
          Positioned(
            top: 60,
            left: 80,
            child: _buildMapPin('Mỹ Khê', 'SUP & Lướt sóng'),
          ),
          Positioned(
            top: 140,
            right: 60,
            child: _buildMapPin('Bán đảo Sơn Trà', 'Lặn ngắm san hô'),
          ),
          Positioned(
            bottom: 120,
            left: 100,
            child: _buildMapPin('Non Nước', 'Trường dạy lướt ván'),
          ),
        ],
      ),
    );
  }

  Widget _buildMapPin(String title, String desc) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: BorderRadius.circular(16),
        boxShadow: AppShapes.shadowLevel2,
        border: Border.all(color: AppColors.secondary, width: 1.2),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.location_on, size: 16, color: AppColors.primary),
          const SizedBox(width: 4),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              LocalizedText(title,
                  style: AppTypography.labelSm(
                      color: AppColors.onSurface, fontWeight: FontWeight.bold)),
              LocalizedText(desc,
                  style: AppTypography.bodySm(
                      color: AppColors.outline).copyWith(fontSize: 10)),
            ],
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
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerHigh,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.search_off,
                  size: 36, color: AppColors.outline),
            ),
            const SizedBox(height: 16),
            LocalizedText(
              'Không tìm thấy trải nghiệm phù hợp',
              style: AppTypography.headlineSm(color: AppColors.onSurface),
            ),
            const SizedBox(height: 8),
            LocalizedText(
              'Hãy thử thay đổi từ khóa hoặc xóa bớt tiêu chí lọc khu vực biển và mức giá.',
              textAlign: TextAlign.center,
              style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            OutlinedButton(
              onPressed: () {
                setState(() {
                  _searchController.clear();
                  _selectedLocation = null;
                  _selectedCategory = null;
                  _selectedTimeOfDay = null;
                  _maxPrice = null;
                });
              },
              child: const LocalizedText('Xóa tất cả bộ lọc'),
            ),
          ],
        ),
      ),
    );
  }
}
