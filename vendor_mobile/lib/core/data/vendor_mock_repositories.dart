// Mock Repositories implementing repository interfaces
// Uses VendorMockDatabase singleton to provide cross-screen consistent state mutations.

import '../models/vendor_models.dart';
import 'vendor_mock_database.dart';
import 'vendor_repository_interfaces.dart';

class VendorAuthRepository implements IVendorAuthRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<UserModel> getCurrentUser() async {
    return _db.currentUser;
  }

  @override
  Future<UserModel> login(String email, String password) async {
    await Future.delayed(const Duration(milliseconds: 300));
    if (_db.currentUser.isLocked) {
      throw Exception('Tài khoản bị khóa. Vui lòng liên hệ quản trị viên.');
    }
    if (email.trim().toLowerCase() == _db.currentUser.email.toLowerCase()) {
      return _db.currentUser;
    }
    if (!email.contains('@')) {
      throw Exception('Email không đúng định dạng.');
    }
    // Allow demo vendor login
    _db.currentUser = _db.currentUser.copyWith(email: email.trim());
    _db.notifyStoreChanged();
    return _db.currentUser;
  }

  @override
  Future<UserModel> register({
    required String fullName,
    required String email,
    String? phone,
    required String password,
  }) async {
    await Future.delayed(const Duration(milliseconds: 400));
    final newUser = UserModel(
      id: 'usr-vnd-${DateTime.now().millisecondsSinceEpoch}',
      email: email.trim(),
      phone: phone?.trim(),
      fullName: fullName.trim(),
      role: UserRole.vendor,
      isEmailVerified: false,
      createdAt: DateTime.now(),
    );
    _db.currentUser = newUser;
    _db.notifyStoreChanged();
    return newUser;
  }

  @override
  Future<void> sendVerificationEmail(String email) async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  @override
  Future<void> requestPasswordReset(String email) async {
    await Future.delayed(const Duration(milliseconds: 300));
  }

  @override
  Future<void> changePassword({
    required String currentPassword,
    required String newPassword,
  }) async {
    await Future.delayed(const Duration(milliseconds: 350));
    if (currentPassword.isEmpty) {
      throw Exception('Vui lòng nhập mật khẩu hiện tại.');
    }
    if (newPassword.length < 8) {
      throw Exception('Mật khẩu mới phải có tối thiểu 8 ký tự.');
    }
  }

  @override
  Future<void> logout() async {
    await Future.delayed(const Duration(milliseconds: 200));
  }
}

class VendorProfileRepository implements IVendorProfileRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<VendorModel> getVendorProfile() async {
    return _db.currentVendor;
  }

  @override
  Future<VendorModel> updateVendorProfile({
    String? businessName,
    String? taxCode,
    String? address,
    String? avatarUrl,
    String? fullName,
    String? phone,
  }) async {
    await Future.delayed(const Duration(milliseconds: 300));
    _db.currentVendor = _db.currentVendor.copyWith(
      businessName: businessName,
      taxCode: taxCode,
      address: address,
    );
    if (fullName != null || phone != null || avatarUrl != null) {
      _db.currentUser = _db.currentUser.copyWith(
        fullName: fullName,
        phone: phone,
        avatarUrl: avatarUrl,
      );
    }
    _db.notifyStoreChanged();
    return _db.currentVendor;
  }

  @override
  Future<VendorModel> updateBankProfile({
    required String bankName,
    required String bankAccountNumber,
    required String bankAccountHolder,
  }) async {
    await Future.delayed(const Duration(milliseconds: 300));
    _db.currentVendor = _db.currentVendor.copyWith(
      bankName: bankName,
      bankAccountNumber: bankAccountNumber,
      bankAccountHolder: bankAccountHolder,
    );
    _db.notifyStoreChanged();
    return _db.currentVendor;
  }
}

class VendorDocumentRepository implements IVendorDocumentRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<VendorDocumentModel>> getVendorDocuments() async {
    return List.unmodifiable(_db.vendorDocuments);
  }

  @override
  Future<VendorDocumentModel> uploadOrReplaceDocument({
    required VendorDocType docType,
    required String fileName,
    required String fileSize,
  }) async {
    await Future.delayed(const Duration(milliseconds: 300));
    final index = _db.vendorDocuments.indexWhere((d) => d.docType == docType);
    final doc = VendorDocumentModel(
      id: index != -1 ? _db.vendorDocuments[index].id : 'vdoc-${DateTime.now().millisecondsSinceEpoch}',
      vendorId: _db.currentVendor.id,
      docType: docType,
      fileUrl: fileName,
      status: DocumentReviewStatus.pending, // Pending Admin review, Vendor CANNOT self-approve
      fileName: fileName,
      fileSize: fileSize,
      uploadedAt: DateTime.now(),
    );
    if (index != -1) {
      _db.vendorDocuments[index] = doc;
    } else {
      _db.vendorDocuments.add(doc);
    }
    _db.notifyStoreChanged();
    return doc;
  }
}

class VendorServiceRepository implements IVendorServiceRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<ServiceModel>> getServices() async {
    return List.unmodifiable(_db.services);
  }

  @override
  Future<ServiceModel?> getServiceById(String id) async {
    try {
      return _db.services.firstWhere((s) => s.id == id);
    } catch (_) {
      return null;
    }
  }

  @override
  Future<ServiceModel> createService(ServiceModel service) async {
    await Future.delayed(const Duration(milliseconds: 300));
    _db.services.add(service);
    _db.notifyStoreChanged();
    return service;
  }

  @override
  Future<ServiceModel> updateService(ServiceModel service) async {
    await Future.delayed(const Duration(milliseconds: 300));
    final index = _db.services.indexWhere((s) => s.id == service.id);
    if (index != -1) {
      _db.services[index] = service;
      _db.notifyStoreChanged();
      return service;
    }
    throw Exception('Dịch vụ không tồn tại.');
  }

  @override
  Future<ServiceModel> updateServiceStatus(String serviceId, ServiceStatus status) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.services.indexWhere((s) => s.id == serviceId);
    if (index != -1) {
      final updated = _db.services[index].copyWith(status: status);
      _db.services[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Dịch vụ không tồn tại.');
  }

  @override
  Future<List<ServiceSafetyDocumentModel>> getServiceSafetyDocuments(String serviceId) async {
    return _db.serviceSafetyDocs.where((d) => d.serviceId == serviceId).toList();
  }

  @override
  Future<ServiceSafetyDocumentModel> uploadServiceSafetyDocument({
    required String serviceId,
    required String fileName,
  }) async {
    await Future.delayed(const Duration(milliseconds: 300));
    final doc = ServiceSafetyDocumentModel(
      id: 'sdoc-${DateTime.now().millisecondsSinceEpoch}',
      serviceId: serviceId,
      fileUrl: fileName,
      status: DocumentReviewStatus.pending, // Pending Admin review
      fileName: fileName,
      uploadedAt: DateTime.now(),
    );
    _db.serviceSafetyDocs.removeWhere((d) => d.serviceId == serviceId);
    _db.serviceSafetyDocs.add(doc);
    _db.notifyStoreChanged();
    return doc;
  }
}

class VendorSlotRepository implements IVendorSlotRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<ServiceSlotModel>> getSlotsForService(String serviceId, DateTime date) async {
    return _db.slots.where((s) => s.serviceId == serviceId && s.date.year == date.year && s.date.month == date.month && s.date.day == date.day).toList();
  }

  @override
  Future<ServiceSlotModel?> getSlotById(String slotId) async {
    try {
      return _db.slots.firstWhere((s) => s.id == slotId);
    } catch (_) {
      return null;
    }
  }

  @override
  Future<ServiceSlotModel> saveSlot(ServiceSlotModel slot) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.slots.indexWhere((s) => s.id == slot.id);
    if (index != -1) {
      _db.slots[index] = slot;
    } else {
      _db.slots.add(slot);
    }
    _db.notifyStoreChanged();
    return slot;
  }

  @override
  Future<ServiceSlotModel> updateSlotCapacity(String slotId, int newCapacity) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.slots.indexWhere((s) => s.id == slotId);
    if (index != -1) {
      final slot = _db.slots[index];
      final minRequired = slot.bookedCount + slot.heldCount;
      if (newCapacity < minRequired) {
        throw Exception('Sức chứa không được nhỏ hơn tổng số khách đã đặt và giữ tạm ($minRequired).');
      }
      final updated = slot.copyWith(capacity: newCapacity);
      _db.slots[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Khung giờ không tồn tại.');
  }

  @override
  Future<ServiceSlotModel> toggleBlockSlot(String slotId, bool blocked) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.slots.indexWhere((s) => s.id == slotId);
    if (index != -1) {
      final updated = _db.slots[index].copyWith(
        status: blocked ? SlotStatus.blocked : SlotStatus.open,
      );
      _db.slots[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Khung giờ không tồn tại.');
  }
}

class VendorOrderRepository implements IVendorOrderRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<SubOrderModel>> getSubOrders({SubOrderStatus? filterStatus}) async {
    if (filterStatus == null) {
      return List.unmodifiable(_db.subOrders);
    }
    return _db.subOrders.where((o) => o.status == filterStatus).toList();
  }

  @override
  Future<SubOrderModel?> getSubOrderById(String subOrderId) async {
    try {
      return _db.subOrders.firstWhere((o) => o.id == subOrderId);
    } catch (_) {
      return null;
    }
  }

  @override
  Future<SubOrderModel> confirmSubOrder(String subOrderId) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.subOrders.indexWhere((o) => o.id == subOrderId);
    if (index != -1) {
      final updated = _db.subOrders[index].copyWith(status: SubOrderStatus.confirmed);
      _db.subOrders[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Đơn không tồn tại.');
  }

  @override
  Future<SubOrderModel> rejectSubOrder(String subOrderId) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.subOrders.indexWhere((o) => o.id == subOrderId);
    if (index != -1) {
      final updated = _db.subOrders[index].copyWith(status: SubOrderStatus.rejected);
      _db.subOrders[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Đơn không tồn tại.');
  }

  @override
  Future<SubOrderModel> checkInSubOrder(String subOrderId) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.subOrders.indexWhere((o) => o.id == subOrderId);
    if (index != -1) {
      final current = _db.subOrders[index];
      if (current.status != SubOrderStatus.confirmed) {
        throw Exception('Chỉ đơn đã xác nhận mới có thể check-in.');
      }
      if (current.isCheckedIn) {
        throw Exception('Vé đã check-in trước đó.');
      }
      final updated = current.copyWith(checkedInAt: DateTime.now());
      _db.subOrders[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Đơn không tồn tại.');
  }

  @override
  Future<SubOrderModel> completeSubOrder(String subOrderId) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.subOrders.indexWhere((o) => o.id == subOrderId);
    if (index != -1) {
      final current = _db.subOrders[index];
      if (current.status != SubOrderStatus.confirmed) {
        throw Exception('Chỉ đơn đã xác nhận mới có thể hoàn tất.');
      }
      final updated = current.copyWith(status: SubOrderStatus.completed);
      _db.subOrders[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Đơn không tồn tại.');
  }

  @override
  Future<TicketLookupResult> lookupTicket(String ticketCode) async {
    await Future.delayed(const Duration(milliseconds: 300));
    final cleanCode = ticketCode.trim().replaceAll('#', '');
    if (cleanCode.isEmpty) {
      return const TicketLookupResult(
        type: TicketLookupResultType.invalid,
        message: 'Vui lòng nhập mã vé để tra cứu.',
      );
    }

    final match = _db.subOrders.where((o) => o.id.replaceAll('#', '').toLowerCase() == cleanCode.toLowerCase()).toList();
    if (match.isEmpty) {
      return const TicketLookupResult(
        type: TicketLookupResultType.invalid,
        message: 'Mã vé không hợp lệ hoặc không thuộc nhà cung cấp này.',
      );
    }

    final order = match.first;
    if (order.isCheckedIn) {
      return TicketLookupResult(
        type: TicketLookupResultType.used,
        subOrder: order,
        message: 'Vé đã check-in trước đó lúc ${order.checkedInAt?.hour.toString().padLeft(2, '0')}:${order.checkedInAt?.minute.toString().padLeft(2, '0')}. Không thể dùng lại.',
      );
    }

    switch (order.status) {
      case SubOrderStatus.confirmed:
        return TicketLookupResult(
          type: TicketLookupResultType.valid,
          subOrder: order,
          message: 'Vé hợp lệ. Có thể xác nhận vào bãi và nhận ván SUP.',
        );
      case SubOrderStatus.pending:
        return TicketLookupResult(
          type: TicketLookupResultType.pending,
          subOrder: order,
          message: 'Đơn hàng chưa được xác nhận. Chưa thể check-in.',
        );
      case SubOrderStatus.cancelled:
        return TicketLookupResult(
          type: TicketLookupResultType.cancelled,
          subOrder: order,
          message: 'Đơn hàng đã bị hủy. Vé không còn giá trị.',
        );
      case SubOrderStatus.refunded:
        return TicketLookupResult(
          type: TicketLookupResultType.refunded,
          subOrder: order,
          message: 'Đơn hàng đã được hoàn tiền. Vé không còn hiệu lực.',
        );
      case SubOrderStatus.completed:
        return TicketLookupResult(
          type: TicketLookupResultType.used,
          subOrder: order,
          message: 'Dịch vụ đã hoàn tất trước đó.',
        );
      case SubOrderStatus.rejected:
        return TicketLookupResult(
          type: TicketLookupResultType.invalid,
          subOrder: order,
          message: 'Đơn hàng đã bị từ chối.',
        );
    }
  }
}

class VendorReviewRepository implements IVendorReviewRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<ReviewModel>> getReviews({String? filter}) async {
    if (filter == 'pending') {
      return _db.reviews.where((r) => r.vendorReply == null).toList();
    }
    if (filter == 'flagged') {
      return _db.reviews.where((r) => r.isFlagged).toList();
    }
    if (filter == '5star') {
      return _db.reviews.where((r) => r.rating == 5).toList();
    }
    if (filter == '4star') {
      return _db.reviews.where((r) => r.rating == 4).toList();
    }
    return List.unmodifiable(_db.reviews);
  }

  @override
  Future<ReviewModel> replyToReview(String reviewId, String replyContent) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.reviews.indexWhere((r) => r.id == reviewId);
    if (index != -1) {
      final updated = _db.reviews[index].copyWith(
        vendorReply: replyContent.trim(),
        vendorRepliedAt: DateTime.now(),
      );
      _db.reviews[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Đánh giá không tồn tại.');
  }
}

class VendorVoucherRepository implements IVendorVoucherRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<DiscountCodeModel>> getVouchers() async {
    return List.unmodifiable(_db.vouchers);
  }

  @override
  Future<DiscountCodeModel> createVoucher(DiscountCodeModel voucher) async {
    await Future.delayed(const Duration(milliseconds: 250));
    _db.vouchers.insert(0, voucher);
    _db.notifyStoreChanged();
    return voucher;
  }

  @override
  Future<DiscountCodeModel> updateVoucher(DiscountCodeModel voucher) async {
    await Future.delayed(const Duration(milliseconds: 250));
    final index = _db.vouchers.indexWhere((v) => v.id == voucher.id);
    if (index != -1) {
      if (voucher.maxUses < voucher.usedCount) {
        throw Exception('Số lượt dùng tối đa không được nhỏ hơn số lượt đã sử dụng (${voucher.usedCount}).');
      }
      if (voucher.validTo.isBefore(voucher.validFrom)) {
        throw Exception('Ngày kết thúc phải sau ngày bắt đầu.');
      }
      _db.vouchers[index] = voucher;
      _db.notifyStoreChanged();
      return voucher;
    }
    throw Exception('Mã giảm giá không tồn tại.');
  }

  @override
  Future<DiscountCodeModel> toggleVoucherActive(String voucherId, bool isActive) async {
    await Future.delayed(const Duration(milliseconds: 200));
    final index = _db.vouchers.indexWhere((v) => v.id == voucherId);
    if (index != -1) {
      final updated = _db.vouchers[index].copyWith(isActive: isActive);
      _db.vouchers[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Mã không tồn tại.');
  }

  @override
  Future<List<DiscountRedemptionModel>> getRedemptionsForVoucher(String voucherId) async {
    return _db.redemptions.where((r) => r.discountCodeId == voucherId).toList();
  }
}

class VendorSettlementRepository implements IVendorSettlementRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<SettlementModel>> getSettlements({SettlementStatus? status}) async {
    if (status == null) {
      return List.unmodifiable(_db.settlements);
    }
    return _db.settlements.where((s) => s.status == status).toList();
  }

  @override
  Future<SettlementModel?> getSettlementById(String id) async {
    try {
      return _db.settlements.firstWhere((s) => s.id == id);
    } catch (_) {
      return null;
    }
  }

  @override
  Future<double> getEligiblePayoutAmount({bool combined = false}) async {
    if (combined) {
      // Sum eligible pending settlements
      return _db.settlements
          .where((s) => s.status == SettlementStatus.pending)
          .fold<double>(0.0, (acc, item) => acc + item.netPayableAmount);
    }
    final firstPending = _db.settlements.where((s) => s.status == SettlementStatus.pending).firstOrNull;
    return firstPending?.netPayableAmount ?? 0.0;
  }

  @override
  Future<PayoutRequestModel> requestPayout({
    String? settlementId,
    required double amount,
  }) async {
    await Future.delayed(const Duration(milliseconds: 350));
    final request = PayoutRequestModel(
      id: 'PAY-${DateTime.now().year}${DateTime.now().month.toString().padLeft(2, '0')}-${(100 + _db.payoutRequests.length).toString()}',
      vendorId: _db.currentVendor.id,
      settlementId: settlementId,
      amount: amount,
      status: PayoutRequestStatus.requested,
      createdAt: DateTime.now(),
      bankName: _db.currentVendor.bankName ?? 'Vietcombank',
      bankAccountNumber: _db.currentVendor.bankAccountNumber ?? '0041000889988',
      bankAccountHolder: _db.currentVendor.bankAccountHolder ?? 'TRAN HAI DANG',
    );
    _db.payoutRequests.insert(0, request);
    _db.notifyStoreChanged();
    return request;
  }

  @override
  Future<List<PayoutRequestModel>> getPayoutRequests() async {
    return List.unmodifiable(_db.payoutRequests);
  }
}

class VendorDisputeRepository implements IVendorDisputeRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<DisputeModel>> getDisputes({DisputeStatus? status}) async {
    if (status == null) {
      return List.unmodifiable(_db.disputes);
    }
    return _db.disputes.where((d) => d.status == status).toList();
  }

  @override
  Future<DisputeModel?> getDisputeById(String id) async {
    try {
      return _db.disputes.firstWhere((d) => d.id == id);
    } catch (_) {
      return null;
    }
  }

  @override
  Future<DisputeModel> submitDisputeEvidence(String disputeId, String attachmentUrl) async {
    await Future.delayed(const Duration(milliseconds: 300));
    final index = _db.disputes.indexWhere((d) => d.id == disputeId);
    if (index != -1) {
      final current = _db.disputes[index];
      final updatedList = List<String>.from(current.vendorAttachments)..add(attachmentUrl);
      final updated = current.copyWith(vendorAttachments: updatedList);
      _db.disputes[index] = updated;
      _db.notifyStoreChanged();
      return updated;
    }
    throw Exception('Khiếu nại không tồn tại.');
  }
}

class VendorRefundRepository implements IVendorRefundRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<RefundModel>> getRefunds() async {
    return List.unmodifiable(_db.refunds);
  }

  @override
  Future<RefundModel?> getRefundById(String id) async {
    try {
      return _db.refunds.firstWhere((r) => r.id == id);
    } catch (_) {
      return null;
    }
  }
}

class VendorChatRepository implements IVendorChatRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<ConversationModel>> getConversations() async {
    return List.unmodifiable(_db.conversations);
  }

  @override
  Future<List<MessageModel>> getMessages(String conversationId) async {
    return _db.messages[conversationId] ?? [];
  }

  @override
  Future<MessageModel> sendMessage(String conversationId, String content) async {
    await Future.delayed(const Duration(milliseconds: 200));
    final msg = MessageModel(
      id: 'msg-${DateTime.now().millisecondsSinceEpoch}',
      conversationId: conversationId,
      senderId: _db.currentUser.id,
      content: content.trim(),
      createdAt: DateTime.now(),
      isRead: true,
    );
    final list = _db.messages.putIfAbsent(conversationId, () => []);
    list.add(msg);

    // update conversation last message
    final cIndex = _db.conversations.indexWhere((c) => c.id == conversationId);
    if (cIndex != -1) {
      _db.conversations[cIndex] = _db.conversations[cIndex].copyWith(
        lastMessage: content.trim(),
        lastMessageTime: DateTime.now(),
      );
    }
    _db.notifyStoreChanged();
    return msg;
  }
}

class VendorNotificationRepository implements IVendorNotificationRepository {
  final VendorMockDatabase _db = VendorMockDatabase.instance;

  @override
  Future<List<NotificationModel>> getNotifications() async {
    return List.unmodifiable(_db.notifications);
  }

  @override
  Future<WeatherCacheModel> getWeatherInfo() async {
    return _db.weatherCache;
  }
}
