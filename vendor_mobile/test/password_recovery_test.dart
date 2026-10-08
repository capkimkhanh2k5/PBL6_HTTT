import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vendor_mobile/core/auth/auth_session.dart';
import 'package:vendor_mobile/features/auth/presentation/screens/forgot_password_screen.dart';

class FakeRecovery extends AuthSession {
  int sends = 0, resets = 0;
  @override
  Future<void> requestPasswordReset(String email) async { sends++; }
  @override
  Future<void> resetPassword(String email, String otp, String password) async {
    resets++;
    if (otp != '123456') throw AuthFailure('Invalid OTP');
  }
}
void main() {
  testWidgets('Recovery validates, preserves failed input and shows success only after reset', (tester) async {
    final auth = FakeRecovery();
    await tester.pumpWidget(MaterialApp(home: ForgotPasswordScreen(auth: auth)));
    await tester.tap(find.byType(FilledButton)); await tester.pump();
    expect(auth.sends, 0);
    await tester.enterText(find.byType(TextFormField).first, 'test@example.com');
    await tester.tap(find.byType(FilledButton)); await tester.pumpAndSettle();
    expect(auth.sends, 1);
    expect(find.byType(TextFormField), findsNWidgets(4));
    await tester.enterText(find.byType(TextFormField).at(1), '000000');
    await tester.enterText(find.byType(TextFormField).at(2), 'NewStrong123!');
    await tester.enterText(find.byType(TextFormField).at(3), 'mismatch');
    await tester.ensureVisible(find.byType(FilledButton));
    await tester.tap(find.byType(FilledButton)); await tester.pump();
    expect(auth.resets, 0);
    await tester.enterText(find.byType(TextFormField).at(3), 'NewStrong123!');
    await tester.ensureVisible(find.byType(FilledButton));
    await tester.tap(find.byType(FilledButton)); await tester.pumpAndSettle();
    expect(find.text('Invalid OTP'), findsOneWidget);
    expect(auth.resets, 1);
    await tester.enterText(find.byType(TextFormField).at(1), '123456');
    await tester.ensureVisible(find.byType(FilledButton));
    await tester.tap(find.byType(FilledButton)); await tester.pumpAndSettle();
    expect(auth.resets, 2);
    expect(find.byIcon(Icons.check_circle_outline), findsOneWidget);
    expect(find.byType(TextFormField), findsNothing);
    await tester.pumpWidget(const SizedBox());
    auth.client.close(force: true);
  });
}
