import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:dynamic_color/dynamic_color.dart';

import './pages/accounts.dart';
import './pages/courses/list.dart';
import './pages/login.dart';
import './api/api_service.dart';
import './session/cookie.dart';
import './session/account.dart';
import './utils/storage.dart';
import './utils/logger.dart';
import 'push/notification.dart';
import 'push/update.dart';

final GlobalKey<NavigatorState> navigatorKey = GlobalKey<NavigatorState>();

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initLogger();
  FlutterError.onError = (details) {
    logger.e('FlutterError', error: details.exception, stackTrace: details.stack);
  };
  await StorageManager.initialize();
  await ApiService.initialize();
  await AccountManager.initialize();
  await CookieManager.initialize();
  await NotificationService().initialize();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return DynamicColorBuilder(builder: (lightDynamic, darkDynamic) {
      final lightScheme =
          lightDynamic ?? ColorScheme.fromSeed(seedColor: const Color(0xFFC21F30));
      final darkScheme = darkDynamic ??
          ColorScheme.fromSeed(
              seedColor: const Color(0xFFC21F30), brightness: Brightness.dark);
      return MaterialApp(
        navigatorKey: navigatorKey,
        title: '长江雨课堂助手',
        locale: const Locale('zh', 'CN'),
        supportedLocales: const [Locale('zh', 'CN'), Locale('en', 'US')],
        localizationsDelegates: const [
          GlobalMaterialLocalizations.delegate,
          GlobalWidgetsLocalizations.delegate,
          GlobalCupertinoLocalizations.delegate,
        ],
        theme: ThemeData(useMaterial3: true, colorScheme: lightScheme),
        darkTheme: ThemeData(useMaterial3: true, colorScheme: darkScheme),
        themeMode: ThemeMode.system,
        home: const MainPage(),
        routes: {
          '/accounts': (context) => const AccountsPage(),
          '/login': (context) => const LoginPage(),
        },
      );
    });
  }
}

class MainPage extends StatefulWidget {
  const MainPage({super.key});
  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  int _selectedIndex = 0;
  final GlobalKey<CoursesPageState> _coursesKey = GlobalKey<CoursesPageState>();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      Update.checkUpdate();
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(
        index: _selectedIndex,
        children: [
          CoursesPage(key: _coursesKey),
          const AccountsPage(),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) {
          setState(() => _selectedIndex = index);
          _coursesKey.currentState?.onVisibilityChanged(index == 0);
        },
        destinations: const [
          NavigationDestination(icon: Icon(Icons.school), label: '课程'),
          NavigationDestination(icon: Icon(Icons.account_circle), label: '账号'),
        ],
      ),
    );
  }
}
