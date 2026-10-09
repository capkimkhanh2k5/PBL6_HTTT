import 'package:flutter/material.dart';
import '../../../../core/data/catalog_repository.dart';
import '../../../../core/l10n/app_localizations.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';

/// Public catalog details. Booking is connected separately from catalog reads.
class CatalogDetailScreen extends StatefulWidget {
  const CatalogDetailScreen({
    super.key,
    required this.serviceId,
    this.repository,
  });
  final String serviceId;
  final CatalogRepository? repository;
  @override
  State<CatalogDetailScreen> createState() => _CatalogDetailScreenState();
}

class _CatalogDetailScreenState extends State<CatalogDetailScreen> {
  ServiceModel? _service;
  String? _error;
  bool _loading = true;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final service = await (widget.repository ?? CatalogRepository()).detail(
        widget.serviceId,
      );
      if (mounted) setState(() => _service = service);
    } catch (e) {
      if (mounted) setState(() => _error = e.toString());
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final service = _service;
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(title: const LocalizedText('Chi tiết trải nghiệm')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
          ? Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Padding(
                    padding: const EdgeInsets.all(24),
                    child: Text(_error!),
                  ),
                  TextButton(
                    onPressed: _load,
                    child: const LocalizedText('Thử lại'),
                  ),
                ],
              ),
            )
          : service == null
          ? const SizedBox.shrink()
          : RefreshIndicator(
              onRefresh: _load,
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                children: [
                  if (service.imageUrls.isNotEmpty)
                    SizedBox(
                      height: 280,
                      child: PageView(
                        children: service.imageUrls
                            .map(
                              (url) => Image.network(
                                url,
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => const Icon(
                                  Icons.image_not_supported_outlined,
                                  size: 64,
                                ),
                              ),
                            )
                            .toList(),
                      ),
                    )
                  else
                    const SizedBox(
                      height: 180,
                      child: Icon(Icons.beach_access, size: 64),
                    ),
                  Padding(
                    padding: const EdgeInsets.all(20),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          service.name,
                          style: Theme.of(context).textTheme.headlineSmall,
                        ),
                        const SizedBox(height: 12),
                        if (service.categoryName.isNotEmpty)
                          Chip(label: Text(service.categoryName)),
                        if (service.address.isNotEmpty) Text(service.address),
                        const SizedBox(height: 12),
                        Text(
                          '${service.price} đ',
                          style: Theme.of(context).textTheme.titleLarge
                              ?.copyWith(color: AppColors.secondary),
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            const Icon(
                              Icons.star,
                              color: Colors.amber,
                              size: 20,
                            ),
                            Text(
                              ' ${service.avgRating.toStringAsFixed(1)} · ${service.ratingCount} ',
                            ),
                            const LocalizedText('đánh giá'),
                          ],
                        ),
                        const Divider(height: 32),
                        const LocalizedText('Giới thiệu'),
                        const SizedBox(height: 8),
                        Text(service.description),
                        const SizedBox(height: 24),
                        const LocalizedText(
                          'Đặt chỗ hiện chưa khả dụng.',
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}
