import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_tencent_captcha/flutter_tencent_captcha.dart';

import '../api/login.dart';
import '../api/qr_login.dart';
import '../models/user.dart';
import '../session/account.dart';
import '../utils/encrypt.dart';

Future<bool> handleLoginSuccess(BuildContext context, {User? loggedInUser}) async {
  try {
    final user = loggedInUser ?? await RCLoginApi(User.empty).getUserInfo();
    if (user == null) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('获取用户信息失败')),
        );
      }
      return false;
    }
    await AccountManager.addAccount(user);
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('${user.name} 登录成功')),
      );
    }
    return true;
  } catch (e) {
    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('登录处理失败')),
      );
    }
    return false;
  }
}

/// 微信扫码登录状态：网页端 WebSocket 协议（与 tools/YuketangAssistant 一致）
class QRCodeLoginState {
  WebQrLogin? _login;
  String? qrToken;
  String? qrImageUrl;
  bool isLoading = true;
  bool isLoginActive = true;
  bool hasError = false;
  String statusText = '正在获取二维码…';
  VoidCallback? onRefresh;

  Future<bool> initialize() async {
    _login = WebQrLogin();
    final ok = await _login!.start(
      onQrReady: (session) {
        if (!isLoginActive) return;
        qrImageUrl = session.qrUrl;
        qrToken = session.loginId;
        isLoading = false;
        hasError = false;
        statusText = '请用微信扫码并在手机上确认（${session.expireSeconds} 秒内有效）';
        onRefresh?.call();
      },
      onStatus: (text) {
        if (!isLoginActive) return;
        statusText = text;
        isLoading = false;
        hasError = true;
        onRefresh?.call();
      },
    );
    if (!ok) {
      isLoading = false;
      hasError = true;
      onRefresh?.call();
    }
    return ok;
  }

  /// 重新获取二维码（旧连接会被关闭）
  Future<bool> refresh() async {
    _login?.dispose();
    qrImageUrl = null;
    qrToken = null;
    isLoading = true;
    hasError = false;
    statusText = '正在获取二维码…';
    onRefresh?.call();
    return initialize();
  }

  /// 等待扫码确认；成功后返回已换好 sessionid 的用户
  Future<User?> startPolling() async {
    final login = _login;
    if (login == null) return null;
    final user = await login.waitForLogin();
    return (user != null && isLoginActive) ? user : null;
  }

  void dispose() {
    isLoginActive = false;
    _login?.dispose();
  }
}

class LoginPage extends StatefulWidget {
  final String initialLoginType;
  const LoginPage({super.key, this.initialLoginType = 'password'});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  final _captchaController = TextEditingController();
  final _captchaFocusNode = FocusNode();
  bool _isLoading = false;
  bool _showPassword = false;
  String _currentLoginType = '1'; // 1 密码 2 验证码 3 二维码
  Timer? _countdownTimer;
  int _countdownSeconds = 0;
  String? _ticket;
  String? _randstr;

  @override
  void initState() {
    super.initState();
    TencentCaptcha.init(Constant.tCaptchaAppId);
    if (widget.initialLoginType == 'captcha') {
      _currentLoginType = '2';
    } else if (widget.initialLoginType == 'qrcode') {
      _currentLoginType = '3';
      _showQRCodeLogin();
    }
  }

  @override
  void dispose() {
    _usernameController.dispose();
    _passwordController.dispose();
    _captchaController.dispose();
    _countdownTimer?.cancel();
    _captchaFocusNode.dispose();
    super.dispose();
  }

  Future<bool?> _showTencentCaptcha() async {
    final completer = Completer<bool?>();
    try {
      await TencentCaptcha.verify(
        config: TencentCaptchaConfig(
          bizState: 'tencent-captcha',
          enableDarkMode: Theme.of(context).brightness == Brightness.dark,
        ),
        onSuccess: (data) {
          if (data != null) {
            _ticket = data['ticket'];
            _randstr = data['randstr'];
            completer.complete(true);
          } else {
            completer.complete(false);
          }
        },
        onFail: (data) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('验证失败：${data['errorMessage']}')));
          }
          completer.complete(false);
        },
      );
    } catch (e) {
      completer.complete(false);
    }
    return completer.future;
  }

  Future<void> _showQRCodeLogin() async {
    final qrState = QRCodeLoginState();
    StateSetter? dialogSetState;
    qrState.onRefresh = () => dialogSetState?.call(() {});

    if (!await qrState.initialize()) {
      qrState.dispose();
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('获取二维码失败，请检查网络')),
        );
      }
      return;
    }

    // 等待扫码确认（凭据交换与 Cookie 落盘在 WebQrLogin 内完成）
    qrState.startPolling().then((user) async {
      if (user == null || !mounted) return;
      final ok = await handleLoginSuccess(context, loggedInUser: user);
      if (ok && mounted && Navigator.of(context).canPop()) {
        Navigator.pop(context, true);
      }
    });

    if (!mounted) {
      qrState.dispose();
      return;
    }
    await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (context) => StatefulBuilder(builder: (context, setState) {
        dialogSetState = setState;
        return AlertDialog(
          title: const Text('微信扫码登录'),
          content: Column(mainAxisSize: MainAxisSize.min, children: [
            SizedBox(
              width: 220,
              height: 220,
              child: qrState.qrImageUrl != null
                  ? Image.network(
                      qrState.qrImageUrl!,
                      fit: BoxFit.contain,
                      headers: const {
                        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) '
                            'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Safari/537.36',
                      },
                    )
                  : qrState.isLoading
                      ? const Center(child: CircularProgressIndicator())
                      : const Icon(Icons.error_outline, size: 48),
            ),
            const SizedBox(height: 12),
            Text(qrState.statusText, textAlign: TextAlign.center),
            if (qrState.hasError)
              TextButton.icon(
                onPressed: () => qrState.refresh(),
                icon: const Icon(Icons.refresh),
                label: const Text('重新获取二维码'),
              ),
          ]),
          actions: [
            TextButton(
              onPressed: () {
                qrState.dispose();
                Navigator.pop(context);
              },
              child: const Text('取消'),
            )
          ],
        );
      }),
    );
    qrState.dispose();
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _isLoading = true);
    try {
      Map<String, dynamic>? result;
      if (_currentLoginType == '2') {
        final verified = await RCLoginApi.verifyCaptcha(
            _usernameController.text, _captchaController.text);
        if (verified == null || verified['code'] != 0) {
          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text(verified?['msg'] ?? '验证码验证失败')));
          }
          return;
        }
        result = await RCLoginApi.login(3, _usernameController.text,
            _captchaController.text, _ticket ?? '', _randstr ?? '');
      } else {
        if (_ticket == null || _randstr == null) {
          final ok = await _showTencentCaptcha();
          if (ok != true) return;
        }
        result = await RCLoginApi.login(2, _usernameController.text,
            _passwordController.text, _ticket!, _randstr!);
        _ticket = null;
        _randstr = null;
      }
      if (result != null && result['code'] == 0) {
        final ok = await handleLoginSuccess(context);
        if (ok && mounted) Navigator.pop(context, true);
      } else if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(result?['msg'] ?? '登录失败')));
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text('登录时发生错误：$e')));
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  Future<void> _sendCaptcha() async {
    final phone = _usernameController.text.trim();
    if (phone.isEmpty) {
      ScaffoldMessenger.of(context)
          .showSnackBar(const SnackBar(content: Text('请输入手机号')));
      return;
    }
    final ok = await _showTencentCaptcha();
    if (ok != true) return;
    final result = await RCLoginApi.sendCaptcha(phone, _ticket!, _randstr!);
    if (result != null && result['code'] == 0) {
      setState(() {
        _countdownSeconds = 60;
        _countdownTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
          if (_countdownSeconds > 0) {
            setState(() => _countdownSeconds--);
          } else {
            timer.cancel();
          }
        });
      });
    } else if (mounted) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(result?['msg'] ?? '发送验证码失败')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(_currentLoginType == '1'
            ? '密码登录'
            : _currentLoginType == '2'
                ? '验证码登录'
                : '二维码登录'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
          key: _formKey,
          child: Column(children: [
            TextFormField(
              controller: _usernameController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                  labelText: '账号', hintText: '手机号/邮箱', border: OutlineInputBorder()),
              validator: (v) => v == null || v.isEmpty ? '请输入账号' : null,
            ),
            const SizedBox(height: 16),
            if (_currentLoginType == '1')
              TextFormField(
                controller: _passwordController,
                obscureText: !_showPassword,
                decoration: InputDecoration(
                  labelText: '密码',
                  border: const OutlineInputBorder(),
                  suffixIcon: IconButton(
                    icon: Icon(_showPassword
                        ? Icons.visibility
                        : Icons.visibility_off),
                    onPressed: () => setState(() => _showPassword = !_showPassword),
                  ),
                ),
                validator: (v) => v == null || v.isEmpty ? '请输入密码' : null,
              )
            else
              Row(children: [
                Expanded(
                  child: TextFormField(
                    controller: _captchaController,
                    focusNode: _captchaFocusNode,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                        labelText: '验证码', border: OutlineInputBorder()),
                    validator: (v) => v == null || v.isEmpty ? '请输入验证码' : null,
                  ),
                ),
                const SizedBox(width: 12),
                ElevatedButton(
                  onPressed: _countdownSeconds == 0 && !_isLoading
                      ? _sendCaptcha
                      : null,
                  child: Text(_countdownSeconds > 0 ? '${_countdownSeconds}s' : '获取验证码'),
                ),
              ]),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              height: 50,
              child: ElevatedButton(
                onPressed: _isLoading
                    ? null
                    : _currentLoginType == '3'
                        ? _showQRCodeLogin
                        : _login,
                child: Text(_currentLoginType == '3' ? '二维码登录' : '登录'),
              ),
            ),
          ]),
        ),
      ),
    );
  }
}
