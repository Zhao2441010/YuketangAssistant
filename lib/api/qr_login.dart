import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'api_service.dart';
import '../models/user.dart';
import '../utils/logger.dart';

/// 网页端微信扫码登录会话信息。
class WebQrSession {
  final String qrUrl;
  final String loginId;
  final int expireSeconds;

  const WebQrSession({
    required this.qrUrl,
    required this.loginId,
    required this.expireSeconds,
  });
}

/// 网页端微信扫码登录。
///
/// 协议照 tools/YuketangAssistant 的 YktWebSocketLogin 实现（已实测可用）：
///  1. WS 连接 wss://changjiang.yuketang.cn/wsapp/
///  2. 发 requestlogin（role=web, type=qrcode）
///  3. 收 loginid + ticket（微信二维码 URL）+ expire_seconds
///  4. 用户微信扫码确认后，同一连接收到 loginsuccess（UserID / Auth）
///  5. POST /pc/web_login 用 UserID + Auth 换 sessionid / csrftoken
///  6. GET /v/course_meta/user_info 拿用户信息，Cookie 落盘由 CookieInterceptor 完成
///
/// 与原 APP 协议（pre-info + /api/v3/user/login 长轮询）相比，这条链路：
///  - 不依赖 30s 长轮询，避免 NAT / WebView 静默掐断连接后卡在二维码页；
///  - 登录态来自网页端 web_login，和网页端会话完全等价。
class WebQrLogin {
  WebSocket? _socket;
  StreamSubscription<dynamic>? _subscription;
  Timer? _expireTimer;

  String? _userId;
  String? _auth;
  bool _disposed = false;
  bool _exchanging = false;
  void Function(String status)? _onStatus;

  final Completer<WebQrSession> _qrReady = Completer<WebQrSession>();
  final Completer<User?> _loggedIn = Completer<User?>();

  WebQrSession? _session;
  WebQrSession? get session => _session;

  /// 启动扫码登录。
  ///
  /// [onQrReady] 在二维码可用时回调，[onStatus] 用于展示等待/过期/失败状态。
  Future<bool> start({
    void Function(WebQrSession session)? onQrReady,
    void Function(String status)? onStatus,
  }) async {
    _onStatus = onStatus;
    try {
      _socket = await WebSocket.connect(ApiService.wsUrl).timeout(
        const Duration(seconds: 10),
      );
      if (_disposed) {
        _socket?.close();
        return false;
      }

      _subscription = _socket!.listen(
        _onMessage,
        onError: (error) {
          logger.e('扫码 WebSocket 错误: $error');
          onStatus?.call('网络连接异常，请重新获取二维码');
          _safeComplete(null);
        },
        onDone: () {
          if (!_disposed && !_loggedIn.isCompleted) {
            logger.w('扫码 WebSocket 已断开');
            onStatus?.call('连接已断开，请重新获取二维码');
            _safeComplete(null);
          }
        },
      );

      final frame = jsonEncode({
        'op': 'requestlogin',
        'role': 'web',
        'version': 1.4,
        'type': 'qrcode',
        'from': 'web',
      });
      _socket!.add(frame);
      _qrReady.future.then((session) {
        onQrReady?.call(session);
      }, onError: (e) {
        logger.e('扫码二维码获取失败: $e');
        onStatus?.call('获取二维码失败：$e');
      });
      return true;
    } catch (e) {
      logger.e('扫码 WebSocket 连接失败: $e');
      onStatus?.call('无法连接服务器，请检查网络');
      _safeComplete(null);
      return false;
    }
  }

  void _onMessage(dynamic raw) {
    if (_disposed) return;
    Map<String, dynamic> msg;
    try {
      msg = jsonDecode(raw as String) as Map<String, dynamic>;
    } catch (_) {
      return;
    }

    final op = msg['op']?.toString();
    if (op == 'requestlogin') {
      final ticket = (msg['ticket'] ?? msg['qrUrl'] ?? '').toString();
      if (ticket.isEmpty) return;
      final qrUrl = ticket.startsWith('http')
          ? ticket
          : 'https://mp.weixin.qq.com/cgi-bin/showqrcode?ticket=$ticket';
      final expire = int.tryParse(msg['expire_seconds']?.toString() ?? '') ?? 60;
      _session = WebQrSession(
        qrUrl: qrUrl,
        loginId: (msg['loginid'] ?? msg['loginId'] ?? '').toString(),
        expireSeconds: expire,
      );
      if (!_qrReady.isCompleted) _qrReady.complete(_session!);
      _expireTimer?.cancel();
      _expireTimer = Timer(Duration(seconds: expire), () {
        if (!_loggedIn.isCompleted) {
          logger.i('扫码二维码已过期');
          _onStatus?.call('二维码已过期，请点击重新获取');
          _safeComplete(null);
        }
      });
      return;
    }

    if (op == 'loginsuccess') {
      // 只缓存带 Auth 的确认消息；subscribe_status 通知可能先到且不含凭据。
      final userId = (msg['UserID'] ?? msg['userId'] ?? msg['userid'] ?? '').toString();
      final auth = (msg['Auth'] ?? msg['auth'] ?? '').toString();
      if (userId.isEmpty || auth.isEmpty) {
        logger.d('收到扫码通知但无登录凭据: $msg');
        return;
      }
      _userId = userId;
      _auth = auth;
      _expireTimer?.cancel();
      _exchangeSession();
    }
  }

  /// 用 UserID + Auth 换网页端 sessionid（Cookie 由 CookieInterceptor 自动落盘）。
  Future<void> _exchangeSession() async {
    if (_exchanging || _disposed) return;
    _exchanging = true;
    try {
      final response = await ApiService.sendRequest(
        '/pc/web_login',
        method: 'POST',
        body: {'UserID': _userId, 'Auth': _auth},
        headers: {
          'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
              'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36',
          'referer': '${ApiService.baseUrl}/',
          'origin': ApiService.baseUrl,
        },
        userId: '', // Set-Cookie 进临时 CookieJar，登录成功后按 uid 落盘
      );
      if (response == null) {
        logger.e('web_login 换取会话失败');
        _safeComplete(null);
        return;
      }

      final info = await ApiService.sendRequest(
        '/v/course_meta/user_info',
        userId: '',
      );
      final profile = info?.data?['data']?['user_profile'];
      if (profile == null) {
        logger.e('扫码后获取用户信息失败');
        _safeComplete(null);
        return;
      }
      final user = User(
        uid: profile['user_id'].toString(),
        name: profile['name'] ?? '未知用户',
        avatar: profile['avatar']?.isNotEmpty == true
            ? profile['avatar']
            : (profile['avatar_96'] ?? ''),
        phone: profile['phone_number'] ?? '未知手机号',
        school: profile['school'] ?? '未知学校',
        platform: 'rainClassroom',
      );
      _safeComplete(user);
    } catch (e) {
      logger.e('扫码登录凭据交换失败: $e');
      _safeComplete(null);
    } finally {
      _exchanging = false;
    }
  }

  Future<User?> waitForLogin() => _loggedIn.future;

  void _safeComplete(User? user) {
    if (!_loggedIn.isCompleted) _loggedIn.complete(user);
  }

  void dispose() {
    _disposed = true;
    _expireTimer?.cancel();
    _subscription?.cancel();
    _socket?.close();
    _safeComplete(null);
  }
}
