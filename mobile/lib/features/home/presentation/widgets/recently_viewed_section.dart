import 'package:flutter/material.dart';
import '../../../../core/data/library_store.dart';
import '../../../../core/l10n/app_localizations.dart';

class RecentlyViewedSection extends StatefulWidget {
  final LibraryStore? store;
  final ValueChanged<SavedService>? onItemTap;
  const RecentlyViewedSection({super.key, this.store, this.onItemTap});
  @override
  State<RecentlyViewedSection> createState() => _RecentlyViewedSectionState();
}

class _RecentlyViewedSectionState extends State<RecentlyViewedSection> {
  late final LibraryStore store;
  @override
  void initState() {
    super.initState();
    store = widget.store ?? LibraryStore.instance;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) store.loadRecent();
    });
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: store,
    builder: (context, _) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.history),
              const SizedBox(width: 8),
              const Expanded(child: LocalizedText('Gần đây bạn xem')),
              IconButton(
                onPressed: store.loadingRecent ? null : store.loadRecent,
                tooltip: translate('Tải lại', AppLanguage.instance.code),
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
          if (store.loadingRecent) const LinearProgressIndicator(),
          if (store.recentError != null) ...[
            Text(store.recentError!),
            TextButton(
              onPressed: store.loadRecent,
              child: const LocalizedText('Thử lại'),
            ),
          ] else if (!store.loadingRecent && store.recent.isEmpty)
            const LocalizedText('Bạn chưa xem trải nghiệm nào.'),
          if (store.recent.isNotEmpty)
            SizedBox(
              height: 205,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: store.recent.length,
                separatorBuilder: (_, _) => const SizedBox(width: 12),
                itemBuilder: (context, index) {
                  final item = store.recent[index];
                  return SizedBox(
                    width: 215,
                    child: Card(
                      clipBehavior: Clip.antiAlias,
                      child: InkWell(
                        onTap: () => widget.onItemTap?.call(item),
                        child: Column(
                          children: [
                            if (item.imageUrl?.isNotEmpty == true)
                              Image.network(
                                item.imageUrl!,
                                height: 125,
                                width: double.infinity,
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => const SizedBox(
                                  height: 125,
                                  child: Icon(Icons.image_not_supported),
                                ),
                              )
                            else
                              const SizedBox(
                                height: 125,
                                child: Icon(Icons.beach_access),
                              ),
                            Padding(
                              padding: const EdgeInsets.all(12),
                              child: Text(
                                item.name,
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    ),
  );
}
