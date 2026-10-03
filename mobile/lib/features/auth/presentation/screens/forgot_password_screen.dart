import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../shared/widgets/app_pill_button.dart';

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  int _step = 1; // 1: Email, 2: OTP, 3: New Password
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _otpController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  final TextEditingController _confirmPasswordController =
      TextEditingController();

  @override
  void dispose() {
    _emailController.dispose();
    _otpController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  void _nextStep() {
    if (_step == 1) {
      if (_emailController.text.trim().isEmpty) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: LocalizedText('Vui lòng nhập email đăng ký tài khoản.')),
        );
        return;
      }
      setState(() => _step = 2);
    } else if (_step == 2) {
      if (_otpController.text.trim().length < 4) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: LocalizedText('Vui lòng nhập mã OTP hợp lệ.')),
        );
        return;
      }
      setState(() => _step = 3);
    } else {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: LocalizedText('Đổi mật khẩu thành công! Bạn có thể đăng nhập ngay.'),
          backgroundColor: AppColors.secondary,
        ),
      );
      Navigator.pop(context);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        title: LocalizedText(
          'Khôi phục mật khẩu',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(AppShapes.gutterMobile),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // STEP PROGRESS PILL
            Center(
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                decoration: BoxDecoration(
                  color: AppColors.secondaryContainer.withValues(alpha: 0.35),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: LocalizedText(
                  'Bước $_step / 3: ${_getStepTitle()}',
                  style: AppTypography.labelSm(
                    color: AppColors.onSecondaryContainer,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 24),

            if (_step == 1) ...[
              LocalizedText(
                'Nhập địa chỉ email',
                style: AppTypography.headlineSm(color: AppColors.onSurface),
              ),
              const SizedBox(height: 6),
              LocalizedText(
                'Mã xác nhận bảo mật gồm 6 chữ số sẽ được gửi đến hộp thư email của bạn.',
                style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
              ),
              const SizedBox(height: 16),
              _buildField(
                controller: _emailController,
                hint: 'name@example.com',
                icon: Icons.mail_outline,
                keyboardType: TextInputType.emailAddress,
              ),
            ] else if (_step == 2) ...[
              LocalizedText(
                'Nhập mã xác thực OTP',
                style: AppTypography.headlineSm(color: AppColors.onSurface),
              ),
              const SizedBox(height: 6),
              LocalizedText(
                'Đã gửi mã đến ${_emailController.text}. Vui lòng nhập mã xác minh bên dưới:',
                style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
              ),
              const SizedBox(height: 16),
              _buildField(
                controller: _otpController,
                hint: 'Ví dụ: 829412',
                icon: Icons.vpn_key_outlined,
                keyboardType: TextInputType.number,
              ),
            ] else ...[
              LocalizedText(
                'Tạo mật khẩu mới',
                style: AppTypography.headlineSm(color: AppColors.onSurface),
              ),
              const SizedBox(height: 6),
              LocalizedText(
                'Mật khẩu nên chứa ít nhất 8 ký tự, bao gồm chữ và số để đảm bảo an toàn.',
                style: AppTypography.bodySm(color: AppColors.onSurfaceVariant),
              ),
              const SizedBox(height: 16),
              _buildField(
                controller: _passwordController,
                hint: 'Mật khẩu mới',
                icon: Icons.lock_outline,
                obscure: true,
              ),
              const SizedBox(height: 12),
              _buildField(
                controller: _confirmPasswordController,
                hint: 'Xác nhận lại mật khẩu',
                icon: Icons.lock_outline,
                obscure: true,
              ),
            ],

            const SizedBox(height: 32),
            AppPillButton(
              label: _step == 3 ? 'Cập nhật mật khẩu' : 'Tiếp tục',
              variant: AppButtonVariant.primary,
              width: double.infinity,
              onPressed: _nextStep,
            ),
          ],
        ),
      ),
    );
  }

  String _getStepTitle() {
    if (_step == 1) return 'Xác định tài khoản';
    if (_step == 2) return 'Xác thực OTP';
    return 'Đặt lại mật khẩu';
  }

  Widget _buildField({
    required TextEditingController controller,
    required String hint,
    required IconData icon,
    bool obscure = false,
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
          Icon(icon, size: 20, color: AppColors.secondary),
          const SizedBox(width: 10),
          Expanded(
            child: TextField(
              controller: controller,
              obscureText: obscure,
              keyboardType: keyboardType,
              decoration: InputDecoration(
                hintText: tr(context, hint),
                hintStyle: AppTypography.bodySm(color: AppColors.outline),
                border: InputBorder.none,
                isDense: true,
                contentPadding: const EdgeInsets.symmetric(vertical: 12),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
