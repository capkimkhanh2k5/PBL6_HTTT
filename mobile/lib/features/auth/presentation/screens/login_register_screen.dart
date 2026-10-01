import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';
import 'forgot_password_screen.dart';

class LoginRegisterScreen extends StatefulWidget {
  final bool initialIsLogin;

  const LoginRegisterScreen({super.key, this.initialIsLogin = true});

  @override
  State<LoginRegisterScreen> createState() => _LoginRegisterScreenState();
}

class _LoginRegisterScreenState extends State<LoginRegisterScreen> {
  late bool _isLogin;
  bool _obscurePassword = true;

  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _nameController = TextEditingController();
  final TextEditingController _phoneController = TextEditingController();

  @override
  void initState() {
    super.initState();
    _isLogin = widget.initialIsLogin;
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _nameController.dispose();
    _phoneController.dispose();
    super.dispose();
  }

  void _submit() {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          _isLogin
              ? 'Đăng nhập thành công! Chào mừng bạn trở lại DANASEA.'
              : 'Đăng ký tài khoản thành công! Vui lòng kiểm tra email để xác thực.',
        ),
        backgroundColor: AppColors.secondary,
      ),
    );
    Navigator.pop(context);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close, color: AppColors.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: AppShapes.gutterMobile),
        child: Column(
          children: [
            // BRAND LOGO & TITLE
            Container(
              width: 56,
              height: 56,
              decoration: const BoxDecoration(
                color: AppColors.secondary,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.surfing, size: 30, color: Colors.white),
            ),
            const SizedBox(height: 10),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'DANA',
                  style: AppTypography.headlineLg(color: AppColors.onSurface),
                ),
                Text(
                  'SEA',
                  style: AppTypography.headlineLg(color: AppColors.primary),
                ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              'Trải nghiệm thể thao & đại dương nguyên bản Đà Nẵng',
              style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),

            // LIVE OCEAN AMBIENT STRIP
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: AppColors.secondaryContainer.withValues(alpha: 0.35),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.waves, size: 16, color: AppColors.secondary),
                  const SizedBox(width: 6),
                  Flexible(
                    child: Text(
                      'Sóng 0.8m • Biển êm • 26°C • Cập nhật',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: AppTypography.labelSm(
                        color: AppColors.onSecondaryFixedVariant,
                        fontWeight: FontWeight.w700,
                      ).copyWith(fontSize: 11),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // TAB SWITCHER PILLS
            Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerHigh,
                borderRadius: BorderRadius.circular(24),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: InkWell(
                      onTap: () => setState(() => _isLogin = true),
                      borderRadius: BorderRadius.circular(20),
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        decoration: BoxDecoration(
                          color: _isLogin
                              ? AppColors.surfaceContainerLowest
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(20),
                          boxShadow: _isLogin
                              ? [
                                  BoxShadow(
                                    color: Colors.black.withValues(alpha: 0.05),
                                    blurRadius: 4,
                                  )
                                ]
                              : null,
                        ),
                        child: Center(
                          child: Text(
                            'Đăng nhập',
                            style: AppTypography.labelMd(
                              color: _isLogin
                                  ? AppColors.onSurface
                                  : AppColors.onSurfaceVariant,
                              fontWeight:
                                  _isLogin ? FontWeight.w700 : FontWeight.w500,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                  Expanded(
                    child: InkWell(
                      onTap: () => setState(() => _isLogin = false),
                      borderRadius: BorderRadius.circular(20),
                      child: Container(
                        padding: const EdgeInsets.symmetric(vertical: 8),
                        decoration: BoxDecoration(
                          color: !_isLogin
                              ? AppColors.surfaceContainerLowest
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(20),
                          boxShadow: !_isLogin
                              ? [
                                  BoxShadow(
                                    color: Colors.black.withValues(alpha: 0.05),
                                    blurRadius: 4,
                                  )
                                ]
                              : null,
                        ),
                        child: Center(
                          child: Text(
                            'Đăng ký',
                            style: AppTypography.labelMd(
                              color: !_isLogin
                                  ? AppColors.onSurface
                                  : AppColors.onSurfaceVariant,
                              fontWeight:
                                  !_isLogin ? FontWeight.w700 : FontWeight.w500,
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // FORM FIELDS
            if (!_isLogin) ...[
              _buildInput(
                controller: _nameController,
                hint: 'Họ và tên của bạn',
                icon: Icons.person_outline,
              ),
              const SizedBox(height: 12),
              _buildInput(
                controller: _phoneController,
                hint: 'Số điện thoại liên hệ',
                icon: Icons.phone_outlined,
                keyboardType: TextInputType.phone,
              ),
              const SizedBox(height: 12),
            ],

            _buildInput(
              controller: _emailController,
              hint: 'Email đăng nhập',
              icon: Icons.mail_outline,
              keyboardType: TextInputType.emailAddress,
            ),
            const SizedBox(height: 12),

            _buildInput(
              controller: _passwordController,
              hint: 'Mật khẩu bảo mật',
              icon: Icons.lock_outline,
              obscureText: _obscurePassword,
              trailing: IconButton(
                icon: Icon(
                  _obscurePassword
                      ? Icons.visibility_off_outlined
                      : Icons.visibility_outlined,
                  size: 20,
                  color: AppColors.outline,
                ),
                onPressed: () {
                  setState(() => _obscurePassword = !_obscurePassword);
                },
              ),
            ),
            const SizedBox(height: 8),

            if (_isLogin)
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => const ForgotPasswordScreen(),
                      ),
                    );
                  },
                  child: Text(
                    'Quên mật khẩu?',
                    style: AppTypography.labelSm(color: AppColors.secondary),
                  ),
                ),
              ),
            const SizedBox(height: 16),

            // SUBMIT BUTTON
            AppPillButton(
              label: _isLogin ? 'Đăng nhập vào DANASEA' : 'Tạo tài khoản người mua',
              variant: AppButtonVariant.primary,
              width: double.infinity,
              onPressed: _submit,
            ),
            const SizedBox(height: 24),

            // FOOTER TERMS
            Text(
              'Bằng việc tiếp tục, bạn đồng ý với Điều khoản dịch vụ và Chính sách bảo mật của nền tảng DANASEA.',
              textAlign: TextAlign.center,
              style: AppTypography.bodySm(color: AppColors.outline)
                  .copyWith(fontSize: 10),
            ),
            const SizedBox(height: 20),
          ],
        ),
      ),
    );
  }

  Widget _buildInput({
    required TextEditingController controller,
    required String hint,
    required IconData icon,
    bool obscureText = false,
    Widget? trailing,
    TextInputType? keyboardType,
  }) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusDefault,
        border: Border.all(color: AppColors.borderSubtle),
      ),
      child: Row(
        children: [
          Icon(icon, size: 20, color: AppColors.tertiary),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              controller: controller,
              obscureText: obscureText,
              keyboardType: keyboardType,
              decoration: InputDecoration(
                hintText: hint,
                hintStyle: AppTypography.bodySm(color: AppColors.outline),
                border: InputBorder.none,
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(vertical: 12),
              ),
            ),
          ),
          ?trailing,
        ],
      ),
    );
  }
}
