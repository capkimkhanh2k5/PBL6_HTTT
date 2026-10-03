import 'package:flutter_localizations/flutter_localizations.dart';
import 'core/l10n/app_localizations.dart';
import 'package:flutter/material.dart';
import 'core/theme/app_theme.dart';
import 'features/home/presentation/screens/home_screen.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await AppLanguage.instance.initialize();
  runApp(const DanaSeaApp());
}

class DanaSeaApp extends StatelessWidget {
  const DanaSeaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: AppLanguage.instance,
      builder: (context, child) => MaterialApp(
        locale: AppLanguage.instance.locale,
        supportedLocales: const [Locale('vi'), Locale('en')],
        localizationsDelegates: GlobalMaterialLocalizations.delegates,
        title: 'DanaSea',
        debugShowCheckedModeBanner: false,
        theme: AppTheme.lightTheme,
        home: const HomeScreen(),
      ),
    );
  }
}
