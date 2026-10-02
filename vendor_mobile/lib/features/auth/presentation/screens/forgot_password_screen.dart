import 'dart:async';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/widgets/vendor_button.dart';
import '../../../../core/widgets/vendor_text_field.dart';
import '../../../../core/widgets/toast_notification.dart';

class ForgotPasswordScreen extends StatefulWidget {
  final String? initialEmail;

  const ForgotPasswordScreen({super.key, this.initialEmail});

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  int _currentStep = 0; // 0: request, 1: sent, 2: expired, 3: reset
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _newPassController = TextEditingController();
  final TextEditingController _confirmPassController = TextEditingController();

  bool _obscureNewPass = true;
  bool _obscureConfirmPass = true;

  int _countdown = 60;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    _emailController.text = widget.initialEmail ?? 'partner@danangoceanclub.com';
  }

  @override
  void dispose() {
    _timer?.cancel();
    _emailController.dispose();
    _newPassController.dispose();
    _confirmPassController.dispose();
    super.dispose();
  }

  void _startTimer() {
    _timer?.cancel();
    setState(() {
      _countdown = 60;
    });
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_countdown > 0) {
        setState(() {
          _countdown--;
        });
      } else {
        timer.cancel();
      }
    });
  }

  void _switchStep(int step) {
    setState(() {
      _currentStep = step;
    });
    if (step == 1) {
      _startTimer();
    }
  }

  int _getPasswordStrength(String pass) {
    int score = 0;
    if (pass.length >= 8) score++;
    if (pass.contains(RegExp(r'[A-Z]')) && pass.contains(RegExp(r'[0-9]'))) score++;
    if (pass.contains(RegExp(r'[^A-Za-z0-9]'))) score++;
    if (pass.length >= 12) score++;
    return score;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.secondary),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.primaryContainer,
              ),
              child: const Icon(Icons.sailing, color: AppColors.onPrimary, size: 20),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'DANASEA',
                    style: Theme.of(context).textTheme.titleSmall?.copyWith(
                          fontWeight: FontWeight.bold,
                          color: AppColors.onSurface,
                        ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  Text(
                    'VENDOR PORTAL',
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                          color: AppColors.secondary,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 0.8,
                        ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          Container(
            margin: const EdgeInsets.only(right: 16),
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainer,
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.verified_user, size: 14, color: AppColors.secondary),
                SizedBox(width: 4),
                Text(
                  'Bảo mật đối tác',
                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.tertiary),
                ),
              ],
            ),
          )
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Editorial Header
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: AppColors.secondaryContainer.withOpacity(0.4),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: const Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.shield_outlined, size: 14, color: AppColors.onSecondaryContainer),
                    SizedBox(width: 4),
                    Text(
                      'Xác thực & Khôi phục',
                      style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.onSecondaryContainer),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              RichText(
                text: TextSpan(
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: AppColors.onSurface,
                      ),
                  children: const [
                    TextSpan(text: 'Bảo vệ tài khoản\n'),
                    TextSpan(text: 'đối tác bãi biển', style: TextStyle(color: AppColors.primary)),
                  ],
                ),
              ),
              const SizedBox(height: 6),
              Text(
                'Hệ thống xác minh độc quyền dành cho các đơn vị lướt ván, chèo SUP và lặn biển tại bờ biển Đà Nẵng.',
                style: Theme.of(context).textTheme.bodySmall?.copyWith(color: AppColors.tertiary),
              ),
              const SizedBox(height: 16),

              // Step pills
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Expanded(
                    child: Text(
                      'BƯỚC QUY TRÌNH (MÔ PHỎNG)',
                      style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.tertiary, letterSpacing: 0.5),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text('Bước ${_currentStep + 1}/4',
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.secondary)),
                ],
              ),
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainer,
                  borderRadius: BorderRadius.circular(30),
                ),
                child: Row(
                  children: [
                    _buildStepTab(0, '1. Gửi'),
                    _buildStepTab(1, '2. Hộp thư'),
                    _buildStepTab(2, '3. Hết hạn'),
                    _buildStepTab(3, '4. Mật khẩu'),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Coastal banner vignette
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  borderRadius: AppShapes.radiusMd,
                  gradient: const LinearGradient(
                    colors: [AppColors.secondary, AppColors.tertiary],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                ),
                child: const Row(
                  children: [
                    Icon(Icons.water, color: AppColors.secondaryContainer, size: 28),
                    SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'BẢO MẬT TÀI KHOẢN BẾN BÃI',
                            style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.secondaryContainer, letterSpacing: 0.5),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'Liên kết đặt lại mật khẩu chỉ dùng cho tài khoản đã yêu cầu.',
                            style: TextStyle(fontSize: 12, color: Colors.white),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Render current step panel
              if (_currentStep == 0) _buildStep0Request(),
              if (_currentStep == 1) _buildStep1Sent(),
              if (_currentStep == 2) _buildStep2Expired(),
              if (_currentStep == 3) _buildStep3Reset(),

              const SizedBox(height: 20),
              // Support footer widget
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainer,
                  borderRadius: AppShapes.radiusMd,
                ),
                child: Row(
                  children: [
                    Container(
                      width: 36,
                      height: 36,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppColors.secondary.withOpacity(0.15),
                      ),
                      child: const Icon(Icons.beach_access, color: AppColors.secondary, size: 20),
                    ),
                    const SizedBox(width: 12),
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Trung tâm hỗ trợ vận hành', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.onSurface)),
                          Text('Bán đảo Sơn Trà & Mỹ Khê', style: TextStyle(fontSize: 12, color: AppColors.tertiary)),
                        ],
                      ),
                    ),
                    InkWell(
                      onTap: () => ToastNotification.showInfo(context, 'Hotline 24/7: 1900-DANA-SEA'),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerLowest,
                          borderRadius: BorderRadius.circular(20),
                          boxShadow: AppShapes.shadowSm,
                        ),
                        child: const Text('Hotline 24/7', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.secondary)),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStepTab(int step, String label) {
    final bool active = _currentStep == step;
    return Expanded(
      child: GestureDetector(
        onTap: () => _switchStep(step),
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            color: active ? AppColors.surfaceContainerLowest : Colors.transparent,
            borderRadius: BorderRadius.circular(24),
            boxShadow: active ? AppShapes.shadowSm : null,
          ),
          child: Center(
            child: Text(
              label,
              style: TextStyle(
                fontSize: 11,
                fontWeight: active ? FontWeight.bold : FontWeight.normal,
                color: active ? AppColors.primary : AppColors.tertiary,
              ),
            ),
          ),
        ),
      ),
    );
  }

  // STEP 0: Request Reset Form
  Widget _buildStep0Request() {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusLg,
        boxShadow: AppShapes.shadowSm,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 44,
            height: 44,
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.primaryFixed,
            ),
            child: const Icon(Icons.lock_reset, color: AppColors.primary, size: 24),
          ),
          const SizedBox(height: 12),
          const Text('Khôi phục mật khẩu', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
          const SizedBox(height: 4),
          const Text(
            'Nhập email đăng ký tài khoản đơn vị vận hành đối tác (Vendor). DANASEA sẽ gửi liên kết bảo mật có thời hạn sử dụng.',
            style: TextStyle(fontSize: 13, color: AppColors.tertiary),
          ),
          const SizedBox(height: 16),
          VendorTextField(
            controller: _emailController,
            label: 'Email quản trị đối tác',
            isRequired: true,
            hint: 'partner@danangoceanclub.com',
            prefixIcon: Icons.mail_outline,
            keyboardType: TextInputType.emailAddress,
          ),
          const SizedBox(height: 4),
          const Row(
            children: [
              Icon(Icons.info_outline, size: 14, color: AppColors.secondary),
              SizedBox(width: 4),
              Expanded(
                child: Text('Email phải trùng khớp với hồ sơ đăng ký kinh doanh.', style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
              ),
            ],
          ),
          const SizedBox(height: 20),
          VendorButton(
            text: 'Gửi liên kết khôi phục',
            icon: Icons.arrow_forward,
            onPressed: () {
              if (_emailController.text.trim().isEmpty) {
                ToastNotification.showError(context, 'Vui lòng nhập email quản trị');
                return;
              }
              ToastNotification.showSuccess(context, 'Đã gửi liên kết xác minh đến ${_emailController.text}');
              _switchStep(1);
            },
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: AppShapes.radiusSm,
            ),
            child: const Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.mark_email_read_outlined, color: AppColors.secondary, size: 20),
                SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Hướng dẫn kiểm tra', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppColors.onSurface)),
                      SizedBox(height: 2),
                      Text('Vui lòng kiểm tra kỹ cả thư mục Quảng cáo và Hộp thư rác (Spam) nếu không nhận được email sau 1 phút.',
                          style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // STEP 1: Sent link confirmation
  Widget _buildStep1Sent() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusLg,
        boxShadow: AppShapes.shadowSm,
      ),
      child: Column(
        children: [
          Container(
            width: 56,
            height: 56,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.secondaryContainer.withOpacity(0.6),
            ),
            child: const Icon(Icons.forward_to_inbox, color: AppColors.secondary, size: 28),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: AppColors.secondaryFixed,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Text('KIỂM TRA HỘP THƯ', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.onSecondaryFixed)),
          ),
          const SizedBox(height: 10),
          const Text('Đã gửi liên kết xác minh', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
          const SizedBox(height: 6),
          const Text(
            'Chúng tôi đã gửi đường dẫn đặt lại mật khẩu an toàn đến hộp thư quản lý:',
            textAlign: TextAlign.center,
            style: TextStyle(fontSize: 13, color: AppColors.tertiary),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerHigh,
              borderRadius: BorderRadius.circular(20),
            ),
            child: Text(_emailController.text, style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.onSurface)),
          ),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: AppShapes.radiusSm,
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Thời hạn hiệu lực', style: TextStyle(fontSize: 12, color: AppColors.tertiary)),
                Text('15 phút kể từ lúc gửi', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.primary)),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.hourglass_top, size: 16, color: AppColors.tertiary),
              const SizedBox(width: 4),
              Text(
                _countdown > 0 ? 'Gửi lại mã sau: ${_countdown}s' : 'Có thể gửi lại liên kết',
                style: TextStyle(
                  fontSize: 13,
                  fontWeight: FontWeight.bold,
                  color: _countdown > 0 ? AppColors.primary : AppColors.secondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          VendorButton(
            text: 'Gửi lại liên kết',
            icon: Icons.replay,
            variant: VendorButtonVariant.secondary,
            onPressed: _countdown == 0
                ? () {
                    ToastNotification.showSuccess(context, 'Đã gửi lại liên kết mới');
                    _startTimer();
                  }
                : null,
          ),
          const SizedBox(height: 16),
          const Divider(),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
            children: [
              TextButton(
                onPressed: () => _switchStep(2),
                child: const Text('Mô phỏng: Link hết hạn', style: TextStyle(fontSize: 12, color: AppColors.error, decoration: TextDecoration.underline)),
              ),
              const Text('•', style: TextStyle(color: AppColors.tertiary)),
              TextButton(
                onPressed: () => _switchStep(3),
                child: const Text('Mô phỏng: Link hợp lệ', style: TextStyle(fontSize: 12, color: AppColors.secondary, decoration: TextDecoration.underline)),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // STEP 2: Expired warning
  Widget _buildStep2Expired() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusLg,
        boxShadow: AppShapes.shadowSm,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 52,
            height: 52,
            decoration: const BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.errorContainer,
            ),
            child: const Icon(Icons.link_off, color: AppColors.error, size: 26),
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: AppColors.errorContainer,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.warning, size: 14, color: AppColors.error),
                SizedBox(width: 4),
                Text('Không còn hiệu lực', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.onErrorContainer)),
              ],
            ),
          ),
          const SizedBox(height: 10),
          const Text('Liên kết đã hết hạn', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
          const SizedBox(height: 6),
          const Text(
            'Liên kết đã hết hạn hoặc không còn hiệu lực. Vui lòng yêu cầu liên kết mới để đảm bảo an toàn tài khoản đối tác.',
            style: TextStyle(fontSize: 13, color: AppColors.tertiary),
          ),
          const SizedBox(height: 16),
          _buildReasonItem(Icons.timer_off, 'Quá thời hạn hiệu lực 15 phút kể từ lúc gửi.'),
          _buildReasonItem(Icons.check_circle_outline, 'Liên kết đã được sử dụng một lần để đặt mật khẩu thành công.'),
          _buildReasonItem(Icons.lock_outline, 'Yêu cầu mới hơn đã được kích hoạt sau đó.'),
          const SizedBox(height: 20),
          VendorButton(
            text: 'Yêu cầu liên kết mới',
            icon: Icons.cached,
            onPressed: () => _switchStep(0),
          ),
          const SizedBox(height: 10),
          Center(
            child: TextButton.icon(
              onPressed: () => ToastNotification.showInfo(context, 'Ban quản lý vịnh: 1900-DANA-SEA'),
              icon: const Icon(Icons.support_agent, size: 16, color: AppColors.secondary),
              label: const Text('Cần hỗ trợ từ ban quản lý vịnh', style: TextStyle(fontSize: 12, color: AppColors.secondary)),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildReasonItem(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 16, color: AppColors.tertiary),
          const SizedBox(width: 8),
          Expanded(child: Text(text, style: const TextStyle(fontSize: 12, color: AppColors.tertiary))),
        ],
      ),
    );
  }

  // STEP 3: Reset new password
  Widget _buildStep3Reset() {
    final pass = _newPassController.text;
    final strength = _getPasswordStrength(pass);
    final isMatch = _confirmPassController.text.isNotEmpty && _confirmPassController.text == pass;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusLg,
        boxShadow: AppShapes.shadowSm,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 48,
            height: 48,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: AppColors.secondaryContainer.withOpacity(0.7),
            ),
            child: const Icon(Icons.key, color: AppColors.secondary, size: 24),
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: AppColors.secondaryFixed,
              borderRadius: BorderRadius.circular(16),
            ),
            child: const Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Icon(Icons.domain_verification, size: 14, color: AppColors.onSecondaryFixed),
                SizedBox(width: 4),
                Text('Xác thực thành công', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.onSecondaryFixed)),
              ],
            ),
          ),
          const SizedBox(height: 10),
          const Text('Đặt lại mật khẩu mới', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
          const SizedBox(height: 4),
          const Text('Thiết lập mật khẩu bảo mật cao cho tài khoản điều phối dịch vụ biển tại DANASEA.',
              style: TextStyle(fontSize: 13, color: AppColors.tertiary)),
          const SizedBox(height: 16),

          // Field 1
          VendorTextField(
            controller: _newPassController,
            label: 'Mật khẩu mới',
            isRequired: true,
            hint: 'Tối thiểu 8 ký tự',
            prefixIcon: Icons.lock_outline,
            obscureText: _obscureNewPass,
            onChanged: (val) => setState(() {}),
            suffixIcon: IconButton(
              icon: Icon(_obscureNewPass ? Icons.visibility : Icons.visibility_off, color: AppColors.tertiary, size: 20),
              onPressed: () => setState(() => _obscureNewPass = !_obscureNewPass),
            ),
          ),
          const SizedBox(height: 8),

          // Strength bars
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Độ mạnh mật khẩu:', style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
              Text(
                pass.isEmpty
                    ? 'Chưa nhập'
                    : strength <= 1
                        ? 'Yếu'
                        : strength == 2
                            ? 'Trung bình'
                            : strength == 3
                                ? 'Khá'
                                : 'Rất mạnh',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                  color: pass.isEmpty
                      ? AppColors.tertiary
                      : strength <= 1
                          ? AppColors.error
                          : strength == 2
                              ? AppColors.primary
                              : AppColors.secondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            children: List.generate(4, (index) {
              Color barColor = AppColors.surfaceContainerHighest;
              if (pass.isNotEmpty) {
                if (strength <= 1 && index == 0) barColor = AppColors.error;
                if (strength == 2 && index <= 1) barColor = AppColors.primary;
                if (strength == 3 && index <= 2) barColor = AppColors.secondary;
                if (strength >= 4) barColor = AppColors.secondary;
              }
              return Expanded(
                child: Container(
                  height: 4,
                  margin: EdgeInsets.only(right: index < 3 ? 4 : 0),
                  decoration: BoxDecoration(
                    color: barColor,
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              );
            }),
          ),
          const SizedBox(height: 16),

          // Field 2
          VendorTextField(
            controller: _confirmPassController,
            label: 'Xác nhận mật khẩu mới',
            isRequired: true,
            hint: 'Nhập lại mật khẩu mới',
            prefixIcon: Icons.verified_outlined,
            obscureText: _obscureConfirmPass,
            onChanged: (val) => setState(() {}),
            suffixIcon: IconButton(
              icon: Icon(_obscureConfirmPass ? Icons.visibility : Icons.visibility_off, color: AppColors.tertiary, size: 20),
              onPressed: () => setState(() => _obscureConfirmPass = !_obscureConfirmPass),
            ),
          ),
          if (_confirmPassController.text.isNotEmpty)
            Padding(
              padding: const EdgeInsets.only(top: 4, left: 4),
              child: Text(
                isMatch ? 'Mật khẩu xác nhận hoàn toàn khớp.' : 'Mật khẩu xác nhận chưa khớp.',
                style: TextStyle(
                  fontSize: 11,
                  fontWeight: isMatch ? FontWeight.bold : FontWeight.normal,
                  color: isMatch ? AppColors.secondary : AppColors.error,
                ),
              ),
            ),

          const SizedBox(height: 14),
          // Checklists
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLow,
              borderRadius: AppShapes.radiusSm,
            ),
            child: Column(
              children: [
                _buildChecklistRule(pass.length >= 8, 'Từ 8 ký tự trở lên'),
                _buildChecklistRule(pass.contains(RegExp(r'[A-Z]')) && pass.contains(RegExp(r'[0-9]')), 'Bao gồm chữ hoa và chữ số'),
                _buildChecklistRule(pass.contains(RegExp(r'[^A-Za-z0-9]')), 'Chứa ít nhất một ký tự đặc biệt (!@#\$)'),
              ],
            ),
          ),
          const SizedBox(height: 20),
          VendorButton(
            text: 'Lưu mật khẩu mới',
            icon: Icons.save,
            onPressed: () {
              if (pass.length < 8) {
                ToastNotification.showError(context, 'Mật khẩu cần tối thiểu 8 ký tự');
                return;
              }
              if (pass != _confirmPassController.text) {
                ToastNotification.showError(context, 'Mật khẩu xác nhận không khớp');
                return;
              }
              ToastNotification.showSuccess(context, 'Mật khẩu mới đã được cập nhật an toàn!');
              Navigator.of(context).maybePop();
            },
          ),
        ],
      ),
    );
  }

  Widget _buildChecklistRule(bool passed, String label) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Icon(
            passed ? Icons.check_circle : Icons.radio_button_unchecked,
            size: 15,
            color: passed ? AppColors.secondary : AppColors.tertiary,
          ),
          const SizedBox(width: 8),
          Text(
            label,
            style: TextStyle(
              fontSize: 11,
              fontWeight: passed ? FontWeight.bold : FontWeight.normal,
              color: passed ? AppColors.secondary : AppColors.tertiary,
            ),
          ),
        ],
      ),
    );
  }
}
