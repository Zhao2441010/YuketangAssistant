import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:cookie_jar/cookie_jar.dart';

import '../api/api_service.dart';
import '../api/login.dart';
import '../models/user.dart';
import 'account.dart';
import '../utils/storage.dart';
import '../utils/logger.dart';

class CookieInterceptor extends Interceptor {
  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    if (options.headers['Cookie'] == null) {
      final uri = options.uri;
      final String? userId = options.extra['userId'];
      if (userId != null) {
        final CookieJar? jar = userId.isEmpty
            ? CookieManager.tempCookieJar
            : CookieManager.getCookieJarForUser(userId);
        if (jar != null) {
          final cookies = await jar.loadForRequest(uri);
          if (cookies.isNotEmpty) {
            options.headers['Cookie'] =
                cookies.map((c) => '${c.name}=${c.value}').join('; ');
            final cookieMap = Map.fromEntries(
                cookies.map((c) => MapEntry(c.name, c.value)));
            options.headers['x-csrftoken'] = cookieMap['csrftoken'];
            options.headers['x-uid'] = userId;
            options.headers['sessionid'] = cookieMap['sessionid'];
          }
        }
      }
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) async {
    final setCookieHeaders = response.headers['set-cookie'];
    if (setCookieHeaders != null) {
      List<Cookie> cookies = [];
      for (final header in setCookieHeaders) {
        try {
          cookies.add(Cookie.fromSetCookieValue(header));
        } catch (e) {
          logger.w('忽略错误 Cookie: $header');
        }
      }
      final String? userId = response.requestOptions.extra['userId'];
      if (userId != null) {
        if (userId.isEmpty) {
          await CookieManager.tempCookieJar.saveFromResponse(
              response.realUri, cookies);
        } else {
          final jar = CookieManager.getCookieJarForUser(userId);
          if (jar != null) {
            await jar.saveFromResponse(response.realUri, cookies);
            CookieManager.saveCookiesToStorage(userId, jar);
          }
        }
      }
    }
    handler.next(response);
  }
}

class CookieManager {
  static Uri get domainUri => Uri.parse(ApiService.baseUrl);

  static final Map<String, CookieJar> _userCookieJars = {};
  static final CookieJar _tempCookieJar = CookieJar();
  static CookieJar get tempCookieJar => _tempCookieJar;

  static int _refreshCounts = 0;

  static Future<void> initialize() async => loadAllCookies();

  static Future<void> loadAllCookies() async {
    final accounts = AccountManager.allAccounts;
    if (accounts.isEmpty) return;
    await Future.wait(accounts.map((user) async {
      try {
        final jar = CookieJar();
        _userCookieJars[user.uid] = jar;
        await _loadCookiesForUser(user.uid, jar);
      } catch (e) {
        logger.e('预加载账号 ${user.uid} 的 CookieJar 失败：$e');
      }
    }));
    if (_refreshCounts < 2) {
      await _refreshAccounts();
    }
  }

  static Future<void> _refreshAccounts() async {
    _refreshCounts++;
    final accounts = AccountManager.allAccounts;
    if (accounts.isEmpty) return;
    final results = await ApiService.sendForEachUser<User?>(
      accounts,
      (user) => RCLoginApi(user).getUserInfo(),
    );
    int successCount = 0, failCount = 0;
    for (int i = 0; i < accounts.length; i++) {
      final refreshedUser = results[i];
      if (refreshedUser != null) {
        await AccountManager.addAccount(refreshedUser);
        successCount++;
      } else {
        accounts[i].setStatus(false);
        failCount++;
      }
    }
    if (successCount > 0 || failCount > 0) {
      logger.i('账号刷新完成：成功 $successCount 个，失败 $failCount 个');
    }
  }

  static CookieJar? getCookieJarForUser(String userId) => _userCookieJars[userId];

  static Future<void> _loadCookiesForUser(String userId, CookieJar jar) async {
    final String? cookiesJson = StorageManager.prefs.getString('cookies_$userId');
    if (cookiesJson == null) return;
    try {
      final List<dynamic> cookiesData = json.decode(cookiesJson);
      final cookies = cookiesData.map((d) {
        final cookie = Cookie(d['name'], d['value']);
        cookie.domain = d['domain'];
        cookie.path = d['path'] ?? '/';
        cookie.secure = d['secure'] ?? false;
        cookie.httpOnly = d['httpOnly'] ?? false;
        return cookie;
      }).toList();
      await jar.saveFromResponse(domainUri, cookies);
    } catch (e) {
      logger.e('用户 $userId 的 Cookie 加载失败：$e');
    }
  }

  static Future<void> saveCookiesToStorage(String userId, CookieJar jar) async {
    final cookies = await jar.loadForRequest(domainUri);
    final data = cookies.map((c) => {
      'name': c.name,
      'value': c.value,
      'domain': c.domain,
      'path': c.path ?? '/',
      'secure': c.secure,
      'httpOnly': c.httpOnly
    }).toList();
    await StorageManager.prefs.setString('cookies_$userId', json.encode(data));
  }

  static Future<void> clearCookiesForUser(String userId) async {
    _userCookieJars.remove(userId);
    await StorageManager.prefs.remove('cookies_$userId');
  }

  static Future<void> saveTempCookies(String userId) async {
    final tempCookies = await _tempCookieJar.loadForRequest(domainUri);
    if (tempCookies.isEmpty) return;
    final newJar = CookieJar();
    await newJar.saveFromResponse(domainUri, tempCookies);
    _userCookieJars[userId] = newJar;
    await saveCookiesToStorage(userId, newJar);
    await _tempCookieJar.deleteAll();
  }
}
