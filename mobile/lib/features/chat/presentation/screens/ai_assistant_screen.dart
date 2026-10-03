import 'package:mobile/core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import '../../../../core/data/mock_database_data.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../../core/theme/app_shapes.dart';
import '../../../../core/theme/app_typography.dart';
import '../../../experience_detail/presentation/screens/experience_detail_screen.dart';

class AiAssistantScreen extends StatefulWidget {
  const AiAssistantScreen({super.key});

  @override
  State<AiAssistantScreen> createState() => _AiAssistantScreenState();
}

class _AiAssistantScreenState extends State<AiAssistantScreen> {
  late List<AiMessageModel> _messages;
  final TextEditingController _controller = TextEditingController();

  final List<String> _suggestedPrompts = [
    'Đi biển 2 người dưới 300k/người?',
    'Lặn ngắm san hô Sơn Trà hôm nay biển có trong không?',
    'Gợi ý lịch trình biển 1 ngày ở Đà Nẵng',
  ];

  @override
  void initState() {
    super.initState();
    _messages = List.from(MockDatabaseData.aiInitialMessages);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _sendQuery(String query) {
    if (query.trim().isEmpty) return;

    setState(() {
      _messages.add(
        AiMessageModel(
          id: 'ai-usr-${DateTime.now().millisecondsSinceEpoch}',
          role: AiRole.user,
          content: query,
          createdAt: DateTime.now(),
        ),
      );
    });
    _controller.clear();

    // AI Response simulation
    Future.delayed(const Duration(milliseconds: 600), () {
      if (!mounted) return;
      setState(() {
        _messages.add(
          AiMessageModel(
            id: 'ai-bot-${DateTime.now().millisecondsSinceEpoch}',
            role: AiRole.assistant,
            content:
                'Dựa trên thông số sóng biển hiện tại tại bán đảo Sơn Trà (sóng 0.4m, nước trong tầm nhìn 8m), đây là gợi ý tối ưu nhất cho bạn:',
            recommendedService: MockDatabaseData.services[1],
            createdAt: DateTime.now(),
          ),
        );
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        backgroundColor: AppColors.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: AppColors.onSurface),
          onPressed: () => Navigator.pop(context),
        ),
        title: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              padding: const EdgeInsets.all(4),
              decoration: BoxDecoration(
                color: AppColors.secondaryContainer,
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Icon(Icons.auto_awesome,
                  size: 16, color: AppColors.secondary),
            ),
            const SizedBox(width: 6),
            Flexible(
              child: LocalizedText(
                'DANASEA AI',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: AppTypography.headlineSm(color: AppColors.onSurface),
              ),
            ),
          ],
        ),
        centerTitle: true,
        actions: [
          IconButton(
            onPressed: () {
              setState(() {
                _messages = [MockDatabaseData.aiInitialMessages.first];
              });
            },
            icon: const Icon(Icons.refresh, size: 20, color: AppColors.secondary),
            tooltip: tr(context, 'Làm mới'),
          ),
        ],
      ),
      body: Column(
        children: [
          // LIVE OCEAN ALERT STRIP
          Container(
            width: double.infinity,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            color: AppColors.surfaceContainerLow,
            child: Row(
              children: [
                const Icon(Icons.waves, size: 16, color: AppColors.secondary),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      LocalizedText(
                        'TRẠNG THÁI BIỂN ĐÀ NẴNG HÔM NAY',
                        style: AppTypography.labelSm(
                          color: AppColors.onSurfaceVariant,
                          fontWeight: FontWeight.w700,
                        ).copyWith(fontSize: 10),
                      ),
                      LocalizedText(
                        'Mỹ Khê & Sơn Trà: Sóng 0.4m • Nắng dịu • Lý tưởng',
                        style: AppTypography.labelSm(
                          color: AppColors.secondary,
                          fontWeight: FontWeight.w700,
                        ).copyWith(fontSize: 11),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: AppColors.secondaryContainer,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: LocalizedText(
                    'LIVE',
                    style: AppTypography.labelSm(
                      color: AppColors.onSecondaryContainer,
                      fontWeight: FontWeight.w800,
                    ).copyWith(fontSize: 10),
                  ),
                ),
              ],
            ),
          ),

          // CHAT STREAM
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.all(AppShapes.gutterMobile),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final msg = _messages[index];
                final isUser = msg.role == AiRole.user;

                return Column(
                  crossAxisAlignment: isUser
                      ? CrossAxisAlignment.end
                      : CrossAxisAlignment.start,
                  children: [
                    // Sender info
                    Padding(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 4, vertical: 2),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          if (!isUser) ...[
                            Container(
                              width: 18,
                              height: 18,
                              decoration: const BoxDecoration(
                                color: AppColors.secondary,
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(Icons.smart_toy,
                                  size: 11, color: Colors.white),
                            ),
                            const SizedBox(width: 4),
                          ],
                          LocalizedText(
                            isUser ? 'Bạn' : 'DANASEA AI',
                            style: AppTypography.labelSm(
                              color: AppColors.onSurfaceVariant,
                            ).copyWith(fontSize: 10),
                          ),
                        ],
                      ),
                    ),

                    // Bubble
                    Container(
                      margin: const EdgeInsets.only(bottom: 6),
                      padding: const EdgeInsets.all(12),
                      constraints: BoxConstraints(
                        maxWidth: MediaQuery.of(context).size.width * 0.85,
                      ),
                      decoration: BoxDecoration(
                        color: isUser
                            ? AppColors.primary
                            : AppColors.surfaceContainerLowest,
                        borderRadius: BorderRadius.circular(16),
                        boxShadow: AppShapes.shadowLevel1,
                        border: isUser
                            ? null
                            : Border.all(color: AppColors.borderSubtle),
                      ),
                      child: LocalizedText(
                        msg.content,
                        style: AppTypography.bodyMd(
                          color: isUser ? Colors.white : AppColors.onSurface,
                        ),
                      ),
                    ),

                    // Embedded Recommendation Card (if AI suggests a service)
                    if (msg.recommendedService != null)
                      _buildRecommendedCard(msg.recommendedService!),

                    const SizedBox(height: 10),
                  ],
                );
              },
            ),
          ),

          // SUGGESTED PROMPTS
          Container(
            height: 38,
            padding: const EdgeInsets.symmetric(horizontal: 12),
            child: ListView.separated(
              scrollDirection: Axis.horizontal,
              itemCount: _suggestedPrompts.length,
              separatorBuilder: (_, unused) => const SizedBox(width: 8),
              itemBuilder: (context, index) {
                return ActionChip(
                  label: LocalizedText(
                    _suggestedPrompts[index],
                    style: AppTypography.labelSm(color: AppColors.secondary),
                  ),
                  backgroundColor:
                      AppColors.secondaryContainer.withValues(alpha: 0.3),
                  onPressed: () => _sendQuery(_suggestedPrompts[index]),
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
                Expanded(
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 14),
                    decoration: BoxDecoration(
                      color: AppColors.surfaceContainerLow,
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: TextField(
                      controller: _controller,
                      decoration: InputDecoration(
                        hintText: tr(context, 'Hỏi bất kỳ điều gì về du lịch biển Đà Nẵng...'),
                        hintStyle:
                            AppTypography.bodySm(color: AppColors.outline),
                        border: InputBorder.none,
                        isDense: true,
                        contentPadding:
                            const EdgeInsets.symmetric(vertical: 10),
                      ),
                      onSubmitted: _sendQuery,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                CircleAvatar(
                  backgroundColor: AppColors.primary,
                  radius: 20,
                  child: IconButton(
                    icon: const Icon(Icons.send, size: 18, color: Colors.white),
                    onPressed: () => _sendQuery(_controller.text),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecommendedCard(ServiceModel service) {
    return Container(
      margin: const EdgeInsets.only(top: 4, bottom: 8),
      width: double.infinity,
      decoration: BoxDecoration(
        color: AppColors.surfaceContainerLowest,
        borderRadius: AppShapes.radiusDefault,
        boxShadow: AppShapes.shadowLevel2,
        border: Border.all(color: AppColors.borderOcean),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            height: 120,
            width: double.infinity,
            child: Image.network(
              service.imageUrls.isNotEmpty
                  ? service.imageUrls.first
                  : 'https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=500&auto=format&fit=crop&q=80',
              fit: BoxFit.cover,
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(AppShapes.spaceSm),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: LocalizedText(
                        service.vendorName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.labelSm(
                          color: AppColors.secondary,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 6, vertical: 1),
                      decoration: BoxDecoration(
                        color: AppColors.primaryFixed,
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: LocalizedText(
                        'Được gợi ý',
                        style: AppTypography.labelSm(
                          color: AppColors.primary,
                        ).copyWith(fontSize: 10),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 4),
                LocalizedText(
                  service.name,
                  style: AppTypography.headlineSm(
                    color: AppColors.onSurface,
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Expanded(
                      child: LocalizedText(
                        '${service.price ~/ 1000}.000 đ / khách',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: AppTypography.headlineSm(
                          color: AppColors.primary,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppColors.primary,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                        padding: const EdgeInsets.symmetric(
                            horizontal: 14, vertical: 6),
                      ),
                      onPressed: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) =>
                                ExperienceDetailScreen(service: service),
                          ),
                        );
                      },
                      child: LocalizedText(
                        'Xem & Đặt',
                        style: AppTypography.labelSm(color: Colors.white),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
