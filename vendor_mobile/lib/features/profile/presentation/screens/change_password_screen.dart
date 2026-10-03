import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/widgets/vendor_button.dart';
import '../../../../core/widgets/vendor_text_field.dart';
import '../../../../core/widgets/toast_notification.dart';
import '../../../auth/presentation/screens/forgot_password_screen.dart';

class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});

  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final TextEditingController _currentPassController = TextEditingController();
  final TextEditingController _newPassController = TextEditingController();
  final TextEditingController _confirmPassController = TextEditingController();

  bool _obscureCurrent = true;
  bool _obscureNew = true;
  bool _obscureConfirm = true;

  String? _errorMessage;
  bool _showSuccess = false;

  @override
  void dispose() {
    _currentPassController.dispose();
    _newPassController.dispose();
    _confirmPassController.dispose();
    super.dispose();
  }

  int _calculateStrength(String pass) {
    int score = 0;
    if (pass.length >= 8) score++;
    if (pass.contains(RegExp(r'[a-z]')) && pass.contains(RegExp(r'[A-Z]'))) score++;
    if (pass.contains(RegExp(r'[0-9]'))) score++;
    if (pass.contains(RegExp(r'[^A-Za-z0-9]'))) score++;
    return score;
  }

  void _submit() {
    setState(() {
      _errorMessage = null;
      _showSuccess = false;
    });

    final current = _currentPassController.text.trim();
    final newPass = _newPassController.text.trim();
    final confirm = _confirmPassController.text.trim();

    if (current.isEmpty) {
      setState(() {
        _errorMessage = 'Vui lòng điền mật khẩu hiện tại của bạn.';
      });
      return;
    }

    if (newPass.length < 8 || newPass != confirm) {
      setState(() {
        _errorMessage = 'Mật khẩu xác nhận không trùng khớp hoặc chưa đạt chuẩn an toàn 8 ký tự.';
      });
      return;
    }

    setState(() {
      _showSuccess = true;
      _currentPassController.clear();
      _newPassController.clear();
      _confirmPassController.clear();
    });
    ToastNotification.showSuccess(context, 'Đổi mật khẩu thành công!');
  }

  @override
  Widget build(BuildContext context) {
    final newPass = _newPassController.text;
    final strength = _calculateStrength(newPass);
    final isMatch = _confirmPassController.text.isNotEmpty && _confirmPassController.text == newPass;

    final hasLen = newPass.length >= 8;
    final hasUpperLower = newPass.contains(RegExp(r'[a-z]')) && newPass.contains(RegExp(r'[A-Z]'));
    final hasNumber = newPass.contains(RegExp(r'[0-9]'));
    final hasSpecial = newPass.contains(RegExp(r'[^A-Za-z0-9]'));

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.secondary),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            LocalizedText('CÀI ĐẶT AN NINH',
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: AppColors.tertiary,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 0.8,
                    )),
            LocalizedText('Đổi mật khẩu',
                style: Theme.of(context).textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: AppColors.onSurface,
                    )),
          ],
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Partner Identity Card
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: AppShapes.radiusLg,
                  boxShadow: AppShapes.shadowSm,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 48,
                          height: 48,
                          decoration: BoxDecoration(
                            borderRadius: BorderRadius.circular(12),
                            color: AppColors.surfaceContainerLowest,
                            boxShadow: AppShapes.shadowSm,
                          ),
                          child: const Icon(Icons.sailing, color: AppColors.secondary, size: 28),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                    decoration: BoxDecoration(
                                      color: AppColors.secondaryContainer.withOpacity(0.6),
                                      borderRadius: BorderRadius.circular(12),
                                    ),
                                    child: const LocalizedText('DOC-8842',
                                        style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.onSecondaryContainer)),
                                  ),
                                  const SizedBox(width: 6),
                                  const Flexible(
                                    child: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        Icon(Icons.verified, size: 12, color: AppColors.secondary),
                                        SizedBox(width: 2),
                                        Flexible(
                                          child: LocalizedText(
                                            'Bến bãi chính thức',
                                            style: TextStyle(fontSize: 11, color: AppColors.tertiary),
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 2),
                              const LocalizedText('Trần Hải Đăng', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.onSurface), maxLines: 1, overflow: TextOverflow.ellipsis),
                              const LocalizedText('Danang Ocean Club • Mỹ Khê', style: TextStyle(fontSize: 12, color: AppColors.tertiary), maxLines: 1, overflow: TextOverflow.ellipsis),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLowest.withOpacity(0.8),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Row(
                        children: [
                          Icon(Icons.lock_outline, size: 16, color: AppColors.tertiary),
                          SizedBox(width: 8),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                LocalizedText('Email định danh đối tác', style: TextStyle(fontSize: 10, color: AppColors.tertiary)),
                                LocalizedText('partner@danangoceanclub.com', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppColors.onSurface), overflow: TextOverflow.ellipsis),
                              ],
                            ),
                          ),
                          SizedBox(width: 4),
                          LocalizedText('Chỉ đọc', style: TextStyle(fontSize: 10, color: AppColors.tertiary, fontWeight: FontWeight.bold)),
                        ],
                      ),
                    ),
                    const SizedBox(height: 6),
                    const Row(
                      children: [
                        Icon(Icons.info_outline, size: 13, color: AppColors.tertiary),
                        SizedBox(width: 4),
                        Expanded(
                          child: LocalizedText('Email bảo mật không thể thay đổi tại màn hình này để duy trì hợp đồng bến bãi.',
                              style: TextStyle(fontSize: 10, color: AppColors.tertiary)),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Mock feedback preview buttons
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerHigh.withOpacity(0.6),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  children: [
                    const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: LocalizedText(
                            'MÔ PHỎNG PHẢN HỒI HỆ THỐNG',
                            style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.tertiary),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        SizedBox(width: 6),
                        LocalizedText('Thử nghiệm', style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppColors.secondary)),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Row(
                      children: [
                        Expanded(
                          child: InkWell(
                            onTap: () {
                              setState(() {
                                _showSuccess = false;
                                _errorMessage = 'Mật khẩu hiện tại không chính xác hoặc xác nhận mật khẩu chưa trùng khớp. Vui lòng kiểm tra lại.';
                              });
                            },
                            child: Container(
                              padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 8),
                              decoration: BoxDecoration(
                                color: AppColors.surfaceContainerLowest,
                                borderRadius: BorderRadius.circular(20),
                                boxShadow: AppShapes.shadowSm,
                              ),
                              child: const Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(Icons.error_outline, size: 14, color: AppColors.primary),
                                  SizedBox(width: 4),
                                  Flexible(
                                    child: LocalizedText(
                                      'Mẫu lỗi',
                                      style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.primary),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: InkWell(
                            onTap: () {
                              setState(() {
                                _errorMessage = null;
                                _showSuccess = true;
                              });
                            },
                            child: Container(
                              padding: const EdgeInsets.symmetric(vertical: 6, horizontal: 8),
                              decoration: BoxDecoration(
                                color: AppColors.surfaceContainerLowest,
                                borderRadius: BorderRadius.circular(20),
                                boxShadow: AppShapes.shadowSm,
                              ),
                              child: const Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  Icon(Icons.check_circle_outline, size: 14, color: AppColors.secondary),
                                  SizedBox(width: 4),
                                  Flexible(
                                    child: LocalizedText(
                                      'Thành công',
                                      style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.secondary),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),

              // Alert banners
              if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.errorContainer,
                    borderRadius: AppShapes.radiusSm,
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.warning_amber_rounded, color: AppColors.error, size: 20),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const LocalizedText('Không thể cập nhật mật khẩu', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onErrorContainer)),
                            const SizedBox(height: 2),
                            LocalizedText(_errorMessage!, style: const TextStyle(fontSize: 11, color: AppColors.onErrorContainer)),
                          ],
                        ),
                      ),
                      IconButton(
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        icon: const Icon(Icons.close, size: 16, color: AppColors.onErrorContainer),
                        onPressed: () => setState(() => _errorMessage = null),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
              ],

              if (_showSuccess) ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.secondaryFixed,
                    borderRadius: AppShapes.radiusSm,
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Icon(Icons.verified_user, color: AppColors.secondary, size: 20),
                      const SizedBox(width: 8),
                      const Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            LocalizedText('Đổi mật khẩu thành công!', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSecondaryFixed)),
                            SizedBox(height: 2),
                            LocalizedText('Phiên làm việc của bạn đã được cập nhật chuẩn an toàn cao cấp DANASEA Core.',
                                style: TextStyle(fontSize: 11, color: AppColors.onSecondaryFixed)),
                          ],
                        ),
                      ),
                      IconButton(
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        icon: const Icon(Icons.close, size: 16, color: AppColors.onSecondaryFixed),
                        onPressed: () => setState(() => _showSuccess = false),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
              ],

              // Form fields
              Container(
                padding: const EdgeInsets.all(18),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLowest,
                  borderRadius: AppShapes.radiusLg,
                  boxShadow: AppShapes.shadowSm,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Field 1: Current password
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Expanded(
                          child: LocalizedText('Mật khẩu hiện tại', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.onSurface), overflow: TextOverflow.ellipsis),
                        ),
                        const SizedBox(width: 8),
                        InkWell(
                          onTap: () {
                            Navigator.of(context).push(
                              MaterialPageRoute(builder: (_) => const ForgotPasswordScreen()),
                            );
                          },
                          child: const LocalizedText('Quên mật khẩu?', style: TextStyle(fontSize: 12, color: AppColors.primary, fontWeight: FontWeight.w600)),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    VendorTextField(
                      controller: _currentPassController,
                      label: '',
                      hint: 'Nhập mật khẩu đang dùng',
                      prefixIcon: Icons.vpn_key_outlined,
                      obscureText: _obscureCurrent,
                      suffixIcon: IconButton(
                        icon: Icon(_obscureCurrent ? Icons.visibility : Icons.visibility_off, color: AppColors.tertiary, size: 20),
                        onPressed: () => setState(() => _obscureCurrent = !_obscureCurrent),
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Field 2: New password
                    const LocalizedText('Mật khẩu mới', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                    const SizedBox(height: 6),
                    VendorTextField(
                      controller: _newPassController,
                      label: '',
                      hint: 'Nhập mật khẩu mới',
                      prefixIcon: Icons.password_outlined,
                      obscureText: _obscureNew,
                      onChanged: (val) => setState(() {}),
                      suffixIcon: IconButton(
                        icon: Icon(_obscureNew ? Icons.visibility : Icons.visibility_off, color: AppColors.tertiary, size: 20),
                        onPressed: () => setState(() => _obscureNew = !_obscureNew),
                      ),
                    ),
                    const SizedBox(height: 8),

                    // Strength Meter
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLow,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Expanded(
                                child: LocalizedText('Độ mạnh mật khẩu:', style: TextStyle(fontSize: 11, color: AppColors.tertiary), overflow: TextOverflow.ellipsis),
                              ),
                              const SizedBox(width: 6),
                              LocalizedText(
                                newPass.isEmpty
                                    ? 'Chưa nhập'
                                    : strength == 1
                                        ? 'Yếu'
                                        : strength == 2
                                            ? 'Trung bình'
                                            : strength == 3
                                                ? 'Khá mạnh'
                                                : 'Tuyệt đối an toàn',
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                  color: newPass.isEmpty
                                      ? AppColors.tertiary
                                      : strength == 1
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
                            children: List.generate(4, (i) {
                              Color c = AppColors.surfaceContainerHighest;
                              if (newPass.isNotEmpty) {
                                if (strength == 1 && i == 0) c = AppColors.primaryContainer;
                                if (strength == 2 && i <= 1) c = AppColors.primaryContainer;
                                if (strength == 3 && i <= 2) c = AppColors.secondary;
                                if (strength >= 4) c = AppColors.secondary;
                              }
                              return Expanded(
                                child: Container(
                                  height: 4,
                                  margin: EdgeInsets.only(right: i < 3 ? 4 : 0),
                                  decoration: BoxDecoration(color: c, borderRadius: BorderRadius.circular(2)),
                                ),
                              );
                            }),
                          ),
                          const SizedBox(height: 10),
                          Row(
                            children: [
                              Expanded(child: _buildRule(hasLen, 'Ít nhất 8 ký tự')),
                              Expanded(child: _buildRule(hasUpperLower, 'Chữ hoa & thường')),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Row(
                            children: [
                              Expanded(child: _buildRule(hasNumber, 'Bao gồm số (0-9)')),
                              Expanded(child: _buildRule(hasSpecial, 'Ký tự đặc biệt (@#\$)')),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),

                    // Field 3: Confirm password
                    const LocalizedText('Xác nhận mật khẩu mới', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                    const SizedBox(height: 6),
                    VendorTextField(
                      controller: _confirmPassController,
                      label: '',
                      hint: 'Nhập lại mật khẩu mới',
                      prefixIcon: Icons.check_circle_outline,
                      obscureText: _obscureConfirm,
                      onChanged: (val) => setState(() {}),
                      suffixIcon: IconButton(
                        icon: Icon(_obscureConfirm ? Icons.visibility : Icons.visibility_off, color: AppColors.tertiary, size: 20),
                        onPressed: () => setState(() => _obscureConfirm = !_obscureConfirm),
                      ),
                    ),
                    if (_confirmPassController.text.isNotEmpty)
                      Padding(
                        padding: const EdgeInsets.only(top: 4, left: 4),
                        child: Row(
                          children: [
                            Icon(isMatch ? Icons.check : Icons.close, size: 14, color: isMatch ? AppColors.secondary : AppColors.error),
                            const SizedBox(width: 4),
                            Expanded(
                              child: LocalizedText(
                                isMatch ? 'Mật khẩu xác nhận hoàn toàn trùng khớp' : 'Mật khẩu chưa khớp',
                                style: TextStyle(fontSize: 11, color: isMatch ? AppColors.secondary : AppColors.error, fontWeight: FontWeight.bold),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                    const SizedBox(height: 20),

                    // Actions
                    VendorButton(
                      text: 'Cập nhật mật khẩu',
                      icon: Icons.save,
                      onPressed: _submit,
                    ),
                    const SizedBox(height: 8),
                    VendorButton(
                      text: 'Hủy bỏ',
                      variant: VendorButtonVariant.secondary,
                      onPressed: () => Navigator.of(context).maybePop(),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Security advisory
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: AppShapes.radiusMd,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          width: 32,
                          height: 32,
                          decoration: BoxDecoration(
                            shape: BoxShape.circle,
                            color: AppColors.secondaryFixed.withOpacity(0.5),
                          ),
                          child: const Icon(Icons.shield_outlined, color: AppColors.secondary, size: 18),
                        ),
                        const SizedBox(width: 8),
                        const Expanded(
                          child: LocalizedText('Khuyến nghị an ninh trạm bến DANASEA', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppColors.onSurface), maxLines: 1, overflow: TextOverflow.ellipsis),
                        ),
                      ],
                    ),
                    const SizedBox(height: 12),
                    _buildAdvisoryItem(Icons.update, 'Đổi mật khẩu định kỳ 90 ngày: Đặc biệt sau các đợt cao điểm du lịch biển hoặc thay đổi nhân sự phụ trách bến.'),
                    _buildAdvisoryItem(Icons.devices, 'Thiết bị công cộng: Luôn đăng xuất khỏi máy POS hoặc tablet tại quầy bãi biển khi kết thúc ca làm việc.'),
                    const Divider(height: 20),
                    const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: LocalizedText('Hỗ trợ kỹ thuật: 1900-6886', style: TextStyle(fontSize: 11, color: AppColors.tertiary, fontWeight: FontWeight.w600), overflow: TextOverflow.ellipsis),
                        ),
                        SizedBox(width: 8),
                        LocalizedText('24/7 Sea Ops', style: TextStyle(fontSize: 11, color: AppColors.secondary, fontWeight: FontWeight.bold)),
                      ],
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

  Widget _buildRule(bool passed, String text) {
    return Row(
      children: [
        Icon(passed ? Icons.check_circle : Icons.radio_button_unchecked, size: 13, color: passed ? AppColors.secondary : AppColors.tertiary),
        const SizedBox(width: 4),
        Expanded(
          child: LocalizedText(text,
              style: TextStyle(fontSize: 10, color: passed ? AppColors.secondary : AppColors.tertiary, fontWeight: passed ? FontWeight.bold : FontWeight.normal)),
        ),
      ],
    );
  }

  Widget _buildAdvisoryItem(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, size: 16, color: AppColors.secondary),
          const SizedBox(width: 8),
          Expanded(child: LocalizedText(text, style: const TextStyle(fontSize: 11, color: AppColors.tertiary))),
        ],
      ),
    );
  }
}
