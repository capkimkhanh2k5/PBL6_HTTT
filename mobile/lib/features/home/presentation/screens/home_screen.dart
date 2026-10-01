import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_bottom_nav_bar.dart';
import '../../../cart/presentation/screens/cart_screen.dart';
import '../../../chat/presentation/screens/ai_assistant_screen.dart';
import '../../../experience_detail/presentation/screens/experience_detail_screen.dart';
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
  late List<ExperienceItem> _experiences;

  @override
  void initState() {
    super.initState();
    _experiences = List.from(MockHomeData.featuredExperiences);
  }

  void _toggleFavorite(int index) {
    setState(() {
      final item = _experiences[index];
      _experiences[index] = item.copyWith(isFavorite: !item.isFavorite);
    });
  }

  ServiceModel _getServiceModel(ExperienceItem item) {
    // Find matching service from MockDatabaseData or construct fallback
    final matches = MockDatabaseData.services
        .where((s) => s.id == item.id || s.name.contains(item.title) || item.title.contains(s.name));
    if (matches.isNotEmpty) {
      return matches.first.copyWith(isFavorite: item.isFavorite);
    }
    return ServiceModel(
      id: item.id,
      vendorId: 'vnd-001',
      vendorName: 'Danang Ocean Club',
      categoryId: 'cat-001',
      categoryName: 'Trải nghiệm biển',
      name: item.title,
      slug: item.id,
      description:
          'Trải nghiệm biển hấp dẫn bậc nhất Đà Nẵng cùng đội ngũ huấn luyện viên giàu kinh nghiệm và trang thiết bị an toàn đạt chuẩn.',
      price: item.price,
      durationMinutes: 120,
      capacityPerSlot: 15,
      locationName: item.location,
      address: '${item.location}, Đà Nẵng',
      latitude: 16.0601,
      longitude: 108.2465,
      waiverContent:
          'Tôi cam kết đủ điều kiện sức khỏe tham gia các hoạt động thể thao biển và tuân thủ tuyệt đối quy định an toàn cứu sinh.',
      avgRating: item.rating,
      ratingCount: item.reviewCount,
      imageUrls: [item.imageUrl],
      isFavorite: item.isFavorite,
    );
  }

  void _openDetail(ExperienceItem item) {
    final service = _getServiceModel(item);
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ExperienceDetailScreen(service: service),
      ),
    );
  }

  void _openSearch([String? query, String? category]) {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => SearchScreen(
          initialQuery: query,
          initialCategory: category,
        ),
      ),
    );
  }

  void _openAiAssistant() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => const AiAssistantScreen(),
      ),
    );
  }

  void _openNotifications() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => const NotificationsScreen(),
      ),
    );
  }

  void _openCart() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => const CartScreen(),
      ),
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
          WishlistScreen(
            onExplore: () => setState(() => _currentNavIndex = 0),
          ),
          MyOrdersScreen(
            onExplore: () => setState(() => _currentNavIndex = 0),
          ),
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
            onCategoryChanged: (index) {
              final catNames = [
                'Chèo SUP',
                'Cano lướt sóng',
                'Lặn san hô',
                'Chèo kayak',
                'Tour biển đảo'
              ];
              if (index >= 0 && index < catNames.length) {
                _openSearch(null, catNames[index]);
              }
            },
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 3. FEATURED EXPERIENCES ("Trải nghiệm nổi bật tại Đà Nẵng")
          Padding(
            padding:
                const EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Trải nghiệm nổi bật tại Đà Nẵng',
                  style: AppTypography.headlineMd(
                    color: AppColors.onSurface,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Lựa chọn được yêu thích nhất trong tuần',
                  style: AppTypography.bodySm(
                    color: AppColors.onSurfaceVariant,
                  ),
                ),
                const SizedBox(height: 14),

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
                      item: exp,
                      onFavoriteToggle: () => _toggleFavorite(index),
                      onTap: () => _openDetail(exp),
                    );
                  },
                ),
                const SizedBox(height: 8),
                Center(
                  child: Text(
                    '* Dữ liệu trải nghiệm biển Đà Nẵng đối chiếu theo thiết kế hệ thống',
                    style: AppTypography.labelSm(
                      color: AppColors.outline,
                      fontWeight: FontWeight.w400,
                    ).copyWith(fontStyle: FontStyle.italic),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 4. EXPLORE BY BEACH REGION ("Vùng biển bạn muốn đến?")
          Padding(
            padding:
                const EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Vùng biển bạn muốn đến?',
                  style: AppTypography.headlineMd(
                    color: AppColors.onSurface,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
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
            padding:
                const EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            child: AiAssistantSection(
              onPlanWithAiTap: _openAiAssistant,
              onPromptSelected: (prompt) => _openAiAssistant(),
            ),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 6. REAL-TIME WEATHER & OCEAN CONDITIONS
          const Padding(
            padding:
                EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            child: WeatherOceanSection(),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 7. HOW IT WORKS SECTION
          const Padding(
            padding:
                EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
            child: HowItWorksSection(),
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 8. RECENTLY VIEWED CAROUSEL
          RecentlyViewedSection(
            onItemTap: (recent) {
              final matching = _experiences
                  .where((e) => e.title == recent.title)
                  .toList();
              if (matching.isNotEmpty) {
                _openDetail(matching.first);
              } else {
                _openSearch(recent.title);
              }
            },
          ),
          const SizedBox(height: AppShapes.spaceLg),

          // 9. FOOTER
          const HomeFooter(),
        ],
      ),
    );
  }
}
