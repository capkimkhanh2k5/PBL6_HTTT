import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';

class VendorChatScreen extends StatefulWidget {
  final ConversationModel conversation;

  const VendorChatScreen({
    super.key,
    required this.conversation,
  });

  @override
  State<VendorChatScreen> createState() => _VendorChatScreenState();
}

class _VendorChatScreenState extends State<VendorChatScreen> {
  late List<ChatMessageModel> _messages;
  final TextEditingController _textController = TextEditingController();

  final List<String> _quickPrompts = [
    'Điểm gửi xe ở đâu?',
    'Có tủ khóa gửi đồ không?',
    'Mấy giờ có mặt là vừa?',
  ];

  @override
  void initState() {
    super.initState();
    _messages = List.from(MockDatabaseData.sampleChatMessages);
  }

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  void _sendMessage([String? customText]) {
    final content = customText ?? _textController.text.trim();
    if (content.isEmpty) return;

    setState(() {
      _messages.add(
        ChatMessageModel(
          id: 'msg-${DateTime.now().millisecondsSinceEpoch}',
          conversationId: widget.conversation.id,
          senderId: MockDatabaseData.currentUser.id,
          isMe: true,
          content: content,
          createdAt: DateTime.now(),
        ),
      );
    });

    if (customText == null) {
      _textController.clear();
    }
  }

  @override
  Widget build(BuildContext context) {
    final conv = widget.conversation;

    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        titleSpacing: 0,
        title: Row(
          children: [
            CircleAvatar(
              radius: 18,
              backgroundColor: AppColors.secondaryContainer,
              backgroundImage: conv.vendorAvatar != null
                  ? NetworkImage(conv.vendorAvatar!)
                  : null,
              child: conv.vendorAvatar == null
                  ? LocalizedText(conv.vendorName[0])
                  : null,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Flexible(
                        child: LocalizedText(
                          conv.vendorName,
                          style: AppTypography.labelMd(
                            color: AppColors.onSurface,
                            fontWeight: FontWeight.w700,
                          ),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 4),
                      const Icon(Icons.verified,
                          size: 13, color: AppColors.secondary),
                    ],
                  ),
                  LocalizedText(
                    'Đang hoạt động trên biển',
                    style: AppTypography.bodySm(color: AppColors.secondary)
                        .copyWith(fontSize: 10),
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.phone_outlined,
                color: AppColors.secondary),
            onPressed: () {
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(content: LocalizedText('Gọi hotline đối tác: 0905 888 999')),
              );
            },
          ),
        ],
      ),
      body: Column(
        children: [
          // ORDER CONTEXT PILL
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            color: AppColors.surfaceContainerLow,
            child: Row(
              children: [
                const Icon(Icons.sailing, size: 16, color: AppColors.secondary),
                const SizedBox(width: 8),
                Expanded(
                  child: LocalizedText(
                    'Đơn hàng #${conv.masterOrderId ?? 'DNS-8924'} • Chèo SUP đón bình minh Mỹ Khê',
                    style: AppTypography.bodySm(
                      color: AppColors.onSurface,
                    ).copyWith(fontSize: 11),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                const Icon(Icons.chevron_right,
                    size: 16, color: AppColors.outline),
              ],
            ),
          ),

          // MESSAGES STREAM
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(AppShapes.gutterMobile),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final msg = _messages[index];
                return Align(
                  alignment:
                      msg.isMe ? Alignment.centerRight : Alignment.centerLeft,
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 10),
                    padding: const EdgeInsets.symmetric(
                        horizontal: 14, vertical: 10),
                    constraints: BoxConstraints(
                      maxWidth: MediaQuery.of(context).size.width * 0.75,
                    ),
                    decoration: BoxDecoration(
                      color: msg.isMe
                          ? AppColors.primary
                          : AppColors.surfaceContainerLowest,
                      borderRadius: BorderRadius.only(
                        topLeft: const Radius.circular(16),
                        topRight: const Radius.circular(16),
                        bottomLeft: Radius.circular(msg.isMe ? 16 : 4),
                        bottomRight: Radius.circular(msg.isMe ? 4 : 16),
                      ),
                      boxShadow: AppShapes.shadowLevel1,
                    ),
                    child: Column(
                      crossAxisAlignment: msg.isMe
                          ? CrossAxisAlignment.end
                          : CrossAxisAlignment.start,
                      children: [
                        LocalizedText(
                          msg.content,
                          style: AppTypography.bodyMd(
                            color: msg.isMe
                                ? Colors.white
                                : AppColors.onSurface,
                          ),
                        ),
                        const SizedBox(height: 4),
                        LocalizedText(
                          '${msg.createdAt.hour}:${msg.createdAt.minute.toString().padLeft(2, '0')}',
                          style: TextStyle(
                            fontSize: 10,
                            color: msg.isMe
                                ? Colors.white.withValues(alpha: 0.7)
                                : AppColors.outline,
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),

          // QUICK PROMPTS CHIPS
          Container(
            height: 38,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _quickPrompts.length,
              separatorBuilder: (_, unused) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                return ActionChip(
                  label: LocalizedText(
                    _quickPrompts[index],
                    style: AppTypography.labelSm(color: AppColors.secondary),
                  ),
                  backgroundColor:
                      AppColors.secondaryContainer.withValues(alpha: 0.3),
                  onPressed: () => _sendMessage(_quickPrompts[index]),
                );
              },
            ),
          ),
          const SizedBox(height: 6),

          // INPUT BAR
          Container(
            padding: EdgeInsets.only(
              left: 12,
              right: 12,
              top: 8,
              bottom: MediaQuery.of(context).padding.bottom + 8,
            ),
            decoration: BoxDecoration(
              color: AppColors.surfaceContainerLowest,
              border: Border(
                top: BorderSide(
                  color: AppColors.borderSubtle.withValues(alpha: 0.8),
                ),
              ),
            ),
            child: Row(
              children: [
                IconButton(
                  icon: const Icon(Icons.add_photo_alternate_outlined,
                      color: AppColors.secondary),
                  onPressed: () {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: LocalizedText('Tính năng đính kèm ảnh')),
                    );
                  },
                ),
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: TextField(
                      controller: _textController,
                      decoration: InputDecoration(
                        hintText: tr(context, 'Nhập tin nhắn cho nhà cung cấp...'),
                        hintStyle:
                            AppTypography.bodySm(color: AppColors.outline),
                        border: InputBorder.none,
                        isDense: true,
                        contentPadding:
                            const EdgeInsets.symmetric(vertical: 10),
                      ),
                      onSubmitted: (_) => _sendMessage(),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                CircleAvatar(
                  backgroundColor: AppColors.primary,
                  radius: 20,
                  child: IconButton(
                    icon: const Icon(Icons.send, size: 18, color: Colors.white),
                    onPressed: () => _sendMessage(),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
