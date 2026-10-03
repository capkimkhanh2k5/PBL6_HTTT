import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../../core/widgets/toast_notification.dart';
import '../../../../core/data/vendor_mock_database.dart';
import '../../../../core/data/vendor_mock_repositories.dart';

class EditProfileBankScreen extends StatefulWidget {
  const EditProfileBankScreen({super.key});

  @override
  State<EditProfileBankScreen> createState() => _EditProfileBankScreenState();
}

class _EditProfileBankScreenState extends State<EditProfileBankScreen> {
  final VendorProfileRepository _profileRepo = VendorProfileRepository();
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  late TextEditingController _fullNameController;
  late TextEditingController _phoneController;
  late TextEditingController _bankAccountController;
  late TextEditingController _bankHolderController;
  String _selectedBank = 'Vietcombank';

  bool _isSaving = false;
  bool _showSuccessAlert = false;

  final List<String> _bankOptions = [
    'Vietcombank',
    'BIDV',
    'Techcombank',
    'MB Bank',
    'VietinBank',
    'ACB',
    'VPBank',
  ];

  @override
  void initState() {
    super.initState();
    final user = _db.currentUser;
    final vendor = _db.currentVendor;

    _fullNameController = TextEditingController(text: user.fullName);
    _phoneController = TextEditingController(text: user.phone ?? '');
    _bankAccountController = TextEditingController(text: vendor.bankAccountNumber ?? '0041000889988');
    _bankHolderController = TextEditingController(text: vendor.bankAccountHolder ?? 'TRAN HAI DANG');
    if (vendor.bankName != null && _bankOptions.contains(vendor.bankName)) {
      _selectedBank = vendor.bankName!;
    }
  }

  @override
  void dispose() {
    _fullNameController.dispose();
    _phoneController.dispose();
    _bankAccountController.dispose();
    _bankHolderController.dispose();
    super.dispose();
  }

  Future<void> _handleSave() async {
    final fullName = _fullNameController.text.trim();
    final bankAccount = _bankAccountController.text.trim();
    final bankHolder = _bankHolderController.text.trim();

    if (fullName.isEmpty) {
      ToastNotification.show(context, message: 'Vui lòng nhập họ và tên đại diện.', type: ToastType.error);
      return;
    }
    if (bankAccount.isEmpty || bankHolder.isEmpty) {
      ToastNotification.show(context, message: 'Vui lòng nhập đầy đủ thông tin tài khoản ngân hàng.', type: ToastType.error);
      return;
    }

    setState(() => _isSaving = true);
    try {
      await _profileRepo.updateVendorProfile(
        fullName: fullName,
        phone: _phoneController.text.trim(),
      );
      await _profileRepo.updateBankProfile(
        bankName: _selectedBank,
        bankAccountNumber: bankAccount,
        bankAccountHolder: bankHolder.toUpperCase(),
      );

      if (!mounted) return;
      setState(() {
        _isSaving = false;
        _showSuccessAlert = true;
      });

      ToastNotification.show(
        context,
        message: 'Cập nhật thông tin hồ sơ và tài khoản thành công!',
        type: ToastType.success,
      );
    } catch (e) {
      if (!mounted) return;
      setState(() => _isSaving = false);
      ToastNotification.show(context, message: 'Lỗi cập nhật: $e', type: ToastType.error);
    }
  }

  @override
  Widget build(BuildContext context) {
    final user = _db.currentUser;
    final vendor = _db.currentVendor;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const LocalizedText('Hồ sơ & Ngân hàng'),
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.maybePop(context),
        ),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Success alert if triggered
            if (_showSuccessAlert) ...[
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.secondaryContainer,
                  borderRadius: BorderRadius.circular(AppShapes.rLg),
                  boxShadow: const [
                    BoxShadow(color: Color(0x0F000000), blurRadius: 6, offset: Offset(0, 2)),
                  ],
                ),
                child: Row(
                  children: [
                    Container(
                      width: 32,
                      height: 32,
                      decoration: const BoxDecoration(
                        color: AppColors.secondary,
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.verified, color: AppColors.onSecondary, size: 18),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          LocalizedText(
                            'Hồ sơ đối soát đã cập nhật!',
                            style: AppTypography.labelLg.copyWith(
                              color: AppColors.onSecondaryContainer,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          LocalizedText(
                            'Thông tin ngân hàng đã được cập nhật trong bản xem trước.',
                            style: AppTypography.bodySm.copyWith(
                              color: AppColors.onSecondaryContainer.withAlpha(220),
                            ),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, size: 18, color: AppColors.onSecondaryContainer),
                      onPressed: () => setState(() => _showSuccessAlert = false),
                    ),
                  ],
                ),
              ),
            ],

            // Ocean Partner Identity Header Badge
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                gradient: const LinearGradient(
                  colors: [AppColors.surfaceContainer, AppColors.surfaceContainerLow],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x08000000), blurRadius: 6, offset: Offset(0, 2)),
                ],
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Expanded(
                    child: Row(
                      children: [
                        Container(
                          width: 10,
                          height: 10,
                          decoration: const BoxDecoration(
                            color: AppColors.primaryContainer,
                            shape: BoxShape.circle,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: LocalizedText(
                            'ĐỐI TÁC ${vendor.businessName.toUpperCase()}',
                            style: AppTypography.labelSm.copyWith(
                              color: AppColors.secondary,
                              fontWeight: FontWeight.bold,
                              letterSpacing: 0.5,
                            ),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLowest,
                      borderRadius: BorderRadius.circular(AppShapes.rFull),
                    ),
                    child: LocalizedText(
                      'Mã: ${vendor.id}',
                      style: AppTypography.labelSm.copyWith(
                        color: AppColors.secondary,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // SECTION 1: Personal Representative Profile
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Row(
                          children: [
                            Container(
                              width: 32,
                              height: 32,
                              decoration: const BoxDecoration(
                                color: AppColors.secondaryContainer,
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(Icons.badge_outlined, color: AppColors.secondary, size: 18),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: LocalizedText(
                                'Đại diện pháp lý',
                                style: AppTypography.headlineSm.copyWith(color: AppColors.onSurface),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainer,
                          borderRadius: BorderRadius.circular(AppShapes.rFull),
                        ),
                        child: LocalizedText(
                          'Đã xác minh',
                          style: AppTypography.labelSm.copyWith(color: AppColors.secondary),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Avatar Zone
                  Row(
                    children: [
                      Stack(
                        children: [
                          CircleAvatar(
                            radius: 36,
                            backgroundColor: AppColors.surfaceContainer,
                            backgroundImage: user.avatarUrl != null
                                ? NetworkImage(user.avatarUrl!)
                                : null,
                            child: user.avatarUrl == null
                                ? const Icon(Icons.person, size: 36, color: AppColors.primary)
                                : null,
                          ),
                          Positioned(
                            bottom: 0,
                            right: 0,
                            child: Container(
                              width: 26,
                              height: 26,
                              decoration: const BoxDecoration(
                                color: AppColors.secondary,
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(Icons.photo_camera, size: 15, color: Colors.white),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            OutlinedButton.icon(
                              onPressed: () {
                                ToastNotification.show(
                                  context,
                                  message: 'Mô phỏng: Đã chọn ảnh chân dung mới.',
                                  type: ToastType.info,
                                );
                              },
                              icon: const Icon(Icons.cloud_upload_outlined, size: 16),
                              label: const LocalizedText('Thay đổi ảnh'),
                              style: OutlinedButton.styleFrom(
                                foregroundColor: AppColors.onSurface,
                                side: const BorderSide(color: AppColors.outlineVariant),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(AppShapes.rFull),
                                ),
                              ),
                            ),
                            const SizedBox(height: 4),
                            LocalizedText(
                              'Định dạng JPG, PNG dưới 5MB. Khuyên dùng ảnh chân dung rõ mặt.',
                              style: AppTypography.bodySm.copyWith(color: AppColors.tertiary),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),

                  // Full Name
                  LocalizedText('Họ và tên người đại diện *', style: AppTypography.labelMd.copyWith(color: AppColors.tertiary)),
                  const SizedBox(height: 6),
                  TextField(
                    controller: _fullNameController,
                    decoration: InputDecoration(
                      prefixIcon: const Icon(Icons.person_outline, color: AppColors.tertiary),
                      filled: true,
                      fillColor: AppColors.surfaceContainerLow,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(AppShapes.rFull),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Phone Number
                  LocalizedText('Số điện thoại liên hệ (tùy chọn)', style: AppTypography.labelMd.copyWith(color: AppColors.tertiary)),
                  const SizedBox(height: 6),
                  TextField(
                    controller: _phoneController,
                    keyboardType: TextInputType.phone,
                    decoration: InputDecoration(
                      prefixIcon: const Icon(Icons.phone_iphone, color: AppColors.tertiary),
                      filled: true,
                      fillColor: AppColors.surfaceContainerLow,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(AppShapes.rFull),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Email (Read-only)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: LocalizedText(
                          'Hộp thư nhận quyết toán',
                          style: AppTypography.labelMd.copyWith(color: AppColors.tertiary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Row(
                        children: [
                          const Icon(Icons.lock, size: 13, color: AppColors.secondary),
                          const SizedBox(width: 4),
                          LocalizedText('Được mã hóa', style: AppTypography.labelSm.copyWith(color: AppColors.secondary)),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  TextField(
                    readOnly: true,
                    controller: TextEditingController(text: user.email),
                    decoration: InputDecoration(
                      prefixIcon: const Icon(Icons.mark_email_read, color: AppColors.tertiary),
                      filled: true,
                      fillColor: AppColors.surfaceContainer,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(AppShapes.rFull),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(AppShapes.rMd),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.verified_user, color: AppColors.secondary, size: 16),
                        const SizedBox(width: 8),
                        Expanded(
                          child: LocalizedText(
                            'Email bảo mật xác minh không thể sửa trực tiếp. Vui lòng liên hệ Hotline quản trị nếu cần đổi.',
                            style: AppTypography.bodySm.copyWith(color: AppColors.tertiary, fontSize: 12),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),

            // SECTION 2: Bank Account for Payout
            Container(
              padding: const EdgeInsets.all(20),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.circular(AppShapes.rLg),
                boxShadow: const [
                  BoxShadow(color: Color(0x0A000000), blurRadius: 8, offset: Offset(0, 2)),
                ],
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Expanded(
                        child: Row(
                          children: [
                            Container(
                              width: 32,
                              height: 32,
                              decoration: const BoxDecoration(
                                color: AppColors.primaryFixed,
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(Icons.account_balance, color: AppColors.primary, size: 18),
                            ),
                            const SizedBox(width: 10),
                            Expanded(
                              child: LocalizedText(
                                'Tài khoản nhận tiền',
                                style: AppTypography.headlineSm.copyWith(color: AppColors.onSurface),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppColors.primaryFixed,
                          borderRadius: BorderRadius.circular(AppShapes.rFull),
                        ),
                        child: LocalizedText(
                          'Kỳ hạn Payout',
                          style: AppTypography.labelSm.copyWith(
                            color: AppColors.primary,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),

                  // Visual Payout Card
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      gradient: const LinearGradient(
                        colors: [AppColors.secondary, Color(0xFF004F57)],
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                      ),
                      borderRadius: BorderRadius.circular(AppShapes.rLg),
                      boxShadow: const [
                        BoxShadow(color: Color(0x24006874), blurRadius: 10, offset: Offset(0, 4)),
                      ],
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Row(
                                children: [
                                  const Icon(Icons.waves, color: AppColors.secondaryFixed, size: 20),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: LocalizedText(
                                      'DANASEA VENDOR PASS',
                                      style: AppTypography.labelSm.copyWith(
                                        color: AppColors.secondaryFixed,
                                        letterSpacing: 1.0,
                                      ),
                                      overflow: TextOverflow.ellipsis,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 8),
                            Icon(Icons.contactless, color: Colors.white.withAlpha(180), size: 22),
                          ],
                        ),
                        const SizedBox(height: 16),
                        LocalizedText(
                          'SỐ TÀI KHOẢN THỤ HƯỞNG',
                          style: AppTypography.labelSm.copyWith(color: AppColors.secondaryFixed),
                        ),
                        const SizedBox(height: 4),
                        LocalizedText(
                          _bankAccountController.text.isNotEmpty ? _bankAccountController.text : '•••• •••• ••••',
                          style: AppTypography.headlineSm.copyWith(
                            color: Colors.white,
                            letterSpacing: 2.0,
                            fontFamily: 'monospace',
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 16),
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  LocalizedText(
                                    'CHỦ TÀI KHOẢN',
                                    style: AppTypography.labelSm.copyWith(color: AppColors.secondaryFixed),
                                  ),
                                  const SizedBox(height: 2),
                                  LocalizedText(
                                    _bankHolderController.text.isNotEmpty ? _bankHolderController.text.toUpperCase() : 'TRAN HAI DANG',
                                    style: AppTypography.labelLg.copyWith(
                                      color: Colors.white,
                                      fontWeight: FontWeight.bold,
                                    ),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(width: 8),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                              decoration: BoxDecoration(
                                color: Colors.white.withAlpha(50),
                                borderRadius: BorderRadius.circular(AppShapes.rFull),
                              ),
                              child: LocalizedText(
                                _selectedBank,
                                style: AppTypography.labelSm.copyWith(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 20),

                  // Bank selector
                  LocalizedText('Ngân hàng thụ hưởng *', style: AppTypography.labelMd.copyWith(color: AppColors.tertiary)),
                  const SizedBox(height: 6),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(AppShapes.rFull),
                    ),
                    child: DropdownButtonHideUnderline(
                      child: DropdownButton<String>(
                        isExpanded: true,
                        value: _selectedBank,
                        items: _bankOptions.map((bank) {
                          return DropdownMenuItem<String>(
                            value: bank,
                            child: Row(
                              children: [
                                const Icon(Icons.account_balance, size: 18, color: AppColors.secondary),
                                const SizedBox(width: 10),
                                LocalizedText(bank, style: AppTypography.bodyMd.copyWith(color: AppColors.onSurface)),
                              ],
                            ),
                          );
                        }).toList(),
                        onChanged: (val) {
                          if (val != null) setState(() => _selectedBank = val);
                        },
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Account Number Input
                  LocalizedText('Số tài khoản *', style: AppTypography.labelMd.copyWith(color: AppColors.tertiary)),
                  const SizedBox(height: 6),
                  TextField(
                    controller: _bankAccountController,
                    keyboardType: TextInputType.number,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      prefixIcon: const Icon(Icons.pin, color: AppColors.tertiary),
                      filled: true,
                      fillColor: AppColors.surfaceContainerLow,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(AppShapes.rFull),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    ),
                  ),
                  const SizedBox(height: 16),

                  // Account Holder Name Input
                  LocalizedText('Tên chủ tài khoản (in hoa không dấu) *', style: AppTypography.labelMd.copyWith(color: AppColors.tertiary)),
                  const SizedBox(height: 6),
                  TextField(
                    controller: _bankHolderController,
                    textCapitalization: TextCapitalization.characters,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      prefixIcon: const Icon(Icons.person, color: AppColors.tertiary),
                      filled: true,
                      fillColor: AppColors.surfaceContainerLow,
                      border: OutlineInputBorder(
                        borderRadius: BorderRadius.circular(AppShapes.rFull),
                        borderSide: BorderSide.none,
                      ),
                      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Save Button
            SizedBox(
              width: double.infinity,
              height: 52,
              child: ElevatedButton(
                onPressed: _isSaving ? null : _handleSave,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primary,
                  foregroundColor: Colors.white,
                  elevation: 2,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(AppShapes.rFull)),
                ),
                child: _isSaving
                    ? const SizedBox(
                        width: 22,
                        height: 22,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.save_outlined, size: 20),
                          const SizedBox(width: 8),
                          Flexible(
                            child: LocalizedText(
                              'Lưu thay đổi hồ sơ',
                              style: AppTypography.labelLg.copyWith(color: Colors.white),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                        ],
                      ),
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}
