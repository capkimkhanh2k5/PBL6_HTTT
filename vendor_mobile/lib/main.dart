import 'package:flutter_localizations/flutter_localizations.dart';
import 'core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'core/theme/app_theme.dart';
import 'core/data/vendor_mock_database.dart';
import 'features/auth/presentation/screens/login_register_screen.dart';
import 'features/auth/presentation/screens/forgot_password_screen.dart';
import 'features/dashboard/presentation/screens/dashboard_screen.dart';
import 'features/profile/presentation/screens/business_profile_screen.dart';
import 'features/profile/presentation/screens/vendor_documents_screen.dart';
import 'features/profile/presentation/screens/edit_profile_bank_screen.dart';
import 'features/profile/presentation/screens/change_password_screen.dart';
import 'features/profile/presentation/screens/partner_account_screen.dart';
import 'features/services/presentation/screens/service_list_screen.dart';
import 'features/services/presentation/screens/service_create_edit_screen.dart';
import 'features/slots/presentation/screens/slot_calendar_screen.dart';
import 'features/slots/presentation/screens/slot_config_screen.dart';
import 'features/orders/presentation/screens/order_list_screen.dart';
import 'features/orders/presentation/screens/sub_order_detail_screen.dart';
import 'features/orders/presentation/screens/qr_checkin_screen.dart';
import 'features/orders/presentation/screens/refund_detail_screen.dart';
import 'features/chat/presentation/screens/chat_list_screen.dart';
import 'features/notifications/presentation/screens/notification_safety_screen.dart';
import 'features/reviews/presentation/screens/review_feedback_screen.dart';
import 'features/vouchers/presentation/screens/voucher_management_screen.dart';
import 'features/settlements/presentation/screens/settlement_revenue_screen.dart';
import 'features/settlements/presentation/screens/payout_detail_screen.dart';
import 'features/disputes/presentation/screens/dispute_management_screen.dart';
import 'features/navigation/presentation/screens/main_navigation_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppLanguage.instance.initialize();
  // Initialize mock database singleton
  VendorMockDatabase.instance;
  runApp(const DanaSeaVendorApp());
}

class DanaSeaVendorApp extends StatelessWidget {
  const DanaSeaVendorApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: AppLanguage.instance,
      builder: (context, child) => MaterialApp(
        locale: AppLanguage.instance.locale,
        supportedLocales: const [Locale('vi'), Locale('en')],
        localizationsDelegates: GlobalMaterialLocalizations.delegates,
        title: 'DANASEA Vendor',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        initialRoute: '/',
        routes: {
          '/': (context) => const MainNavigationScreen(),
          '/login': (context) => const LoginRegisterScreen(),
          '/forgot-password': (context) => const ForgotPasswordScreen(),
          '/dashboard': (context) => const DashboardScreen(),
          '/business-profile': (context) => const BusinessProfileScreen(),
          '/vendor-documents': (context) => const VendorDocumentsScreen(),
          '/edit-profile-bank': (context) => const EditProfileBankScreen(),
          '/services': (context) => const ServiceListScreen(),
          '/service-create': (context) => const ServiceCreateEditScreen(),
          '/slots-calendar': (context) => const SlotCalendarScreen(),
          '/slot-config': (context) => const SlotConfigScreen(),
          '/orders': (context) => const OrderListScreen(),
          '/qr-checkin': (context) => const QrCheckinScreen(),
          '/refund-detail': (context) => const RefundDetailScreen(),
          '/chat': (context) => const ChatListScreen(),
          '/notifications': (context) => const NotificationSafetyScreen(),
          '/reviews': (context) => const ReviewFeedbackScreen(),
          '/vouchers': (context) => const VoucherManagementScreen(),
          '/settlements': (context) => const SettlementRevenueScreen(),
          '/payout-detail': (context) => const PayoutDetailScreen(),
          '/disputes': (context) => const DisputeManagementScreen(),
          '/change-password': (context) => const ChangePasswordScreen(),
          '/partner-account': (context) => const PartnerAccountScreen(),
        },
        onGenerateRoute: (settings) {
          if (settings.name == '/service-edit') {
            final serviceId = settings.arguments as String?;
            final service = serviceId != null
                ? VendorMockDatabase.instance.services.firstWhere(
                    (s) => s.id == serviceId,
                    orElse: () => VendorMockDatabase.instance.services.first,
                  )
                : null;
            return MaterialPageRoute(
              builder: (context) => ServiceCreateEditScreen(service: service),
            );
          }
          if (settings.name == '/order-detail') {
            final subOrderId = settings.arguments as String? ?? 'DNS-8924-1';
            final subOrder = VendorMockDatabase.instance.subOrders.firstWhere(
              (o) => o.id == subOrderId,
              orElse: () => VendorMockDatabase.instance.subOrders.first,
            );
            return MaterialPageRoute(
              builder: (context) => SubOrderDetailScreen(subOrder: subOrder),
            );
          }
          return null;
        },
      ),
    );
  }
}
