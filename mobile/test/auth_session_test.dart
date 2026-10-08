import 'dart:convert';
import 'dart:io';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:mobile/core/auth/auth_session.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late HttpServer server;
  late AuthSession auth;
  final requests = <String>[];
  var rotation = 0;
  setUp(() async {
    HttpOverrides.global = null;
    FlutterSecureStorage.setMockInitialValues({});
    requests.clear();
    rotation = 0;
    server = await HttpServer.bind(InternetAddress.loopbackIPv4, 0);
    auth = AuthSession(baseUrl: 'http://127.0.0.1:${server.port}');
    server.listen((req) async {
      requests.add('${req.method} ${req.uri.path}');
      final text = await utf8.decoder.bind(req).join();
      final body = text.isEmpty
          ? <String, dynamic>{}
          : jsonDecode(text) as Map<String, dynamic>;
      final path = req.uri.path;
      req.response.headers.contentType = ContentType.json;
      if (path == '/api/auth/login' && body['password'] == 'wrong') {
        req.response.statusCode = 401;
        req.response.write(jsonEncode({'message': 'Invalid credentials'}));
      } else if (path == '/api/auth/login' || path == '/api/auth/register') {
        req.response.cookies.add(Cookie('refresh_token', 'initial'));
        req.response.write(jsonEncode({'accessToken': 'access'}));
      } else if (path == '/api/auth/refresh') {
        expect(req.cookies.single.value, rotation == 0 ? 'initial' : 'rotated');
        rotation++;
        req.response.cookies.add(Cookie('refresh_token', 'rotated'));
        req.response.write(jsonEncode({'accessToken': 'new-access'}));
      } else if (path == '/api/users/me') {
        expect(req.headers.value('Authorization'), startsWith('Bearer '));
        req.response.write(
          jsonEncode({
            'email': 'test@example.com',
            'fullName': body['fullName'] ?? 'Test',
          }),
        );
      } else if (path == '/api/auth/otp/verify' && body['code'] != '123456') {
        req.response.statusCode = 400;
        req.response.write(jsonEncode({'message': 'Invalid OTP'}));
      }
      await req.response.close();
    });
  });
  tearDown(() async {
    auth.client.close(force: true);
    await server.close(force: true);
  });
  test('Failed login cannot create a session', () async {
    await expectLater(
      auth.login('test@example.com', 'wrong'),
      throwsA(isA<AuthFailure>()),
    );
    expect(auth.profile, isNull);
    expect(await auth.restore(), isFalse);
  });
  test(
    'Registration waits for OTP, rejects wrong code and updates name after verification',
    () async {
      await auth.register('New Name', 'test@example.com', 'Strong123!');
      expect(auth.pendingVerification, isTrue);
      expect(requests, isNot(contains('GET /api/users/me')));
      await expectLater(auth.verify('000000'), throwsA(isA<AuthFailure>()));
      expect(auth.pendingVerification, isTrue);
      await auth.verify('123456');
      expect(auth.pendingVerification, isFalse);
      expect(auth.profile?['fullName'], 'New Name');
    },
  );
  test('Restores session with rotated cookie and logout clears it', () async {
    await auth.login('test@example.com', 'Strong123!');
    final restored = AuthSession(baseUrl: auth.baseUrl);
    try {
      expect(await restored.restore(), isTrue);
      expect(restored.profile?['email'], 'test@example.com');
      await Future.wait([restored.refresh(), restored.refresh()]);
      expect(rotation, 2);
      await restored.logout();
      expect(await restored.restore(), isFalse);
    } finally {
      restored.client.close(force: true);
    }
  });
  test('Password validation matches backend', () {
    expect(AuthSession.strongPassword('Strong123!'), isTrue);
    expect(AuthSession.strongPassword('12345678'), isFalse);
    expect(AuthSession.strongPassword('Strong123#'), isFalse);
  });
}
