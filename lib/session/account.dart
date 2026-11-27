import 'dart:async';
import 'dart:convert';

import '../models/user.dart';
import 'cookie.dart';
import '../utils/storage.dart';

class AccountChangeNotifier {
  static final AccountChangeNotifier _instance = AccountChangeNotifier._internal();
  factory AccountChangeNotifier() => _instance;
  AccountChangeNotifier._internal();

  final StreamController<void> _controller = StreamController.broadcast();
  Stream<void> get accountChanges => _controller.stream;
  void notifyAccountChanged() => _controller.add(null);
}

/// 长江雨课堂账号管理（单一平台、多账号）
class AccountManager {
  static const _sessionKey = 'yktassistant_current_session';
  static const _accountsKey = 'yktassistant_accounts';

  static List<User> _accounts = [];
  static String? _currentSessionId;

  static List<User> get allAccounts => _accounts;
  static String? get currentSessionId => _currentSessionId;
  static User? get currentUser =>
      _currentSessionId != null ? getAccountById(_currentSessionId!) : null;

  static Future<void> initialize() async {
    _accounts = await _getAllAccountsFromStorage();
    _currentSessionId = await getCurrentSession();
  }

  static Future<List<User>> _getAllAccountsFromStorage() async {
    final accountsJson = StorageManager.prefs.getString(_accountsKey);
    if (accountsJson != null) {
      final List<dynamic> data = json.decode(accountsJson);
      return data.map((d) => User.fromJson(d)).toList();
    }
    return [];
  }

  static Future<void> _saveAccounts() async {
    await StorageManager.prefs.setString(
      _accountsKey,
      json.encode(_accounts.map((u) => u.toJson()).toList()),
    );
  }

  static Future<String?> getCurrentSession() async {
    _currentSessionId = StorageManager.prefs.getString(_sessionKey);
    return _currentSessionId;
  }

  static Future<void> setCurrentSession(String? userId) async {
    _currentSessionId = userId;
    if (userId != null) {
      StorageManager.prefs.setString(_sessionKey, userId);
      final user = getAccountById(userId);
      if (user != null) {
        _accounts.remove(user);
        _accounts.insert(0, user);
      }
      _saveAccounts();
    } else {
      StorageManager.prefs.remove(_sessionKey);
    }
    AccountChangeNotifier().notifyAccountChanged();
  }

  static void setCurrentSessionTemp(String? userId) => _currentSessionId = userId;

  static bool hasActiveSession() =>
      _currentSessionId != null && _currentSessionId!.isNotEmpty;

  static User? getAccountById(String userId) {
    try {
      return _accounts.firstWhere((acc) => acc.uid == userId);
    } catch (_) {
      return null;
    }
  }

  static Future<void> addAccount(User user) async {
    final index = _accounts.indexWhere((acc) => acc.uid == user.uid);
    await CookieManager.saveTempCookies(user.uid);
    if (index != -1) {
      _accounts[index] = user;
    } else {
      _accounts.add(user);
      if (!hasActiveSession()) {
        await setCurrentSession(user.uid);
      }
    }
    _saveAccounts();
    AccountChangeNotifier().notifyAccountChanged();
  }

  static Future<void> removeAccounts(List<String> userIds) async {
    _accounts.removeWhere((acc) => userIds.contains(acc.uid));
    _saveAccounts();
    for (final uid in userIds) {
      if (uid == currentSessionId) {
        await setCurrentSession(null);
      }
      await CookieManager.clearCookiesForUser(uid);
    }
    AccountChangeNotifier().notifyAccountChanged();
  }

  static Future<void> moveAccount(int fromIndex, int toIndex) async {
    if (fromIndex < 0 || fromIndex >= _accounts.length ||
        toIndex < 0 || toIndex >= _accounts.length || fromIndex == toIndex) {
      return;
    }
    final user = _accounts.removeAt(fromIndex);
    _accounts.insert(toIndex, user);
    await _saveAccounts();
  }

  /// 重新加载账号与 Cookie（启动时调用）
  static Future<void> reloadAccounts() async {
    _accounts = await _getAllAccountsFromStorage();
    await CookieManager.loadAllCookies();
    final currentSession = await getCurrentSession();
    await setCurrentSession(currentSession);
  }
}
