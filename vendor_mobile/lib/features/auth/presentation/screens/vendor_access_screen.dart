import 'package:flutter/material.dart';
import '../../../../core/auth/auth_session.dart';
import '../../../../core/l10n/app_localizations.dart';
import '../../../navigation/presentation/screens/main_navigation_screen.dart';
import 'login_register_screen.dart';

class VendorAccessScreen extends StatefulWidget {
  const VendorAccessScreen({super.key});
  @override
  State<VendorAccessScreen> createState() => _VendorAccessScreenState();
}

class _VendorAccessScreenState extends State<VendorAccessScreen> {
  final fields = <String, String>{
    'businessName': 'Tên doanh nghiệp',
    'taxCode': 'Mã số thuế',
    'address': 'Địa chỉ',
    'bankAccountNumber': 'Số tài khoản ngân hàng',
    'bankName': 'Tên ngân hàng',
    'bankAccountHolder': 'Chủ tài khoản',
  };
  late final controllers = {
    for (final key in fields.keys) key: TextEditingController(),
  };
  Map<String, dynamic>? profile;
  bool busy = true;
  bool missing = false;
  String? error;
  @override
  void initState() {
    super.initState();
    load();
  }

  @override
  void dispose() {
    for (final c in controllers.values) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> load() async {
    setState(() {
      busy = true;
      error = null;
    });
    try {
      profile = await AuthSession.instance.request('/api/vendor/profile');
      missing = false;
      if (profile?['verificationStatus'] == 'APPROVED' && mounted) {
        Navigator.of(context).pushReplacement(
          MaterialPageRoute(builder: (_) => const MainNavigationScreen()),
        );
      }
    } on AuthFailure catch (e) {
      if (e.status == 404) {
        missing = true;
      } else {
        error = e.message;
      }
    } catch (e) {
      error = e.toString();
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> submit() async {
    if (controllers.values.any((c) => c.text.trim().isEmpty)) {
      setState(() => error = 'Vui lòng nhập đầy đủ thông tin.');
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await AuthSession.instance.request(
        '/api/vendor/profile',
        method: 'POST',
        data: {for (final e in controllers.entries) e.key: e.value.text.trim()},
      );
      await load();
    } catch (e) {
      if (mounted) setState(() => error = e.toString());
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const LocalizedText('Tài khoản đối tác')),
    body: ListView(
      padding: const EdgeInsets.all(24),
      children: [
        if (busy) const LinearProgressIndicator(),
        if (error != null) LocalizedText(error!),
        if (missing) ...[
          const LocalizedText('Hoàn thiện hồ sơ doanh nghiệp'),
          for (final e in fields.entries)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: TextField(
                controller: controllers[e.key],
                decoration: InputDecoration(labelText: tr(context, e.value)),
              ),
            ),
          FilledButton(
            onPressed: busy ? null : submit,
            child: const LocalizedText('Gửi hồ sơ'),
          ),
        ] else if (profile != null) ...[
          LocalizedText(
            profile!['verificationStatus'] == 'REJECTED'
                ? 'Hồ sơ bị từ chối. Vui lòng liên hệ hỗ trợ.'
                : 'Hồ sơ đang chờ xét duyệt.',
          ),
        ],
        TextButton(
          onPressed: busy ? null : load,
          child: const LocalizedText('Kiểm tra lại'),
        ),
        TextButton(
          onPressed: busy
              ? null
              : () async {
                  try {
                    await AuthSession.instance.logout();
                  } catch (_) {}
                  if (context.mounted)
                    Navigator.of(context).pushAndRemoveUntil(
                      MaterialPageRoute(
                        builder: (_) => const LoginRegisterScreen(),
                      ),
                      (_) => false,
                    );
                },
          child: const LocalizedText('Đăng xuất'),
        ),
      ],
    ),
  );
}
