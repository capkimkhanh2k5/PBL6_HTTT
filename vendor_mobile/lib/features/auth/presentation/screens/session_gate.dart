import 'package:flutter/material.dart';
import '../../../../core/auth/auth_session.dart';
import '../../../../core/l10n/app_localizations.dart';
import 'login_register_screen.dart';
import 'verify_email_screen.dart';
import 'vendor_access_screen.dart';

class SessionGate extends StatefulWidget {
  const SessionGate({super.key});
  @override
  State<SessionGate> createState() => _SessionGateState();
}

class _SessionGateState extends State<SessionGate> {
  late Future<bool> session;
  @override
  void initState() {
    super.initState();
    session = AuthSession.instance.restore();
  }

  @override
  Widget build(BuildContext context) => FutureBuilder<bool>(
    future: session,
    builder: (context, snapshot) {
      if (snapshot.connectionState != ConnectionState.done)
        return const Scaffold(body: Center(child: CircularProgressIndicator()));
      if (snapshot.hasError)
        return Scaffold(
          body: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Padding(
                  padding: const EdgeInsets.all(24),
                  child: LocalizedText(snapshot.error.toString()),
                ),
                FilledButton(
                  onPressed: () =>
                      setState(() => session = AuthSession.instance.restore()),
                  child: const LocalizedText('Thử lại'),
                ),
              ],
            ),
          ),
        );
      if (snapshot.data != true) return const LoginRegisterScreen();
      if (AuthSession.instance.pendingVerification)
        return Scaffold(
          body: Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const LocalizedText('Tài khoản cần xác minh email.'),
                FilledButton(
                  onPressed: () async {
                    final verified = await Navigator.push<bool>(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const VerifyEmailScreen(),
                      ),
                    );
                    if (verified == true && context.mounted)
                      Navigator.of(context).pushAndRemoveUntil(
                        MaterialPageRoute(
                          builder: (_) => const VendorAccessScreen(),
                        ),
                        (_) => false,
                      );
                  },
                  child: const LocalizedText('Xác minh email'),
                ),
                TextButton(
                  onPressed: () async {
                    await AuthSession.instance.clear();
                    if (context.mounted)
                      setState(() => session = Future.value(false));
                  },
                  child: const LocalizedText('Dùng tài khoản khác'),
                ),
              ],
            ),
          ),
        );
      return const VendorAccessScreen();
    },
  );
}
