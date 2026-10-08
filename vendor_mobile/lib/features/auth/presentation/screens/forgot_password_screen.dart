import 'package:flutter/material.dart';
import '../../../../core/l10n/app_localizations.dart';

class ForgotPasswordScreen extends StatelessWidget {
  final String? initialEmail;
  const ForgotPasswordScreen({super.key, this.initialEmail});
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const LocalizedText('Quên mật khẩu?')),
    body: const Padding(
      padding: EdgeInsets.all(24),
      child: LocalizedText(
        'Khôi phục mật khẩu chưa khả dụng. Vui lòng liên hệ hỗ trợ.',
      ),
    ),
  );
}
