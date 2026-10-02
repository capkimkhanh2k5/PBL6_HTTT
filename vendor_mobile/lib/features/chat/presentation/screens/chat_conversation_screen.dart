import 'package:flutter/material.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/data/vendor_mock_database.dart';
import '../../../../core/models/vendor_models.dart';
import '../../../orders/presentation/screens/sub_order_detail_screen.dart';

class ChatConversationScreen extends StatefulWidget {
  final String conversationId;
  final String customerName;
  final String subOrderCode;

  const ChatConversationScreen({
    super.key,
    required this.conversationId,
    required this.customerName,
    required this.subOrderCode,
  });

  @override
  State<ChatConversationScreen> createState() => _ChatConversationScreenState();
}

class _ChatConversationScreenState extends State<ChatConversationScreen> {
  final _db = VendorMockDatabase.instance;
  final _msgController = TextEditingController();
  final _scrollController = ScrollController();

  @override
  void initState() {
    super.initState();
    _db.addListener(_onDbChanged);
    _scrollToBottom();
  }

  @override
  void dispose() {
    _db.removeListener(_onDbChanged);
    _msgController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _onDbChanged() {
    if (mounted) setState(() {});
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  void _sendMessage(String text) {
    if (text.trim().isEmpty) return;
    _db.sendMessage(widget.conversationId, 'vnd-001', text.trim());
    _msgController.clear();
    _scrollToBottom();
  }

  @override
  Widget build(BuildContext context) {
    final messages = _db.messages[widget.conversationId] ?? [];
    final subOrder = _db.subOrders.firstWhere(
      (o) => o.subOrderCode == widget.subOrderCode,
      orElse: () => _db.subOrders.first,
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
        title: Row(
          children: [
            Container(
              width: 36,
              height: 36,
              decoration: const BoxDecoration(
                shape: BoxShape.circle,
                color: AppColors.secondaryContainer,
              ),
              child: Center(
                child: Text(
                  widget.customerName.isNotEmpty ? widget.customerName[0].toUpperCase() : 'K',
                  style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: AppColors.onSecondaryContainer),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    widget.customerName,
                    style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, color: AppColors.onSurface),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(width: 6, height: 6, decoration: const BoxDecoration(shape: BoxShape.circle, color: AppColors.secondary)),
                      const SizedBox(width: 4),
                      const Flexible(
                        child: Text(
                          'Đang trực tuyến',
                          style: TextStyle(fontSize: 10, color: AppColors.secondary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.call_outlined, color: AppColors.secondary),
            onPressed: () {},
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            // Pinned Sub-Order Context Banner
            Container(
              margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                borderRadius: AppShapes.radiusSm,
                boxShadow: AppShapes.shadowSm,
              ),
              child: Row(
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: AppColors.secondaryContainer,
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(Icons.surfing, color: AppColors.onSecondaryContainer, size: 20),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Flexible(
                              child: Text(
                                '#${subOrder.subOrderCode}',
                                style: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.secondary),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            const SizedBox(width: 4),
                            Text('• ${subOrder.quantity} khách', style: const TextStyle(fontSize: 10, color: AppColors.tertiary)),
                          ],
                        ),
                        Text(subOrder.serviceName, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSurface), maxLines: 1, overflow: TextOverflow.ellipsis),
                        Text('${subOrder.slotDate} • ${subOrder.slotTime}', style: const TextStyle(fontSize: 10, color: AppColors.tertiary), maxLines: 1, overflow: TextOverflow.ellipsis),
                      ],
                    ),
                  ),
                  TextButton(
                    onPressed: () {
                      Navigator.of(context).push(
                        MaterialPageRoute(
                          builder: (_) => SubOrderDetailScreen(subOrder: subOrder),
                        ),
                      );
                    },
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                      backgroundColor: AppColors.secondaryContainer.withOpacity(0.5),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                    ),
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('Xem đơn', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: AppColors.onSecondaryContainer)),
                        Icon(Icons.chevron_right, size: 14, color: AppColors.onSecondaryContainer),
                      ],
                    ),
                  ),
                ],
              ),
            ),

            // Messages Stream
            Expanded(
              child: ListView.builder(
                controller: _scrollController,
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                itemCount: messages.length + 1, // +1 for initial payment banner
                itemBuilder: (context, index) {
                  if (index == 0) {
                    return _buildPaymentNoticeBubble(subOrder);
                  }
                  final msg = messages[index - 1];
                  final isMe = msg.senderId == 'vnd-001';
                  return _buildMessageBubble(msg, isMe);
                },
              ),
            ),

            // Quick Recommendation Reply Pills
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
              child: Row(
                children: [
                  _buildQuickPill('Có sẵn túi chống nước', 'Bên mình có trang bị sẵn túi chống nước cho mỗi khách rồi bạn nhé!'),
                  const SizedBox(width: 8),
                  _buildQuickPill('Gửi đồ tại quầy', 'Dạ có tủ gửi đồ an toàn tại quầy luôn bạn nhé.'),
                  const SizedBox(width: 8),
                  _buildQuickPill('Vị trí tập kết', 'Quầy Danang Ocean Club tại Lô 12 Võ Nguyên Giáp, bãi tắm số 3 Mỹ Khê ạ.'),
                ],
              ),
            ),

            // Input Bar
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: AppColors.surfaceContainerLowest,
                boxShadow: [
                  BoxShadow(color: Colors.black.withOpacity(0.04), blurRadius: 8, offset: const Offset(0, -2)),
                ],
              ),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.add_photo_alternate_outlined, color: AppColors.secondary, size: 22),
                    onPressed: () {
                      _sendMessage('📍 Đã gửi vị trí: Bến bãi Danang Ocean Club - Bãi biển Mỹ Khê');
                    },
                  ),
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 14),
                      decoration: BoxDecoration(
                        color: AppColors.surfaceContainerLow,
                        borderRadius: BorderRadius.circular(24),
                      ),
                      child: TextField(
                        controller: _msgController,
                        style: const TextStyle(fontSize: 13),
                        decoration: const InputDecoration(
                          hintText: 'Nhập tin nhắn cho khách...',
                          hintStyle: TextStyle(fontSize: 12, color: AppColors.outline),
                          border: InputBorder.none,
                        ),
                        onSubmitted: _sendMessage,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  InkWell(
                    onTap: () => _sendMessage(_msgController.text),
                    borderRadius: BorderRadius.circular(20),
                    child: Container(
                      width: 40,
                      height: 40,
                      decoration: const BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppColors.secondary,
                      ),
                      child: const Icon(Icons.send, color: Colors.white, size: 18),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPaymentNoticeBubble(SubOrderModel order) {
    return Center(
      child: Container(
        margin: const EdgeInsets.symmetric(vertical: 10),
        padding: const EdgeInsets.all(12),
        constraints: const BoxConstraints(maxWidth: 320),
        decoration: BoxDecoration(
          color: AppColors.surfaceContainer,
          borderRadius: AppShapes.radiusSm,
          boxShadow: AppShapes.shadowSm,
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              width: 32,
              height: 32,
              decoration: const BoxDecoration(shape: BoxShape.circle, color: AppColors.primaryFixed),
              child: const Icon(Icons.verified, color: AppColors.primary, size: 18),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Khách đã thanh toán', style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppColors.onSurface)),
                  const SizedBox(height: 2),
                  Text(
                    '${order.customerName} đã đặt dịch vụ #${order.subOrderCode} • Tổng ${order.totalPrice.toStringAsFixed(0)} đ',
                    style: const TextStyle(fontSize: 11, color: AppColors.onSurfaceVariant),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildMessageBubble(MessageModel msg, bool isMe) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: isMe ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          if (!isMe)
            Container(
              width: 28,
              height: 28,
              margin: const EdgeInsets.only(right: 6, bottom: 2),
              decoration: const BoxDecoration(shape: BoxShape.circle, color: AppColors.surfaceContainerHigh),
              child: const Icon(Icons.person, size: 16, color: AppColors.onSurfaceVariant),
            ),
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
              decoration: BoxDecoration(
                color: isMe ? AppColors.secondary : AppColors.surfaceContainerLowest,
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(16),
                  topRight: const Radius.circular(16),
                  bottomLeft: Radius.circular(isMe ? 16 : 4),
                  bottomRight: Radius.circular(isMe ? 4 : 16),
                ),
                boxShadow: AppShapes.shadowSm,
              ),
              child: Column(
                crossAxisAlignment: isMe ? CrossAxisAlignment.end : CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    msg.content,
                    style: TextStyle(
                      fontSize: 13,
                      color: isMe ? Colors.white : AppColors.onSurface,
                      height: 1.4,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '${msg.sentAt.hour}:${msg.sentAt.minute.toString().padLeft(2, '0')}',
                        style: TextStyle(
                          fontSize: 10,
                          color: isMe ? Colors.white70 : AppColors.tertiary,
                        ),
                      ),
                      if (isMe) ...[
                        const SizedBox(width: 4),
                        const Icon(Icons.done_all, size: 12, color: Colors.white70),
                      ],
                    ],
                  ),
                ],
              ),
            ),
          ),
          if (isMe)
            Container(
              width: 28,
              height: 28,
              margin: const EdgeInsets.only(left: 6, bottom: 2),
              decoration: const BoxDecoration(shape: BoxShape.circle, color: AppColors.secondary),
              child: const Center(
                child: Text('DO', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: Colors.white)),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildQuickPill(String title, String fullText) {
    return InkWell(
      onTap: () => _sendMessage(fullText),
      borderRadius: BorderRadius.circular(16),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
        decoration: BoxDecoration(
          color: AppColors.surfaceContainerLowest,
          borderRadius: BorderRadius.circular(16),
          boxShadow: AppShapes.shadowSm,
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.flash_on, size: 13, color: AppColors.secondary),
            const SizedBox(width: 4),
            Text(title, style: const TextStyle(fontSize: 11, color: AppColors.onSurface)),
          ],
        ),
      ),
    );
  }
}
