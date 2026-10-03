import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:vendor_mobile/core/l10n/translations.dart';

void main() {
  test('English catalog preserves every template placeholder', () {
    final placeholders = RegExp(r'\{\d+\}');
    for (final entry in englishTranslations.entries) {
      expect(placeholders.allMatches(entry.value).map((m) => m[0]).toSet(),
        placeholders.allMatches(entry.key).map((m) => m[0]).toSet(), reason: entry.key);
    }
    expect(translate('Đơn hàng', 'en'), 'Orders');
    expect(translate('2 khách', 'en'), '2 guests');
    expect(translate('Đã thêm Chèo SUP vào giỏ hàng!', 'en'), 'Added Paddleboarding to your cart!');
    expect(translate('Nguyễn Văn An', 'en'), 'Nguyễn Văn An');
    expect(translate('Đơn hàng', 'vi'), 'Đơn hàng');
  });

  testWidgets('Switches existing route and preserves input, then restores preference', (tester) async {
    SharedPreferences.setMockInitialValues({});
    await AppLanguage.instance.initialize();
    final controller = TextEditingController();
    await tester.pumpWidget(ListenableBuilder(
      listenable: AppLanguage.instance,
      builder: (context, _) => MaterialApp(
        locale: AppLanguage.instance.locale,
        supportedLocales: const [Locale('vi'), Locale('en')],
        localizationsDelegates: GlobalMaterialLocalizations.delegates,
        home: Scaffold(body: Column(children: [
          const LocalizedText('Đơn hàng'),
          TextField(controller: controller),
          Builder(builder: (context) => TextButton(
            onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(
              builder: (_) => const Scaffold(body: LocalizedText('Tài khoản')))),
            child: const Text('Open'),
          )),
        ])),
      ),
    ));
    await tester.pumpAndSettle();
    await tester.enterText(find.byType(TextField), 'My draft');
    await tester.tap(find.text('Open'));
    await tester.pumpAndSettle();
    await AppLanguage.instance.setLanguage('en');
    await tester.pumpAndSettle();
    expect(find.text('Account'), findsOneWidget);
    tester.state<NavigatorState>(find.byType(Navigator)).pop();
    await tester.pumpAndSettle();
    expect(find.text('Orders'), findsOneWidget);
    expect(controller.text, 'My draft');
    expect((await SharedPreferences.getInstance()).getString(AppLanguage.preferenceKey), 'en');
    await AppLanguage.instance.initialize();
    expect(AppLanguage.instance.code, 'en');
    await AppLanguage.instance.setLanguage('vi');
    await tester.pumpAndSettle();
    expect(find.text('Đơn hàng'), findsOneWidget);
    await tester.pumpWidget(const SizedBox());
    controller.dispose();
  });
}
