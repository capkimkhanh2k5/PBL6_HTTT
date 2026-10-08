import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/auth/auth_session.dart';
import 'package:mobile/core/data/catalog_repository.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late HttpServer server;
  late AuthSession session;
  late CatalogRepository repository;
  late Uri requested;
  var invalid = false;
  setUp(() async {
    HttpOverrides.global = null;
    invalid = false;
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    session = AuthSession(baseUrl: 'http://127.0.0.1:${server.port}');
    repository = CatalogRepository(session: session);
    server.listen((request) async {
      requested = request.uri;
      request.response.headers.contentType = ContentType.json;
      if (invalid) {
        request.response.write('<html>not JSON</html>');
      } else if (request.uri.path == '/api/categories') {
        request.response.write(
          jsonEncode([
            {
              'id': 'root',
              'name': 'Biển',
              'children': [
                {'id': 'sup', 'name': 'Chèo SUP', 'children': []},
                {'id': 'hidden', 'name': 'Ẩn', 'isActive': false},
              ],
            },
          ]),
        );
      } else if (request.uri.path.endsWith('/missing')) {
        request.response.statusCode = 404;
        request.response.write(jsonEncode({'message': 'Service not found'}));
      } else if (request.uri.path == '/api/services') {
        request.response.write(
          jsonEncode({
            'content': [
              {
                'id': 'real-id',
                'name': 'SUP',
                'price': 300000,
                'promotionalPrice': 250000,
                'averageRating': null,
                'primaryImageUrl': null,
              },
            ],
            'page': int.parse(request.uri.queryParameters['page']!),
            'size': 20,
            'totalElements': 41,
          }),
        );
      } else {
        request.response.write(
          jsonEncode({
            'id': 'real-id',
            'name': 'SUP',
            'price': 300000,
            'description': 'Real description',
            'categoryId': 'sup',
            'categoryName': 'Chèo SUP',
            'imageUrls': ['https://example.com/image.jpg'],
            'averageRating': 4.5,
            'reviewCount': 8,
          }),
        );
      }
      await request.response.close();
    });
  });
  tearDown(() async {
    session.client.close(force: true);
    await server.close(force: true);
  });

  test(
    'Category endpoint accepts root JSON array and preserves child IDs',
    () async {
      final categories = await repository.categories();
      expect(categories.map((c) => c.id), ['root', 'sup']);
    },
  );
  test(
    'Search encodes Vietnamese and reserved characters, filters and page',
    () async {
      final page = await repository.search(
        keyword: '  chèo & biển  ',
        categoryId: 'sup',
        maxPrice: 500000,
        page: 1,
      );
      expect(requested.queryParameters['keyword'], 'chèo & biển');
      expect(requested.queryParameters['categoryId'], 'sup');
      expect(requested.queryParameters['maxPrice'], '500000');
      expect(page.page, 1);
      expect(page.total, 41);
      expect(page.items.single.id, 'real-id');
      expect(page.items.single.price, 250000);
      expect(page.items.single.imageUrls, isEmpty);
      expect(page.items.single.avgRating, 0);
      expect(page.items.single.vendorName, isEmpty);
    },
  );
  test('Detail uses server description, images and category', () async {
    final service = await repository.detail('real-id');
    expect(requested.path, '/api/services/real-id');
    expect(service.description, 'Real description');
    expect(service.categoryId, 'sup');
    expect(service.ratingCount, 8);
    expect(service.imageUrls, ['https://example.com/image.jpg']);
  });
  test(
    'Missing service propagates backend error without mock fallback',
    () async {
      await expectLater(
        repository.detail('missing'),
        throwsA(isA<AuthFailure>().having((e) => e.status, 'status', 404)),
      );
    },
  );
  test(
    'Invalid JSON is reported instead of silently becoming empty results',
    () async {
      invalid = true;
      await expectLater(repository.search(), throwsA(isA<AuthFailure>()));
    },
  );
}
