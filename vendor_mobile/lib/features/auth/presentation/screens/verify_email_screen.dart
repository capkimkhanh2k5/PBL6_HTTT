import 'package:flutter/material.dart';
import '../../../../core/auth/auth_session.dart';
import '../../../../core/l10n/app_localizations.dart';

class VerifyEmailScreen extends StatefulWidget {
  const VerifyEmailScreen({super.key});
  @override
  State<VerifyEmailScreen> createState() => _VerifyEmailScreenState();
}

class _VerifyEmailScreenState extends State<VerifyEmailScreen> {
  final code = TextEditingController();
  bool busy = false;
  String? message;
  @override
  void dispose() {
    code.dispose();
    super.dispose();
  }

  Future<void> run(bool verify) async {
    if (busy) return;
    setState(() {
      busy = true;
      message = null;
    });
    try {
      if (verify) {
        if (!RegExp(r'^\d{6}$').hasMatch(code.text.trim()))
          throw AuthFailure('Vui lòng nhập mã OTP hợp lệ.');
        await AuthSession.instance.verify(code.text);
        if (mounted) Navigator.pop(context, true);
      } else {
        await AuthSession.instance.sendOtp();
        if (mounted)
          setState(
            () => message = 'Đã gửi mã xác minh. Kiểm tra email của bạn.',
          );
      }
    } catch (e) {
      if (mounted) setState(() => message = e.toString());
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const LocalizedText('Xác minh email')),
    body: ListView(
      padding: const EdgeInsets.all(24),
      children: [
        Text(AuthSession.instance.pendingEmail ?? ''),
        const LocalizedText(
          'Nhấn gửi mã để nhận OTP gồm 6 chữ số. Mã có hiệu lực trong 5 phút.',
        ),
        const SizedBox(height: 20),
        TextField(
          controller: code,
          keyboardType: TextInputType.number,
          maxLength: 6,
          decoration: InputDecoration(labelText: tr(context, 'Mã OTP')),
        ),
        if (message != null) LocalizedText(message!),
        FilledButton(
          onPressed: busy ? null : () => run(true),
          child: const LocalizedText('Xác nhận'),
        ),
        TextButton(
          onPressed: busy ? null : () => run(false),
          child: const LocalizedText('Gửi mã xác minh'),
        ),
        if (busy) const Center(child: CircularProgressIndicator()),
      ],
    ),
  );
}
