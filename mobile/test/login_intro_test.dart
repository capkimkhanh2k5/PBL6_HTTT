import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/main.dart';
import 'package:mobile/features/auth/presentation/screens/login_register_screen.dart';

void main() {
  testWidgets('App opens login and reveals usable form without overflow', (tester) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(const DanaSeaApp());
    expect(find.byType(LoginRegisterScreen), findsOneWidget);
    expect(find.text('DANASEA'), findsOneWidget);
    expect(find.byKey(const ValueKey('login-brand-symbol')), findsOneWidget);
    final wordmark = find.byKey(const ValueKey('intro-wordmark'));
    final initialPosition = tester.getCenter(wordmark);
    await tester.pump(const Duration(milliseconds: 1300));
    final settledPosition = tester.getCenter(wordmark);
    expect(settledPosition.dy, lessThan(initialPosition.dy));
    await tester.pump(const Duration(milliseconds: 1300));
    expect(tester.getCenter(wordmark), settledPosition);
    final curtain = find.byKey(const ValueKey('login-curtain'));
    final fullHeight = tester.getSize(curtain).height;
    await tester.pump(const Duration(milliseconds: 250));
    expect(tester.getCenter(wordmark).dx, greaterThan(settledPosition.dx));
    expect(tester.getSize(curtain).height, fullHeight);
    await tester.pump(const Duration(milliseconds: 500));
    expect(wordmark, findsNothing);
    expect(tester.getSize(curtain).height, lessThan(fullHeight));
    await tester.pumpAndSettle();
    final clip = tester.widget<ClipPath>(find.ancestor(of: curtain, matching: find.byType(ClipPath)).first);
    final size = tester.getSize(curtain);
    final bounds = clip.clipper!.getClip(size).getBounds();
    expect(bounds.bottom, lessThanOrEqualTo(size.height));
    expect(find.byType(TextField), findsNWidgets(2));
    expect(tester.takeException(), isNull);
    await tester.ensureVisible(find.widgetWithText(FilledButton, 'Đăng nhập'));
    await tester.tap(find.widgetWithText(FilledButton, 'Đăng nhập'));
    await tester.pump();
    expect(find.byType(LoginRegisterScreen), findsOneWidget);
    expect(find.byType(SnackBar), findsOneWidget);
  });
}
