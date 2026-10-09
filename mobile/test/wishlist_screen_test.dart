import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mobile/core/auth/auth_session.dart';
import 'package:mobile/core/data/library_store.dart';
import 'package:mobile/features/wishlist/presentation/screens/wishlist_screen.dart';

const saved = SavedService(serviceId: 'service-1', name: 'Real SUP');
class Repository extends LibraryRepository {
  bool reject = true;
  @override
  Future<List<SavedService>> wishlist() async => [saved];
  @override
  Future<void> setFavorite(String id, bool value) async {
    if (reject) throw AuthFailure('Delete failed');
  }
}
void main() {
  testWidgets('Wishlist keeps item on failure, removes it after successful retry', (tester) async {
    final repository = Repository();
    final store = LibraryStore(repository: repository);
    await tester.pumpWidget(MaterialApp(home: WishlistScreen(store: store)));
    await tester.pumpAndSettle();
    expect(find.text('Real SUP'), findsOneWidget);
    await tester.tap(find.byIcon(Icons.favorite));
    await tester.pumpAndSettle();
    expect(find.text('Real SUP'), findsOneWidget);
    expect(find.text('Delete failed'), findsOneWidget);
    repository.reject = false;
    await tester.tap(find.byIcon(Icons.favorite));
    await tester.pumpAndSettle();
    expect(find.text('Real SUP'), findsNothing);
    expect(store.contains('service-1'), isFalse);
    await tester.pumpWidget(const SizedBox());
    store.dispose();
  });
}
