import 'package:flutter/foundation.dart';
import '../auth/auth_session.dart';

class SavedService {
  final String serviceId, name;
  final String? imageUrl;
  const SavedService({
    required this.serviceId,
    required this.name,
    this.imageUrl,
  });
  factory SavedService.fromJson(Map<String, dynamic> json) => SavedService(
    serviceId: json['serviceId'] as String,
    name: json['serviceName'] as String,
    imageUrl: json['primaryImageUrl'] as String?,
  );
}

class LibraryRepository {
  LibraryRepository({AuthSession? session})
    : session = session ?? AuthSession.instance;
  final AuthSession session;
  Future<List<SavedService>> _list(String path) async {
    final data = await session.requestJson(path);
    if (data is! List) throw AuthFailure('Dữ liệu máy chủ không hợp lệ.');
    return data
        .map((e) => SavedService.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<SavedService>> wishlist() => _list('/api/wishlists');
  Future<List<SavedService>> recent() => _list('/api/recently-viewed');
  Future<void> setFavorite(String serviceId, bool value) async {
    await session.requestJson(
      '/api/wishlists/${Uri.encodeComponent(serviceId)}',
      method: value ? 'POST' : 'DELETE',
    );
  }
}

/// One account-scoped source of truth shared by Home, detail and wishlist.
class LibraryStore extends ChangeNotifier {
  static final instance = LibraryStore();
  LibraryStore({LibraryRepository? repository})
    : repository = repository ?? LibraryRepository() {
    this.repository.session.accountVersion.addListener(_reset);
  }
  final LibraryRepository repository;
  List<SavedService> _wishlist = [], _recent = [];
  List<SavedService> get wishlist => List.unmodifiable(_wishlist);
  List<SavedService> get recent => List.unmodifiable(_recent);
  bool loadingWishlist = false, loadingRecent = false, wishlistReady = false;
  String? wishlistError, recentError;
  final Set<String> _pending = {};
  int _epoch = 0, _recentRequest = 0;
  bool contains(String id) => _wishlist.any((e) => e.serviceId == id);
  bool busy(String id) => _pending.contains(id) || loadingWishlist;
  void _reset() {
    ++_epoch;
    ++_recentRequest;
    _wishlist = [];
    _recent = [];
    _pending.clear();
    wishlistReady = false;
    loadingWishlist = false;
    loadingRecent = false;
    wishlistError = null;
    recentError = null;
    notifyListeners();
  }

  Future<void> loadWishlist() async {
    if (loadingWishlist || _pending.isNotEmpty) return;
    final epoch = _epoch;
    loadingWishlist = true;
    wishlistError = null;
    notifyListeners();
    try {
      final items = await repository.wishlist();
      if (epoch != _epoch) return;
      _wishlist = items;
      wishlistReady = true;
    } catch (e) {
      if (epoch == _epoch) wishlistError = e.toString();
    } finally {
      if (epoch == _epoch) {
        loadingWishlist = false;
        notifyListeners();
      }
    }
  }

  Future<void> loadRecent() async {
    final epoch = _epoch, request = ++_recentRequest;
    loadingRecent = true;
    recentError = null;
    notifyListeners();
    try {
      final items = await repository.recent();
      if (epoch != _epoch || request != _recentRequest) return;
      _recent = items;
    } catch (e) {
      if (epoch == _epoch && request == _recentRequest) {
        recentError = e.toString();
      }
    } finally {
      if (epoch == _epoch && request == _recentRequest) {
        loadingRecent = false;
        notifyListeners();
      }
    }
  }

  Future<void> toggle(SavedService item) async {
    final epoch = _epoch;
    if (busy(item.serviceId)) return;
    if (!wishlistReady) {
      await loadWishlist();
      if (epoch != _epoch) return;
      if (!wishlistReady) {
        throw AuthFailure(
          wishlistError ?? 'Không tải được danh sách yêu thích.',
        );
      }
    }
    if (busy(item.serviceId)) return;
    final add = !contains(item.serviceId);
    _pending.add(item.serviceId);
    notifyListeners();
    try {
      await repository.setFavorite(item.serviceId, add);
      if (epoch != _epoch) return;
      _wishlist = _wishlist
          .where((e) => e.serviceId != item.serviceId)
          .toList();
      if (add) _wishlist.insert(0, item);
    } finally {
      if (epoch == _epoch) {
        _pending.remove(item.serviceId);
        notifyListeners();
      }
    }
  }

  @override
  void dispose() {
    ++_epoch;
    repository.session.accountVersion.removeListener(_reset);
    super.dispose();
  }
}
