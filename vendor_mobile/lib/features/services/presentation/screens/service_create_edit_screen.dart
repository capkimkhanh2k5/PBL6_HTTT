import 'package:vendor_mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/data/vendor_mock_database.dart';
import '../../../../core/models/vendor_models.dart';
import '../../../../core/widgets/vendor_button.dart';
import '../../../../core/widgets/vendor_text_field.dart';
import '../../../../core/widgets/toast_notification.dart';
import 'service_safety_cert_screen.dart';

class ServiceCreateEditScreen extends StatefulWidget {
  final ServiceModel? service;

  const ServiceCreateEditScreen({super.key, this.service});

  @override
  State<ServiceCreateEditScreen> createState() => _ServiceCreateEditScreenState();
}

class _ServiceCreateEditScreenState extends State<ServiceCreateEditScreen> {
  final _db = VendorMockDatabase.instance;

  final TextEditingController _nameViController = TextEditingController();
  final TextEditingController _nameEnController = TextEditingController();
  final TextEditingController _slugController = TextEditingController();
  final TextEditingController _descViController = TextEditingController();
  final TextEditingController _descEnController = TextEditingController();
  final TextEditingController _priceController = TextEditingController();
  final TextEditingController _durationController = TextEditingController();
  final TextEditingController _capacityController = TextEditingController();
  final TextEditingController _meetingPointNameController = TextEditingController();
  final TextEditingController _meetingPointAddressController = TextEditingController();
  final TextEditingController _latController = TextEditingController();
  final TextEditingController _lngController = TextEditingController();

  String _category = 'WATER_SPORTS';
  String _descTab = 'vi';
  List<String> _images = [];

  bool get _isEdit => widget.service != null;

  String _normalizeCategory(String cat) {
    switch (cat) {
      case 'cat-001':
      case 'WATER_SPORTS':
        return 'WATER_SPORTS';
      case 'cat-002':
      case 'DIVING':
        return 'DIVING';
      case 'cat-003':
      case 'SURFING':
        return 'SURFING';
      case 'cat-004':
      case 'BOAT_CRUISE':
        return 'BOAT_CRUISE';
      default:
        return 'WATER_SPORTS';
    }
  }

  @override
  void initState() {
    super.initState();
    if (_isEdit) {
      final s = widget.service!;
      _nameViController.text = s.nameVi;
      _nameEnController.text = s.nameEn;
      _slugController.text = s.slug;
      _descViController.text = s.descriptionVi;
      _descEnController.text = s.descriptionEn;
      _priceController.text = s.price.toStringAsFixed(0);
      _durationController.text = s.durationMinutes.toString();
      _capacityController.text = s.maxCapacity.toString();
      _meetingPointNameController.text = s.meetingPointName;
      _meetingPointAddressController.text = s.meetingPointAddress;
      _latController.text = s.latitude.toString();
      _lngController.text = s.longitude.toString();
      _category = _normalizeCategory(s.category);
      _images = List.from(s.images);
    } else {
      _nameViController.text = 'Chèo SUP ngắm bình minh biển Mỹ Khê';
      _nameEnController.text = 'Sunrise Stand-up Paddleboarding at My Khe Beach';
      _slugController.text = 'cheo-sup-binh-minh-my-khe';
      _descViController.text =
          'Hành trình xuất phát từ bãi cát Mỹ Khê lúc bình minh 5:00 sáng. Huấn luyện viên chuyên nghiệp kèm cặp, cung cấp áo phao chất lượng cao, ván SUP composite và mái chèo chuẩn thi đấu.';
      _descEnController.text =
          'Depart from pristine My Khe beach at dawn (5:00 AM). Accompanied by certified ocean coaches, high-end composite paddleboards, and safety gear.';
      _priceController.text = '280000';
      _durationController.text = '120';
      _capacityController.text = '10';
      _meetingPointNameController.text = 'Bến bãi Danang Ocean Club - Bãi biển Mỹ Khê';
      _meetingPointAddressController.text = 'Lô 12 Võ Nguyên Giáp, Phường Phước Mỹ, Quận Sơn Trà, Đà Nẵng';
      _latController.text = '16.0601';
      _lngController.text = '108.2472';
      _images = [
        'https://images.unsplash.com/photo-1544551763-46a013bb70d5',
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e',
      ];
    }
  }

  @override
  void dispose() {
    _nameViController.dispose();
    _nameEnController.dispose();
    _slugController.dispose();
    _descViController.dispose();
    _descEnController.dispose();
    _priceController.dispose();
    _durationController.dispose();
    _capacityController.dispose();
    _meetingPointNameController.dispose();
    _meetingPointAddressController.dispose();
    _latController.dispose();
    _lngController.dispose();
    super.dispose();
  }

  ServiceModel _buildServiceModel() {
    return ServiceModel(
      id: widget.service?.id ?? widget.service?.serviceId ?? 'srv-${DateTime.now().millisecondsSinceEpoch % 10000}',
      vendorId: widget.service?.vendorId ?? 'vnd-001',
      categoryId: _category,
      name: _nameViController.text.trim(),
      nameEn: _nameEnController.text.trim(),
      slug: _slugController.text.trim(),
      description: _descViController.text.trim(),
      descriptionEn: _descEnController.text.trim(),
      price: double.tryParse(_priceController.text.replaceAll('.', '')) ?? 280000,
      durationMinutes: int.tryParse(_durationController.text) ?? 120,
      capacityPerSlot: int.tryParse(_capacityController.text) ?? 10,
      locationName: _meetingPointNameController.text.trim(),
      address: _meetingPointAddressController.text.trim(),
      latitude: double.tryParse(_latController.text) ?? 16.0601,
      longitude: double.tryParse(_lngController.text) ?? 108.2472,
      weatherSensitive: widget.service?.weatherSensitive ?? true,
      minWindKmh: widget.service?.minWindKmh ?? 0.0,
      maxWaveM: widget.service?.maxWaveM ?? 1.2,
      waiverContent: widget.service?.waiverContent ??
          'Khách cam kết biết bơi cơ bản, luôn mặc áo phao tiêu chuẩn trong suốt hành trình chèo và tuân thủ tuyệt đối tín hiệu điều phối của huấn luyện viên Danang Ocean Club.',
      status: widget.service?.status ?? ServiceStatus.draft,
      imageUrls: _images,
      createdAt: widget.service?.createdAt ?? DateTime.now(),
      updatedAt: DateTime.now(),
    );
  }

  void _saveDraftAndProceed() {
    final updated = _buildServiceModel();
    _db.saveService(updated);
    ToastNotification.showSuccess(context, 'Đã lưu thông tin cơ bản');
    Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => ServiceSafetyCertScreen(service: updated),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: AppColors.secondary, size: 20),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: LocalizedText(
          _isEdit ? 'Sửa dịch vụ' : 'Tạo mới dịch vụ',
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: AppColors.secondary),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Vendor Mini Banner
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLow,
                  borderRadius: AppShapes.radiusMd,
                  boxShadow: AppShapes.shadowSm,
                ),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: Row(
                        children: [
                          Container(
                            width: 32,
                            height: 32,
                            decoration: const BoxDecoration(
                              shape: BoxShape.circle,
                              color: AppColors.secondaryContainer,
                            ),
                            child: const Icon(Icons.kayaking, size: 18, color: AppColors.onSecondaryContainer),
                          ),
                          const SizedBox(width: 8),
                          const Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                LocalizedText('NHÀ CUNG CẤP', style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: AppColors.secondary)),
                                LocalizedText('Danang Ocean Club', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.onSurface), overflow: TextOverflow.ellipsis),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerHigh,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const LocalizedText('Bước 1/3', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.onSurfaceVariant)),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 12),

              // Progress Bar
              Row(
                children: [
                  Expanded(
                    child: Container(height: 5, decoration: BoxDecoration(color: AppColors.primary, borderRadius: BorderRadius.circular(3))),
                  ),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Container(height: 5, decoration: BoxDecoration(color: AppColors.surfaceContainerHighest, borderRadius: BorderRadius.circular(3))),
                  ),
                  const SizedBox(width: 4),
                  Expanded(
                    child: Container(height: 5, decoration: BoxDecoration(color: AppColors.surfaceContainerHighest, borderRadius: BorderRadius.circular(3))),
                  ),
                ],
              ),
              const SizedBox(height: 6),
              const Row(
                children: [
                  Expanded(
                    child: LocalizedText('1. Trải nghiệm',
                      style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.primary),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  SizedBox(width: 4),
                  Expanded(
                    child: LocalizedText('2. An toàn',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 11, color: AppColors.tertiary),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  SizedBox(width: 4),
                  Expanded(
                    child: LocalizedText('3. Lịch trình',
                      textAlign: TextAlign.end,
                      style: TextStyle(fontSize: 11, color: AppColors.tertiary),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),

              // SECTION 1: Thông tin cơ bản
              _buildSectionCard(
                title: 'Thông tin cơ bản',
                icon: Icons.edit_note,
                badge: 'BẮT BUỘC',
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    VendorTextField(
                      controller: _nameViController,
                      label: 'Tên dịch vụ (Tiếng Việt)',
                      isRequired: true,
                      hint: 'Nhập tên trải nghiệm bằng tiếng Việt',
                    ),
                    const SizedBox(height: 12),
                    VendorTextField(
                      controller: _nameEnController,
                      label: 'Tên dịch vụ (Tiếng Anh)',
                      isRequired: false,
                      hint: 'Nhập tên dịch vụ tiếng Anh',
                    ),
                    const SizedBox(height: 12),
                    const LocalizedText('Đường dẫn định danh (Slug)', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                    const SizedBox(height: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 2),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLow,
                        borderRadius: BorderRadius.circular(24),
                      ),
                      child: Row(
                        children: [
                          const LocalizedText('danasea.vn/tours/', style: TextStyle(fontSize: 11, color: AppColors.secondary, fontWeight: FontWeight.bold)),
                          Expanded(
                            child: TextField(
                              controller: _slugController,
                              style: const TextStyle(fontSize: 12),
                              decoration: const InputDecoration(border: InputBorder.none, contentPadding: EdgeInsets.zero),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    const LocalizedText('Danh mục trải nghiệm', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                    const SizedBox(height: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLow,
                        borderRadius: BorderRadius.circular(24),
                      ),
                      child: DropdownButtonHideUnderline(
                        child: DropdownButton<String>(
                          value: _category,
                          isExpanded: true,
                          items: const [
                            DropdownMenuItem(value: 'WATER_SPORTS', child: LocalizedText('Thể thao nước (Chèo SUP / Kayak)', style: TextStyle(fontSize: 13))),
                            DropdownMenuItem(value: 'DIVING', child: LocalizedText('Lặn ngắm san hô (Snorkeling & Diving)', style: TextStyle(fontSize: 13))),
                            DropdownMenuItem(value: 'SURFING', child: LocalizedText('Lướt ván diều & Lướt sóng (Surfing)', style: TextStyle(fontSize: 13))),
                            DropdownMenuItem(value: 'BOAT_CRUISE', child: LocalizedText('Du thuyền khám phá bán đảo Sơn Trà', style: TextStyle(fontSize: 13))),
                          ],
                          onChanged: (val) {
                            if (val != null) setState(() => _category = val);
                          },
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),

                    // Multi-language Description Tabs
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Expanded(
                          child: LocalizedText(
                            'Mô tả chi tiết lịch trình',
                            style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.all(2),
                          decoration: BoxDecoration(color: AppColors.surfaceContainer, borderRadius: BorderRadius.circular(16)),
                          child: Row(
                            children: [
                              _buildTabPill('vi', 'Tiếng Việt'),
                              _buildTabPill('en', 'Tiếng Anh'),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    TextField(
                      controller: _descTab == 'vi' ? _descViController : _descEnController,
                      maxLines: 4,
                      style: const TextStyle(fontSize: 12),
                      decoration: InputDecoration(
                        filled: true,
                        fillColor: AppColors.surfaceContainerLow,
                        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: BorderSide.none),
                        hintText: tr(context, _descTab == 'vi' ? 'Mô tả hành trình bằng tiếng Việt...' : 'Tour itinerary in English...'),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // SECTION 2: Vận hành & Đơn giá duy nhất
              _buildSectionCard(
                title: 'Vận hành & Đơn giá',
                icon: Icons.monetization_on,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLow,
                        borderRadius: AppShapes.radiusSm,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Expanded(
                                child: LocalizedText(
                                  'Đơn giá duy nhất',
                                  style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                decoration: BoxDecoration(color: AppColors.secondary, borderRadius: BorderRadius.circular(10)),
                                child: const LocalizedText('1 mức giá cố định', style: TextStyle(fontSize: 10, color: Colors.white, fontWeight: FontWeight.bold)),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                            decoration: BoxDecoration(
                              color: AppColors.surfaceContainerLowest,
                              borderRadius: BorderRadius.circular(24),
                            ),
                            child: Row(
                              children: [
                                Expanded(
                                  child: TextField(
                                    controller: _priceController,
                                    keyboardType: TextInputType.number,
                                    style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: AppColors.primary),
                                    decoration: const InputDecoration(border: InputBorder.none, contentPadding: EdgeInsets.zero),
                                  ),
                                ),
                                const LocalizedText('VNĐ', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.primary)),
                                const SizedBox(width: 4),
                                const LocalizedText('/ khách', style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
                              ],
                            ),
                          ),
                          const SizedBox(height: 6),
                          const Row(
                            children: [
                              Icon(Icons.info_outline, size: 12, color: AppColors.secondary),
                              SizedBox(width: 4),
                              Expanded(
                                child: LocalizedText('Giá trọn gói đã bao gồm thiết bị chèo, áo phao và hướng dẫn viên.',
                                    style: TextStyle(fontSize: 10, color: AppColors.tertiary)),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: VendorTextField(
                            controller: _durationController,
                            label: 'Thời lượng (phút)',
                            hint: '120',
                            keyboardType: TextInputType.number,
                            prefixIcon: Icons.schedule,
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: VendorTextField(
                            controller: _capacityController,
                            label: 'Sức chứa / slot',
                            hint: '10',
                            keyboardType: TextInputType.number,
                            prefixIcon: Icons.groups,
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // SECTION 3: Điểm đón khách
              _buildSectionCard(
                title: 'Điểm đón khách',
                icon: Icons.location_on,
                child: Column(
                  children: [
                    VendorTextField(
                      controller: _meetingPointNameController,
                      label: 'Tên điểm hẹn tập kết',
                      hint: 'Bến bãi Danang Ocean Club',
                    ),
                    const SizedBox(height: 12),
                    VendorTextField(
                      controller: _meetingPointAddressController,
                      label: 'Địa chỉ cụ thể',
                      hint: 'Lô 12 Võ Nguyên Giáp, Sơn Trà, Đà Nẵng',
                    ),
                    const SizedBox(height: 12),
                    Row(
                      children: [
                        Expanded(
                          child: VendorTextField(
                            controller: _latController,
                            label: 'Vĩ độ (Latitude)',
                            hint: '16.0601',
                            keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: VendorTextField(
                            controller: _lngController,
                            label: 'Kinh độ (Longitude)',
                            hint: '108.2472',
                            keyboardType: const TextInputType.numberWithOptions(decimal: true),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // SECTION 4: Quản lý hình ảnh
              _buildSectionCard(
                title: 'Hình ảnh dịch vụ',
                icon: Icons.photo_library,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (_images.isEmpty)
                      const LocalizedText('Chưa có hình ảnh nào', style: TextStyle(fontSize: 12, color: AppColors.tertiary))
                    else
                      ListView.separated(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        itemCount: _images.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemBuilder: (context, i) {
                          return Container(
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: AppColors.surfaceContainerLow,
                              borderRadius: BorderRadius.circular(10),
                            ),
                            child: Row(
                              children: [
                                Container(
                                  width: 48,
                                  height: 40,
                                  decoration: BoxDecoration(
                                    color: AppColors.surfaceContainerHighest,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: const Icon(Icons.image, color: AppColors.tertiary, size: 20),
                                ),
                                const SizedBox(width: 10),
                                Expanded(
                                  child: LocalizedText(i == 0 ? 'Ảnh bìa đại diện' : 'Ảnh ${i + 1}',
                                      style: TextStyle(
                                        fontSize: 12,
                                        fontWeight: i == 0 ? FontWeight.bold : FontWeight.normal,
                                        color: i == 0 ? AppColors.secondary : AppColors.onSurface,
                                      )),
                                ),
                                IconButton(
                                  icon: const Icon(Icons.arrow_upward, size: 16),
                                  onPressed: i > 0
                                      ? () {
                                          setState(() {
                                            final item = _images.removeAt(i);
                                            _images.insert(i - 1, item);
                                          });
                                        }
                                      : null,
                                ),
                                IconButton(
                                  icon: const Icon(Icons.arrow_downward, size: 16),
                                  onPressed: i < _images.length - 1
                                      ? () {
                                          setState(() {
                                            final item = _images.removeAt(i);
                                            _images.insert(i + 1, item);
                                          });
                                        }
                                      : null,
                                ),
                                IconButton(
                                  icon: const Icon(Icons.delete_outline, color: AppColors.error, size: 18),
                                  onPressed: () {
                                    setState(() {
                                      _images.removeAt(i);
                                    });
                                  },
                                ),
                              ],
                            ),
                          );
                        },
                      ),
                    const SizedBox(height: 10),
                    OutlinedButton.icon(
                      onPressed: () {
                        setState(() {
                          _images.add('https://images.unsplash.com/photo-${DateTime.now().millisecondsSinceEpoch}');
                        });
                        ToastNotification.showInfo(context, 'Đã thêm ảnh mới');
                      },
                      icon: const Icon(Icons.add_photo_alternate, size: 16),
                      label: const LocalizedText('Thêm ảnh trải nghiệm', style: TextStyle(fontSize: 12)),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: AppColors.secondary,
                        side: const BorderSide(color: AppColors.secondary),
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                      ),
                    ),
                    const SizedBox(height: 4),
                    const LocalizedText('Ảnh đầu tiên là ảnh bìa. Dùng mũi tên để thay đổi thứ tự.', style: TextStyle(fontSize: 10, color: AppColors.tertiary)),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // Action Buttons
              Row(
                children: [
                  Expanded(
                    child: VendorButton(
                      text: 'Hủy thay đổi',
                      variant: VendorButtonVariant.secondary,
                      onPressed: () => Navigator.of(context).maybePop(),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    flex: 2,
                    child: VendorButton(
                      text: 'Sang bước an toàn',
                      icon: Icons.arrow_forward,
                      onPressed: _saveDraftAndProceed,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSectionCard({
    required String title,
    required IconData icon,
    String? badge,
    required Widget child,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusMd,
        boxShadow: AppShapes.shadowSm,
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
                    Icon(icon, color: AppColors.secondary, size: 20),
                    const SizedBox(width: 8),
                    Expanded(
                      child: LocalizedText(
                        title,
                        style: const TextStyle(fontSize: 15, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ),
              if (badge != null) ...[
                const SizedBox(width: 8),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(color: AppColors.primaryFixed.withOpacity(0.5), borderRadius: BorderRadius.circular(10)),
                  child: LocalizedText(badge, style: const TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: AppColors.primary)),
                ),
              ],
            ],
          ),
          const SizedBox(height: 14),
          child,
        ],
      ),
    );
  }

  Widget _buildTabPill(String tab, String label) {
    final active = _descTab == tab;
    return GestureDetector(
      onTap: () => setState(() => _descTab = tab),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
        decoration: BoxDecoration(
          color: active ? AppColors.surfaceContainerLowest : Colors.transparent,
          borderRadius: BorderRadius.circular(14),
          boxShadow: active ? AppShapes.shadowSm : null,
        ),
        child: LocalizedText(
          label,
          style: TextStyle(
            fontSize: 10,
            fontWeight: active ? FontWeight.bold : FontWeight.normal,
            color: active ? AppColors.primary : AppColors.tertiary,
          ),
        ),
      ),
    );
  }
}
