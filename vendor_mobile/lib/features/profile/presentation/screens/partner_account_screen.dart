import '../../../../core/auth/auth_session.dart';
import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/data/vendor_mock_database.dart';
import '../../../../core/models/vendor_models.dart';
import '../../../auth/presentation/screens/login_register_screen.dart';
import '../../../notifications/presentation/screens/notification_safety_screen.dart';
import 'business_profile_screen.dart';
import 'vendor_documents_screen.dart';
import 'edit_profile_bank_screen.dart';
import 'change_password_screen.dart';
import '../../../reviews/presentation/screens/review_feedback_screen.dart';
import '../../../vouchers/presentation/screens/voucher_management_screen.dart';
import '../../../settlements/presentation/screens/settlement_revenue_screen.dart';
import '../../../settlements/presentation/screens/payout_detail_screen.dart';
import '../../../disputes/presentation/screens/dispute_management_screen.dart';

class PartnerAccountScreen extends StatefulWidget {
  const PartnerAccountScreen({super.key});

  @override
  State<PartnerAccountScreen> createState() => _PartnerAccountScreenState();
}

class _PartnerAccountScreenState extends State<PartnerAccountScreen> {
  final VendorMockDatabase _db = VendorMockDatabase.instance;
  String get _currentLocale => AppLanguage.instance.code;

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

  void _handleLogout() async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppShapes.rXl)),
        title: const LocalizedText('Đăng xuất tài khoản'),
        content: const LocalizedText('Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng DANASEA Vendor?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const LocalizedText('Hủy'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.error,
              foregroundColor: Colors.white,
            ),
            child: const LocalizedText('Đăng xuất'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      try { await AuthSession.instance.logout(); } catch (_) {}
      if (!mounted) return;
      Navigator.pushAndRemoveUntil(
        context,
        MaterialPageRoute(builder: (ctx) => const LoginRegisterScreen()),
        (route) => false,
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    Localizations.localeOf(context);
    final user = _db.currentUser;
    final vendor = _db.currentVendor;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 32,
              height: 32,
              decoration: const BoxDecoration(
                color: AppColors.secondary,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.waves, color: Colors.white, size: 18),
            ),
            const SizedBox(width: 8),
            Flexible(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  LocalizedText(
                    'DANASEA',
                    style: AppTypography.headlineSm.copyWith(
                      color: AppColors.secondary,
                      fontWeight: FontWeight.bold,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  LocalizedText(
                    'ĐỐI TÁC',
                    style: AppTypography.labelSm.copyWith(
                      color: AppColors.secondary,
                      letterSpacing: 1.0,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
        backgroundColor: AppColors.surface,
        elevation: 0,
        actions: [
          Container(
            margin: const EdgeInsets.symmetric(vertical: 14),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 2),
            decoration: BoxDecoration(
              color: AppColors.secondaryFixed.withAlpha(80),
              borderRadius: BorderRadius.circular(AppShapes.rFull),
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 6,
                  height: 6,
                  decoration: const BoxDecoration(
                    color: AppColors.secondary,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 6),
                LocalizedText(
                  'Sẵn sàng',
                  style: AppTypography.labelSm.copyWith(
                    color: AppColors.onSecondaryFixedVariant,
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ],
            ),
          ),
          IconButton(
            icon: const Icon(Icons.notifications_outlined, color: AppColors.onSurfaceVariant),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(builder: (ctx) => const NotificationSafetyScreen()),
              );
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Partner Profile Hero Card
            Container(
              width: double.infinity,
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainer,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x08000000), blurRadius: 8, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                children: [
                  // Avatar with verified badge
                  Stack(
                    children: [
                      CircleAvatar(
                        radius: 44,
                        backgroundColor: AppColors.surfaceContainerLowest,
                        backgroundImage: user.avatarUrl != null ? NetworkImage(user.avatarUrl!) : null,
                        child: user.avatarUrl == null ? const Icon(Icons.person, size: 44) : null,
                      ),
                      Positioned(
                        bottom: 0,
                        right: 2,
                        child: Container(
                          width: 26,
                          height: 26,
                          decoration: const BoxDecoration(
                            color: AppColors.secondary,
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.verified, color: Colors.white, size: 16),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  LocalizedText(
                    (AuthSession.instance.profile?['fullName'] ?? AuthSession.instance.profile?['email'] ?? '').toString(),
                    style: AppTypography.headlineSm.copyWith(
                      color: AppColors.onSurface,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 2),
                  LocalizedText(
                    vendor.businessName,
                    style: AppTypography.labelLg.copyWith(
                      color: AppColors.secondary,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLowest,
                      borderRadius: BorderRadius.circular(AppShapes.rFull),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.verified_user, size: 14, color: AppColors.secondary),
                        const SizedBox(width: 4),
                        Flexible(
                          child: LocalizedText(
                            'Đối tác biển đã xác thực',
                            style: AppTypography.labelSm.copyWith(
                              color: AppColors.secondary,
                              fontWeight: FontWeight.bold,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),

                  // Contact Metadata Pills
                  Wrap(
                    alignment: WrapAlignment.center,
                    spacing: 8,
                    runSpacing: 6,
                    children: [
                      Container(
                        constraints: const BoxConstraints(maxWidth: 240),
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(AppShapes.rFull),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.mail_outline, size: 14, color: AppColors.tertiary),
                            const SizedBox(width: 4),
                            Flexible(
                              child: LocalizedText(
                                (AuthSession.instance.profile?['email'] ?? '').toString(),
                                style: AppTypography.bodySm.copyWith(color: AppColors.onSurfaceVariant, fontSize: 12),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                      if (user.phone != null) ...[
                        Container(
                          constraints: const BoxConstraints(maxWidth: 240),
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLow,
                            borderRadius: BorderRadius.circular(AppShapes.rFull),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              const Icon(Icons.phone_iphone, size: 14, color: AppColors.tertiary),
                              const SizedBox(width: 4),
                              Flexible(
                                child: LocalizedText(
                                  user.phone!,
                                  style: AppTypography.bodySm.copyWith(color: AppColors.onSurfaceVariant, fontSize: 12),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Operational Metrics Strip
                  Row(
                    children: [
                      Expanded(
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 8),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLowest,
                            borderRadius: BorderRadius.circular(AppShapes.rMd),
                          ),
                          child: Column(
                            children: [
                              LocalizedText('Điểm đánh giá', style: AppTypography.labelSm.copyWith(color: AppColors.tertiary)),
                              const SizedBox(height: 2),
                              LocalizedText('4.9 / 5.0', style: AppTypography.labelLg.copyWith(color: AppColors.secondary, fontWeight: FontWeight.bold)),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Container(
                          padding: const EdgeInsets.symmetric(vertical: 8),
                          decoration: BoxDecoration(
                            color: AppColors.surfaceContainerLowest,
                            borderRadius: BorderRadius.circular(AppShapes.rMd),
                          ),
                          child: Column(
                            children: [
                              LocalizedText('Tỉ lệ check-in', style: AppTypography.labelSm.copyWith(color: AppColors.tertiary)),
                              const SizedBox(height: 2),
                              LocalizedText('98.5%', style: AppTypography.labelLg.copyWith(color: AppColors.secondary, fontWeight: FontWeight.bold)),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),

            // Language Switcher Bar (Responsive Wrap)
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: BorderRadius.circular(AppShapes.rMd),
              ),
              child: Wrap(
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: 8,
                runSpacing: 8,
                children: [
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      const Icon(Icons.translate, size: 18, color: AppColors.tertiary),
                      const SizedBox(width: 8),
                      LocalizedText(
                        'Ngôn ngữ ứng dụng',
                        style: AppTypography.labelMd.copyWith(color: AppColors.onSurface, fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      GestureDetector(
                        onTap: () => AppLanguage.instance.setLanguage('vi'),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: _currentLocale == 'vi' ? AppColors.secondary : Colors.transparent,
                            borderRadius: BorderRadius.circular(AppShapes.rFull),
                          ),
                          child: LocalizedText(
                            'Tiếng Việt',
                            style: AppTypography.labelSm.copyWith(
                              color: _currentLocale == 'vi' ? Colors.white : AppColors.onSurfaceVariant,
                              fontWeight: _currentLocale == 'vi' ? FontWeight.bold : FontWeight.normal,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(width: 4),
                      GestureDetector(
                        onTap: () => AppLanguage.instance.setLanguage('en'),
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: _currentLocale == 'en' ? AppColors.secondary : Colors.transparent,
                            borderRadius: BorderRadius.circular(AppShapes.rFull),
                          ),
                          child: LocalizedText(
                            'English',
                            style: AppTypography.labelSm.copyWith(
                              color: _currentLocale == 'en' ? Colors.white : AppColors.onSurfaceVariant,
                              fontWeight: _currentLocale == 'en' ? FontWeight.bold : FontWeight.normal,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Group 1: Business & Legal
            _buildSectionHeader(Icons.corporate_fare, 'Kinh doanh & Pháp lý'),
            Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x06000000), blurRadius: 6, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                children: [
                  _buildMenuItem(
                    icon: Icons.storefront,
                    iconBgColor: AppColors.secondaryContainer.withAlpha(80),
                    iconColor: AppColors.secondary,
                    title: 'Hồ sơ doanh nghiệp',
                    subtitle: '${vendor.businessName} • Lô 12 Võ Nguyên Giáp',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const BusinessProfileScreen()),
                    ),
                  ),
                  const Divider(height: 1, color: AppColors.surfaceContainerHigh),
                  _buildMenuItem(
                    icon: Icons.verified,
                    iconBgColor: AppColors.secondaryContainer.withAlpha(80),
                    iconColor: AppColors.secondary,
                    title: 'Giấy tờ & Chứng nhận an toàn',
                    subtitle: '2 chứng nhận hợp lệ',
                    badgeText: 'Chuẩn vị thế',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const VendorDocumentsScreen()),
                    ),
                  ),
                  const Divider(height: 1, color: AppColors.surfaceContainerHigh),
                  _buildMenuItem(
                    icon: Icons.account_balance,
                    iconBgColor: AppColors.secondaryContainer.withAlpha(80),
                    iconColor: AppColors.secondary,
                    title: 'Tài khoản ngân hàng nhận tiền',
                    subtitle: '${vendor.bankName ?? "Vietcombank"} •••• ${vendor.bankAccountNumber?.substring(vendor.bankAccountNumber!.length - 4) ?? "8988"} (Đã kiểm duyệt)',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const EditProfileBankScreen()),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Group 2: Sales & Customer Care
            _buildSectionHeader(Icons.sports_kabaddi, 'Bán hàng & Khách hàng'),
            Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x06000000), blurRadius: 6, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                children: [
                  _buildMenuItem(
                    icon: Icons.hotel_class,
                    iconBgColor: AppColors.primaryFixed.withAlpha(80),
                    iconColor: AppColors.primaryContainer,
                    title: 'Đánh giá của khách hàng',
                    subtitle: '${vendor.ratingAvg} ★ • ${vendor.ratingCount} lượt phản hồi',
                    badgeText: 'Xuất sắc',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const ReviewFeedbackScreen()),
                    ),
                  ),
                  const Divider(height: 1, color: AppColors.surfaceContainerHigh),
                  _buildMenuItem(
                    icon: Icons.loyalty,
                    iconBgColor: AppColors.primaryFixed.withAlpha(80),
                    iconColor: AppColors.primaryContainer,
                    title: 'Mã giảm giá của đối tác',
                    subtitle: '${_db.vouchers.where((v) => v.isActive).length} mã đang kích hoạt',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const VoucherManagementScreen()),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Group 3: Finance & Dispatches
            _buildSectionHeader(Icons.payments, 'Tài chính & Khiếu nại'),
            Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x06000000), blurRadius: 6, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                children: [
                  _buildMenuItem(
                    icon: Icons.query_stats,
                    iconBgColor: AppColors.secondaryContainer.withAlpha(80),
                    iconColor: AppColors.secondary,
                    title: 'Đối soát doanh thu kỳ này',
                    subtitle: 'Chu kỳ 16/10 - 31/10/2024',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const SettlementRevenueScreen()),
                    ),
                  ),
                  const Divider(height: 1, color: AppColors.surfaceContainerHigh),
                  _buildMenuItem(
                    icon: Icons.history_edu,
                    iconBgColor: AppColors.secondaryContainer.withAlpha(80),
                    iconColor: AppColors.secondary,
                    title: 'Yêu cầu thanh toán & Lịch sử',
                    subtitle: 'Lần chuyển gần nhất: 16/10',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const PayoutDetailScreen()),
                    ),
                  ),
                  const Divider(height: 1, color: AppColors.surfaceContainerHigh),
                  _buildMenuItem(
                    icon: Icons.report_problem_outlined,
                    iconBgColor: AppColors.errorContainer.withAlpha(70),
                    iconColor: AppColors.error,
                    title: 'Khiếu nại dịch vụ liên quan',
                    subtitle: '${_db.disputes.where((d) => d.status == DisputeStatus.inReview).length} yêu cầu cần theo dõi',
                    badgeText: 'Cần xử lý',
                    badgeIsDestructive: true,
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const DisputeManagementScreen()),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // Group 4: System Settings
            _buildSectionHeader(Icons.tune, 'Cài đặt hệ thống'),
            Container(
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x06000000), blurRadius: 6, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                children: [
                  _buildMenuItem(
                    icon: Icons.lock_reset,
                    iconBgColor: AppColors.surfaceContainer,
                    iconColor: AppColors.onSurfaceVariant,
                    title: 'Đổi mật khẩu',
                    subtitle: 'Cập nhật mật khẩu bảo mật tài khoản',
                    onTap: () => Navigator.push(
                      context,
                      MaterialPageRoute(builder: (ctx) => const ChangePasswordScreen()),
                    ),
                  ),
                  const Divider(height: 1, color: AppColors.surfaceContainerHigh),
                  _buildMenuItem(
                    icon: Icons.logout,
                    iconBgColor: AppColors.errorContainer.withAlpha(60),
                    iconColor: AppColors.error,
                    title: 'Đăng xuất',
                    subtitle: 'Thoát khỏi phiên làm việc hiện tại',
                    onTap: _handleLogout,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 50),
          ],
        ),
      ),
    );
  }

  Widget _buildSectionHeader(IconData icon, String title) {
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 8),
      child: Row(
        children: [
          Icon(icon, size: 18, color: AppColors.secondary),
          const SizedBox(width: 8),
          Expanded(
            child: LocalizedText(
              title,
              style: AppTypography.labelLg.copyWith(
                color: AppColors.onSurface,
                fontWeight: FontWeight.bold,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMenuItem({
    required IconData icon,
    required Color iconBgColor,
    required Color iconColor,
    required String title,
    required String subtitle,
    String? badgeText,
    bool badgeIsDestructive = false,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Row(
            children: [
              Container(
                width: 38,
                height: 38,
                decoration: BoxDecoration(
                  color: iconBgColor,
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, color: iconColor, size: 20),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    LocalizedText(
                      title,
                      style: AppTypography.labelLg.copyWith(
                        color: AppColors.onSurface,
                        fontWeight: FontWeight.bold,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 2),
                    LocalizedText(
                      subtitle,
                      style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              if (badgeText != null) ...[
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: badgeIsDestructive
                        ? AppColors.errorContainer
                        : AppColors.secondaryFixed.withAlpha(80),
                    borderRadius: BorderRadius.circular(AppShapes.rFull),
                  ),
                  child: LocalizedText(
                    badgeText,
                    style: AppTypography.labelSm.copyWith(
                      color: badgeIsDestructive
                          ? AppColors.onErrorContainer
                          : AppColors.onSecondaryFixedVariant,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                const SizedBox(width: 6),
              ],
              const Icon(Icons.chevron_right, color: AppColors.outlineVariant),
            ],
          ),
        ),
      ),
    );
  }
}
