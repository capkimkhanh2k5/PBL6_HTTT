import 'package:flutter/material.dart';
import '../../../../core/data/library_store.dart';
import '../../../../core/l10n/app_localizations.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../experience_detail/presentation/screens/catalog_detail_screen.dart';

class WishlistScreen extends StatefulWidget {
  final VoidCallback? onExplore;
  final LibraryStore? store;
  const WishlistScreen({super.key, this.onExplore, this.store});
  @override
  State<WishlistScreen> createState() => _WishlistScreenState();
}

class _WishlistScreenState extends State<WishlistScreen> {
  late final LibraryStore store;
  @override
  void initState() {
    super.initState();
    store = widget.store ?? LibraryStore.instance;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) store.loadWishlist();
    });
  }

  Future<void> _remove(SavedService item) async {
    try {
      await store.toggle(item);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(e.toString())));
      }
    }
  }

  @override
  Widget build(BuildContext context) => ListenableBuilder(
    listenable: store,
    builder: (context, _) => Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(
        automaticallyImplyLeading: false,
        title: LocalizedText('Yêu thích (${store.wishlist.length})'),
      ),
      body: RefreshIndicator(
        onRefresh: store.loadWishlist,
        child: ListView(
          physics: const AlwaysScrollableScrollPhysics(),
          padding: const EdgeInsets.all(16),
          children: [
            if (store.loadingWishlist) const LinearProgressIndicator(),
            if (store.wishlistError != null) ...[
              Text(store.wishlistError!),
              TextButton(
                onPressed: store.loadWishlist,
                child: const LocalizedText('Thử lại'),
              ),
            ],
            if (store.wishlistReady &&
                !store.loadingWishlist &&
                store.wishlist.isEmpty &&
                store.wishlistError == null) ...[
              const Padding(
                padding: EdgeInsets.all(32),
                child: LocalizedText('Chưa có trải nghiệm yêu thích.'),
              ),
              TextButton(
                onPressed: widget.onExplore,
                child: const LocalizedText('Khám phá trải nghiệm'),
              ),
            ],
            for (final item in store.wishlist)
              Card(
                child: ListTile(
                  leading: item.imageUrl?.isNotEmpty == true
                      ? Image.network(
                          item.imageUrl!,
                          width: 64,
                          height: 64,
                          fit: BoxFit.cover,
                          errorBuilder: (_, _, _) =>
                              const Icon(Icons.image_not_supported),
                        )
                      : const Icon(Icons.beach_access),
                  title: Text(item.name),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => CatalogDetailScreen(
                        serviceId: item.serviceId,
                        library: store,
                      ),
                    ),
                  ),
                  trailing: IconButton(
                    tooltip: translate(
                      'Bỏ yêu thích',
                      AppLanguage.instance.code,
                    ),
                    onPressed: store.busy(item.serviceId)
                        ? null
                        : () => _remove(item),
                    icon: const Icon(Icons.favorite, color: AppColors.primary),
                  ),
                ),
              ),
          ],
        ),
      ),
    ),
  );
}
