import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/auth/auth_session.dart';
import 'package:mobile/core/data/library_store.dart';

const item = SavedService(serviceId: 'service-id', name: 'SUP');

class FakeLibrary extends LibraryRepository {
  List<SavedService> saved = [];
  int mutations = 0;
  bool fail = false;
  Completer<void>? mutation;
  Completer<List<SavedService>>? read;
  @override
  Future<List<SavedService>> wishlist() async =>
      read != null ? read!.future : List.of(saved);
  @override
  Future<List<SavedService>> recent() async => [item];
  @override
  Future<void> setFavorite(String id, bool value) async {
    mutations++;
    if (mutation != null) await mutation!.future;
    if (fail) throw AuthFailure('Rejected', 500);
    saved = value ? [item] : [];
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  test(
    'Duplicate taps make one POST; change appears only after success',
    () async {
      final repo = FakeLibrary()..mutation = Completer<void>();
      final store = LibraryStore(repository: repo);
      addTearDown(store.dispose);
      await store.loadWishlist();
      final first = store.toggle(item);
      await store.toggle(item);
      expect(repo.mutations, 1);
      expect(store.contains(item.serviceId), isFalse);
      expect(store.busy(item.serviceId), isTrue);
      repo.mutation!.complete();
      await first;
      expect(store.contains(item.serviceId), isTrue);
      repo.mutation = null;
      await store.toggle(item);
      expect(store.wishlist, isEmpty);
    },
  );
  test(
    'Rejected DELETE preserves existing favorite and releases busy state',
    () async {
      final repo = FakeLibrary()
        ..saved = [item]
        ..fail = true;
      final store = LibraryStore(repository: repo);
      addTearDown(store.dispose);
      await store.loadWishlist();
      await expectLater(store.toggle(item), throwsA(isA<AuthFailure>()));
      expect(store.contains(item.serviceId), isTrue);
      expect(store.busy(item.serviceId), isFalse);
    },
  );
  test(
    'Account switch clears cached data and ignores stale mutation response',
    () async {
      final repo = FakeLibrary()..mutation = Completer<void>();
      final store = LibraryStore(repository: repo);
      addTearDown(store.dispose);
      await store.loadWishlist();
      await store.loadRecent();
      final pending = store.toggle(item);
      repo.session.accountVersion.value++;
      expect(store.recent, isEmpty);
      repo.mutation!.complete();
      await pending;
      expect(store.wishlist, isEmpty);
      expect(store.wishlistReady, isFalse);
    },
  );
  test('Account switch discards previous wishlist read', () async {
    final repo = FakeLibrary()..read = Completer<List<SavedService>>();
    final store = LibraryStore(repository: repo);
    addTearDown(store.dispose);
    final pending = store.loadWishlist();
    repo.session.accountVersion.value++;
    repo.read!.complete([item]);
    await pending;
    expect(store.wishlist, isEmpty);
  });
  test(
    'HTTP contract uses serviceId and accepts POST 201 / DELETE 204 empty bodies',
    () async {
      HttpOverrides.global = null;
      final server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
      final session = AuthSession(baseUrl: 'http://127.0.0.1:${server.port}');
      final repo = LibraryRepository(session: session);
      final calls = <String>[];
      server.listen((request) async {
        calls.add('${request.method} ${request.uri.path}');
        if (request.method == 'POST') {
          request.response.statusCode = 201;
        } else if (request.method == 'DELETE') {
          request.response.statusCode = 204;
        } else {
          request.response.headers.contentType = ContentType.json;
          request.response.write(
            jsonEncode([
              {
                'id': 'row-id',
                'serviceId': 'service-id',
                'serviceName': 'SUP',
                'primaryImageUrl': null,
              },
            ]),
          );
        }
        await request.response.close();
      });
      try {
        expect((await repo.wishlist()).single.serviceId, 'service-id');
        expect((await repo.recent()).single.name, 'SUP');
        await repo.setFavorite('service-id', true);
        await repo.setFavorite('service-id', false);
        expect(calls, [
          'GET /api/wishlists',
          'GET /api/recently-viewed',
          'POST /api/wishlists/service-id',
          'DELETE /api/wishlists/service-id',
        ]);
      } finally {
        session.client.close(force: true);
        await server.close(force: true);
      }
    },
  );
}
