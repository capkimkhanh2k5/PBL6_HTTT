import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../../core/auth/auth_session.dart';
import '../../../../core/l10n/app_localizations.dart';
import 'session_gate.dart';

class ForgotPasswordScreen extends StatefulWidget {
  final String? initialEmail;
  final AuthSession? auth;
  const ForgotPasswordScreen({super.key, this.initialEmail, this.auth});
  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _form = GlobalKey<FormState>();
  late final _email = TextEditingController(text: widget.initialEmail);
  final _otp = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();
  AuthSession get _auth => widget.auth ?? AuthSession.instance;
  bool _sent = false, _busy = false, _done = false, _obscure = true;
  String? _error;
  int _seconds = 0;
  Timer? _timer;

  @override
  void dispose() {
    _timer?.cancel();
    for (final controller in [_email, _otp, _password, _confirm]) {
      controller.dispose();
    }
    super.dispose();
  }

  void _cooldown() {
    _timer?.cancel();
    _seconds = 60;
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) return;
      setState(() => _seconds--);
      if (_seconds == 0) timer.cancel();
    });
  }

  Future<void> _send({bool resend = false}) async {
    if (_busy || (!resend && !_form.currentState!.validate())) return;
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await _auth.requestPasswordReset(_email.text);
      if (!mounted) return;
      setState(() {
        _sent = true;
        _otp.clear();
        _cooldown();
      });
    } on AuthFailure catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted)
        setState(
          () => _error = 'Không thể thực hiện yêu cầu. Vui lòng thử lại.',
        );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _reset() async {
    if (_busy || !_form.currentState!.validate()) return;
    FocusScope.of(context).unfocus();
    setState(() {
      _busy = true;
      _error = null;
    });
    try {
      await _auth.resetPassword(_email.text, _otp.text, _password.text);
      if (!mounted) return;
      _timer?.cancel();
      _password.clear();
      _confirm.clear();
      _otp.clear();
      setState(() => _done = true);
    } on AuthFailure catch (e) {
      if (mounted) setState(() => _error = e.message);
    } catch (_) {
      if (mounted)
        setState(
          () => _error = 'Không thể thực hiện yêu cầu. Vui lòng thử lại.',
        );
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  void _login() => Navigator.of(context).pushAndRemoveUntil(
    MaterialPageRoute(builder: (_) => const SessionGate()),
    (_) => false,
  );

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: !_busy && !_done,
    onPopInvokedWithResult: (didPop, result) {
      if (!didPop && _done) _login();
    },
    child: Scaffold(
      appBar: AppBar(title: const LocalizedText('Quên mật khẩu?')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(24),
          child: Form(
            key: _form,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: _done
                  ? [
                      Icon(
                        Icons.check_circle_outline,
                        size: 64,
                        color: Theme.of(context).colorScheme.primary,
                      ),
                      const SizedBox(height: 24),
                      const LocalizedText(
                        'Đã đổi mật khẩu. Vui lòng đăng nhập lại.',
                      ),
                      const SizedBox(height: 24),
                      FilledButton(
                        onPressed: _login,
                        child: const LocalizedText('Đăng nhập'),
                      ),
                    ]
                  : [
                      LocalizedText(
                        _sent
                            ? 'Nếu email hợp lệ, bạn sẽ nhận mã gồm 6 số. Mã có hiệu lực trong 15 phút.'
                            : 'Nhập email tài khoản để nhận mã đặt lại mật khẩu.',
                      ),
                      const SizedBox(height: 24),
                      TextFormField(
                        controller: _email,
                        enabled: !_busy && !_sent,
                        keyboardType: TextInputType.emailAddress,
                        autocorrect: false,
                        decoration: const InputDecoration(labelText: 'Email'),
                        validator: (v) =>
                            RegExp(
                              r'^[^\s@]+@[^\s@]+\.[^\s@]+$',
                            ).hasMatch(v?.trim() ?? '')
                            ? null
                            : tr(context, 'Nhập email hợp lệ.'),
                      ),
                      if (_sent) ...[
                        Align(
                          alignment: Alignment.centerRight,
                          child: TextButton(
                            onPressed: _busy
                                ? null
                                : () {
                                    _timer?.cancel();
                                    setState(() {
                                      _sent = false;
                                      _error = null;
                                      _otp.clear();
                                    });
                                  },
                            child: const LocalizedText('Dùng email khác'),
                          ),
                        ),
                        TextFormField(
                          controller: _otp,
                          enabled: !_busy,
                          keyboardType: TextInputType.number,
                          autofillHints: const [AutofillHints.oneTimeCode],
                          inputFormatters: [
                            FilteringTextInputFormatter.digitsOnly,
                            LengthLimitingTextInputFormatter(6),
                          ],
                          decoration: InputDecoration(
                            labelText: tr(context, 'Mã OTP'),
                          ),
                          validator: (v) => RegExp(r'^\d{6}$').hasMatch(v ?? '')
                              ? null
                              : tr(context, 'Nhập mã OTP gồm 6 số.'),
                        ),
                        const SizedBox(height: 20),
                        TextFormField(
                          controller: _password,
                          enabled: !_busy,
                          obscureText: _obscure,
                          autocorrect: false,
                          enableSuggestions: false,
                          decoration: InputDecoration(
                            labelText: tr(context, 'Mật khẩu mới'),
                            suffixIcon: IconButton(
                              tooltip: tr(context, 'Hiện/ẩn mật khẩu'),
                              onPressed: () =>
                                  setState(() => _obscure = !_obscure),
                              icon: Icon(
                                _obscure
                                    ? Icons.visibility_off
                                    : Icons.visibility,
                              ),
                            ),
                          ),
                          validator: (v) => AuthSession.strongPassword(v ?? '')
                              ? null
                              : tr(
                                  context,
                                  'Dùng 8–100 ký tự gồm chữ hoa, chữ thường, số và @\$!%*?&.',
                                ),
                        ),
                        const SizedBox(height: 20),
                        TextFormField(
                          controller: _confirm,
                          enabled: !_busy,
                          obscureText: _obscure,
                          autocorrect: false,
                          enableSuggestions: false,
                          decoration: InputDecoration(
                            labelText: tr(context, 'Nhập lại mật khẩu mới'),
                          ),
                          validator: (v) => v == _password.text
                              ? null
                              : tr(context, 'Mật khẩu nhập lại không khớp.'),
                        ),
                      ],
                      if (_error != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 20),
                          child: Text(
                            tr(context, _error!),
                            style: TextStyle(
                              color: Theme.of(context).colorScheme.error,
                            ),
                          ),
                        ),
                      const SizedBox(height: 24),
                      FilledButton(
                        onPressed: _busy
                            ? null
                            : (_sent ? _reset : () => _send()),
                        child: _busy
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                ),
                              )
                            : LocalizedText(
                                _sent ? 'Đặt lại mật khẩu' : 'Gửi mã xác nhận',
                              ),
                      ),
                      if (_sent)
                        TextButton(
                          onPressed: _busy || _seconds > 0
                              ? null
                              : () => _send(resend: true),
                          child: Text(
                            '${tr(context, "Gửi lại mã")}${_seconds > 0 ? " ($_seconds s)" : ""}',
                          ),
                        ),
                    ],
            ),
          ),
        ),
      ),
    ),
  );
}
