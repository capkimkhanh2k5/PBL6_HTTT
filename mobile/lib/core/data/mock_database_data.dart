import '../models/danasea_models.dart';

/// Cohesive Mock Database Data representing live PostgreSQL state for DanaSea
/// All models strictly reflect E:\DANASEA_Database_Design.docx and Stitch references.
class MockDatabaseData {
  MockDatabaseData._();

  // ==================================================
  // CURRENT USER
  // ==================================================
  static final UserModel currentUser = UserModel(
    id: 'usr-001',
    email: 'an.nguyen@example.com',
    phone: '0905 123 456',
    fullName: 'Nguyễn Văn An',
    role: UserRole.customer,
    avatarUrl:
        'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=300&auto=format&fit=crop&q=80',
    isEmailVerified: true,
    isLocked: false,
    locale: 'vi',
    createdAt: DateTime(2024, 5, 10),
  );

  // ==================================================
  // VENDORS
  // ==================================================
  static final VendorModel vendorOceanClub = VendorModel(
    id: 'vnd-001',
    userId: 'usr-vnd-001',
    businessName: 'Danang Ocean Club',
    taxCode: '0401829482',
    address: 'Võ Nguyên Giáp, Bãi biển Mỹ Khê, Ngũ Hành Sơn, Đà Nẵng',
    bankAccountNumber: '1029384756',
    bankName: 'Vietcombank',
    bankAccountHolder: 'CTY TNHH CAU LAC BO BIEN DA NANG',
    verificationStatus: VendorVerificationStatus.approved,
    ratingAvg: 4.9,
    ratingCount: 128,
    badgeTier: VendorBadgeTier.verified,
    avatarUrl:
        'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=300&auto=format&fit=crop&q=80',
  );

  static final VendorModel vendorSonTraAdv = VendorModel(
    id: 'vnd-002',
    userId: 'usr-vnd-002',
    businessName: 'Sơn Trà Adventure',
    taxCode: '0402948192',
    address: 'Bãi Bụt, Hoàng Sa, Sơn Trà, Đà Nẵng',
    bankAccountNumber: '9876543210',
    bankName: 'Techcombank',
    bankAccountHolder: 'CTY DU LICH BIEN SON TRA',
    verificationStatus: VendorVerificationStatus.approved,
    ratingAvg: 4.8,
    ratingCount: 96,
    badgeTier: VendorBadgeTier.topRated,
    avatarUrl:
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=300&auto=format&fit=crop&q=80',
  );

  static final VendorModel vendorDanangSurf = VendorModel(
    id: 'vnd-003',
    userId: 'usr-vnd-003',
    businessName: 'Da Nang Surf School',
    taxCode: '0403381928',
    address: 'Bãi biển Non Nước, Ngũ Hành Sơn, Đà Nẵng',
    bankAccountNumber: '1902847291',
    bankName: 'MB Bank',
    bankAccountHolder: 'TRUONG LUOT VAN DA NANG',
    verificationStatus: VendorVerificationStatus.approved,
    ratingAvg: 4.7,
    ratingCount: 74,
    badgeTier: VendorBadgeTier.verified,
    avatarUrl:
        'https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=300&auto=format&fit=crop&q=80',
  );

  // ==================================================
  // CATEGORIES
  // ==================================================
  static final List<CategoryModel> categories = [
    const CategoryModel(
      id: 'cat-001',
      name: 'Chèo SUP',
      nameEn: 'Stand-up Paddleboard',
      slug: 'cheo-sup',
      iconUrl: 'surfing',
    ),
    const CategoryModel(
      id: 'cat-002',
      name: 'Lặn biển',
      nameEn: 'Scuba Diving & Snorkeling',
      slug: 'lan-bien',
      iconUrl: 'scuba_diving',
    ),
    const CategoryModel(
      id: 'cat-003',
      name: 'Cano lướt sóng',
      nameEn: 'Speedboat & Wakeboarding',
      slug: 'cano-luot-song',
      iconUrl: 'directions_boat',
    ),
    const CategoryModel(
      id: 'cat-004',
      name: 'Lướt ván',
      nameEn: 'Surfing',
      slug: 'luot-van',
      iconUrl: 'skateboarding',
    ),
    const CategoryModel(
      id: 'cat-005',
      name: 'Dù lượn biển',
      nameEn: 'Parasailing',
      slug: 'du-luon-bien',
      iconUrl: 'paragliding',
    ),
  ];

  // ==================================================
  // SERVICES (EXPERIENCES)
  // ==================================================
  static final List<ServiceModel> services = [
    ServiceModel(
      id: 'srv-001',
      vendorId: vendorOceanClub.id,
      vendorName: vendorOceanClub.businessName,
      vendorBadge: vendorOceanClub.badgeTier,
      categoryId: 'cat-001',
      categoryName: 'Chèo SUP',
      name: 'Chèo SUP đón bình minh Mỹ Khê',
      nameEn: 'Sunrise SUP Paddle at My Khe Beach',
      slug: 'cheo-sup-don-binh-minh-my-khe',
      description:
          'Trải nghiệm chèo ván đứng (SUP) đón ánh bình minh rạng ngời tại bãi biển Mỹ Khê Đà Nẵng. Mặt biển sáng sớm phẳng lặng như gương soi, nước ấm dịu và không khí trong lành mang lại nguồn năng lượng sảng khoái. Gói dịch vụ đã bao gồm ván chèo chuyên dụng, áo phao tiêu chuẩn, thợ chụp ảnh chuyên nghiệp bằng máy ảnh và flycam, cùng hướng dẫn viên đồng hành đảm bảo an toàn tuyệt đối.',
      price: 280000,
      durationMinutes: 120,
      capacityPerSlot: 15,
      locationName: 'Bãi biển Mỹ Khê',
      address: 'Lô 12 Võ Nguyên Giáp, Phước Mỹ, Sơn Trà, Đà Nẵng',
      latitude: 16.0601,
      longitude: 108.2465,
      waiverContent:
          'Tôi cam kết đủ điều kiện sức khỏe tham gia hoạt động trên biển, tuân thủ mặc áo phao và hướng dẫn của huấn luyện viên trong suốt thời gian diễn ra trải nghiệm.',
      weatherSensitive: true,
      minWindKmh: 4.0,
      maxWaveM: 1.2,
      avgRating: 4.9,
      ratingCount: 128,
      viewCount: 1420,
      imageUrls: [
        'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1519046904884-53103b34b206?w=800&auto=format&fit=crop&q=80',
      ],
      isFavorite: true,
    ),
    ServiceModel(
      id: 'srv-002',
      vendorId: vendorSonTraAdv.id,
      vendorName: vendorSonTraAdv.businessName,
      vendorBadge: vendorSonTraAdv.badgeTier,
      categoryId: 'cat-002',
      categoryName: 'Lặn biển',
      name: 'Lặn ngắm san hô Bãi Bụt Sơn Trà',
      nameEn: 'Coral Reef Snorkeling at Bai But Son Tra',
      slug: 'lan-ngam-san-ho-bai-but-son-tra',
      description:
          'Khám phá hệ sinh thái rạn san hô tự nhiên tuyệt đẹp tại Bãi Bụt thuộc bán đảo Sơn Trà. Nước biển ngọc lam trong vắt với tầm nhìn đáy lên đến 8-10m. Khách được trang bị kính lặn, ống thở silicone cao cấp, chân vịt và hướng dẫn kỹ năng lặn ống thở căn bản cùng chuyên gia lặn biển.',
      price: 450000,
      durationMinutes: 150,
      capacityPerSlot: 10,
      locationName: 'Bán đảo Sơn Trà',
      address: 'Bãi Bụt, Hoàng Sa, Thọ Quang, Sơn Trà, Đà Nẵng',
      latitude: 16.1158,
      longitude: 108.2751,
      waiverContent:
          'Tôi xác nhận không có tiền sử bệnh tim mạch, huyết áp cao cấp tính hoặc chấn thương màng nhĩ nghiêm trọng. Tôi đồng ý tuân thủ quy tắc bảo vệ rạn san hô, không dẫm đạp hay bẻ gãy san hô.',
      weatherSensitive: true,
      minWindKmh: 5.0,
      maxWaveM: 1.0,
      avgRating: 4.8,
      ratingCount: 96,
      viewCount: 980,
      imageUrls: [
        'https://images.unsplash.com/photo-1544551763-77ef2d0cfc6c?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1682687220063-4742bd7fd538?w=800&auto=format&fit=crop&q=80',
      ],
      isFavorite: false,
    ),
    ServiceModel(
      id: 'srv-003',
      vendorId: vendorSonTraAdv.id,
      vendorName: vendorSonTraAdv.businessName,
      vendorBadge: vendorSonTraAdv.badgeTier,
      categoryId: 'cat-003',
      categoryName: 'Cano lướt sóng',
      name: 'Cano cao tốc lướt sóng Mũi Nghê',
      nameEn: 'Speedboat Adventure to Mui Nghe Cliff',
      slug: 'cano-cao-toc-luot-song-mui-nghe',
      description:
          'Hành trình cano cao tốc băng qua những con sóng xanh ngắt quanh vách đá kỳ vĩ Mũi Nghê - điểm đón bình minh đầu tiên của Đà Nẵng. Cảm giác phấn khích tột độ khi lướt qua bọt sóng trắng xóa cùng cảnh quan thiên nhiên hoang sơ ngoạn mục.',
      price: 450000,
      durationMinutes: 90,
      capacityPerSlot: 12,
      locationName: 'Mũi Nghê, Sơn Trà',
      address: 'Bến thuyền Sơn Trà, Bãi Trẹm, Thọ Quang, Đà Nẵng',
      latitude: 16.1284,
      longitude: 108.3129,
      waiverContent:
          'Tôi đồng ý mặc áo phao bảo hộ trong toàn bộ thời gian di chuyển trên cano và nghe theo chỉ dẫn an toàn của thuyền trưởng.',
      weatherSensitive: true,
      minWindKmh: 6.0,
      maxWaveM: 1.5,
      avgRating: 4.9,
      ratingCount: 65,
      viewCount: 840,
      imageUrls: [
        'https://images.unsplash.com/photo-1559827291-72ee739d0d9a?w=800&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop&q=80',
      ],
      isFavorite: true,
    ),
    ServiceModel(
      id: 'srv-004',
      vendorId: vendorDanangSurf.id,
      vendorName: vendorDanangSurf.businessName,
      vendorBadge: vendorDanangSurf.badgeTier,
      categoryId: 'cat-004',
      categoryName: 'Lướt ván',
      name: 'Khóa học lướt ván vỡ lòng Non Nước',
      nameEn: 'Beginner Surfing Lesson at Non Nuoc Beach',
      slug: 'khoa-hoc-luot-van-non-nuoc',
      description:
          'Làm quen với môn thể thao lướt sóng cùng huấn luyện viên có chứng chỉ quốc tế ISA. Học cách đọc sóng, bắt nhịp sóng, đứng trên ván lướt vững chãi và an toàn tại bờ biển Non Nước với bãi cát thoải và sóng đều.',
      price: 600000,
      durationMinutes: 120,
      capacityPerSlot: 6,
      locationName: 'Bãi biển Non Nước',
      address: 'Đường Trường Sa, Hòa Hải, Ngũ Hành Sơn, Đà Nẵng',
      latitude: 16.0028,
      longitude: 108.2612,
      waiverContent:
          'Tôi xác nhận biết bơi căn bản và tự chịu trách nhiệm về thể lực bản thân khi tham gia tập luyện thể thao dưới nước.',
      weatherSensitive: true,
      minWindKmh: 8.0,
      maxWaveM: 1.8,
      avgRating: 4.7,
      ratingCount: 42,
      viewCount: 620,
      imageUrls: [
        'https://images.unsplash.com/photo-1502680390469-be75c86b636f?w=800&auto=format&fit=crop&q=80',
      ],
      isFavorite: false,
    ),
  ];

  // ==================================================
  // SERVICE SLOTS
  // ==================================================
  static List<ServiceSlotModel> getSlotsForService(String serviceId, DateTime date) {
    return [
      ServiceSlotModel(
        id: 'slt-$serviceId-01',
        serviceId: serviceId,
        date: date,
        startTime: '05:00',
        endTime: '07:00',
        capacity: 15,
        bookedCount: 12,
        status: SlotStatus.open,
      ),
      ServiceSlotModel(
        id: 'slt-$serviceId-02',
        serviceId: serviceId,
        date: date,
        startTime: '07:30',
        endTime: '09:30',
        capacity: 15,
        bookedCount: 6,
        status: SlotStatus.open,
      ),
      ServiceSlotModel(
        id: 'slt-$serviceId-03',
        serviceId: serviceId,
        date: date,
        startTime: '15:30',
        endTime: '17:30',
        capacity: 15,
        bookedCount: 14,
        status: SlotStatus.open,
      ),
    ];
  }

  // ==================================================
  // ORDERS & SUB-ORDERS (MASTER - SUB ORDER MODEL)
  // ==================================================
  static final MasterOrderModel sampleOrderPaid = MasterOrderModel(
    id: 'DNS-8924',
    customerId: 'usr-001',
    status: MasterOrderStatus.paid,
    totalAmount: 1030000,
    discountAmount: 40000,
    discountCode: 'DANASEA2024',
    createdAt: DateTime(2024, 10, 28, 5, 42),
    subOrders: [
      SubOrderModel(
        id: 'DNS-8924-1',
        masterOrderId: 'DNS-8924',
        vendorId: 'vnd-001',
        vendorName: 'Danang Ocean Club',
        serviceId: 'srv-001',
        serviceName: 'Chèo SUP đón bình minh Mỹ Khê',
        serviceImageUrl:
            'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=500&auto=format&fit=crop&q=80',
        locationName: 'Bãi biển Mỹ Khê',
        slotId: 'slt-srv-001-01',
        slotDate: DateTime(2024, 10, 28),
        slotTime: '05:00 - 07:00',
        quantity: 2,
        unitPrice: 280000,
        subtotalAmount: 560000,
        status: SubOrderStatus.confirmed,
        waiverAccepted: true,
        waiverAcceptedAt: DateTime(2024, 10, 28, 5, 40),
        qrSecret: 'f47ac10b-58cc-4372-a567-0e02b2c3d479',
      ),
      SubOrderModel(
        id: 'DNS-8924-2',
        masterOrderId: 'DNS-8924',
        vendorId: 'vnd-002',
        vendorName: 'Sơn Trà Adventure',
        serviceId: 'srv-002',
        serviceName: 'Lặn biển ngắm san hô Bãi Bụt',
        serviceImageUrl:
            'https://images.unsplash.com/photo-1544551763-77ef2d0cfc6c?w=500&auto=format&fit=crop&q=80',
        locationName: 'Bán đảo Sơn Trà',
        slotId: 'slt-srv-002-02',
        slotDate: DateTime(2024, 10, 29),
        slotTime: '08:30 - 11:00',
        quantity: 1,
        unitPrice: 450000,
        subtotalAmount: 450000,
        status: SubOrderStatus.pending,
        waiverAccepted: true,
        waiverAcceptedAt: DateTime(2024, 10, 28, 5, 40),
        qrSecret: 'c9a646d3-9c61-4cb7-89e4-85cf1a95e2b8',
      ),
    ],
  );

  static final MasterOrderModel sampleOrderCompleted = MasterOrderModel(
    id: 'DNS-7712',
    customerId: 'usr-001',
    status: MasterOrderStatus.completed,
    totalAmount: 900000,
    discountAmount: 0,
    createdAt: DateTime(2024, 10, 25, 14, 20),
    subOrders: [
      SubOrderModel(
        id: 'DNS-7712-1',
        masterOrderId: 'DNS-7712',
        vendorId: 'vnd-002',
        vendorName: 'Sơn Trà Adventure',
        serviceId: 'srv-003',
        serviceName: 'Cano lướt sóng Mũi Nghê Sơn Trà',
        serviceImageUrl:
            'https://images.unsplash.com/photo-1559827291-72ee739d0d9a?w=500&auto=format&fit=crop&q=80',
        locationName: 'Bán đảo Sơn Trà',
        slotId: 'slt-srv-003-03',
        slotDate: DateTime(2024, 10, 26),
        slotTime: '15:30 - 17:00',
        quantity: 2,
        unitPrice: 450000,
        subtotalAmount: 900000,
        status: SubOrderStatus.completed,
        waiverAccepted: true,
        waiverAcceptedAt: DateTime(2024, 10, 25, 14, 18),
        qrSecret: '8a2b5e71-460d-4b87-9bc1-57d34190bcf2',
        checkedInAt: DateTime(2024, 10, 26, 15, 25),
      ),
    ],
  );

  static final List<MasterOrderModel> allOrders = [
    sampleOrderPaid,
    sampleOrderCompleted,
  ];

  // ==================================================
  // REFUNDS
  // ==================================================
  static final List<RefundModel> refunds = [
    RefundModel(
      id: 'ref-001',
      subOrderId: 'DNS-8924-1',
      subOrderCode: '#DNS-8924-1',
      serviceName: 'Chèo SUP đón bình minh Mỹ Khê',
      amount: 560000,
      refundPercentage: 100.0,
      reason: RefundReason.customerCancel,
      status: RefundStatus.pending,
      originalPaymentMethod: 'VNPAY (Thẻ ATM Quốc tế / Visa)',
      createdAt: DateTime(2024, 10, 28, 7, 15),
    ),
    RefundModel(
      id: 'ref-002',
      subOrderId: 'DNS-7712-1',
      subOrderCode: '#DNS-7712-1',
      serviceName: 'Cano lướt sóng Mũi Nghê',
      amount: 900000,
      refundPercentage: 100.0,
      reason: RefundReason.weather,
      status: RefundStatus.processed,
      originalPaymentMethod: 'MOMO Ví điện tử',
      processedAt: DateTime(2024, 10, 26, 17, 0),
      createdAt: DateTime(2024, 10, 26, 14, 10),
    ),
  ];

  // ==================================================
  // DISPUTES
  // ==================================================
  static final List<DisputeModel> disputes = [
    DisputeModel(
      id: 'DISP-1042',
      subOrderId: 'DNS-8924-1',
      subOrderCode: 'DNS-8924-1',
      serviceName: 'Chèo SUP đón bình minh Mỹ Khê',
      vendorName: 'Danang Ocean Club',
      raisedBy: 'usr-001',
      category: 'Chất lượng dịch vụ không đúng mô tả',
      description:
          'Áo phao cung cấp bị rách dây cài an toàn và không có thợ chụp ảnh đồng hành như cam kết trong gói dịch vụ.',
      status: DisputeStatus.resolved,
      resolutionNote:
          'Admin đã kiểm tra phản ánh, nhà cung cấp thừa nhận sai sót do quá tải khách. Đã xử lý hoàn tiền 50% bồi thường cho khách hàng.',
      resolvedAt: DateTime(2024, 10, 27, 10, 30),
      createdAt: DateTime(2024, 10, 26, 12, 0),
      attachmentUrls: [
        'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=500&auto=format&fit=crop&q=80',
      ],
    ),
    DisputeModel(
      id: 'DISP-1043',
      subOrderId: 'DNS-8924-2',
      subOrderCode: 'DNS-8924-2',
      serviceName: 'Lặn biển ngắm san hô Bãi Bụt',
      vendorName: 'Sơn Trà Adventure',
      raisedBy: 'usr-001',
      category: 'Thời gian khởi hành bị trễ quá 60 phút',
      description:
          'Tàu đón trễ 1 tiếng khiến nhóm không kịp lịch trình tiếp theo trong ngày.',
      status: DisputeStatus.inReview,
      createdAt: DateTime(2024, 10, 28, 14, 0),
    ),
    DisputeModel(
      id: 'DISP-1044',
      subOrderId: 'DNS-8924-1',
      subOrderCode: 'DNS-8924-1',
      serviceName: 'Chèo SUP đón bình minh Mỹ Khê',
      vendorName: 'Danang Ocean Club',
      raisedBy: 'usr-001',
      category: 'Yêu cầu hoàn tiền do đến muộn',
      description:
          'Khách đến điểm tập kết muộn 40 phút so với giờ xuất bến, cano đã khởi hành trước.',
      status: DisputeStatus.rejected,
      resolutionNote:
          'Admin từ chối khiếu nại: Đối tác đã chờ 20 phút và gọi điện xác nhận nhưng khách không có mặt. Trường hợp khách tự ý trễ giờ không thuộc diện được bồi hoàn theo điều khoản dịch vụ.',
      resolvedAt: DateTime(2024, 10, 27, 16, 0),
      createdAt: DateTime(2024, 10, 27, 8, 30),
    ),
    DisputeModel(
      id: 'DISP-1045',
      subOrderId: 'DNS-7712-2',
      subOrderCode: 'DNS-7712-2',
      serviceName: 'Lặn ngắm rạn san hô Nam Bán Đảo',
      vendorName: 'Sơn Trà Eco Tour',
      raisedBy: 'usr-001',
      category: 'Thiếu thiết bị lặn chuyên dụng đi kèm',
      description:
          'Gói tour cam kết bao gồm chân vịt và kính lặn chống mờ chuyên dụng nhưng tại bến nhân viên thông báo hết chân vịt.',
      status: DisputeStatus.open,
      createdAt: DateTime(2024, 10, 29, 9, 15),
    ),
  ];

  // ==================================================
  // REVIEWS
  // ==================================================
  static final List<ReviewModel> reviews = [
    ReviewModel(
      id: 'rev-001',
      subOrderId: 'DNS-7712-1',
      customerId: 'usr-001',
      customerName: 'Nguyễn Văn An',
      customerAvatar:
          'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200&auto=format&fit=crop&q=80',
      vendorId: 'vnd-001',
      serviceId: 'srv-001',
      rating: 5,
      comment:
          'Trải nghiệm tuyệt vời ngoài sức tưởng tượng! Anh hướng dẫn viên bơi kèm rất nhiệt tình, chụp ảnh bình minh Mỹ Khê cực kỳ nghệ thuật. Nước biển sáng sớm êm và sạch mát.',
      images: [
        'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=500&auto=format&fit=crop&q=80',
        'https://images.unsplash.com/photo-1519046904884-53103b34b206?w=500&auto=format&fit=crop&q=80',
      ],
      vendorReply:
          'Danang Ocean Club cảm ơn bạn An rất nhiều! Hẹn gặp lại bạn trong những chuyến lướt sóng tiếp theo!',
      vendorRepliedAt: DateTime(2024, 10, 27, 9, 15),
      createdAt: DateTime(2024, 10, 26, 18, 30),
    ),
    ReviewModel(
      id: 'rev-002',
      subOrderId: 'DNS-7700-1',
      customerId: 'usr-002',
      customerName: 'Lê Minh Tú',
      vendorId: 'vnd-001',
      serviceId: 'srv-001',
      rating: 5,
      comment:
          'Ván SUP chất lượng, áo phao mới tinh. 5h sáng ra biển gió nhẹ sóng êm chèo rất nhẹ tay. 10/10 điểm cho dịch vụ biển Đà Nẵng!',
      createdAt: DateTime(2024, 10, 25, 10, 0),
    ),
  ];

  // ==================================================
  // NOTIFICATIONS
  // ==================================================
  static final List<NotificationModel> notifications = [
    NotificationModel(
      id: 'notif-001',
      userId: 'usr-001',
      type: 'ORDER_CONFIRMED',
      title: 'Vé điện tử QR đã sẵn sàng!',
      body:
          'Nhà cung cấp Danang Ocean Club đã xác nhận slot Chèo SUP sáng 28/10. Bạn có thể mở vé QR để chuẩn bị ra biển.',
      relatedEntityType: 'order',
      relatedEntityId: 'DNS-8924',
      isRead: false,
      createdAt: DateTime.now().subtract(const Duration(minutes: 10)),
    ),
    NotificationModel(
      id: 'notif-002',
      userId: 'usr-001',
      type: 'WEATHER_ALERT',
      title: 'Cập nhật sóng biển sáng nay',
      body:
          'Biển Mỹ Khê sóng êm 0.4m, gió nhẹ 8 km/h, không mưa. Điều kiện lý tưởng cho các hoạt động thể thao mặt nước.',
      relatedEntityType: 'weather',
      isRead: false,
      createdAt: DateTime.now().subtract(const Duration(hours: 2)),
    ),
    NotificationModel(
      id: 'notif-003',
      userId: 'usr-001',
      type: 'REFUND_STATUS',
      title: 'Yêu cầu hoàn tiền đang được xử lý',
      body:
          'Hệ thống đang chuyển 560.000đ về phương thức thanh toán ban đầu của đơn #DNS-8924-1.',
      relatedEntityType: 'refund',
      relatedEntityId: 'ref-001',
      isRead: true,
      createdAt: DateTime.now().subtract(const Duration(days: 1)),
    ),
  ];

  // ==================================================
  // CHAT CONVERSATIONS
  // ==================================================
  static final List<ConversationModel> conversations = [
    ConversationModel(
      id: 'conv-001',
      customerId: 'usr-001',
      vendorId: 'vnd-001',
      vendorName: 'Danang Ocean Club',
      vendorAvatar:
          'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=200&auto=format&fit=crop&q=80',
      masterOrderId: 'DNS-8924',
      lastMessage:
          'Chào bạn An, sáng mai đội ngũ sẵn sàng đón bạn lúc 04:50 tại điểm tập kết Mỹ Khê nhé!',
      lastMessageTime: DateTime.now().subtract(const Duration(minutes: 15)),
      unreadCount: 1,
      isOnline: true,
    ),
    ConversationModel(
      id: 'conv-002',
      customerId: 'usr-001',
      vendorId: 'vnd-002',
      vendorName: 'Sơn Trà Adventure',
      vendorAvatar:
          'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=200&auto=format&fit=crop&q=80',
      masterOrderId: 'DNS-8924',
      lastMessage: 'Đã nhận yêu cầu của bạn về áo lặn kích cỡ L cho 2 người.',
      lastMessageTime: DateTime.now().subtract(const Duration(hours: 3)),
      unreadCount: 0,
      isOnline: false,
    ),
  ];

  static final List<ChatMessageModel> sampleChatMessages = [
    ChatMessageModel(
      id: 'msg-001',
      conversationId: 'conv-001',
      senderId: 'usr-001',
      isMe: true,
      content:
          'Chào shop, sáng mai 5h nhóm mình có mặt thì gửi xe ở bãi nào gần nhất vậy ạ?',
      createdAt: DateTime.now().subtract(const Duration(minutes: 30)),
    ),
    ChatMessageModel(
      id: 'msg-002',
      conversationId: 'conv-001',
      senderId: 'vnd-001',
      isMe: false,
      content:
          'Chào bạn An! Bạn có thể gửi xe tại bãi tắm số 2 đối diện khách sạn Mường Thanh nhé, có nhân viên trực 24/24.',
      createdAt: DateTime.now().subtract(const Duration(minutes: 25)),
    ),
    ChatMessageModel(
      id: 'msg-003',
      conversationId: 'conv-001',
      senderId: 'vnd-001',
      isMe: false,
      content:
          'Chào bạn An, sáng mai đội ngũ sẵn sàng đón bạn lúc 04:50 tại điểm tập kết Mỹ Khê nhé!',
      createdAt: DateTime.now().subtract(const Duration(minutes: 15)),
    ),
  ];

  // ==================================================
  // AI ASSISTANT CONVERSATION
  // ==================================================
  static final List<AiMessageModel> aiInitialMessages = [
    AiMessageModel(
      id: 'ai-001',
      role: AiRole.assistant,
      content:
          'Chào bạn! Tôi là Trợ lý AI DANASEA. Hôm nay biển Đà Nẵng rất đẹp với sóng êm 0.4m tại Mỹ Khê và nước trong tại Sơn Trà. Bạn muốn khám phá trải nghiệm nào?',
      createdAt: DateTime.now().subtract(const Duration(minutes: 10)),
    ),
    AiMessageModel(
      id: 'ai-002',
      role: AiRole.user,
      content:
          'Bọn mình có 2 người, muốn đi trải nghiệm biển vào sáng sớm mai ở Mỹ Khê, ngân sách tầm 300k/người thì nên chọn gì bạn nhỉ?',
      createdAt: DateTime.now().subtract(const Duration(minutes: 8)),
    ),
    AiMessageModel(
      id: 'ai-003',
      role: AiRole.assistant,
      content:
          'Với 2 người và ngân sách 300.000đ/người vào sáng sớm mai tại Mỹ Khê, hoạt động lý tưởng nhất là Chèo SUP đón bình minh Mỹ Khê! Giá trọn gói chỉ 280.000đ/người đã gồm chụp ảnh và đồ uống.',
      recommendedService: services[0],
      createdAt: DateTime.now().subtract(const Duration(minutes: 7)),
    ),
  ];
}
