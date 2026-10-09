import '../../../../core/data/library_store.dart';
import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/catalog_repository.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_bottom_nav_bar.dart';
import '../../../cart/presentation/screens/cart_screen.dart';
import '../../../chat/presentation/screens/ai_assistant_screen.dart';
import '../../../experience_detail/presentation/screens/catalog_detail_screen.dart';
import '../../../notifications/presentation/screens/notifications_screen.dart';
import '../../../orders/presentation/screens/my_orders_screen.dart';
import '../../../profile/presentation/screens/profile_screen.dart';
import '../../../search/presentation/screens/search_screen.dart';
import '../../../wishlist/presentation/screens/wishlist_screen.dart';
import '../../data/mock_home_data.dart';
import '../../domain/models/home_models.dart';
import '../widgets/ai_assistant_section.dart';
import '../widgets/category_tabs_widget.dart';
import '../widgets/editorial_region_card.dart';
import '../widgets/experience_card.dart';
import '../widgets/hero_search_widget.dart';
import '../widgets/home_footer.dart';
import '../widgets/home_header.dart';
import '../widgets/how_it_works_section.dart';
import '../widgets/recently_viewed_section.dart';
import '../widgets/weather_ocean_section.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentNavIndex = 0;
  List<ExperienceItem> _experiences = [];
  List<CategoryModel> _categories = [];
  final _catalog = CatalogRepository();
  final _library = LibraryStore.instance;
  bool _loading = true;
  String? _error;

  Future<void> _loadCatalog() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final results = await Future.wait([
        _catalog.categories(),
        _catalog.search(),
      ]);
      if (!mounted) return;
      setState(() {
        _categories = results[0] as List<CategoryModel>;
        _experiences = (results[1] as CatalogPage).items
            .map(
              (s) => ExperienceItem(
                id: s.id,
                title: s.name,
                location: s.address,
                duration: '',
                rating: s.avgRating,
                reviewCount: s.ratingCount,
                price: s.price,
                imageUrl: s.imageUrls.isEmpty ? '' : s.imageUrls.first,
                isVerified: false,
              ),
            )
            .toList();
      });
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  void initState() {
    super.initState();
    _loadCatalog();
    _library.addListener(_libraryChanged);
  }

  void _libraryChanged() {
    if (mounted) setState(() {});
  }

  @override
  void dispose() {
    _library.removeListener(_libraryChanged);
    super.dispose();
  }

  Future<void> _toggleFavorite(int index) async {
    final item = _experiences[index];
    try {
      await _library.toggle(
        SavedService(
          serviceId: item.id,
          name: item.title,
          imageUrl: item.imageUrl,
        ),
      );
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  void _openDetail(ExperienceItem item) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => CatalogDetailScreen(serviceId: item.id),
      ),
    );
  }

  void _openSearch([String? query, String? category]) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) =>
            SearchScreen(initialQuery: query, initialCategory: category),
      ),
    );
  }

  void _openAiAssistant() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const AiAssistantScreen()),
    );
  }

  void _openNotifications() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const NotificationsScreen()),
    );
  }

  void _openCart() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const CartScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: _currentNavIndex == 0
          ? HomeHeader(
              onAiTap: _openAiAssistant,
              onNotificationTap: _openNotifications,
              onCartTap: _openCart,
              onSearchTap: () => _openSearch(),
              onProfileTap: () => setState(() => _currentNavIndex = 3),
            )
          : null,
      bottomNavigationBar: AppBottomNavBar(
        currentIndex: _currentNavIndex,
        onTap: (index) {
          setState(() => _currentNavIndex = index);
        },
      ),
      body: IndexedStack(
        index: _currentNavIndex,
        children: [
          _buildExploreBody(),
          WishlistScreen(onExplore: () => setState(() => _currentNavIndex = 0)),
          MyOrdersScreen(onExplore: () => setState(() => _currentNavIndex = 0)),
          const ProfileScreen(),
        ],
      ),
    );
  }

  Widget _buildExploreBody() {
    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 1. HERO SECTION & FLOATING SEARCH CARD
          HeroSearchWidget(
            onExploreTap: () => _openSearch(),
            onAiSuggestTap: _openAiAssistant,
            onSearchTap: () => _openSearch(),
          ),

          // 2. CATEGORY TABS ("Khám phá theo sở thích")
          CategoryTabsWidget(
            categories: _categories,
            onShowAll: () => _openSearch(),
            onCategoryChanged: (index) =>
                _openSearch(null, _categories[index].id),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 3. FEATURED EXPERIENCES ("Trải nghiệm nổi bật tại Đà Nẵng")
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppShapes.gutterMobile,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LocalizedText(
                  'Trải nghiệm nổi bật tại Đà Nẵng',
                  style: AppTypography.headlineMd(color: AppColors.onSurface),
                ),
                const SizedBox(height: 2),
                LocalizedText(
                  'Lựa chọn được yêu thích nhất trong tuần',
                  style: AppTypography.bodySm(
                    color: AppColors.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 14),

                if (_loading) const LinearProgressIndicator(),
                if (_error != null) ...[
                  Text(_error!),
                  TextButton(
                    onPressed: _loadCatalog,
                    child: const LocalizedText('Thử lại'),
                  ),
                ],
                if (!_loading && _error == null && _experiences.isEmpty)
                  const LocalizedText('Không tìm thấy trải nghiệm.'),
                // Cards List
                ListView.separated(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: _experiences.length,
                  separatorBuilder: (context, index) =>
                      const SizedBox(height: AppShapes.spaceLg),
                  itemBuilder: (context, index) {
                    final exp = _experiences[index];
                    return ExperienceCard(
                      item: exp.copyWith(isFavorite: _library.contains(exp.id)),
                      onFavoriteToggle: _library.busy(exp.id)
                          ? null
                          : () => _toggleFavorite(index),
                      onTap: () => _openDetail(exp),
                    );
                  },
                ),
                const SizedBox(height: 8),
                Center(
                  child: TextButton(
                    onPressed: () => _openSearch(),
                    child: LocalizedText(
                      'Khám phá tất cả trải nghiệm',
                      style: AppTypography.labelSm(
                        color: AppColors.outline,
                        fontWeight: FontWeight.w400,
                      ).copyWith(fontStyle: FontStyle.italic),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 4. EXPLORE BY BEACH REGION ("Vùng biển bạn muốn đến?")
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppShapes.gutterMobile,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                LocalizedText(
                  'Vùng biển bạn muốn đến?',
                  style: AppTypography.headlineMd(color: AppColors.onSurface),
                ),
                const SizedBox(height: 2),
                LocalizedText(
                  'Điểm đến phong phú dọc bờ duyên hải miền Trung',
                  style: AppTypography.bodySm(
                    color: AppColors.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 14),

                ...MockHomeData.beachRegions.map((region) {
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 12),
                    child: EditorialRegionCard(
                      item: region,
                      onTap: () => _openSearch(region.title),
                    ),
                  );
                }),
              ],
            ),
          ),
          const SizedBox(height: AppShapes.spaceMd),

          // 5. AI SMART ASSISTANT SECTION
          Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppShapes.gutterMobile,
            ),
            child: AiAssistantSection(
              onPlanWithAiTap: _openAiAssistant,
              onPromptSelected: (prompt) => _openAiAssistant(),
            ),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 6. REAL-TIME WEATHER & OCEAN CONDITIONS
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            child: WeatherOceanSection(),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 7. HOW IT WORKS SECTION
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            child: HowItWorksSection(),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 8. RECENTLY VIEWED CAROUSEL
          RecentlyViewedSection(
            store: _library,
            onItemTap: (item) => Navigator.push(
              context,
              MaterialPageRoute(
                builder: (_) => CatalogDetailScreen(serviceId: item.serviceId),
              ),
            ),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 9. FOOTER
          const HomeFooter(),
        ],
      ),
    );
  }
}
