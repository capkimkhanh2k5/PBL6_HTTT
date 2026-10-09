import 'dart:async';
import 'package:flutter/material.dart';
import '../../../../core/data/catalog_repository.dart';
import '../../../../core/l10n/app_localizations.dart';
import '../../../../core/models/danasea_models.dart';
import '../../../../core/theme/app_colors.dart';
import '../../../experience_detail/presentation/screens/catalog_detail_screen.dart';

class SearchScreen extends StatefulWidget {
  final String? initialQuery;
  final String? initialCategory;
  final CatalogRepository? repository;
  const SearchScreen({
    super.key,
    this.initialQuery,
    this.initialCategory,
    this.repository,
  });
  @override
  State<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends State<SearchScreen> {
  late final CatalogRepository _repository;
  late final TextEditingController _searchController;
  List<CategoryModel> _categories = [];
  List<ServiceModel> _items = [];
  String? _category;
  int? _maxPrice;
  int _page = 0, _total = 0, _generation = 0;
  bool _loading = true;
  String? _error, _categoryError;
  Timer? _debounce;

  @override
  void initState() {
    super.initState();
    _repository = widget.repository ?? CatalogRepository();
    _searchController = TextEditingController(text: widget.initialQuery ?? '');
    _category = widget.initialCategory;
    _loadCategories();
    _search();
  }

  Future<void> _loadCategories() async {
    try {
      final categories = await _repository.categories();
      if (mounted) {
        setState(() {
          _categories = categories;
          _categoryError = null;
        });
      }
    } catch (e) {
      if (mounted) setState(() => _categoryError = e.toString());
    }
  }

  Future<void> _search({bool more = false}) async {
    _debounce?.cancel();
    final generation = ++_generation;
    final page = more ? _page + 1 : 0;
    setState(() {
      _loading = true;
      _error = null;
      if (!more) {
        _items = [];
        _total = 0;
      }
    });
    try {
      final result = await _repository.search(
        keyword: _searchController.text,
        categoryId: _category,
        maxPrice: _maxPrice,
        page: page,
      );
      if (!mounted || generation != _generation) return;
      setState(() {
        _items = more ? [..._items, ...result.items] : result.items;
        _page = result.page;
        _total = result.total;
      });
    } catch (e) {
      if (mounted && generation == _generation) {
        setState(() => _error = e.toString());
      }
    } finally {
      if (mounted && generation == _generation) {
        setState(() => _loading = false);
      }
    }
  }

  void _queryChanged(String value) {
    _debounce?.cancel();
    // Invalidate in-flight results immediately, before the debounce completes.
    ++_generation;
    setState(() {
      _loading = true;
      _items = [];
      _total = 0;
      _error = null;
    });
    _debounce = Timer(const Duration(milliseconds: 350), _search);
  }

  @override
  void dispose() {
    _debounce?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.surface,
      appBar: AppBar(title: const LocalizedText('Tìm trải nghiệm')),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: TextField(
              controller: _searchController,
              maxLength: 100,
              onChanged: _queryChanged,
              onSubmitted: (_) => _search(),
              decoration: InputDecoration(
                hintText: translate(
                  'Tìm trải nghiệm',
                  AppLanguage.instance.code,
                ),
                prefixIcon: const Icon(Icons.search),
                border: const OutlineInputBorder(),
                counterText: '',
              ),
            ),
          ),
          if (_categoryError != null)
            TextButton(
              onPressed: _loadCategories,
              child: const LocalizedText('Không tải được danh mục. Thử lại'),
            ),
          SizedBox(
            height: 48,
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              scrollDirection: Axis.horizontal,
              children: [
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: ChoiceChip(
                    label: const LocalizedText('Tất cả'),
                    selected: _category == null,
                    onSelected: (_) {
                      setState(() => _category = null);
                      _search();
                    },
                  ),
                ),
                ..._categories.map(
                  (category) => Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: ChoiceChip(
                      label: Text(
                        AppLanguage.instance.code == 'en' &&
                                category.nameEn.isNotEmpty
                            ? category.nameEn
                            : category.name,
                      ),
                      selected: _category == category.id,
                      onSelected: (_) {
                        setState(() => _category = category.id);
                        _search();
                      },
                    ),
                  ),
                ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Row(
              children: [
                Expanded(child: LocalizedText('$_total kết quả')),
                DropdownButton<int>(
                  value: _maxPrice ?? 0,
                  items: [
                    const DropdownMenuItem(
                      value: 0,
                      child: LocalizedText('Tất cả mức giá'),
                    ),
                    for (final price in [300000, 500000, 1000000])
                      DropdownMenuItem(value: price, child: Text('≤ $price đ')),
                  ],
                  onChanged: (value) {
                    setState(() => _maxPrice = value == 0 ? null : value);
                    _search();
                  },
                ),
              ],
            ),
          ),
          if (_loading) const LinearProgressIndicator(),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                children: [
                  Text(_error!),
                  TextButton(
                    onPressed: () => _search(more: _items.isNotEmpty),
                    child: const LocalizedText('Thử lại'),
                  ),
                ],
              ),
            ),
          Expanded(
            child: RefreshIndicator(
              onRefresh: () => _search(),
              child: ListView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.all(16),
                children: [
                  if (!_loading && _error == null && _items.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(32),
                      child: Center(
                        child: LocalizedText('Không tìm thấy trải nghiệm.'),
                      ),
                    ),
                  ..._items.map(
                    (service) => Card(
                      clipBehavior: Clip.antiAlias,
                      margin: const EdgeInsets.only(bottom: 16),
                      child: InkWell(
                        onTap: () => Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (_) => CatalogDetailScreen(
                              serviceId: service.id,
                              repository: _repository,
                            ),
                          ),
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            if (service.imageUrls.isNotEmpty)
                              Image.network(
                                service.imageUrls.first,
                                width: double.infinity,
                                height: 170,
                                fit: BoxFit.cover,
                                errorBuilder: (_, _, _) => const SizedBox(
                                  height: 100,
                                  child: Center(
                                    child: Icon(Icons.image_not_supported),
                                  ),
                                ),
                              ),
                            Padding(
                              padding: const EdgeInsets.all(16),
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    service.name,
                                    style: Theme.of(
                                      context,
                                    ).textTheme.titleMedium,
                                  ),
                                  Text(service.address),
                                  const SizedBox(height: 8),
                                  Text(
                                    '${service.price} đ · ★ ${service.avgRating.toStringAsFixed(1)}',
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  if (!_loading && _error == null && _items.length < _total)
                    TextButton(
                      onPressed: () => _search(more: true),
                      child: const LocalizedText('Xem thêm'),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
