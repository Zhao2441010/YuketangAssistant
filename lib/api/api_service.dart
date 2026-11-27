import 'dart:convert';

import 'package:dio/dio.dart';

import '../utils/encrypt.dart';
import '../utils/logger.dart';
import '../utils/storage.dart';
import '../session/cookie.dart';
import '../session/account.dart';
import '../models/user.dart';

/// 长江雨课堂客户端请求头（复刻官方 Android APP）
class HeadersManager {
  static const _brand = 'google';
  static const _deviceModel = 'Pixel 9 Pro';
  static const _systemVersion = '16';
  static const _buildNumber = '1610';
  static const _incremental = '14624737';
  static const _rcVersion = '1.3.3';

  static final Map<String, String> rainClassroomHeaders = {
    'user-agent': 'Android',
    'brand': '$_brand $_deviceModel',
    'uuid': '',
    'buildnumber': _buildNumber,
    'xtua': 'client=app&tag=$_rcVersion&platform=Android',
    'systemversion': _systemVersion,
    'incremental': _incremental,
    'accept': 'application/json',
    'isphysicaldevice': 'true',
    'xtbz': 'ykt',
    'x-client': 'app'
  };

  static Map<String, String> get headers => Map.unmodifiable(rainClassroomHeaders);
}

/// 日志脱敏拦截器
class DioLoggerInterceptor extends Interceptor {
  static const Set<String> _maskRequestKeys = {
    'uname', 'to', 'phoneNumber', 'password', 'code', 'logininfo', 'uid',
    'objectId', 'address', 'latitude', 'longitude', 'currentFaceId', 'ticket', 'rand'
  };
  static const Set<String> _maskResponsePaths = {
    'data.user_profile.name', 'data.user_profile.phone_number',
    'data.user_profile.school_number', 'data.user_profile.nickname',
    'data.user_profile.user_id', 'data.id', 'data.name', 'data.schoolNumber',
    'data.lessonToken', 'data.token', '_token'
  };

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    logger.i('${options.method} ${_redactUri(options.uri)}');
    if (options.data != null) {
      logger.d('Body: ${_format(_redactRequest(options.data))}');
    }
    handler.next(options);
  }

  @override
  void onResponse(Response response, ResponseInterceptorHandler handler) {
    final options = response.requestOptions;
    logger.i('${response.statusCode} ${options.method} ${_redactUri(options.uri)}');
    final data = response.data;
    if (data != null && data is! List<int>) {
      logger.d('Body: ${_format(_redactResponse(data))}');
    }
    handler.next(response);
  }

  @override
  void onError(DioException err, ErrorInterceptorHandler handler) {
    logger.e('✗ ${err.response?.statusCode ?? '-'} ${err.requestOptions.method} '
        '${_redactUri(err.requestOptions.uri)}', error: err);
    final data = err.response?.data;
    if (data != null && data is! List<int>) {
      logger.d('Error body: ${_format(_redactResponse(data))}');
    }
    handler.next(err);
  }

  static dynamic _redactRequest(dynamic data) {
    if (data is Map) {
      return data.map((key, value) => MapEntry(
          key, _maskRequestKeys.contains(key.toString()) ? '*' : _redactRequest(value)));
    }
    if (data is List) return data.map(_redactRequest).toList();
    if (data is FormData) {
      final map = <String, dynamic>{};
      for (final e in data.fields) {
        map[e.key] = _maskRequestKeys.contains(e.key) ? '*' : e.value;
      }
      for (final e in data.files) {
        map[e.key] = '<file>';
      }
      return map;
    }
    return data;
  }

  static dynamic _redactResponse(dynamic data, [String prefix = '']) {
    if (data is String) {
      try {
        final decoded = jsonDecode(data);
        if (decoded is Map || decoded is List) {
          return jsonEncode(_redactResponse(decoded, prefix));
        }
      } catch (_) {}
      return data;
    }
    if (data is Map) {
      return data.map((key, value) {
        final k = key.toString();
        final path = prefix.isEmpty ? k : '$prefix.$k';
        return MapEntry(key, _maskResponsePaths.contains(path) ? '*' : _redactResponse(value, path));
      });
    }
    if (data is List) return data.map((e) => _redactResponse(e, prefix)).toList();
    return data;
  }

  static String _redactUri(Uri uri) {
    final q = uri.queryParameters.map(
        (k, v) => MapEntry(k, _maskRequestKeys.contains(k) ? '*' : v));
    return uri.replace(queryParameters: q).toString();
  }

  static String _format(dynamic data) {
    if (data is Map || data is List) {
      try {
        return jsonEncode(data);
      } catch (_) {
        return data.toString();
      }
    }
    return data.toString();
  }
}

/// 全局 HTTP 服务：固定指向长江雨课堂
class ApiService {
  static const String serverName = '长江雨课堂';
  static const String baseUrl = 'https://changjiang.yuketang.cn';
  static const String wsUrl = 'wss://changjiang.yuketang.cn/wsapp/';

  static late Dio _dio;
  static const _uniqueIdKey = 'app_unique_id';

  static String get uniqueId {
    final prefs = StorageManager.prefs;
    if (prefs.containsKey(_uniqueIdKey)) {
      return prefs.getString(_uniqueIdKey)!;
    }
    final id = EncryptionUtil.getUniqueId();
    prefs.setString(_uniqueIdKey, id);
    return id;
  }

  static Future<void> initialize() async {
    _dio = Dio(BaseOptions(
      baseUrl: baseUrl,
      headers: HeadersManager.headers,
      connectTimeout: const Duration(seconds: 3),
      receiveTimeout: const Duration(seconds: 10),
      sendTimeout: const Duration(seconds: 10),
      followRedirects: false,
      validateStatus: (status) => status != null && status >= 200 && status < 400,
    ));
    _dio.interceptors.add(CookieInterceptor());
    _dio.interceptors.add(DioLoggerInterceptor());
  }

  static Future<Response?> sendRequest(
    String url, {
    String method = 'GET',
    Map<String, String>? params,
    Map<String, String>? headers,
    dynamic body,
    ResponseType responseType = ResponseType.json,
    int? receiveTimeoutSeconds,
    bool allowRedirects = true,
    String? userId,
  }) async {
    try {
      final options = Options(
        method: method,
        headers: headers,
        responseType: responseType,
        receiveTimeout: Duration(seconds: receiveTimeoutSeconds ?? 10),
        extra: {'userId': userId ?? AccountManager.currentSessionId},
      );

      var response = await _dio.request(
        url,
        queryParameters: params,
        data: body,
        options: options,
      );

      if (allowRedirects) {
        int redirectCount = 0;
        const maxRedirects = 3;
        while (redirectCount < maxRedirects) {
          var locationUrl = response.headers['location']?.first;
          if (locationUrl == null) break;
          if (!locationUrl.startsWith('http')) {
            final uri = response.realUri;
            final baseUri = '${uri.scheme}://${uri.host}${uri.hasPort ? ':${uri.port}' : ''}';
            locationUrl = baseUri + locationUrl;
          }
          response = await _dio.get(locationUrl, options: options);
          redirectCount++;
        }
      }

      if (options.responseType == ResponseType.json && response.data is String) {
        response.data = jsonDecode(response.data);
      }
      return response;
    } catch (e, stackTrace) {
      logger.e('HTTP Exception [$url]: $e\n$stackTrace');
    }
    return null;
  }

  static Future<List<T?>> sendForEachUser<T>(
    List<User> users,
    Future<T?> Function(User user) apiFunction,
  ) async {
    if (users.isEmpty) return [];
    final futures = users.map((user) async {
      try {
        return await apiFunction(user);
      } catch (e) {
        logger.e('用户 ${user.name} 请求失败：$e');
        return null;
      }
    }).toList();
    return await Future.wait(futures);
  }
}

abstract class Api {
  final User? user;
  Api([User? user]) : user = user ?? AccountManager.currentUser;
}
