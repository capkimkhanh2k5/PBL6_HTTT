import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import 'vendor_chat_screen.dart';

class ConversationsScreen extends StatefulWidget {
  const ConversationsScreen({super.key});

  @override
  State<ConversationsScreen> createState() => _ConversationsScreenState();
}

class _ConversationsScreenState extends State<ConversationsScreen> {
  int _selectedPillIndex = 0;
  final TextEditingController _searchController = TextEditingController();

  final List<String> _pills = [
    'Tất cả (2)',
    'Chưa đọc (1)',
    'Đang có tour (1)',
    'Hỗ trợ CSKH',
  ];

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final convs = MockDatabaseData.conversations;

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
          'Tin nhắn',
          style: AppTypography.headlineSm(color: AppColors.onSurface),
        ),
        centerTitle: true,
      ),
      body: Column(
        children: [
          // LIVE OCEAN BANNER
          Padding(
            padding: const EdgeInsets.symmetric(
                horizontal: AppShapes.gutterMobile, vertical: 4),
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: BorderRadius.circular(12),
              ),
              child: Row(
                children: [
                  Container(
                    width: 8,
                    height: 8,
                    decoration: const BoxDecoration(
                      color: AppColors.secondary,
                      shape: BoxShape.circle,
                    ),
                  ),
                  const SizedBox(width: 8),
                  const Icon(Icons.waves, size: 16, color: AppColors.secondary),
                  const SizedBox(width: 6),
                  Expanded(
                    child: LocalizedText(
                      'Biển Mỹ Khê sóng êm (0.4m) • Kênh phản hồi trực tiếp',
                      style: AppTypography.bodySm(
                        color: AppColors.onSurface,
                      ).copyWith(fontSize: 11),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  LocalizedText(
                    'Trực tuyến',
                    style: AppTypography.labelSm(
                      color: AppColors.secondary,
                      fontWeight: FontWeight.w700,
                    ).copyWith(fontSize: 11),
                  ),
                ],
              ),
            ),
          ),

          // SEARCH INPUT
          Padding(
            padding: const EdgeInsets.symmetric(
                horizontal: AppShapes.gutterMobile, vertical: 8),
            child: Container(
              height: 42,
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLow,
                borderRadius: BorderRadius.circular(21),
              ),
              child: TextField(
                controller: _searchController,
                decoration: InputDecoration(
                  hintText: tr(context, 'Tìm đối tác hoặc mã đơn...'),
                  hintStyle: AppTypography.bodySm(color: AppColors.outline),
                  prefixIcon: const Icon(Icons.search,
                      size: 20, color: AppColors.secondary),
                  border: InputBorder.none,
                  contentPadding: const EdgeInsets.symmetric(vertical: 10),
                ),
              ),
            ),
          ),

          // PILLS
          Container(
            height: 40,
            padding: const EdgeInsets.symmetric(vertical: 2),
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(
                  horizontal: AppShapes.gutterMobile),
              itemCount: _pills.length,
              separatorBuilder: (_, unused) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                final isSel = _selectedPillIndex == index;
                return InkWell(
                  onTap: () => setState(() => _selectedPillIndex = index),
                  borderRadius: BorderRadius.circular(16),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                        horizontal: 14, vertical: 6),
                    decoration: BoxDecoration(
                      color: isSel
                          ? AppColors.secondary
                          : AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(16),
                    ),
                    child: LocalizedText(
                      _pills[index],
                      style: AppTypography.labelSm(
                        color: isSel ? Colors.white : AppColors.onSurfaceVariant,
                        fontWeight: isSel ? FontWeight.w700 : FontWeight.w500,
                      ),
                    ),
                  ),
                );
              },
            ),
          ),

          // CONVERSATIONS LIST
          Expanded(
            child: ListView.separated(
              padding: const EdgeInsets.all(AppShapes.gutterMobile),
              itemCount: convs.length,
              separatorBuilder: (_, unused) => const Divider(height: 12),
              itemBuilder: (context, index) {
                final conv = convs[index];
                return InkWell(
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (_) => VendorChatScreen(conversation: conv),
                      ),
                    );
                  },
                  borderRadius: BorderRadius.circular(12),
                  child: Padding(
                    padding: const EdgeInsets.all(8),
                    child: Row(
                      children: [
                        // Avatar with online status
                        Stack(
                          children: [
                            CircleAvatar(
                              radius: 26,
                              backgroundColor: AppColors.secondaryContainer,
                              backgroundImage: conv.vendorAvatar != null
                                  ? NetworkImage(conv.vendorAvatar!)
                                  : null,
                              child: conv.vendorAvatar == null
                                  ? LocalizedText(conv.vendorName[0])
                                  : null,
                            ),
                            if (conv.isOnline)
                              Positioned(
                                bottom: 0,
                                right: 0,
                                child: Container(
                                  width: 12,
                                  height: 12,
                                  decoration: BoxDecoration(
                                    color: AppColors.secondary,
                                    shape: BoxShape.circle,
                                    border: Border.all(
                                        color: Colors.white, width: 2),
                                  ),
                                ),
                              ),
                          ],
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment:
                                    MainAxisAlignment.spaceBetween,
                                children: [
                                  Expanded(
                                    child: LocalizedText(
                                      conv.vendorName,
                                      maxLines: 1,
                                      overflow: TextOverflow.ellipsis,
                                      style: AppTypography.labelLg(
                                        color: AppColors.onSurface,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  LocalizedText(
                                    '${conv.lastMessageTime.hour}:${conv.lastMessageTime.minute.toString().padLeft(2, '0')}',
                                    style: AppTypography.bodySm(
                                            color: AppColors.outline)
                                        .copyWith(fontSize: 11),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              LocalizedText(
                                conv.lastMessage,
                                style: AppTypography.bodySm(
                                  color: conv.unreadCount > 0
                                      ? AppColors.onSurface
                                      : AppColors.onSurfaceVariant,
                                  fontWeight: conv.unreadCount > 0
                                      ? FontWeight.w700
                                      : FontWeight.w400,
                                ),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        if (conv.unreadCount > 0) ...[
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.all(6),
                            decoration: const BoxDecoration(
                              color: AppColors.primary,
                              shape: BoxShape.circle,
                            ),
                            child: LocalizedText(
                              '${conv.unreadCount}',
                              style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 10,
                                  fontWeight: FontWeight.bold),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
