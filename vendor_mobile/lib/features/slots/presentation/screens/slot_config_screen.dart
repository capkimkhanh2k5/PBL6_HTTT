import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/data/vendor_mock_database.dart';
import '../../../../core/models/vendor_models.dart';
import '../../../../core/widgets/vendor_button.dart';
import '../../../../core/widgets/toast_notification.dart';

class SlotConfigScreen extends StatefulWidget {
  final ServiceSlotModel? slot;
  final String serviceId;
  final String? initialDate;

  const SlotConfigScreen({
    super.key,
    this.slot,
    this.serviceId = 'srv-001',
    this.initialDate,
  });

  @override
  State<SlotConfigScreen> createState() => _SlotConfigScreenState();
}

class _SlotConfigScreenState extends State<SlotConfigScreen> {
  final _db = VendorMockDatabase.instance;

  late TextEditingController _dateController;
  late TextEditingController _startTimeController;
  late TextEditingController _endTimeController;
  late int _capacity;
  late bool _isBlocked;
  bool _repeatWeekdays = false;

  bool get _isEdit => widget.slot != null;
  int get _bookedCount => widget.slot?.bookedCount ?? 0;
  int get _heldCount => widget.slot?.heldCount ?? 0;
  int get _minAllowedCapacity => _bookedCount + _heldCount;

  @override
  void initState() {
    super.initState();
    if (_isEdit) {
      final s = widget.slot!;
      _dateController = TextEditingController(text: s.slotDate);
      _startTimeController = TextEditingController(text: s.startTime.substring(0, 5));
      _endTimeController = TextEditingController(text: s.endTime.substring(0, 5));
      _capacity = s.capacity;
      _isBlocked = s.isBlocked;
    } else {
      _dateController = TextEditingController(text: widget.initialDate ?? '2025-05-04');
      _startTimeController = TextEditingController(text: '05:00');
      _endTimeController = TextEditingController(text: '07:00');
      _capacity = 10;
      _isBlocked = false;
    }
  }

  @override
  void dispose() {
    _dateController.dispose();
    _startTimeController.dispose();
    _endTimeController.dispose();
    super.dispose();
  }

  void _changeCapacity(int delta) {
    final newCap = _capacity + delta;
    if (newCap < _minAllowedCapacity) {
      ToastNotification.showError(
        context,
        'Không thể giảm sức chứa dưới tổng số khách đã đặt & tạm giữ ($_minAllowedCapacity khách)',
      );
      return;
    }
    if (newCap < 1) return;
    setState(() {
      _capacity = newCap;
    });
  }

  void _saveSlot() {
    final date = _dateController.text.trim();
    final start = _startTimeController.text.trim();
    final end = _endTimeController.text.trim();

    if (_capacity < _minAllowedCapacity) {
      ToastNotification.showError(
        context,
        'Sức chứa tối thiểu phải từ $_minAllowedCapacity khách trở lên',
      );
      return;
    }

    final newSlot = ServiceSlotModel(
      slotId: widget.slot?.slotId ?? 'slt-${DateTime.now().millisecondsSinceEpoch % 10000}',
      serviceId: widget.serviceId,
      slotDate: date,
      startTime: '$start:00',
      endTime: '$end:00',
      capacity: _capacity,
      bookedCount: _bookedCount,
      heldCount: _heldCount,
      isBlocked: _isBlocked,
      createdAt: widget.slot?.createdAt ?? DateTime.now(),
      updatedAt: DateTime.now(),
    );

    _db.saveSlot(newSlot);
    ToastNotification.showSuccess(context, 'Đã lưu cấu hình khung giờ thành công');
    Navigator.of(context).maybePop();
  }

  @override
  Widget build(BuildContext context) {
    final service = _db.services.firstWhere(
      (s) => s.serviceId == widget.serviceId,
      orElse: () => _db.services.first,
    );

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.secondary),
          onPressed: () => Navigator.of(context).maybePop(),
        ),
        title: Text(
          _isEdit ? 'Cấu hình khung giờ' : 'Thêm khung giờ mới',
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18, color: AppColors.onSurface),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Service Header Info
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLowest,
                  borderRadius: AppShapes.radiusMd,
                  boxShadow: AppShapes.shadowSm,
                ),
                child: Row(
                  children: [
                    Container(
                      width: 48,
                      height: 48,
                      decoration: BoxDecoration(
                        color: AppColors.secondaryContainer,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Icon(Icons.kayaking, color: AppColors.onSecondaryContainer, size: 24),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(color: AppColors.secondaryContainer, borderRadius: BorderRadius.circular(8)),
                            child: const Text('DỊCH VỤ ĐANG CHỌN', style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: AppColors.onSecondaryContainer)),
                          ),
                          const SizedBox(height: 4),
                          Text(service.nameVi, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.onSurface), maxLines: 1, overflow: TextOverflow.ellipsis),
                          const SizedBox(height: 2),
                          Row(
                            children: [
                              const Icon(Icons.location_on, size: 12, color: AppColors.secondary),
                              const SizedBox(width: 3),
                              Expanded(
                                child: Text(service.meetingPointName, style: const TextStyle(fontSize: 11, color: AppColors.tertiary), maxLines: 1, overflow: TextOverflow.ellipsis),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // CARD 1: Date & Recurrence
              Container(
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
                        const Expanded(
                          child: Row(
                            children: [
                              Icon(Icons.calendar_today, size: 18, color: AppColors.primary),
                              SizedBox(width: 6),
                              Expanded(
                                child: Text('Ngày áp dụng',
                                    style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                                    maxLines: 1,
                                    overflow: TextOverflow.ellipsis),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(color: AppColors.secondaryFixed, borderRadius: BorderRadius.circular(10)),
                          child: const Text('Hôm nay', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.onSecondaryFixed)),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    InkWell(
                      onTap: () async {
                        final picked = await showDatePicker(
                          context: context,
                          initialDate: DateTime.tryParse(_dateController.text) ?? DateTime(2025, 5, 4),
                          firstDate: DateTime(2025, 1, 1),
                          lastDate: DateTime(2026, 12, 31),
                        );
                        if (picked != null) {
                          setState(() {
                            _dateController.text =
                                '${picked.year}-${picked.month.toString().padLeft(2, '0')}-${picked.day.toString().padLeft(2, '0')}';
                          });
                        }
                      },
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceContainerLow,
                          borderRadius: BorderRadius.circular(24),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.event, size: 20, color: AppColors.tertiary),
                            const SizedBox(width: 10),
                            Expanded(
                              child: Text(_dateController.text, style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600, color: AppColors.onSurface)),
                            ),
                            const Icon(Icons.edit_calendar, size: 18, color: AppColors.secondary),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 10),
                    Material(
                      color: Colors.transparent,
                      child: CheckboxListTile(
                        value: _repeatWeekdays,
                        onChanged: (val) => setState(() => _repeatWeekdays = val ?? false),
                        contentPadding: EdgeInsets.zero,
                        controlAffinity: ListTileControlAffinity.leading,
                        title: const Text('Lặp lại cho các ngày trong tuần (T2 - T6)', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                        subtitle: const Text('Tạo khung giờ tương ứng cho các ngày làm việc', style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // CARD 2: Operating Time
              Container(
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
                        const Expanded(
                          child: Row(
                            children: [
                              Icon(Icons.schedule, size: 18, color: AppColors.secondary),
                              SizedBox(width: 6),
                              Expanded(
                                child: Text(
                                  'Khung giờ hoạt động',
                                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(color: AppColors.primaryFixed, borderRadius: BorderRadius.circular(10)),
                          child: Text('Thời lượng: ${service.durationMinutes} phút',
                              style: const TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppColors.onPrimaryFixedVariant)),
                        ),
                      ],
                    ),
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Giờ bắt đầu', style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
                              const SizedBox(height: 4),
                              TextField(
                                controller: _startTimeController,
                                textAlign: TextAlign.center,
                                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                                decoration: InputDecoration(
                                  filled: true,
                                  fillColor: AppColors.surfaceContainerLow,
                                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(24), borderSide: BorderSide.none),
                                  prefixIcon: const Icon(Icons.wb_twilight, size: 18, color: AppColors.secondary),
                                  contentPadding: const EdgeInsets.symmetric(vertical: 10),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('Giờ kết thúc', style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
                              const SizedBox(height: 4),
                              TextField(
                                controller: _endTimeController,
                                textAlign: TextAlign.center,
                                style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                                decoration: InputDecoration(
                                  filled: true,
                                  fillColor: AppColors.surfaceContainerLow,
                                  border: OutlineInputBorder(borderRadius: BorderRadius.circular(24), borderSide: BorderSide.none),
                                  prefixIcon: const Icon(Icons.wb_sunny, size: 18, color: AppColors.primaryContainer),
                                  contentPadding: const EdgeInsets.symmetric(vertical: 10),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(color: AppColors.surfaceContainerLow, borderRadius: BorderRadius.circular(8)),
                      child: const Row(
                        children: [
                          Icon(Icons.tsunami, size: 14, color: AppColors.secondary),
                          SizedBox(width: 6),
                          Expanded(
                            child: Text('Thời điểm lý tưởng: Mặt biển phẳng lặng, độ cao sóng 0.3m', style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),

              // CARD 3: Capacity & Booked Stepper
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: AppColors.surfaceContainerLowest,
                  borderRadius: AppShapes.radiusMd,
                  boxShadow: AppShapes.shadowSm,
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Row(
                            children: [
                              Icon(Icons.groups, size: 18, color: AppColors.primary),
                              SizedBox(width: 6),
                              Expanded(
                                child: Text(
                                  'Sức chứa tối đa (Capacity)',
                                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                                  maxLines: 1,
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ),
                        SizedBox(width: 8),
                        Text('Sức chứa thiết lập', style: TextStyle(fontSize: 11, color: AppColors.tertiary)),
                      ],
                    ),
                    const SizedBox(height: 14),

                    // Capacity Stepper
                    Row(
                      children: [
                        InkWell(
                          onTap: () => _changeCapacity(-1),
                          child: Container(
                            width: 44,
                            height: 44,
                            decoration: BoxDecoration(
                              shape: BoxShape.circle,
                              color: AppColors.surfaceContainerLow,
                            ),
                            child: const Icon(Icons.remove, color: AppColors.onSurface, size: 20),
                          ),
                        ),
                        Expanded(
                          child: Center(
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              crossAxisAlignment: CrossAxisAlignment.baseline,
                              textBaseline: TextBaseline.alphabetic,
                              children: [
                                Text('$_capacity', style: const TextStyle(fontSize: 26, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                                const SizedBox(width: 4),
                                const Text('khách', style: TextStyle(fontSize: 13, color: AppColors.tertiary)),
                              ],
                            ),
                          ),
                        ),
                        InkWell(
                          onTap: () => _changeCapacity(1),
                          child: Container(
                            width: 44,
                            height: 44,
                            decoration: const BoxDecoration(
                              shape: BoxShape.circle,
                              color: AppColors.secondaryContainer,
                            ),
                            child: const Icon(Icons.add, color: AppColors.onSecondaryContainer, size: 20),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),

                    // Read-only Booked & Held Info Box
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainer,
                        borderRadius: AppShapes.radiusSm,
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              const Expanded(
                                child: Row(
                                  children: [
                                    Icon(Icons.lock_outline, size: 14, color: AppColors.tertiary),
                                    SizedBox(width: 4),
                                    Expanded(
                                      child: Text(
                                        'Số khách đã đặt (Booked):',
                                        style: TextStyle(fontSize: 12, color: AppColors.tertiary),
                                        maxLines: 1,
                                        overflow: TextOverflow.ellipsis,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                                decoration: BoxDecoration(color: AppColors.surfaceDim, borderRadius: BorderRadius.circular(10)),
                                child: Text('$_bookedCount khách', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                              ),
                            ],
                          ),
                          if (_heldCount > 0) ...[
                            const SizedBox(height: 6),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                const Expanded(
                                  child: Row(
                                    children: [
                                      Icon(Icons.timer_outlined, size: 14, color: AppColors.tertiary),
                                      SizedBox(width: 4),
                                      Expanded(
                                        child: Text(
                                          'Tạm giữ thanh toán (Held):',
                                          style: TextStyle(fontSize: 12, color: AppColors.tertiary),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Text('$_heldCount khách', style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.primary)),
                              ],
                            ),
                          ],
                          const SizedBox(height: 8),
                          ClipRRect(
                            borderRadius: BorderRadius.circular(3),
                            child: LinearProgressIndicator(
                              value: _capacity > 0 ? (_bookedCount / _capacity).clamp(0.0, 1.0) : 0,
                              minHeight: 5,
                              backgroundColor: AppColors.surfaceContainerHighest,
                              valueColor: const AlwaysStoppedAnimation<Color>(AppColors.primary),
                            ),
                          ),
                          const SizedBox(height: 6),
                          const Text(
                            'Dữ liệu chỉ đọc từ hệ thống đơn đặt của khách. Đối tác không thể giảm sức chứa dưới tổng số khách đã đặt & tạm giữ.',
                            style: TextStyle(fontSize: 10, color: AppColors.tertiary),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 14),

                    // Block toggle
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLow,
                        borderRadius: AppShapes.radiusSm,
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.block, size: 20, color: AppColors.primary),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text('Chặn nhận thêm khách (BLOCKED)', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                                Text(
                                  'Ngừng mở bán mới trên ứng dụng mà không ảnh hưởng tới $_bookedCount khách đã đặt thành công.',
                                  style: const TextStyle(fontSize: 11, color: AppColors.tertiary),
                                ),
                              ],
                            ),
                          ),
                          Switch.adaptive(
                            value: _isBlocked,
                            activeColor: AppColors.primary,
                            onChanged: (val) => setState(() => _isBlocked = val),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),

              // Action buttons
              VendorButton(
                text: 'Lưu cấu hình khung giờ',
                icon: Icons.save,
                onPressed: _saveSlot,
              ),
              const SizedBox(height: 10),
              if (_isEdit && _bookedCount == 0)
                Center(
                  child: TextButton.icon(
                    onPressed: () {
                      _db.deleteSlot(widget.slot!.slotId);
                      ToastNotification.showSuccess(context, 'Đã xóa khung giờ');
                      Navigator.of(context).maybePop();
                    },
                    icon: const Icon(Icons.delete_outline, color: AppColors.error, size: 16),
                    label: const Text('Xóa khung giờ này', style: TextStyle(color: AppColors.error, fontSize: 12)),
                  ),
                ),
              const SizedBox(height: 20),
            ],
          ),
        ),
      ),
    );
  }
}
